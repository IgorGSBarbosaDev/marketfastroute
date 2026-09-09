# PRD — Indoor Supermarket Navigator

## 1. Product Summary

Sistema web para navegação interna em supermercados de grande porte.

O cliente seleciona uma loja, escolhe os produtos que deseja comprar e recebe uma rota otimizada dentro da loja, visualizada em um mapa indoor com aparência 2.5D inspirada em mapas como Google Maps e Waze.

O objetivo principal é reduzir o tempo e a distância percorrida pelo cliente dentro do supermercado.

---

## 2. Problem

Supermercados e atacarejos de grande porte possuem:

- grande variedade de produtos;
- corredores extensos;
- setores distribuídos em grandes áreas;
- múltiplas entradas;
- dificuldade de localização mesmo com placas;
- clientes que percorrem caminhos desnecessários para completar uma compra.

O problema central é:

> O cliente sabe quais produtos deseja comprar, mas não conhece a rota mais eficiente para encontrá-los dentro da loja.

---

## 3. Product Goal

Permitir que um cliente:

1. selecione uma unidade do supermercado;
2. pesquise produtos disponíveis naquela unidade;
3. monte sua lista de compras;
4. solicite a melhor rota;
5. visualize o trajeto completo no mapa;
6. localize os produtos com o mínimo possível de interação com o sistema.

---

## 4. Product Principles

### 4.1 Minimal Interaction

Durante a compra, o sistema deve exigir o mínimo possível de interação.

Fluxo esperado:

```text
Selecionar loja
→ adicionar produtos
→ calcular rota
→ visualizar mapa
→ seguir percurso
→ chegar aos caixas
```

O usuário não deve precisar confirmar manualmente cada produto coletado.

### 4.2 Route First

A principal funcionalidade do produto é calcular a melhor ordem possível para visitar todos os produtos da lista.

### 4.3 Store-Specific Maps

Cada unidade deve possuir seu próprio mapa.

Duas unidades da mesma rede podem possuir:

- layouts diferentes;
- corredores diferentes;
- setores diferentes;
- entradas diferentes;
- caixas diferentes;
- produtos em posições diferentes.

### 4.4 Visual Orientation

O mapa deve priorizar orientação e legibilidade.

Não é objetivo do MVP criar um ambiente 3D realista.

---

## 5. Target Users

### Primary

Clientes de supermercados e atacarejos de grande porte.

### Secondary

Administradores e funcionários responsáveis por:

- estrutura da loja;
- cadastro de produtos;
- localização dos produtos;
- manutenção do mapa.

---

## 6. Main User Journey

### Customer

1. acessar o sistema;
2. selecionar uma loja;
3. buscar produtos;
4. adicionar produtos à lista;
5. solicitar cálculo da rota;
6. visualizar a rota no mapa;
7. seguir a rota dentro da loja;
8. chegar à área de caixas.

---

## 7. Core Features

### 7.1 Store Selection

O cliente deve conseguir selecionar a unidade onde realizará a compra.

Cada unidade possui dados independentes.

---

### 7.2 Product Search

O cliente deve conseguir pesquisar produtos disponíveis na unidade selecionada.

Pesquisa mínima por:

- nome;
- categoria;
- SKU;
- EAN, quando aplicável.

---

### 7.3 Shopping List

O cliente deve conseguir:

- adicionar produtos;
- remover produtos;
- visualizar a lista;
- alterar quantidade.

A quantidade não altera a rota quando todas as unidades do produto estiverem na mesma localização.

---

### 7.4 Route Calculation

O sistema deve calcular uma rota contendo:

```text
ponto inicial
→ produto 1
→ produto 2
→ ...
→ produto N
→ área de caixas
```

A ordem dos produtos deve ser definida pelo sistema.

A ordem de inclusão na lista não deve determinar a ordem da rota.

---

### 7.5 Indoor Map

O sistema deve exibir um mapa específico da loja.

O mapa pode conter:

- estacionamento;
- entradas;
- saídas;
- carrinhos;
- caixas;
- corredores;
- prateleiras;
- setores;
- ilhas;
- pontos de interesse;
- produtos da rota.

---

### 7.6 Route Visualization

A rota deve ser desenhada visualmente sobre o mapa.

O usuário deve conseguir identificar:

- ponto inicial;
- direção geral;
- produtos;
- ordem dos produtos;
- destino final;
- setores;
- corredores.

---

### 7.7 Product Location

