#!/usr/bin/env python3
"""Black-box regression checks for the backend business-risk scenarios.

Run this script against a running Spring Boot instance.  DELIVERY_BASE_URL may
point at the Vite proxy (for example, http://localhost:5173/api/v1) or directly
at the backend (http://localhost:8080/api/v1).
"""

import json
import os
import sys
import urllib.error
import urllib.request
import uuid


BASE_URL = os.environ.get("DELIVERY_BASE_URL", "http://localhost:8080/api/v1").rstrip("/")
PASSWORD = "Passw0rd!"


def call(method, path, *, token=None, body=None, expected_status=200, expected_code=0, headers=None):
    request_headers = {"Content-Type": "application/json"}
    if token:
        request_headers["Authorization"] = f"Bearer {token}"
    if headers:
        request_headers.update(headers)
    payload = None if body is None else json.dumps(body).encode("utf-8")
    request = urllib.request.Request(BASE_URL + path, data=payload, headers=request_headers, method=method)
    try:
        response = urllib.request.urlopen(request, timeout=15)
        status = response.status
        raw = response.read().decode("utf-8")
    except urllib.error.HTTPError as error:
        status = error.code
        raw = error.read().decode("utf-8")
    try:
        result = json.loads(raw)
    except json.JSONDecodeError as error:
        raise AssertionError(f"{method} {path} returned non-JSON ({status}): {raw}") from error
    if status != expected_status or result.get("code") != expected_code:
        raise AssertionError(
            f"{method} {path}: expected HTTP/code {expected_status}/{expected_code}, "
            f"got {status}/{result.get('code')}: {result}"
        )
    return result.get("data")


def upload_image(token):
    boundary = f"----delivery-{uuid.uuid4().hex}"
    image = bytes([82, 73, 70, 70, 0, 0, 0, 0, 87, 69, 66, 80])
    payload = (
        f"--{boundary}\r\n"
        'Content-Disposition: form-data; name="file"; filename="risk.webp"\r\n'
        "Content-Type: image/webp\r\n\r\n"
    ).encode("ascii") + image + f"\r\n--{boundary}--\r\n".encode("ascii")
    request = urllib.request.Request(
        BASE_URL + "/files/images",
        data=payload,
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": f"multipart/form-data; boundary={boundary}",
        },
        method="POST",
    )
    try:
        response = urllib.request.urlopen(request, timeout=15)
        status = response.status
        raw = response.read().decode("utf-8")
    except urllib.error.HTTPError as error:
        status = error.code
        raw = error.read().decode("utf-8")
    result = json.loads(raw)
    if status != 201 or result.get("code") != 0:
        raise AssertionError(f"POST /files/images: expected HTTP/code 201/0, got {status}/{result.get('code')}: {result}")
    return result["data"]


