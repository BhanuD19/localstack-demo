package com.example.localstack;

import com.example.localstack.config.AwsConfig;
import com.example.localstack.data.DocumentMetadataRepository;
import com.example.localstack.data.dbEntities.DocumentMetadata;
import com.example.localstack.service.impl.S3ServiceImpl;
import io.awspring.cloud.s3.S3Template;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.multipart.MultipartFile;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsAsyncClient;
import software.amazon.awssdk.services.kms.model.EncryptRequest;
import software.amazon.awssdk.services.kms.model.EncryptResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
@Import(TestcontainersConfiguration.class)
@ExtendWith(MockitoExtension.class)
public class S3ServiceImplTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // This will use the system property set by TestcontainersConfiguration
        String dynamicKeyId = System.getProperty("app.kms.key.id");
        if (dynamicKeyId != null) {
            registry.add("app.kms.key.id", () -> dynamicKeyId);
        }
    }

    @Mock
    private S3Template s3Template;

    @Mock
    private KmsAsyncClient kmsAsyncClient;

    @Mock
    private AwsConfig awsConfig;

    @Mock
    private DocumentMetadataRepository documentMetadataRepository;

    @Mock
    private MultipartFile multipartFile;

    private S3ServiceImpl s3Service;

    private static final String TEST_BUCKET_NAME = "test-bucket";
    private static final String TEST_KMS_KEY_ID = "test-kms-key-id";
    private static final String TEST_USER_ID = "test-user-123";
    private static final String TEST_PATH = "documents/test";
    private static final String TEST_FILENAME = "test-file.txt";
    private static final String TEST_CONTENT_TYPE = "text/plain";
    private static final long TEST_FILE_SIZE = 1024L;
    private static final String TEST_FILE_CONTENT = "This is test file content";

    @BeforeEach
    void setUp() {
        s3Service = new S3ServiceImpl(s3Template, kmsAsyncClient, awsConfig, documentMetadataRepository);

        // Set up common mock behaviors
        lenient().when(awsConfig.bucketName()).thenReturn(TEST_BUCKET_NAME);
        lenient().when(awsConfig.kmsKeyId()).thenReturn(TEST_KMS_KEY_ID);
    }

    @Test
    void upload_ShouldSuccessfullyUploadFile_WhenAllDependenciesWorkCorrectly() throws Exception {
        // Given
        Map<String, String> metadata = createTestMetadata();
        setupMultipartFileMock();
        setupSuccessfulKmsEncryption();

        // When
        s3Service.upload(TEST_PATH, multipartFile, metadata, TEST_USER_ID);

        // Then
        verify(s3Template).createBucket(TEST_BUCKET_NAME);
        verify(s3Template).upload(eq(TEST_BUCKET_NAME), anyString(), any(ByteArrayInputStream.class));

        ArgumentCaptor<DocumentMetadata> documentCaptor = ArgumentCaptor.forClass(DocumentMetadata.class);
        verify(documentMetadataRepository).save(documentCaptor.capture());

        DocumentMetadata savedDocument = documentCaptor.getValue();
        assertThat(savedDocument.getFileName()).isEqualTo(TEST_FILENAME);
        assertThat(savedDocument.getFilePath()).isEqualTo(TEST_PATH);
        assertThat(savedDocument.getContentType()).isEqualTo(TEST_CONTENT_TYPE);
        assertThat(savedDocument.getFileSize()).isEqualTo(TEST_FILE_SIZE);
        assertThat(savedDocument.getCreatedBy()).isEqualTo(TEST_USER_ID);
        assertThat(savedDocument.getLastModifiedBy()).isEqualTo(TEST_USER_ID);
        assertThat(savedDocument.getS3Bucket()).isEqualTo(TEST_BUCKET_NAME);
        assertThat(savedDocument.getKmsKeyId()).isEqualTo(TEST_KMS_KEY_ID);
        assertThat(savedDocument.getVersion()).isEqualTo("1.0");
        assertThat(savedDocument.getIsEncrypted()).isTrue();
        assertThat(savedDocument.getMetadata()).isEqualTo(metadata);
        assertThat(savedDocument.getDocumentId()).isNotNull();
        assertThat(savedDocument.getS3Key()).contains(TEST_PATH.replaceAll("^/", ""));
        assertThat(savedDocument.getS3Key()).contains(TEST_FILENAME);
        assertThat(savedDocument.getCreatedAt()).isNotNull();
        assertThat(savedDocument.getUpdatedAt()).isNotNull();
    }

    @Test
    void upload_ShouldThrowRuntimeException_WhenMultipartFileThrowsIOException() throws Exception {
        // Given - Only mock what's needed before the IOException occurs
        lenient().when(multipartFile.getOriginalFilename()).thenReturn(TEST_FILENAME);
        when(multipartFile.getInputStream()).thenThrow(new IOException("File read error"));

        // When & Then
        assertThatThrownBy(() -> s3Service.upload(TEST_PATH, multipartFile, createTestMetadata(), TEST_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to upload file to S3 bucket")
                .hasMessageContaining(TEST_BUCKET_NAME);
    }

    @Test
    void upload_ShouldGenerateCorrectS3Key_WithDifferentPaths() throws Exception {
        // Given
        String pathWithLeadingSlash = "/documents/test";
        setupMultipartFileMock();
        setupSuccessfulKmsEncryption();

        // When
        s3Service.upload(pathWithLeadingSlash, multipartFile, createTestMetadata(), TEST_USER_ID);

        // Then
        ArgumentCaptor<String> s3KeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(s3Template).upload(eq(TEST_BUCKET_NAME), s3KeyCaptor.capture(), any(ByteArrayInputStream.class));

        String capturedS3Key = s3KeyCaptor.getValue();
        assertThat(capturedS3Key).startsWith("documents/test/");
        assertThat(capturedS3Key).endsWith("/" + TEST_FILENAME);
        assertThat(capturedS3Key).doesNotStartWith("/");
    }

    @Test
    void upload_ShouldThrowRuntimeException_WhenKmsEncryptionFails() throws Exception {
        // Given - Only mock what's needed before KMS encryption fails
        when(multipartFile.getOriginalFilename()).thenReturn(TEST_FILENAME);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(TEST_FILE_CONTENT.getBytes()));
        
        CompletableFuture<EncryptResponse> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new ExecutionException("KMS encryption failed", new RuntimeException()));
        when(kmsAsyncClient.encrypt(any(EncryptRequest.class))).thenReturn(failedFuture);

        // When & Then
        assertThatThrownBy(() -> s3Service.upload(TEST_PATH, multipartFile, createTestMetadata(), TEST_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to upload file to S3 bucket")
                .hasMessageContaining(TEST_BUCKET_NAME);
    }

    @Test
    void upload_ShouldThrowRuntimeException_WhenKmsEncryptionTimesOut() throws Exception {
        // Given - Only mock what's needed before KMS encryption times out
        when(multipartFile.getOriginalFilename()).thenReturn(TEST_FILENAME);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(TEST_FILE_CONTENT.getBytes()));
        
        CompletableFuture<EncryptResponse> timeoutFuture = new CompletableFuture<>();
        // Don't complete the future to simulate timeout
        when(kmsAsyncClient.encrypt(any(EncryptRequest.class))).thenReturn(timeoutFuture);

        // When & Then
        assertThatThrownBy(() -> s3Service.upload(TEST_PATH, multipartFile, createTestMetadata(), TEST_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to upload file to S3 bucket")
                .hasMessageContaining(TEST_BUCKET_NAME);
    }

    @Test
    void upload_ShouldThrowRuntimeException_WhenS3TemplateThrowsException() throws Exception {
        // Given
        when(multipartFile.getOriginalFilename()).thenReturn(TEST_FILENAME);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(TEST_FILE_CONTENT.getBytes()));
        setupSuccessfulKmsEncryption();
        doThrow(new RuntimeException("S3 upload failed")).when(s3Template).upload(anyString(), anyString(), any(InputStream.class));

        // When & Then
        assertThatThrownBy(() -> s3Service.upload(TEST_PATH, multipartFile, createTestMetadata(), TEST_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to upload file to S3 bucket")
                .hasMessageContaining(TEST_BUCKET_NAME);
    }

    @Test
    void upload_ShouldThrowRuntimeException_WhenDocumentMetadataRepositoryThrowsException() throws Exception {
        // Given
        setupMultipartFileMock();
        setupSuccessfulKmsEncryption();
        when(documentMetadataRepository.save(any(DocumentMetadata.class)))
                .thenThrow(new RuntimeException("Database save failed"));

        // When & Then
        assertThatThrownBy(() -> s3Service.upload(TEST_PATH, multipartFile, createTestMetadata(), TEST_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to upload file to S3 bucket")
                .hasMessageContaining(TEST_BUCKET_NAME);
    }

    @Test
    void upload_ShouldUseCorrectKmsKeyAndParameters_WhenEncrypting() throws Exception {
        // Given
        setupMultipartFileMock();
        setupSuccessfulKmsEncryption();

        // When
        s3Service.upload(TEST_PATH, multipartFile, createTestMetadata(), TEST_USER_ID);

        // Then
        ArgumentCaptor<EncryptRequest> encryptRequestCaptor = ArgumentCaptor.forClass(EncryptRequest.class);
        verify(kmsAsyncClient).encrypt(encryptRequestCaptor.capture());

        EncryptRequest capturedRequest = encryptRequestCaptor.getValue();
        assertThat(capturedRequest.keyId()).isEqualTo(TEST_KMS_KEY_ID);
        assertThat(capturedRequest.plaintext().asUtf8String()).isEqualTo(TEST_FILE_CONTENT);
    }

    @Test
    void upload_ShouldHandleEmptyMetadata() throws Exception {
        // Given
        setupMultipartFileMock();
        setupSuccessfulKmsEncryption();
        Map<String, String> emptyMetadata = new HashMap<>();

        // When
        s3Service.upload(TEST_PATH, multipartFile, emptyMetadata, TEST_USER_ID);

        // Then
        ArgumentCaptor<DocumentMetadata> documentCaptor = ArgumentCaptor.forClass(DocumentMetadata.class);
        verify(documentMetadataRepository).save(documentCaptor.capture());

        DocumentMetadata savedDocument = documentCaptor.getValue();
        assertThat(savedDocument.getMetadata()).isEqualTo(emptyMetadata);
    }

    @Test
    void upload_ShouldHandleNullMetadata() throws Exception {
        // Given
        setupMultipartFileMock();
        setupSuccessfulKmsEncryption();

        // When
        s3Service.upload(TEST_PATH, multipartFile, null, TEST_USER_ID);

        // Then
        ArgumentCaptor<DocumentMetadata> documentCaptor = ArgumentCaptor.forClass(DocumentMetadata.class);
        verify(documentMetadataRepository).save(documentCaptor.capture());

        DocumentMetadata savedDocument = documentCaptor.getValue();
        assertThat(savedDocument.getMetadata()).isNull();
    }

    @Test
    void upload_ShouldCreateBucketBeforeUpload() throws Exception {
        // Given
        setupMultipartFileMock();
        setupSuccessfulKmsEncryption();

        // When
        s3Service.upload(TEST_PATH, multipartFile, createTestMetadata(), TEST_USER_ID);

        // Then
        verify(s3Template).createBucket(TEST_BUCKET_NAME);
    }

    private void setupMultipartFileMock() throws IOException {
        when(multipartFile.getOriginalFilename()).thenReturn(TEST_FILENAME);
        when(multipartFile.getContentType()).thenReturn(TEST_CONTENT_TYPE);
        when(multipartFile.getSize()).thenReturn(TEST_FILE_SIZE);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(TEST_FILE_CONTENT.getBytes()));
    }

    private void setupSuccessfulKmsEncryption() throws InterruptedException, ExecutionException, TimeoutException {
        EncryptResponse encryptResponse = mock(EncryptResponse.class);
        SdkBytes encryptedBytes = SdkBytes.fromByteArray("encrypted-content".getBytes());
        when(encryptResponse.ciphertextBlob()).thenReturn(encryptedBytes);

        CompletableFuture<EncryptResponse> successfulFuture = CompletableFuture.completedFuture(encryptResponse);
        when(kmsAsyncClient.encrypt(any(EncryptRequest.class))).thenReturn(successfulFuture);
    }

    private Map<String, String> createTestMetadata() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("author", "test-author");
        metadata.put("department", "engineering");
        metadata.put("project", "test-project");
        return metadata;
    }


//    @Test
//    void testUploadAndDownloadWithKmsEncryption() throws IOException {
//        // Given
//        String originalContent = "This is a test file content that should be encrypted using KMS before uploading to S3";
//        String testKey = "test-encrypted-file.txt";
//        InputStream inputStream = new ByteArrayInputStream(originalContent.getBytes());
//
//        // When - Upload encrypted content
//        s3Services.upload(awsConfig.bucketName(), testKey, inputStream, metadata);
//
//        // Then - Download and verify content is correctly decrypted
//        try (InputStream downloadedContent = s3Services.download(awsConfig.bucketName(), testKey)) {
//            assertThat(new String(downloadedContent.readAllBytes())).isEqualTo(originalContent);
//        }
//    }


}