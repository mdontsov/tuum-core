CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    customer_id VARCHAR(100) NOT NULL,
    country VARCHAR(2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_accounts_customer_id ON accounts (customer_id);

CREATE TABLE balances (
    account_id UUID NOT NULL REFERENCES accounts(id),
    currency VARCHAR(3) NOT NULL,
    available_amount NUMERIC(19,4) NOT NULL CHECK (available_amount >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (account_id, currency)
);

CREATE TABLE account_transactions (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES accounts(id),
    amount NUMERIC(19,4) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL,
    direction VARCHAR(3) NOT NULL CHECK (direction IN ('IN', 'OUT')),
    description VARCHAR(500) NOT NULL,
    balance_after NUMERIC(19,4) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_transactions_account_created
    ON account_transactions (account_id, created_at, id);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    attempts INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_outbox_unpublished
    ON outbox_events (occurred_at) WHERE published_at IS NULL;
