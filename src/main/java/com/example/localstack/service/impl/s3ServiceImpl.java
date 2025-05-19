package com.example.localstack.service.impl;

import com.example.localstack.service.s3Services;
import io.awspring.cloud.s3.S3Template;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
public class s3ServiceImpl implements s3Services {
    private final S3Template s3Template;

    public s3ServiceImpl(S3Template s3Template) {
        this.s3Template = s3Template;
    }

    /**
     * @param bucketName - S3 Bucket to upload to
     * @param key - Name of the resource on s3
     * @param inputStream - Stream of the input data
     */
    @Override
    public void upload(String bucketName, String key, InputStream inputStream) {
        log.info("Uploading file to S3 bucket: {}", bucketName);
        s3Template.createBucket(bucketName);
        s3Template.upload(bucketName, key, inputStream);
        log.info("File uploaded successfully");
    }

    /**
     * @param bucketName - S3 bucket to download from
     * @param key - Name of the resource to download
     * @return - InputStream of the data stored
     */
    @Override
    public InputStream download(String bucketName, String key) throws IOException {
        log.info("Downloading file from S3 bucket: {}", bucketName);
        return s3Template.download(bucketName, key).getInputStream();
    }

    /**
     * @param bucketName - S3 bucket to download from
     * @param key - Name of the resource to download
     * @return - String representation of the data stored
     */
    @Override
    public String downloadAsString(String bucketName, String key) throws IOException {
        try (InputStream is = download(bucketName, key)) {
            return new String(is.readAllBytes());
        }
    }
}
