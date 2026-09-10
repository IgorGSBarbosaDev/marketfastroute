# Architecture — Indoor Supermarket Navigator

## 1. Architecture Overview

O projeto será desenvolvido como um monorepo contendo frontend, backend, infraestrutura e documentação.

Stack oficial:

### Frontend

- TypeScript
- React
- Vite
- shadcn/ui

### Backend

- Java
- Spring Boot

### Database

- PostgreSQL

### Cache / Ephemeral Data

- Redis

### Infrastructure

- Docker
- Docker Compose para ambiente local

Tecnologias fora desta lista não devem ser adicionadas sem decisão explícita.

---

# 2. Monorepo Structure

Estrutura inicial recomendada:

```text
/
├── apps/
│   ├── web/
│   │   ├── src/
│   │   ├── public/
│   │   ├── package.json
│   │   └── vite.config.ts
│   │
│   └── api/
│       ├── src/
│       ├── pom.xml
│       └── Dockerfile
│
├── packages/
│   ├── ui/
│   ├── config/
│   └── types/
│
├── infra/
│   ├── docker/
│   └── compose.yaml
│
├── docs/
│   ├── PRD.md
│   ├── SCOPE.md
│   └── ARCHITECTURE.md
│
├── .env.example
├── .gitignore
├── README.md
└── AGENTS.md
```

A estrutura pode evoluir, mas mudanças relevantes devem ser documentadas.

---

# 3. Frontend Architecture

## 3.1 Stack

Frontend oficial:

```text
TypeScript
React
Vite
shadcn/ui
```

Não utilizar Next.js neste projeto sem decisão explícita.

---

## 3.2 Responsibilities

O frontend é responsável por:

- interface do cliente;
- interface administrativa;
- busca e seleção de produtos;
- lista de compras;
- renderização do mapa;
- interação com o mapa;
- visualização da rota;
- comunicação com API.

O frontend não deve:

- implementar regra oficial de cálculo de rota;
- acessar PostgreSQL diretamente;
- acessar Redis diretamente;
- conter lógica de domínio crítica duplicada do backend.

---

## 3.3 Suggested Frontend Structure

```text
apps/web/src/
├── app/
├── components/
├── features/
│   ├── stores/
│   ├── products/
│   ├── shopping-list/
│   ├── map/
│   ├── routing/
│   └── admin/
├── hooks/
├── lib/
├── services/
├── types/
└── main.tsx
```

Organização por feature deve ser preferida para regras específicas do produto.

---

# 4. UI

## 4.1 shadcn/ui

shadcn/ui será a base para componentes de interface comuns.

Exemplos:

- buttons;
- dialogs;
- inputs;
- selects;
- sheets;
- tables;
- forms;
- cards;
- command/search;
- toast.

O mapa não deve ser forçado para dentro de abstrações do shadcn/ui.

---

## 4.2 Map Rendering

Para o MVP, a preferência arquitetural é um mapa web vetorial/interativo.

Abordagem inicial recomendada:

- SVG interativo.

Possíveis evoluções:

- Canvas;
- PixiJS;
- WebGL.

Mudanças devem ocorrer apenas quando SVG deixar de atender aos requisitos de performance ou experiência.

Não utilizar engine 3D no MVP.

---

# 5. Backend Architecture

## 5.1 Stack

Backend oficial:

```text
Java
Spring Boot
```

---

## 5.2 Responsibilities

O backend é responsável por:

- regras de negócio;
- lojas;
- produtos;
- categorias;
- localizações;
- estrutura do mapa;
- grafo de navegação;
- cálculo de rota;
- persistência;
- validações;
- cache;
- API.

---

## 5.3 Suggested Backend Structure

Preferência por módulos de domínio/feature.

```text
apps/api/src/main/java/.../
├── store/
├── product/
├── catalog/
├── map/
├── routing/
├── shopping/
├── admin/
├── shared/
└── config/
```

Cada módulo pode conter:

```text
controller
service
repository
domain
dto
mapper
```

Não criar abstrações genéricas sem necessidade real.

---

# 6. API Style

A comunicação inicial entre frontend e backend será via REST API.

Formato:

```text
JSON over HTTP
```

Exemplo de prefixo:

```text
/api/v1
```

Exemplos conceituais:

```text
GET  /api/v1/stores
GET  /api/v1/stores/{storeId}/products
GET  /api/v1/stores/{storeId}/map
POST /api/v1/routes
```

Os endpoints públicos operam somente sobre lojas ativas. Uma loja inativa é
tratada como indisponível e retorna `STORE_NOT_FOUND`, assim como uma loja
inexistente. Os endpoints públicos de localização de produtos resolvem
automaticamente o mapa `ACTIVE` da loja; o `mapId` não faz parte da URL e
localizações pertencentes a mapas `DRAFT` ou `ARCHIVED` permanecem disponíveis
apenas para administração. Se uma loja ativa não possuir mapa ativo, o fluxo
retorna `STORE_MAP_NOT_FOUND`.

Endpoints finais devem ser definidos em contrato de API antes de estabilização.

---

# 7. PostgreSQL

PostgreSQL será a fonte persistente principal.

Dados persistentes incluem:

- lojas;
- produtos;
- categorias;
- setores;
- corredores;
- pontos de interesse;
- estruturas do mapa;
- posições;
- nós;
- conexões;
- localização por loja.

Redis não substitui PostgreSQL como fonte oficial desses dados.

---

# 8. Redis

Redis será utilizado apenas quando houver benefício claro.

Possíveis usos:

