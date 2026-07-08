# YAS Service Mesh

This folder contains the Istio service mesh setup used for the YAS DevOps project.

## Scope

- Mesh runtime: Istio.
- Topology UI: Kiali.
- Mesh namespace: `dev`.
- Demo policies:
  - Strict mTLS in `dev`.
  - Istio mutual TLS for service-to-service traffic.
  - Retry policy for calls to `search`.
  - Authorization policy that allows `search` only from `storefront-bff` and `product`.
  - Authorization policy that allows `product` only from `storefront-bff`, `backoffice-bff`, `order`, and `sampledata`.
  - PERMISSIVE mTLS only on ingress-facing workloads so `ingress-nginx` can still serve the browser routes.

Staging is intentionally left with workloads scaled to 0. The same manifests can be copied to `staging` when staging workloads are enabled.

## Install

```bash
helm repo add istio https://istio-release.storage.googleapis.com/charts
helm repo add kiali https://kiali.org/helm-charts
helm repo update

kubectl create namespace istio-system --dry-run=client -o yaml | kubectl apply -f -
helm upgrade --install istio-base istio/base -n istio-system --version 1.30.2
helm upgrade --install istiod istio/istiod -n istio-system --version 1.30.2 --wait

kubectl create namespace kiali-operator --dry-run=client -o yaml | kubectl apply -f -
helm upgrade --install kiali-server kiali/kiali-server \
  -n kiali-operator \
  --version 2.28.0 \
  -f k8s/deploy/service-mesh/kiali.values.yaml \
  --wait
```

## Enable sidecar injection

```bash
kubectl label namespace dev istio-injection=enabled --overwrite
kubectl rollout restart deployment -n dev
kubectl rollout status deployment -n dev --timeout=10m
```

If the nginx ingress controller is not installed as an Istio gateway, keep the browser entrypoints working by injecting the ingress controller too, or by using the workload-level `PERMISSIVE` exceptions in `mesh-policies.yaml`.

```bash
kubectl label namespace ingress-nginx istio-injection=enabled --overwrite
kubectl rollout restart deployment/ingress-nginx-controller -n ingress-nginx
kubectl rollout status deployment/ingress-nginx-controller -n ingress-nginx --timeout=3m
```

## Apply policies

```bash
kubectl apply -f k8s/deploy/service-mesh/mesh-policies.yaml
kubectl apply -f k8s/deploy/service-mesh/test-client.yaml
```

## Kiali

```bash
kubectl port-forward -n kiali-operator svc/kiali 20001:20001
```

Open:

```text
http://localhost:20001
```

Use Kiali Graph with namespace `dev` to capture the topology screenshot.

## Test plan

Verify sidecars:

```bash
kubectl get pod -n dev -o custom-columns=NAME:.metadata.name,CONTAINERS:.spec.containers[*].name
```

Verify mTLS:

```bash
istioctl authn tls-check "$(kubectl get pod -n dev -l app.kubernetes.io/name=storefront-bff -o jsonpath='{.items[0].metadata.name}')" -n dev
```

Allowed call to `search` from `storefront-bff`:

```bash
kubectl exec -n dev deploy/storefront-bff -c storefront-bff -- \
  wget -S -O /tmp/search-ok.json http://search/search/storefront/search/suggestions 2>&1 | head -30
```

Denied call to `search` from an unrelated test pod:

```bash
kubectl exec -n dev mesh-test-client -c curl -- \
  curl -sS -i http://search/search/storefront/search/suggestions
```

Expected result for the denied call is an HTTP 403 from Envoy RBAC.

Retry evidence:

```bash
kubectl describe virtualservice -n dev search-retry-policy
kubectl logs -n dev deploy/storefront-bff -c istio-proxy --tail=100
```

The `VirtualService` has `attempts: 3` and retries on `5xx,connect-failure,refused-stream,gateway-error,reset`.

The captured run for this cluster is documented in `test-results.md`.
