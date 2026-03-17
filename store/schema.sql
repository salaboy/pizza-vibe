CREATE TABLE IF NOT EXISTS orders (
    order_id     UUID PRIMARY KEY,
    order_items  JSONB NOT NULL,
    drink_items  JSONB,
    order_data   TEXT NOT NULL DEFAULT '',
    order_status TEXT NOT NULL DEFAULT 'pending'
);

CREATE TABLE IF NOT EXISTS order_events (
    id         SERIAL PRIMARY KEY,
    order_id   UUID NOT NULL REFERENCES orders(order_id),
    status     TEXT NOT NULL,
    source     TEXT NOT NULL,
    message    TEXT,
    tool_name  TEXT,
    tool_input TEXT
);

CREATE TABLE IF NOT EXISTS agent_events (
    id         SERIAL PRIMARY KEY,
    agent_id   TEXT NOT NULL,
    kind       TEXT NOT NULL,
    text       TEXT NOT NULL,
    timestamp  TIMESTAMPTZ NOT NULL
);

-- Drop order_id column if it exists from a previous schema version
ALTER TABLE agent_events DROP COLUMN IF EXISTS order_id;