- cache de mapa;
- cache de catálogo;
- cache de cálculos de rota;
- dados temporários;
- rate limiting;
- sessões administrativas, se necessário.

Regra:

> Toda informação que precise sobreviver à perda do Redis deve estar persistida em PostgreSQL.

Redis não deve ser introduzido em fluxos que não necessitem de cache ou dado efêmero.

---

# 9. Domain Model — High Level

Entidades conceituais esperadas:

```text
Store
Product
Category
Sector
Aisle
ShelfBlock
PointOfInterest
ProductLocation
MapNode
MapEdge
Route
RouteStop
```

Relacionamento principal:

```text
Store
 ├── Map
 │    ├── Sector
 │    ├── Aisle
 │    ├── ShelfBlock
 │    ├── PointOfInterest
 │    ├── MapNode
 │    └── MapEdge
 │
 └── ProductLocation
      └── Product
```

O mesmo produto pode existir em várias lojas com localizações diferentes.

---

# 10. Routing Architecture

O módulo `routing` deve ser independente da camada de apresentação.

Entrada conceitual:

```text
storeId
startNode
productIds[]
destination
```

Saída conceitual:

```text
orderedStops[]
path[]
distance
metadata
```

O algoritmo concreto deve ficar encapsulado.

Isso permite trocar a estratégia sem alterar controllers ou frontend.

---

# 11. Route Algorithm Boundaries

Devem existir responsabilidades separadas para:

### Pathfinding

Menor caminho entre dois pontos.

### Stop Ordering

Escolha da ordem dos produtos.

### Route Composition

Composição do percurso completo.

Exemplo:

```text
Stop Ordering
      ↓
Pathfinding entre cada par
      ↓
Route Composition
```

Não acoplar toda a lógica em uma única classe ou endpoint.

---

# 12. Docker

Todo ambiente de desenvolvimento deve ser reproduzível via Docker.

Serviços esperados:

```text
web
api
postgres
redis
```

Para desenvolvimento, pode ser vantajoso executar frontend e backend localmente com apenas dependências em containers.

A configuração deve permitir ambos os modos.

---

# 13. Docker Compose

O ambiente local deve possuir um arquivo central de Compose.

Exemplo conceitual:

```text
infra/compose.yaml
```

Serviços:

```text
postgres
redis
api
web
```

Credenciais locais devem vir de variáveis de ambiente.

Segredos reais não devem ser versionados.

---

# 14. Environment Configuration

Variáveis devem possuir documentação em:

```text
.env.example
```

Exemplos:

```text
POSTGRES_DB
POSTGRES_USER
POSTGRES_PASSWORD
REDIS_HOST
REDIS_PORT
API_PORT
VITE_API_URL
```

Não armazenar secrets reais no repositório.

---

# 15. Caching Strategy

Cache não deve ser tratado como requisito automático.

Utilizar Redis quando medições ou requisitos justificarem.

Possíveis chaves:

```text
store:{storeId}:map
store:{storeId}:catalog
route:{hash}
```

Toda estratégia de invalidação deve ser explícita.

---

# 16. Error Handling

Backend deve utilizar formato padronizado para erros.

Exemplo conceitual:

```json
{
  "code": "PRODUCT_NOT_FOUND",
  "message": "Product not found",
  "details": {}
}
```

Não retornar stack trace ao cliente.

---

# 17. Validation

Backend é a autoridade final para validação.

Frontend pode validar para UX, mas não substitui validação do servidor.

---

# 18. Observability

Mínimo esperado:

- logs estruturados;
- request correlation id;
- health check;
- métricas básicas quando necessário.

Spring Boot Actuator pode ser utilizado.

---

# 19. Testing Strategy

## Frontend

- testes unitários para lógica;
- testes de componentes quando relevante;
- testes de fluxo crítico.

## Backend

- testes unitários;
- testes de serviço;
- testes de repository;
- testes de integração;
- testes específicos do algoritmo de rota.

O módulo de routing deve possuir casos determinísticos de teste.

---

# 20. Architecture Rules for AI Agents

Agentes devem seguir:

1. Ler `PRD.md`, `SCOPE.md` e `ARCHITECTURE.md` antes de implementar features.
2. Não alterar stack sem decisão explícita.
3. Não adicionar Next.js.
4. Não adicionar Node.js como backend.
5. Não adicionar banco diferente de PostgreSQL.
6. Não substituir Redis por outra solução sem decisão.
7. Não implementar recursos fora do MVP.
8. Não adicionar localização indoor.
9. Não adicionar QR Code.
10. Não implementar mapa 3D real.
11. Não duplicar regras de domínio no frontend.
12. Não acessar banco pelo frontend.
13. Não criar microserviços sem necessidade documentada.
14. Tratar arquitetura atual como monólito modular.
15. Registrar decisões relevantes antes de alterar limites arquiteturais.

---

# 21. Initial Architectural Style

A arquitetura inicial será:

> Monorepo + frontend SPA + backend monólito modular + PostgreSQL + Redis.

Não utilizar microserviços no MVP.

Fluxo principal:

```text
React/Vite
    ↓
Spring Boot REST API
    ↓
Domain / Application Logic
    ↓
PostgreSQL
    ↓
Redis quando aplicável
```

---

# 22. Future Evolution

A arquitetura deve permitir, sem implementar agora:

- integração com ERP;
- importação de catálogo;
- atualização automática de localizações;
- aplicativo mobile;
- analytics;
- autenticação de clientes;
- sistemas externos.

Essas possibilidades não fazem parte do escopo atual.
