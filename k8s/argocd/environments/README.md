# YAS Dev and Staging with ArgoCD

This folder defines one ArgoCD Application for each YAS environment:

- `yas-dev` manages all application charts in the `dev` namespace from `env-dev`.
- `yas-staging` manages all application charts in the `staging` namespace from `env-staging`.

Each generated Application uses ArgoCD multi-source support. The service charts remain
independent under `k8s/charts`, so the GitOps workflow can still update only the image tag
of a changed service. ArgoCD then detects the changed environment revision and syncs the
single environment Application; Kubernetes only rolls out workloads whose rendered
manifests changed.

## Safe migration from per-service Applications

The ApplicationSet sets `preserveResourcesOnDeletion: true`. Apply the migration in two
steps so deleting the old generated Applications does not delete their live workloads:

```sh
kubectl patch applicationset yas-dev-staging-services -n argocd \
  --type merge \
  -p '{"spec":{"syncPolicy":{"preserveResourcesOnDeletion":true}}}'

kubectl apply -k k8s/argocd/environments
```

After reconciliation, verify that only the two environment Applications remain and that
they are healthy:

```sh
kubectl get applications -n argocd -l app.kubernetes.io/part-of=yas
kubectl get pods -n dev
kubectl get pods -n staging
```

## Deployment flow

1. A commit on `main` triggers CI only for services whose source or shared build files changed.
2. Each service runs tests, builds the application, and publishes an immutable SHA image.
3. Only after that service CI succeeds, its `Deploy` job calls
   `.github/workflows/argocd-gitops.yaml` to update the matching chart on `env-dev`.
4. ArgoCD refreshes `yas-dev`; only Deployments whose rendered image changed create new
   ReplicaSets and pods.
5. A `v*` release tag publishes both SHA and release-tag images, then follows the same
   per-service flow through `env-staging` and `yas-staging`.

If tests, build, or image publishing fails, the `Deploy` job is skipped and the environment
branch remains on its last known-good image.

Environment hosts:

- Dev storefront: `storefront-dev.yas.local.com`
- Dev backoffice: `backoffice-dev.yas.local.com`
- Dev Swagger: `api-dev.yas.local.com/swagger-ui`
- Staging storefront: `storefront-staging.yas.local.com`
- Staging backoffice: `backoffice-staging.yas.local.com`
- Staging Swagger: `api-staging.yas.local.com/swagger-ui`
