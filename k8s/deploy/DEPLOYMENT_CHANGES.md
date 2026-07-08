# Minikube Deployment Change Review

Date: 2026-06-13

## Scope

These changes were made while deploying YAS to WSL Minikube from `k8s/deploy`.
The goal was to keep the default setup and change only files that blocked Helm
rendering, installation, pod startup, or local Minikube compatibility.

## Deployment Result

- Minikube was started with the documented local sizing: `16g` memory and `40000mb` disk.
- All YAS application pods in the `yas` namespace reached `1/1 Running`.
- Helm reported no failed releases.
- `api.yas.local.com` through the Minikube ingress NodePort returned `200` for `/swagger-ui/index.html`.
- Minikube IP during deployment was `192.168.49.2`.

## Changed Files

| File | Change | Why |
| --- | --- | --- |
| `.gitattributes` | Added `*.sh text eol=lf`. | Windows checkout settings previously produced CRLF or mixed line endings in deployment scripts. Bash stopped at the service loop with `do\r`, so only the charts before that loop were installed. The rule keeps shell scripts executable in WSL. |
| `k8s/deploy/postgres/postgresql/templates/postgresql.yaml` | Fixed malformed Helm expressions for `recommendation` and `webhook` database owners. | Helm lint failed because `{ { .Values.username } }` is invalid template/YAML syntax. The fix uses the same `{{ .Values.username }}` pattern as the other databases. |
| `k8s/deploy/setup-cluster.sh` | Enabled fail-fast behavior with `set -ex` and allowed the existing `read -d ''` command to complete at normal EOF. | The script previously returned success even when multiple Helm commands failed, which hid partial deployments. With `set -e`, the `read -d ''` command's normal EOF return had to be explicitly accepted so setup can continue. |
| `k8s/deploy/setup-cluster.sh` | Pinned Strimzi to `0.45.2`. | Current Strimzi `1.0.0` only serves `kafka.strimzi.io/v1` and no longer supports the repo's ZooKeeper-based `spec.zookeeper` Kafka manifest. `0.45.2` still supports the repo's `v1beta2` resources. |
| `k8s/deploy/setup-cluster.sh` | Disabled Strimzi leader election for the single-replica Minikube operator. | On this single-node Minikube cluster, the pinned operator reconciled Kafka successfully but then exited after leader-election churn. Disabling leader election keeps the single operator stable. |
| `k8s/deploy/setup-cluster.sh` | Added waits for Strimzi CRDs and the Strimzi operator rollout, plus a bounded Kafka chart retry. | Kafka custom resources were applied before the CRDs/operator were ready, causing `no matches for kind "Kafka"` errors. Kubernetes API discovery can still briefly lag CRD registration, so the chart install retries up to three times before failing. |
| `k8s/deploy/setup-cluster.sh` | Raises `fs.inotify.max_user_instances` to `1024` on the Minikube node before installing Promtail. | Promtail uses filesystem watchers for pod log directories. The Minikube default of `128` inotify instances was exhausted by the full stack and made Promtail fail with `failed to make file target manager: too many open files`. |
| `k8s/deploy/setup-cluster.sh` | Added `--set loki.useTestSchema=true` to the Loki install. | The current Loki chart requires an explicit schema config. For local non-production Minikube, the chart's test schema is the smallest compatible setting. |
| `k8s/deploy/setup-cluster.sh` | Added a rollout wait for the OpenTelemetry operator. | The OpenTelemetry collector install initially failed because the operator webhook was not ready. |
| `k8s/deploy/setup-cluster.sh` | Added `--set grafana.assertNoLeakedSecrets=false` to the Prometheus install. | The current kube-prometheus-stack chart blocks explicit `grafana.ini.database.password` values by default. This repo already uses local dev credentials in values, so the local deployment needs the assertion disabled. |
| `k8s/deploy/setup-cluster.sh` | Fixed `hotname` to `hostname` for the Grafana chart. | The previous `--set hotname=...` value was a typo and did not set the chart's actual hostname value. |
| `k8s/charts/backend/values.yaml` | Added optional `hostAliases: []`. | This keeps the backend chart default unchanged while allowing selected services to add pod host aliases. |
| `k8s/charts/backend/templates/deployment.yaml` | Rendered `.Values.hostAliases` into pod specs when set. | The BFF services need to resolve `identity.yas.local.com` to the Minikube ingress IP from inside pods. |
| `k8s/deploy/deploy-yas-applications.sh` | Captured `MINIKUBE_IP="$(minikube ip)"` and passed a host alias for `identity.$DOMAIN` to `backoffice-bff` and `storefront-bff`. | Spring OAuth client startup resolves the configured issuer URL. Keycloak advertises the external issuer host, so the BFF pods must reach `identity.yas.local.com` through Minikube ingress without changing the issuer URL. |
| `k8s/deploy/elasticsearch/elasticsearch-cluster/values.yaml` | Added `elasticsearch.version: 9.2.3`. | The search service is built against Elasticsearch 9.2.3 client dependencies. |
| `k8s/deploy/elasticsearch/elasticsearch-cluster/templates/elasticsearch.yaml` | Changed the Elasticsearch version from hardcoded `8.8.1` to `.Values.elasticsearch.version`. | Search crashed against Elasticsearch 8.8.1 with client/server protocol mismatch errors. Making it a value keeps the chart configurable. |
| `k8s/deploy/observability/opentelemetry/values.yaml` | Removed the Loki receiver/exporter and log pipeline; kept OTLP traces to Tempo. | The current collector image no longer includes `loki` receiver/exporter components, so the collector crashed at startup. |
| `k8s/deploy/observability/promtail.values.yaml` | Sent Promtail directly to `http://loki-gateway/loki/api/v1/push`. | Since logs no longer go through the OpenTelemetry collector, Promtail should push directly to Loki. |
| `payment/src/main/resources/db/changelog/data/changelog-0001-provider.sql` | Changed insert column from `is_enabled` to `enabled`. | Payment DDL renames `is_enabled` to `enabled` before data changelogs run, so the original seed insert failed. |
| `payment/src/main/resources/db/changelog/data/changelog-0002-provider.sql` | Changed insert column from `is_enabled` to `enabled`. | Same Liquibase ordering issue as the first payment provider seed file. |
| `payment-paypal/Dockerfile` | Changed the copied jar from `target/payment-paypal*.jar` to `target/payment-paypal*-exec.jar`. | The plain `payment-paypal-1.0-SNAPSHOT.jar` has no main manifest. The module's executable artifact is `payment-paypal-1.0-SNAPSHOT-exec.jar`. |

