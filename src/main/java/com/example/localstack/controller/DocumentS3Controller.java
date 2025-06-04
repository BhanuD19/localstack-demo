package com.example.localstack.controller;

import com.example.localstack.config.security.UserContext;
import com.example.localstack.service.impl.S3ServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Document Management API")
@SecurityRequirements({@SecurityRequirement(name = "bearerAuth")})
@Slf4j
public class DocumentS3Controller {
    private final S3ServiceImpl s3ServiceImpl;
    private final UserContext userContext;
    public DocumentS3Controller(S3ServiceImpl s3ServiceImpl, UserContext userContext) {
        this.s3ServiceImpl = s3ServiceImpl;
        this.userContext = userContext;
    }

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload document to S3 bucket")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("path") String path,
            @RequestParam Map<String, String> metadata
    ) {
        String userId = userContext.getCurrentUserId();
        log.info("Uploading file to S3 bucket: {} for user: {}", path, userId);
        s3ServiceImpl.upload(path, file, metadata, userId);
        return ResponseEntity.ok(new DocumentUploadResponse(HttpStatus.CREATED, "File uploaded successfully"));
    }

    public record DocumentUploadResponse(HttpStatus status, String message){}
}
