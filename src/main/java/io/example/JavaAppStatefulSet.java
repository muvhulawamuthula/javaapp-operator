package io.example;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import java.util.*;

@KubernetesDependent
public class JavaAppStatefulSet
        extends CRUDKubernetesDependentResource<StatefulSet, JavaApp> {

    public JavaAppStatefulSet() { super(StatefulSet.class); }

    @Override
    protected StatefulSet desired(JavaApp primary, Context<JavaApp> ctx) {
        var spec   = primary.getSpec();
        var name   = primary.getMetadata().getName();
        var ns     = primary.getMetadata().getNamespace();
        var labels = Map.of("app", name, "managed-by", "javaapp-operator");

        List<EnvVar> envVars = new ArrayList<>();
        if (spec.getEnv() != null) {
            spec.getEnv().forEach((k, v) ->
                envVars.add(new EnvVarBuilder().withName(k).withValue(v).build()));
        }

        var container = new ContainerBuilder()
            .withName("app")
            .withImage(spec.getImage())
            .withEnv(envVars)
            .withVolumeMounts(new VolumeMountBuilder()
                .withName(spec.getStorage().getClaimName())
                .withMountPath(spec.getStorage().getMountPath()).build())
            .withEnvFrom(new EnvFromSourceBuilder()
                .withNewConfigMapRef().withName(name + "-config").endConfigMapRef().build())
            .withResources(buildResources(spec.getResources()))
            .withReadinessProbe(new ProbeBuilder()
                .withNewHttpGet().withPath("/q/health/ready")
                    .withPort(new IntOrString(8080)).endHttpGet()
                .withInitialDelaySeconds(10).withPeriodSeconds(5).build())
            .withLivenessProbe(new ProbeBuilder()
                .withNewHttpGet().withPath("/q/health/live")
                    .withPort(new IntOrString(8080)).endHttpGet()
                .withInitialDelaySeconds(30).withPeriodSeconds(10).build())
            .build();

        return new StatefulSetBuilder()
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
                .withReplicas(spec.isHpaManaged() ? null : spec.getReplicas())
                .withServiceName(name)
                .withNewSelector().addToMatchLabels(labels).endSelector()
                .withNewTemplate()
                    .withNewMetadata().withLabels(labels)
                        // Roll the pods whenever the rendered config (env + version) changes.
                        .addToAnnotations("checksum/config",
                            Integer.toHexString(Objects.hash(spec.getEnv(), spec.getVersion())))
                    .endMetadata()
                    .withNewSpec()
                        .withContainers(container)
                        .withTerminationGracePeriodSeconds(60L)
                    .endSpec()
                .endTemplate()
                .withVolumeClaimTemplates(new PersistentVolumeClaimBuilder()
                    .withNewMetadata().withName(spec.getStorage().getClaimName()).endMetadata()
                    .withNewSpec()
                        .withAccessModes(spec.getStorage().getAccessMode())
                        // null storageClassName => use the cluster's default StorageClass
                        .withStorageClassName(spec.getStorage().getStorageClassName())
                        .withNewResources()
                            .addToRequests("storage", new Quantity(spec.getStorage().getSize()))
                        .endResources()
                    .endSpec().build())
            .endSpec().build();
    }

    private ResourceRequirements buildResources(ResourcesSpec res) {
        if (res == null) res = new ResourcesSpec();
        return new ResourceRequirementsBuilder()
            .addToRequests("cpu",    new Quantity(res.getRequestCpu()))
            .addToRequests("memory", new Quantity(res.getRequestMemory()))
            .addToLimits("cpu",      new Quantity(res.getLimitCpu()))
            .addToLimits("memory",   new Quantity(res.getLimitMemory()))
            .build();
    }
}
