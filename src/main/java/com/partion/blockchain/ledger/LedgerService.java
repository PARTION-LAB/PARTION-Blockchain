package com.partion.blockchain.ledger;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerRepository ledgerRepository;
    private final HashService hashService;

    @KafkaListener(topics = "${partion.blockchain.topic}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consume(LedgerEvent event) {
        append(event);
    }

    public List<BlockRecord> getBlocks() {
        return ledgerRepository.findBlocks();
    }

    public List<LedgerTransactionRecord> getRecentTransactions(int limit) {
        return ledgerRepository.findRecentTransactions(Math.min(Math.max(1, limit), 100));
    }

    public LedgerVerifyResponse verify() {
        List<BlockRecord> blocks = ledgerRepository.findBlocks();
        if (blocks.isEmpty()) {
            return new LedgerVerifyResponse(true, 0, 0, "", "Ledger is empty.");
        }

        String expectedPreviousHash = HashService.GENESIS_PREVIOUS_HASH;
        long expectedBlockNumber = 0;
        for (BlockRecord block : blocks) {
            if (block.blockNumber() != expectedBlockNumber) {
                return invalid(blocks, "Block number sequence is broken.");
            }

            if (!expectedPreviousHash.equals(block.previousHash())) {
                return invalid(blocks, "Previous hash chain is broken.");
            }

            expectedPreviousHash = block.currentHash();
            expectedBlockNumber += 1;
        }

        BlockRecord latest = blocks.get(blocks.size() - 1);
        return new LedgerVerifyResponse(
                true,
                latest.blockNumber(),
                blocks.size(),
                latest.currentHash(),
                "Ledger hash chain is valid."
        );
    }

    private LedgerVerifyResponse invalid(List<BlockRecord> blocks, String message) {
        BlockRecord latest = blocks.get(blocks.size() - 1);
        return new LedgerVerifyResponse(
                false,
                latest.blockNumber(),
                blocks.size(),
                latest.currentHash(),
                message
        );
    }

    @Transactional
    public void append(LedgerEvent event) {
        if (ledgerRepository.existsByReference(
                event.eventType(),
                event.referenceType(),
                event.referenceId()
        )) {
            log.info("Skip duplicated ledger event. eventType={} referenceType={} referenceId={}",
                    event.eventType(), event.referenceType(), event.referenceId());
            return;
        }

        String payload = hashService.canonicalPayload(event);
        String payloadHash = hashService.payloadHash(payload);
        String transactionHash = hashService.transactionHash(event, payloadHash);

        BlockRecord latest = ledgerRepository.findLatestBlock().orElse(null);
        long blockNumber = latest == null ? 0 : latest.blockNumber() + 1;
        String previousHash = latest == null ? HashService.GENESIS_PREVIOUS_HASH : latest.currentHash();
        String merkleRoot = transactionHash;
        LocalDateTime createdAt = LocalDateTime.now();
        String blockHash = hashService.blockHash(
                blockNumber,
                previousHash,
                merkleRoot,
                createdAt.toString()
        );

        long blockId = ledgerRepository.insertBlock(
                blockNumber,
                previousHash,
                merkleRoot,
                blockHash,
                createdAt
        );
        ledgerRepository.insertTransaction(
                blockId,
                transactionHash,
                payloadHash,
                event,
                payload,
                createdAt
        );

        log.info("Ledger block appended. blockNumber={} transactionHash={} blockHash={}",
                blockNumber, transactionHash, blockHash);
    }
}
