package com.example.localstack.service;

import com.example.localstack.data.Message;

public interface MessagePublisher {
    void publish(String queueName, Message message);
}
