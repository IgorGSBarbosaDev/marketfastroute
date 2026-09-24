# Scope — Indoor Supermarket Navigator

## 1. Purpose

Este documento define os limites do projeto.

Funcionalidades não listadas como `IN SCOPE` não devem ser implementadas sem decisão explícita.

Agentes de IA não devem inferir funcionalidades adicionais com base em produtos semelhantes.

---

# 2. MVP — IN SCOPE

## Customer

- selecionar loja;
- pesquisar produtos;
- visualizar produtos disponíveis;
- adicionar produtos à lista;
- remover produtos;
- alterar quantidade;
- calcular melhor rota;
- visualizar rota completa;
- visualizar mapa indoor da loja;
- visualizar setores;
- visualizar corredores;
- visualizar entradas;
- visualizar saídas;
- visualizar área de carrinhos;
- visualizar caixas;
- visualizar outros pontos de interesse configurados;
- visualizar localização dos produtos selecionados;
- zoom e movimentação do mapa.

---

## Map

- mapa específico por loja;
- planta 2D e visualização 3D derivadas do mesmo mapa ativo;
- renderização web;
- setores identificados;
- corredores identificados;
- blocos/prateleiras representados visualmente;
- pontos de interesse;
- rota sobreposta ao mapa;
- rota 3D calculada pelo backend, seguindo os corredores do grafo;
- seleção entre as visões 2D e 3D;
- grafo de navegação associado ao mapa.

---

## Route

- ponto inicial configurável por loja;
- destino final na área de caixas;
- múltiplos produtos;
- ordem dos produtos determinada pelo sistema;
- cálculo de menor caminho entre pontos;
- otimização de sequência de múltiplos destinos;
- recálculo quando a lista mudar antes do início da rota.

---

## Administration

- cadastro de lojas;
- cadastro de produtos;
- cadastro de categorias;
- cadastro de setores;
- cadastro de corredores;
- cadastro de pontos de interesse;
- cadastro de posições do mapa;
- associação de produtos a localizações;
- edição da localização de produtos;
- ativação/desativação de produtos;
- manutenção do grafo da loja.

---

## Platform

- aplicação web;
- responsiva;
- preparada para uso em smartphone;
- arquitetura de monorepo.

---

# 3. Explicitly OUT OF SCOPE — MVP

Os itens abaixo não devem ser implementados no MVP.

## User Positioning

- GPS indoor;
- QR Code;
- Bluetooth Beacon;
- UWB;
- Wi-Fi positioning;
- rastreamento do usuário;
- localização indoor em tempo real;
- atualização automática da posição do usuário.

---

## Shopping Interaction

- confirmação manual obrigatória de produto coletado;
- check-in por corredor;
- escaneamento de produto;
- escaneamento de código de barras pelo cliente.

---

## Checkout

- pagamento pelo aplicativo;
- self-checkout;
- integração com maquininha;
- carteira digital;
- fila de caixa em tempo real;
- recomendação automática de caixa;
- visão computacional para filas.

---

## Commerce

- e-commerce;
- entrega;
- retirada;
- carrinho online;
- reserva de produtos;
- integração de pedido com PDV.

---

## Marketing

- anúncios;
- promoções personalizadas;
- recomendação de produtos;
- cross-sell;
- upsell;
- ofertas baseadas em rota.

---

## Advanced Mapping

- modelagem 3D fotorrealista;
- Unity;
- Unreal Engine;
- avatares;
- realidade aumentada;
- realidade virtual.

### Exceção autorizada — visualização 3D operacional

O MVP inclui uma visualização WebGL em Three.js para o mapa ativo da loja. Ela
usa setores, corredores, prateleiras e pontos de interesse da API; a rota vem
do endpoint existente e segue o grafo publicado. A planta SVG continua
disponível como outra visualização dos mesmos dados. Modelagem fotorrealista e
visualização baseada em Unity/Unreal continuam fora do escopo. A decisão está
registrada em `docs/ADR-004-operational-3d-map.md`.

---

## Accounts

Não é requisito obrigatório do MVP:

- conta de cliente;
- login de cliente;
- histórico de compras;
- listas salvas em nuvem;
- programa de fidelidade.

Para este MVP, por decisão do usuário, não há autenticação administrativa: o
ambiente é somente para uso local ou em rede privada confiável. O Compose
publica os serviços no loopback por padrão. Uma demonstração em LAN expõe o
frontend e também suas rotas administrativas a qualquer pessoa que alcance a
máquina; o projeto não deve ser publicado na internet.

---

## Integrations

Não fazem parte do MVP:

- ERP;
- SAP;
- sistemas de estoque;
- PDV;
- CRM;
- sistema de fidelidade;
- preço em tempo real.

A arquitetura deve permitir futuras integrações sem implementá-las agora.

---

# 4. Future Candidates

Itens abaixo podem ser considerados depois do MVP.

Não devem ser tratados como requisitos atuais.

- preços;
- estoque;
- promoções;
- histórico;
- listas salvas;
- contas de usuário;
- localização indoor;
- recomendação de caixa;
- integração ERP;
- analytics avançado;
- heatmaps;
- recomendações de produtos;
- acessibilidade avançada;
- rotas alternativas;
- restrições de mobilidade;
- múltiplos andares.
- localização mais específica que o corredor, como gôndola, lado e nível de
  prateleira; por enquanto, a instrução de rota informa somente o corredor.

---

# 5. Assumption Rules

Agentes e desenvolvedores devem seguir estas regras:

1. Não inventar requisito.
2. Não implementar funcionalidade apenas porque é comum em aplicativos de supermercado.
3. Não adicionar biblioteca ou serviço externo sem necessidade documentada.
4. Não alterar stack definida em `ARCHITECTURE.md`.
5. Não assumir localização em tempo real.
6. Não assumir login de cliente.
7. Não assumir integração com ERP.
8. Não assumir disponibilidade de estoque.
9. Não assumir preço de produto.
10. Não assumir mapa 3D real.
11. Não assumir algoritmo final quando existir decisão pendente.
12. Quando houver ambiguidade relevante, registrar como `UNRESOLVED`.

---

# 6. Scope Change

Qualquer funcionalidade fora deste documento deve seguir:

```text
Proposta
→ análise
→ decisão
→ atualização do SCOPE
→ atualização do PRD se necessário
→ implementação
```

A existência de código experimental não altera automaticamente o escopo oficial.
