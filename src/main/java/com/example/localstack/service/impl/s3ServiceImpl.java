package com.example.localstack.service.impl;

import com.example.localstack.config.AwsConfig;
import com.example.localstack.service.S3Services;
import io.awspring.cloud.s3.S3Template;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsAsyncClient;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import software.amazon.awssdk.services.kms.model.EncryptRequest;
import software.amazon.awssdk.services.kms.model.EncryptResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


@Slf4j
@Service
public class S3ServiceImpl implements S3Services {
    private final S3Template s3Template;
    private final KmsAsyncClient kmsAsyncClient;
    private final AwsConfig awsConfig;

    public S3ServiceImpl(S3Template s3Template, KmsAsyncClient kmsAsyncClient, AwsConfig awsConfig) {
        this.s3Template = s3Template;
        this.kmsAsyncClient = kmsAsyncClient;
        this.awsConfig = awsConfig;
    }

    /**
     * @param bucketName - S3 Bucket to upload to
     * @param key - Name of the resource on s3
     * @param inputStream - Stream of the input data
     */
    @Override
    public void upload(String bucketName, String key, InputStream inputStream) {
        log.info("Uploading file to S3 bucket: {}", bucketName);
        try {
            s3Template.createBucket(bucketName);
            InputStream encryptedContent = encryptContent(inputStream);
            s3Template.upload(bucketName, key, encryptedContent);
            log.info("File uploaded successfully with key: {}", key);
        } catch (Exception e) {
            log.error("Error uploading file to S3 bucket: {}", bucketName);
            throw new RuntimeException("Failed to upload file to S3 bucket: " + bucketName + " with key: " + key + " due to: " + e.getMessage() + "");
        }
    }

    /**
     * @param bucketName - S3 bucket to download from
     * @param key - Name of the resource to download
     * @return - InputStream of the data stored
     */
    @Override
    public InputStream download(String bucketName, String key) throws IOException {
        log.info("Downloading file from S3 bucket: {}", bucketName);
        InputStream encryptedContent = s3Template.download(bucketName, key).getInputStream();
        return decryptContent(encryptedContent);
    }

    private InputStream decryptContent(InputStream encryptedContent) {
        try {
            byte[] content = encryptedContent.readAllBytes();
            DecryptRequest decryptRequest = DecryptRequest.builder().ciphertextBlob(SdkBytes.fromByteArray(content)).build();
            DecryptResponse decryptResponse = kmsAsyncClient.decrypt(decryptRequest).get(5, TimeUnit.SECONDS);
            log.info("Decrypted content using kmskey: {}", awsConfig.kmsKeyId());
            return decryptResponse.plaintext().asInputStream();
        } catch (ExecutionException | InterruptedException | TimeoutException | IOException e) {
            log.error("Error decrypting content :", e);
            throw new RuntimeException("failed to decrypt content", e);
        }
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

    private InputStream encryptContent(InputStream content) {
        try {
            byte[] contentBytes = content.readAllBytes();
            EncryptRequest encryptRequest = EncryptRequest.builder().keyId(awsConfig.kmsKeyId()).plaintext(SdkBytes.fromByteArray(contentBytes)).build();
        EncryptResponse encryptResponse;

            encryptResponse = kmsAsyncClient.encrypt(encryptRequest).get(5, TimeUnit.SECONDS);
        log.info("Encrypted content using kmskey: {}", awsConfig.kmsKeyId());
        return encryptResponse.ciphertextBlob().asInputStream();
        } catch (InterruptedException | ExecutionException | TimeoutException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
