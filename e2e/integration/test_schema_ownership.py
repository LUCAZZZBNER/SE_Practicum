"""Black-box verification of physical service schemas and local read models.

This test intentionally does not start the monolith or reuse application tests. It
provisions the same database layout as Compose, applies the checked-in Flyway SQL,
and exercises ownership and cross-domain write behavior directly.
"""
import subprocess
import time
import unittest
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


class SchemaOwnershipTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.container = 'delivery-schema-test-' + uuid.uuid4().hex[:10]
        cls.addClassCleanup(cls.cleanup)
        cls.exec_cmd('docker', 'run', '-d', '--name', cls.container,
                '-e', 'MYSQL_ROOT_PASSWORD=schema-root', 'mysql:8.4')
        for _ in range(120):
            try:
                cls.sql('SELECT 1')
                break
            except subprocess.CalledProcessError:
                time.sleep(1)
        else:
            raise RuntimeError('Disposable MySQL failed to start')
        cls.sql('CREATE DATABASE delivery_identity; CREATE DATABASE delivery_catalog; '
                'CREATE DATABASE delivery_cart; CREATE DATABASE delivery_order; CREATE DATABASE delivery_media;')
        for name in ('identity', 'media', 'catalog', 'cart', 'order'):
            migration = ROOT / f'services/{name}-service/src/main/resources/db/migration/V1__{name}_schema.sql'
            cls.sql(migration.read_text(), f'delivery_{name}')

    @classmethod
    def exec_cmd(cls, *args, input=None):
        return subprocess.check_output(args, input=input, stderr=subprocess.STDOUT).decode()

    @classmethod
    def sql(cls, statement, database=None):
        args = ['docker', 'exec', '-i', cls.container, 'mysql', '-uroot', '-pschema-root', '-N', '-B']
        if database:
            args.append(database)
        output = cls.exec_cmd(*args, input=statement.encode())
        return '\n'.join(line for line in output.splitlines()
                          if not line.startswith('mysql: [Warning]')) + '\n'

    @classmethod
    def cleanup(cls):
        subprocess.run(['docker', 'rm', '-f', cls.container], capture_output=True)

    def test_each_domain_has_owned_tables_and_only_read_projections(self):
        self.assertIn('users', self.sql('SHOW TABLES', 'delivery_identity'))
        self.assertIn('products', self.sql('SHOW TABLES', 'delivery_catalog'))
        self.assertIn('cart_items', self.sql('SHOW TABLES', 'delivery_cart'))
        self.assertIn('orders', self.sql('SHOW TABLES', 'delivery_order'))
        self.assertIn('images', self.sql('SHOW TABLES', 'delivery_media'))
        self.assertIn('delivery_cart\tcart_items\tBASE TABLE', self.sql(
            'SELECT TABLE_SCHEMA,TABLE_NAME,TABLE_TYPE FROM information_schema.tables '
            'WHERE TABLE_SCHEMA="delivery_cart" AND TABLE_NAME="cart_items"'))
        self.assertIn('delivery_cart\tproducts\tBASE TABLE', self.sql(
            'SELECT TABLE_SCHEMA,TABLE_NAME,TABLE_TYPE FROM information_schema.tables '
            'WHERE TABLE_SCHEMA="delivery_cart" AND TABLE_NAME="products"'))

    def test_local_read_models_are_independent(self):
        self.sql("INSERT INTO delivery_identity.merchants(account,password_hash,name,phone) "
                  "VALUES ('m','p','Merchant','13800000000');")
        self.sql("INSERT INTO delivery_catalog.shops(merchant_id,name,status) VALUES (1,'Shop','OPEN');")
        self.sql("INSERT INTO delivery_catalog.product_categories(shop_id,name) VALUES (1,'Food');")
        self.sql("INSERT INTO delivery_catalog.products(shop_id,category_id,name,price,stock,status) "
                  "VALUES (1,1,'Item',10,2,'ON_SALE');")
        self.sql("INSERT INTO delivery_order.products(id,shop_id,category_id,name,price,stock,status,version) "
                 "VALUES (1,1,1,'Item',10,2,'ON_SALE',1);")
        self.assertEqual('2\n', self.sql('SELECT stock FROM products WHERE id=1', 'delivery_order'))
        self.sql("INSERT INTO delivery_cart.cart_items(user_id,product_id,quantity) VALUES (7,1,1);")
        self.sql("INSERT INTO delivery_order.cart_items(id,user_id,product_id,quantity) VALUES (1,7,1,1);")
        self.assertEqual('1\n', self.sql('SELECT COUNT(*) FROM cart_items WHERE user_id=7', 'delivery_order'))


if __name__ == '__main__':
    unittest.main(verbosity=2)
