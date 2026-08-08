-- =====================================================================
-- V10 — Aprovação e precificação de OS / Orçamentos
-- Rastreia quem aprovou, quando, e o motivo em caso de reprovação.
-- O novo status REPROVADA não precisa de migration (coluna é VARCHAR).
-- =====================================================================

-- Uma instrução ALTER por coluna: sintaxe aceita tanto pelo H2 (dev)
-- quanto pelo PostgreSQL (prod). O H2 não suporta múltiplos ADD COLUMN
-- separados por vírgula em um único ALTER TABLE.
ALTER TABLE service_orders ADD COLUMN approved_by_id   BIGINT;
ALTER TABLE service_orders ADD COLUMN approved_at      TIMESTAMP;
ALTER TABLE service_orders ADD COLUMN rejection_reason VARCHAR(500);

-- Quem aprovou (users). SET NULL preserva a OS se o usuário for removido.
ALTER TABLE service_orders
    ADD CONSTRAINT fk_service_orders_approved_by
    FOREIGN KEY (approved_by_id) REFERENCES usuarios (id)
    ON DELETE SET NULL;