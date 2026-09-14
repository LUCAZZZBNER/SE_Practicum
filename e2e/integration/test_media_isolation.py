"""New HTTP acceptance tests with real MySQL and two independently launched services.

Run after packaging identity-service and media-service. No monolith is started.
All containers, credentials and rows are disposable and local to this test run.
"""
import base64
import json
import os
from pathlib import Path
import socket
import subprocess
import time
import unittest
import urllib.error
import urllib.request
import uuid

ROOT = Path(__file__).resolve().parents[2]
PNG = base64.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=')
SERVICE_TOKEN = 'test-service-token-01234567890123456789'
JWT_SECRET = 'test-jwt-signing-secret-01234567890123456789'


def command(*args, input=None):
    return subprocess.check_output(args, input=input, stderr=subprocess.STDOUT).decode().strip()


def port():
    with socket.socket() as sock:
        sock.bind(('127.0.0.1', 0))
        return sock.getsockname()[1]


def http(base, path, data=None, method=None, headers=None):
    headers = dict(headers or {})
    if isinstance(data, dict):
        data = json.dumps(data).encode()
        headers['Content-Type'] = 'application/json'
    request = urllib.request.Request(base + path, data=data, method=method, headers=headers)
    try:
        response = urllib.request.urlopen(request, timeout=8)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        raw = response.read()
        content = json.loads(raw) if 'application/json' in response.headers.get('Content-Type', '') else raw
        return response.status, content, dict(response.headers)


class MediaIsolationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.processes = []
        cls.files = []
        cls.container = 'delivery-media-test-' + uuid.uuid4().hex[:10]
        cls.addClassCleanup(cls.cleanup)
        command('docker', 'run', '-d', '--name', cls.container,
                '-p', '127.0.0.1::3306', '-e', 'MYSQL_ROOT_PASSWORD=isolation-root', 'mysql:8.4')
        for _ in range(90):
            try:
                cls.sql('SELECT 1')
                break
            except subprocess.CalledProcessError:
                time.sleep(1)
        else:
            raise RuntimeError('Disposable MySQL failed to start')
        mysql_port = command('docker', 'port', cls.container, '3306').rsplit(':', 1)[1]
        cls.sql("CREATE DATABASE delivery_identity; CREATE DATABASE delivery_media; "
                "CREATE USER 'identity_app'@'%' IDENTIFIED BY 'identity-test'; "
                "CREATE USER 'media_app'@'%' IDENTIFIED BY 'media-test'; "
                "GRANT ALL ON delivery_identity.* TO 'identity_app'@'%'; "
                "GRANT ALL ON delivery_media.* TO 'media_app'@'%';")
        cls.identity = 'http://127.0.0.1:' + str(port())
        cls.media = 'http://127.0.0.1:' + str(port())
        cls.mysql_port = mysql_port
        cls.start('identity', cls.identity)
        cls.start('media', cls.media)

    @classmethod
    def start(cls, name, base):
        env = dict(os.environ, SERVER_PORT=base.rsplit(':', 1)[1],
                   JWT_SECRET=JWT_SECRET, INTERNAL_SERVICE_TOKEN=SERVICE_TOKEN,
                   IDENTITY_SERVICE_URL=cls.identity, IDENTITY_INTERNAL_URL=cls.identity,
                   SPRING_FLYWAY_BASELINE_ON_MIGRATE='false')
        env.update({name.upper() + '_DATASOURCE_URL':
                    f'jdbc:mysql://127.0.0.1:{cls.mysql_port}/delivery_{name}?serverTimezone=UTC',
                    name.upper() + '_DB_USERNAME': name + '_app',
                    name.upper() + '_DB_PASSWORD': name + '-test'})
        logs = ROOT / 'tmp' / 'media-isolation'
        logs.mkdir(parents=True, exist_ok=True)
        log = open(logs / (name + '.log'), 'ab')
        cls.files.append(log)
        jar = ROOT / f'services/{name}-service/target/delivery-{name}-service-0.0.1-SNAPSHOT.jar'
        process = subprocess.Popen(['java', '-Xmx192m', '-jar', str(jar)], env=env,
                                   stdout=log, stderr=subprocess.STDOUT)
        cls.processes.append(process)
        for _ in range(60):
            if process.poll() is not None:
                raise RuntimeError(f'{name} exited; see {logs / (name + ".log")}')
            try:
                if http(base, '/actuator/health/readiness')[0] == 200:
                    return process
            except (OSError, urllib.error.URLError):
                pass
            time.sleep(1)
        raise RuntimeError(f'{name} did not become ready')

    @classmethod
    def sql(cls, sql):
        return command('docker', 'exec', '-i', cls.container, 'mysql', '-uroot',
                       '-pisolation-root', '-N', '-B', input=sql.encode())

    @classmethod
    def cleanup(cls):
        for process in cls.processes:
            if process.poll() is None:
                process.terminate()
                try:
                    process.wait(timeout=10)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait()
        for log in cls.files:
            log.close()
        subprocess.run(['docker', 'rm', '-f', cls.container], capture_output=True)

    def merchant(self):
        account = 'media-' + uuid.uuid4().hex[:10]
        payload = dict(account=account, password='testPassword123', passwordConfirm='testPassword123',
                       name='Media test merchant', phone='13800000000')
        status, body, _ = http(self.identity, '/api/v1/merchants', payload)
        self.assertEqual(status, 201, body)
        merchant_id = body['data']['id']
        status, body, _ = http(self.identity, '/api/v1/merchants/login',
                               dict(account=account, password='testPassword123'))
        self.assertEqual(status, 200, body)
        return merchant_id, body['data']['accessToken']

    def upload(self, token, content=PNG, mime='image/png'):
        boundary = 'media-test-boundary'
        body = (f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="pixel.png"\r\n'
                f'Content-Type: {mime}\r\n\r\n').encode() + content + f'\r\n--{boundary}--\r\n'.encode()
        headers = {'Content-Type': 'multipart/form-data; boundary=' + boundary}
        if token:
            headers['Authorization'] = 'Bearer ' + token
        return http(self.media, '/api/v1/files/images', body, 'POST', headers)

    def test_upload_and_readback_without_identity_tables_in_media_database(self):
        _, token = self.merchant()
        status, body, _ = self.upload(token)
        self.assertEqual(status, 201, body)
        asset = body['data']
        self.assertEqual(asset['size'], len(PNG))
        self.assertEqual(asset['contentType'], 'image/png')
        status, binary, headers = http(self.media, asset['url'])
        self.assertEqual(status, 200)
        self.assertEqual(binary, PNG)
        self.assertIn('immutable', headers.get('Cache-Control', ''))
        self.assertIn('1', self.sql('SELECT COUNT(*) FROM delivery_media.images WHERE id=' + str(asset['id'])))
        tables = self.sql('SHOW TABLES FROM delivery_media')
        self.assertNotIn('merchants', tables)
        self.assertNotIn('users', tables)
        with self.assertRaises(subprocess.CalledProcessError):
            command('docker', 'exec', self.container, 'mysql', '-h127.0.0.1', '-umedia_app',
                    '-pmedia-test', '-e', 'SELECT * FROM delivery_identity.merchants')

    def test_internal_merchant_lookup_requires_separate_service_credential(self):
        merchant_id, token = self.merchant()
        path = '/internal/v1/merchants/' + str(merchant_id)
        for headers in ({}, {'X-Service-Token': 'wrong'}, {'Authorization': 'Bearer ' + token}):
            status, _, _ = http(self.identity, path, headers=headers)
            self.assertEqual(status, 401)
        status, body, _ = http(self.identity, path, headers={'X-Service-Token': SERVICE_TOKEN})
        self.assertEqual(status, 200, body)
        self.assertEqual(body, {'id': merchant_id, 'status': 'ACTIVE'})

    def test_invalid_authentication_and_invalid_media_do_not_persist(self):
        status, _, _ = self.upload(None)
        self.assertEqual(status, 401)
        status, _, _ = self.upload('invalid')
        self.assertEqual(status, 401)
        _, token = self.merchant()
        before = self.sql('SELECT COUNT(*) FROM delivery_media.images')
        status, _, _ = self.upload(token, b'not an image')
        self.assertEqual(status, 400)
        self.assertEqual(before, self.sql('SELECT COUNT(*) FROM delivery_media.images'))

    def test_suspended_merchant_cannot_upload(self):
        merchant_id, token = self.merchant()
        self.sql("UPDATE delivery_identity.merchants SET status='SUSPENDED' WHERE id=" + str(merchant_id))
        status, body, _ = self.upload(token)
        self.assertEqual(status, 403, body)
        self.assertEqual(body['code'], 1202)


if __name__ == '__main__':
    unittest.main(verbosity=2)
