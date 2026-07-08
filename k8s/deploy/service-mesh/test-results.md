# Service Mesh Test Results

Captured on 2026-07-09 for namespace `dev`.

## Runtime status

All dev workloads were restarted with Istio sidecars and became ready:

```text
backoffice-bff    true    true,true
cart              true    true,true
customer          true    true,true
inventory         true    true,true
location          true    true,true
media             true    true,true
order             true    true,true
payment           true    true,true
payment-paypal    true    true,true
product           true    true,true
search            true    true,true
storefront-bff    true    true,true
storefront-ui     true    true,true
tax               true    true,true
```

The external dev entrypoints still respond after mesh policy is enabled:

```text
storefront-dev status=200
backoffice-dev status=302
```

## AuthorizationPolicy deny test

Command:

```bash
kubectl exec -n dev mesh-test-client -c curl -- \
  curl -sS -i --max-time 10 http://search/search/storefront/search/suggestions
```

Result:

```text
HTTP/1.1 403 Forbidden
server: envoy

RBAC: access denied
```

This proves an unrelated pod using service account `default` cannot call `search`.

## Allowed service-to-service path

Command:

```bash
kubectl exec -n dev deploy/storefront-bff -c storefront-bff -- \
  wget -S -O - -T 10 http://search/search/storefront/search/suggestions
```

Observed result:

```text
HTTP/1.1 500 Internal Server Error
```

The application endpoint returned `500`, but Envoy did not return `403 RBAC`. The request was allowed by the `search-allow-product-and-storefront-bff` policy and reached the `search` workload.

## mTLS evidence

Command:

```bash
kubectl exec -n dev deploy/search -c istio-proxy -- \
  pilot-agent request GET stats | grep 'connection_security_policy.mutual_tls'
```

Relevant Envoy metric:

```text
source_workload.storefront-bff ... destination_workload.search ... response_code.500 ... connection_security_policy.mutual_tls
source_workload.mesh-test-client ... destination_workload.search ... response_code.403 ... connection_security_policy.mutual_tls
```

This proves the observed service-to-service requests are carried over mutual TLS.

## Retry policy

The retry policy is configured by `VirtualService/search-retry-policy`:

```text
attempts: 3
perTryTimeout: 2s
retryOn: 5xx,connect-failure,refused-stream,gateway-error,reset
timeout: 10s
```

The `search` call produced repeated `500` samples in Envoy/Kiali metrics, which can be used as retry evidence in the Kiali graph and workload traffic panels.

## Kiali

Kiali was installed and responded with HTTP 200:

```bash
kubectl -n kiali-operator port-forward svc/kiali 20001:20001
curl -I http://127.0.0.1:20001/kiali/
```

Result:

```text
HTTP/1.1 200 OK
```

Open `http://127.0.0.1:20001/kiali/`, choose namespace `dev`, then use Graph view to capture the topology screenshot.
