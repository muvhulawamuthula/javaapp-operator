package io.example;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import java.util.HashMap;
import java.util.Map;

@KubernetesDependent
public class JavaAppConfigMap
        extends CRUDKubernetesDependentResource<ConfigMap, JavaApp> {

    public JavaAppConfigMap() { super(ConfigMap.class); }

    @Override
    protected ConfigMap desired(JavaApp primary, Context<JavaApp> ctx) {
        var spec = primary.getSpec();
        var name = primary.getMetadata().getName();
        var ns   = primary.getMetadata().getNamespace();

        Map<String, String> data = new HashMap<>();
        data.put("APP_NAME", name);
        data.put("APP_NAMESPACE", ns);
        if (spec.getVersion() != null) data.put("APP_VERSION", spec.getVersion());
        if (spec.getEnv() != null) data.putAll(spec.getEnv());

        return new ConfigMapBuilder()
            .withNewMetadata()
                .withName(name + "-config").withNamespace(ns)
                .withLabels(Map.of("app", name, "managed-by", "javaapp-operator"))
                .addNewOwnerReference()
                    .withApiVersion(primary.getApiVersion())
                    .withKind(primary.getKind())
                    .withName(name)
                    .withUid(primary.getMetadata().getUid())
                    .withController(true)
                .endOwnerReference()
            .endMetadata()
            .withData(data).build();
    }
}
