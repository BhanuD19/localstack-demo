package com.example.localstack.service.impl;

import com.example.localstack.config.AwsConfig;
import com.example.localstack.data.messageEntities.Message;
import com.example.localstack.service.MessageListener;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
@Service
public class MessageListenerImpl implements MessageListener {
    private final AwsConfig awsConfig;
    private final S3ServiceImpl s3ServiceImpl;

    public MessageListenerImpl(AwsConfig awsConfig, S3ServiceImpl s3ServiceImpl) {
        this.awsConfig = awsConfig;
        this.s3ServiceImpl = s3ServiceImpl;
    }

    /**
     * Handles an incoming SQS message, processes it, and uploads its content to an S3 bucket.
     *
     * @param message The SQS message to process, containing a unique identifier and content.
     */
    @SqsListener(queueNames = "${app.queue-name}")
    @Override
    public void handle(Message message) {
        log.debug("Message received: {}", message);
        var bucketName = awsConfig.bucketName();
        log.info("Uploading message to S3 bucket: {}", bucketName);
        var key = message.uuid().toString();
        var inputStream = new ByteArrayInputStream(message.content().getBytes(UTF_8));
        s3ServiceImpl.upload(bucketName, key, inputStream);
        log.info("{} Message uploaded successfully", key);
    }
}
