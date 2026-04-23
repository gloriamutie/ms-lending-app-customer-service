# Customer Service (ms-lending-app-customer-service)

Manages customer profiles, loan limits, financial history, and idempotent limit reservations for the lending platform.

## Tech Stack

| Component        | Technology                                                   |
|------------------|--------------------------------------------------------------|
| Framework        | Spring Boot 3.4.8 / Spring WebFlux                           |
| Language         | Java 21                                                      |
| Database         | PostgreSQL (R2DBC — reactive)                                |
| Migrations       | Flyway (runs over JDBC at startup)                           |
| Caching          | Spring Cache (`CaffeineCacheManager`)                        |
| Event Broker     | Apache Kafka — topic `lendingCustomerEventsv2`               |
| Real-time Stream | Reactor `Sinks.Many` → SSE (`text/event-stream`)             |
| Security         | API Key (`X-API-KEY` header)                                 |
| Testing          | JUnit 5 + Mockito + StepVerifier                             |

## Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 15+
- Apache Kafka 3.x+
- Create database: `CREATE DATABASE lending_customer_db;`

## Getting Started

```bash
cd ms-lending-app-customer-service

# Build
./mvnw clean compile

# Run
./mvnw spring-boot:run

# Run tests
./mvnw clean test

# Generate coverage report
./mvnw clean test jacoco:report
```

The service starts on **port 8083** and Flyway auto-creates all tables on first startup.

## Configuration

| Property                          | Default                                                  |
|-----------------------------------|----------------------------------------------------------|
| `server.port`                     | `8083`                                                   |
| `spring.r2dbc.url`                | `r2dbc:postgresql://localhost:5432/lending_customer_db`  |
| `spring.datasource.url`           | `jdbc:postgresql://localhost:5432/lending_customer_db`   |
| `app.security.api-key`            | `customer-service-api-key-2024`                          |
| `spring.kafka.bootstrap-servers`  | `localhost:9092`                                         |
| `app.kafka.topic.loan-events`     | `lendingCustomerEventsv2`                                |
| `app.kafka.topic.partitions`      | `2`                                                      |
| `spring.cache.type`               | `simple`                                                 |

## Database Schema

Flyway migration `V1__init_schema.sql` creates:

- **customers** — customer profiles (name, email, phone, ID, DOB, status)
- **customer_loan_limits** — borrowing limits per customer (max amount, available, credit score, risk category)
- **customer_financial_history** — historical financial records
- **limit_reservations** — idempotent reserve/release operations (used by Loan Service saga)

`V2__seed_data.sql` inserts demo customers: Jane Wanjiku (LOW risk), John Kamau (MEDIUM risk), Mary Akinyi (PENDING).

## API Endpoints

All endpoints require header: `X-API-KEY: customer-service-api-key-2024`

### Customer Management

| Method | Endpoint                          | Content-Type / Accept            | Description                         |
|--------|-----------------------------------|----------------------------------|-------------------------------------|
| `POST` | `/api/v1/customers`               | `application/json`               | Create customer                     |
| `GET`  | `/api/v1/customers/{customerId}`  | `application/json`               | Get customer by ID (cached)         |
| `GET`  | `/api/v1/customers`               | `application/x-ndjson`           | Stream all customers (NDJSON)       |
| `PUT`  | `/api/v1/customers/{customerId}`  | `application/json`               | Update customer details             |

### Loan Limits

| Method | Endpoint                                         | Content-Type / Accept   | Description                                        |
|--------|--------------------------------------------------|-------------------------|----------------------------------------------------|
| `POST` | `/api/v1/customers/{customerId}/loan-limits`     | `application/json`      | Set/update loan limit → publishes Kafka + SSE      |
| `GET`  | `/api/v1/customers/{customerId}/loan-limits`     | `application/json`      | Get loan limit for a customer (cached)             |
| `GET`  | `/api/v1/customers/loan-limits/stream`           | `text/event-stream`     | **SSE** — real-time stream of `LIMIT_UPDATED` events |

### Financial History

| Method | Endpoint                                           | Accept                  | Description                          |
|--------|----------------------------------------------------|-------------------------|--------------------------------------|
| `GET`  | `/api/v1/customers/{customerId}/financial-history` | `application/x-ndjson`  | Stream financial history (NDJSON)    |

### Saga Endpoints (internal, called by Loan Service)

| Method | Endpoint                                              | Description                       |
|--------|-------------------------------------------------------|-----------------------------------|
| `PUT`  | `/api/v1/customers/{customerId}/loan-limits/reserve`  | Reserve limit (idempotent)        |
| `PUT`  | `/api/v1/customers/{customerId}/loan-limits/release`  | Release limit (saga compensation) |

## Loan Limit SSE Stream — How It Works

When `POST /api/v1/customers/{customerId}/loan-limits` is called:

