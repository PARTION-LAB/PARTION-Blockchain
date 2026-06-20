package com.partion.blockchain.ledger;

import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

@Service
public class HashService {

    public static final String GENESIS_PREVIOUS_HASH =
            "0x0000000000000000000000000000000000000000000000000000000000000000";

    private final ObjectMapper objectMapper;

    public HashService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String canonicalPayload(LedgerEvent event) {
        Map<String, Object> payload = new TreeMap<>();
        payload.put("amount", normalize(event.amount()));
        payload.put("buyOrderId", event.buyOrderId());
        payload.put("eventId", event.eventId());
        payload.put("eventType", event.eventType());
        payload.put("occurredAt", event.occurredAt());
        payload.put("price", normalize(event.price()));
        payload.put("productCategory", event.productCategory());
        payload.put("productId", event.productId());
        payload.put("productName", event.productName());
        payload.put("quantity", event.quantity());
        payload.put("referenceId", event.referenceId());
        payload.put("referenceType", event.referenceType());
        payload.put("sellOrderId", event.sellOrderId());

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to serialize ledger payload.", exception);
        }
    }

    public String payloadHash(String canonicalPayload) {
        return sha256Hex("PARTION_PAYLOAD|" + canonicalPayload);
    }

    public String transactionHash(LedgerEvent event, String payloadHash) {
        return sha256Hex("PARTION_TX|"
                + event.eventType() + "|"
                + event.referenceType() + "|"
                + event.referenceId() + "|"
                + payloadHash);
    }

    public String blockHash(
            long blockNumber,
            String previousHash,
            String merkleRoot,
            String createdAt
    ) {
        return sha256Hex("PARTION_BLOCK|"
                + blockNumber + "|"
                + previousHash + "|"
                + merkleRoot + "|"
                + createdAt);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder("0x");
            for (byte item : bytes) {
                hex.append(String.format("%02x", item));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private String normalize(BigDecimal value) {
        if (value == null) {
            return null;
        }

        return value.stripTrailingZeros().toPlainString();
    }
}
