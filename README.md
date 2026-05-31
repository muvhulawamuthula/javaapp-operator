# JavaApp Operator

A Kubernetes operator that automates the full lifecycle of **stateful Java backends**, built with the
[Java Operator SDK (JOSDK)](https://javaoperatorsdk.io) and [Quarkus](https://quarkus.io). It ships an
OLM bundle ready to publish to [OperatorHub.io](https://operatorhub.io).

From a single `JavaApp` custom resource the operator provisions and continuously reconciles:

| Resource | Purpose |
| --- | --- |
| **StatefulSet** | Stable, ordered pods with per-pod `PersistentVolumeClaim`s for durable state |
| **headless Service** | Stable network identities for the pods |
| **ConfigMap** | Application configuration, mounted into every pod via `envFrom` |

## Features

- **Declarative deployment** of a stateful Java workload from one CR.
- **Persistent storage** via StatefulSet `volumeClaimTemplates` — configurable size, `StorageClass`,
  access mode, mount path and claim name.
- **Readiness-gated rolling upgrades**: bumping `spec.version` rewrites the ConfigMap and the pod-template
  config checksum, triggering a rolling update. The operator tracks `updatedReplicas`/`readyReplicas` and
  only marks the app `Running` (and records `status.currentVersion`) once the rollout is complete.
- **HPA-friendly mode** (`spec.hpaManaged: true`) — the operator stops managing the replica count so a
  `HorizontalPodAutoscaler` can own it.
- **Observable status** — `phase`, ready/updated replica counts and Kubernetes-style `conditions`
  (`Ready`, `Progressing`), surfaced as `kubectl get javaapps` printer columns.
- **Graceful teardown** — a finalizer waits for pods to drain before the CR is removed.
- **Schema validation at the API server** — `image` and `storage` are required, `storage.size` is
  pattern-validated, defaults are baked into the CRD.

## The `JavaApp` API

```yaml
apiVersion: example.io/v1alpha1
kind: JavaApp
metadata:
  name: my-java-app
spec:
  image: eclipse-temurin:21-jre   # required
  replicas: 2                     # default 1; ignored when hpaManaged=true
  version: "1.0.0"                # optional; change it to trigger a managed upgrade
  resources:                      # optional; sensible defaults applied
    requestCpu: "500m"
    requestMemory: "512Mi"
    limitCpu: "2"
    limitMemory: "1Gi"
  env:                            # optional; injected via ConfigMap
    APP_ENV: "production"
  storage:                        # required
    size: 10Gi                    # required
    storageClassName: standard    # optional; omit to use the cluster default
    accessMode: ReadWriteOnce
    mountPath: /data
    claimName: data
  hpaManaged: false
```

```console
$ kubectl get javaapps
NAME          PHASE     READY   VERSION
my-java-app   Running   2       1.0.0
```

## Architecture

The reconciler uses a JOSDK **managed workflow** of dependent resources:

```
JavaApp (primary)
  └─ @Workflow
       ├─ ConfigMap        (created first)
       ├─ StatefulSet      (dependsOn ConfigMap — mounts it via envFrom)
       └─ Service
```

`JavaAppReconciler` owns the lifecycle state machine (phase + conditions, upgrade gating, finalizer
cleanup); the three `CRUDKubernetesDependentResource` classes own the desired-state of each child object.
See [`src/main/java/io/example`](src/main/java/io/example).

## Build & test

Requires JDK 21+.

```shell
./mvnw verify                      # compile, generate CRD + OLM bundle, run unit tests
./mvnw verify -DskipITs=false      # also run the cluster integration test (needs a reachable cluster)
```

Build artifacts:

- `target/kubernetes/javaapps.example.io-v1.yml` — the CRD (also checked in at `config/crd/`)
- `target/kubernetes/kubernetes.yml` — operator Deployment + RBAC + ServiceMonitor
- `target/bundle/javaapp-operator/` — the OLM bundle (CSV, CRD, `annotations.yaml`, `bundle.Dockerfile`)

## Run locally (dev mode)

With a cluster reachable via `~/.kube/config`:

```shell
kubectl apply -f config/crd/javaapps.example.io-v1.yml
./mvnw quarkus:dev                 # runs the operator on your machine against the cluster
kubectl apply -f config/samples/example.io_v1alpha1_javaapp.yaml
kubectl get javaapps -w
```

## Deploy to a cluster

```shell
# 1. Build & push the operator image
./mvnw package -Pci -Dquarkus.container-image.push=true \
  -Dquarkus.container-image.registry=quay.io \
  -Dquarkus.container-image.group=<your-namespace>

# 2. Install CRD + operator (RBAC, Deployment, ServiceMonitor)
kubectl apply -f config/crd/javaapps.example.io-v1.yml
kubectl apply -f target/kubernetes/kubernetes.yml

# 3. Create a JavaApp
kubectl apply -f config/samples/example.io_v1alpha1_javaapp.yaml
```

> The generated `ClusterRoleBinding`s leave the ServiceAccount namespace blank so you can deploy into any
> namespace; set it with `-Dquarkus.kubernetes.namespace=<ns>` at build time, or edit the binding before
> applying.

## Publish to OperatorHub (OLM)

The OLM bundle is generated on every build. To publish:

```shell
# 1. Build a release image at the bundle version and push it
./mvnw clean package -Pci -Dquarkus.container-image.push=true

# 2. Build & push the bundle image (from the generated bundle.Dockerfile)
cd target/bundle/javaapp-operator
docker build -f bundle.Dockerfile -t quay.io/<your-namespace>/javaapp-operator-bundle:0.1.0 .
docker push quay.io/<your-namespace>/javaapp-operator-bundle:0.1.0

# 3. Validate the bundle, then test it on a cluster with OLM installed
operator-sdk bundle validate quay.io/<your-namespace>/javaapp-operator-bundle:0.1.0
operator-sdk run bundle quay.io/<your-namespace>/javaapp-operator-bundle:0.1.0
```

To list on [operatorhub.io](https://operatorhub.io), fork
[`k8s-operatorhub/community-operators`](https://github.com/k8s-operatorhub/community-operators) and open a
PR adding `operators/javaapp-operator/0.1.0/` with the contents of `target/bundle/javaapp-operator/`.

Bundle/CSV metadata (display name, description, icon, categories, maintainers, install modes, `alm-examples`)
is declared in [`JavaAppOperatorCSVMetadata.java`](src/main/java/io/example/JavaAppOperatorCSVMetadata.java).

## License

Apache-2.0
