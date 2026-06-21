#!/bin/bash
# Install everything deploy-yas-applications.sh should have installed
# past swagger-ui: the 2 BFFs and the 16 microservices.
# cd's into deploy dir at the top, so it works from anywhere.

set -ex

DEPLOY_DIR="/mnt/d/StudentLife/2_Workspace/8-misc/yas/k8s/deploy"
cd "$DEPLOY_DIR"

DOMAIN=$(yq -r '.domain' cluster-config.yaml)
MINIKUBE_IP="$(minikube ip)"

echo "============================================================"
echo "[recover-yas-apps.sh] DOMAIN=$DOMAIN MINIKUBE_IP=$MINIKUBE_IP"
echo "============================================================"

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

# 3. The 16 microservices (cart, customer, inventory, ..., sampledata)
i=0
for chart in cart customer inventory location media order payment payment-paypal product promotion rating search tax recommendation webhook sampledata ; do
    i=$((i+1))
    echo "[recover-yas-apps.sh]   ($i/16) $chart ..."
    helm dependency build ../charts/"$chart"
    helm upgrade --install "$chart" ../charts/"$chart" \
      --namespace yas --create-namespace \
      --set backend.ingress.host="api.$DOMAIN"
    echo "[recover-yas-apps.sh]   ($i/16) $chart installed — sleeping 60s..."
    sleep 60
done

echo ""
echo "============================================================"
echo "[recover-yas-apps.sh] all installs complete"
echo "============================================================"
echo "Verify with:"
echo "  kubectl get pods -n yas"