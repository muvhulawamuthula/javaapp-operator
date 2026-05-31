package io.example;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Service;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JavaAppConfigMapAndServiceTest {

    @Test
    void configMapCarriesAppMetadataAndEnv() {
        ConfigMap cm = new JavaAppConfigMap().desired(TestFixtures.javaApp(), null);

        assertThat(cm.getMetadata().getName()).isEqualTo("demo-config");
        assertThat(cm.getData())
                .containsEntry("APP_NAME", "demo")
                .containsEntry("APP_NAMESPACE", "apps")
                .containsEntry("APP_VERSION", "1.0.0")
                .containsEntry("APP_ENV", "test");
    }

    @Test
    void serviceIsHeadlessAndPublishesNotReadyAddresses() {
        Service svc = new JavaAppService().desired(TestFixtures.javaApp(), null);

        assertThat(svc.getMetadata().getName()).isEqualTo("demo");
        assertThat(svc.getSpec().getClusterIP()).isEqualTo("None");
        assertThat(svc.getSpec().getPublishNotReadyAddresses()).isTrue();
        assertThat(svc.getSpec().getPorts().get(0).getPort()).isEqualTo(8080);
        assertThat(svc.getSpec().getSelector()).containsEntry("app", "demo");
    }
}
