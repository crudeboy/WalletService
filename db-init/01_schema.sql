-- Enable the pgcrypto extension for UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Wallets table
CREATE TABLE wallets (
    id BIGSERIAL PRIMARY KEY,
    balance BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

-- Transactions table
CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id BIGINT NOT NULL,
    related_wallet_id BIGINT,
    amount BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_wallet FOREIGN KEY(wallet_id) REFERENCES wallets(id),
    CONSTRAINT fk_related_wallet FOREIGN KEY(related_wallet_id) REFERENCES wallets(id)
);

-- Index for faster wallet queries
CREATE INDEX idx_wallet_id ON transactions(wallet_id);