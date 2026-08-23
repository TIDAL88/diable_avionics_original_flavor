package data.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.AssignmentTargetAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.CombatTaskManagerAPI;
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.campaign.lastline.DiableLastLineFleetFactory;

import java.util.List;

/**
 * Temporary diagnostic logger for tracking Virtuous combat assignments.
 * Remove once the opening escort behavior is verified.
 */
public final class DALastLineVirtuousOrderMonitorPlugin
        extends BaseEveryFrameCombatPlugin {

    private static final float CHECK_INTERVAL = 0.25f;

    private final IntervalUtil orderCheck = new IntervalUtil(
            CHECK_INTERVAL,
            CHECK_INTERVAL
    );

    private CombatEngineAPI engine;
    private CombatFleetManagerAPI fleetManager;
    private CombatTaskManagerAPI taskManager;

    private String lastAssignmentKey;
    private boolean virtuousWasDeployed;
    private boolean finished;
    private float elapsed;

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        fleetManager = engine.getFleetManager(FleetSide.ENEMY);
        if (fleetManager != null) {
            taskManager = fleetManager.getTaskManager(false);
        }

        if (fleetManager == null || taskManager == null) {
            Global.getLogger(DALastLineVirtuousOrderMonitorPlugin.class).warn(
                    "Unable to install Virtuous order monitor: "
                            + "enemy fleet or task manager is unavailable"
            );
            finish();
            return;
        }

        Global.getLogger(DALastLineVirtuousOrderMonitorPlugin.class).info(
                "Started Virtuous order monitor"
        );
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (finished || engine == null) return;

        if (engine.isCombatOver()) {
            Global.getLogger(DALastLineVirtuousOrderMonitorPlugin.class).info(
                    "Ended Virtuous order monitor: combat over"
            );
            finish();
            return;
        }
        if (engine.isPaused()) return;

        elapsed += amount;
        orderCheck.advance(amount);
        if (!orderCheck.intervalElapsed()) return;

        DeployedFleetMemberAPI virtuous = findDeployedVirtuous();
        if (virtuous == null) {
            if (virtuousWasDeployed) {
                Global.getLogger(
                        DALastLineVirtuousOrderMonitorPlugin.class
                ).info(
                        "Virtuous order monitor: Virtuous no longer active at "
                                + formatTime(elapsed)
                );
                finish();
            }
            return;
        }

        if (!virtuousWasDeployed) {
            virtuousWasDeployed = true;
            Global.getLogger(DALastLineVirtuousOrderMonitorPlugin.class).info(
                    "Virtuous order monitor: Virtuous deployed at "
                            + formatTime(elapsed)
            );
        }

        CombatFleetManagerAPI.AssignmentInfo assignment =
                taskManager.getAssignmentFor(virtuous.getShip());
        String assignmentKey = describeAssignment(assignment);
        if (assignmentKey.equals(lastAssignmentKey)) return;

        lastAssignmentKey = assignmentKey;
        Global.getLogger(DALastLineVirtuousOrderMonitorPlugin.class).info(
                "Virtuous order monitor at "
                        + formatTime(elapsed)
                        + ": "
                        + assignmentKey
        );
    }

    private DeployedFleetMemberAPI findDeployedVirtuous() {
        for (DeployedFleetMemberAPI deployed
                : fleetManager.getDeployedCopyDFM()) {
            if (!isActiveShip(deployed)) continue;
            if (isVirtuous(deployed.getMember())) return deployed;
        }
        return null;
    }

    private boolean isActiveShip(DeployedFleetMemberAPI deployed) {
        if (deployed == null
                || deployed.isFighterWing()
                || deployed.isStationModule()) {
            return false;
        }

        ShipAPI ship = deployed.getShip();
        return ship != null
                && ship.isAlive()
                && !ship.isHulk()
                && !ship.isRetreating();
    }

    private boolean isVirtuous(FleetMemberAPI member) {
        if (member == null) return false;

        ShipVariantAPI variant = member.getVariant();
        if (variant != null
                && DiableLastLineFleetFactory.VIRTUOUS_VARIANT_ID.equals(
                variant.getHullVariantId())) {
            return true;
        }

        return member.isFlagship()
                && member.getHullSpec() != null
                && member.getHullSpec().getBaseHullId() != null
                && member.getHullSpec().getBaseHullId().startsWith(
                "diableavionics_virtuous"
        );
    }

    private String describeAssignment(
            CombatFleetManagerAPI.AssignmentInfo assignment
    ) {
        if (assignment == null) return "no assignment";

        AssignmentTargetAPI target = assignment.getTarget();
        String targetDescription = "no target";
        if (target instanceof DeployedFleetMemberAPI
                && ((DeployedFleetMemberAPI) target).getMember() != null) {
            DeployedFleetMemberAPI deployedTarget =
                    (DeployedFleetMemberAPI) target;
            targetDescription =
                    deployedTarget.getMember().getShipName()
                            + " ("
                            + deployedTarget.getMember()
                                    .getDeploymentPointsCost()
                            + " DP)";
        } else if (target != null) {
            targetDescription = target.toString();
        }

        return assignment.getType() + " -> " + targetDescription;
    }

    private String formatTime(float time) {
        return String.format("%.1fs", time);
    }

    private void finish() {
        if (finished) return;
        finished = true;
        if (engine != null) {
            engine.removePlugin(this);
        }
    }
}
