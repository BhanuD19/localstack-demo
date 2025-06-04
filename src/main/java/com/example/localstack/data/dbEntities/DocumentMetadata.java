package com.example.localstack.data.dbEntities;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class DocumentMetadata {
    
    @Getter(onMethod_ = @DynamoDbPartitionKey)
    private String documentId;
    
    @Getter(onMethod_ = @DynamoDbAttribute("fileName"))
    private String fileName;
    
    @Getter(onMethod_ = @DynamoDbAttribute("filePath"))
    private String filePath;
    
    @Getter(onMethod_ = @DynamoDbAttribute("contentType"))
    private String contentType;
    
    @Getter(onMethod_ = @DynamoDbAttribute("fileSize"))
    private Long fileSize;
    
    @Getter(onMethod_ = @DynamoDbAttribute("version"))
    private String version;
    
    @Getter(onMethod_ = @DynamoDbAttribute("createdAt"))
    private Instant createdAt;
    
    @Getter(onMethod_ = @DynamoDbAttribute("updatedAt"))
    private Instant updatedAt;
    
    @Getter(onMethod_ = @DynamoDbAttribute("createdBy"))
    private String createdBy;
    
    @Getter(onMethod_ = @DynamoDbAttribute("lastModifiedBy"))
    private String lastModifiedBy;
    
    @Getter(onMethod_ = @DynamoDbAttribute("s3Key"))
    private String s3Key;
    
    @Getter(onMethod_ = @DynamoDbAttribute("s3Bucket"))
    private String s3Bucket;
    
    @Getter(onMethod_ = @DynamoDbAttribute("metadata"))
    private Map<String, String> metadata;
    
    @Getter(onMethod_ = @DynamoDbAttribute("tags"))
    private Map<String, String> tags;
    
    @Getter(onMethod_ = @DynamoDbAttribute("isEncrypted"))
    private Boolean isEncrypted;

    @Getter(onMethod_ = @DynamoDbAttribute("kmsKeyId"))
    private String kmsKeyId;
}