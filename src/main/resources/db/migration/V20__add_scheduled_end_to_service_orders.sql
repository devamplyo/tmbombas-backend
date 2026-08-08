-- =====================================================================
-- V20 — Agendamento: fim do horário agendado na Ordem de Serviço
-- scheduled_date = início; scheduled_end = fim. Juntos formam a janela
-- usada para montar a agenda e detectar conflitos de horário do técnico.
-- =====================================================================

ALTER TABLE service_orders
    ADD COLUMN scheduled_end TIMESTAMP;