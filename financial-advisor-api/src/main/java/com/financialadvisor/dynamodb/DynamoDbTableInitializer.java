package com.financialadvisor.dynamodb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.List;

/**
 * Automatically creates the DynamoDB profile table on application startup (IR-2).
 * Uses single-table design with composite key: PK=USER#userId, SK=SNAPSHOT#snapshotId.
 * Includes GSI for LatestSnapshotIndex.
 */
@Component
public class DynamoDbTableInitializer {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbTableInitializer.class);

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public DynamoDbTableInitializer(DynamoDbClient dynamoDbClient,
                                    @Value("${aws.dynamodb.table-name}") String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeTable() {
        if (tableExists()) {
            log.info("DynamoDB table '{}' already exists", tableName);
            return;
        }

        log.info("Creating DynamoDB table '{}'...", tableName);
        try {
            dynamoDbClient.createTable(CreateTableRequest.builder()
                    .tableName(tableName)
                    .keySchema(
                            KeySchemaElement.builder()
                                    .attributeName("PK")
                                    .keyType(KeyType.HASH)
                                    .build(),
                            KeySchemaElement.builder()
                                    .attributeName("SK")
                                    .keyType(KeyType.RANGE)
                                    .build()
                    )
                    .attributeDefinitions(
                            AttributeDefinition.builder()
                                    .attributeName("PK")
                                    .attributeType(ScalarAttributeType.S)
                                    .build(),
                            AttributeDefinition.builder()
                                    .attributeName("SK")
                                    .attributeType(ScalarAttributeType.S)
                                    .build(),
                            AttributeDefinition.builder()
                                    .attributeName("userId")
                                    .attributeType(ScalarAttributeType.S)
                                    .build(),
                            AttributeDefinition.builder()
                                    .attributeName("isLatest")
                                    .attributeType(ScalarAttributeType.S)
                                    .build()
                    )
                    .globalSecondaryIndexes(
                            GlobalSecondaryIndex.builder()
                                    .indexName("LatestSnapshotIndex")
                                    .keySchema(
                                            KeySchemaElement.builder()
                                                    .attributeName("userId")
                                                    .keyType(KeyType.HASH)
                                                    .build(),
                                            KeySchemaElement.builder()
                                                    .attributeName("isLatest")
                                                    .keyType(KeyType.RANGE)
                                                    .build()
                                    )
                                    .projection(Projection.builder()
                                            .projectionType(ProjectionType.ALL)
                                            .build())
                                    .provisionedThroughput(ProvisionedThroughput.builder()
                                            .readCapacityUnits(5L)
                                            .writeCapacityUnits(5L)
                                            .build())
                                    .build()
                    )
                    .provisionedThroughput(ProvisionedThroughput.builder()
                            .readCapacityUnits(5L)
                            .writeCapacityUnits(5L)
                            .build())
                    .build());

            dynamoDbClient.waiter().waitUntilTableExists(
                    DescribeTableRequest.builder().tableName(tableName).build()
            );

            log.info("DynamoDB table '{}' created successfully with LatestSnapshotIndex GSI", tableName);
        } catch (Exception e) {
            log.error("Failed to create DynamoDB table '{}': {}", tableName, e.getMessage());
        }
    }

    private boolean tableExists() {
        try {
            List<String> tables = dynamoDbClient.listTables().tableNames();
            return tables.contains(tableName);
        } catch (Exception e) {
            log.warn("Cannot check DynamoDB tables: {}", e.getMessage());
            return false;
        }
    }
}
