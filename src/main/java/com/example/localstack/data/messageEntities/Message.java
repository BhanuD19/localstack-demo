package com.example.localstack.data.messageEntities;

import java.util.UUID;

public record Message(UUID uuid, String content) {
}
