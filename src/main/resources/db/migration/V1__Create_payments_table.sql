-- V1__Create_payments_table.sql
-- Place this file in: src/main/resources/db/migration/

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    amount NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    transaction_reference VARCHAR(255),
    description TEXT,
    merchant_id VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- Indexes for performance optimization
CREATE INDEX idx_payments_idempotency_key ON payments(idempotency_key);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_created_at ON payments(created_at DESC);
CREATE INDEX idx_payments_merchant_id ON payments(merchant_id);
CREATE INDEX idx_payments_transaction_ref ON payments(transaction_reference);

-- Composite index for common query patterns
CREATE INDEX idx_payments_status_created_at ON payments(status, created_at DESC);
CREATE INDEX idx_payments_merchant_status ON payments(merchant_id, status);

-- Add comments for documentation
COMMENT ON TABLE payments IS 'Stores all payment transactions with idempotency support';
COMMENT ON COLUMN payments.idempotency_key IS 'Unique key to prevent duplicate payments';
COMMENT ON COLUMN payments.version IS 'Optimistic locking version field';
COMMENT ON COLUMN payments.status IS 'Payment status: PENDING, PROCESSING, SUCCESS, FAILED, CANCELLED';

-- Create audit trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger for automatic updated_at
CREATE TRIGGER update_payments_updated_at 
    BEFORE UPDATE ON payments
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();