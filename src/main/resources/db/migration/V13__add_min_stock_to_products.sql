-- =====================================================================
-- V13 — Alerta de estoque baixo
-- Adiciona o estoque mínimo (ponto de reposição) de cada produto.
-- O alerta dispara quando stock <= min_stock.
-- =====================================================================

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS min_stock INTEGER NOT NULL DEFAULT 0;

ALTER TABLE products
    ADD CONSTRAINT chk_products_min_stock_nonneg CHECK (min_stock >= 0);

CREATE INDEX idx_products_low_stock ON products (stock);