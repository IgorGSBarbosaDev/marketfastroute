"""Deterministic fictional store fixture. Run with --check to detect SQL drift."""
import hashlib
from pathlib import Path
import sys
import unicodedata


def uid(key):
    value = hashlib.md5(('marketfastroute.demo.' + key).encode()).hexdigest()
    return f'{value[:8]}-{value[8:12]}-{value[12:16]}-{value[16:20]}-{value[20:]}'


def slug(name):
    return ''.join(c for c in unicodedata.normalize('NFD', name.upper())
                   if not unicodedata.combining(c)).replace(' ', '-')


# Four short aisles per department; each line is a distinct assortment.
DEPARTMENTS = [
    ('MERCEARIA', 'Grãos e básicos', [
        'Arroz|Arroz integral|Arroz parboilizado|Arroz japonês',
        'Feijão carioca|Feijão preto|Lentilha|Grão de bico',
        'Farinha de trigo|Farinha de mandioca|Fubá|Tapioca',
        'Açúcar cristal|Açúcar demerara|Sal refinado|Sal grosso']),
    ('MASSAS', 'Massas e molhos', [
        'Espaguete|Penne|Fusilli|Lasanha seca',
        'Macarrão integral|Macarrão de arroz|Massa sem glúten|Nhoque seco',
        'Molho de tomate|Passata|Extrato de tomate|Tomate pelado',
        'Molho pesto|Molho branco|Molho bolonhesa|Molho de queijo']),
    ('TEMPEROS', 'Óleos e temperos', [
        'Óleo de soja|Óleo de girassol|Óleo de milho|Óleo de canola',
        'Azeite extravirgem|Vinagre de maçã|Vinagre balsâmico|Vinagre de vinho',
        'Orégano|Páprica|Pimenta do reino|Cominho',
        'Maionese|Mostarda|Ketchup|Molho de pimenta']),
    ('CONSERVAS', 'Conservas e sopas', [
        'Milho em conserva|Ervilha em conserva|Seleta de legumes|Palmito',
        'Atum em água|Sardinha em óleo|Patê de atum|Patê de azeitona',
        'Azeitona verde|Azeitona preta|Pepino em conserva|Alcaparra',
        'Sopa de legumes|Creme de cebola|Caldo de legumes|Caldo de frango']),
    ('CAFE', 'Café da manhã', [
        'Café torrado|Café em grãos|Café solúvel|Café descafeinado',
        'Chá de camomila|Chá verde|Chá de hortelã|Chá mate',
        'Aveia em flocos|Granola|Cereal de milho|Muesli',
        'Mel|Geleia de morango|Pasta de amendoim|Creme de avelã']),
    ('BISCOITOS', 'Biscoitos e snacks', [
        'Biscoito água e sal|Biscoito integral|Biscoito de arroz|Torrada integral',
        'Biscoito de chocolate|Biscoito de leite|Cookie de aveia|Wafer de baunilha',
        'Batata chips|Salgadinho de milho|Pipoca de micro-ondas|Amendoim torrado',
        'Castanha de caju|Castanha do pará|Nozes|Mix de frutas secas']),
    ('DOCES', 'Doces e confeitaria', [
        'Chocolate ao leite|Chocolate amargo|Chocolate branco|Bombom sortido',
        'Bala de fruta|Goma de mascar|Marshmallow|Paçoca',
        'Leite condensado|Creme de leite|Coco ralado|Chocolate em pó',
        'Gelatina|Mistura para bolo|Fermento químico|Essência de baunilha']),
    ('BEBIDAS', 'Bebidas', [
        'Água mineral|Água com gás|Água de coco|Água saborizada',
        'Suco de laranja|Suco de uva|Néctar de pêssego|Suco de maçã',
        'Refrigerante de cola|Refrigerante de guaraná|Tônica|Soda limonada',
        'Cerveja pilsen|Cerveja sem álcool|Vinho tinto|Vinho branco']),
    ('HORTIFRUTI', 'Hortifruti', [
        'Banana prata|Maçã gala|Laranja pera|Mamão',
        'Abacaxi|Melancia|Manga|Uva sem semente',
        'Tomate|Batata|Cebola|Cenoura',
        'Alface|Rúcula|Brócolis|Abobrinha']),
    ('PADARIA', 'Padaria', [
        'Pão integral|Pão de forma|Pão francês|Pão de centeio',
        'Bolo de milho|Bolo de cenoura|Bolo de laranja|Muffin',
        'Croissant|Pão de queijo assado|Rosquinha|Broa de milho',
        'Pão sírio|Tortilha|Bisnaguinha|Pão para hambúrguer']),
    ('HIGIENE', 'Cuidados pessoais', [
        'Shampoo|Condicionador|Máscara capilar|Creme para pentear',
        'Sabonete|Desodorante|Hidratante corporal|Protetor solar',
        'Creme dental|Escova dental|Fio dental|Enxaguante bucal',
        'Fralda infantil|Lenço umedecido|Absorvente|Algodão']),
    ('LIMPEZA', 'Casa e pet', [
        'Detergente neutro|Esponja|Desengordurante|Limpador multiuso',
        'Sabão em pó|Sabão líquido|Amaciante|Alvejante',
        'Papel higiênico|Papel toalha|Saco de lixo|Guardanapo',
        'Ração para cães|Ração para gatos|Areia sanitária|Petisco para cães']),
]
PERIMETER = [
    ('LATICINIOS', 'Laticínios', [
        'Leite integral|Leite desnatado|Leite sem lactose|Bebida vegetal',
        'Iogurte natural|Iogurte de morango|Kefir|Bebida láctea',
        'Manteiga|Requeijão|Creme de ricota|Margarina']),
    ('FRIOS', 'Frios e queijos', [
        'Queijo prato|Muçarela|Queijo minas|Parmesão',
        'Presunto|Peito de peru|Mortadela|Salame',
        'Ricota|Queijo cottage|Queijo brie|Queijo provolone']),
    ('CARNES', 'Carnes e pescados', [
        'Patinho|Acém|Alcatra|Carne moída',
        'Peito de frango|Coxa de frango|Lombo suíno|Linguiça fresca',
        'Filé de tilápia|Salmão|Camarão|Bacalhau']),
    ('CONGELADOS', 'Congelados', [
        'Ervilha congelada|Brócolis congelado|Seleta congelada|Espinafre congelado',
        'Pizza congelada|Lasanha congelada|Pão de queijo congelado|Batata congelada',
        'Sorvete de creme|Sorvete de chocolate|Açaí congelado|Picolé de fruta']),
]
LEGACY_SKUS = dict(zip(
    ['Banana prata', 'Maçã gala', 'Pão integral', 'Bolo de milho', 'Água mineral',
     'Suco de laranja', 'Leite integral', 'Queijo prato', 'Ervilha congelada',
     'Sorvete de creme', 'Arroz', 'Feijão carioca', 'Detergente neutro', 'Sabão em pó'],
    ['BANANA', 'MACA', 'PAO', 'BOLO', 'AGUA', 'SUCO', 'LEITE', 'QUEIJO', 'ERVILHA',
     'SORVETE', 'ARROZ', 'FEIJAO', 'DETERGENTE', 'SABAO']))