## Operational Actions Performed

Some deployment recovery actions changed only the live Minikube cluster, not the
repository:

- Replaced the initially installed Strimzi `1.0.0` CRDs/operator with Strimzi `0.45.2`.
- Recreated the fresh local Elasticsearch cluster/PVC so it could start at `9.2.3`; ECK blocked direct `8.8.1` to `9.2.3` upgrade.
- Deleted the old Kibana pod after the Elasticsearch recreation so the new Kibana pod could schedule on the single-node cluster.
- Built local Minikube images for:
  - `ghcr.io/nashtech-garage/yas-payment:latest`
  - `ghcr.io/nashtech-garage/yas-payment-paypal:latest`

The local image rebuild was necessary because the running chart tags still point
to `latest`, and the remote `payment-paypal:latest` image contained a non-executable
jar.

## Observability Change Details

The observability changes were required because the current upstream
OpenTelemetry Operator/collector image no longer supports the Loki receiver and
Loki exporter names used by this repo's previous collector config.

Before the change, the intended log and trace flow was:

```text
Promtail -> OpenTelemetry Collector Loki receiver -> OpenTelemetry Collector Loki exporter -> Loki
Apps -> OpenTelemetry Collector OTLP receiver -> Tempo
```

After the change, the deployed flow is:

```text
Promtail -> Loki
Apps -> OpenTelemetry Collector OTLP receiver -> Tempo
```

