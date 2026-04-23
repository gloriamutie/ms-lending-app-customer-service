# Customer Service (ms-lending-app-customer-service)

Manages customer profiles, loan limits, financial history, and idempotent limit reservations for the lending platform.

## Tech Stack

| Component        | Technology                                        |
|------------------|---------------------------------------------------|
| Framework        | Spring Boot 3.4.8 / Spring WebFlux                |
| Language         | Java 21                                           |
| Database         | PostgreSQL (R2DBC — reactive)                     |
| Migrations       | Flyway (runs over JDBC at startup)                |
| Caching          | Spring Cache (`CaffeineCacheManager`)             |
| Event Broker     | Apache Kafka (produces `lending.customer.events`) |
| Security         | API Key (`X-API-KEY` header)                      |
| Testing          | JUnit 5 + Mockito + StepVerifier                  |

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

| Property                  | Default                                                 |
|---------------------------|---------------------------------------------------------|
| `server.port`             | `8083`                                                  |
| `spring.r2dbc.url`        | `r2dbc:postgresql://localhost:5432/lending_customer_db` |
| `spring.datasource.url`   | `jdbc:postgresql://localhost:5432/lending_customer_db`  |
| `app.security.api-key`    | `customer-service-api-key-2024`                         |
| `spring.kafka.bootstrap-servers` | `localhost:9092`                                        |
| `spring.cache.type`       | `Caffein`                                               |

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

| Method | Endpoint                                        | Description                    |
|--------|-------------------------------------------------|--------------------------------|
| `POST` | `/api/v1/customers`                             | Create customer                |
| `GET`  | `/api/v1/customers`                             | List all (`?status=ACTIVE`)    |
| `GET`  | `/api/v1/customers/{customerId}`                | Get customer by ID (cached)    |
| `PUT`  | `/api/v1/customers/{customerId}`                | Update customer details        |

### Loan Limits

| Method | Endpoint                                              | Description                        |
|--------|-------------------------------------------------------|------------------------------------|
| `POST` | `/api/v1/customers/{customerId}/loan-limits`          | Set/update loan limit              |
| `GET`  | `/api/v1/customers/{customerId}/loan-limits`          | Get loan limit (cached)            |

### Financial History

| Method | Endpoint                                                | Description               |
|--------|---------------------------------------------------------|---------------------------|
| `GET`  | `/api/v1/customers/{customerId}/financial-history`      | Get financial history     |

### Saga Endpoints (internal, called by Loan Service)

| Method | Endpoint                                                  | Description                       |
|--------|-----------------------------------------------------------|-----------------------------------|
| `PUT`  | `/api/v1/customers/{customerId}/loan-limits/reserve`      | Reserve limit (idempotent)        |
| `PUT`  | `/api/v1/customers/{customerId}/loan-limits/release`      | Release limit (saga compensation) |

## Kafka Events Published

Topic: `lending.customer.events` (6 partitions, key = `customerId`)

| Event Type       | Trigger                    |
|------------------|----------------------------|
| `LIMIT_UPDATED`  | Loan limit set or updated  |

## Example Request — Create Customer

```bash
curl -X POST http://localhost:8083/api/v1/customers \
  -H "Content-Type: application/json" \
  -H "X-API-KEY: customer-service-api-key-2024" \
  -d '{
    "firstName": "Jane",
    "lastName": "jane",
    "email": "jane.jane@email.com",
    "phoneNumber": "+25****5678",
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
│   ├── KafkaProducerConfig.java        # 6 partitions, idempotent producer
│   ├── R2dbcConfig.java
│   └── SecurityConfig.java
├── controller/
│   └── CustomerController.java
├── dblayer/
│   ├── entities/                       # Customer, CustomerLoanLimit, CustomerFinancialHistory, LimitReservation
│   └── repo/                           # Reactive repositories
├── exception/
│   ├── GlobalExceptionHandler.java
│   └── CustomerNotFoundException.java
├── model/
│   ├── dto/                            # Request/Response records
│   └── enums/                          # CustomerStatus, RiskCategory
├── service/
│   ├── CustomerService.java            # Interface
│   ├── LimitReservationService.java    # Interface
│   └── serviceImpl/                    # Implementations
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
- Service publishes to topic `lendingCustomerEventsv2`.
- If topic auto-creation is disabled in your broker, create it manually:

```bash
kafka-topics --bootstrap-server localhost:9092 --create --topic lending.customer.events --partitions 2 --replication-factor 1
```

