ALTER TABLE service_order_items ADD COLUMN product_id BIGINT;
ALTER TABLE service_order_items ADD COLUMN quantity INTEGER;
ALTER TABLE service_order_items ADD CONSTRAINT fk_service_order_items_product
    FOREIGN KEY (product_id) REFERENCES products (id);
