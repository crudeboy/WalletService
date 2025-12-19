CREATE TABLE wallets (
  id BIGSERIAL PRIMARY KEY,
  balance BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE TABLE transactions (
  id BIGSERIAL PRIMARY KEY,
  wallet_id BIGINT NOT NULL,
  related_wallet_id BIGINT,
  amount BIGINT NOT NULL,
  type VARCHAR(20) NOT NULL,
  idempotency_key VARCHAR(255) NOT NULL UNIQUE,
  created_at TIMESTAMP NOT NULL
);
