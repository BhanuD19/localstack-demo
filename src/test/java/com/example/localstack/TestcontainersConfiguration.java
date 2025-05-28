package com.example.localstack;

import com.example.localstack.config.AwsConfig;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsAsyncClient;

import java.io.IOException;
import java.net.URI;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {
    @Autowired
    AwsConfig awsConfig;
    @Value("${spring.cloud.aws.kms.region}")
    private String awsRegion;

    @Getter
    private static String dynamicKmsKeyId;

    @Bean
    @ServiceConnection
    LocalStackContainer localStackContainer() {
        try {
            LocalStackContainer localStackContainer = new LocalStackContainer(DockerImageName.parse("localstack/localstack:4.3.0"));
            localStackContainer.start();
            localStackContainer.execInContainer("awslocal", "s3", "mb", "s3://" + awsConfig.bucketName());
            localStackContainer.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", awsConfig.queueName());
            String kmsKeyDetails = localStackContainer.execInContainer("awslocal", "kms", "create-key", "--description", "test key", "--region", awsRegion).getStdout();
            String keyId = extractKeyId(kmsKeyDetails);
            dynamicKmsKeyId = keyId;
            localStackContainer.execInContainer("awslocal", "kms", "create-alias", "--alias-name", awsConfig.kmsKeyId(), "--target-key-id", extractKeyId(kmsKeyDetails), "--region", awsRegion);
            System.setProperty("app.kms.key.id", keyId);
            return localStackContainer;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    @Primary
    public KmsAsyncClient kmsAsyncClient(LocalStackContainer localStackContainer) {
        return KmsAsyncClient.builder().endpointOverride(URI.create(localStackContainer.getEndpoint().toString()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localStackContainer.getAccessKey(), localStackContainer.getSecretKey())))
                .region(Region.of(awsRegion))
                .build();
    }

    @Bean
    @Primary
    public AwsConfig testAwsConfig() {
        return new AwsConfig(awsConfig.queueName(), awsConfig.bucketName(), dynamicKmsKeyId);
    }

    private String extractKeyId(String keyOutput) {
        String id = keyOutput.replaceAll("(?s).*\"KeyId\":\\s*\"([^\"]+)\".*\n", "$1");
        return id.trim();
    }
}
