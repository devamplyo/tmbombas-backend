-- =====================================================================
-- V17 — Aprovação de cliente cadastrado por vendedor
-- Cliente criado por vendedor nasce PENDENTE e precisa de aprovação do
-- ADM. Criado pelo ADM já nasce APROVADO. Clientes existentes viram
-- APROVADO (DEFAULT) para não quebrar a constraint NOT NULL.
-- =====================================================================

ALTER TABLE clients ADD COLUMN status            VARCHAR(20) NOT NULL DEFAULT 'APROVADO';
ALTER TABLE clients ADD COLUMN created_by_id     BIGINT;
ALTER TABLE clients ADD COLUMN approved_by_id    BIGINT;
ALTER TABLE clients ADD COLUMN approved_at       TIMESTAMP;
ALTER TABLE clients ADD COLUMN rejection_reason  VARCHAR(500);

ALTER TABLE clients
    ADD CONSTRAINT fk_clients_created_by
    FOREIGN KEY (created_by_id) REFERENCES usuarios (id) ON DELETE SET NULL;

ALTER TABLE clients
    ADD CONSTRAINT fk_clients_approved_by
    FOREIGN KEY (approved_by_id) REFERENCES usuarios (id) ON DELETE SET NULL;

CREATE INDEX idx_clients_status ON clients (status);