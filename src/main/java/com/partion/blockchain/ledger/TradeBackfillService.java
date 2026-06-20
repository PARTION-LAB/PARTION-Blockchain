package com.partion.blockchain.ledger;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeBackfillService implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final LedgerService ledgerService;

    @Value("${partion.blockchain.backfill.enabled:true}")
    private boolean backfillEnabled;

    @Override
    public void run(ApplicationArguments args) {
        if (!backfillEnabled) {
            return;
        }

        List<LedgerEvent> events = jdbcTemplate.query("""
                SELECT
                    t.id AS trade_id,
                    t.product_id,
                    p.name AS product_name,
                    p.category AS product_category,
                    t.buy_order_id,
                    t.sell_order_id,
                    t.price,
                    t.quantity,
                    t.traded_at
                FROM trades t
                JOIN products p ON p.id = t.product_id
                LEFT JOIN ledger_events le
                    ON le.event_type = 'TRADE_SETTLED'
                   AND le.reference_type = 'TRADE'
                   AND le.reference_id = t.id
                WHERE le.id IS NULL
                ORDER BY t.id ASC
                """, (rs, rowNum) -> toLedgerEvent(rs));

        for (LedgerEvent event : events) {
            ledgerService.append(event);
        }

        if (!events.isEmpty()) {
            log.info("Backfilled ledger blocks from existing trades. count={}", events.size());
        }
    }

    private LedgerEvent toLedgerEvent(ResultSet rs) throws java.sql.SQLException {
        long tradeId = rs.getLong("trade_id");
        Timestamp tradedAt = rs.getTimestamp("traded_at");
        long occurredAt = tradedAt == null
                ? System.currentTimeMillis()
                : tradedAt.toLocalDateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        BigDecimal price = rs.getBigDecimal("price");
        long quantity = rs.getLong("quantity");

        return new LedgerEvent(
                "TRADE_SETTLED-" + tradeId,
                "TRADE_SETTLED",
                "TRADE",
                tradeId,
                rs.getLong("product_id"),
                rs.getString("product_name"),
                rs.getString("product_category"),
                rs.getLong("buy_order_id"),
                rs.getLong("sell_order_id"),
                price,
                quantity,
                price.multiply(BigDecimal.valueOf(quantity)),
                occurredAt
        );
    }
}
