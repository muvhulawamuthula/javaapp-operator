package io.example;

import io.fabric8.kubernetes.api.model.*;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import java.util.Map;

@KubernetesDependent
public class JavaAppService
        extends CRUDKubernetesDependentResource<Service, JavaApp> {

    public JavaAppService() { super(Service.class); }

    @Override
    protected Service desired(JavaApp primary, Context<JavaApp> ctx) {
        var name   = primary.getMetadata().getName();
        var ns     = primary.getMetadata().getNamespace();
        var labels = Map.of("app", name, "managed-by", "javaapp-operator");

        return new ServiceBuilder()
            .withNewMetadata()
                .withName(name).withNamespace(ns).withLabels(labels)
                .addNewOwnerReference()
                    .withApiVersion(primary.getApiVersion())
                    .withKind(primary.getKind())
                    .withName(name)
                    .withUid(primary.getMetadata().getUid())
                    .withController(true)
                .endOwnerReference()
            .endMetadata()
            .withNewSpec()
                .withClusterIP("None")
                .withPublishNotReadyAddresses(true)
                .withSelector(Map.of("app", name))
                .withPorts(new ServicePortBuilder()
                    .withName("http")
                    .withPort(8080)
                    .withTargetPort(new IntOrString(8080))
                    .build())
            .endSpec().build();
    }
}
