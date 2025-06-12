package com.example.localstack.controller;

import com.example.localstack.config.security.UserContext;
import com.example.localstack.data.dbEntities.DocumentMetadata;
import com.example.localstack.service.S3Services;
import com.example.localstack.service.impl.S3ServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Document Management API")
@SecurityRequirements({@SecurityRequirement(name = "bearerAuth")})
@Slf4j
public class DocumentS3Controller {
    private final S3Services s3ServiceImpl;
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

    @GetMapping("/{documentId}")
    @Operation(summary = "Download document by ID")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Resource> downloadDocument(@PathVariable String documentId) throws IOException {
        String userId = userContext.getCurrentUserId();
        log.info("User {} downloading document: {}", userId, documentId);

        Optional<DocumentMetadata> metadata = s3ServiceImpl.getDocumentMetadata(documentId);
        if (metadata.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        DocumentMetadata doc = metadata.get();

        // Check if user has access to this document
        if (!hasAccessToDocument(doc, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        InputStream documentStream = s3ServiceImpl.downloadDocument(documentId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(doc.getContentType()))
                .body(new InputStreamResource(documentStream));
    }

    @GetMapping("/search")
    @Operation(summary = "Search documents by metadata")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<DocumentMetadata>> searchDocuments(@RequestParam Map<String, String> searchCriteria) {
        String userId = userContext.getCurrentUserId();
        log.info("User {} searching documents with criteria: {}", userId, searchCriteria);

        List<DocumentMetadata> results = s3ServiceImpl.searchByMetadata(searchCriteria);

        // Filter results based on user access
        List<DocumentMetadata> filteredResults = results.stream()
                .filter(doc -> hasAccessToDocument(doc, userId))
                .toList();

        return ResponseEntity.ok(filteredResults);
    }

    private boolean hasAccessToDocument(DocumentMetadata doc, String userId) {
        // Simple access control - user can access their own documents
        // In production, implement proper RBAC
        return doc.getCreatedBy().equals(userId) || hasAdminRole();
    }

    private boolean hasAdminRole() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    // Response DTOs
    public record BulkUploadResponse(int successCount, int failureCount, String message) {}
    public record DocumentUploadResponse(HttpStatus status, String message){}
}
