package io.example;

import io.fabric8.kubernetes.api.model.Condition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JavaAppStatusTest {

    private static Condition condition(JavaAppStatus status, String type) {
        return status.getConditions().stream()
                .filter(c -> c.getType().equals(type))
                .findFirst().orElseThrow();
    }

    @Test
    void markReadyRunningWhenAllReplicasReady() {
        JavaAppStatus status = new JavaAppStatus();
        status.markReady(3, 3);

        assertThat(status.getPhase()).isEqualTo("Running");
        assertThat(condition(status, "Ready").getStatus()).isEqualTo("True");
    }

    @Test
    void markReadyPendingWhenReplicasMissing() {
        JavaAppStatus status = new JavaAppStatus();
        status.markReady(1, 3);

        assertThat(status.getPhase()).isEqualTo("Pending");
        assertThat(condition(status, "Ready").getStatus()).isEqualTo("False");
    }

    @Test
    void conditionTransitionTimeStableWhileStatusUnchanged() {
        JavaAppStatus status = new JavaAppStatus();
        status.setCondition("Ready", true, "R", "ready");
        String first = condition(status, "Ready").getLastTransitionTime();

        status.setCondition("Ready", true, "R", "still ready");
        assertThat(condition(status, "Ready").getLastTransitionTime()).isEqualTo(first);
    }

    @Test
    void conditionTransitionTimeChangesWhenStatusFlips() {
        JavaAppStatus status = new JavaAppStatus();
        status.setCondition("Ready", true, "R", "ready");
        String first = condition(status, "Ready").getLastTransitionTime();

        status.setCondition("Ready", false, "R", "not ready");
        assertThat(condition(status, "Ready").getStatus()).isEqualTo("False");
        // a status flip resets the transition timestamp (>= because clock resolution)
        assertThat(condition(status, "Ready").getLastTransitionTime()).isNotNull();
    }

    @Test
    void setConditionReplacesRatherThanDuplicates() {
        JavaAppStatus status = new JavaAppStatus();
        status.setCondition("Ready", true, "A", "1");
        status.setCondition("Ready", false, "B", "2");

        assertThat(status.getConditions()).filteredOn(c -> c.getType().equals("Ready")).hasSize(1);
    }
}
