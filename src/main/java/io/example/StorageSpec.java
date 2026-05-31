package io.example;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.fabric8.generator.annotation.Default;
import io.fabric8.generator.annotation.Pattern;
import io.fabric8.generator.annotation.Required;

public class StorageSpec {
    @Required
    @Pattern("^[0-9]+(\\.[0-9]+)?(Ei|Pi|Ti|Gi|Mi|Ki|E|P|T|G|M|k)?$")
    @JsonPropertyDescription("Storage size, e.g. 10Gi")
    private String size;

    @JsonPropertyDescription("StorageClass name. Omit to use the cluster default StorageClass.")
    private String storageClassName;

    @Default("ReadWriteOnce")
    @JsonPropertyDescription("PVC access mode. Defaults to ReadWriteOnce.")
    private String accessMode = "ReadWriteOnce";

    @Default("/data")
    @JsonPropertyDescription("Mount path inside the container. Defaults to /data.")
    private String mountPath = "/data";

    @Default("data")
    @JsonPropertyDescription("Volume claim name. Defaults to data.")
    private String claimName = "data";

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public String getStorageClassName() { return storageClassName; }
    public void setStorageClassName(String s) { this.storageClassName = s; }
    public String getAccessMode() { return accessMode; }
    public void setAccessMode(String a) { this.accessMode = a; }
    public String getMountPath() { return mountPath; }
    public void setMountPath(String m) { this.mountPath = m; }
    public String getClaimName() { return claimName; }
    public void setClaimName(String c) { this.claimName = c; }
}
