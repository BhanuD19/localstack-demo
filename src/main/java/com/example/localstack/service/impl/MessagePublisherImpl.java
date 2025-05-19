package com.example.localstack.service.impl;

import com.example.localstack.data.Message;
import com.example.localstack.service.MessagePublisher;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MessagePublisherImpl implements MessagePublisher {
    private final SqsTemplate sqsTemplate;

    public MessagePublisherImpl(SqsTemplate sqsTemplate) {
        this.sqsTemplate = sqsTemplate;
    }

    /**
     * @param queueName - Queue name to which the message has to be sent
     * @param message - SQS Message model to upload
     */
    @Override
    public void publish(String queueName, Message message) {
        log.info("Publishing message to queue: {}", queueName);
        sqsTemplate.send(to -> to.queue(queueName).payload(message));
        log.info("Message published successfully");
    }
}
