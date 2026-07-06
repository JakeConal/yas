#!/bin/bash
# Recover the observability + zookeeper pieces that setup-cluster.sh skipped
# after opentelemetry-collector failed. Run from any directory; the script
# cd's into the deploy dir itself for the relative chart paths.
#
# Usage:  bash recover-missing-observability.sh

set -ex

REPO_DIR="/mnt/d/StudentLife/2_Workspace/8-misc/yas"
DEPLOY_DIR="$REPO_DIR/k8s/deploy"

echo "============================================================"
echo "[recover-missing-observability.sh] starting"
echo "============================================================"

# Promtail needs this — Minikube default 128 inotify instances is too low
minikube ssh -- 'sudo sysctl -w fs.inotify.max_user_instances=1024'

# 1. Promtail (logs → Loki directly, otel collector no longer handles logs)
helm upgrade --install promtail grafana/promtail \
  --create-namespace --namespace observability \
  --values "$DEPLOY_DIR/observability/promtail.values.yaml"

# 2. Prometheus + bundled Grafana (kube-prometheus-stack)
helm upgrade --install prometheus prometheus-community/kube-prometheus-stack \
  --create-namespace --namespace observability \
  -f "$DEPLOY_DIR/observability/prometheus.values.yaml" \
  --set hostname="grafana.yas.local.com" \
  --set 'grafana.grafana\.ini.database.user'="yasadminuser" \
  --set 'grafana.grafana\.ini.database.password'="admin" \
  --set grafana.assertNoLeakedSecrets=false

# 3. Grafana operator (manages custom Grafana CRDs from step 4)
helm upgrade --install grafana-operator oci://ghcr.io/grafana-operator/helm-charts/grafana-operator \
  --version v5.0.2 \
  --create-namespace --namespace observability

# 4. Custom Grafana chart (datasources + dashboards). Uses relative path,
#    so we MUST cd into deploy dir first.
cd "$DEPLOY_DIR"
GRAFANA_USER=$(yq -r '.grafana.username' cluster-config.yaml)
GRAFANA_PASS=$(yq -r '.grafana.password' cluster-config.yaml)
DOMAIN=$(yq -r '.domain' cluster-config.yaml)
helm upgrade --install grafana ./observability/grafana \
  --create-namespace --namespace observability \
  --set hostname="grafana.$DOMAIN" \
  --set grafana.username="$GRAFANA_USER" \
  --set grafana.password="$GRAFANA_PASS" \
  --set postgresql.username="yasadminuser" \
  --set postgresql.password="admin"

# 5. Zookeeper (used by Kafka)
helm upgrade --install zookeeper ./zookeeper \
  --namespace zookeeper --create-namespace

echo ""
echo "============================================================"
echo "[recover-missing-observability.sh] done"
echo "============================================================"
echo "Verify with:"
echo "  kubectl get pods -n observability"
echo "  kubectl get pods -n zookeeper"