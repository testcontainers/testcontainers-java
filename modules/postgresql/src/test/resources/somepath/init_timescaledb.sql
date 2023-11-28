-- Extend the database with TimescaleDB
CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;

CREATE TABLE bar
(
    foo  VARCHAR(255),
    time TIMESTAMPTZ NOT NULL
);

SELECT create_hypertable('bar', 'time');

INSERT INTO bar (time, foo)
VALUES (CURRENT_TIMESTAMP, 'hello world');
