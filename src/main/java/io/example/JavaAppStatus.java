package io.example;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.fabric8.crd.generator.annotation.PrinterColumn;
import io.fabric8.kubernetes.api.model.Condition;
import io.fabric8.kubernetes.api.model.ConditionBuilder;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JavaAppStatus {
    @PrinterColumn(name = "Phase")
    @JsonPropertyDescription("Pending | Running | Upgrading | Degraded | Terminating")
    private String phase = "Pending";
    @PrinterColumn(name = "Ready")
    @JsonPropertyDescription("Number of ready pods.")
    private int readyReplicas;
    @JsonPropertyDescription("Number of updated pods.")
    private int updatedReplicas;
    @PrinterColumn(name = "Version")
    @JsonPropertyDescription("Application version currently running.")
    private String currentVersion;
    @JsonPropertyDescription("Kubernetes-style status conditions.")
    private List<Condition> conditions = new ArrayList<>();
    @JsonPropertyDescription("ISO-8601 timestamp of the last reconciliation.")
    private String lastReconcileTime;

    public void setCondition(String type, boolean ready, String reason, String message) {
        String sv = ready ? "True" : "False";
        String now = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        Condition ex = conditions.stream().filter(c -> c.getType().equals(type)).findFirst().orElse(null);
        String lt = (ex != null && Objects.equals(ex.getStatus(), sv)) ? ex.getLastTransitionTime() : now;
        conditions.removeIf(c -> c.getType().equals(type));
        conditions.add(new ConditionBuilder().withType(type).withStatus(sv)
            .withReason(reason).withMessage(message).withLastTransitionTime(lt).build());
    }

    public void markReady(int ready, int desired) {
        if (ready >= desired) {
            setCondition("Ready", true, "ReplicaSetReady", String.format("All %d replica(s) ready.", desired));
            setPhase("Running");
        } else {
            setCondition("Ready", false, "InsufficientReplicas", String.format("%d of %d ready.", ready, desired));
            setPhase("Pending");
        }
    }

    public void touchReconcileTime() {
        this.lastReconcileTime = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }
    public int getReadyReplicas() { return readyReplicas; }
    public void setReadyReplicas(int r) { this.readyReplicas = r; }
    public int getUpdatedReplicas() { return updatedReplicas; }
    public void setUpdatedReplicas(int r) { this.updatedReplicas = r; }
    public String getCurrentVersion() { return currentVersion; }
    public void setCurrentVersion(String v) { this.currentVersion = v; }
    public List<Condition> getConditions() { return conditions; }
    public void setConditions(List<Condition> c) { this.conditions = c; }
    public String getLastReconcileTime() { return lastReconcileTime; }
    public void setLastReconcileTime(String t) { this.lastReconcileTime = t; }
}
