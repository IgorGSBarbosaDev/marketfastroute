# Base de demonstração

`infra/demo/seed-market-aurora.sql` cria conteúdo explicitamente fictício para
testar o fluxo local do MVP:

- uma loja `Mercado Aurora — demonstração fictícia`;
- sete setores, 14 corredores de produto, três travessas de circulação e 21
  blocos de prateleira identificados;
- um grafo de 52 nós e 57 conexões bidirecionais entre travessas, corredores,
  entrada, carrinhos e caixas;
- entrada, carrinhos e caixas;
- quatorze produtos fictícios com localizações navegáveis.

A planta reserva 60 × 40 m. Os corredores de produto têm 2,5 m de largura; as
travessas e a ligação lateral às caixas aparecem como áreas transitáveis
próprias. Hortifruti e padaria ficam junto à entrada; mercearia ocupa o miolo;
laticínios e congelados ficam no perímetro oposto. Três gôndolas identificadas
por setor deixam um corredor livre entre cada par de prateleiras. O grafo conecta
as travessas da entrada e dos caixas às duas extremidades dos corredores de
produto, para que as paradas do mesmo corredor sejam distintas e o caminho não
corte as gôndolas.

Essa composição é um exemplo representativo, não um padrão obrigatório de todas
as lojas. Um guia da Virginia Cooperative Extension descreve hortifruti próximo
à entrada e à padaria, mercearia nos corredores centrais, laticínios ao fundo e
itens de impulso junto às caixas; a planta Aurora adapta esse tipo de fluxo ao
layout fictício do MVP. [Virginia Tech, *Grocery Store Layouts: Where is it Located and Why?*](https://vtechworks.lib.vt.edu/bitstream/handle/10919/93116/AAEC-190.pdf)

O comando Compose aguarda a API aplicar as migrações e a base estar pronta.
Carregue ou atualize a base com:

```powershell
pnpm demo:seed
```

O script é idempotente dentro do namespace `MFR-DEMO-*` e cria a versão 3 como
`DRAFT`. No painel administrativo, selecione a loja, valide a planta e ative-a
para testar a busca e o cálculo de rota. A publicação obedece à validação do
backend; o seed não muda automaticamente o mapa ativo.

É seguro repetir o seed enquanto a versão 3 continuar em `DRAFT`. A atualização
regrava a geometria e o grafo reservados para o Mercado Aurora. Depois que
essa versão for ativada ou arquivada, o comando se recusa a atualizar sua
geometria; para reconstruir a demonstração, limpe os dados reservados e rode o
seed novamente. Assim uma nova execução não sobrescreve o mapa publicado nem o
histórico. As alterações permanecem restritas aos registros com códigos
`MFR-DEMO-*` e à loja `MFR-DEMO-AURORA`.

Os nomes, endereços e produtos são exemplos inventados e não descrevem uma
loja real nem informam preço ou estoque.

Para remover a loja e os produtos fictícios do banco Compose, rode
`pnpm demo:clear`. O comando remove somente a loja reservada
`MFR-DEMO-AURORA` e os códigos `MFR-DEMO-*`; ele se recusa a continuar se
detectar uso desses códigos em outra loja, produto ou hierarquia de categorias.
Os mapas e elementos estruturais são removidos pelas relações da loja demo.
Os dois scripts recusam registros que já usem os códigos ou IDs reservados
para conteúdo diferente do Mercado Aurora.
