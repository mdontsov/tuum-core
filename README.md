# Core Banking Service

A small account and transaction service built with Java 17, Spring Boot, MyBatis,
PostgreSQL, RabbitMQ, Gradle and JUnit 5.

## Run the application

The only prerequisite is a running Docker engine. No Java, Gradle, database,
message broker or `PATH` change is required.

```shell
docker compose up --build
```

Wait until the `app` container is healthy. The services are then available at:

- REST API: `http://localhost:8080`
- Health: `http://localhost:8080/actuator/health`
- RabbitMQ management: `http://localhost:15672` (`banking` / `banking`)
- PostgreSQL: `localhost:5432` (`banking` / `banking`)

Stop the application with `docker compose down`. To also remove local database
data, run `docker compose down --volumes`.

## API

Create an account:

```shell
curl -i -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{"customerId":"customer-123","country":"EE","currencies":["EUR","USD"]}'
```

Get an account:

```shell
curl http://localhost:8080/api/v1/accounts/ACCOUNT_ID
```

Create a transaction:

```shell
curl -i -X POST http://localhost:8080/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{"accountId":"ACCOUNT_ID","amount":100.00,"currency":"EUR","direction":"IN","description":"Deposit"}'
```

Get transactions:

```shell
curl "http://localhost:8080/api/v1/transactions?accountId=ACCOUNT_ID"
```

Supported currencies are `EUR`, `SEK`, `GBP` and `USD`; directions are `IN`
and `OUT`. Amounts must be positive and may have at most four decimal places.
Errors use RFC 9457 Problem Details and include a stable `code` property.

## Build and test

The Gradle Wrapper is committed, so a local Java 17+ installation is sufficient:

```shell
./gradlew clean check
```

On Windows:

```powershell
.\gradlew.bat clean check
```

Integration tests use Testcontainers and therefore require Docker. They start real
PostgreSQL and RabbitMQ instances and cover REST validation, persistence, balance
changes, concurrent withdrawals, the transactional outbox and actual RabbitMQ
delivery. JaCoCo enforces at least 80% instruction coverage during `check`; the HTML
report is written to `build/reports/jacoco/test/html/index.html`.

## Architecture and important choices

This is a modular monolith. Accounts, balances, transactions and messaging have
separate packages, but they share one process and consistency boundary. Splitting
balance ownership and transaction creation between services would require a saga,
compensation logic and eventual consistency without providing value at this scale.

Every balance change and ledger entry is committed atomically. Transaction creation
locks the affected `(account_id, currency)` balance row with `SELECT FOR UPDATE`,
checks available funds, updates the balance, inserts an immutable ledger entry and
inserts outbox records in one PostgreSQL transaction. Consequently, concurrent
withdrawals cannot overdraw a balance, while different balances proceed independently.

Money uses Java `BigDecimal` and PostgreSQL `NUMERIC(19,4)`. UUIDs avoid a central ID
generator. Flyway owns schema initialization, and MyBatis XML keeps locking and SQL
behavior explicit.

Business mutations create domain events in `outbox_events` in the same database
transaction. A scheduled publisher claims events with `FOR UPDATE SKIP LOCKED`, sends
persistent JSON messages to the durable `account.events` topic exchange, waits for a
RabbitMQ publisher confirmation, then marks them published. Routing keys are:

- `account.created`
- `balance.created`
- `balance.updated`
- `transaction.created`

Delivery is at least once. Every message carries stable `eventId`, `eventType`,
`aggregateType` and `aggregateId` headers, so consumers can deduplicate by event ID.

## Performance estimate

The included k6 workload gives every virtual user its own account, avoiding an
artificial single-account lock bottleneck. With the application already running:

```shell
docker compose --profile performance run --rm k6
```

A conservative pre-benchmark estimate for a typical 8-core development laptop is
**300–700 committed transactions per second**, including four PostgreSQL writes
(balance, transaction and two outbox records) per request. This is an estimate, not
a measured claim for the reviewer's hardware. The reproducible k6 result—request
rate, error rate, and p95 latency—is the authoritative figure. A workload focused on
one account/currency will be much lower by design because those mutations must be
serialized to preserve financial correctness.

## Horizontal scaling considerations

The HTTP application is stateless and can run in multiple replicas. PostgreSQL is
the source of truth; row locking preserves balance correctness across replicas.
Outbox publishers use `SKIP LOCKED`, allowing replicas to share publication work.
RabbitMQ consumers must be idempotent because delivery is at least once.

At larger scale, size the total connection pool across replicas, monitor lock waits
and outbox lag, apply back-pressure, and partition or archive the transaction and
outbox tables. Read replicas can serve history queries. If write volume outgrows one
database, accounts can be sharded by account ID, but a single account's ordered
balance mutations should remain in one consistency boundary. Migrations must run
with Flyway locking, and production deployments need metrics, tracing, centralized
logs, readiness checks and graceful shutdown.

## Repository handover

Commit this directory to an accessible GitHub or GitLab repository and grant the
reviewers access. The repository should include all files currently present; no
credentials beyond the local Docker development defaults are required.
