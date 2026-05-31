package io.example;

import io.fabric8.kubernetes.api.model.ObjectMeta;

import java.util.Map;

/** Shared builders for constructing {@link JavaApp} fixtures in unit tests. */
final class TestFixtures {
    private TestFixtures() {}

    static JavaApp javaApp() {
        JavaApp app = new JavaApp();
        ObjectMeta meta = new ObjectMeta();
        meta.setName("demo");
        meta.setNamespace("apps");
        meta.setUid("uid-123");
        app.setMetadata(meta);
        app.setSpec(defaultSpec());
        return app;
    }

    static JavaAppSpec defaultSpec() {
        JavaAppSpec spec = new JavaAppSpec();
        spec.setImage("eclipse-temurin:21-jre");
        spec.setReplicas(3);
        spec.setVersion("1.0.0");
        spec.setEnv(Map.of("APP_ENV", "test"));
        StorageSpec storage = new StorageSpec();
        storage.setSize("5Gi");
        storage.setStorageClassName("fast-ssd");
        storage.setAccessMode("ReadWriteOnce");
        storage.setMountPath("/var/data");
        storage.setClaimName("data");
        spec.setStorage(storage);
        return spec;
    }
}