MAP_VERSION = 5


def fixture():
    store, map_id = uid('store'), uid(f'map.v{MAP_VERSION}')
    tables = {}
    accesses = []
    segments = []

    def mid(key):
        return uid(f'v{MAP_VERSION}.' + key)

    def add(table, **row):
        tables.setdefault(table, []).append(row)
        return row['id']

    def rect(table, code, name, x, y, width, height, **extra):
        return add(table, id=mid(table + '.' + code), map_id=map_id, code=code, name=name,
                   x=x, y=y, width=width, height=height, rotation=0, active=True, **extra)

    def horizontal(y, left=14, right=230):
        segments.append((left, y, right, y))

    def vertical(x, top=14, bottom=155):
        segments.append((x, top, x, bottom))

    add('store', id=store, name='Mercado Aurora — demonstração fictícia',
        code='MFR-DEMO-AURORA', address='Endereço demonstrativo', city='Cidade Fictícia', state='SP', active=True)
    add('store_map', id=map_id, store_id=store, version=MAP_VERSION,
        name='Aurora — mercado completo', width=240, height=160,
        scale_meters_per_unit=0.25, status='DRAFT')

    # The circulation network follows these explicit spines and branch aisles.
    # Their intersections are split below; no path can cut a fixture's clearance.
    for i, y in enumerate([26, 64, 106, 132, 155]):
        horizontal(y, right=168 if i == 0 else 230)
        rect('aisle', f'T{i+1}', ['Travessa dos frescos', 'Travessa central', 'Travessa dos setores',
                                'Acesso aos caixas', 'Saída dos caixas'][i], 10, y-3, 160 if i == 0 else 224, 6, sector_id=None)
    horizontal(14, 14, 168)
    horizontal(40, 168, 230)
    horizontal(148, 14, 74)
    for code, x in [('L', 14), ('C', 122), ('S', 168), ('R', 230)]:
        vertical(x)
        rect('aisle', code, 'Ligação entre setores', x-3, 14, 6, 141, sector_id=None)
    vertical(74, 106)
    vertical(20, 148)
    vertical(224, 132)
    for x in [112, 128, 144, 160, 176, 192, 208]:
        vertical(x, 132)

    sector_geometry = [
        (22, 30, 48, 29), (70, 30, 48, 29), (22, 70, 48, 33), (70, 70, 48, 33),
        (130, 28, 32, 56), (130, 82, 14, 52), (150, 82, 14, 52), (178, 60, 50, 44),
        (20, 108, 46, 38), (2, 28, 17, 88), (76, 108, 48, 22), (178, 112, 48, 18),
    ]

    def category(code, name):
        return add('category', id=uid('category.MFR-DEMO-' + code), code='MFR-DEMO-' + code, name=name, active=True)

    def stock(assortment, cat, sector, aisle, shelf, points):
        for index, name in enumerate(assortment.split('|')):
            sku = 'MFR-DEMO-' + LEGACY_SKUS.get(name, slug(name))
            product = add('product', id=uid('product.' + sku), category_id=cat, sku=sku, name=name,
                          description='Item sintético da base de demonstração.', active=True)
            sp = add('store_product', id=uid('store-product.' + sku), store_id=store, product_id=product, active=True)
            x, y = points[index//2]
            accesses.append((x, y))
            add('product_location', id=mid('location.' + sku), store_id=store, store_product_id=sp,
                map_id=map_id, sector_id=sector, aisle_id=aisle, shelf_block_id=shelf,
                module=f'Módulo {index+1}', shelf_level=index%3+1, x=x, y=y,
                navigation_node_id=mid(f'node.{x:g}.{y:g}'), primary_location=True, active=True)

    for dept, (code, name, assortments) in enumerate(DEPARTMENTS):
        cat = category(code, name)
        sector = rect('sector', code, name, *sector_geometry[dept])
        for column, assortment in enumerate(assortments):
            aisle_code = f'A{dept*4+column+1:02}'
            kind = 'Gôndola'
            if dept < 4:
                x = 22 + (dept%2)*48 + column*12
                y, width, height = (36 if dept < 2 else 76), 4, (20 if dept < 2 else 24)
                ax, ay, aw, ah = x+4, y, 8, height
                points = [(x+8, y+height/4), (x+8, y+height*3/4)]
                vertical(x+8, 26, 106)
            elif dept in (4, 5, 6):
                x = 130 if dept in (4, 5) else 150
                y = (32 if dept == 4 else 86) + column*12
                width, height = (32 if dept == 4 else 14), 4
                ax, ay, aw, ah = x, y+4, width, 8
                points = [(x+width/4, y+8), (x+width*3/4, y+8)]
                horizontal(y+8, 122, 168)
            elif dept == 7:
                x, y, width, height = 178+(column%2)*28, 68+(column//2)*24, 22, 4
                ax, ay, aw, ah = x, y+4, width, 8
                points = [(x+width/4, y+8), (x+width*3/4, y+8)]
                horizontal(y+8, 168, 230)
            elif dept == 8:
                kind = 'Ilha'
                x, y, width, height = 22+(column%2)*26, 112+(column//2)*16+(column%2)*6, 16, 4
                ax, ay, aw, ah = x, y+4, width, 8
                points = [(x+4, y+8), (x+12, y+8)]
                horizontal(y+8, 14, 74)
            elif dept == 9:
                kind = 'Padaria mural'
                x, y, width, height = 3, 32+column*22, 4, 14
                ax, ay, aw, ah = 10, y, 8, height
                points = [(14, y+4), (14, y+10)]
            elif dept == 10:
                x, y, width, height = 76+column*12, 112, 4, 16
                ax, ay, aw, ah = x+4, y, 8, height
                points = [(x+8, y+4), (x+8, y+12)]
                vertical(x+8, 106, 132)
            else:
                x, y, width, height = 178+column*12, 116, 4, 12
                ax, ay, aw, ah = x+4, y, 8, height
                points = [(x+8, y+3), (x+8, y+9)]
                vertical(x+8, 106, 132)
            aisle = rect('aisle', aisle_code, assortment.split('|')[0]+' e variedades', ax, ay, aw, ah, sector_id=sector)
            shelf = rect('shelf_block', f'G{dept*4+column+1:02}', kind+' · '+assortment.replace('|', ', '),
                         x, y, width, height, sector_id=sector, aisle_id=aisle)
            stock(assortment, cat, sector, aisle, shelf, points)

    # Cold wall at the back; service meat counters form a separate room-like zone.
    for group, (code, name, assortments) in enumerate(PERIMETER):
        cat = category(code, name)
        butcher = code == 'CARNES'
        cold_group = group if group < 2 else 2
        bounds = (174, 3, 62, 55) if butcher else (14+cold_group*48, 3, 46, 18)
        sector = rect('sector', code, 'Açougue e pescados' if butcher else name, *bounds)
        for column, assortment in enumerate(assortments):
            number = group*3+column+1
            x = 178+column*16 if butcher else 14+cold_group*48+column*16
            y = 20 if butcher else 3
            width, height = 14, (5 if butcher else 4)
            ay = 28 if butcher else 10
            aisle = rect('aisle', f'P{number:02}', assortment.split('|')[0]+' e variedades',
                         x, ay, width, 8, sector_id=sector)
            kind = 'Balcão de açougue' if butcher else 'Freezer mural' if code == 'CONGELADOS' else 'Refrigerador mural'
            shelf = rect('shelf_block', f'F{number:02}', kind+' · '+assortment.replace('|', ', '),
                         x, y, width, height, sector_id=sector, aisle_id=aisle)
            points = [(x+3.5, ay+4), (x+10.5, ay+4)]
            if butcher:
                for px, py in points:
                    vertical(px, py, 40)
            stock(assortment, cat, sector, aisle, shelf, points)

    butcher_sector = mid('sector.CARNES')
    for code, name, geom in [
        ('AC-PAREDE', 'Parede de serviço · Açougue e pescados', (172, 3, 64, 2)),
        ('AC-LATERAL', 'Parede de serviço · Preparo', (234, 5, 2, 53)),
        ('AC-RETORNO', 'Balcão de apoio · Atendimento', (172, 20, 4, 17)),
        ('AC-PREPARO', 'Bancada de preparo · Açougue', (179, 9, 49, 4)),
        ('AC-BANCO1', 'Cadeiras · Espera do açougue', (204, 44, 16, 3)),
        ('AC-BANCO2', 'Cadeiras · Espera do açougue', (204, 51, 16, 3)),
        ('AC-SENHAS', 'Totem de senhas · Retire sua senha', (186, 45, 3, 3)),
    ]:
        rect('shelf_block', code, name, *geom, sector_id=butcher_sector, aisle_id=None)
    # A short perpendicular endcap adds an orientation landmark without blocking the spine.
    for code, name, geom, sector in [
        ('PONTA1', 'Ponta de gôndola · Café da manhã', (132, 82, 12, 2), 'CAFE'),
        ('PONTA2', 'Ponta de gôndola · Bebidas', (182, 82, 12, 2), 'BEBIDAS'),
        ('MURAL', 'Prateleira mural · Casa e pet', (235, 112, 3, 18), 'LIMPEZA'),
    ]:
        rect('shelf_block', code, name, *geom, sector_id=mid('sector.'+sector), aisle_id=None)
    for lane, x in enumerate([104, 120, 136, 152, 168, 184, 200], 1):
        rect('shelf_block', f'CX{lane:02}', f'Caixa paralelo · {lane:02}', x, 138, 5, 14, sector_id=None, aisle_id=None)
    for code, name, geom in [
        ('PORTAL-E', 'Portal de entrada · Bem-vindo', (14, 156, 12, 2)),
        ('PORTAL-S', 'Portal de saída · Até breve', (218, 156, 12, 2)),
        ('CARRINHOS', 'Estação de carrinhos · Entrada', (42, 149, 12, 4)),
    ]:
        rect('shelf_block', code, name, *geom, sector_id=None, aisle_id=None)

    pois = [('ENTRANCE', 'Entrada', 20, 154), ('EXIT', 'Saída', 224, 154),
            ('CHECKOUT', 'Caixas', 112, 134), ('CART', 'Carrinhos', 40, 148)]
    special = {(x, y): kind for kind, _, x, y in pois}
    points = set(accesses) | set(special)
    # Add endpoints and every perpendicular intersection. Overlapping collinear
    # segments share the same nodes, and links are deduplicated by endpoint IDs.
    segments = sorted(set(segments))
    for x1, y1, x2, y2 in segments:
        points.update([(x1, y1), (x2, y2)])
    for x1, y, x2, same_y in segments:
        if y != same_y:
            continue
        for x, y1, same_x, y2 in segments:
            if x == same_x and x1 <= x <= x2 and y1 <= y <= y2:
                points.add((x, y))
    obstacles = tables['shelf_block']
    clearance = 1.6  # map units = 0.4 m from the route centerline

    def clear(a, b):
        for shelf in obstacles:
            left, right = shelf['x']-clearance, shelf['x']+shelf['width']+clearance
            top, bottom = shelf['y']-clearance, shelf['y']+shelf['height']+clearance
            if a[0] == b[0]:
                collision = left < a[0] < right and max(a[1], b[1]) >= top and min(a[1], b[1]) <= bottom
            else:
                collision = top < a[1] < bottom and max(a[0], b[0]) >= left and min(a[0], b[0]) <= right
            if collision:
                return False
        return True

    points = {point for point in points if clear(point, point)}
    links = set()
    for x1, y1, x2, y2 in segments:
        line = sorted(p for p in points if (x1 == x2 == p[0] and y1 <= p[1] <= y2)
                      or (y1 == y2 == p[1] and x1 <= p[0] <= x2))
        for a, b in zip(line, line[1:]):
            if clear(a, b):
                links.add((a, b))
    used = {point for edge in links for point in edge}
    for x, y in sorted(used):
        kind = special.get((x, y), 'PRODUCT_ACCESS' if (x, y) in accesses else 'INTERSECTION')
        add('map_node', id=mid(f'node.{x:g}.{y:g}'), map_id=map_id,
            type='PATH' if kind == 'CART' else kind, x=x, y=y, label=f'{kind} {x:g},{y:g}', active=True)
    for a, b in sorted(links):
        left, right = mid(f'node.{a[0]:g}.{a[1]:g}'), mid(f'node.{b[0]:g}.{b[1]:g}')
        add('map_edge', id=mid('edge.'+left+right), map_id=map_id, from_node_id=left, to_node_id=right,
            distance_meters=(abs(a[0]-b[0])+abs(a[1]-b[1]))*0.25, bidirectional=True, active=True)
    for kind, name, x, y in pois:
        add('point_of_interest', id=mid('poi.'+kind), map_id=map_id, navigation_node_id=mid(f'node.{x:g}.{y:g}'),
            type=kind, name=name, x=x, y=y, active=True)
    return tables


def literal(value):
    if value is None:
        return 'NULL'
    if isinstance(value, bool):
        return 'TRUE' if value else 'FALSE'
    if isinstance(value, (int, float)):
        return str(value)
    return "'" + value.replace("'", "''") + "'"


def sql(tables):
    statements = ['-- Generated by generate_market_aurora.py. Edit the generator, then regenerate.', 'BEGIN;']
    # Fail before any writes on ID/code collisions or non-draft replacement.
    guards = []
    for table in ['store', 'store_map', 'category', 'product', 'store_product', 'sector',
                  'aisle', 'shelf_block', 'map_node', 'map_edge', 'point_of_interest', 'product_location']:
        rows = tables[table]
        columns = list(rows[0])
        values = ',\n'.join('(' + ', '.join(literal(row[c]) for c in columns) + ')' for row in rows)
        statements.append(f'CREATE TEMP TABLE expected_{table} (LIKE {table} INCLUDING DEFAULTS) ON COMMIT DROP;')
        statements.append(f'INSERT INTO expected_{table} ({", ".join(columns)}) VALUES\n{values};')
        natural = {'store': ['code'], 'store_map': ['store_id', 'version'], 'category': ['code'],
                   'product': ['sku'], 'store_product': ['store_id', 'product_id'],
                   'sector': ['map_id', 'code'], 'aisle': ['map_id', 'code'],
                   'shelf_block': ['map_id', 'code']}.get(table, ['map_id'])
        if natural == ['map_id']:
            check = 'e.id = t.id AND e.map_id <> t.map_id'
        else:
            same = ' AND '.join(f't.{c} = e.{c}' for c in natural)
            different = ' OR '.join(f't.{c} <> e.{c}' for c in natural)
            check = f'(({same}) AND t.id <> e.id) OR (t.id = e.id AND ({different}))'
        guards.append(f"IF EXISTS (SELECT 1 FROM {table} t JOIN expected_{table} e ON {check}) THEN RAISE EXCEPTION 'Reserved demo identity conflict: {table}'; END IF;")
    guards.append(f"IF EXISTS (SELECT 1 FROM store_product sp JOIN expected_product p ON p.id = sp.product_id WHERE sp.store_id <> '{uid('store')}') THEN RAISE EXCEPTION 'Demo product linked to another store'; END IF;")
    guards.append("IF EXISTS (SELECT 1 FROM product p JOIN expected_category c ON c.id = p.category_id WHERE p.id NOT IN (SELECT id FROM expected_product)) THEN RAISE EXCEPTION 'Demo category used by another product'; END IF;")
    statements.append('DO $$ BEGIN\n' + '\n'.join(guards) + f"\nIF EXISTS (SELECT 1 FROM store_map WHERE id = '{uid(f'map.v{MAP_VERSION}')}' AND status <> 'DRAFT') THEN RAISE EXCEPTION 'Aurora target version is published; create a new version instead'; END IF;\nEND $$;")
    # Serialize with API writers before rechecking the draft under its row lock.
    statements.append(f"SELECT id FROM store_map WHERE id = '{uid(f'map.v{MAP_VERSION}')}' FOR UPDATE;")
    statements.append(f"DO $$ BEGIN IF EXISTS (SELECT 1 FROM store_map WHERE id = '{uid(f'map.v{MAP_VERSION}')}' AND status <> 'DRAFT') THEN RAISE EXCEPTION 'Aurora target version is not a draft'; END IF; END $$;")
    for table in ['store', 'category', 'product', 'store_product', 'store_map', 'sector',
                  'aisle', 'shelf_block', 'map_node', 'map_edge', 'point_of_interest', 'product_location']:
        columns = list(tables[table][0])
        update = ', '.join(f'{c} = EXCLUDED.{c}' for c in columns if c not in ('id', 'status'))
        statements.append(f'INSERT INTO {table} ({", ".join(columns)}) SELECT {", ".join(columns)} FROM expected_{table}\nON CONFLICT (id) DO UPDATE SET {update};')
    statements.append('COMMIT;\n')
    return '\n\n'.join(statements)


if __name__ == '__main__':
    target = Path(__file__).with_name('seed-market-aurora.sql')
    content = sql(fixture())
    if '--check' in sys.argv:
        if target.read_text(encoding='utf-8') != content:
            sys.exit('Demo SQL is stale; run python infra/demo/generate_market_aurora.py')
        print('Demo SQL matches the fixture.')
    else:
        target.write_text(content, encoding='utf-8', newline='\n')
