# TH Piscinas API

API REST para gestão da **TH Piscinas / TH Bombas** — controle de usuários, clientes, produtos, vendas, ordens de serviço e relatórios financeiros, com autenticação JWT e controle de acesso por perfil.

## Tecnologias

- **Java 17**
- **Spring Boot 4.0.6** (Web, Data JPA, Security, Validation)
- **Spring Security + JWT** (jjwt 0.12.6)
- **PostgreSQL** (produção) / **H2** (perfil de desenvolvimento)
- **Flyway** — versionamento e migração do banco
- **MapStruct 1.6.3** — mapeamento entre entidades e DTOs
- **Lombok**
- **SpringDoc OpenAPI / Swagger UI**
- **Maven**

## Arquitetura

Estrutura em camadas (`controller` → `service` → `repository` → `model`), com DTOs de request/response, mappers (MapStruct) e tratamento global de exceções.

```
src/main/java/com/projeto/th_piscinas_api/
├── controller/     # Endpoints REST
├── service/        # Regras de negócio
├── repository/     # Spring Data JPA
├── model/          # Entidades JPA
├── dto/            # Objetos de transferência (request/response)
├── mapper/         # MapStruct (entidade <-> DTO)
├── security/       # JWT, filtros e configuração do Spring Security
├── exception/      # Exceções de negócio e handler global
└── util/           # Enums (perfis, categorias, status, etc.)
```

## Funcionalidades

- **Autenticação & Autorização** — login com JWT, refresh token, perfil autenticado (`/me`) e logout (revogação do refresh token) com controle de acesso por perfil.
- **Usuários** — CRUD e reset de senha (restrito ao `ADM_MASTER`).
- **Clientes** — pessoa física e jurídica, com endereço; cliente cadastrado por vendedor nasce `PENDENTE` e precisa de aprovação/reprovação do ADM.
- **Produtos** — catálogo com categorias (bombas, filtros, motores, peças, acessórios, químicos), código de barras, estoque mínimo e alerta de estoque baixo.
- **Carrinho** — cálculo de subtotais/total e checagem de estoque antes de fechar a venda (não persiste nada).
- **Vendas** — registro de vendas com múltiplos itens e cancelamento (soft cancel, com autorização de ADM).
- **Entrada de Estoque** — registro de lote de compra: soma ao estoque, atualiza o custo do produto e gera despesa (SAÍDA/FORNECEDOR) no financeiro.
- **Pedidos Externos** — vendedor externo monta e envia um pedido para aprovação; o pedido só vira venda (baixando estoque e entrando no financeiro) quando o ADM aprova.
- **Ordens de Serviço** — ciclo completo de status (aberta → orçada → aprovada → agendada → em andamento → concluída/cancelada/reprovada), com precificação/orçamento, aprovação/reprovação, agendamento e início/fim real do atendimento pelo técnico.
- **Área do Técnico** — histórico de atividades, próximas manutenções, clientes atendidos, criação de orçamento e início/fim de serviço.
- **Tarefas de Serviço** — checklist/tarefas vinculadas a uma ordem de serviço e a um técnico.
- **Registros de Serviço** — anotações e fotos (upload multipart) feitos pelo técnico durante o atendimento; as fotos ficam em storage externo, só a referência é persistida.
- **Fornecedores** — cadastro de fornecedores e vínculo com lançamentos de saída (base do relatório de gasto por fornecedor).
- **Contas a Receber** — separa "venda registrada" de "dinheiro recebido": a receita só vira lançamento financeiro quando o ADM confirma o recebimento.
- **Manutenção Preventiva** — planos recorrentes por cliente (frequência em dias + próxima data prevista), apontando clientes em dia, próximos do vencimento ou vencidos.
- **Agenda** — janela de início/fim do horário agendado nas ordens de serviço, usada para montar a agenda do técnico.
- **Colaboradores** — visão consolidada de vendedores/técnicos com histórico de vendas e ordens de serviço.
- **Relatórios Financeiros** — fluxo de lançamentos por período (dia/semana/mês), gasto por fornecedor e relatório de vendas externas.
- **NFS-e** — emissão/consulta simulada de nota de serviço por ordem de serviço, com recibo em HTML ou XML (integração real via FocusNFe é opcional).

### Perfis de acesso

| Perfil | Descrição |
|--------|-----------|
| `ADM_MASTER` | Acesso total (usuários, relatórios, etc.) |
| `VENDEDOR_INTERNO` | Vendas |
| `VENDEDOR_EXTERNO` | Vendas |
| `TECNICO_CONDOMINIAL` | Serviços técnicos |

## Endpoints principais

