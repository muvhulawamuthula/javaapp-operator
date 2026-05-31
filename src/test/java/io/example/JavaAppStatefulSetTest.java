package io.example;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JavaAppStatefulSetTest {

    private final JavaAppStatefulSet dependent = new JavaAppStatefulSet();

    @Test
    void rendersDesiredStatefulSetFromSpec() {
        JavaApp app = TestFixtures.javaApp();

        StatefulSet sts = dependent.desired(app, null);

        assertThat(sts.getMetadata().getName()).isEqualTo("demo");
        assertThat(sts.getMetadata().getNamespace()).isEqualTo("apps");
        assertThat(sts.getSpec().getReplicas()).isEqualTo(3);
        assertThat(sts.getSpec().getServiceName()).isEqualTo("demo");

        var container = sts.getSpec().getTemplate().getSpec().getContainers().get(0);
        assertThat(container.getImage()).isEqualTo("eclipse-temurin:21-jre");
        assertThat(container.getReadinessProbe().getHttpGet().getPath()).isEqualTo("/q/health/ready");
        assertThat(container.getLivenessProbe().getHttpGet().getPath()).isEqualTo("/q/health/live");
        // config injected via envFrom -> <name>-config ConfigMap
        assertThat(container.getEnvFrom().get(0).getConfigMapRef().getName()).isEqualTo("demo-config");
        // explicit env var rendered
        assertThat(container.getEnv()).anyMatch(e -> e.getName().equals("APP_ENV") && e.getValue().equals("test"));
    }

    @Test
    void wiresStorageIntoVolumeClaimTemplate() {
        StatefulSet sts = dependent.desired(TestFixtures.javaApp(), null);

        PersistentVolumeClaim pvc = sts.getSpec().getVolumeClaimTemplates().get(0);
        assertThat(pvc.getMetadata().getName()).isEqualTo("data");
        assertThat(pvc.getSpec().getStorageClassName()).isEqualTo("fast-ssd");
        assertThat(pvc.getSpec().getAccessModes()).containsExactly("ReadWriteOnce");
        assertThat(pvc.getSpec().getResources().getRequests().get("storage").toString()).isEqualTo("5Gi");
    }

    @Test
    void nullStorageClassFallsBackToClusterDefault() {
        JavaApp app = TestFixtures.javaApp();
        app.getSpec().getStorage().setStorageClassName(null);

        StatefulSet sts = dependent.desired(app, null);

        assertThat(sts.getSpec().getVolumeClaimTemplates().get(0).getSpec().getStorageClassName()).isNull();
    }

    @Test
    void setsControllerOwnerReference() {
        StatefulSet sts = dependent.desired(TestFixtures.javaApp(), null);

        OwnerReference owner = sts.getMetadata().getOwnerReferences().get(0);
        assertThat(owner.getName()).isEqualTo("demo");
        assertThat(owner.getUid()).isEqualTo("uid-123");
        assertThat(owner.getController()).isTrue();
    }

    @Test
    void hpaManagedLeavesReplicasUnset() {
        JavaApp app = TestFixtures.javaApp();
        app.getSpec().setHpaManaged(true);

        StatefulSet sts = dependent.desired(app, null);

        // replicas null => the operator does not fight an external HorizontalPodAutoscaler
        assertThat(sts.getSpec().getReplicas()).isNull();
    }

    @Test
    void configChecksumChangesWhenVersionChanges() {
        JavaApp v1 = TestFixtures.javaApp();
        JavaApp v2 = TestFixtures.javaApp();
        v2.getSpec().setVersion("2.0.0");

        String c1 = dependent.desired(v1, null).getSpec().getTemplate()
                .getMetadata().getAnnotations().get("checksum/config");
        String c2 = dependent.desired(v2, null).getSpec().getTemplate()
                .getMetadata().getAnnotations().get("checksum/config");

        assertThat(c1).isNotEqualTo(c2);
    }
}