def main():
    suffix = uuid.uuid4().hex[:12]
    merchant_account = f"merchant-{suffix}"
    user_account = f"user-{suffix}"

    merchant = {
        "account": merchant_account,
        "password": PASSWORD,
        "passwordConfirm": PASSWORD,
        "name": "Risk Test Merchant",
        "phone": "13800000001",
    }
    user = {
        "account": user_account,
        "password": PASSWORD,
        "passwordConfirm": PASSWORD,
        "nickname": "Risk Test User",
        "phone": "13900000001",
    }

    merchant_id = call("POST", "/merchants", body=merchant, expected_status=201)["id"]
    merchant_token = call("POST", "/merchants/login", body={"account": merchant_account, "password": PASSWORD})[
        "accessToken"
    ]
    user_id = call("POST", "/users", body=user, expected_status=201)["id"]
    user_token = call("POST", "/users/login", body={"account": user_account, "password": PASSWORD})["accessToken"]

    # Duplicate-account protection and authentication/role boundaries.
    call("POST", "/merchants", body=merchant, expected_status=409, expected_code=1201)
    call("POST", "/users", body=user, expected_status=409, expected_code=1101)
    call("GET", "/cart-items", expected_status=401, expected_code=1002)
    call("GET", "/cart-items", token=merchant_token, expected_status=403, expected_code=1003)
    call("POST", "/shops", token=user_token, body={"name": "forbidden"}, expected_status=403, expected_code=1003)

    shop = call("POST", "/shops", token=merchant_token, body={"name": f"Risk Shop {suffix}"}, expected_status=201)
    shop_id = shop["id"]
    call(
        "PATCH",
        f"/shops/{shop_id}/address",
        token=merchant_token,
        body={"region": "杭州", "detail": "学院路 1 号", "phone": "05711234567"},
    )
    call("PATCH", f"/shops/{shop_id}", token=merchant_token, body={"status": "OPEN"})
    category = call(
        "POST",
        f"/shops/{shop_id}/categories",
        token=merchant_token,
        body={"name": "Meals", "sortOrder": 1},
        expected_status=201,
    )
    category_id = category["id"]
    call(
        "POST",
        f"/shops/{shop_id}/categories",
        token=merchant_token,
        body={"name": "Meals"},
        expected_status=409,
        expected_code=1005,
    )

    image = upload_image(merchant_token)
    product = call(
        "POST",
        "/products",
        token=merchant_token,
        body={
            "shopId": shop_id,
            "categoryId": category_id,
            "name": "Rice",
            "imageId": image["id"],
            "skus": [{"name": "Default", "price": 12.50, "stock": 5}],
        },
        expected_status=201,
    )
    product_id = product["id"]
    product = call(
        "PATCH",
        f"/products/{product_id}",
        token=merchant_token,
        body={"status": "ON_SALE"},
    )
    sku = product["skus"][0]
    sku = call(
        "PATCH",
        f"/skus/{sku['id']}",
        token=merchant_token,
        body={"status": "ON_SALE", "version": sku["version"]},
    )
    call(
        "PATCH",
        f"/skus/{sku['id']}",
        token=merchant_token,
        body={"name": "stale update", "version": sku["version"] - 1},
        expected_status=409,
        expected_code=1404,
    )

    address = call(
        "POST",
        "/user-addresses",
        token=user_token,
        body={
            "recipient": "Risk Test User",
            "phone": "13900000001",
            "region": "杭州",
            "detail": "学院路 2 号",
            "isDefault": True,
        },
        expected_status=201,
    )

    changed = call(
        "POST",
        "/products",
        token=merchant_token,
        body={
            "shopId": shop_id,
            "categoryId": category_id,
            "name": "Price Change",
            "imageId": image["id"],
            "skus": [{"name": "Default", "price": 8.00, "stock": 2}],
        },
        expected_status=201,
    )
    changed = call(
        "PATCH",
        f"/products/{changed['id']}",
        token=merchant_token,
        body={"status": "ON_SALE"},
    )
    changed_sku = changed["skus"][0]
    changed_sku = call(
        "PATCH",
        f"/skus/{changed_sku['id']}",
        token=merchant_token,
        body={"status": "ON_SALE", "version": changed_sku["version"]},
    )
    changed_cart = call(
        "POST", "/cart-items", token=user_token, body={"skuId": changed_sku["id"], "quantity": 1}, expected_status=201
    )
    changed_version = changed_sku["version"]
    call(
        "PATCH",
        f"/skus/{changed_sku['id']}",
        token=merchant_token,
        body={"price": 9.00, "version": changed_version},
    )
    call(
        "POST",
        "/orders",
        token=user_token,
        body={
            "items": [{"cartItemId": changed_cart["id"], "skuVersion": changed_version}],
            "addressId": address["id"],
        },
        headers={"X-Idempotency-Key": f"price-change-{suffix}"},
        expected_status=409,
        expected_code=1603,
    )
    call("DELETE", f"/cart-items/{changed_cart['id']}", token=user_token)

    cart_item = call("POST", "/cart-items", token=user_token, body={"skuId": sku["id"], "quantity": 2}, expected_status=201)
    cart_item_id = cart_item["id"]
    idempotency_key = f"risk-{suffix}"
    order_body = {
        "items": [{"cartItemId": cart_item_id, "skuVersion": sku["version"]}],
        "addressId": address["id"],
    }
    order = call(
        "POST",
        "/orders",
        token=user_token,
        body=order_body,
        headers={"X-Idempotency-Key": idempotency_key},
        expected_status=201,
    )
    retry = call(
        "POST",
        "/orders",
        token=user_token,
        body=order_body,
        headers={"X-Idempotency-Key": idempotency_key},
        expected_status=201,
    )
    assert retry["id"] == order["id"], "idempotent retry created a second order"
    call(
        "POST",
        "/orders",
        token=user_token,
        body={
            "items": [{"cartItemId": cart_item_id, "skuVersion": sku["version"] + 1}],
            "addressId": address["id"],
        },
        headers={"X-Idempotency-Key": idempotency_key},
        expected_status=409,
        expected_code=1602,
    )
    cancel_headers = {"X-Idempotency-Key": f"cancel-{suffix}"}
    cancelled = call("POST", f"/orders/{order['id']}/cancel", token=user_token, body={}, headers=cancel_headers)
    cancel_retry = call("POST", f"/orders/{order['id']}/cancel", token=user_token, body={}, headers=cancel_headers)
    assert cancel_retry["id"] == cancelled["id"], "idempotent cancellation did not return the original order"
    call(
        "POST",
        f"/orders/{order['id']}/cancel",
        token=user_token,
        body={},
        headers={"X-Idempotency-Key": f"cancel-again-{suffix}"},
        expected_status=409,
        expected_code=1601,
    )

    # A second product verifies cart quantity and stock limits at the API boundary.
    limited = call(
        "POST",
        "/products",
        token=merchant_token,
        body={
            "shopId": shop_id,
            "categoryId": category_id,
            "name": "Limited",
            "imageId": image["id"],
            "skus": [{"name": "Default", "price": 1.00, "stock": 1}],
        },
        expected_status=201,
    )
    limited = call(
        "PATCH",
        f"/products/{limited['id']}",
        token=merchant_token,
        body={"status": "ON_SALE"},
    )
    limited_sku = limited["skus"][0]
    limited_sku = call(
        "PATCH",
        f"/skus/{limited_sku['id']}",
        token=merchant_token,
        body={"status": "ON_SALE", "version": limited_sku["version"]},
    )
    call("POST", "/cart-items", token=user_token, body={"skuId": limited_sku["id"], "quantity": 1}, expected_status=201)
    call(
        "POST",
        "/cart-items",
        token=user_token,
        body={"skuId": limited_sku["id"], "quantity": 1},
        expected_status=409,
        expected_code=1402,
    )

    print(f"backend business-risk checks passed against {BASE_URL} (merchant={merchant_id}, user={user_id})")


if __name__ == "__main__":
    try:
        main()
    except (AssertionError, urllib.error.URLError) as error:
        print(f"backend business-risk checks failed: {error}", file=sys.stderr)
        raise SystemExit(1)
