package com.example.localstack.service;

import com.example.localstack.data.enums.CompressionLevel;
import com.example.localstack.service.impl.CompressionServiceImpl;

import java.io.IOException;

public interface CompressionService {
    CompressionServiceImpl.CompressedData compress(byte[] data, CompressionLevel level);

    byte[] decompress(byte[] compressedData, boolean isGzipped) throws IOException;
}
