# Planejamento do catálogo — Aurora v5

Catálogo fictício implementado em `infra/demo/generate_market_aurora.py`.
Cada linha corresponde a um expositor principal, com quatro produtos e dois pontos de acesso.

| Corredor | Setor | Produtos |
| --- | --- | --- |
| A01 | Grãos e básicos | Arroz; Arroz integral; Arroz parboilizado; Arroz japonês |
| A02 | Grãos e básicos | Feijão carioca; Feijão preto; Lentilha; Grão de bico |
| A03 | Grãos e básicos | Farinha de trigo; Farinha de mandioca; Fubá; Tapioca |
| A04 | Grãos e básicos | Açúcar cristal; Açúcar demerara; Sal refinado; Sal grosso |
| A05 | Massas e molhos | Espaguete; Penne; Fusilli; Lasanha seca |
| A06 | Massas e molhos | Macarrão integral; Macarrão de arroz; Massa sem glúten; Nhoque seco |
| A07 | Massas e molhos | Molho de tomate; Passata; Extrato de tomate; Tomate pelado |
| A08 | Massas e molhos | Molho pesto; Molho branco; Molho bolonhesa; Molho de queijo |
| A09 | Óleos e temperos | Óleo de soja; Óleo de girassol; Óleo de milho; Óleo de canola |
| A10 | Óleos e temperos | Azeite extravirgem; Vinagre de maçã; Vinagre balsâmico; Vinagre de vinho |
| A11 | Óleos e temperos | Orégano; Páprica; Pimenta do reino; Cominho |
| A12 | Óleos e temperos | Maionese; Mostarda; Ketchup; Molho de pimenta |
| A13 | Conservas e sopas | Milho em conserva; Ervilha em conserva; Seleta de legumes; Palmito |
| A14 | Conservas e sopas | Atum em água; Sardinha em óleo; Patê de atum; Patê de azeitona |
| A15 | Conservas e sopas | Azeitona verde; Azeitona preta; Pepino em conserva; Alcaparra |
| A16 | Conservas e sopas | Sopa de legumes; Creme de cebola; Caldo de legumes; Caldo de frango |
| A17 | Café da manhã | Café torrado; Café em grãos; Café solúvel; Café descafeinado |
| A18 | Café da manhã | Chá de camomila; Chá verde; Chá de hortelã; Chá mate |
| A19 | Café da manhã | Aveia em flocos; Granola; Cereal de milho; Muesli |
| A20 | Café da manhã | Mel; Geleia de morango; Pasta de amendoim; Creme de avelã |
| A21 | Biscoitos e snacks | Biscoito água e sal; Biscoito integral; Biscoito de arroz; Torrada integral |
| A22 | Biscoitos e snacks | Biscoito de chocolate; Biscoito de leite; Cookie de aveia; Wafer de baunilha |
| A23 | Biscoitos e snacks | Batata chips; Salgadinho de milho; Pipoca de micro-ondas; Amendoim torrado |
| A24 | Biscoitos e snacks | Castanha de caju; Castanha do pará; Nozes; Mix de frutas secas |
| A25 | Doces e confeitaria | Chocolate ao leite; Chocolate amargo; Chocolate branco; Bombom sortido |
| A26 | Doces e confeitaria | Bala de fruta; Goma de mascar; Marshmallow; Paçoca |
| A27 | Doces e confeitaria | Leite condensado; Creme de leite; Coco ralado; Chocolate em pó |
| A28 | Doces e confeitaria | Gelatina; Mistura para bolo; Fermento químico; Essência de baunilha |
| A29 | Bebidas | Água mineral; Água com gás; Água de coco; Água saborizada |
| A30 | Bebidas | Suco de laranja; Suco de uva; Néctar de pêssego; Suco de maçã |
| A31 | Bebidas | Refrigerante de cola; Refrigerante de guaraná; Tônica; Soda limonada |
| A32 | Bebidas | Cerveja pilsen; Cerveja sem álcool; Vinho tinto; Vinho branco |
| A33 | Hortifruti | Banana prata; Maçã gala; Laranja pera; Mamão |
| A34 | Hortifruti | Abacaxi; Melancia; Manga; Uva sem semente |
| A35 | Hortifruti | Tomate; Batata; Cebola; Cenoura |
| A36 | Hortifruti | Alface; Rúcula; Brócolis; Abobrinha |
| A37 | Padaria | Pão integral; Pão de forma; Pão francês; Pão de centeio |
| A38 | Padaria | Bolo de milho; Bolo de cenoura; Bolo de laranja; Muffin |
| A39 | Padaria | Croissant; Pão de queijo assado; Rosquinha; Broa de milho |
| A40 | Padaria | Pão sírio; Tortilha; Bisnaguinha; Pão para hambúrguer |
| A41 | Cuidados pessoais | Shampoo; Condicionador; Máscara capilar; Creme para pentear |
| A42 | Cuidados pessoais | Sabonete; Desodorante; Hidratante corporal; Protetor solar |
| A43 | Cuidados pessoais | Creme dental; Escova dental; Fio dental; Enxaguante bucal |
| A44 | Cuidados pessoais | Fralda infantil; Lenço umedecido; Absorvente; Algodão |
| A45 | Casa e pet | Detergente neutro; Esponja; Desengordurante; Limpador multiuso |
| A46 | Casa e pet | Sabão em pó; Sabão líquido; Amaciante; Alvejante |
| A47 | Casa e pet | Papel higiênico; Papel toalha; Saco de lixo; Guardanapo |
| A48 | Casa e pet | Ração para cães; Ração para gatos; Areia sanitária; Petisco para cães |
| P01 | Laticínios | Leite integral; Leite desnatado; Leite sem lactose; Bebida vegetal |
| P02 | Laticínios | Iogurte natural; Iogurte de morango; Kefir; Bebida láctea |
| P03 | Laticínios | Manteiga; Requeijão; Creme de ricota; Margarina |
| P04 | Frios e queijos | Queijo prato; Muçarela; Queijo minas; Parmesão |
| P05 | Frios e queijos | Presunto; Peito de peru; Mortadela; Salame |
| P06 | Frios e queijos | Ricota; Queijo cottage; Queijo brie; Queijo provolone |
| P07 | Açougue e pescados | Patinho; Acém; Alcatra; Carne moída |
| P08 | Açougue e pescados | Peito de frango; Coxa de frango; Lombo suíno; Linguiça fresca |
| P09 | Açougue e pescados | Filé de tilápia; Salmão; Camarão; Bacalhau |
| P10 | Congelados | Ervilha congelada; Brócolis congelado; Seleta congelada; Espinafre congelado |
| P11 | Congelados | Pizza congelada; Lasanha congelada; Pão de queijo congelado; Batata congelada |
| P12 | Congelados | Sorvete de creme; Sorvete de chocolate; Açaí congelado; Picolé de fruta |