| Recurso | Base path |
|---------|-----------|
| Autenticação | `/api/auth` (`/login`, `/refresh`, `/me`, `/logout`) |
| Usuários | `/api/users` (`/{id}/resetPassword`) |
| Clientes | `/api/clients` (`/pending`, `/{id}/approve`, `/{id}/reject`) |
| Produtos | `/api/products` (`/low-stock`, `/search`, `/barcode/{barcode}`) |
| Carrinho | `POST /api/cart/calculate` |
| Vendas | `/api/sales` (`/{id}/cancel`) |
| Entradas de estoque | `/api/stock-entries` |
| Pedidos externos | `/api/external-orders` (`/mine`, `/{id}/approve`, `/{id}/reject`) |
| Vendas externas (relatório) | `GET /api/admin/external-sales` |
| Ordens de serviço | `/api/serviceorders` |
| Aprovação/precificação de OS | `/api/admin/service-orders/{id}` (`/price`, `/approve`, `/reject`) |
| Agendamento | `/api/admin/service-orders/{id}/schedule`, `/api/admin/agenda` |
| Área do técnico | `/api/technician` (`/activities`, `/next-maintenance`, `/clients`, `/budgets`, `/service-orders/{id}/start`, `/service-orders/{id}/finish`) |
| Tarefas de serviço | `/api/servicetasks` |
| Registros de serviço (OS) | `/api/technician/service-orders/{id}/records` (multipart: nota + fotos) |
| NFS-e | `/api/nfse` (`/status`, `/emit`, `/{id}/consult`, `/{id}/simulado`) |
| Fornecedores | `/api/suppliers` (`/spending` p/ relatório de gastos) |
| Contas a receber | `/api/admin/receivables` (`/{id}/confirm`) |
| Planos de manutenção | `/api/admin/maintenance-plans` (`/due`, `/{id}/register`, `/{id}/release`) |
| Colaboradores | `/api/admin/collaborators` (`/{id}` p/ detalhe) |
| Relatórios financeiros | `/api/admin/reports/flow`, `POST /api/admin/reports` |

Documentação interativa disponível via **Swagger UI** em `/swagger-ui.html` (OpenAPI em `/v3/api-docs`).

## Pré-requisitos

- JDK 17+
- Maven 3.9+
- PostgreSQL (apenas para o perfil `prod`)

## Configuração

A aplicação usa o perfil `dev` por padrão (banco H2 em arquivo). O perfil `dev`
já traz valores padrão para `JWT_SECRET`, `JWT_EXPIRATION_MINUTES` e o usuário
administrador inicial (`admin` / `admin123`), então sobe sem nenhuma configuração
extra. Para personalizar o ADM inicial, exporte antes do primeiro start:

```bash
export ADMIN_MATRICULA="seu-usuario"
export ADMIN_SENHA="sua-senha"
export ADMIN_NOME="Nome do ADM"
```

> No primeiro start, um usuário `ADM_MASTER` é criado automaticamente. **Troque a senha no primeiro acesso.**
> Os defaults do perfil `dev` são apenas para desenvolvimento — em produção todas
> essas variáveis (inclusive `JWT_SECRET`) devem vir do ambiente.

### Variáveis de produção (perfil `prod`)

```bash
export SPRING_PROFILES_ACTIVE=prod
export DATABASE_URL=jdbc:postgresql://host:5432/th_piscinas
export DATABASE_USERNAME=usuario
export DATABASE_PASSWORD=senha
```

### Integração NFS-e (FocusNFe) — opcional

```bash
export FOCUSNFE_TOKEN=...
export NFSE_CNPJ_PRESTADOR=...
export NFSE_INSCRICAO_MUNICIPAL=...
# demais variáveis NFSE_* conforme application.yaml
```

## Executando

```bash
# Desenvolvimento (H2)
./mvnw spring-boot:run

# Build do .jar
./mvnw clean package
java -jar target/th-piscinas-api-0.0.5.jar
```

A API sobe em `http://localhost:4015`.

- **Swagger UI:** http://localhost:4015/swagger-ui.html
- **H2 Console** (perfil dev): http://localhost:4015/h2-console

## Banco de dados

O schema é gerenciado por **Flyway** (`src/main/resources/db/migration`), com `ddl-auto: validate` — o Hibernate apenas valida o schema, que é criado/evoluído pelas migrations:

| Versão | Tabela / mudança |
|--------|------------------|
| V1 | usuários |
| V2 | lançamentos financeiros |
| V3 | produtos |
| V4 | vendas e itens de venda |
| V5 | clientes |
| V6 | ordens de serviço |
| V10 | aprovação/precificação de OS (`approved_by_id`, `approved_at`, `rejection_reason`) |
| V11 | controle de estoque nos produtos (`barcode`, `min_stock`, `unit`) |
| V12 | planos de manutenção preventiva |
| V13 | estoque mínimo do produto (alerta de estoque baixo) |
| V14 | código de barras do produto (frente de caixa) |
| V15 | cancelamento de venda (soft cancel, com autorização de ADM) |
| V16 | fornecedores + vínculo do lançamento financeiro ao fornecedor |
| V17 | entrada de lote de produtos (compra de mercadoria) |
| V18 | aprovação de cliente cadastrado por vendedor |
| V19 | pedido do vendedor externo |
| V20 | fim do horário agendado na ordem de serviço (`scheduled_end`) |
| V21 | NFS-e (invoices) |
| V22 | contas a receber |
| V23 | tarefas de serviço (service tasks) |
| V24 | liberação (`released`) e técnico do plano de manutenção |
| V25 | início real do serviço na ordem de serviço (`started_at`) |
| V26 | registros de serviço do técnico (texto + fotos) |

> As migrations usam uma instrução `ALTER TABLE` por coluna (em vez de múltiplos `ADD COLUMN` separados por vírgula) para serem compatíveis tanto com H2 (dev) quanto com PostgreSQL (prod).

## Testes

```bash
./mvnw test
```

## Licença

Projeto proprietário — TH Piscinas / TH Bombas.
