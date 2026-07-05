#!/bin/bash
set -euo pipefail

cd "$(cd "$(dirname "$0")" && pwd)"

NAMESPACE="${NAMESPACE:-yas}"
TIMEOUT="${TIMEOUT:-900s}"
STARTUP_FAILURE_THRESHOLD="${STARTUP_FAILURE_THRESHOLD:-180}"
PROBE_FAILURE_THRESHOLD="${PROBE_FAILURE_THRESHOLD:-30}"

# Start the services kept for the demo flow one at a time. This avoids several
# BestEffort Java pods competing for memory during Minikube cold starts.
ORDERED_DEPLOYMENTS=(
  product
  cart
  order
  customer
  inventory
  tax
  media
  search
  storefront-bff
  storefront-ui
  backoffice-bff
  backoffice-ui
  swagger-ui
  sampledata
)

DISABLED_DEPLOYMENTS=(
  location
  payment
  payment-paypal
  promotion
  rating
  recommendation
  webhook
)

patch_deployment_for_cold_start() {
  local deployment="$1"

  if kubectl get deployment "$deployment" -n "$NAMESPACE" \
    -o jsonpath='{.spec.template.spec.containers[0].startupProbe.failureThreshold}' 2>/dev/null | grep -q .; then
    kubectl patch deployment "$deployment" -n "$NAMESPACE" --type='json' -p="[
    {\"op\":\"replace\",\"path\":\"/spec/strategy/rollingUpdate/maxSurge\",\"value\":0},
    {\"op\":\"replace\",\"path\":\"/spec/strategy/rollingUpdate/maxUnavailable\",\"value\":1},
    {\"op\":\"replace\",\"path\":\"/spec/template/spec/containers/0/startupProbe/failureThreshold\",\"value\":$STARTUP_FAILURE_THRESHOLD},
    {\"op\":\"replace\",\"path\":\"/spec/template/spec/containers/0/livenessProbe/failureThreshold\",\"value\":$PROBE_FAILURE_THRESHOLD},
    {\"op\":\"replace\",\"path\":\"/spec/template/spec/containers/0/readinessProbe/failureThreshold\",\"value\":$PROBE_FAILURE_THRESHOLD}
  ]"
  else
    kubectl patch deployment "$deployment" -n "$NAMESPACE" --type='json' -p="[
    {\"op\":\"replace\",\"path\":\"/spec/strategy/rollingUpdate/maxSurge\",\"value\":0},
    {\"op\":\"replace\",\"path\":\"/spec/strategy/rollingUpdate/maxUnavailable\",\"value\":1}
  ]"
  fi
}

wait_for_pod_to_exist() {
  local deployment="$1"
  local waited=0

  until kubectl get pod -n "$NAMESPACE" \
    -l "app.kubernetes.io/name=$deployment" \
    --no-headers 2>/dev/null | grep -q .; do
    if [ "$waited" -ge 120 ]; then
      echo "Timed out waiting for pod for deployment/$deployment to be created" >&2
      return 1
    fi

    sleep 2
    waited=$((waited + 2))
  done
}

echo "Scaling disabled deployments down in namespace '$NAMESPACE'..."
for deployment in "${DISABLED_DEPLOYMENTS[@]}"; do
  kubectl scale deployment/"$deployment" -n "$NAMESPACE" --replicas=0 2>/dev/null || true
done

echo "Scaling ordered deployments down in namespace '$NAMESPACE'..."
kubectl scale -n "$NAMESPACE" "${ORDERED_DEPLOYMENTS[@]/#/deployment/}" --replicas=0

echo "Waiting for old ordered pods to terminate..."
kubectl wait --for=delete pod -n "$NAMESPACE" \
  -l 'app.kubernetes.io/name in (product,cart,order,customer,inventory,tax,media,search,storefront-bff,storefront-ui,backoffice-bff,backoffice-ui,swagger-ui,sampledata)' \
  --timeout=180s || true

for deployment in "${ORDERED_DEPLOYMENTS[@]}"; do
  echo ""
  echo "Preparing $deployment for slow Minikube cold start..."
  patch_deployment_for_cold_start "$deployment"

  echo "Starting $deployment..."
  kubectl scale deployment/"$deployment" -n "$NAMESPACE" --replicas=1
  wait_for_pod_to_exist "$deployment"
  kubectl wait --for=condition=Ready pod -n "$NAMESPACE" \
    -l "app.kubernetes.io/name=$deployment" \
    --timeout="$TIMEOUT"
done

echo ""
echo "Ordered deployments are available:"
kubectl get -n "$NAMESPACE" "${ORDERED_DEPLOYMENTS[@]/#/deployment/}" \
  -o custom-columns=NAME:.metadata.name,DESIRED:.spec.replicas,READY:.status.readyReplicas,AVAILABLE:.status.availableReplicas

echo ""
echo "Current $NAMESPACE pods:"
kubectl get pods -n "$NAMESPACE"
