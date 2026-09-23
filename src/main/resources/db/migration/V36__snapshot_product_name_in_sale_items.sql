ALTER TABLE sale_items ADD COLUMN product_name VARCHAR(255) NOT NULL DEFAULT '';
UPDATE sale_items si SET product_name = p.name FROM products p WHERE p.id = si.product_id AND si.product_name = '';
