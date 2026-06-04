package com.gms.controller;

import com.gms.service.FileStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Webhook endpoint for Azure Defender for Storage (or equivalent virus scanning service)
 * to report scan results. This endpoint should be secured via a shared secret or
 * Event Grid validation in production.
 */
@RestController
@RequestMapping("/api/internal/scan-results")
public class FileScanWebhookController {

    private final FileStorageService fileStorageService;

    public FileScanWebhookController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * Receives scan result from the virus scanning service.
     * Expected payload: { "fileId": "uuid", "scanStatus": "CLEAN" | "INFECTED" }
     */
    @PostMapping
    public ResponseEntity<Void> receiveScanResult(@RequestBody Map<String, String> payload) {
        String fileId = payload.get("fileId");
        String scanStatus = payload.get("scanStatus");

        if (fileId == null || scanStatus == null) {
            return ResponseEntity.badRequest().build();
        }

        if (!"CLEAN".equals(scanStatus) && !"INFECTED".equals(scanStatus)) {
            return ResponseEntity.badRequest().build();
        }

        fileStorageService.updateScanResult(fileId, scanStatus);
        return ResponseEntity.ok().build();
    }
}
