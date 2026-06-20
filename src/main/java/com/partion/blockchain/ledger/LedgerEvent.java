package com.partion.blockchain.ledger;

import java.math.BigDecimal;

public record LedgerEvent(
        String eventId,
        String eventType,
        String referenceType,
        Long referenceId,
        Long productId,
        String productName,
        String productCategory,
        Long buyOrderId,
        Long sellOrderId,
        BigDecimal price,
        Long quantity,
        BigDecimal amount,
        Long occurredAt
) {
}
