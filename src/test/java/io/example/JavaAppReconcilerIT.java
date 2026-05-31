package io.example;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.javaoperatorsdk.operator.junit.LocallyRunOperatorExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End-to-end test that runs the reconciler locally against a real Kubernetes cluster
 * (e.g. kind / minikube / k3s) using the JOSDK test harness.
 *
 * <p>Skipped by default; enable with {@code mvn verify -DskipITs=false} against a cluster
 * reachable via {@code ~/.kube/config}. The harness creates a throwaway namespace per run,
 * applies the generated CRD, and tears everything down afterwards.
 */
class JavaAppReconcilerIT {

    @RegisterExtension
    static final LocallyRunOperatorExtension operator = LocallyRunOperatorExtension.builder()
            .withReconciler(new JavaAppReconciler())
            .build();

    @Test
    void createsManagedResourcesAndReportsStatus() {
        JavaApp app = new JavaApp();
        app.setMetadata(new ObjectMetaBuilder().withName("it-app").build());
        JavaAppSpec spec = new JavaAppSpec();
        spec.setImage("eclipse-temurin:21-jre");
        spec.setReplicas(1);
        spec.setVersion("1.0.0");
        spec.setEnv(Map.of("APP_ENV", "it"));
        StorageSpec storage = new StorageSpec();
        storage.setSize("1Gi");
        spec.setStorage(storage);
        app.setSpec(spec);

        operator.create(app);

        // The StatefulSet, Service and ConfigMap should be created by the workflow dependents.
        await().atMost(Duration.ofMinutes(2)).untilAsserted(() -> {
            StatefulSet sts = operator.get(StatefulSet.class, "it-app");
            assertThat(sts).isNotNull();
            assertThat(sts.getSpec().getReplicas()).isEqualTo(1);
            assertThat(operator.get(io.fabric8.kubernetes.api.model.Service.class, "it-app")).isNotNull();
            assertThat(operator.get(io.fabric8.kubernetes.api.model.ConfigMap.class, "it-app-config")).isNotNull();
        });

        // And the operator should write back an observable status phase.
        await().atMost(Duration.ofMinutes(2)).untilAsserted(() -> {
            JavaApp reconciled = operator.get(JavaApp.class, "it-app");
            assertThat(reconciled.getStatus()).isNotNull();
            assertThat(reconciled.getStatus().getPhase()).isIn("Pending", "Running", "Upgrading");
        });
    }
}