### Failure Observed

The OpenTelemetry collector pod entered `CrashLoopBackOff` and failed during
configuration parsing with errors equivalent to:

```text
'receivers' unknown type: "loki"
'exporters' unknown type: "loki"
```

This means the collector binary installed by the current chart did not include
the Loki receiver/exporter components expected by
`k8s/deploy/observability/opentelemetry/values.yaml`.

### Collector Config Change

In `k8s/deploy/observability/opentelemetry/values.yaml`, the unsupported log
pipeline was removed:

- Removed the `loki` receiver.
- Removed the Loki-specific `attributes` processor.
- Removed the `loki` exporter.
- Removed the `logs` pipeline.
- Kept the OTLP receiver on ports `4317` and `4318`.
- Kept trace export to Tempo through `otlphttp`.
- Changed the old `logging` exporter name to `debug`, which is the supported
  exporter name in newer collector versions.

The collector now handles traces only:

```yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318

processors:
  batch: {}

exporters:
  debug:
    verbosity: detailed
  otlphttp:
    endpoint: http://tempo:4318

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [otlphttp]
```

### Promtail Config Change

Because logs no longer pass through the OpenTelemetry collector,
`k8s/deploy/observability/promtail.values.yaml` was changed from:

```yaml
url: http://opentelemetry-collector:3500/loki/api/v1/push
```

to:

```yaml
url: http://loki-gateway/loki/api/v1/push
```

This sends Kubernetes container logs directly from Promtail to Loki.

### Impact

- Logs still go to Loki.
- Traces still go to Tempo through the OpenTelemetry collector.
- The collector pod starts successfully because it no longer references
  unsupported Loki components.
- The previous collector-side log enrichment labels were removed. If that log
  enrichment is required later, use a collector distribution that includes Loki
  receiver/exporter support or redesign the log path around currently supported
  OpenTelemetry log exporters.
- Promtail continues to collect logs from all namespaces. The Minikube setup
  script raises the node inotify-instance limit before its install so Promtail
  can create the required directory watchers.

## Verification

Commands run after the fixes:

```bash
helm lint k8s/charts/backend
helm lint k8s/charts/backoffice-bff
helm lint k8s/charts/storefront-bff
helm lint k8s/deploy/elasticsearch/elasticsearch-cluster
helm lint k8s/deploy/observability/opentelemetry
helm lint k8s/deploy/postgres/postgresql
kubectl get pods -n yas
helm list -A --failed
curl -H 'Host: api.yas.local.com' http://192.168.49.2:30860/swagger-ui/index.html
```

Results:

- Helm lint passed for touched charts.
- `kubectl get pods -n yas` showed every YAS application pod as `1/1 Running`.
- `helm list -A --failed` returned no failed releases.
- Swagger UI returned HTTP `200` through the Minikube ingress NodePort.

## Notes And Follow-Up

- In this WSL Docker-driver Minikube setup, ingress is exposed through NodePort
  `30860`, not directly on port `80` from WSL. The README host-file examples may
  still require either a hosts entry plus the NodePort, or a different Minikube
  ingress/tunnel setup.
- The payment and payment-paypal fixes are source/Dockerfile fixes. For a fresh
  cluster to avoid pulling the old remote `latest` images, rebuild these images
  into Minikube again or publish fixed images to the registry.
- The Strimzi pin is a compatibility choice for the current ZooKeeper-based Kafka
  manifest. A longer-term upgrade would migrate the Kafka chart to Strimzi `v1`
  and KRaft.
- The Loki test schema is appropriate for local Minikube only. Production should
  use an explicit durable Loki schema/storage configuration.
