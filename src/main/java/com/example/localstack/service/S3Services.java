package com.example.localstack.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public interface S3Services {
    void upload(String path, MultipartFile inputFile, Map<String, String> metaData, String userId);
    InputStream download(String bucketName, String key) throws IOException;
    String downloadAsString(String bucketName, String key) throws IOException;
}
