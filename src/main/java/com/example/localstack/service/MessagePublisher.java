package com.example.localstack.service;

import com.example.localstack.data.messageEntities.Message;

public interface MessagePublisher {
    void publish(String queueName, Message message);
}
