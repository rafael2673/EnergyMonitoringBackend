--liquibase formatted sql

-- changeset Rafael:1
CREATE TABLE sensor_data (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(255) NOT NULL,
    corrente FLOAT NOT NULL,
    tensao FLOAT NOT NULL,
    energia_total FLOAT NOT NULL,
    custo FLOAT NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);
