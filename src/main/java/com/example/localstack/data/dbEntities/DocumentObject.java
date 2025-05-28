package com.example.localstack.data.dbEntities;

import java.time.LocalDateTime;
import java.util.Map;

public class DocumentObject {
    private Integer id;
    private String name;
    private String contentType;
    private Long size;
    private String path;
    private String version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private Map<String, String> metadata;
}
