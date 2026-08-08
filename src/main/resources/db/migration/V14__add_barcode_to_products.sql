-- =====================================================================
-- V14 — Frente de caixa: código de barras do produto
-- Adiciona o EAN/GTIN (o que o leitor de código de barras dispara),
-- distinto do 'code' interno. Usado na busca rápida do caixa.
-- =====================================================================

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS barcode VARCHAR(64);

ALTER TABLE products
    ADD CONSTRAINT uk_products_barcode UNIQUE (barcode);

CREATE INDEX idx_products_barcode ON products (barcode);