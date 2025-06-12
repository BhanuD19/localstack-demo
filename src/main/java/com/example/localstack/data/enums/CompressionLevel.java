package com.example.localstack.data.enums;

public enum CompressionLevel {
    FAST(1), BALANCED(6), MAXIMUM(9);

    private final int level;
    CompressionLevel(int level) {
        this.level = level;
    }
    public int getLevel() {
        return level;
    }
}
