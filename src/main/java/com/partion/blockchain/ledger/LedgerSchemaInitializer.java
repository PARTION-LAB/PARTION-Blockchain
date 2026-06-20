package com.partion.blockchain.ledger;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LedgerSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initialize() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ledger_blocks (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    block_number BIGINT NOT NULL UNIQUE,
                    previous_hash VARCHAR(255),
                    merkle_root VARCHAR(255),
                    current_hash VARCHAR(255) NOT NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ledger_events (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    block_id BIGINT NOT NULL,
                    transaction_hash VARCHAR(255) NOT NULL UNIQUE,
                    payload_hash VARCHAR(255) NOT NULL,
                    event_type VARCHAR(50) NOT NULL,
                    reference_type VARCHAR(50),
                    reference_id BIGINT,
                    payload JSON,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_ledger_event_block
                        FOREIGN KEY (block_id) REFERENCES ledger_blocks(id)
                )
                """);

        addColumnIfMissing("ledger_blocks", "merkle_root", "VARCHAR(255)");
        addColumnIfMissing("ledger_events", "transaction_hash", "VARCHAR(255)");
        addColumnIfMissing("ledger_events", "payload_hash", "VARCHAR(255)");
        addIndexIfMissing("idx_ledger_blocks_number", "ledger_blocks", "block_number");
        addIndexIfMissing("idx_ledger_events_reference", "ledger_events", "reference_type, reference_id");
        addIndexIfMissing("idx_ledger_events_type_time", "ledger_events", "event_type, created_at");
        addUniqueIndexIfMissing("uk_ledger_events_transaction_hash", "ledger_events", "transaction_hash");
    }

    private void addColumnIfMissing(String table, String column, String type) {
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
        } catch (RuntimeException exception) {
            log.debug("Skip adding existing column {}.{}", table, column);
        }
    }

    private void addIndexIfMissing(String index, String table, String columns) {
        try {
            jdbcTemplate.execute("CREATE INDEX " + index + " ON " + table + "(" + columns + ")");
        } catch (RuntimeException exception) {
            log.debug("Skip adding existing index {}", index);
        }
    }

    private void addUniqueIndexIfMissing(String index, String table, String columns) {
        try {
            jdbcTemplate.execute("CREATE UNIQUE INDEX " + index + " ON " + table + "(" + columns + ")");
        } catch (RuntimeException exception) {
            log.debug("Skip adding existing unique index {}", index);
        }
    }
}
