#!/bin/bash
# Rebuild payment + payment-paypal docker images, load into minikube,
# restart the deployments. The deployed images were built BEFORE the
# SQL fix (payment: is_enabled -> enabled) and BEFORE the Dockerfile
# fix (payment-paypal: *.jar -> *-exec.jar), so both pods are
# CrashLoopBackOff until the new images are loaded.

set -ex
set -o pipefail   # so `./mvnw ... | tail -N` fails when mvnw fails

# Source SDKMAN! so both java and any sdk-installed tools are on PATH,
# and point JAVA_HOME at the current SDKMAN java symlink.
if [ -f "$HOME/.sdkman/bin/sdkman-init.sh" ]; then
  source "$HOME/.sdkman/bin/sdkman-init.sh"
fi
export JAVA_HOME="${JAVA_HOME:-$HOME/.sdkman/candidates/java/current}"

REPO="/mnt/d/StudentLife/2_Workspace/8-misc/yas"

# Fix CRLF line endings on the Maven Wrapper scripts if needed.
# Without this, /bin/sh (dash) chokes on the \r and mvnw fails with
# "cannot execute: required file not found". The .gitattributes rule
# `*.sh text eol=lf` prevents this on future checkouts but doesn't
# fix files already tracked with CRLF.
for module in payment payment-paypal; do
  for f in "$REPO/$module/mvnw" "$REPO/$module/mvnw.cmd"; do
    if [ -f "$f" ] && file "$f" | grep -q CRLF; then
      echo "[$module] stripping CRLF from $f"
      sed -i 's/\r$//' "$f"
      chmod +x "$f"
    fi
  done
done

build_module() {
  local module="$1"
  local image="$2"

  echo ""
  echo "============================================================"
  echo "[$module] building JAR with ./mvnw package"
  echo "============================================================"
  cd "$REPO/$module" || { echo "FATAL: $REPO/$module not found"; exit 1; }
  ./mvnw package -DskipTests -B

  echo "[$module] verifying exec jar exists"
  local exec_jar
  exec_jar=$(ls target/*-exec.jar 2>/dev/null | head -1)
  if [ -z "$exec_jar" ]; then
    echo "FATAL: no *-exec.jar found in $REPO/$module/target/ — mvnw build likely failed silently" >&2
    ls -la "$REPO/$module/target/" >&2
    exit 1
  fi
  echo "[$module] using jar: $exec_jar"

  echo "[$module] building docker image $image:latest"
  docker build -t "$image:latest" .

  echo "[$module] loading $image:latest into minikube"
  minikube image load "$image:latest"

  echo "[$module] restarting deployment"
  kubectl rollout restart "deployment/$module" -n yas
}

build_module "payment"        "ghcr.io/nashtech-garage/yas-payment"
build_module "payment-paypal" "ghcr.io/nashtech-garage/yas-payment-paypal"

echo ""
echo "============================================================"
echo "[all] waiting for rollouts"
echo "============================================================"
kubectl rollout status deployment/payment        -n yas --timeout=300s
kubectl rollout status deployment/payment-paypal -n yas --timeout=300s

echo ""
echo "============================================================"
echo "[all] done. Verify with:"
echo "  kubectl get pods -n yas -l app.kubernetes.io/name=payment"
echo "============================================================"