1. **Persists** the limit to PostgreSQL via R2DBC.
2. **Publishes** a `LIMIT_UPDATED` event to Kafka topic `lendingCustomerEventsv2` (durable, keyed by `customerId`). A `whenComplete` callback logs the partition and offset on success, or the error on failure.
3. **Emits** the `LoanLimitResponse` to an in-process `Sinks.Many` (hot multicast). All active SSE subscribers receive it immediately.

```
POST /loan-limits
       │
       ├─► PostgreSQL (save)
       ├─► Kafka topic: lendingCustomerEventsv2  (durable)
       └─► Sinks.Many ──► GET /loan-limits/stream (SSE) ──► Loan Service local DB
```

### Subscribe to the stream

```bash
# Terminal 1 — listen for live LIMIT_UPDATED events
curl -N \
  -H "X-API-KEY: customer-service-api-key-2024" \
  -H "Accept: text/event-stream" \
  http://localhost:8083/api/v1/customers/loan-limits/stream

# Terminal 2 — trigger a limit update
curl -X POST http://localhost:8083/api/v1/customers/<customerId>/loan-limits \
  -H "Content-Type: application/json" \
  -H "X-API-KEY: customer-service-api-key-2024" \
  -d '{
    "maxLoanAmount": 500000,
    "creditScore": 750,
    "riskCategory": "LOW"
  }'
```

You should see an SSE event appear in Terminal 1 immediately:

```
data: {"id":"...","customerId":"...","maxLoanAmount":500000.00,"availableAmount":500000.00,"creditScore":750,"riskCategory":"LOW","lastAssessedAt":"2026-04-23T10:00:00"}
```

> **Note:** The SSE stream is a **hot publisher** — subscribers only receive events emitted _after_ connecting. Use Kafka consumer for historical replay.

## Kafka Events Published

Topic: `lendingCustomerEventsv2` (2 partitions, key = `customerId`)

| Event Type      | Trigger                   | Payload fields                                 |
|-----------------|---------------------------|------------------------------------------------|
| `LIMIT_UPDATED` | Loan limit set or updated | `eventType`, `customerId`, `maxLoanAmount`     |

## Example Request — Create Customer

```bash
curl -X POST http://localhost:8083/api/v1/customers \
  -H "Content-Type: application/json" \
  -H "X-API-KEY: customer-service-api-key-2024" \
  -d '{
    "firstName": "Jane",
    "lastName": "Wanjiku",
    "email": "jane.wanjiku@email.com",
    "phoneNumber": "+254712345678",
    "idNumber": "ID12345678",
    "dateOfBirth": "1990-05-15",
    "status": "ACTIVE"
  }'
```

## Project Structure

```
src/main/java/com/glo/lending/customer/
├── CustomerServiceApplication.java
├── components/
│   └── CustomerCacheService.java       # Spring CacheManager wrapper
├── config/
│   ├── CacheConfig.java                # @EnableCaching
│   ├── KafkaProducerConfig.java        # idempotent producer, topic auto-create from properties
│   └── SecurityConfig.java
├── controller/
│   └── CustomerController.java         # REST + SSE endpoints
├── dblayer/
│   ├── entities/                       # Customer, CustomerLoanLimit, CustomerFinancialHistory, LimitReservation
│   └── repo/                           # Reactive R2DBC repositories
├── exception/
│   ├── GlobalExceptionHandler.java
│   └── CustomerNotFoundException.java
├── model/
│   ├── dto/                            # Request/Response records
│   └── enums/                          # CustomerStatus, RiskCategory
├── service/
│   ├── CustomerService.java            # Interface (incl. streamLoanLimits)
│   └── serviceImpl/
│       └── CustomerServiceImpl.java    # Sinks.Many hot stream + Kafka publish
└── utils/
    └── CustomerMapper.java
```

## Troubleshooting

### Flyway did not migrate

- Confirm DB name is `lending_customer_db` in both `spring.r2dbc.url` and `spring.datasource.url`.
- Clean and rebuild to refresh classpath resources:

```bash
./mvnw clean package
./mvnw spring-boot:run
```

- Verify migration history in PostgreSQL:

```sql
SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

### Kafka not initializing

- Ensure Kafka broker is reachable at `localhost:9092`.
- The service auto-creates topic `lendingCustomerEventsv2` on startup via the `NewTopic` bean (reads from `app.kafka.topic.loan-events`).
- If auto-creation is disabled on the broker, create it manually:

```bash
kafka-topics --bootstrap-server localhost:9092 \
  --create --topic lendingCustomerEventsv2 \
  --partitions 2 --replication-factor 1
```

- Check broker connectivity:

```bash
kafka-broker-api-versions --bootstrap-server localhost:9092
```

### SSE stream returns no events

- Make sure you pass the correct `Accept` header: `Accept: text/event-stream`
- The stream is a **hot publisher** — connect _before_ calling `POST /loan-limits`
- Check logs for `Emitted LIMIT_UPDATED to SSE stream` (DEBUG level) and `Published LIMIT_UPDATED to Kafka` (INFO level)
- If `SSE sink emit failed` appears in logs, check for backpressure issues (too many events with no active subscriber)
