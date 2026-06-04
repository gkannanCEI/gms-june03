package com.gms.controller;

import com.gms.dto.AnswerDTO;
import com.gms.dto.SaveResult;
import com.gms.entity.Application;
import com.gms.entity.FileAttachment;
import com.gms.entity.RoundPage;
import com.gms.exception.ResourceNotFoundException;
import com.gms.repository.RoundPageRepository;
import com.gms.service.ApplicationDataService;
import com.gms.service.ApplicationService;
import com.gms.service.FileStorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
public class ApplicationDataController {

    private final ApplicationService applicationService;
    private final ApplicationDataService applicationDataService;
    private final FileStorageService fileStorageService;
    private final RoundPageRepository roundPageRepository;

    public ApplicationDataController(ApplicationService applicationService,
                                     ApplicationDataService applicationDataService,
                                     FileStorageService fileStorageService,
                                     RoundPageRepository roundPageRepository) {
        this.applicationService = applicationService;
        this.applicationDataService = applicationDataService;
        this.fileStorageService = fileStorageService;
        this.roundPageRepository = roundPageRepository;
    }

    // --- Application lifecycle ---

    @PostMapping("/api/programs/{programId}/rounds/{roundId}/applications")
    public ResponseEntity<Application> createApplication(@PathVariable Long programId,
                                                          @PathVariable Long roundId) {
        Application app = applicationService.createApplication(roundId);
        return ResponseEntity.status(HttpStatus.CREATED).body(app);
    }

    @GetMapping("/api/programs/{programId}/rounds/{roundId}/applications/mine")
    public ResponseEntity<Application> getMyApplication(@PathVariable Long programId,
                                                         @PathVariable Long roundId) {
        return ResponseEntity.ok(applicationService.getMyApplication(roundId));
    }

    @PutMapping("/api/applications/{appId}/status")
    public ResponseEntity<Application> updateStatus(@PathVariable Long appId,
                                                     @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(applicationService.updateStatus(appId, body.get("status")));
    }

    // --- Answers ---

    /**
     * GAP-1 / GAP-16: The pageId path variable is now resolved to the
     * corresponding RoundPage.id (roundPageId) before calling saveAnswers(),
     * so the service can load per-question RPQ constraints (min/max values,
     * role visibility, etc.) from gms_round_page_question.
     *
     * Resolution: We look up the Application to find its programRoundId,
     * then find the RoundPage for (programRoundId, pageId).
     */
    @PostMapping("/api/applications/{appId}/pages/{pageId}/answers")
    public ResponseEntity<SaveResult> saveAnswers(@PathVariable Long appId,
                                                   @PathVariable Long pageId,
                                                   @RequestBody List<AnswerDTO> answers) {
        applicationService.verifyOwnership(appId);
        Long roundPageId = resolveRoundPageId(appId, pageId);
        SaveResult result = applicationDataService.saveAnswers(appId, roundPageId, answers);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/applications/{appId}/pages/{pageId}/answers")
    public ResponseEntity<List<AnswerDTO>> loadAnswers(@PathVariable Long appId,
                                                       @PathVariable Long pageId,
                                                       @RequestParam List<Long> questionIds) {
        applicationService.verifyOwnership(appId);
        List<AnswerDTO> answers = applicationDataService.loadAnswers(appId, questionIds);
        return ResponseEntity.ok(answers);
    }

    // --- File attachments ---

    @PostMapping("/api/applications/{appId}/files")
    public ResponseEntity<FileAttachment> uploadFile(@PathVariable Long appId,
                                                      @RequestParam Long questionId,
                                                      @RequestParam("file") MultipartFile file) {
        applicationService.verifyOwnership(appId);
        FileAttachment attachment = fileStorageService.uploadFile(appId, questionId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(attachment);
    }

    @GetMapping("/api/applications/{appId}/files/{fileId}")
    public ResponseEntity<Void> downloadFile(@PathVariable Long appId,
                                             @PathVariable String fileId) {
        applicationService.verifyOwnership(appId);
        FileAttachment attachment = fileStorageService.getFile(appId, fileId);
        if (!"CLEAN".equals(attachment.getScanStatus())) {
            return ResponseEntity.notFound().build();
        }
        String url = fileStorageService.generateDownloadUrl(attachment);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }

    @DeleteMapping("/api/applications/{appId}/files/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable Long appId,
                                           @PathVariable String fileId) {
        applicationService.verifyOwnership(appId);
        fileStorageService.deleteFile(appId, fileId);
        return ResponseEntity.noContent().build();
    }

    // --- Admin endpoints ---

    @GetMapping("/api/admin/programs/{programId}/rounds/{roundId}/applications")
    public ResponseEntity<Page<Application>> listApplications(@PathVariable Long programId,
                                                               @PathVariable Long roundId,
                                                               @RequestParam(required = false) String status,
                                                               @RequestParam(required = false) Boolean eligibilityWarning,
                                                               Pageable pageable) {
        return ResponseEntity.ok(applicationService.listApplicationsForRound(roundId, status, eligibilityWarning, pageable));
    }

    @PutMapping("/api/admin/applications/{appId}/reopen")
    public ResponseEntity<Application> reopenApplication(@PathVariable Long appId) {
        return ResponseEntity.ok(applicationService.reopenApplication(appId));
    }

    // --- Private helpers ---

    /**
     * Resolves the gms_round_page.id for a given application + page combination.
     * Fetches the application to discover its programRoundId, then looks up the
     * RoundPage record that joins that round with the requested page.
     */
    private Long resolveRoundPageId(Long appId, Long pageId) {
        Application app = applicationService.getApplication(appId);
        Long roundId = app.getProgramRound().getId();
        RoundPage roundPage = roundPageRepository.findByProgramRoundIdAndPageId(roundId, pageId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Page " + pageId + " is not assigned to the round for application " + appId));
        return roundPage.getId();
    }
}
