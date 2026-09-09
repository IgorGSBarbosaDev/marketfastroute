# ADR-001 — Estratégia de routing do MVP

## Status

Accepted

## Decisão

O cálculo de menor caminho usa Dijkstra. `map_edge.distance_meters` é o custo
oficial do grafo e as coordenadas dos nós estão em unidades locais do mapa; por
isso não há garantia contratual de que a distância geométrica seja uma
heurística admissível para A*. O algoritmo considera somente nós e arestas
ativos do `StoreMap` carregado.

A ordem de múltiplas paradas usa uma heurística gulosa de vizinho mais próximo:
em cada passo, o próximo produto é aquele cuja localização primária ativa está
mais próxima pelo menor caminho do grafo. Se o produto não tiver primária, sua
localização ativa de menor `productLocationId` é usada. Empates são resolvidos por
`productId` e depois `productLocationId`, tornando o resultado previsível. A
heurística não tenta resolver TSP exatamente.

`RouteComposer` calcula os segmentos na ordem escolhida, remove a repetição do
nó de junção entre segmentos e soma as distâncias dos segmentos. A rota é
montada em memória e não é persistida.

## Endpoints do percurso

São preferidos pontos de interesse ativos e navegáveis do tipo `ENTRANCE` e
`CHECKOUT`. Na ausência deles, um nó ativo do grafo com o tipo correspondente é
usado como compatibilidade com o modelo existente. Mais de uma opção ativa
causa erro de configuração, pois a regra de seleção entre múltiplas entradas ou
áreas de caixas ainda não foi definida no produto.

## Consequências

- O algoritmo concreto permanece atrás da interface `PathFinder`.
- A heurística pode não produzir a rota globalmente ótima, mas é simples,
  determinística e adequada ao MVP.
- A localização primária ativa é preferida; quando ela não existe, a primeira
  localização ativa em ordem de `productLocationId` é usada. Sem localização
  ativa válida no mapa ativo, o produto não pode virar parada de rota.
