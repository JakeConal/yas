# Docker Hub CI

Every service workflow publishes a Docker image after a push that changes that
service. The immutable image tag is the full commit SHA from `github.sha`. A push
to `main` also updates the `latest` tag for compatibility with the default Helm
values.

Pull request workflows still run tests and builds, but they do not publish
images. This prevents untrusted pull requests from accessing Docker Hub
credentials.

## Repository secrets

Create these Actions secrets under **Settings → Secrets and variables → Actions**:

- `DOCKERHUB_USERNAME`: Docker Hub account name.
- `DOCKERHUB_TOKEN`: Docker Hub access token with read/write permission.

Do not use the Docker Hub account password and do not commit either value to the
repository.

## Verification

Push a code change on a feature branch, for example:

```shell
git switch -c dev-tax-service
git add tax
git commit -m "Test tax service CI"
git push -u origin dev-tax-service
```

Get the expected tag locally:

```shell
git rev-parse HEAD
```

After the `tax service ci` workflow succeeds, Docker Hub must contain:

```text
<dockerhub-username>/yas-tax:<full-commit-sha>
```

The same convention applies to every service with a Dockerfile, such as
`yas-product`, `yas-cart`, `yas-storefront-bff`, and `yas-storefront`.
