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
  esquerda; planta da API à direita. O SVG contém setores, corredores,
  prateleiras, pontos de referência e o percurso calculado pelo servidor.
- **Rota:** a coluna lateral mostra as paradas ordenadas. No desktop, um resumo
  acompanha a rota sobre o mapa; no celular, ele fica abaixo da planta para
  manter o caminho visível. Zoom, enquadramento, pan e instruções textuais
  continuam disponíveis. Em telas táteis, a rolagem vertical da página funciona
  mesmo quando começa sobre o mapa.
- **Administração:** seletor de unidade, cinco áreas de trabalho e formulários
  ligados aos contratos administrativos existentes. O relatório do servidor
  apresenta pendências antes da ativação. A navegação administrativa quebra
  em duas linhas no celular para manter os cinco destinos visíveis.
- **Demonstração 3D:** área separada e explicitamente fictícia, carregada sob
  demanda. O mercado tem seis setores, corredores, cruzamentos, carrinhos,
  entrada, caixas e um caminho decorativo independente da API.

## Movimento e acessibilidade

- O caminho SVG é revelado ao receber uma rota; resumos entram com deslocamento
  curto; carregamentos usam indicador discreto.
- A apresentação da cena dura oito segundos e termina parada. A pessoa pode
  pausar, reproduzir novamente, girar pelo ponteiro ou setas e redefinir a
  câmera. `prefers-reduced-motion` inicia a cena parada.
- O CSS reduz animações e transições em `prefers-reduced-motion`.
- Foco visível, nomes acessíveis nos controles do mapa, campos rotulados,
  estados `status`/`alert` e instruções da rota em lista textual acompanham a
  visualização.
- Em telas com toque, botões e campos usam área mínima de 44 × 44 px.
- Se WebGL2 não iniciar ou perder o contexto, a cena mantém setores, corredores,
  entrada, caixas e texto em uma planta estática.

## Revisão visual — 2026-09-23

Capturas e fluxos foram revistos no navegador local, em 1440×900 e 390×844:

- busca por `banana`, inclusão na lista, cálculo da rota e marcadores no SVG;
- composição móvel com a aba Lista e, após cálculo, a aba Mapa;
- área administrativa e relatório de validação, em desktop e celular;
- cena 3D com WebGL2 iniciando no navegador.

Ajustes feitos na revisão: as cinco abas administrativas passam a ocupar duas
linhas no celular; o resumo da rota deixa de cobrir o mapa móvel; a planta
ajusta sua altura ao desenho; o resumo identifica corretamente “1 parada”. O
fallback sem WebGL2 tem teste automatizado.

Essa revisão não substitui auditoria independente de contraste nem teste
completo com leitores de tela. A execução integrada dos cadastros e da
publicação foi concluída com PostgreSQL/Testcontainers e Compose local em
2026-09-23.
