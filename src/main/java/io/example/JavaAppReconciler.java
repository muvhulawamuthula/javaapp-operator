package io.example;

import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetStatus;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Reconciler for the {@code JavaApp} custom resource.
 *
 * <p>The desired StatefulSet / Service / ConfigMap are managed declaratively by the
 * {@link Workflow} dependents below; this class is responsible for the <em>lifecycle state machine</em>:
 * computing the observed phase, gating version upgrades on readiness, and reporting Kubernetes-style
 * status conditions so the resource is observable via {@code kubectl get javaapps}.
 */
@Workflow(dependents = {
    @Dependent(type = JavaAppConfigMap.class, name = "config"),
    @Dependent(type = JavaAppStatefulSet.class, name = "statefulset", dependsOn = "config"),
    @Dependent(type = JavaAppService.class, name = "service")
})
@ControllerConfiguration(
    maxReconciliationInterval = @MaxReconciliationInterval(interval = 10, timeUnit = TimeUnit.MINUTES)
)
@ApplicationScoped
public class JavaAppReconciler implements Reconciler<JavaApp>, Cleaner<JavaApp> {

    private static final Logger LOG = Logger.getLogger(JavaAppReconciler.class);

    @Override
    public UpdateControl<JavaApp> reconcile(JavaApp app, Context<JavaApp> ctx) {
        LOG.infof("Reconciling %s", app);
        var spec   = app.getSpec();
        var status = app.getOrInitStatus();

        Optional<StatefulSetStatus> stsStatus = ctx.getSecondaryResource(StatefulSet.class)
                .map(StatefulSet::getStatus);
        int ready   = stsStatus.map(s -> orZero(s.getReadyReplicas())).orElse(0);
        int updated = stsStatus.map(s -> orZero(s.getUpdatedReplicas())).orElse(0);
        int desired = spec.isHpaManaged() ? Math.max(ready, 1) : spec.getReplicas();

        status.setReadyReplicas(ready);
        status.setUpdatedReplicas(updated);

        boolean upgradeRequested = spec.getVersion() != null
                && !spec.getVersion().equals(status.getCurrentVersion());
        // The rollout is complete once every pod is ready AND on the latest revision.
        boolean rolloutComplete = ready >= desired && updated >= desired;

        if (upgradeRequested && !rolloutComplete) {
            status.setPhase("Upgrading");
            status.setCondition("Progressing", true, "UpgradeInProgress",
                    String.format("Rolling out version %s (%d/%d updated, %d/%d ready)",
                            spec.getVersion(), updated, desired, ready, desired));
            status.setCondition("Ready", ready >= desired, "RolloutInProgress",
                    String.format("%d of %d replica(s) ready during upgrade.", ready, desired));
        } else {
            status.setCondition("Progressing", false, "UpToDate", "No upgrade in progress.");
            status.markReady(ready, desired);
            if (spec.getVersion() != null && rolloutComplete) {
                status.setCurrentVersion(spec.getVersion());
            }
        }

        status.touchReconcileTime();
        return UpdateControl.patchStatus(app);
    }

    /**
     * Invoked by JOSDK when reconciliation (including dependent workflow execution) throws.
     * Surfaces the failure on the resource's status rather than only in the operator logs.
     */
    @Override
    public ErrorStatusUpdateControl<JavaApp> updateErrorStatus(JavaApp app, Context<JavaApp> ctx, Exception e) {
        LOG.errorf(e, "Error reconciling %s", app.getMetadata().getName());
        var status = app.getOrInitStatus();
        status.setPhase("Degraded");
        status.setCondition("Ready", false, "ReconcileError",
                e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        status.touchReconcileTime();
        return ErrorStatusUpdateControl.patchStatus(app);
    }

    @Override
    public DeleteControl cleanup(JavaApp app, Context<JavaApp> ctx) {
        LOG.infof("Cleanup started for %s", app.getMetadata().getName());
        Optional<StatefulSet> maybeSts = ctx.getSecondaryResource(StatefulSet.class);
        if (maybeSts.isEmpty()) return DeleteControl.defaultDelete();
        int ready = Optional.ofNullable(maybeSts.get().getStatus())
                            .map(s -> orZero(s.getReadyReplicas())).orElse(0);
        if (ready > 0) {
            LOG.infof("Waiting for %d pod(s) to terminate — rescheduling in 10s", ready);
            app.getOrInitStatus().setPhase("Terminating");
            return DeleteControl.noFinalizerRemoval().rescheduleAfter(10, TimeUnit.SECONDS);
        }
        return DeleteControl.defaultDelete();
    }

    private static int orZero(Integer v) { return v == null ? 0 : v; }
}
