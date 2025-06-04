package com.example.localstack.service.impl;

import com.example.localstack.config.AwsConfig;
import com.example.localstack.data.DocumentMetadataRepository;
import com.example.localstack.data.dbEntities.DocumentMetadata;
import com.example.localstack.service.S3Services;
import io.awspring.cloud.s3.S3Template;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsAsyncClient;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import software.amazon.awssdk.services.kms.model.EncryptRequest;
import software.amazon.awssdk.services.kms.model.EncryptResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


@Slf4j
@Service
public class S3ServiceImpl implements S3Services {
    private final S3Template s3Template;
    private final KmsAsyncClient kmsAsyncClient;
    private final AwsConfig awsConfig;
    private final DocumentMetadataRepository documentMetadataRepository;

    public S3ServiceImpl(S3Template s3Template, KmsAsyncClient kmsAsyncClient, AwsConfig awsConfig, DocumentMetadataRepository documentMetadataRepository) {
        this.s3Template = s3Template;
        this.kmsAsyncClient = kmsAsyncClient;
        this.awsConfig = awsConfig;
        this.documentMetadataRepository = documentMetadataRepository;
    }

    /**
     * Uploads a file to an S3 bucket. The content of the file is encrypted
     * before being uploaded. If the bucket does not exist, it will be created.
     * Logs the upload process and handles any exceptions that occur during
     * the operation.
     *
     * @param path      The name of the S3 bucket where the file will be uploaded.
     * @param inputFile The input stream containing the file content to be uploaded.
     * @param metaData  The input stream containing the file content to be uploaded.
     * @param userId    The user id of the user uploading the file.
     */
    @Override
    public void upload(String path, MultipartFile inputFile, Map<String, String> metaData, String userId) {
        String documentId = UUID.randomUUID().toString();
        String s3Key = generateS3Key(path, documentId, inputFile.getOriginalFilename());

        log.info("Uploading file to S3 bucket: {}", path);
        try {
            s3Template.createBucket(awsConfig.bucketName());
            byte[] encryptedContent = encryptContent(inputFile.getInputStream());
            s3Template.upload(awsConfig.bucketName(), s3Key, new ByteArrayInputStream(encryptedContent));
            DocumentMetadata documentMetadata = DocumentMetadata.builder()
                    .documentId(documentId)
                    .fileName(inputFile.getOriginalFilename())
                    .filePath(path)
                    .contentType(inputFile.getContentType())
                    .fileSize(inputFile.getSize())
                    .version("1.0")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .createdBy(userId)
                    .lastModifiedBy(userId)
                    .s3Key(s3Key)
                    .s3Bucket(awsConfig.bucketName())
                    .metadata(metaData)
                    .isEncrypted(true)
                    .kmsKeyId(awsConfig.kmsKeyId())
                    .build();
            documentMetadataRepository.save(documentMetadata);
            log.info("File uploaded successfully with key: {}", documentId);
        } catch (Exception e) {
            log.error("Error uploading file to S3 bucket: {}", awsConfig.bucketName());
            throw new RuntimeException("Failed to upload file to S3 bucket: ".concat(awsConfig.bucketName()).concat( " with key: ").concat(s3Key).concat(" due to: ").concat(e.getMessage()));
        }
    }

    private String generateS3Key(String path, String documentId, String originalFilename) {
        return String.format("%s/%s/%s", path.replaceAll("^/", ""), documentId, originalFilename);
    }

    /**
     * Downloads a file from an S3 bucket, decrypts its content, and returns it as an InputStream.
     *
     * @param bucketName The name of the S3 bucket from which the file will be downloaded.
     * @param key The unique key or identifier of the file to be downloaded within the bucket.
     * @return An InputStream containing the decrypted content of the downloaded file.
     * @throws IOException If an error occurs during the download process.
     */
    @Override
    public InputStream download(String bucketName, String key) throws IOException {
        log.info("Downloading file from S3 bucket: {}", bucketName);
        InputStream encryptedContent = s3Template.download(bucketName, key).getInputStream();
        return decryptContent(encryptedContent);
    }

    /**
     * Decrypts the provided encrypted input stream using AWS KMS and returns the decrypted content as an InputStream.
     * This method reads all bytes from the provided encrypted content, creates a KMS DecryptRequest, and uses the
     * AWS KMS client to decrypt the content. If any errors occur during decryption, a RuntimeException is thrown.
     *
     * @param encryptedContent The InputStream containing the encrypted content to be decrypted.
     * @return An InputStream containing the decrypted content.
     * @throws RuntimeException If an error occurs during the decryption process.
     */
    private InputStream decryptContent(InputStream encryptedContent) {
        try {
            byte[] content = encryptedContent.readAllBytes();
            DecryptRequest decryptRequest = DecryptRequest.builder().ciphertextBlob(SdkBytes.fromByteArray(content)).build();
            DecryptResponse decryptResponse = kmsAsyncClient.decrypt(decryptRequest).get(5, TimeUnit.SECONDS);
            log.debug("Decrypted content using kmskey: {}", awsConfig.kmsKeyId());
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

    /**
     * Encrypts the provided input stream using AWS KMS and returns the encrypted content as an InputStream.
     * This method reads all bytes from the provided content, creates a KMS EncryptRequest, and uses the
     * AWS KMS client to encrypt the content. If any errors occur during encryption, a RuntimeException is thrown.
     *
     * @param content The InputStream containing the data to be encrypted.
     * @return An InputStream containing the encrypted content.
     * @throws RuntimeException If an error occurs during the encryption process.
     */
    private byte[] encryptContent(InputStream content) {
        try {
            byte[] contentBytes = content.readAllBytes();
            EncryptRequest encryptRequest = EncryptRequest.builder().keyId(awsConfig.kmsKeyId()).plaintext(SdkBytes.fromByteArray(contentBytes)).build();
        EncryptResponse encryptResponse;

            encryptResponse = kmsAsyncClient.encrypt(encryptRequest).get(5, TimeUnit.SECONDS);
        log.debug("Encrypted content using kmskey: {}", awsConfig.kmsKeyId());
        return encryptResponse.ciphertextBlob().asByteArray();
        } catch (InterruptedException | ExecutionException | TimeoutException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
