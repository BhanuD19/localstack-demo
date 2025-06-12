package com.example.localstack.service.impl;

import com.example.localstack.data.enums.CompressionLevel;
import com.example.localstack.service.CompressionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

@Service
@Slf4j
public class CompressionServiceImpl implements CompressionService {
    private static final int COMPRESSION_THRESHOLD = 1024; // 1KB minimum for compression
    private static final double MIN_COMPRESSION_RATIO = 0.9; // Only keep if compressed to 90% or less

    @Override
    public CompressedData compress(byte[] data, CompressionLevel level) {
        if (data.length < COMPRESSION_THRESHOLD) {
            log.debug("File too small for compression, skipping");
            return new CompressedData(data, 1.0, false);
        }

        try {
            byte[] compressedData = performCompression(data, level);
            double compressionRatio = (double) compressedData.length / data.length;

            // Only use compressed data if it provides meaningful savings
            if (compressionRatio < MIN_COMPRESSION_RATIO) {
                log.info("Compression successful: {} bytes -> {} bytes (ratio: {})",
                        data.length, compressedData.length, compressionRatio);
                return new CompressedData(compressedData, compressionRatio, true);
            } else {
                log.info("Compression not beneficial (ratio: {}), using original", compressionRatio);
                return new CompressedData(data, 1.0, false);
            }

        } catch (IOException e) {
            log.warn("Compression failed, using original data", e);
            return new CompressedData(data, 1.0, false);
        }
    }

    private byte[] performCompression(byte[] data, CompressionLevel level) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DeflaterOutputStream dos = createCompressor(baos, level)) {

            dos.write(data);
            dos.finish();

            return baos.toByteArray();
        }
    }

    private DeflaterOutputStream createCompressor(ByteArrayOutputStream baos, CompressionLevel level)
            throws IOException {

        return switch (level) {
            case FAST -> new DeflaterOutputStream(baos, new Deflater(Deflater.BEST_SPEED));
            case MAXIMUM -> new GZIPOutputStream(baos); // GZIP for maximum compression
            default -> new DeflaterOutputStream(baos, new Deflater(Deflater.DEFAULT_COMPRESSION));
        };
    }

    @Override
    public byte[] decompress(byte[] compressedData, boolean isGzipped) throws IOException {
        if (isGzipped) {
            return decompressGzip(compressedData);
        } else {
            return decompressDeflate(compressedData);
        }
    }

    private byte[] decompressGzip(byte[] compressedData) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             java.util.zip.GZIPInputStream gis = new java.util.zip.GZIPInputStream(
                     new java.io.ByteArrayInputStream(compressedData))) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = gis.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }

            return baos.toByteArray();
        }
    }

    private byte[] decompressDeflate(byte[] compressedData) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             java.util.zip.InflaterOutputStream ios = new java.util.zip.InflaterOutputStream(baos)) {

            ios.write(compressedData);
            ios.finish();

            return baos.toByteArray();
        }
    }

    public record CompressedData(byte[] data, double compressionRatio, boolean wasCompressed) {}

}
