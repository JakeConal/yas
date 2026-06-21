#!/bin/bash
set -ex

# Always run from this script's own directory so relative paths
# (./cluster-config.yaml, ../charts/...) resolve regardless of where
# the script was invoked from.
cd "$(dirname "$0")"

# Auto restart when change configmap or secret
helm repo add stakater https://stakater.github.io/stakater-charts
helm repo update

read -rd '' DOMAIN \
< <(yq -r '.domain' ./cluster-config.yaml)

# Bail loudly if cluster-config.yaml was unreadable or .domain is missing.
# Without this, an empty DOMAIN silently propagates into every --set flag
# and helm installs fail with confusing "api." hostnames.
if [ -z "$DOMAIN" ] || [ "$DOMAIN" = "null" ]; then
  echo "FATAL: could not read .domain from ./cluster-config.yaml (cwd=$(pwd))" >&2
  exit 1
fi

MINIKUBE_IP="$(minikube ip)"

helm dependency build ../charts/backoffice-bff
helm upgrade --install backoffice-bff ../charts/backoffice-bff \
--namespace yas --create-namespace \
--set backend.ingress.host="backoffice.$DOMAIN" \
--set backend.hostAliases[0].ip="$MINIKUBE_IP" \
--set backend.hostAliases[0].hostnames[0]="identity.$DOMAIN"

helm dependency build ../charts/backoffice-ui
helm upgrade --install backoffice-ui ../charts/backoffice-ui \
--namespace yas --create-namespace

sleep 60

helm dependency build ../charts/storefront-bff
helm upgrade --install storefront-bff ../charts/storefront-bff \
--namespace yas --create-namespace \
--set backend.ingress.host="storefront.$DOMAIN" \
--set backend.hostAliases[0].ip="$MINIKUBE_IP" \
--set backend.hostAliases[0].hostnames[0]="identity.$DOMAIN"

helm dependency build ../charts/storefront-ui
helm upgrade --install storefront-ui ../charts/storefront-ui \
--namespace yas --create-namespace

sleep 60

helm upgrade --install swagger-ui ../charts/swagger-ui \
--namespace yas --create-namespace \
--set ingress.host="api.$DOMAIN"

sleep 20

for chart in {"cart","customer","inventory","location","media","order","payment","payment-paypal","product","promotion","rating","search","tax","recommendation","webhook","sampledata"} ; do
    helm dependency build ../charts/"$chart"
    helm upgrade --install "$chart" ../charts/"$chart" \
    --namespace yas --create-namespace \
    --set backend.ingress.host="api.$DOMAIN"
    sleep 60
done
