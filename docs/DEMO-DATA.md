# Base de demonstração — Aurora v5

O Mercado Aurora é uma loja fictícia de 60 × 40 m. A versão 5 substitui a
composição básica da versão 3 por uma planta mais densa, sem mudar o contrato
REST, o modelo relacional ou o cálculo de rotas.

| Elemento | Versão 3 | Versão 5 |
| --- | ---: | ---: |
| Setores | 7 | 16 |
| Corredores de produto | 14 | 60 trechos distribuídos por zonas |
| Circulação complementar | 3 trechos | 5 travessas + 4 ligações principais |
| Expositores e mobiliário | 21 | 80 |
| Largura das gôndolas centrais | 2–2,25 m | 1 m |
| Largura dos corredores centrais | 2,5 m | 2 m |
| Comprimento dos trechos de produto | 10,5–19,25 m | 3–8 m, conforme a zona |
| Produtos / localizações primárias | 14 | 240 |
| Nós / conexões bidirecionais | 52 / 57 | 235 / 286 |

## Organização espacial

A planta não repete três fileiras uniformes. A orientação e o comprimento dos
móveis mudam conforme a seção, e o açougue forma uma área de atendimento própria.

| Região | Corredores | Organização |
| --- | --- | --- |
| Grãos e massas | A01–A08 | Gôndolas verticais de 5 m, largura de 1 m |
| Temperos e conservas | A09–A16 | Gôndolas verticais de 6 m, separadas da primeira faixa por uma travessa |
| Café da manhã | A17–A20 | Gôndolas horizontais de 8 m |
| Biscoitos e doces | A21–A28 | Dois conjuntos de gôndolas horizontais curtas de 3,5 m |
| Bebidas | A29–A32 | Quatro ilhas horizontais de 5,5 m, em dois pares |
| Hortifruti | A33–A36 | Ilhas baixas de 4 m, desencontradas junto à entrada |
| Padaria | A37–A40 | Expositores murais na parede esquerda |
| Cuidados pessoais | A41–A44 | Corredores verticais curtos de 4 m |
| Limpeza e pet | A45–A48 | Trechos de 3 m; pet separado no A48 |
| Laticínios e frios | P01–P06 | Refrigeradores na parede de fundo |
| Açougue e pescados | P07–P09 | Três balcões de vitrine, parede revestida e bancada de preparo |
| Congelados | P10–P12 | Freezers murais no fundo |

O mobiliário inclui 60 expositores principais com localizações de produtos,
sete peças da área de atendimento, duas pontas de gôndola, uma prateleira mural
complementar, sete caixas paralelos, dois portais e uma estação de carrinhos.
Os acessórios fazem parte da geometria compartilhada com o 2D; o grafo deixa
livres os acessos, inclusive entre os caixas e ao redor da espera do açougue.

No açougue, há oito cadeiras em duas fileiras, um dispensador de tickets e um
painel fixo com `SENHA 042 · GUICHÊ 02 · DEMONSTRAÇÃO`. Trata-se somente de
cenografia: não há emissão de tickets, contagem de pessoas, chamada de senhas,
atendimento ou informação de fila em tempo real.

Entrada e saída têm portais separados, compactos, com painéis de vidro e
sinalização. A estação de carrinhos fica perto da entrada. Os sete caixas têm
esteiras, área de leitura, terminal e área de embalagem, todos paralelos e com
passagens entre si. O destino da rota continua sendo um único ponto da área
de caixas; não há escolha de caixa ou cálculo de fila.

Estas medidas são escolhas da simulação, não um projeto executivo ou uma
norma para lojas reais. Os produtos não informam preço ou estoque.

## Catálogo por corredor

Cada corredor principal tem quatro produtos pesquisáveis, distribuídos por
módulos e níveis, com dois pontos de parada ao longo do acesso. Os 14 SKUs
anteriores mantêm suas identidades. O detalhamento completo está em
[DEMO-ASSORTMENT.md](DEMO-ASSORTMENT.md).

## Representação 2D e 3D

Ambas as vistas usam a geometria da API. O SVG distingue gôndolas, ilhas,
padaria e refrigerados, e revela os códigos dos móveis ao aproximar. O 3D usa
prateleiras com duas faces, murais com fundo, ilhas baixas com divisórias e
refrigeradores com portas translúcidas, puxadores e montantes. A área de serviço
acrescenta vitrines com bandejas, paredes revestidas, bancadas e cadeiras.
Os caixas incluem esteira, leitor, terminal e área de embalagem. Embalagens são
ilustrativas e instanciadas; não representam quantidades em estoque.

O tipo visual é uma convenção de apresentação no início de `ShelfBlock.name`:
`Gôndola ·`, `Prateleira mural ·`, `Ilha ·`, `Padaria ·`, `Refrigerador mural ·`
ou `Freezer mural ·`. Mobiliário adicional usa `Balcão de açougue ·`,
`Parede de serviço ·`, `Bancada de preparo ·`, `Balcão de apoio ·`, `Cadeiras ·`,
`Totem de senhas ·`, `Caixa paralelo ·`, `Portal de entrada ·`, `Portal de saída ·`
e `Estação de carrinhos ·`. Esses nomes identificam exclusivamente a aparência. Nomes sem convenção continuam funcionando como gôndolas,
com reconhecimento de hortifruti e padaria pelo setor. Não há regra de rota
ou de catálogo baseada nesses prefixos.

O grafo usa segmentos ortogonais com custos em metros, duas paradas por
corredor e conexões às travessas. Não há cálculo de caminho no frontend.

## Carregar e publicar

```powershell
pnpm demo:seed
```

O script gera a versão **5 em DRAFT**. Pode ser repetido enquanto essa versão
continuar em rascunho. IDs e códigos reservados são conferidos antes de qualquer
alteração; produtos demo vinculados a outra loja bloqueiam a carga.

Para publicar em uma instalação com versão anterior ativa: valide a versão 5
na administração, arquive explicitamente a versão anterior e ative a versão 5.
O seed não troca automaticamente o mapa ativo e nunca regrava um mapa publicado.
As geometrias e os grafos das versões anteriores permanecem no histórico.
O catálogo é compartilhado entre versões: as categorias dos produtos demo
existentes são atualizadas conforme a nova distribuição.

Para uma nova alteração de layout após a publicação, crie uma nova versão de
rascunho. `pnpm demo:clear` é uma operação destrutiva opcional que apaga a loja
demo e seu histórico, além do catálogo reservado. Só deve ser usada quando
esse descarte for desejado. Recusa vínculos do catálogo com outras lojas,
produtos ou hierarquias de categorias.

## Manutenção e validação

Edite `infra/demo/generate_market_aurora.py`, que contém o planejamento do
catálogo e a geometria, e regenere o SQL versionado. O Compose continua
executando somente PostgreSQL; Python não é dependência de runtime da aplicação.

```powershell
python infra/demo/generate_market_aurora.py
python infra/demo/generate_market_aurora.py --check
python -m unittest discover -s infra/demo -p 'test_*.py' -v
python infra/demo/verify_seed_postgres.py
```

O último comando requer o PostgreSQL do Compose. Cria e remove exclusivamente
um banco temporário próprio, aplica as migrations e verifica carga repetida,
rollback, proteção do mapa publicado, proteção de produtos compartilhados,
limpeza isolada e recarga. Os testes geométricos verificam limites, sobreposição,
conectividade, custos métricos e afastamento de 0,4 m entre arestas e expositores.
