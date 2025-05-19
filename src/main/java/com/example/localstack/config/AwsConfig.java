package com.example.localstack.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app")
public record AwsConfig(
        @DefaultValue("testqueue") String queueName,
        @DefaultValue("testbucket") String bucketName) {}
