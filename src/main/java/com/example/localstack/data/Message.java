package com.example.localstack.data;

import java.util.UUID;

public record Message(UUID uuid, String content) {
}
