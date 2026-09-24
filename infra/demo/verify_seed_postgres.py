"""Exercise seed/clear against an owned disposable database in the local Compose postgres."""
from datetime import datetime
from pathlib import Path
import subprocess

from generate_market_aurora import MAP_VERSION, fixture, uid


def run(args, content=None, check=True):
    result = subprocess.run(args, input=content, text=True, encoding='utf-8', capture_output=True)
    if check and result.returncode:
        raise RuntimeError(result.stderr)
    return result


if __name__ == '__main__':
    root = Path(__file__).resolve().parents[2]
    container = 'marketfastroute-postgres-1'
    username = run(['docker', 'exec', container, 'printenv', 'POSTGRES_USER']).stdout.strip()
    database = 'mfr_demo_check_' + datetime.now().strftime('%Y%m%d%H%M%S%f')

    def psql(content, db=database, check=True):
        return run(['docker', 'exec', '-i', container, 'psql', '-X', '-q', '-t', '-A',
                    '-v', 'ON_ERROR_STOP=1', '-U', username, '-d', db], content, check)

    psql(f'CREATE DATABASE {database};', db='postgres')
    try:
        for migration in sorted((root / 'apps/api/src/main/resources/db/migration').glob('V*.sql')):
            psql(migration.read_text(encoding='utf-8'))
        seed = Path(__file__).with_name('seed-market-aurora.sql').read_text(encoding='utf-8')
        clear = Path(__file__).with_name('clear-market-aurora.sql').read_text(encoding='utf-8')
        psql(seed)

        def fingerprint():
            return psql('\n'.join(f"SELECT md5(string_agg(row_to_json(t)::text, '' ORDER BY id)) FROM {table} t;"
                                  for table in fixture())).stdout

        before = fingerprint()
        psql(seed)
        assert before == fingerprint(), 'A repeated seed changed persisted records'
        assert psql('SELECT count(*) FROM product_location;').stdout.strip() == '240'
        psql(f"UPDATE store_map SET status = 'ACTIVE' WHERE id = '{uid(f'map.v{MAP_VERSION}')}';")
        before = fingerprint()
        assert psql(seed, check=False).returncode != 0, 'Published map was overwritten'
        assert before == fingerprint(), 'Failed seed did not roll back'
        # Another store using a demo product blocks cleanup, without deleting anything.
        psql("INSERT INTO store (id, name, code, address, city, state, active) VALUES ('00000000-0000-0000-0000-000000000001', 'Independent', 'INDEPENDENT', 'Test', 'Test', 'SP', true);")
        psql("INSERT INTO store_product (store_id, product_id, active) SELECT '00000000-0000-0000-0000-000000000001', id, true FROM product LIMIT 1;")
        before = fingerprint()
        assert psql(clear, check=False).returncode != 0, 'Cleanup touched shared products'
        assert before == fingerprint(), 'Rejected cleanup changed data'
        psql("DELETE FROM store_product WHERE store_id = '00000000-0000-0000-0000-000000000001';")
        psql(clear)
        assert psql('SELECT count(*) FROM store;').stdout.strip() == '1'
        assert psql('SELECT count(*) FROM product;').stdout.strip() == '0'
        psql(seed)
        assert psql('SELECT count(*) FROM product_location;').stdout.strip() == '240'
        print('PostgreSQL: seed, repeatability, published-map protection, rollback, isolated clear and reseed passed.')
    finally:
        # This exact database was created above solely for this verification.
        psql(f'DROP DATABASE {database};', db='postgres')
