package io.example;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.fabric8.generator.annotation.Default;
import io.fabric8.generator.annotation.Min;
import io.fabric8.generator.annotation.Required;
import java.util.Map;

public class JavaAppSpec {
    @Required
    @JsonPropertyDescription("OCI image reference, e.g. eclipse-temurin:21-jre")
    private String image;

    @Min(0)
    @Default("1")
    @JsonPropertyDescription("Number of replicas. Defaults to 1.")
    private int replicas = 1;

    @JsonPropertyDescription("Logical application version, e.g. 1.2.0. Changing it triggers a managed rolling upgrade.")
    private String version;

    @JsonPropertyDescription("CPU and memory requests/limits for each pod.")
    private ResourcesSpec resources;

    @JsonPropertyDescription("Extra environment variables injected into every pod.")
    private Map<String, String> env;

    @Required
    @JsonPropertyDescription("PersistentVolumeClaim template for the StatefulSet.")
    private StorageSpec storage;

    @JsonProperty("hpaManaged")
    @JsonPropertyDescription("When true the operator will NOT manage replica count (leave it to an HPA).")
    private boolean hpaManaged = false;

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public int getReplicas() { return replicas; }
    public void setReplicas(int replicas) { this.replicas = replicas; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public ResourcesSpec getResources() { return resources; }
    public void setResources(ResourcesSpec resources) { this.resources = resources; }
    public Map<String, String> getEnv() { return env; }
    public void setEnv(Map<String, String> env) { this.env = env; }
    public StorageSpec getStorage() { return storage; }
    public void setStorage(StorageSpec storage) { this.storage = storage; }
    public boolean isHpaManaged() { return hpaManaged; }
    public void setHpaManaged(boolean hpaManaged) { this.hpaManaged = hpaManaged; }
}
