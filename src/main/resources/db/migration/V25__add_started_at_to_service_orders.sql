-- =====================================================================
-- V25 — Início real do serviço
-- started_at = quando o técnico DE FATO iniciou (distinto do scheduled_date,
-- que é o horário agendado). O fim real reaproveita o completed_at.
-- =====================================================================

ALTER TABLE service_orders
    ADD COLUMN started_at TIMESTAMP;