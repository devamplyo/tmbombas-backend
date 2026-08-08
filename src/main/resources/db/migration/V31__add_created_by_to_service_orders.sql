ALTER TABLE service_orders
ADD COLUMN IF NOT EXISTS created_by_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_service_orders_created_by ON service_orders (created_by_id);
