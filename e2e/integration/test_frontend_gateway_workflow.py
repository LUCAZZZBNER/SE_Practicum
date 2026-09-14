"""Independent black-box workflow through the frontend proxy and gateway.

Run against a disposable Compose stack with DELIVERY_BASE_URL set to the frontend
origin, for example http://localhost:5173/api/v1.
"""
import base64
import json
import os
import urllib.error
import urllib.request
import uuid

BASE = os.environ.get("DELIVERY_BASE_URL", "http://localhost:5173/api/v1").rstrip("/")
PASSWORD = "Passw0rd!"
PNG = base64.b64decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=")


def call(method, path, body=None, token=None, headers=None, expected=200):
    request_headers = {"Content-Type": "application/json", **(headers or {})}
    if token:
        request_headers["Authorization"] = "Bearer " + token
    payload = None if body is None else json.dumps(body).encode()
    request = urllib.request.Request(BASE + path, data=payload, method=method, headers=request_headers)
    try:
        response = urllib.request.urlopen(request, timeout=15)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        result = json.loads(response.read())
    assert response.status == expected, (method, path, response.status, result)
    return result["data"]


def upload(token):
    boundary = "workflow-" + uuid.uuid4().hex
    body = (f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="pixel.png"\r\nContent-Type: image/png\r\n\r\n').encode() + PNG + f"\r\n--{boundary}--\r\n".encode()
    request = urllib.request.Request(BASE + "/files/images", data=body, method="POST", headers={"Authorization": "Bearer " + token, "Content-Type": "multipart/form-data; boundary=" + boundary})
    with urllib.request.urlopen(request, timeout=15) as response:
        assert response.status == 201
        return json.loads(response.read())["data"]


def main():
    suffix = uuid.uuid4().hex[:10]
    password = PASSWORD
    merchant = {"account": "merchant-" + suffix, "password": password, "passwordConfirm": password, "name": "Workflow Merchant", "phone": "13800000001"}
    user = {"account": "user-" + suffix, "password": password, "passwordConfirm": password, "nickname": "Workflow User", "phone": "13900000001"}
    call("POST", "/merchants", merchant, expected=201)
    merchant_token = call("POST", "/merchants/login", {"account": merchant["account"], "password": password})["accessToken"]
    call("POST", "/users", user, expected=201)
    user_token = call("POST", "/users/login", {"account": user["account"], "password": password})["accessToken"]
    shop = call("POST", "/shops", {"name": "Workflow Shop"}, merchant_token, expected=201)
    call("PATCH", f"/shops/{shop['id']}/address", {"region": "Region", "detail": "Detail", "phone": "13800000002"}, merchant_token)
    call("PATCH", f"/shops/{shop['id']}", {"status": "OPEN"}, merchant_token)
    category = call("POST", f"/shops/{shop['id']}/categories", {"name": "Food", "sortOrder": 1}, merchant_token, expected=201)
    image = upload(merchant_token)
    product = call("POST", "/products", {"shopId": shop["id"], "categoryId": category["id"], "name": "Workflow Item", "price": 10, "stock": 2, "imageId": image["id"], "skus": [{"name": "Default", "price": 10, "stock": 2}]}, merchant_token, expected=201)
    sku = call("PATCH", f"/skus/{product['skus'][0]['id']}", {"status": "ON_SALE", "version": product["skus"][0]["version"]}, merchant_token)
    product = call("PATCH", f"/products/{product['id']}", {"status": "ON_SALE", "version": product["version"]}, merchant_token)
    address = call("POST", "/user-addresses", {"recipient": "Workflow", "phone": "13900000002", "region": "Region", "detail": "Detail", "isDefault": True}, user_token, expected=201)
    cart = call("POST", "/cart-items", {"skuId": sku["id"], "quantity": 1}, user_token, expected=201)
    body = {"addressId": address["id"], "items": [{"cartItemId": cart["id"], "skuVersion": sku["version"]}]}
    key = "workflow-" + suffix
    order = call("POST", "/orders", body, user_token, {"X-Idempotency-Key": key}, 201)
    retry = call("POST", "/orders", body, user_token, {"X-Idempotency-Key": key}, 201)
    assert retry["id"] == order["id"]
    print("frontend-gateway-workflow-ok", order["id"])
    return {"merchant_token": merchant_token, "user_token": user_token, "sku": sku,
            "address": address, "body": body, "order": order, "shop": shop,
            "product": product}


if __name__ == "__main__":
    main()