Cada produto deve possuir uma localização específica para cada loja.

A localização pode conter:

- setor;
- corredor;
- lado;
- bloco;
- módulo;
- prateleira;
- ponto de navegação associado.

---

## 8. Map Requirements

O mapa deve possuir visual 2.5D ou pseudo-3D.

Características esperadas:

- visão superior;
- blocos com percepção de profundidade;
- corredores claramente identificáveis;
- labels de setores;
- ícones de pontos de interesse;
- zoom;
- pan;
- rota destacada;
- produtos destacados.

Não é requisito do MVP possuir renderização 3D real.

---

## 9. Administrative Features

O sistema administrativo deve permitir manutenção dos dados da loja.

### MVP

- cadastrar loja;
- cadastrar setores;
- cadastrar corredores;
- cadastrar estruturas do mapa;
- cadastrar pontos de interesse;
- cadastrar produtos;
- associar produto a uma localização;
- editar localização de produto;
- ativar/desativar produtos;
- manter o grafo de navegação da loja.

---

## 10. Route Model

Cada loja deve ser representada internamente por um grafo.

### Node

Representa um ponto navegável.

Exemplos:

- início de corredor;
- fim de corredor;
- cruzamento;
- entrada;
- caixa;
- ponto próximo a um produto.

### Edge

Representa uma conexão navegável entre dois nós.

Cada conexão deve possuir um custo.

O custo inicial será baseado principalmente em distância.

---

## 11. Route Optimization

O sistema precisa resolver dois problemas diferentes:

### Shortest Path

Encontrar o menor caminho entre dois pontos do mapa.

Algoritmos possíveis incluem:

- Dijkstra;
- A*.

A escolha final deve ser registrada em decisão arquitetural.

### Multi-Stop Route

Encontrar uma boa ordem para visitar múltiplos produtos.

Esse problema se aproxima do Traveling Salesman Problem.

O MVP pode utilizar heurística adequada ao número esperado de produtos por compra.

A implementação final não deve ser assumida sem decisão arquitetural registrada.

---

## 12. Functional Requirements

### FR-001
O sistema deve permitir selecionar uma loja.

### FR-002
O sistema deve listar produtos da loja selecionada.

### FR-003
O sistema deve permitir pesquisar produtos.

### FR-004
O sistema deve permitir criar uma lista de compras.

### FR-005
O sistema deve calcular uma rota a partir da lista.

### FR-006
O sistema deve considerar um ponto inicial configurado da loja.

### FR-007
O sistema deve considerar a área de caixas como destino final.

### FR-008
O sistema deve exibir a rota no mapa.

### FR-009
O sistema deve suportar mapas diferentes por loja.

### FR-010
O sistema deve suportar localização diferente para o mesmo produto em lojas distintas.

### FR-011
O sistema administrativo deve permitir manutenção das localizações.

### FR-012
O sistema deve permitir identificação visual de setores.

### FR-013
O sistema deve permitir identificação visual de pontos de interesse.

---

## 13. Non-Functional Requirements

### Performance

- carregamento inicial deve ser rápido;
- interações do mapa devem permanecer fluidas;
- cálculo de rota deve responder em tempo compatível com interação de usuário.

### Responsiveness

O frontend deve funcionar em:

- smartphone;
- tablet;
- desktop.

### Maintainability

Código deve possuir responsabilidades claras entre frontend, backend e infraestrutura.

### Scalability

A arquitetura deve permitir múltiplas lojas sem duplicação de aplicação.

### Data Integrity

Produto, loja, localização e mapa devem possuir relacionamentos consistentes.

### Observability

Backend deve possuir logging estruturado e endpoints de health check.

---

## 14. MVP Success Criteria

O MVP será considerado funcional quando:

1. existir pelo menos uma loja configurada;
2. existir um mapa navegável;
3. existirem produtos associados a posições;
4. o cliente conseguir montar uma lista;
5. o sistema calcular uma rota válida;
6. a rota for exibida sobre o mapa;
7. o percurso iniciar na entrada e terminar nos caixas.

---

## 15. Out of Scope Reference

Consultar:

`/docs/SCOPE.md`

O PRD não autoriza implementação automática de funcionalidades não especificadas.

---

## 16. Source of Truth

Em caso de conflito:

1. decisões registradas em ADR;
2. `SCOPE.md`;
3. `ARCHITECTURE.md`;
4. este `PRD.md`.

Requisitos não documentados devem ser tratados como não definidos.
