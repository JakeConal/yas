#!/bin/bash
set -ex

# Always run from this script's own directory so relative paths
# (./cluster-config.yaml, ./postgres/..., ./observability/...) resolve
# regardless of where the script was invoked from. The nested cd|pwd
# resolves the absolute path even when $0 is relative (./setup-cluster.sh).
cd "$(cd "$(dirname "$0")" && pwd)"

# Helm returns exit 0 even when the release ends up in FAILED state.
# Without this check, set -e doesn't catch the failure and the script
# silently continues to subsequent installs — that's the bug that hid
# the opentelemetry-collector crash and skipped prometheus/grafana/etc.
# We rely on `helm status` returning exit code 1 for FAILED releases
# (and missing ones), which works without jq/yq/awk dependencies.
check_helm_release() {
  local release="$1"
  local ns="$2"
  if ! helm status "$release" -n "$ns" > /dev/null 2>&1; then
    echo "" >&2
    echo "FATAL: helm release '$release' in namespace '$ns' is missing or FAILED." >&2
    echo "Diagnose with: helm status $release -n $ns" >&2
    echo "" >&2
    helm status "$release" -n "$ns" 2>&1 | tail -30 >&2 || true
    exit 1
  fi
}

STRIMZI_CHART_VERSION="0.45.2"

# Add chart repos and update
helm repo add postgres-operator-charts https://opensource.zalando.com/postgres-operator/charts/postgres-operator
helm repo add strimzi https://strimzi.io/charts/
helm repo add akhq https://akhq.io/
helm repo add elastic https://helm.elastic.co
helm repo add grafana https://grafana.github.io/helm-charts
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add open-telemetry https://open-telemetry.github.io/opentelemetry-helm-charts
helm repo add jetstack https://charts.jetstack.io
helm repo update

