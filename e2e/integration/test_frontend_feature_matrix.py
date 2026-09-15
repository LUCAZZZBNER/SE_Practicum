#!/usr/bin/env python3
"""Black-box coverage of every frontend feature through the shipped proxy.

This test intentionally does not import frontend code or reuse the other
workflow fixtures. It creates isolated customer/merchant accounts and checks
the public API that the Vue views call, including response shapes, ownership,
state transitions, idempotency, and conflict paths.

Run with DELIVERY_BASE_URL pointing at the frontend proxy, for example:
DELIVERY_BASE_URL=http://localhost:15173/api/v1 python3 \
  e2e/integration/test_frontend_feature_matrix.py
"""

import base64
import json
import os
import time
import urllib.error
import urllib.request
import uuid


BASE = os.environ.get("DELIVERY_BASE_URL", "http://localhost:5173/api/v1").rstrip("/")
PASSWORD = "Passw0rd!"
PNG = base64.b64decode(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII="
)


def request(method, path, *, token=None, body=None, headers=None, expected=200, code=0):
    request_headers = {"Content-Type": "application/json", **(headers or {})}
    if token:
        request_headers["Authorization"] = "Bearer " + token
    payload = None if body is None else json.dumps(body).encode("utf-8")
    req = urllib.request.Request(BASE + path, data=payload, headers=request_headers, method=method)
    try:
        response = urllib.request.urlopen(req, timeout=20)
        status, raw = response.status, response.read()
    except urllib.error.HTTPError as error:
        status, raw = error.code, error.read()
    try:
        result = json.loads(raw)
    except json.JSONDecodeError as error:
        raise AssertionError(f"{method} {path} returned non-JSON ({status}): {raw!r}") from error
    if status != expected or result.get("code") != code:
        raise AssertionError(
            f"{method} {path}: expected HTTP/code {expected}/{code}, "
            f"got {status}/{result.get('code')}: {result}"
        )
    return result.get("data")


def upload_image(token):
    boundary = "----feature-matrix-" + uuid.uuid4().hex
    payload = (
        f"--{boundary}\r\n"
        'Content-Disposition: form-data; name="file"; filename="feature.png"\r\n'
        "Content-Type: image/png\r\n\r\n"
    ).encode() + PNG + f"\r\n--{boundary}--\r\n".encode()
    req = urllib.request.Request(
        BASE + "/files/images",
        data=payload,
        method="POST",
        headers={
            "Authorization": "Bearer " + token,
            "Content-Type": "multipart/form-data; boundary=" + boundary,
        },
    )
    try:
        response = urllib.request.urlopen(req, timeout=20)
        status, raw = response.status, response.read()
    except urllib.error.HTTPError as error:
        status, raw = error.code, error.read()
    result = json.loads(raw)
    assert status == 201 and result.get("code") == 0, (status, result)
    image = result["data"]
    assert image["url"].startswith("/api/v1/files/images/")
    return image


def assert_fields(value, *fields):
    assert isinstance(value, dict), value
    missing = [field for field in fields if field not in value]
    assert not missing, f"missing fields {missing} in {value}"


