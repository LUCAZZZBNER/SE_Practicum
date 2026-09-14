"""Black-box broker-outage acceptance on an explicitly named disposable stack."""
import os
import subprocess
import time
from test_frontend_gateway_workflow import main as setup

def main():
    project = os.environ["DELIVERY_TEST_PROJECT"]
    password = os.environ["MYSQL_ROOT_PASSWORD"]
    assert project.startswith("ms-"), "Use a disposable ms-* Compose project"
    kafka = project + "-kafka-1"
    db = project + "-db-1"
    def sql(query):
        result = subprocess.run(["docker", "exec", "-e", "MYSQL_PWD=" + password, db,
                                 "mysql", "-uroot", "-N", "-B", "-e", query],
                                check=True, capture_output=True, text=True)
        return result.stdout.strip()
    subprocess.run(["docker", "stop", kafka], check=True, stdout=subprocess.DEVNULL)
    try:
        order = setup()["order"]
        query = f"SELECT published_at IS NULL FROM delivery_order.order_event_outbox WHERE order_id={order['id']}"
        assert sql(query) == "1", "The committed order must remain in the outbox while Kafka is down"
    finally:
        subprocess.run(["docker", "start", kafka], check=True, stdout=subprocess.DEVNULL)
    for _ in range(60):
        if sql(query) == "0":
            print("kafka-outage-recovery-ok", order["id"])
            return
        time.sleep(1)
    raise AssertionError("Kafka recovery did not drain the outbox")

if __name__ == "__main__":
    main()
