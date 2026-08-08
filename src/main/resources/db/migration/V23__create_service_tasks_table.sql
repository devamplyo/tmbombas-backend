CREATE TABLE service_tasks (
    id               BIGSERIAL PRIMARY KEY,
    service_order_id BIGINT REFERENCES service_orders(id),
    technician_id    BIGINT REFERENCES usuarios(id),
    client_id        BIGINT REFERENCES clients(id),
    description      TEXT,
    status           VARCHAR(50) NOT NULL,
    scheduled_date   DATE,
    scheduled_time   VARCHAR(50),
    started_at       TIMESTAMP,
    completed_at     TIMESTAMP,
    created_at       TIMESTAMP NOT NULL
);
