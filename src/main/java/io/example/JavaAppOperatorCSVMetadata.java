package io.example;

import io.quarkiverse.operatorsdk.annotations.CSVMetadata;
import io.quarkiverse.operatorsdk.annotations.SharedCSVMetadata;

/**
 * OLM / OperatorHub bundle metadata for the JavaApp Operator.
 *
 * <p>This drives the generated {@code ClusterServiceVersion} (CSV) — the document OperatorHub renders
 * on the operator's listing page and that OLM uses to install the operator. It is generated at build
 * time into {@code target/bundle/} by the {@code quarkus-operator-sdk-bundle-generator} extension.
 *
 * <p>The {@code version} here is the OLM operator version and must be valid semver (no {@code -SNAPSHOT}),
 * independent of the Maven project version.
 */
@CSVMetadata(
    bundleName = "javaapp-operator",
    csvName = "javaapp-operator.v0.1.0",
    version = "0.1.0",
    displayName = "JavaApp Operator",
    description = """
        The JavaApp Operator automates day-1 and day-2 lifecycle management for stateful Java backends \
        on Kubernetes. From a single `JavaApp` custom resource it provisions and continuously \
        reconciles a StatefulSet (with per-pod PersistentVolumeClaims), a headless Service for stable \
        network identities, and a ConfigMap for application configuration.

        Features:
        - Declarative deployment of stateful Java workloads from one CR.
        - Persistent storage via StatefulSet volumeClaimTemplates with configurable size, \
          StorageClass and access mode.
        - Readiness-gated rolling upgrades: bumping `spec.version` triggers a managed rollout that \
          the operator tracks to completion before marking the app `Running`.
        - Optional HPA-friendly mode that yields replica management to a HorizontalPodAutoscaler.
        - Rich, observable status: `phase`, ready/updated replica counts and Kubernetes-style \
          conditions (`Ready`, `Progressing`), surfaced as `kubectl get javaapps` printer columns.
        - Graceful, finalizer-based teardown that waits for pods to drain.

        Built with the Java Operator SDK (JOSDK) and Quarkus.""",
    keywords = { "java", "stateful", "statefulset", "lifecycle", "quarkus", "josdk", "backend" },
    maturity = "alpha",
    provider = @CSVMetadata.Provider(
        name = "muvhulawamuthula",
        url = "https://github.com/muvhulawamuthula/javaapp-operator"
    ),
    maintainers = @CSVMetadata.Maintainer(
        name = "muvhulawamuthula",
        email = "mutulamuvhulawa@gmail.com"
    ),
    icon = @CSVMetadata.Icon(fileName = "icon.svg", mediatype = "image/svg+xml"),
    links = {
        @CSVMetadata.Link(name = "Source", url = "https://github.com/muvhulawamuthula/javaapp-operator"),
        @CSVMetadata.Link(name = "Java Operator SDK", url = "https://javaoperatorsdk.io")
    },
    installModes = {
        @CSVMetadata.InstallMode(type = "OwnNamespace", supported = true),
        @CSVMetadata.InstallMode(type = "SingleNamespace", supported = true),
        @CSVMetadata.InstallMode(type = "MultiNamespace", supported = false),
        @CSVMetadata.InstallMode(type = "AllNamespaces", supported = true)
    },
    annotations = @CSVMetadata.Annotations(
        categories = "Application Runtime,Developer Tools",
        capabilities = "Seamless Upgrades",
        repository = "https://github.com/muvhulawamuthula/javaapp-operator",
        containerImage = "quay.io/muvhulawamuthula/javaapp-operator:0.1.0",
        certified = false,
        almExamples = """
            [
              {
                "apiVersion": "example.io/v1alpha1",
                "kind": "JavaApp",
                "metadata": { "name": "my-java-app" },
                "spec": {
                  "image": "eclipse-temurin:21-jre",
                  "replicas": 2,
                  "version": "1.0.0",
                  "resources": {
                    "requestCpu": "500m", "requestMemory": "512Mi",
                    "limitCpu": "2", "limitMemory": "1Gi"
                  },
                  "env": { "APP_ENV": "production" },
                  "storage": {
                    "size": "10Gi",
                    "accessMode": "ReadWriteOnce",
                    "mountPath": "/data"
                  },
                  "hpaManaged": false
                }
              }
            ]"""
    )
)
public class JavaAppOperatorCSVMetadata implements SharedCSVMetadata {
}
