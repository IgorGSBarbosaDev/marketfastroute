# Market Fast Route

Base técnica do Indoor Supermarket Navigator: uma aplicação web para montar uma lista de compras e, posteriormente, calcular uma rota dentro de uma loja específica.

## Estado atual

Esta etapa contém a fundação do projeto e o primeiro fluxo de routing no
backend:

- frontend React + Vite + TypeScript + shadcn/ui;
- backend Java + Spring Boot;
- PostgreSQL e Redis para desenvolvimento local;
- health check técnico em `/actuator/health`;
- configuração de correlation ID, CORS e erros padronizados;
- Docker Compose para subir frontend, backend, PostgreSQL e Redis juntos;
- Flyway configurado com o schema relacional do MVP versionado em
  `apps/api/src/main/resources/db/migration`;
- cálculo de rota em `POST /api/v1/routes`, sem persistência da rota;
- testes automatizados básicos.

Os dados administrativos ainda precisam ser cadastrados para produzir uma
rota; autenticação administrativa continua fora desta etapa.

## Pré-requisitos

- Node.js compatível com a versão do Vite instalada;
- pnpm 11.19.0;
- Java 21 para o backend;
- Docker Desktop com Docker Compose.

## Desenvolvimento via Docker Compose

Copie `.env.example` para `.env` se quiser configurar credenciais ou portas.
Os valores padrão já permitem iniciar o ambiente.

Comando principal:

```powershell
docker compose -f infra/compose.yaml up --build
```

O Compose inicia os serviços `postgres`, `redis`, `api` e `web`. O frontend
possui hot reload por volume montado e o backend executa `spring-boot:run` em
modo de desenvolvimento.

Para validar a configuração sem iniciar os serviços:

```powershell
docker compose -f infra/compose.yaml config
```

Para executar em segundo plano:

```powershell
docker compose -f infra/compose.yaml up --build -d
```

Para parar os serviços preservando os dados do PostgreSQL:

```powershell
docker compose -f infra/compose.yaml down
```

`docker compose down -v` remove também o volume persistente do PostgreSQL.

## URLs locais

- Frontend: http://localhost:5173
- Health da API: http://localhost:8080/actuator/health
- PostgreSQL: `localhost:5432`, banco `supermarket`
- Redis: `localhost:6379`

As chamadas do frontend usam `/api`. O Vite encaminha esse prefixo para
`http://api:8080` dentro da rede do Compose; o endpoint `/api/actuator/health`
é encaminhado para o Actuator da API.

## Execução local fora do Compose

```powershell
pnpm install
pnpm web:dev
.\apps\api\mvnw.cmd spring-boot:run
```

Nesse modo, PostgreSQL e Redis devem estar acessíveis no host e as variáveis
`POSTGRES_HOST` e `REDIS_HOST` devem apontar para `localhost`. Para o fluxo
completo e reproduzível, use o Compose acima.

Os atalhos equivalentes são:

```powershell
pnpm compose:config
pnpm compose:up
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

Autenticação administrativa e uso de cache Redis continuam futuros. A
estratégia de routing do MVP está registrada em
`docs/ADR-001-routing.md` e implementada no backend.
