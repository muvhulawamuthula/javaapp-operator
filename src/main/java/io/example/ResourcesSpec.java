package io.example;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class ResourcesSpec {
    @JsonPropertyDescription("CPU request, e.g. 500m. Defaults to 250m.")
    private String requestCpu = "250m";
    @JsonPropertyDescription("Memory request, e.g. 256Mi. Defaults to 256Mi.")
    private String requestMemory = "256Mi";
    @JsonPropertyDescription("CPU limit. Defaults to 1.")
    private String limitCpu = "1";
    @JsonPropertyDescription("Memory limit. Defaults to 512Mi.")
    private String limitMemory = "512Mi";

    public String getRequestCpu() { return requestCpu; }
    public void setRequestCpu(String v) { this.requestCpu = v; }
    public String getRequestMemory() { return requestMemory; }
    public void setRequestMemory(String v) { this.requestMemory = v; }
    public String getLimitCpu() { return limitCpu; }
    public void setLimitCpu(String v) { this.limitCpu = v; }
    public String getLimitMemory() { return limitMemory; }
    public void setLimitMemory(String v) { this.limitMemory = v; }
}
