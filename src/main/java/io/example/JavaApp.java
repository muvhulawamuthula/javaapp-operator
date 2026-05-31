package io.example;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("example.io")
@Version("v1alpha1")
@ShortNames("japp")
@Plural("javaapps")
public class JavaApp extends CustomResource<JavaAppSpec, JavaAppStatus> implements Namespaced {
    @JsonIgnore
    public JavaAppStatus getOrInitStatus() {
        if (getStatus() == null) setStatus(new JavaAppStatus());
        return getStatus();
    }
    @Override
    public String toString() {
        return String.format("JavaApp{name=%s, ns=%s, image=%s, replicas=%d}",
            getMetadata().getName(), getMetadata().getNamespace(),
            getSpec() != null ? getSpec().getImage() : "?",
            getSpec() != null ? getSpec().getReplicas() : 0);
    }
}
