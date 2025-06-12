package com.example.localstack.data;

import com.example.localstack.data.dbEntities.DocumentMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@Slf4j
public class DocumentMetadataRepository {
    private final DynamoDbTable<DocumentMetadata> table;

    public DocumentMetadataRepository(DynamoDbEnhancedClient client, @Value("${app.dynamodb.table-name}") String tableName) {
        table = client.table(tableName, TableSchema.fromBean(DocumentMetadata.class));
    }

    public DocumentMetadata save(DocumentMetadata documentMetadata) {
        log.info("Saving document metadata: {}", documentMetadata);
        table.putItem(documentMetadata);
        return documentMetadata;
    }

    public Optional<DocumentMetadata> findById(String documentId) {
        log.info("Finding document by ID : {}", documentId);
        DocumentMetadata search = table.getItem(r -> r.key(k -> k.partitionValue(documentId)));
        return Optional.ofNullable(search);
    }

    public List<DocumentMetadata> searchByMetadata(Map<String, String> searchData) {
        log.info("Searching documents by metadata: {}", searchData);
        if (searchData.isEmpty()) {
            return table.scan().items().stream().toList();
        }

        StringBuilder filterExp = new StringBuilder();
        Map<String, AttributeValue> attributeValueMap = new HashMap<>();
        int index = 0;
        for (Map.Entry<String, String> entry : searchData.entrySet()) {
            if (index > 0) {
                filterExp.append(" AND ");
            }
            String valuePlaceHolder = ":metadataValue".concat(Integer.toString(index));
            filterExp.append("contains(metadata. ").append(entry.getKey()).append(", ").append(valuePlaceHolder).append(")");
            attributeValueMap.put(valuePlaceHolder, AttributeValue.builder().s(entry.getValue()).build());
            index++;
        }

        Expression expression = Expression.builder()
                .expression(filterExp.toString())
                .expressionValues(attributeValueMap)
                .build();
        ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
                .filterExpression(expression)
                .build();

        return table.scan(scanRequest).items().stream().toList();
    }

    public List<DocumentMetadata> findByPath(String path) {
        log.info("Searching documents by path prefix : {}", path);
        Expression expression = Expression.builder()
                .expression("begins_with(path, :path)")
                .expressionValues(Map.of(":path", AttributeValue.builder().s(path).build()))
                .build();
        ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
                .filterExpression(expression)
                .build();
        return table.scan(scanRequest).items().stream().toList();
    }

    public List<DocumentMetadata> findByCreatedBy(String userId) {
        log.info("Searching documents created by user: {}", userId);

        Expression expression = Expression.builder()
                .expression("createdBy = :userId")
                .expressionValues(Map.of(":userId", AttributeValue.builder().s(userId).build()))
                .build();
        ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
                .filterExpression(expression)
                .build();
        return table.scan(scanRequest).items().stream().toList();
    }

    public void deleteById(String documentId) {
        log.info("Deleting document by ID : {}", documentId);
        table.deleteItem(r -> r.key(k -> k.partitionValue(documentId)));
    }
}
