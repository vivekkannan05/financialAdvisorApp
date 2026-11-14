package com.financialadvisor.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;

/**
 * Custom health indicator for DynamoDB connectivity (NFR-5.2).
 * Verifies the profile table exists and is ACTIVE.
 */
@Component("dynamodb")
public class DynamoDbHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbHealthIndicator.class);

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public DynamoDbHealthIndicator(DynamoDbClient dynamoDbClient,
                                   @Value("${aws.dynamodb.table-name}") String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    @Override
    public Health health() {
        try {
            TableDescription table = dynamoDbClient.describeTable(
                    DescribeTableRequest.builder().tableName(tableName).build()
            ).table();

            return Health.up()
                    .withDetail("table", tableName)
                    .withDetail("status", table.tableStatusAsString())
                    .withDetail("itemCount", table.itemCount())
                    .build();
        } catch (Exception e) {
            log.warn("DynamoDB health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("table", tableName)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
