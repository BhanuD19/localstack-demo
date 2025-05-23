package com.example.localstack.service;

import com.example.localstack.data.messageEntities.Message;

public interface MessageListener {
    void handle(Message message);
}
