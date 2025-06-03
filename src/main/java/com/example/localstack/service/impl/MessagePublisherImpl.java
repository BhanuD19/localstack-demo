package com.example.localstack.service.impl;

import com.example.localstack.data.messageEntities.Message;
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
     * Publishes a message to the specified SQS queue.
     * Logs the attempt to publish and confirms success upon completion.
     *
     * @param queueName The name of the SQS queue to which the message will be published.
     * @param message The message object containing the content and unique identifier to be published.
     */
    @Override
    public void publish(String queueName, Message message) {
        log.info("Publishing message to queue: {}", queueName);
        sqsTemplate.send(to -> to.queue(queueName).payload(message));
        log.info("Message published successfully");
    }
}
