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

A ativação de um `StoreMap` é explícita por `status: "ACTIVE"`. Se outro mapa
da mesma loja já estiver ativo, a API retorna `409`; ela não arquiva o mapa
anterior automaticamente, pois o ciclo de publicação ainda é `UNRESOLVED` no
modelo de dados.
