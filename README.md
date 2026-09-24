# Market Fast Route

Market Fast Route é uma aplicação web para pesquisar produtos de uma unidade,
montar uma lista e visualizar a rota dentro de sua loja.

## Estado atual

O MVP está implementado e aceito em escopo local/rede privada (2026-09-23):

- fluxo de cliente para escolher unidade, pesquisar produtos, montar lista,
  calcular a rota na API e acompanhá-la no mapa SVG;
- área administrativa para catálogo e unidades, edição de mapas versionados,
  validação e ativação segura de uma versão;
- maquete autoral de mercado fictício em Three.js/WebGL, separada do mapa real,
  carregada sob demanda e com fallback sem WebGL e suporte a redução de movimento;
- backend Spring Boot, PostgreSQL/Flyway e seed opt-in de demonstração;
- execução privada com Docker Compose e binds locais por padrão.

A direção visual aprovada é **Atlas interativo da loja**, composição **02 —
Busca e lista em primeiro plano**. A auditoria, as decisões técnicas, referências,
checklist, comando `/go` e evidências de aceite estão em
[`docs/MVP-IMPLEMENTATION-PLAN.md`](docs/MVP-IMPLEMENTATION-PLAN.md). As decisões
visuais estão em [`DESIGN.md`](DESIGN.md), e o produto/limites em
[`PRODUCT.md`](PRODUCT.md), [`docs/PRD.md`](docs/PRD.md) e
[`docs/SCOPE.md`](docs/SCOPE.md).

## Pré-requisitos

- Node.js compatível com a versão do Vite instalada;
- pnpm 11.19.0;
- Java 21 para o backend;
- Docker Desktop com Docker Compose.

## Desenvolvimento via Docker Compose

Antes de iniciar, copie `.env.example` para `.env`. Os valores padrão permitem iniciar o ambiente; edite as portas no `.env` quando houver serviços locais usando as portas padrão.

Comando principal:

```powershell
docker compose --env-file .env -f infra/compose.yaml up --build
```

O Compose inicia os serviços `postgres`, `api` e `web`. O frontend
possui hot reload por volume montado e o backend executa `spring-boot:run` em
modo de desenvolvimento.

Para validar a configuração sem iniciar os serviços:

```powershell
docker compose --env-file .env -f infra/compose.yaml config
```

Para executar em segundo plano:

```powershell
docker compose --env-file .env -f infra/compose.yaml up --build -d
```

Para parar os serviços preservando os dados do PostgreSQL:

```powershell
docker compose --env-file .env -f infra/compose.yaml down
```

`docker compose down -v` remove também o volume persistente do PostgreSQL.

O MVP não tem login administrativo. Para demonstrar em uma rede privada
confiável, configure `WEB_BIND_ADDRESS=0.0.0.0` e inclua a origem exata
`http://<ip-da-maquina>:5173` em `CORS_ALLOWED_ORIGINS` no `.env`. Depois
acesse essa origem pelo dispositivo da rede. Esse acesso também permite
operações administrativas. Não use essa configuração em uma rede pública.

## URLs locais

- Frontend: http://localhost:5173
- Health da API: http://localhost:8080/actuator/health
- PostgreSQL: `localhost:5432`, banco `supermarket`

As chamadas do frontend usam `/api`. O Vite encaminha esse prefixo para
`http://api:8080` dentro da rede do Compose; o endpoint `/api/actuator/health`
é encaminhado para o Actuator da API.

## Execução local fora do Compose

```powershell
pnpm install
pnpm web:dev
.\apps\api\mvnw.cmd spring-boot:run
```

Nesse modo, PostgreSQL deve estar acessível no host e a variável
`POSTGRES_HOST` deve apontar para `localhost`. Para o fluxo
completo e reproduzível, use o Compose acima.
O servidor Vite iniciado diretamente no host escuta apenas em `127.0.0.1` por
padrão. O Compose configura o bind interno do contêiner separadamente; a porta
publicada continua restrita por `WEB_BIND_ADDRESS`.

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

Os testes de contexto da API usam PostgreSQL via Testcontainers e precisam do
Docker disponível. Para um mercado fictício completo, consulte
`docs/DEMO-DATA.md`.

## Decisões e plano do MVP

A estratégia de routing está registrada em `docs/ADR-001-routing.md`; as
coordenadas e regras de publicação do mapa, em
`docs/ADR-002-map-coordinates-and-publication.md`; e o limite da cena Three.js,
em `docs/ADR-003-threejs-demonstration.md`. O plano completo, com prompt
preparado para `/go`, está em `docs/MVP-IMPLEMENTATION-PLAN.md`.
