package com.example.localstack;

import com.example.localstack.config.AwsConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {
    @Autowired
    AwsConfig awsConfig;

    @Bean
    @ServiceConnection
    LocalStackContainer localStackContainer() {
        try {
            LocalStackContainer localStackContainer = new LocalStackContainer(DockerImageName.parse("localstack/localstack:4.3.0"));
            localStackContainer.start();
            localStackContainer.execInContainer("awslocal", "s3", "mb", "s3://" + awsConfig.bucketName());
            localStackContainer.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", awsConfig.queueName());
            return localStackContainer;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
