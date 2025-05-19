package com.example.localstack.service;

import com.example.localstack.data.Message;

public interface MessageListener {
    void handle(Message message);
}
