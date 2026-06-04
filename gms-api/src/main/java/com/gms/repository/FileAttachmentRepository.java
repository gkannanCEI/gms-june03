package com.gms.repository;

import com.gms.entity.FileAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FileAttachmentRepository extends JpaRepository<FileAttachment, String> {
    Optional<FileAttachment> findByIdAndApplicationId(String fileId, Long applicationId);
}
