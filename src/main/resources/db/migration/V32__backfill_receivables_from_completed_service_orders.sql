-- =====================================================================
-- V32 — Backfill de contas a receber das OS já concluídas
--
-- O gancho que abre a conta a receber ao concluir uma OS passou a existir
-- agora. Ordens concluídas antes disso não têm registro em `receivables` e
-- por isso nunca apareceriam na tela Financeiro.
--
-- Idempotente pelo NOT EXISTS: rodar de novo não duplica.
-- Sem `::date` e sem funções específicas do Postgres — a mesma migration
-- roda no H2 do perfil dev.
-- =====================================================================

INSERT INTO receivables (source_type, source_id, client_name, description,
                         amount, due_date, status, created_at)
SELECT 'ORDEM_SERVICO',
       so.id,
       c.name,
       SUBSTR(CASE
                  WHEN so.title IS NULL OR so.title = '' THEN so.order_number
                  ELSE so.order_number || ' - ' || so.title
              END, 1, 255),
       so.price,
       CAST(COALESCE(so.completed_at, CURRENT_TIMESTAMP) AS DATE),
       'PENDENTE',
       CURRENT_TIMESTAMP
FROM service_orders so
LEFT JOIN clients c ON c.id = so.client_id
WHERE so.status = 'CONCLUIDA'
  AND so.price IS NOT NULL
  AND so.price > 0
  AND NOT EXISTS (
      SELECT 1
      FROM receivables r
      WHERE r.source_type = 'ORDEM_SERVICO'
        AND r.source_id = so.id
  );
