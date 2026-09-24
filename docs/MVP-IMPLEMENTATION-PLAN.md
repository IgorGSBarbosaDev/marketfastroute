# Plano executável do MVP — Market Fast Route

Este documento é a sequência de implementação e aceite do MVP. Ele acompanha
o que existe no repositório e pode ser fornecido ao comando `/go` para retomar
ou delegar a execução por etapas.

## Instrução pronta para `/go`

```text
Implemente e conclua o MVP do Market Fast Route conforme
docs/MVP-IMPLEMENTATION-PLAN.md. Leia primeiro AGENTS.md, PRODUCT.md,
docs/PRD.md, docs/SCOPE.md, docs/ARCHITECTURE.md, docs/DATA_MODEL.md e os
ADRs. O plano é cumulativo: confira o estado real do Git e os arquivos antes
de repetir trabalho, conclua cada etapa pendente e atualize os checkboxes e
evidências neste documento.

Para telas novas ou redesenhadas, consulte a escolha e o mock aprovados em
`.impeccable/questions/mvp-mock-approval.json` e `DESIGN.md`. Se não houver
aprovação registrada, apresente três alternativas visuais e aguarde escolha
antes de escrever componentes. Não repita etapas já concluídas.

Mantenha o backend como Spring Boot modular monolith, React/TypeScript/Vite e
PostgreSQL. O sistema é apenas para computador/rede privada e não terá login
neste MVP; mantenha binds locais por padrão e não exponha serviços à internet
pública. O mapa operacional deve usar exclusivamente mapa/rota retornados pela
API e continuar em SVG. Three.js/WebGL é somente uma demonstração separada,
claramente fictícia, com fallback sem WebGL, carregamento sob demanda,
respeito a prefers-reduced-motion e descarte de recursos.

Não edite migrações V1-V4 nem reinterprete mapas existentes. Não crie regras,
entidades, integrações ou funcionalidades fora dos documentos de fonte da
verdade. Preserve os dados e trabalho local existentes. Não faça commit, push
ou publicação. Rode lint, testes e builds solicitados neste plano e relate
separadamente o que depende de Docker. Conclua com revisão do diff, acessibilidade,
fluxos de cliente/admin, desktop/mobile e documentação atualizada.
```

## Resultado esperado

Uma pessoa escolhe uma loja, pesquisa itens por nome/categoria/SKU/EAN, monta
uma lista, calcula uma rota da entrada aos caixas e acompanha as paradas sobre
um mapa SVG navegável. Em uma área administrativa de uso privado, a equipe
gerencia lojas, catálogo, disponibilidade, versões do mapa, geometria, grafo,
POIs e localizações; vê erros de validação por campo e publica uma versão
somente depois de arquivar explicitamente a versão ativa anterior. Uma área
separada apresenta um mercado fictício em 3D.

## Limites acordados

- Sem autenticação neste MVP; a instalação é local ou em rede privada
  confiável. A API administrativa não pode ser exposta publicamente.
- Sem preço, estoque, GPS/rastreamento, ETA, confirmação de coleta, persistência
  de lista/rota, pagamento ou integrações externas.
- A API permanece responsável pelas regras de domínio e pela rota. O frontend
  não escolhe a ordem das paradas nem inventa posições ausentes.
- O mapa real vem do mapa ativo e da rota calculada pela API. Three.js mostra
  somente o cenário autoral fictício.
- Preservar as migrações existentes e não converter dados antigos sem
  inventário e decisão explícita.

## Referências visuais pesquisadas

