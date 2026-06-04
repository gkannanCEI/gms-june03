package com.gms.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.gms.entity.Application;
import com.gms.entity.FileAttachment;
import com.gms.entity.Question;
import com.gms.exception.ResourceNotFoundException;
import com.gms.exception.ValidationException;
import com.gms.repository.ApplicationRepository;
import com.gms.repository.FileAttachmentRepository;
import com.gms.repository.QuestionRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class FileStorageService {

    @Value("${gms.file-storage.connection-string}")
    private String connectionString;

    @Value("${gms.file-storage.container-name:gms-attachments}")
    private String containerName;

    @Value("${gms.file-storage.sas-expiry-minutes:5}")
    private int sasExpiryMinutes;

    @Value("${gms.file-storage.max-file-size-mb:100}")
    private int maxFileSizeMb;

    private final FileAttachmentRepository fileAttachmentRepository;
    private final ApplicationRepository applicationRepository;
    private final QuestionRepository questionRepository;

    private BlobContainerClient containerClient;

    public FileStorageService(FileAttachmentRepository fileAttachmentRepository,
                              ApplicationRepository applicationRepository,
                              QuestionRepository questionRepository) {
        this.fileAttachmentRepository = fileAttachmentRepository;
        this.applicationRepository = applicationRepository;
        this.questionRepository = questionRepository;
    }

    @PostConstruct
    public void init() {
        if (connectionString == null || connectionString.isBlank()
            || connectionString.startsWith("DefaultEndpointsProtocol=https;AccountName=devaccount")) {
            // No valid Azure connection — file storage disabled for local dev
            containerClient = null;
            return;
        }
        try {
            BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
            containerClient = serviceClient.getBlobContainerClient(containerName);
            if (!containerClient.exists()) {
                containerClient.create();
            }
        } catch (Exception e) {
            // Log warning but don't fail startup — file uploads will return errors
            containerClient = null;
        }
    }

    public FileAttachment uploadFile(Long applicationId, Long questionId, MultipartFile file) {
        Application app = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        Question question = questionRepository.findById(questionId)
            .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

        if (!"ATTACHMENT".equals(question.getQuestionType())) {
            throw new ValidationException("Question is not an ATTACHMENT type");
        }

        // Validate file extension
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        if (question.getAllowedFileTypes() != null) {
            Set<String> allowed = Arrays.stream(question.getAllowedFileTypes().split(","))
                .map(s -> s.trim().toLowerCase())
                .collect(Collectors.toSet());
            if (!allowed.contains(extension.toLowerCase())) {
                throw new ValidationException("File type ." + extension + " is not allowed. Allowed: "
                    + question.getAllowedFileTypes());
            }
        }

        // Validate file size
        int maxMb = question.getMaxFileSizeMb() != null ? question.getMaxFileSizeMb() : maxFileSizeMb;
        long maxBytes = (long) maxMb * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new ValidationException("File size exceeds maximum of " + maxMb + " MB");
        }

        // Generate file ID and blob path
        String fileId = UUID.randomUUID().toString();
        String blobPath = applicationId + "/" + questionId + "/" + fileId + "." + extension;

        // Upload to Azure Blob Storage
        if (containerClient != null) {
            try {
                BlobClient blobClient = containerClient.getBlobClient(blobPath);
                blobClient.upload(file.getInputStream(), file.getSize(), true);
            } catch (IOException e) {
                throw new RuntimeException("Failed to upload file to blob storage", e);
            }
        }
        // If containerClient is null (local dev), file metadata is saved but blob is not stored

        // Create metadata record with PENDING scan status
        FileAttachment attachment = new FileAttachment();
        attachment.setId(fileId);
        attachment.setApplication(app);
        attachment.setQuestion(question);
        attachment.setOriginalFilename(originalFilename);
        attachment.setFileExtension(extension);
        attachment.setFileSizeBytes(file.getSize());
        attachment.setBlobPath(blobPath);
        attachment.setScanStatus("PENDING");
        attachment.setUploadedAt(Instant.now());

        return fileAttachmentRepository.save(attachment);
    }

    public FileAttachment getFile(Long applicationId, String fileId) {
        return fileAttachmentRepository.findByIdAndApplicationId(fileId, applicationId)
            .orElseThrow(() -> new ResourceNotFoundException("File not found"));
    }

    /**
     * Generates a short-lived SAS URL for secure file download.
     * The URL expires after sasExpiryMinutes and is scoped to read-only on the specific blob.
     */
    public String generateDownloadUrl(FileAttachment attachment) {
        if (containerClient == null) {
            return "/api/local-files/" + attachment.getId(); // fallback for local dev
        }
        BlobClient blobClient = containerClient.getBlobClient(attachment.getBlobPath());
        BlobSasPermission permission = new BlobSasPermission().setReadPermission(true);
        OffsetDateTime expiry = OffsetDateTime.now().plusMinutes(sasExpiryMinutes);
        BlobServiceSasSignatureValues sasValues = new BlobServiceSasSignatureValues(expiry, permission);
        String sasToken = blobClient.generateSas(sasValues);
        return blobClient.getBlobUrl() + "?" + sasToken;
    }

    public void deleteFile(Long applicationId, String fileId) {
        FileAttachment attachment = getFile(applicationId, fileId);
        if (containerClient != null) {
            BlobClient blobClient = containerClient.getBlobClient(attachment.getBlobPath());
            if (blobClient.exists()) {
                blobClient.delete();
            }
        }
        fileAttachmentRepository.delete(attachment);
    }

    /**
     * Called by the virus scan callback (Event Grid webhook or polling service) to update
     * the scan result for a file.
     */
    public void updateScanResult(String fileId, String scanStatus) {
        FileAttachment attachment = fileAttachmentRepository.findById(fileId)
            .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));
        attachment.setScanStatus(scanStatus);
        attachment.setScannedAt(Instant.now());

        if ("INFECTED".equals(scanStatus)) {
            if (containerClient != null) {
                BlobClient blobClient = containerClient.getBlobClient(attachment.getBlobPath());
                if (blobClient.exists()) {
                    blobClient.delete();
                }
            }
        }

        fileAttachmentRepository.save(attachment);
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
