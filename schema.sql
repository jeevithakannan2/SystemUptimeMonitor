-- SystemUptimeMonitor — PostgreSQL Schema

CREATE TABLE IF NOT EXISTS users (
    id           SERIAL       PRIMARY KEY,
    email        VARCHAR(255) NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,
    role         VARCHAR(50)  NOT NULL,
    organization VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS monitors (
    id             SERIAL        PRIMARY KEY,
    name           VARCHAR(255)  NOT NULL,
    target_url     VARCHAR(2048) NOT NULL,
    check_interval INTEGER       NOT NULL,
    created_time   TIMESTAMP     NOT NULL,
    created_by     INTEGER       NOT NULL REFERENCES users(id),
    failure_count  INTEGER       NOT NULL DEFAULT 0,
    organization   VARCHAR(255)  NOT NULL,
    enabled        BOOLEAN       NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS monitor_runs (
    id            SERIAL    PRIMARY KEY,
    monitor_id    INTEGER   NOT NULL REFERENCES monitors(id) ON DELETE CASCADE,
    time          TIMESTAMP NOT NULL,
    response_time INTEGER   NOT NULL,
    status_code   INTEGER   NOT NULL,
    success       BOOLEAN   NOT NULL
);

CREATE TABLE IF NOT EXISTS incidents (
    id             SERIAL    PRIMARY KEY,
    monitor_run_id INTEGER   NOT NULL REFERENCES monitor_runs(id) ON DELETE CASCADE,
    down_time      TIMESTAMP NOT NULL,
    resolved_time  TIMESTAMP,
    status_code    INTEGER   NOT NULL,
    resolved       BOOLEAN   NOT NULL DEFAULT FALSE,
    notes          TEXT
);

CREATE TABLE IF NOT EXISTS status_codes (
    monitor_id  INTEGER NOT NULL REFERENCES monitors(id) ON DELETE CASCADE,
    status_code INTEGER NOT NULL,
    PRIMARY KEY (monitor_id, status_code)
);

CREATE TABLE IF NOT EXISTS invites (
    id           SERIAL       PRIMARY KEY,
    created_by   INTEGER      NOT NULL REFERENCES users(id),
    created_time TIMESTAMP    NOT NULL,
    expired      BOOLEAN      NOT NULL DEFAULT FALSE,
    url          VARCHAR(255) NOT NULL UNIQUE,
    role         VARCHAR(50)  NOT NULL
);

CREATE TABLE IF NOT EXISTS monitor_audits (
    id         SERIAL      PRIMARY KEY,
    monitor_id INTEGER     NOT NULL REFERENCES monitors(id) ON DELETE CASCADE,
    operation  VARCHAR(50) NOT NULL,
    time       TIMESTAMP   NOT NULL
);