- Os guias oficiais de loja da IKEA ([guia de loja](https://www.ikea.com/in/en/files/pdf/39/f2/39f23f18/store-guide-updated_july1st-2.pdf),
  [planta IKEA Bangna](https://www.ikea.com/th/en/files/pdf/e4/d1/e4d12c2a/ikea-bangna-store-layout_en.pdf))
  servem de referência para legibilidade de setores, percursos e pontos de
  orientação. Essa leitura é uma inferência visual; a direção final permanece
  a escolha do usuário.
- A orientação de movimento vem das [Human Interface Guidelines da Apple](https://developer.apple.com/design/human-interface-guidelines/motion):
  movimento curto, ligado a uma mudança útil e compatível com redução de
  movimento.
- Os limites técnicos da cena usam a documentação do
  [WebGLRenderer do Three.js](https://threejs.org/docs/pages/WebGLRenderer.html)
  e o guia [Creating a scene](https://threejs.org/manual/pages/creating-a-scene.html).

## Situação atual — 2026-09-23

- API Spring, schema PostgreSQL/Flyway, CRUD administrativo, busca de loja e
  produto, localizações, cálculo de rota, Dijkstra e heurística de paradas já
  existiam antes deste plano.
- O shell inicial foi substituído por compra, administração/editor espacial e
  demonstração 3D, conectados aos serviços REST existentes.
- Backend, contratos de frontend, tratamento de erro, validação de publicação,
  seed fictício, decisões de coordenadas e cena Three.js estão implementados.
- Direção visual escolhida: **Atlas interativo da loja**. Composição aprovada:
  **02 — Lista e busca em primeiro plano**, com busca/lista à esquerda, atlas SVG
  amplo à direita e resumo da rota sobre o mapa. A decisão está registrada em
  `.impeccable/questions/mvp-mock-approval.json`.
- O MVP local está implementado e o aceite integrado com PostgreSQL foi
  concluído. O Compose está ativo em `http://127.0.0.1:5173`; como as portas
  `5432` e `8080` já pertenciam a outros processos locais, a API e o PostgreSQL
  deste projeto foram mapeados em `18080` e `55432`.

### Evidências da execução — 2026-09-23

- `pnpm web:test`: 21 testes em 7 arquivos cobrem serviços/erros, jornada de
  compra, retry de busca e lojas, preservação da rota ao mudar quantidade,
  navegação móvel acessível, formulários administrativos, relatório de
  validação, parada do loop WebGL quando pausado e encerramento automático da
  apresentação 3D após oito segundos.
- `pnpm web:lint` passou.
- `pnpm web:build` passou; a cena Three.js fica em um chunk sob demanda de
  553,28 kB minificados (139,03 kB gzip), acima do aviso padrão de 500 kB.
- Navegador local: revisão visual em 1440×900 e 390×844; busca → lista → rota
  mapa SVG. Com os serviços reais, a interface encontrou Arroz, mostrou uma
  parada de 189 m entre entrada e caixas e exibiu a versão publicada no admin;
  a maquete fictícia WebGL2 também carregou no navegador. No viewport móvel, a
  alternância Lista/Mapa mostra o estado selecionado e os controles alcançam
  44 px mínimos; a apresentação 3D pausa sozinha após oito segundos.
- O PostgreSQL limpo aplicou as quatro migrações Flyway. O seed executou duas
  vezes enquanto o mapa estava em `DRAFT`, mantendo seis setores, seis
  corredores, 32 nós, 43 arestas e 12 localizações. A validação retornou
  `publishable: true` sem pendências. Após a ativação manual, repetir o seed foi
  recusado e não alterou o mapa ativo.
- Suíte backend completa: 147 testes passaram, incluindo Testcontainers,
  concorrência de publicação e persistência PostgreSQL. A execução real revelou
  uma falha de materialização JPA no relatório de publicação; a consulta agora
  usa projeção tipada de leitura e os testes de publicação passaram.
- `docker compose` health: API e PostgreSQL `UP`; página web respondeu `200`.
  O Compose continua ativo para permitir abrir o MVP local. Para origens web
  personalizadas, ajuste `CORS_ALLOWED_ORIGINS` à porta/origem escolhida.
- A direção visual e a composição 02 foram aprovadas e registradas; a
  implementação e a verificação integrada do MVP estão concluídas.

## Etapas e critérios de aceite

### 0. Alinhar escopo, contratos e estado real

- [x] Conferir requisitos, estrutura do monorepo e APIs existentes.
- [x] Registrar uso local/rede privada sem autenticação e limites do mapa 3D.
- [x] Registrar contrato espacial e validação de publicação em
  `docs/ADR-002-map-coordinates-and-publication.md`.
- [x] Registrar separação da cena Three.js em
  `docs/ADR-003-threejs-demonstration.md`.
- [x] Ao continuar, conferir `git status`, migrations e dependências; não
  sobrescrever alterações não relacionadas.

Aceite: PRD, escopo, arquitetura, modelo, API e implementação usam termos
compatíveis; desconhecidos continuam identificados como não resolvidos.

### 1. Completar contratos e integridade do backend

- [x] Validar dimensões, limites, pontos/retângulos, nós/arestas ativos,
  entrada/caixas, localizações escolhidas pelo algoritmo e conectividade antes
  de ativar mapa.
- [x] Compartilhar resolução de entrada/caixas entre validação e roteamento.
- [x] Recusar criação de mapa diretamente como ativo e retornar erros 422
  estruturados na tentativa de publicação inválida.
- [x] Expor relatório de validação em
  `GET /api/v1/admin/maps/{mapId}/validation`.
- [x] Preservar a exigência de arquivar explicitamente o mapa ativo antes de
  publicar outro.
- [x] Cobrir retângulo rotacionado fora do mapa, ponto fora dos limites,
  endpoint tipado ambíguo, nó de localização inativo, coordenadas parciais,
  grafo desconectado, POI inválido/duplicado, endpoint ausente e cenário
  publicável em `MapPublicationValidatorTest`; cobrir produto disponível sem
  localização em `RouteServiceTest` e transições/arquivamento em
  `StoreMapAdminServiceTest`.
- [x] Serializar ativações por loja com lock transacional e manter o índice
  único parcial como garantia final; não automatizar arquivamento.
- [x] Tornar versões publicadas imutáveis: escritas de estruturas, grafo,
  POIs e localizações exigem `DRAFT`; bloquear linhas dos mapas durante as
  alterações e validar sob lock antes de ativar.
- [x] Adicionar teste de integração para ativação concorrente de duas versões e
  para recusa de edição estrutural após publicação em `AdminPersistenceTest`.
- [x] Confirmar a serialização concorrente e o índice único com PostgreSQL real
  em Testcontainers (`AdminPersistenceTest`).
- [x] Manter migrações V1-V4 intactas; nenhuma migração foi alterada ou criada.

Aceite: nenhum mapa ativo pode falhar nos requisitos de geometria, endpoints e
grafo; cada pendência identifica código/tipo/id e a interface consegue apontar
o registro a corrigir. Rota inválida continua retornando erro explicável.

### 2. Tornar os dados de demonstração reproduzíveis

- [x] Criar `Mercado Aurora — demonstração fictícia` em namespace próprio,
  com seis setores, seis corredores, prateleiras, entrada, carrinhos, caixas,
  32 nós, 43 arestas e doze produtos/localizações.
- [x] Manter o mapa em DRAFT e fornecer comando opt-in `pnpm demo:seed`.
- [x] Fornecer comando opt-in `pnpm demo:clear` restrito aos códigos da base
  fictícia e com recusa diante de vínculos externos.
- [x] Recusar códigos/IDs reservados incompatíveis e associar somente os doze
  SKUs definidos no seed.
- [x] Verificar SQL em PostgreSQL limpo após as migrações e executar o seed duas
  vezes; os totais permaneceram estáveis e nenhuma linha não-demo foi criada.
- [x] Confirmar que o seed não publica nem substitui um mapa ativo; depois da
  publicação manual, o seed recusa a execução e deixa a versão `ACTIVE` intacta.
- [x] Conferir graficamente que toda geometria e coordenada do seed real cabe
  em 240×160,
  que as seis áreas setoriais são distinguíveis e que há curvas e alternativas
  no grafo.

Aceite: seed só roda quando solicitado, usa dados visivelmente fictícios e
permite concluir busca → lista → rota da entrada aos caixas depois da
publicação manual no admin.

### 3. Implementar jornada do cliente

- [x] Criar tipos alinhados às respostas reais e serviços de API do cliente.
- [x] Padronizar erros de rede, HTTP, validação, API e respostas 204.
- [x] Aprovar e registrar a composição visual 02 antes de escrever os componentes.
- [x] Implementar shell, navegação,
  busca, resumo da lista e estados de carregamento/erro/vazio/sucesso conforme
  o mock escolhido.
- [x] Selecionar loja ativa; pesquisar por nome, categoria, SKU e EAN usando a
  API existente; manter busca responsiva sem disparar requisição em cada tecla
  sem controle.
- [x] Adicionar/remover produtos e alterar quantidade localmente; indicar
  produtos sem localização sem fingir que entrarão no caminho.
- [x] Calcular a rota pela API. Exibir paradas na ordem retornada, distância
  retornada, entrada e caixas configurados.
- [x] Desenhar SVG proporcional ao mapa: setores, corredores, blocos, POIs,
  caminho e paradas; oferecer zoom, pan, enquadramento da rota e alternativa
  textual numerada para leitor de tela.
- [x] Em smartphone, priorizar próxima parada e manter o mapa e a lista
  alcançáveis sem controles pequenos ou rolagem horizontal.
- [x] Incluir navegação entre cliente/admin/demonstração. Manter a lista no
  estado da interface; não persistir lista ou rota no servidor.

Aceite: com loja demo publicada, usuário encontra um item, ajusta quantidade,
adiciona outros, calcula uma única rota de parada otimizada e entende onde
começar, o que visitar e onde terminar; erros e itens sem localização têm
saída recuperável.

### 4. Implementar administração e editor espacial

- [x] Criar contratos tipados e serviços para CRUD existente de loja, categoria,
  produto, disponibilidade, mapa, elementos geométricos, grafo, POI e
  localizações.
- [x] Completar telas e formulários para todas essas áreas, com desativação
  lógica (`active=false`) e confirmação contextual. Nunca apagar fisicamente.
- [x] Fluxo de versões: criar como DRAFT, editar, ver relatório, corrigir,
  arquivar ACTIVE explicitamente, revalidar e então ativar.
- [x] Editor SVG com posição/tamanho em unidades do mapa, seleção/criação de
  setor, corredor, bloco e POI, criação/conexão de nós e arestas, direção/custo,
  vínculo da localização de produto e estado de alterações não salvas.
- [x] Mostrar lista de erros com tipo, identificador, mensagem e ação para
  selecionar o elemento no editor; validação no frontend é apenas feedback, a
  validação autoritativa continua no backend.
- [x] Implementar teclado para selecionar, navegar entre campos, conectar nós
  por formulário e desfazer/cancelar alterações locais; não depender de gesto
  de arrastar para editar.
- [x] Confirmar mudança de versão e navegação com rascunho não salvo.

Aceite: um administrador privado consegue criar/corrigir o mapa de ponta a
ponta, navegar a validação e publicar com ordem explícita sem remover dados.

### 5. Construir a cena Three.js de demonstração

- [x] Dependência `three` e tipos adicionados; decisão arquitetural aceita.
- [x] Criar cenário autoral isométrico com os seis setores e corredores do
  Mercado Aurora, interseções/curvas, entrada, carrinhos, caixas, blocos de
  prateleira e um caminho ilustrativo bem marcado.
- [x] Exibir aviso claro “Mercado fictício — demonstração visual”. O caminho é
  decorativo e não pode parecer a rota calculada do cliente.
- [x] Carregar por importação dinâmica, só ao entrar na área de demonstração;
  usar geometria/material compartilhado quando apropriado e limitar DPR,
  sombras e chamadas de desenho em dispositivos móveis.
- [x] Implementar animação curta e controlável (reproduzir/pausar), controles
  de câmera acessíveis e respeito a `prefers-reduced-motion`.
- [x] Se WebGL2/renderer não estiver disponível, mostrar fallback estático e
  texto equivalente; nenhum erro pode impedir uso de busca, admin ou mapa SVG.
- [x] Ao ocultar/desmontar, cancelar loop, remover listeners e chamar `dispose`
  em geometria, materiais, texturas e renderer.

Aceite: scene aparece somente na seção demonstrativa, funciona em browser
compatível, tem fallback verificável e nunca fornece dados usados pela rota.

### 6. Refinar UI, movimento e acessibilidade

- [x] Aplicar a direção e o mock aprovados sem divergir de PRODUCT.md/PRD.
- [x] Usar tokens para cor, tipografia, espaçamento, foco, elevação e estados;
  os pares de texto principais atendem WCAG AA e o caminho gráfico mantém pelo
  menos 3:1 nas superfícies setoriais revisadas (razões em `DESIGN.md`).
- [x] Animar somente transições úteis (feedback de lista, carregamento de rota,
  foco do mapa e transição do painel); usar propriedades performáticas e
  reduzir/remover movimento conforme preferência do sistema.
- [x] Garantir landmark, ordem de títulos, rótulos, mensagens ligadas a campos,
  foco visível, estados anunciados e operação por teclado.
- [x] Revisar desktop, tablet e smartphone; toque confortável, mapa não captura
  scroll da página sem intenção, estados vazios e erros explicam o próximo
  passo.

Aceite de implementação: fluxos usam controles nativos rotulados, foco visível,
estados anunciados, instruções textuais e respeitam motion reduzido; capturas
confirmam os breakpoints principais. Testes com leitor de tela não foram
executados nesta revisão.

### 7. Configuração de execução e tratamento de falhas

- [x] Remover Redis não utilizado da configuração/dependências padrão; manter
  nota para decisão futura quando houver caso de uso demonstrado.
- [x] Bind de API, web e PostgreSQL em loopback por padrão; acesso LAN requer
  ajuste explícito no host e permanece não autenticado.
- [x] Limitar Vite rodado diretamente no host a `127.0.0.1`; configurar bind
  interno do Compose separadamente para respeitar a porta publicada do host.
- [x] Configurar proxy `/api`, erros comuns e CORS por origens explícitas, sem
  curinga; Vite e Compose seguem o bind local padrão.
- [x] Atualizar README, documentação administrativa, dados demo, decisões e
  variáveis de ambiente; registrar rede privada confiável como limite sem login.
- [x] Validar a sintaxe resolvida do Compose e documentar reset demo sem apagar
  o volume PostgreSQL por padrão.
- [x] Iniciar banco limpo, aplicar Flyway V1–V4, conferir readiness e executar
  seed duas vezes no Compose local.

Aceite: instalação não publica portas na LAN por padrão; `WEB_BIND_ADDRESS`,
`API_BIND_ADDRESS` e `POSTGRES_BIND_ADDRESS` estão documentados; sem Redis o
stack inicia normalmente.

### 8. Validação final e evidências

- [x] Frontend: testes de serviços, formulários, estados da compra, lista,
  mapa/rota, editor, erros e fallback WebGL; `pnpm web:test`, `pnpm web:lint`,
  `pnpm web:build`.
- [x] Backend: `apps/api/mvnw.cmd test`; 147 testes passaram, incluindo
  validação/publicação de mapa, erros API, contratos/controllers e
  repositórios em Testcontainers.
- [x] Infra: `pnpm compose:config` resolveu a configuração.
- [x] Com Docker: Compose, health, migrações, seed idempotente, ativação manual
  pelo endpoint administrativo e roteiro real no navegador foram concluídos.
  O stack fica ativo para uso local; `pnpm compose:down` pode ser usado depois
  e preserva o volume por padrão.
- [x] Revisar diff e `git diff --check`; não dizer “testado” para checks que não
  puderam executar.
- [x] Revisar capturas cliente/admin em desktop/mobile; navegar foco de teclado
  e validar contraste dos pares principais; testes cobrem fallback WebGL2 e
  início pausado com `prefers-reduced-motion` e pausa automática após oito
  segundos. Sem overflow horizontal visível nos viewports 1440×900 e 390×844.
- [x] Atualizar status/evidências neste plano e enviar resumo com arquivos,
  comandos/resultado e bloqueios ambientais reais.

Aceite de implementação: busca → lista → rota API → SVG legível; CRUD/admin →
mapa DRAFT → validação → arquivamento manual → ativação; cena fictícia isolada
com fallback; documentos, contratos e dados compatíveis; execução privada
documentada. Aceite integrado concluído com PostgreSQL/Testcontainers;
MVP local disponível em http://127.0.0.1:5173.
