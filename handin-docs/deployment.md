# Service deployment

The Vue frontend continues to call `/api/v1/**` through the gateway. Service DNS names and ports are internal implementation details, so scaling a service does not require frontend changes or service-specific URLs.

## Local containers

`docker-compose.yml` starts one instance of each service, which keeps the default development stack inexpensive. Compose can exercise horizontal routing without changing configuration:

```bash
docker compose up --build --scale catalog-service=2 --scale cart-service=2 --scale order-service=2
```

Only the gateway, frontend, MySQL, and Kafka publish host ports. The service containers remain reachable through Docker DNS, allowing multiple instances to share the same service name.

## Kubernetes cloud deployment

Kubernetes is reserved for cloud or staging deployment. The base manifests include service health probes, graceful termination, and Kafka topic provisioning. The cloud overlay sets two replicas for every stateless service and the gateway, removes the local Kafka broker, and exposes managed MySQL/Kafka endpoints through `deploy/k8s/overlays/cloud/external-config.yaml`.

Before applying the cloud profile, provide the referenced `delivery-secrets` Secret and edit the external connection endpoints for the target managed services:

```bash
kubectl apply -k deploy/k8s/overlays/cloud
```

Increase the replica patch values or add autoscaling according to cluster capacity. The shared HS256 and internal service token remain the current non-production compatibility configuration.

Outbox publishers use a 60 second database lease keyed by a generated worker id. A row is atomically claimed before processing and released on success or retry; an expired lease can be taken by another instance after a crash. This prevents normal duplicate processing when order-service replicas run their scheduled publisher concurrently.

The cart checkout reaper and catalog reservation reaper also claim stale rows with 60 second database leases before calling another service. This keeps scheduled reconciliation safe when those services are scaled horizontally; abandoned leases are reclaimed after an instance failure.

The cloud profile exposes connection endpoints through `deploy/k8s/overlays/cloud/external-config.yaml`. Change the MySQL and Kafka values to the managed-service DNS names before applying. Credentials remain in the externally supplied `delivery-secrets` Secret.
