package com.financialadvisor.profile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.*;

/**
 * DynamoDB repository for profile snapshots.
 *
 * Single-table design (per design doc §5.3):
 *   PK = USER#<userId>
 *   SK = SNAPSHOT#<snapshotId>
 *
 * GSI "LatestSnapshotIndex": userId (HASH), isLatest (RANGE)
 */
@Repository
public class ProfileRepository {

    private static final Logger log = LoggerFactory.getLogger(ProfileRepository.class);
    private static final String PK_PREFIX = "USER#";
    private static final String SK_PREFIX = "SNAPSHOT#";

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;
    private final ObjectMapper objectMapper;

    public ProfileRepository(DynamoDbClient dynamoDbClient,
                             @Value("${aws.dynamodb.table-name}") String tableName,
                             ObjectMapper objectMapper) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
        this.objectMapper = objectMapper;
    }

    /**
     * Saves a profile snapshot to DynamoDB.
     */
    public void save(ProfileSnapshot snapshot) {
        Map<String, AttributeValue> item = toItem(snapshot);
        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build());

        log.info("Saved snapshot {} for user {}", snapshot.snapshotId(), snapshot.userId());
    }

    /**
     * Gets the latest snapshot for a user.
     * Uses a query on the main table with isLatest filter.
     */
    public Optional<ProfileSnapshot> findLatest(String userId) {
        QueryResponse response = dynamoDbClient.query(QueryRequest.builder()
                .tableName(tableName)
                .indexName("LatestSnapshotIndex")
                .keyConditionExpression("userId = :uid AND isLatest = :latest")
                .expressionAttributeValues(Map.of(
                        ":uid", AttributeValue.builder().s(userId).build(),
                        ":latest", AttributeValue.builder().s("true").build()
                ))
                .limit(1)
                .build());

        if (response.items().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(fromItem(response.items().getFirst()));
    }

    /**
     * Gets all snapshots for a user, ordered chronologically (oldest first).
     */
    public List<ProfileSnapshot> findHistory(String userId) {
        QueryResponse response = dynamoDbClient.query(QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("PK = :pk AND begins_with(SK, :skPrefix)")
                .expressionAttributeValues(Map.of(
                        ":pk", AttributeValue.builder().s(PK_PREFIX + userId).build(),
                        ":skPrefix", AttributeValue.builder().s(SK_PREFIX).build()
                ))
                .scanIndexForward(true)
                .build());

        return response.items().stream()
                .map(this::fromItem)
                .toList();
    }

    /**
     * Marks an existing snapshot as not-latest by updating the isLatest attribute.
     */
    public void markNotLatest(String userId, String snapshotId) {
        dynamoDbClient.updateItem(UpdateItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(
                        "PK", AttributeValue.builder().s(PK_PREFIX + userId).build(),
                        "SK", AttributeValue.builder().s(SK_PREFIX + snapshotId).build()
                ))
                .updateExpression("SET isLatest = :notLatest")
                .expressionAttributeValues(Map.of(
                        ":notLatest", AttributeValue.builder().s("false").build()
                ))
                .build());
    }

    private Map<String, AttributeValue> toItem(ProfileSnapshot snapshot) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("PK", AttributeValue.builder().s(PK_PREFIX + snapshot.userId()).build());
        item.put("SK", AttributeValue.builder().s(SK_PREFIX + snapshot.snapshotId()).build());
        item.put("userId", AttributeValue.builder().s(snapshot.userId()).build());
        item.put("isLatest", AttributeValue.builder()
                .s(snapshot.isLatest() ? "true" : "false").build());
        item.put("snapshotId", AttributeValue.builder().s(snapshot.snapshotId()).build());
        item.put("snapshotDate", AttributeValue.builder()
                .s(snapshot.snapshotDate().toString()).build());

        String dataJson = serializeData(snapshot.data());
        item.put("data", AttributeValue.builder().s(dataJson).build());

        if (snapshot.encryptedFields() != null && !snapshot.encryptedFields().isEmpty()) {
            item.put("encryptedFields", AttributeValue.builder()
                    .ss(snapshot.encryptedFields()).build());
        }

        return item;
    }

    private ProfileSnapshot fromItem(Map<String, AttributeValue> item) {
        String userId = getStringAttr(item, "userId");
        String snapshotId = getStringAttr(item, "snapshotId");
        Instant snapshotDate = Instant.parse(getStringAttr(item, "snapshotDate"));
        boolean isLatest = "true".equals(getStringAttr(item, "isLatest"));
        Map<String, Object> data = deserializeData(getStringAttr(item, "data"));

        List<String> encryptedFields = List.of();
        if (item.containsKey("encryptedFields") && item.get("encryptedFields").ss() != null) {
            encryptedFields = item.get("encryptedFields").ss();
        }

        return new ProfileSnapshot(userId, snapshotId, snapshotDate, isLatest, data, encryptedFields);
    }

    private String getStringAttr(Map<String, AttributeValue> item, String key) {
        AttributeValue val = item.get(key);
        return (val != null) ? val.s() : null;
    }

    private String serializeData(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize profile data", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deserializeData(String json) {
        try {
            if (json == null || json.isBlank()) {
                return new LinkedHashMap<>();
            }
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize profile data", e);
        }
    }
}
