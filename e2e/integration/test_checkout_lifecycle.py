"""Independent order lifecycle and stock-compensation acceptance check."""
import uuid
import time
from test_frontend_gateway_workflow import call, main as setup

def main():
    fixture = setup()
    user = fixture["user_token"]
    merchant = fixture["merchant_token"]
    order = fixture["order"]
    call("POST", f"/orders/{order['id']}/pay", {}, user, {"X-Idempotency-Key": "pay-" + uuid.uuid4().hex}, 200)
    call("POST", f"/merchant/orders/{order['id']}/prepare", {}, merchant, {"X-Idempotency-Key": "prepare-" + uuid.uuid4().hex}, 200)
    call("POST", f"/merchant/orders/{order['id']}/deliver", {}, merchant, {"X-Idempotency-Key": "deliver-" + uuid.uuid4().hex}, 200)
    call("POST", f"/orders/{order['id']}/confirm-receipt", {}, user, {"X-Idempotency-Key": "receipt-" + uuid.uuid4().hex}, 200)

    current = call("GET", f"/products/{fixture['product']['id']}?includeOffSale=true", token=merchant)
    current_sku = next(item for item in current["skus"] if item["id"] == fixture["sku"]["id"])
    cart = call("POST", "/cart-items", {"skuId": fixture["sku"]["id"], "quantity": 1}, user, expected=201)
    body = {"addressId": fixture["address"]["id"], "items": [{"cartItemId": cart["id"], "skuVersion": current_sku["version"]}]}
    key = "cancel-" + uuid.uuid4().hex
    cancelled = call("POST", "/orders", body, user, {"X-Idempotency-Key": key}, 201)
    call("POST", f"/orders/{cancelled['id']}/cancel", {"reason": "independent compensation"}, user,
         {"X-Idempotency-Key": "refund-" + uuid.uuid4().hex}, 200)
    for _ in range(20):
        product = call("GET", f"/products/{fixture['product']['id']}?includeOffSale=true", token=merchant)
        sku = next(item for item in product["skus"] if item["id"] == fixture["sku"]["id"])
        if sku["stock"] == 1:
            break
        time.sleep(.5)
    assert sku["stock"] == 1, sku
    print("checkout-lifecycle-compensation-ok", cancelled["id"], sku["stock"])

if __name__ == "__main__":
    main()
