-- =====================================================
-- Customer Service Schema
-- Database: PostgreSQL
-- =====================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Customers
CREATE TABLE IF NOT EXISTS customers (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    first_name      VARCHAR(50)    NOT NULL,
    last_name       VARCHAR(50)    NOT NULL,
    email           VARCHAR(100)   NOT NULL UNIQUE,
    phone_number    VARCHAR(20)    NOT NULL,
    id_number       VARCHAR(30)    NOT NULL UNIQUE,
    date_of_birth   DATE           NOT NULL,
    status          VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_customer_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED', 'PENDING'))
);

-- Customer Loan Limits
CREATE TABLE IF NOT EXISTS customer_loan_limits (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id     UUID           NOT NULL UNIQUE REFERENCES customers(id) ON DELETE CASCADE,
    max_loan_amount NUMERIC(15,2)  NOT NULL,
    available_amount NUMERIC(15,2) NOT NULL,
    credit_score    INT,
    risk_category   VARCHAR(20)    NOT NULL DEFAULT 'UNCLASSIFIED',
    last_assessed_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_risk_category CHECK (risk_category IN ('LOW', 'MEDIUM', 'HIGH', 'UNCLASSIFIED')),
    CONSTRAINT chk_available_amount CHECK (available_amount >= 0),
    CONSTRAINT chk_available_le_max CHECK (available_amount <= max_loan_amount)
);

-- Customer Financial History
CREATE TABLE IF NOT EXISTS customer_financial_history (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id     UUID           NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    record_type     VARCHAR(50)    NOT NULL,
    description     VARCHAR(500),
    amount          NUMERIC(15,2),
    recorded_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Idempotent Limit Reservations (used by Loan Creation Saga)
CREATE TABLE IF NOT EXISTS limit_reservations (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id     UUID           NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    loan_id         UUID           NOT NULL,
    idempotency_key VARCHAR(100)   NOT NULL,
    amount          NUMERIC(15,2)  NOT NULL,
    operation       VARCHAR(10)    NOT NULL,
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_operation CHECK (operation IN ('RESERVE', 'RELEASE')),
    CONSTRAINT uq_reservation_key_op UNIQUE (idempotency_key, operation)
);

CREATE INDEX idx_customers_email ON customers(email);
CREATE INDEX idx_customers_id_number ON customers(id_number);
CREATE INDEX idx_customers_status ON customers(status);
CREATE INDEX idx_loan_limits_customer ON customer_loan_limits(customer_id);
CREATE INDEX idx_financial_history_customer ON customer_financial_history(customer_id);
CREATE INDEX idx_limit_reservations_key ON limit_reservations(idempotency_key);

