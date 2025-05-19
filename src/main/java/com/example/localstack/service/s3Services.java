package com.example.localstack.service;

import java.io.IOException;
import java.io.InputStream;

public interface s3Services {
    void upload(String bucketName, String key, InputStream inputStream);
    InputStream download(String bucketName, String key) throws IOException;
    String downloadAsString(String bucketName, String key) throws IOException;
}
