package com.example.localstack.service;

import com.example.localstack.data.dbEntities.DocumentMetadata;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface S3Services {
    void upload(String path, MultipartFile inputFile, Map<String, String> metaData, String userId);

    InputStream downloadDocument(String documentId) throws IOException;

    List<DocumentMetadata> searchByMetadata(Map<String, String> searchCriteria);

    List<DocumentMetadata> findByPath(String path);

    List<DocumentMetadata> findByCreatedBy(String userId);

    Optional<DocumentMetadata> getDocumentMetadata(String documentId);
}
