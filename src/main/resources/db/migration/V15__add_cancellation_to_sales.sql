-- =====================================================================
-- V15 — Cancelamento de venda (com autorização de ADM)
-- Soft cancel: a venda não é apagada, vira CANCELADA, preservando o
-- histórico e registrando quem autorizou, quando e por quê.
-- =====================================================================

ALTER TABLE sales ADD COLUMN status                 VARCHAR(20) NOT NULL DEFAULT 'ATIVA';
ALTER TABLE sales ADD COLUMN cancelled_at           TIMESTAMP;
ALTER TABLE sales ADD COLUMN cancellation_reason    VARCHAR(500);
ALTER TABLE sales ADD COLUMN authorized_by_admin_id BIGINT;

-- ADM que autorizou. SET NULL preserva a venda se o usuário for removido.
ALTER TABLE sales
    ADD CONSTRAINT fk_sales_authorized_by_admin
    FOREIGN KEY (authorized_by_admin_id) REFERENCES usuarios (id)
    ON DELETE SET NULL;

CREATE INDEX idx_sales_status ON sales (status);