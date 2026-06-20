package com.partion.blockchain.ledger;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LedgerRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<BlockRecord> blockRowMapper = (rs, rowNum) -> new BlockRecord(
            rs.getLong("id"),
            rs.getLong("block_number"),
            rs.getString("previous_hash"),
            rs.getString("merkle_root"),
            rs.getString("current_hash"),
            rs.getTimestamp("created_at").toLocalDateTime()
    );

    private final RowMapper<LedgerTransactionRecord> transactionRowMapper = (rs, rowNum) -> new LedgerTransactionRecord(
            rs.getLong("id"),
            rs.getLong("block_id"),
            rs.getString("transaction_hash"),
            rs.getString("payload_hash"),
            rs.getString("event_type"),
            rs.getString("reference_type"),
            rs.getLong("reference_id"),
            rs.getString("payload"),
            rs.getTimestamp("created_at").toLocalDateTime()
    );

    public boolean existsByReference(String eventType, String referenceType, Long referenceId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM ledger_events
                WHERE event_type = ?
                  AND reference_type = ?
                  AND reference_id = ?
                """, Integer.class, eventType, referenceType, referenceId);

        return count != null && count > 0;
    }

    public Optional<BlockRecord> findLatestBlock() {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                    SELECT id, block_number, previous_hash, merkle_root, current_hash, created_at
                    FROM ledger_blocks
                    ORDER BY block_number DESC
                    LIMIT 1
                    """, blockRowMapper));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public List<BlockRecord> findBlocks() {
        return jdbcTemplate.query("""
                SELECT id, block_number, previous_hash, merkle_root, current_hash, created_at
                FROM ledger_blocks
                ORDER BY block_number ASC
                """, blockRowMapper);
    }

    public List<LedgerTransactionRecord> findRecentTransactions(int limit) {
        return jdbcTemplate.query("""
                SELECT id, block_id, transaction_hash, payload_hash, event_type, reference_type, reference_id, payload, created_at
                FROM ledger_events
                ORDER BY created_at DESC, id DESC
                LIMIT ?
                """, transactionRowMapper, limit);
    }

    public long insertBlock(
            long blockNumber,
            String previousHash,
            String merkleRoot,
            String currentHash,
            LocalDateTime createdAt
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO ledger_blocks (
                        block_number,
                        previous_hash,
                        merkle_root,
                        current_hash,
                        created_at
                    )
                    VALUES (?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, blockNumber);
            statement.setString(2, previousHash);
            statement.setString(3, merkleRoot);
            statement.setString(4, currentHash);
            statement.setTimestamp(5, Timestamp.valueOf(createdAt));
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to insert ledger block.");
        }

        return key.longValue();
    }

    public void insertTransaction(
            long blockId,
            String transactionHash,
            String payloadHash,
            LedgerEvent event,
            String payload,
            LocalDateTime createdAt
    ) {
        jdbcTemplate.update("""
                INSERT INTO ledger_events (
                    block_id,
                    transaction_hash,
                    payload_hash,
                    event_type,
                    reference_type,
                    reference_id,
                    payload,
                    created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                blockId,
                transactionHash,
                payloadHash,
                event.eventType(),
                event.referenceType(),
                event.referenceId(),
                payload,
                Timestamp.valueOf(createdAt)
        );
    }
}