#Read configuration value from cluster-config.yaml file
read -rd '' DOMAIN POSTGRESQL_REPLICAS POSTGRESQL_USERNAME POSTGRESQL_PASSWORD \
KAFKA_REPLICAS ZOOKEEPER_REPLICAS ELASTICSEARCH_REPLICAES \
GRAFANA_USERNAME GRAFANA_PASSWORD \
< <(yq -r '.domain, .postgresql.replicas, .postgresql.username,
 .postgresql.password, .kafka.replicas, .zookeeper.replicas,
 .elasticsearch.replicas, .grafana.username, .grafana.password' ./cluster-config.yaml) || true

# FATAL guard: bail loudly if any required config value came back empty/null.
# An empty DOMAIN silently propagates into every --set flag and helm installs
# fail with confusing "api." hostnames — that's the bug that bit
# deploy-yas-applications.sh earlier.
for var in DOMAIN POSTGRESQL_REPLICAS POSTGRESQL_USERNAME POSTGRESQL_PASSWORD \
           KAFKA_REPLICAS ZOOKEEPER_REPLICAS ELASTICSEARCH_REPLICAES \
           GRAFANA_USERNAME GRAFANA_PASSWORD ; do
  if [ -z "${!var}" ] || [ "${!var}" = "null" ]; then
    echo "FATAL: $var is empty or null — check ./cluster-config.yaml (cwd=$(pwd))" >&2
    exit 1
  fi
done

# Install the postgres-operator
helm upgrade --install postgres-operator postgres-operator-charts/postgres-operator \
 --create-namespace --namespace postgres
check_helm_release postgres-operator postgres

#Install postgresql
helm upgrade --install postgres ./postgres/postgresql \
--create-namespace --namespace postgres \
--set replicas="$POSTGRESQL_REPLICAS" \
--set username="$POSTGRESQL_USERNAME" \
--set password="$POSTGRESQL_PASSWORD"
check_helm_release postgres postgres

#Install pgadmin
helm upgrade --install pgadmin ./postgres/pgadmin \
--create-namespace --namespace postgres \
--set hostname="pgadmin.$DOMAIN"
check_helm_release pgadmin postgres

#Install strimzi-kafka-operator
helm upgrade --install kafka-operator strimzi/strimzi-kafka-operator \
--create-namespace --namespace kafka \
--version "$STRIMZI_CHART_VERSION" \
--set leaderElection.enable=false

kubectl wait --for=condition=Established crd/kafkas.kafka.strimzi.io --timeout=120s
kubectl wait --for=condition=Established crd/kafkaconnects.kafka.strimzi.io --timeout=120s
kubectl wait --for=condition=Established crd/kafkaconnectors.kafka.strimzi.io --timeout=120s
kubectl rollout status deployment/strimzi-cluster-operator --namespace kafka --timeout=180s

check_helm_release kafka-operator kafka

#Install kafka and postgresql connector
for attempt in 1 2 3; do
  if helm upgrade --install kafka-cluster ./kafka/kafka-cluster \
  --create-namespace --namespace kafka \
  --set kafka.replicas="$KAFKA_REPLICAS" \
  --set zookeeper.replicas="$ZOOKEEPER_REPLICAS" \
  --set postgresql.username="$POSTGRESQL_USERNAME" \
  --set postgresql.password="$POSTGRESQL_PASSWORD"; then
    check_helm_release kafka-cluster kafka
    break
  fi

  if [ "$attempt" -eq 3 ]; then
    exit 1
  fi

  sleep 10
done

#Install akhq
helm upgrade --install akhq akhq/akhq \
--create-namespace --namespace kafka \
 --values ./kafka/akhq.values.yaml \
 --set hostname="akhq.$DOMAIN"
check_helm_release akhq kafka

#Install elastic-operator
helm upgrade --install elastic-operator elastic/eck-operator \
 --create-namespace --namespace elasticsearch
check_helm_release elastic-operator elasticsearch

# Install elasticsearch-cluster
helm upgrade --install elasticsearch-cluster ./elasticsearch/elasticsearch-cluster \
--create-namespace --namespace elasticsearch \
--set elasticsearch.replicas="$ELASTICSEARCH_REPLICAES" \
--set kibana.ingress.hostname="kibana.$DOMAIN"
check_helm_release elasticsearch-cluster elasticsearch

#Install loki
helm upgrade --install loki grafana/loki \
 --create-namespace --namespace observability \
 -f ./observability/loki.values.yaml \
 --set loki.useTestSchema=true
check_helm_release loki observability

#Install tempo
helm upgrade --install tempo grafana/tempo \
--create-namespace --namespace observability \
-f ./observability/tempo.values.yaml
check_helm_release tempo observability

#Install cert manager
helm upgrade --install cert-manager jetstack/cert-manager \
  --namespace cert-manager \
  --create-namespace \
  --version v1.12.0 \
  --set installCRDs=true \
  --set prometheus.enabled=false \
  --set webhook.timeoutSeconds=4 \
  --set admissionWebhooks.certManager.create=true
check_helm_release cert-manager cert-manager

#Install opentelemetry-operator
helm upgrade --install opentelemetry-operator open-telemetry/opentelemetry-operator \
--create-namespace --namespace observability

kubectl rollout status deployment/opentelemetry-operator --namespace observability --timeout=180s

check_helm_release opentelemetry-operator observability

#Install opentelemetry-collector (this is the one that crashed silently
# and made the script skip prometheus/grafana/zookeeper).
helm upgrade --install opentelemetry-collector ./observability/opentelemetry \
--create-namespace --namespace observability
check_helm_release opentelemetry-collector observability

#Install promtail
minikube ssh -- 'sudo sysctl -w fs.inotify.max_user_instances=1024'
helm upgrade --install promtail grafana/promtail \
--create-namespace --namespace observability \
--values ./observability/promtail.values.yaml
check_helm_release promtail observability

#Install prometheus + grafana
helm upgrade --install prometheus prometheus-community/kube-prometheus-stack \
 --create-namespace --namespace observability \
 -f ./observability/prometheus.values.yaml \
--set hostname="grafana.$DOMAIN" \
--set 'grafana.grafana\.ini.database.user'="$POSTGRESQL_USERNAME" \
--set 'grafana.grafana\.ini.database.password'="$POSTGRESQL_PASSWORD" \
--set grafana.assertNoLeakedSecrets=false
check_helm_release prometheus observability

#Install grafana operator
helm upgrade --install grafana-operator oci://ghcr.io/grafana-operator/helm-charts/grafana-operator \
--version v5.0.2 \
--create-namespace --namespace observability
check_helm_release grafana-operator observability

#Add datasource and dashboard to grafana
helm upgrade --install grafana ./observability/grafana \
--create-namespace --namespace observability \
--set hostname="grafana.$DOMAIN" \
--set grafana.username="$GRAFANA_USERNAME" \
--set grafana.password="$GRAFANA_PASSWORD" \
--set postgresql.username="$POSTGRESQL_USERNAME" \
--set postgresql.password="$POSTGRESQL_PASSWORD"
check_helm_release grafana observability

helm upgrade --install zookeeper ./zookeeper \
 --namespace zookeeper --create-namespace
check_helm_release zookeeper zookeeper