# PARTION Blockchain

Private ledger node for PARTION trade settlement events.

The service consumes settled trade ledger events from Kafka topic
`partion.ledger.events`, creates a transaction hash and block hash, links blocks
with `previousHash`, and stores the ledger in MySQL for query and verification.

## Local Build

```bash
mvn test
mvn -DskipTests package
```

## Environment Variables

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `KAFKA_BOOTSTRAP_SERVERS`
- `KAFKA_GROUP_ID`
- `PARTION_LEDGER_TOPIC`

## APIs

- `GET /api/health`
- `GET /api/node/ledger/blocks`
- `GET /api/node/ledger/transactions`
- `GET /api/node/ledger/verify`
