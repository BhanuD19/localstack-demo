package com.example.localstack.config.AWS;

import com.example.localstack.data.dbEntities.DocumentMetadata;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;

@Configuration
@Slf4j
public class DynamoDbConfig {
    @Value("${app.dynamodb.table-name}")
    private String tableName;

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;

    public DynamoDbConfig(DynamoDbEnhancedClient dynamoDbEnhancedClient) {
        this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
    }

    @PostConstruct
    public void createTableIfNotExist() {
        try {
            DynamoDbTable<DocumentMetadata> table = dynamoDbEnhancedClient.table(tableName, TableSchema.fromBean(DocumentMetadata.class));
            table.createTable(CreateTableEnhancedRequest.builder().build());
            log.info("Created dynamoDb table {}", tableName);
        } catch (ResourceInUseException e) {
            log.info("DynamoDb table {} already exists", tableName);
        } catch (Exception e) {
            log.error("Error creating dynamoDb table {} \n {}", tableName, e.getMessage());
        }
    }
}
