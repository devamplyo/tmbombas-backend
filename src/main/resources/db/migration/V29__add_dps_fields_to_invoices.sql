-- =====================================================================
-- V29 — Campos do DPS (NFS-e Nacional) e cancelamento na tabela invoices
-- H2 não suporta múltiplos ADD COLUMN numa só instrução (ver V10) — uma por linha.
-- =====================================================================

ALTER TABLE invoices ADD COLUMN serie_dps            INTEGER;
ALTER TABLE invoices ADD COLUMN numero_dps           BIGINT;
ALTER TABLE invoices ADD COLUMN data_competencia     DATE;
ALTER TABLE invoices ADD COLUMN cancel_justificativa VARCHAR(500);
ALTER TABLE invoices ADD COLUMN cancelled_at         TIMESTAMP;
