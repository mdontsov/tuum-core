# Core Banking Service

This is a small account and transaction service built with Java 21, Spring Boot,
MyBatis, PostgreSQL and RabbitMQ. It supports accounts in multiple currencies,
keeps a transaction history and publishes every business change as an event.

## Running the application

Docker is the only prerequisite. The repository includes the application, database
and RabbitMQ configuration, so there is no need to install Java or Gradle or change
the system `PATH`.

Start the complete stack from the repository root:

```shell
docker compose up --build
```

Once the `app` container is healthy, the following endpoints are available:

- REST API: `http://localhost:8080`
- Health check: `http://localhost:8080/actuator/health`
- RabbitMQ management: `http://localhost:15672` (`banking` / `banking`)
- PostgreSQL: `localhost:5432` (`banking` / `banking`)

The application logs an entry after RabbitMQ confirms each published event. The
entry contains the event ID, event type, routing key and JSON payload. To follow
these messages while using the API, run:

```shell
docker compose logs -f app
```

Stop the containers with:

```shell
docker compose down
```

PostgreSQL data is kept in a Docker volume. Use the following command when a clean
database is needed:

```shell
docker compose down --volumes
```

All commands above work the same way on Windows, macOS and Linux.

## Trying the API

The examples are written as raw HTTP, so they are not tied to a particular shell or
operating system. They can be sent from IntelliJ HTTP Client, VS Code REST Client,
Postman, Insomnia or any similar tool. Ready-to-run copies are also provided in
[`api-requests.http`](api-requests.http).

### Create an account

```http
POST http://localhost:8080/api/v1/accounts
Content-Type: application/json

{
  "customerId": "customer-123",
  "country": "EE",
  "currencies": ["EUR", "USD"]
}
```

The response contains the new account ID and one zero-valued balance for every
requested currency. Supported currencies are `EUR`, `SEK`, `GBP` and `USD`.

### Get an account

Replace `ACCOUNT_ID` with the ID returned by the create request.

```http
GET http://localhost:8080/api/v1/accounts/ACCOUNT_ID
```

### Create a transaction

```http
POST http://localhost:8080/api/v1/transactions
Content-Type: application/json

{
  "accountId": "ACCOUNT_ID",
  "amount": 100.00,
  "currency": "EUR",
  "direction": "IN",
  "description": "Deposit"
}
```

`IN` adds to the balance and `OUT` subtracts from it. Amounts must be positive and
may have up to four decimal places. An outgoing transaction is rejected when the
account does not have enough money in that currency.

### Get transaction history

```http
GET http://localhost:8080/api/v1/transactions?accountId=ACCOUNT_ID
```

Validation and business errors are returned as RFC 9457 Problem Details. Each error
also has a stable `code` field that clients can use without parsing its message.

## Building and testing

To build the application image without starting the stack, run:

```shell
docker compose build app
```

The integration tests use Testcontainers with real PostgreSQL and RabbitMQ
instances. They cover request validation, persistence, balance changes, concurrent
withdrawals, outbox processing and RabbitMQ delivery. JaCoCo checks that instruction
coverage stays above 80%.

The GitHub Actions workflow runs the full Gradle test suite, verifies coverage and
builds the Docker image on every push and pull request. Its HTML test and coverage
reports are available from the workflow run as the `verification-reports` artifact.

## Design

I kept the solution as a modular monolith. Accounts, transactions and messaging are
separated in the code, but they run in one application and use one database. For
this scope, separate account and transaction services would add network calls,
eventual consistency and compensation logic without a practical benefit.

PostgreSQL is the source of truth for accounts, balances and transaction history.
When a transaction is created, the service locks the balance row for the requested
account and currency with `SELECT FOR UPDATE`. It then checks the funds, changes the
balance, inserts the transaction and records the related events in one database
transaction. This prevents concurrent withdrawals from overspending an account,
while operations on unrelated balances can still proceed independently.

Amounts are represented by `BigDecimal` in Java and `NUMERIC(19,4)` in PostgreSQL.
Identifiers are UUIDs, and Flyway creates and updates the database schema. MyBatis
Dynamic SQL keeps the queries explicit, including the locking behaviour.

### RabbitMQ and the transactional outbox

Publishing directly to RabbitMQ inside a database transaction can leave the two
systems out of sync. Instead, each account, balance or transaction change writes an
event to the `outbox_events` table as part of the same commit as the business data.

A scheduled publisher reads pending events with `FOR UPDATE SKIP LOCKED`, sends
them as persistent JSON messages to the durable `account.events` topic exchange,
waits for publisher confirmation and then marks them as published. The available
routing keys are:

- `account.created`
- `balance.created`
- `balance.updated`
- `transaction.created`

Delivery is at least once, so every message includes stable `eventId`, `eventType`,
`aggregateType` and `aggregateId` headers. Consumers can use the event ID to ignore
a duplicate delivery.

## Performance

A k6 workload is included in the `performance` Docker Compose profile. Each virtual
user works with a separate account, which avoids turning the test into a benchmark
of a single locked balance row. With the application running, start it with:

```shell
docker compose --profile performance run --rm k6
```

On my development machine—an AMD Ryzen 9 5950X with 64 GB RAM and roughly 31 GB
assigned to Docker Desktop—the 60-second test used 40 virtual users and produced:

- 235,254 completed transaction requests
- about **3,743 requests per second**
- no failed requests
- 10.56 ms average response time and 18.04 ms p95 response time

That figure shows how quickly the API and database can accept a short burst; it is
not the sustainable end-to-end rate. Each transaction creates two outbox events,
and the single publisher processed about 170 events per second during the same test.
Without allowing the outbox backlog to grow continuously, the current application
therefore handles approximately **80–90 transactions per second end to end**.

Results will vary with hardware, Docker limits, database contents, logging and
RabbitMQ settings. A test that repeatedly updates one account will also be slower,
because changes to the same balance must be serialized for correctness.

## Horizontal scaling

The application does not keep session state, so multiple instances can run behind a
load balancer. They can share PostgreSQL safely because balance correctness is
enforced by database row locks. The outbox query uses `SKIP LOCKED`, which also lets
publishers on several instances divide pending work without blocking one another.

The local test showed that outbox publication is the first part that needs attention.
I would start by publishing in batches, adding publisher workers and tuning the
polling delay. The important operational signals are the number of unpublished
events and the age of the oldest one. If a consumer relies on per-account ordering,
that guarantee also needs to be maintained when publication is parallelized.

More application instances mean more database connections, so pool sizes must be
planned across the whole deployment. RabbitMQ consumers must also be idempotent
because at-least-once delivery can produce duplicates. At higher volumes, old
transaction and outbox data can be archived or partitioned, while read replicas can
serve transaction-history queries. Sharding by account ID is an option if one
database eventually becomes the write bottleneck, but every balance change for a
given account should stay within one database transaction boundary.

## Usage of AI

Codex CLI has been a great help for understanding and integrating MyBatis and RabbitMQ components
mainly because I haven't had any previous work experience with those