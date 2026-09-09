# Market Fast Route

Base técnica do Indoor Supermarket Navigator: uma aplicação web para montar uma lista de compras e, posteriormente, calcular uma rota dentro de uma loja específica.

## Estado atual

Esta etapa contém somente a fundação do projeto:

- frontend React + Vite + TypeScript + shadcn/ui;
- backend Java + Spring Boot;
- PostgreSQL e Redis para desenvolvimento local;
- health check técnico em `/actuator/health`;
- configuração de correlation ID, CORS e erros padronizados;
- testes automatizados básicos.

Ainda não existem lojas, produtos, mapas, grafos, rotas, autenticação ou endpoints de negócio.

## Pré-requisitos

- Node.js compatível com a versão do Vite instalada;
- pnpm 11.19.0;
- Java 21 para o backend;
- Docker Desktop com Docker Compose.

## Configuração local

1. Copie `.env.example` para `.env` e ajuste somente os valores necessários.
2. Instale as dependências do frontend:

   ```powershell
   pnpm install
   ```

3. Inicie PostgreSQL e Redis:

   ```powershell
   docker compose -f infra/compose.yaml up -d postgres redis
   ```

4. Em um terminal, inicie a API:

   ```powershell
   .\apps\api\mvnw.cmd spring-boot:run
   ```

5. Em outro terminal, inicie o frontend:

   ```powershell
   pnpm web:dev
   ```

Frontend: http://localhost:5173
Health da API: http://localhost:8080/actuator/health

## Stack completa via Docker

```powershell
pnpm compose:config
pnpm compose:up
```

Para parar os serviços:

```powershell
pnpm compose:down
```

## Testes e validações

```powershell
pnpm web:test
pnpm web:lint
pnpm web:build
.\apps\api\mvnw.cmd test
git diff --check
```

Os testes de contexto da API usam PostgreSQL via Testcontainers e, portanto, precisam do Docker disponível.

## Decisões ainda futuras

A estratégia de migrations, o contrato da API de negócio, autenticação administrativa, cache Redis e os algoritmos de pathfinding/ordenação serão definidos junto das primeiras funcionalidades. Nenhuma dessas decisões é implementada nesta base.
