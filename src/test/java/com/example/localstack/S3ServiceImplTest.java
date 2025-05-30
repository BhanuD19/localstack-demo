package com.example.localstack;

import com.example.localstack.config.AwsConfig;
import com.example.localstack.service.S3Services;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Import(TestcontainersConfiguration.class)
public class S3ServiceImplTest {
    @Autowired
    private S3Services s3Services;
    @Autowired
    private AwsConfig awsConfig;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // This will use the system property set by TestcontainersConfiguration
        String dynamicKeyId = System.getProperty("app.kms.key.id");
        if (dynamicKeyId != null) {
            registry.add("app.kms.key.id", () -> dynamicKeyId);
        }
    }


    @Test
    void testUploadAndDownloadWithKmsEncryption() throws IOException {
        // Given
        String originalContent = "This is a test file content that should be encrypted using KMS before uploading to S3";
        String testKey = "test-encrypted-file.txt";
        InputStream inputStream = new ByteArrayInputStream(originalContent.getBytes());

        // When - Upload encrypted content
        s3Services.upload(awsConfig.bucketName(), testKey, inputStream);

        // Then - Download and verify content is correctly decrypted
        try (InputStream downloadedContent = s3Services.download(awsConfig.bucketName(), testKey)) {
            assertThat(new String(downloadedContent.readAllBytes())).isEqualTo(originalContent);
        }
    }
}