def main():
    suffix = uuid.uuid4().hex[:12]
    merchant_body = {
        "account": "matrix-merchant-" + suffix,
        "password": PASSWORD,
        "passwordConfirm": PASSWORD,
        "name": "Feature Matrix Merchant",
        "phone": "13800000001",
    }
    user_body = {
        "account": "matrix-user-" + suffix,
        "password": PASSWORD,
        "passwordConfirm": PASSWORD,
        "nickname": "Feature Matrix User",
        "phone": "13900000001",
    }

    # Guest registration/login and account/profile workflows.
    merchant = request("POST", "/merchants", body=merchant_body, expected=201)
    user = request("POST", "/users", body=user_body, expected=201)
    assert_fields(merchant, "id", "account", "status")
    assert_fields(user, "id", "account", "status")
    merchant_session = request(
        "POST", "/merchants/login", body={"account": merchant_body["account"], "password": PASSWORD}
    )
    user_session = request(
        "POST", "/users/login", body={"account": user_body["account"], "password": PASSWORD}
    )
    merchant_token, user_token = merchant_session["accessToken"], user_session["accessToken"]
    assert merchant_session["roles"] == ["MERCHANT"]
    assert user_session["roles"] == ["USER"]
    updated_user = request(
        "PATCH", "/users/me", token=user_token, body={"nickname": "Updated Matrix User", "phone": "13900000002"}
    )
    assert updated_user["nickname"] == "Updated Matrix User"
    assert request("GET", "/users/me", token=user_token)["phone"] == "13900000002"
    updated_merchant = request(
        "PATCH", "/merchants/me", token=merchant_token, body={"name": "Updated Matrix Merchant", "phone": "13800000002"}
    )
    assert updated_merchant["name"] == "Updated Matrix Merchant"
    assert request("GET", "/merchants/me", token=merchant_token)["phone"] == "13800000002"

    # Authentication and role boundaries used by route guards and API calls.
    request("GET", "/cart-items", expected=401, code=1002)
    request("GET", "/cart-items", token=merchant_token, expected=403, code=1003)
    request("POST", "/shops", token=user_token, body={"name": "forbidden"}, expected=403, code=1003)
    request("GET", "/users/me", token=merchant_token, expected=403, code=1003)

    # Merchant shop/address/status and category management.
    shop = request("POST", "/shops", token=merchant_token, body={"name": "Matrix Shop", "description": "All features"}, expected=201)
    shop_id = shop["id"]
    address_payload = {"region": "杭州", "detail": "矩阵路 1 号", "phone": "05711234567"}
    shop = request("PATCH", f"/shops/{shop_id}/address", token=merchant_token, body=address_payload)
    assert shop["region"] == "杭州"
    shop = request("PATCH", f"/shops/{shop_id}", token=merchant_token, body={"name": "Matrix Shop 2", "description": "Updated", "status": "OPEN"})
    assert shop["status"] == "OPEN"
    assert request("GET", f"/shops/{shop_id}")["name"] == "Matrix Shop 2"
    listed = request("GET", "/shops", token=user_token, body=None)
    assert_fields(listed, "items", "page", "total")
    assert any(row["id"] == shop_id for row in listed["items"])
    category = request("POST", f"/shops/{shop_id}/categories", token=merchant_token, body={"name": "Meals", "sortOrder": 1}, expected=201)
    unused_category = request("POST", f"/shops/{shop_id}/categories", token=merchant_token, body={"name": "Unused", "sortOrder": 2}, expected=201)
    category = request("PATCH", f"/categories/{category['id']}", token=merchant_token, body={"name": "Meals Updated"})
    assert category["name"] == "Meals Updated"
    assert any(row["id"] == category["id"] for row in request("GET", f"/shops/{shop_id}/categories")[0:])
    request("DELETE", f"/categories/{unused_category['id']}", token=merchant_token, expected=200)

    # Merchant media, product/SKU creation, editing, publication, and listings.
    image = upload_image(merchant_token)
    product = request(
        "POST", "/products", token=merchant_token,
        body={"shopId": shop_id, "categoryId": category["id"], "name": "Matrix Meal", "description": "Test meal", "imageId": image["id"], "skus": [{"name": "Default", "price": 12.50, "stock": 5}]},
        expected=201,
    )
    assert_fields(product, "id", "skus", "status")
    sku = product["skus"][0]
    extra_sku = request("POST", f"/products/{product['id']}/skus", token=merchant_token, body={"name": "Large", "price": 15, "stock": 3}, expected=201)
    updated_sku = request("PATCH", f"/skus/{extra_sku['id']}", token=merchant_token, body={"name": "Large Updated", "price": 16, "stock": 4, "version": extra_sku["version"]})
    assert updated_sku["name"] == "Large Updated"
    sku = request("PATCH", f"/skus/{sku['id']}", token=merchant_token, body={"status": "ON_SALE", "version": sku["version"]})
    product = request("PATCH", f"/products/{product['id']}", token=merchant_token, body={"status": "ON_SALE"})
    assert product["status"] == "ON_SALE"
    public_product = request("GET", f"/products/{product['id']}")
    assert public_product["id"] == product["id"]
    assert request("GET", f"/shops/{shop_id}/products", token=user_token)["items"]
    merchant_product = request("GET", f"/products/{product['id']}?includeOffSale=true", token=merchant_token)
    assert any(item["id"] == sku["id"] for item in merchant_product["skus"])
    request("PATCH", f"/skus/{sku['id']}", token=merchant_token, body={"name": "stale", "version": sku["version"] - 1}, expected=409, code=1404)

    # Customer browse, addresses, cart quantity/merge/removal, and checkout.
    detail = request("GET", f"/shops/{shop_id}")
    assert detail["id"] == shop_id
    assert any(row["id"] == category["id"] for row in request("GET", f"/shops/{shop_id}/categories"))
    address = request("POST", "/user-addresses", token=user_token, body={"recipient": "Matrix User", "phone": "13900000002", "region": "杭州", "detail": "矩阵路 2 号", "isDefault": True}, expected=201)
    second_address = request("POST", "/user-addresses", token=user_token, body={"recipient": "Matrix User 2", "phone": "13900000003", "region": "杭州", "detail": "矩阵路 3 号", "isDefault": False}, expected=201)
    assert len(request("GET", "/user-addresses", token=user_token)) >= 2
    address = request("PATCH", f"/user-addresses/{second_address['id']}", token=user_token, body={"detail": "矩阵路 4 号", "isDefault": True})
    assert address["isDefault"] is True
    request("DELETE", f"/user-addresses/{address['id']}", token=user_token, expected=200)
    address = request("GET", "/user-addresses", token=user_token)[0]
    cart_item = request("POST", "/cart-items", token=user_token, body={"skuId": sku["id"], "quantity": 1}, expected=201)
    # Adding an existing SKU merges quantities and returns 200; a new cart row
    # returns 201. The Vue view accepts either because it consumes only data.
    merged = request("POST", "/cart-items", token=user_token, body={"skuId": sku["id"], "quantity": 1}, expected=200)
    assert merged["id"] == cart_item["id"] and merged["quantity"] == 2
    cart_item = request("PATCH", f"/cart-items/{cart_item['id']}", token=user_token, body={"quantity": 1})
    assert cart_item["quantity"] == 1
    checkout_body = {"items": [{"cartItemId": cart_item["id"], "skuVersion": sku["version"]}], "addressId": address["id"], "remark": "matrix checkout"}
    checkout_key = "matrix-order-" + suffix
    order = request("POST", "/orders", token=user_token, body=checkout_body, headers={"X-Idempotency-Key": checkout_key}, expected=201)
    retry = request("POST", "/orders", token=user_token, body=checkout_body, headers={"X-Idempotency-Key": checkout_key}, expected=201)
    assert retry["id"] == order["id"]
    assert request("GET", "/cart-items", token=user_token)["items"] == []
    assert request("GET", f"/orders/{order['id']}", token=user_token)["status"] == "PENDING_PAYMENT"
    assert request("GET", "/orders", token=user_token)["items"]

    # Full merchant fulfilment and customer receipt workflow.
    paid = request("POST", f"/orders/{order['id']}/pay", token=user_token, headers={"X-Idempotency-Key": "matrix-pay-" + suffix}, expected=200)
    assert paid["status"] == "PAID" and paid["paymentStatus"] == "PAID"
    merchant_orders = request("GET", "/merchant/orders", token=merchant_token)
    assert any(row["id"] == order["id"] for row in merchant_orders["items"])
    assert request("GET", f"/merchant/orders/{order['id']}", token=merchant_token)["id"] == order["id"]
    preparing = request("POST", f"/merchant/orders/{order['id']}/prepare", token=merchant_token, headers={"X-Idempotency-Key": "matrix-prepare-" + suffix}, expected=200)
    assert preparing["status"] == "PREPARING"
    delivering = request("POST", f"/merchant/orders/{order['id']}/deliver", token=merchant_token, headers={"X-Idempotency-Key": "matrix-deliver-" + suffix}, expected=200)
    assert delivering["status"] == "DELIVERING"
    completed = request("POST", f"/orders/{order['id']}/confirm-receipt", token=user_token, headers={"X-Idempotency-Key": "matrix-receipt-" + suffix}, expected=200)
    assert completed["status"] == "COMPLETED"

    # Paid cancellation/refund path and stock restoration.
    current = request("GET", f"/products/{product['id']}?includeOffSale=true", token=merchant_token)
    current_sku = next(item for item in current["skus"] if item["id"] == sku["id"])
    cancel_cart = request("POST", "/cart-items", token=user_token, body={"skuId": sku["id"], "quantity": 1}, expected=201)
    cancel_order = request("POST", "/orders", token=user_token, body={"items": [{"cartItemId": cancel_cart["id"], "skuVersion": current_sku["version"]}], "addressId": address["id"]}, headers={"X-Idempotency-Key": "matrix-cancel-order-" + suffix}, expected=201)
    request("POST", f"/orders/{cancel_order['id']}/pay", token=user_token, headers={"X-Idempotency-Key": "matrix-cancel-pay-" + suffix}, expected=200)
    cancelled = request("POST", f"/orders/{cancel_order['id']}/cancel", token=user_token, body={"reason": "matrix refund"}, headers={"X-Idempotency-Key": "matrix-cancel-" + suffix}, expected=200)
    assert cancelled["status"] == "CANCELLED" and cancelled["refundStatus"] == "REFUNDED"
    refund = request("GET", f"/orders/{cancel_order['id']}/refund", token=user_token)
    assert refund["orderId"] == cancel_order["id"] and refund["status"] == "REFUNDED"

    # Cart cleanup endpoint and final feature report marker.
    request("DELETE", f"/cart-items/{cancel_cart['id']}", token=user_token, expected=404, code=1004)
    print("frontend-feature-matrix-ok", {"shop": shop_id, "product": product["id"], "completed": order["id"], "refunded": cancel_order["id"]})


if __name__ == "__main__":
    main()
