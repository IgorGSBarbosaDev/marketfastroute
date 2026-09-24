import unittest
from collections import Counter, deque
from pathlib import Path

from generate_market_aurora import fixture, sql


class AuroraLayoutTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.tables = fixture()

    def test_generated_sql_is_current(self):
        self.assertEqual(Path(__file__).with_name('seed-market-aurora.sql').read_text(encoding='utf-8'), sql(self.tables))

    def test_assortments_have_real_searchable_products_and_two_stops(self):
        tables = self.tables
        self.assertEqual(len(tables['product']), 240)
        self.assertEqual(len({p['sku'] for p in tables['product']}), 240)
        self.assertEqual(len(tables['product_location']), 240)
        counts = Counter(p['aisle_id'] for p in tables['product_location'])
        self.assertEqual(len(counts), 60)
        self.assertEqual(set(counts.values()), {4})
        for aisle in counts:
            locations = [p for p in tables['product_location'] if p['aisle_id'] == aisle]
            self.assertEqual(len({p['navigation_node_id'] for p in locations}), 2)
        shelves = {s['id']: s for s in tables['shelf_block']}
        nodes = {node['id'] for node in tables['map_node']}
        for location in tables['product_location']:
            self.assertIn(location['navigation_node_id'], nodes)
            shelf = shelves[location['shelf_block_id']]
            self.assertEqual((shelf['sector_id'], shelf['aisle_id']),
                             (location['sector_id'], location['aisle_id']))
        for poi in tables['point_of_interest']:
            self.assertIn(poi['navigation_node_id'], nodes)

    def test_geometry_fits_and_shelves_do_not_overlap(self):
        for table in ['sector', 'aisle', 'shelf_block']:
            for rect in self.tables[table]:
                self.assertGreater(rect['width'], 0)
                self.assertGreater(rect['height'], 0)
                self.assertGreaterEqual(rect['x'], 0)
                self.assertGreaterEqual(rect['y'], 0)
                self.assertLessEqual(rect['x'] + rect['width'], 240)
                self.assertLessEqual(rect['y'] + rect['height'], 160)
        shelves = self.tables['shelf_block']
        for index, a in enumerate(shelves):
            for b in shelves[index + 1:]:
                overlap = (a['x'] < b['x'] + b['width'] and b['x'] < a['x'] + a['width']
                           and a['y'] < b['y'] + b['height'] and b['y'] < a['y'] + a['height'])
                self.assertFalse(overlap, (a['code'], b['code']))

    def test_graph_edges_have_clearance_and_real_metric_costs(self):
        nodes = {n['id']: n for n in self.tables['map_node']}
        for edge in self.tables['map_edge']:
            a, b = nodes[edge['from_node_id']], nodes[edge['to_node_id']]
            self.assertTrue(a['x'] == b['x'] or a['y'] == b['y'])
            self.assertGreater(edge['distance_meters'], 0)
            self.assertEqual(edge['distance_meters'], (abs(a['x'] - b['x']) + abs(a['y'] - b['y'])) * 0.25)
            # Expand every obstacle by 0.4 m to catch paths grazing fixtures, not just intersections.
            for shelf in self.tables['shelf_block']:
                x1, x2 = shelf['x'] - 1.6, shelf['x'] + shelf['width'] + 1.6
                y1, y2 = shelf['y'] - 1.6, shelf['y'] + shelf['height'] + 1.6
                if a['x'] == b['x']:
                    collision = x1 < a['x'] < x2 and max(a['y'], b['y']) > y1 and min(a['y'], b['y']) < y2
                else:
                    collision = y1 < a['y'] < y2 and max(a['x'], b['x']) > x1 and min(a['x'], b['x']) < x2
                self.assertFalse(collision, (a['label'], b['label'], shelf['code']))

    def test_all_nodes_reachable_from_entrance_and_checkout(self):
        neighbors = {n['id']: set() for n in self.tables['map_node']}
        for edge in self.tables['map_edge']:
            neighbors[edge['from_node_id']].add(edge['to_node_id'])
            neighbors[edge['to_node_id']].add(edge['from_node_id'])
        for kind in ['ENTRANCE', 'CHECKOUT']:
            start = next(n['id'] for n in self.tables['map_node'] if n['type'] == kind)
            queue, visited = deque([start]), {start}
            while queue:
                for other in neighbors[queue.popleft()] - visited:
                    visited.add(other)
                    queue.append(other)
            self.assertEqual(visited, set(neighbors))


if __name__ == '__main__':
    unittest.main()
