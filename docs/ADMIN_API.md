# API administrativa — MVP

Todos os endpoints administrativos usam o prefixo `/api/v1/admin`.

As operações de criação retornam `201 Created`; consultas retornam `200 OK`;
alterações usam `PUT` e retornam `200 OK`. O MVP não expõe `DELETE`: entidades
com `active` usam ativação lógica e `StoreMap` usa `status` (`DRAFT`, `ACTIVE` ou
`ARCHIVED`).

## Catálogo e lojas

| Recurso | Endpoints |
|---|---|
| Store | `GET/POST /stores`, `GET/PUT /stores/{storeId}` |
| Category | `GET/POST /categories`, `GET/PUT /categories/{categoryId}` |
| Product | `GET/POST /products`, `GET/PUT /products/{productId}` |
| StoreProduct | `GET/POST /stores/{storeId}/products`, `GET/PUT /stores/{storeId}/products/{storeProductId}` |
| StoreMap | `GET/POST /stores/{storeId}/maps`, `GET/PUT /stores/{storeId}/maps/{mapId}` |

Mapas devem ser criados como `DRAFT`; enviar `ACTIVE` ou `ARCHIVED` no POST é
rejeitado. Consulte
`GET /maps/{mapId}/validation` para obter `{ mapId, publishable, issues }`.
O relatório valida geometria, referências de navegação, entrada, caixas e
conexões entre os pontos usados pela rota. Ativar um mapa não publicável
retorna `422 MAP_NOT_PUBLISHABLE`; os detalhes incluem o mesmo array de issues
com código, mensagem, tipo e identificador do elemento quando houver.

O ciclo é unidirecional: `DRAFT` pode permanecer rascunho ou virar `ACTIVE`;
`ACTIVE` pode permanecer ativo ou virar `ARCHIVED`; `ARCHIVED` é terminal.
Não é possível pular o arquivamento nem reativar uma versão arquivada. Crie uma
nova versão em rascunho para publicar dados alterados. Ao ativar, o backend
serializa a verificação por loja e bloqueia a versão do mapa enquanto valida.
O índice único do PostgreSQL mantém a garantia final de no máximo um mapa
ativo.

Metadados, estruturas, nós, arestas, POIs e localizações de uma versão
publicada são imutáveis. Criação e atualização desses elementos exige mapa em
`DRAFT`; arquivar altera somente o status da versão ativa.

## Estrutura e grafo do mapa

Os recursos abaixo são escopados por `mapId`:

- `GET/POST /maps/{mapId}/sectors` e `GET/PUT .../{sectorId}`;
- `GET/POST /maps/{mapId}/aisles` e `GET/PUT .../{aisleId}`;
- `GET/POST /maps/{mapId}/shelf-blocks` e `GET/PUT .../{shelfBlockId}`;
- `GET/POST /maps/{mapId}/points-of-interest` e `GET/PUT .../{pointOfInterestId}`;
- `GET/POST /maps/{mapId}/nodes` e `GET/PUT .../{nodeId}`;
- `GET/POST /maps/{mapId}/edges` e `GET/PUT .../{edgeId}`.

As localizações usam:

- `GET/POST /stores/{storeId}/product-locations`;
- `GET/PUT /stores/{storeId}/product-locations/{locationId}`.

## Erros e integridade

- `400` para Bean Validation, enums inválidos e relações/hierarquias inválidas;
- `404` para recursos inexistentes ou fora do escopo da loja/mapa;
- `409` para códigos, SKU/EAN, associações, arestas, localizações principais
  ou mapas `ACTIVE` conflitantes.
- `422` quando um mapa não passa a validação para publicação.

A ativação de um `StoreMap` é explícita por `status: "ACTIVE"`. Se outro mapa
da mesma loja já estiver ativo, a API retorna `409`; o administrador precisa
arquivá-lo explicitamente antes de ativar a nova versão. O processo está
registrado em `docs/ADR-002-map-coordinates-and-publication.md`.
