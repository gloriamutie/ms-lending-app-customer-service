-- =====================================================
-- Customer Service Seed Data
-- =====================================================

-- Customer 1: Active customer with good credit
INSERT INTO customers (id, first_name, last_name, email, phone_number, id_number, date_of_birth, status)
VALUES ('d1e2f3a4-b5c6-7890-def1-234567890abc', 'Jane', 'Wanjiku', 'jane.wanjiku@email.com', '+254712345678', 'ID12345678', '1990-05-15', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO customer_loan_limits (id, customer_id, max_loan_amount, available_amount, credit_score, risk_category)
VALUES ('e1f2a3b4-c5d6-7890-ef12-34567890abcd', 'd1e2f3a4-b5c6-7890-def1-234567890abc', 100000.00, 75000.00, 720, 'LOW')
ON CONFLICT (id) DO NOTHING;

INSERT INTO customer_financial_history (id, customer_id, record_type, description, amount, recorded_at)
VALUES ('f1a2b3c4-d5e6-7890-f123-4567890abcde', 'd1e2f3a4-b5c6-7890-def1-234567890abc', 'LOAN_REPAYMENT', 'Successfully repaid Quick Cash Loan', 5000.00, '2025-12-01 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- Customer 2: Active customer with medium risk
INSERT INTO customers (id, first_name, last_name, email, phone_number, id_number, date_of_birth, status)
VALUES ('d2e3f4a5-b6c7-8901-def2-345678901bcd', 'John', 'Kamau', 'john.kamau@email.com', '+254723456789', 'ID23456789', '1985-08-20', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO customer_loan_limits (id, customer_id, max_loan_amount, available_amount, credit_score, risk_category)
VALUES ('e2f3a4b5-c6d7-8901-ef23-45678901bcde', 'd2e3f4a5-b6c7-8901-def2-345678901bcd', 50000.00, 50000.00, 580, 'MEDIUM')
ON CONFLICT (id) DO NOTHING;

-- Customer 3: Pending customer
INSERT INTO customers (id, first_name, last_name, email, phone_number, id_number, date_of_birth, status)
VALUES ('d3e4f5a6-b7c8-9012-def3-456789012cde', 'Mary', 'Akinyi', 'mary.akinyi@email.com', '+254734567890', 'ID34567890', '1995-02-10', 'PENDING')
ON CONFLICT (id) DO NOTHING;

