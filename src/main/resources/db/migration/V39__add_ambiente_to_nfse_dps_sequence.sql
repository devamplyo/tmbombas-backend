-- =====================================================================
-- V39 — Numeração de DPS separada por ambiente
-- Homologação e produção passam a ter contadores independentes: a produção
-- real não herda os números gastos nos testes. A linha que já existe é de
-- homologação e pula para 999 (o próximo é 1000), porque os números baixos
-- já foram usados em testes anteriores e a Focus recusa número repetido.
-- =====================================================================

ALTER TABLE nfse_dps_sequence ADD COLUMN ambiente VARCHAR(20) NOT NULL DEFAULT 'HOMOLOGACAO';
ALTER TABLE nfse_dps_sequence ALTER COLUMN ambiente DROP DEFAULT;

ALTER TABLE nfse_dps_sequence DROP CONSTRAINT uk_nfse_dps_sequence_cnpj_serie;
ALTER TABLE nfse_dps_sequence
    ADD CONSTRAINT uk_nfse_dps_sequence_cnpj_serie_ambiente UNIQUE (cnpj, serie, ambiente);

UPDATE nfse_dps_sequence SET ultimo_numero = 999 WHERE ambiente = 'HOMOLOGACAO' AND ultimo_numero < 999;
