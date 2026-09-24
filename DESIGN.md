# Design system — Market Fast Route

## Direção aprovada

**Atlas interativo da loja**, com a composição **02 — busca e lista em primeiro
plano**. A busca e a lista formam a coluna de trabalho; a planta SVG usa o maior
espaço visual disponível. No celular, Lista e Mapa viram abas explícitas. A
lista abre primeiro; depois de calcular a rota, o foco muda para o mapa.

Essa hierarquia foi aprovada em 2026-09-23 e está registrada em
`.impeccable/questions/mvp-mock-approval.json`.

## Referências

- Os guias de loja da [IKEA](https://www.ikea.com/in/en/files/pdf/39/f2/39f23f18/store-guide-updated_july1st-2.pdf)
  e a [planta IKEA Bangna](https://www.ikea.com/th/en/files/pdf/e4/d1/e4d12c2a/ikea-bangna-store-layout_en.pdf)
  inspiram a hierarquia de setores e pontos de orientação. A aplicação usa um
  mapa original e os dados da API; essa referência é visual.
- A orientação para animação segue a seção de [Motion das Human Interface
  Guidelines da Apple](https://developer.apple.com/design/human-interface-guidelines/motion):
  movimento curto, associado a uma resposta visível e reduzido quando a pessoa
  prefere menos movimento.
- A cena ilustrativa respeita o ciclo de recursos descrito na documentação do
  [WebGLRenderer do Three.js](https://threejs.org/docs/pages/WebGLRenderer.html)
  e no guia oficial [Creating a scene](https://threejs.org/manual/pages/creating-a-scene.html).

## Tokens visuais

Os tokens estão em `apps/web/src/index.css` e alimentam os componentes shadcn.

| Uso | Token | Valor |
| --- | --- | --- |
| Fundo de papel | `--paper` | `#eeede3` |
| Superfície | `--surface` | `#fbfaf5` |
| Texto principal | `--ink` | `#203b4d` |
| Ação primária | `--navy` | `#284760` |
| Êxito / orientação | `--sage` | `#668b76` |
| Caminho / parada | `--amber` | `#a8731d` |
| Texto secundário | `--muted-ink` | `#52636a` |
| Erro | `--danger` | `#9b4138` |

`Geist Variable` é a família de interface; `Lora Variable` dá caráter
editorial aos títulos de página, áreas e painéis. O contraste usa papel claro e
texto azul escuro; a cor nunca é o único indicador de estado: rótulos, ícones,
texto de validação e marcadores também identificam a informação.

Relações de contraste calculadas nos tokens principais: texto principal/papel
9,95:1; texto secundário/papel 5,33:1; texto branco/botão primário 9,56:1;
marcador âmbar/fundos setoriais claros pelo menos 3,15:1. Isso cobre as
combinações centrais; não é uma auditoria automatizada de todas as telas e
estados.

## Composição e comportamento

- **Compras:** loja ativa, busca com atraso curto, resultados e lista à
  esquerda; planta da API à direita. O cliente pode alternar entre SVG/2D e
  Three.js/3D. As duas vistas usam setores, corredores, prateleiras, pontos de
  referência e o percurso calculado pelo servidor.
- **Rota:** a coluna lateral mostra a lista de compras. Um resumo compacto e
  recolhido fica sobre a planta em telas grandes e pequenas; ao expandir, mostra
  entrada, paradas na ordem calculada e caixas. Calcular a rota abre a planta
  3D; a pessoa pode mudar para 2D sem perder o percurso. Zoom, enquadramento,
  pan e uma lista textual acompanham o caminho.
- **Administração:** seletor de unidade, cinco áreas de trabalho e formulários
  ligados aos contratos administrativos existentes. O relatório do servidor
  apresenta pendências antes da ativação. A navegação administrativa quebra
  em duas linhas no celular para manter os cinco destinos visíveis.
- **Mapa demonstrativo 2D:** a seed do Mercado Aurora tem sete setores, 14
  corredores de produto, três travessas de circulação e 21 gôndolas identificadas.
  O SVG marca o piso transitável, mantém os nomes dos setores acima das formas e
  lê as geometrias e o grafo da API.
- **Mercado 3D:** cena estilizada gerada a partir do mapa ativo da loja,
  carregada sob demanda. Blocos de prateleira mostram produtos procedurais;
  pontos de parada exibem produto e ordem. O mapa de demonstração Mercado
  Aurora usa dados fictícios e deixa o caminho livre entre as gôndolas.

## Movimento e acessibilidade

- O caminho SVG é revelado ao receber uma rota; resumos entram com deslocamento
  curto; carregamentos usam indicador discreto.
- A cena 3D não gira automaticamente. Arraste para orbitar, use o scroll para
  aproximar, o botão direito para mover e os controles para enquadrar ou
  restaurar a planta. O movimento depende das ações da pessoa.
- O CSS reduz animações e transições em `prefers-reduced-motion`.
- Foco visível, nomes acessíveis nos controles do mapa, campos rotulados,
  estados `status`/`alert` e instruções da rota em lista textual acompanham a
  visualização.
- Em telas com toque, botões e campos usam área mínima de 44 × 44 px.
- Se WebGL2 não iniciar ou perder o contexto, a interface oferece a planta
  SVG/2D com a mesma rota.

## Revisão visual — 2026-09-23

Capturas e fluxos foram revistos no navegador local, em 1440×900 e 390×844:

- busca por `banana`, inclusão na lista, cálculo da rota e marcadores no SVG;
- composição móvel com a aba Lista e, após cálculo, a aba Mapa;
- área administrativa e relatório de validação, em desktop e celular;
- cena 3D com WebGL2 iniciando no navegador.

Ajustes feitos na revisão: as cinco abas administrativas passam a ocupar duas
linhas no celular; a planta ajusta sua altura ao desenho; o resumo identifica
corretamente “1 parada”. O fallback sem WebGL2 tem teste automatizado.

Na revisão de 2026-09-23, o resumo da rota passou a recolher seus detalhes e a
ficar sobre o mapa também no celular. A seed demonstrativa foi alinhada à
composição aprovada, mantendo os setores, corredores, prateleiras e grafo como
dados da API.

Essa revisão não substitui auditoria independente de contraste nem teste
completo com leitores de tela. A execução integrada dos cadastros e da
publicação foi concluída com PostgreSQL/Testcontainers e Compose local em
2026-09-23.

## Revisão da rota 3D — 2026-09-23

A cena operacional substitui a rota decorativa independente. A geometria vem
da versão ativa retornada pela API, a rota segue os pontos calculados pelo
backend e a planta 2D continua disponível no seletor. A maquete Mercado
Aurora organiza 14 corredores de produto de 2,5 m e travessas explícitas, com
hortifruti e padaria perto da entrada, mercearia no centro, laticínios e
congelados no perímetro oposto e caixas na frente. As prateleiras ficam fora
das travessas; o conteúdo é
procedural e fictício.
