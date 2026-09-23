ALTER TABLE sales ADD COLUMN client_id BIGINT;
ALTER TABLE sales ADD CONSTRAINT fk_sales_client
    FOREIGN KEY (client_id) REFERENCES clients (id);
