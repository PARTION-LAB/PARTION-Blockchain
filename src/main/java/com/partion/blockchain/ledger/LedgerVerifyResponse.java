package com.partion.blockchain.ledger;

public record LedgerVerifyResponse(
        boolean valid,
        long height,
        long checkedBlocks,
        String latestHash,
        String message
) {
}
