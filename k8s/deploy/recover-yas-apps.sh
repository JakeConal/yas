#!/bin/bash
# Install the selected YAS apps after swagger-ui: the 2 BFFs and
# the backend services kept for the demo flow.
# cd's into deploy dir at the top, so it works from anywhere.

set -ex

DEPLOY_DIR="/mnt/d/StudentLife/2_Workspace/8-misc/yas/k8s/deploy"
cd "$DEPLOY_DIR"

DOMAIN=$(yq -r '.domain' cluster-config.yaml)
MINIKUBE_IP="$(minikube ip)"

echo "============================================================"
echo "[recover-yas-apps.sh] DOMAIN=$DOMAIN MINIKUBE_IP=$MINIKUBE_IP"
echo "============================================================"

DISABLED_BACKEND_SERVICES=(
  location
  payment
  payment-paypal
  promotion
  rating
  recommendation
  webhook
)

for deployment in "${DISABLED_BACKEND_SERVICES[@]}" ; do
    kubectl scale deployment/"$deployment" -n yas --replicas=0 2>/dev/null || true
done

# 1. backoffice-bff (Spring Cloud Gateway → backoffice-ui)
helm dependency build ../charts/backoffice-bff
helm upgrade --install backoffice-bff ../charts/backoffice-bff \
  --namespace yas --create-namespace \
  --set backend.ingress.host="backoffice.$DOMAIN" \
  --set backend.hostAliases[0].ip="$MINIKUBE_IP" \
  --set backend.hostAliases[0].hostnames[0]="identity.$DOMAIN"

# 2. storefront-bff (Spring Cloud Gateway → storefront-ui)
helm dependency build ../charts/storefront-bff
helm upgrade --install storefront-bff ../charts/storefront-bff \
  --namespace yas --create-namespace \
  --set backend.ingress.host="storefront.$DOMAIN" \
  --set backend.hostAliases[0].ip="$MINIKUBE_IP" \
  --set backend.hostAliases[0].hostnames[0]="identity.$DOMAIN"

echo "[recover-yas-apps.sh] BFFs installed. Sleeping 60s before microservice loop..."
sleep 60

# 3. Selected backend services for the demo flow.
ENABLED_BACKEND_SERVICES=(
  product
  cart
  order
  customer
  inventory
  tax
  media
  search
  sampledata
)

i=0
total="${#ENABLED_BACKEND_SERVICES[@]}"
for chart in "${ENABLED_BACKEND_SERVICES[@]}" ; do
    i=$((i+1))
    echo "[recover-yas-apps.sh]   ($i/$total) $chart ..."
    helm dependency build ../charts/"$chart"
    helm upgrade --install "$chart" ../charts/"$chart" \
      --namespace yas --create-namespace \
      --set backend.ingress.host="api.$DOMAIN"
    echo "[recover-yas-apps.sh]   ($i/$total) $chart installed - sleeping 60s..."
    sleep 60
done

echo ""
echo "============================================================"
echo "[recover-yas-apps.sh] all installs complete"
echo "============================================================"
echo "Verify with:"
echo "  kubectl get pods -n yas"
