# Base de demonstração

`infra/demo/seed-market-aurora.sql` cria conteúdo explicitamente fictício para
testar o fluxo local do MVP:

- uma loja `Mercado Aurora — demonstração fictícia`;
- seis setores, seis corredores e doze blocos de prateleira;
- um grafo de 32 nós e 43 conexões com caminhos de ida e volta;
- entrada, carrinhos e caixas;
- doze produtos fictícios com localizações navegáveis.

O comando Compose aguarda a API aplicar as migrações e a base estar pronta.
Carregue ou atualize a base com:

```powershell
pnpm demo:seed
```

O script é idempotente dentro do namespace `MFR-DEMO-*` e cria a planta como
`DRAFT`. No painel administrativo, selecione a loja, valide a planta e ative-a
para testar a busca e o cálculo de rota. A publicação obedece à validação do
backend; o seed não muda automaticamente o mapa ativo.

É seguro repetir o seed enquanto a versão 1 continuar em `DRAFT`. Depois que
essa versão for ativada ou arquivada, o comando se recusa a atualizar sua
geometria; assim uma nova execução não sobrescreve o mapa publicado nem o
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
