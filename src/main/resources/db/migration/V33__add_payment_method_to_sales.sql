-- =====================================================================
-- V33 — Forma de pagamento do PDV (achado F3)
-- A tela sempre teve o campo "Forma de pagamento", mas nada gravava —
-- a tabela sales nunca teve coluna pra isso, e o lançamento financeiro
-- da venda sempre caía em DINHEIRO por padrão, não importa o escolhido.
-- =====================================================================

ALTER TABLE sales ADD COLUMN payment_method VARCHAR(20);
