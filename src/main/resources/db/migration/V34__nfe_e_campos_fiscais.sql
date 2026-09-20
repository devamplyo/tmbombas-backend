-- =====================================================================
-- V34 — NF-e (nota de venda de produto) e campos fiscais
--
-- invoices : passa a guardar também NF-e (document_type), com a venda de
--            origem, a chave de acesso e o protocolo da SEFAZ.
-- products : dados fiscais de cada produto (só o ADM preenche).
-- clients  : inscrição estadual (só existe para empresa contribuinte de ICMS).
--
-- H2 não suporta múltiplos ADD COLUMN numa só instrução (ver V10) — uma por linha.
-- =====================================================================

ALTER TABLE invoices ADD COLUMN document_type VARCHAR(10) DEFAULT 'NFSE' NOT NULL;
ALTER TABLE invoices ADD COLUMN sale_id       BIGINT;
ALTER TABLE invoices ADD COLUMN chave_acesso  VARCHAR(60);
ALTER TABLE invoices ADD COLUMN protocolo     VARCHAR(40);

CREATE INDEX idx_invoices_sale_id ON invoices (sale_id);

ALTER TABLE products ADD COLUMN ncm    VARCHAR(8);
ALTER TABLE products ADD COLUMN cfop   VARCHAR(4);
ALTER TABLE products ADD COLUMN origin INTEGER;
ALTER TABLE products ADD COLUMN csosn  VARCHAR(3);

ALTER TABLE clients ADD COLUMN state_registration VARCHAR(20);
