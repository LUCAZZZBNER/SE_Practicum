"""Independent browser smoke check for the shipped Vue application.

This intentionally does not import the frontend test suite. Chrome executes the
compiled bundle through the real Nginx proxy and evaluates one API request from
the page origin.
"""
import json
import os
import subprocess
import tempfile
import time
import shutil
import urllib.request
import websocket

ORIGIN = os.environ.get("DELIVERY_FRONTEND_URL", "http://localhost:5173").rstrip("/")

def main():
    with tempfile.TemporaryDirectory(prefix="delivery-chrome-") as profile:
        process = subprocess.Popen([
            "google-chrome", "--headless=new", "--no-sandbox", "--disable-gpu",
            "--remote-allow-origins=*",
            "--remote-debugging-port=19222", "--user-data-dir=" + profile,
            "about:blank"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        try:
            for _ in range(50):
                try:
                    version = json.load(urllib.request.urlopen("http://127.0.0.1:19222/json/version", timeout=1))
                    break
                except Exception:
                    time.sleep(.1)
            else:
                raise AssertionError("Chrome remote debugging did not start")
            targets = json.load(urllib.request.urlopen("http://127.0.0.1:19222/json/list"))
            target = next(item for item in targets if item.get("type") == "page")
            socket = websocket.create_connection(target["webSocketDebuggerUrl"], timeout=10)
            sequence = 0
            def command(method, params=None):
                nonlocal sequence
                sequence += 1
                socket.send(json.dumps({"id": sequence, "method": method, "params": params or {}}))
                while True:
                    message = json.loads(socket.recv())
                    if message.get("id") == sequence:
                        return message
            def evaluate(expression):
                nonlocal sequence
                sequence += 1
                socket.send(json.dumps({"id": sequence, "method": "Runtime.evaluate", "params": {"expression": expression, "awaitPromise": True, "returnByValue": True}}))
                while True:
                    message = json.loads(socket.recv())
                    if message.get("id") == sequence:
                        if "result" not in message:
                            raise AssertionError(message)
                        return message["result"]["result"].get("value")
            command("Page.enable")
            command("Runtime.enable")
            command("Page.navigate", {"url": ORIGIN + "/"})
            time.sleep(2)
            result = evaluate("(async()=>({title: document.title, root: !!document.querySelector('#app'), api: await fetch('/api/v1/shops?page=1&pageSize=1').then(r => r.status)}))()")
            assert result["root"], result
            assert result["api"] in (200, 401), result
            print("frontend-browser-smoke-ok", result["title"], result["api"])
            socket.close()
        finally:
            process.terminate()
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()
            shutil.rmtree(profile, ignore_errors=True)

if __name__ == "__main__":
    main()
