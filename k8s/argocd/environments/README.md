# YAS Dev and Staging with ArgoCD

This folder defines the ArgoCD handler for the two required environments:

- `dev`: continuous deployment into the `dev` namespace.
- `staging`: release deployment into the `staging` namespace.

The `yas-dev-staging-services` ApplicationSet generates one ArgoCD Application per
YAS service and per environment. For example:

- `dev-product`
- `dev-order`
- `dev-storefront-bff`
- `staging-product`
- `staging-order`
- `staging-storefront-bff`

Apply it with:

```sh
kubectl apply -k k8s/argocd/environments
```

The current `targetRevision` is `main` so the GKE demo cluster syncs from the same
branch used by the GitHub Actions CI/CD flow required by the assignment:

- `dev.targetRevision`: `main`
- `staging.targetRevision`: a release tag such as `v1.2.3`, or a release branch such as `staging` / `rc_v1.2.3`

Recommended GitHub Actions flow:

1. On commit to `main`, GitHub Actions builds service images with a dev tag, updates the
   dev manifests or image tags, and ArgoCD auto-syncs the `dev-*` apps.
2. On release tag `v1.2.3`, GitHub Actions builds images with tag `v1.2.3`, updates the
   staging manifests or switches staging `targetRevision` to the tag, and ArgoCD
   syncs the `staging-*` apps.

Environment hosts:

- Dev storefront: `storefront-dev.yas.local.com`
- Staging storefront: `storefront-staging.yas.local.com`

Add these hostnames to your local `/etc/hosts` with the same ingress IP used by the
current cluster before opening them in the browser.
