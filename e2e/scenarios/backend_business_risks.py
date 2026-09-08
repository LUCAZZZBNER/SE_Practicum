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

    product = call(
        "POST",
        "/products",
        token=merchant_token,
        body={"shopId": shop_id, "categoryId": category_id, "name": "Rice", "price": 12.50, "stock": 5},
        expected_status=201,
    )
    product_id = product["id"]
    version = product["version"]
    product = call(
        "PATCH",
        f"/products/{product_id}",
        token=merchant_token,
        body={"status": "ON_SALE", "version": version},
    )
    version = product["version"]
    call(
        "PATCH",
        f"/products/{product_id}",
        token=merchant_token,
        body={"name": "stale update", "version": version - 1},
        expected_status=409,
        expected_code=1005,
    )

    cart_item = call("POST", "/cart-items", token=user_token, body={"productId": product_id, "quantity": 2}, expected_status=201)
    cart_item_id = cart_item["id"]
    idempotency_key = f"risk-{suffix}"
    order_body = {"items": [{"cartItemId": cart_item_id, "productVersion": version}]}
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
        body={"items": [{"cartItemId": cart_item_id, "productVersion": version + 1}]},
        headers={"X-Idempotency-Key": idempotency_key},
        expected_status=409,
        expected_code=1603,
    )
    call("POST", f"/orders/{order['id']}/cancel", token=user_token)
    call(
        "POST",
        f"/orders/{order['id']}/cancel",
        token=user_token,
        expected_status=409,
        expected_code=1602,
    )

    # A second product verifies cart quantity and stock limits at the API boundary.
    limited = call(
        "POST",
        "/products",
        token=merchant_token,
        body={"shopId": shop_id, "categoryId": category_id, "name": "Limited", "price": 1.00, "stock": 1},
        expected_status=201,
    )
    limited = call(
        "PATCH",
        f"/products/{limited['id']}",
        token=merchant_token,
        body={"status": "ON_SALE", "version": limited["version"]},
    )
    call("POST", "/cart-items", token=user_token, body={"productId": limited["id"], "quantity": 1}, expected_status=201)
    call(
        "POST",
        "/cart-items",
        token=user_token,
        body={"productId": limited["id"], "quantity": 1},
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
