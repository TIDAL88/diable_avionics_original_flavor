package data.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatAssignmentType;
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
 * Leashes the Last Line Virtuous to the second-highest-DP deployed allied ship for
 * the opening of combat. The timer begins with the first enemy deployment and
 * advances only while combat is unpaused.
 */
public final class DALastLineOpeningEscortPlugin
        extends BaseEveryFrameCombatPlugin {

    private static final float LEASH_DURATION = 35f;
    private static final float ORDER_CHECK_INTERVAL = 0.5f;

    private final IntervalUtil orderCheck = new IntervalUtil(
            ORDER_CHECK_INTERVAL,
            ORDER_CHECK_INTERVAL
    );

    private CombatEngineAPI engine;
    private CombatFleetManagerAPI fleetManager;
    private CombatTaskManagerAPI taskManager;
    private CombatFleetManagerAPI.AssignmentInfo escortAssignment;
    private DeployedFleetMemberAPI escortTarget;

    private float elapsed;
    private boolean timerStarted;
    private boolean virtuousWasDeployed;
    private boolean firstOrderCheck = true;
    private boolean finished;

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        fleetManager = engine.getFleetManager(FleetSide.ENEMY);
        if (fleetManager != null) {
            taskManager = fleetManager.getTaskManager(false);
        }

        if (fleetManager == null || taskManager == null) {
            Global.getLogger(DALastLineOpeningEscortPlugin.class).warn(
                    "Unable to install Last Line opening escort: "
                            + "enemy fleet or task manager is unavailable"
            );
            finish(false);
        }
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (finished || engine == null) return;

        if (engine.isCombatOver()) {
            finish(false);
            return;
        }
        if (engine.isPaused()) return;

        if (!timerStarted) {
            if (!hasDeployedShip()) return;
            timerStarted = true;
            Global.getLogger(DALastLineOpeningEscortPlugin.class).info(
                    "Started Last Line 35-second opening escort window"
            );
        }

        elapsed += amount;
        if (elapsed >= LEASH_DURATION) {
            finish(true);
            return;
        }

        orderCheck.advance(amount);
        if (!firstOrderCheck && !orderCheck.intervalElapsed()) return;
        firstOrderCheck = false;

        DeployedFleetMemberAPI virtuous = findDeployedVirtuous();
        if (virtuous == null) {
            if (virtuousWasDeployed) {
                finish(false);
            }
            return;
        }
        virtuousWasDeployed = true;

        DeployedFleetMemberAPI target = findSecondHighestDpTarget(virtuous);
        if (target == null) {
            removeEscortAssignment(false);
            return;
        }

        maintainEscortAssignment(virtuous, target);
    }

    private boolean hasDeployedShip() {
        for (DeployedFleetMemberAPI deployed
                : fleetManager.getDeployedCopyDFM()) {
            if (isActiveShip(deployed)) return true;
        }
        return false;
    }

    private DeployedFleetMemberAPI findDeployedVirtuous() {
        for (DeployedFleetMemberAPI deployed
                : fleetManager.getDeployedCopyDFM()) {
            if (!isActiveShip(deployed)) continue;
            if (isVirtuous(deployed.getMember())) return deployed;
        }
        return null;
    }

    private DeployedFleetMemberAPI findSecondHighestDpTarget(
            DeployedFleetMemberAPI virtuous
    ) {
        DeployedFleetMemberAPI highest = null;
        DeployedFleetMemberAPI secondHighest = null;
        float highestDp = -1f;
        float secondHighestDp = -1f;

        for (DeployedFleetMemberAPI deployed
                : fleetManager.getDeployedCopyDFM()) {
            if (deployed == virtuous || !isActiveShip(deployed)) continue;

            FleetMemberAPI member = deployed.getMember();
            if (member == null) continue;

            float dp = member.getDeploymentPointsCost();
            if (dp > highestDp) {
                secondHighest = highest;
                secondHighestDp = highestDp;
                highest = deployed;
                highestDp = dp;
            } else if (secondHighest == null
                    || dp > secondHighestDp
                    || dp == secondHighestDp && deployed == escortTarget) {
                secondHighest = deployed;
                secondHighestDp = dp;
            }
        }
        return secondHighest;
    }

    private void maintainEscortAssignment(
            DeployedFleetMemberAPI virtuous,
            DeployedFleetMemberAPI target
    ) {
        CombatFleetManagerAPI.AssignmentInfo current =
                taskManager.getAssignmentFor(virtuous.getShip());
        boolean targetChanged = target != escortTarget;
        boolean escortOrderIsCurrent = current != null
                && current.getType() == CombatAssignmentType.HEAVY_ESCORT
                && current.getTarget() == target;

        if (escortOrderIsCurrent && !targetChanged) {
            // Keep the live task-manager reference so cleanup removes the
            // actual active escort task even if Vanilla replaced its object.
            escortAssignment = current;
            return;
        }

        if (escortAssignment != null && !targetChanged) {
            Global.getLogger(DALastLineOpeningEscortPlugin.class).info(
                    "Virtuous opening escort was replaced by "
                            + describeAssignment(current)
                            + "; reapplying escort to "
                            + target.getMember().getShipName()
            );
        }

        removeEscortAssignment(false);
        escortAssignment = taskManager.createAssignment(
                CombatAssignmentType.HEAVY_ESCORT,
                target,
                false
        );
        taskManager.setAssignmentWeight(escortAssignment, 0f);
        taskManager.giveAssignment(virtuous, escortAssignment, false);
        escortTarget = target;

        Global.getLogger(DALastLineOpeningEscortPlugin.class).info(
                "Applied Virtuous opening escort to "
                        + target.getMember().getShipName()
                        + " ("
                        + target.getMember().getDeploymentPointsCost()
                        + " DP)"
        );
    }

    private String describeAssignment(
            CombatFleetManagerAPI.AssignmentInfo assignment
    ) {
        if (assignment == null) return "no task";
        return assignment.getType().toString();
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

    private void removeEscortAssignment(boolean reassign) {
        if (escortAssignment != null && taskManager != null) {
            taskManager.removeAssignment(escortAssignment);
            escortAssignment = null;
            escortTarget = null;
            if (reassign) {
                taskManager.reassign();
            }
        }
    }

    private void finish(boolean windowExpired) {
        if (finished) return;
        finished = true;

        removeEscortAssignment(windowExpired);
        if (windowExpired) {
            Global.getLogger(DALastLineOpeningEscortPlugin.class).info(
                    "Ended Last Line opening escort after 35 unpaused seconds"
            );
        }
        if (engine != null) {
            engine.removePlugin(this);
        }
    }
}
