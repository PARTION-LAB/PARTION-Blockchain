package com.partion.blockchain.ledger;

import java.time.LocalDateTime;

public record BlockRecord(
        Long id,
        Long blockNumber,
        String previousHash,
        String merkleRoot,
        String currentHash,
        LocalDateTime createdAt
) {
}
