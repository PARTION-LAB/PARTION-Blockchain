package com.partion.blockchain.ledger;

import java.time.LocalDateTime;

public record LedgerTransactionRecord(
        Long id,
        Long blockId,
        String transactionHash,
        String payloadHash,
        String eventType,
        String referenceType,
        Long referenceId,
        String payload,
        LocalDateTime createdAt
) {
}
