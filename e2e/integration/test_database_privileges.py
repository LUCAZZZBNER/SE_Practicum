"""Black-box verification of service database users and cross-domain grants.

The test provisions MySQL from the same initializer used by Compose and checks
the credentials from outside the application code. Existing service tests are
not used as evidence for database isolation.
"""
import subprocess
import time
import unittest
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


class DatabasePrivilegeTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.container = "delivery-privilege-test-" + uuid.uuid4().hex[:10]
        cls.addClassCleanup(cls.cleanup)
        cls.exec_cmd(
            "docker",
            "run",
            "-d",
            "--name",
            cls.container,
            "-e",
            "MYSQL_ROOT_PASSWORD=priv-root",
            "-e",
            "MYSQL_DATABASE=delivery_dev",
            "-e",
            "MYSQL_USER=delivery_app",
            "-e",
            "MYSQL_PASSWORD=delivery-password",
            "-e",
            "IDENTITY_DB_USERNAME=identity_app",
            "-e",
            "IDENTITY_DB_PASSWORD=identity-password",
            "-e",
            "CATALOG_DB_USERNAME=catalog_app",
            "-e",
            "CATALOG_DB_PASSWORD=catalog-password",
            "-e",
            "CART_DB_USERNAME=cart_app",
            "-e",
            "CART_DB_PASSWORD=cart-password",
            "-e",
            "ORDER_DB_USERNAME=order_app",
            "-e",
            "ORDER_DB_PASSWORD=order-password",
            "-e",
            "MEDIA_DB_USERNAME=media_app",
            "-e",
            "MEDIA_DB_PASSWORD=media-password",
            "-v",
            f"{ROOT / 'deploy/mysql/01-create-service-databases.sh'}:/docker-entrypoint-initdb.d/01-create-service-databases.sh:ro",
            "mysql:8.4",
        )
        for _ in range(120):
            try:
                cls.root_sql("SELECT 1")
                break
            except subprocess.CalledProcessError:
                time.sleep(1)
        else:
            raise RuntimeError("Disposable MySQL failed to start")
        cls.root_sql(
            "CREATE TABLE delivery_identity.users (id INT PRIMARY KEY);"
            "CREATE TABLE delivery_catalog.products (id INT PRIMARY KEY, stock INT);"
            "CREATE TABLE delivery_cart.cart_items (id INT PRIMARY KEY);"
        )

    @classmethod
    def exec_cmd(cls, *args, input=None):
        return subprocess.check_output(args, input=input, stderr=subprocess.STDOUT).decode()

    @classmethod
    def root_sql(cls, statement):
        return cls.exec_cmd(
            "docker",
            "exec",
            "-i",
            cls.container,
            "mysql",
            "-uroot",
            "-ppriv-root",
            "-N",
            "-B",
            input=statement.encode(),
        )

    @classmethod
    def user_sql(cls, user, password, statement):
        return cls.exec_cmd(
            "docker",
            "exec",
            "-i",
            cls.container,
            "mysql",
            "-h127.0.0.1",
            f"-u{user}",
            f"-p{password}",
            "-N",
            "-B",
            input=statement.encode(),
        )

    @classmethod
    def cleanup(cls):
        subprocess.run(["docker", "rm", "-f", cls.container], capture_output=True)

    def test_each_service_can_write_only_its_owned_schema(self):
        self.user_sql("identity_app", "identity-password", "INSERT INTO delivery_identity.users VALUES (1)")
        self.user_sql("catalog_app", "catalog-password", "INSERT INTO delivery_catalog.products VALUES (1, 2)")
        with self.assertRaises(subprocess.CalledProcessError):
            self.user_sql("identity_app", "identity-password", "SELECT * FROM delivery_catalog.products")
        with self.assertRaises(subprocess.CalledProcessError):
            self.user_sql("catalog_app", "catalog-password", "INSERT INTO delivery_identity.users VALUES (2)")

    def test_order_cannot_write_catalog_or_cart_schemas(self):
        with self.assertRaises(subprocess.CalledProcessError):
            self.user_sql("order_app", "order-password", "UPDATE delivery_catalog.products SET stock=stock-1 WHERE id=1")
        self.root_sql("INSERT INTO delivery_cart.cart_items VALUES (7)")
        with self.assertRaises(subprocess.CalledProcessError):
            self.user_sql("order_app", "order-password", "DELETE FROM delivery_cart.cart_items WHERE id=7")
        with self.assertRaises(subprocess.CalledProcessError):
            self.user_sql("order_app", "order-password", "DROP TABLE delivery_catalog.products")


if __name__ == "__main__":
    unittest.main(verbosity=2)
