package com.example.localstack.controller;

import com.example.localstack.service.impl.S3ServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/document")
@Tag(name = "Document Management API")
@SecurityRequirements({@SecurityRequirement(name = "bearerAuth")})
public class DocumentS3Controller {
    private final S3ServiceImpl s3ServiceImpl;

    public DocumentS3Controller(S3ServiceImpl s3ServiceImpl) {
        this.s3ServiceImpl = s3ServiceImpl;
    }

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload document to S3 bucket")
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("path") String path,
            @RequestParam Map<String, String> metadata
    ) {
        try {
            s3ServiceImpl.upload("test", UUID.randomUUID().toString(), file.getInputStream());
        } catch (IOException e) {
            return ResponseEntity.ok(new DocumentUploadResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error uploading file"));
        }
        return ResponseEntity.ok(new DocumentUploadResponse(HttpStatus.CREATED, "File uploaded successfully"));
    }

    public record DocumentUploadResponse(HttpStatus status, String message){}
}
