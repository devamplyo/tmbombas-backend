
ALTER TABLE service_orders
ADD COLUMN order_number VARCHAR(20);

UPDATE service_orders
SET order_number = CONCAT('OS-', LPAD(id::TEXT, 6, '0'))
WHERE order_number IS NULL;

ALTER TABLE service_orders
ALTER COLUMN order_number SET NOT NULL;

CREATE UNIQUE INDEX idx_service_orders_order_number
ON service_orders(order_number);