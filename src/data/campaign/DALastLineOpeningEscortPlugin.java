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
 * Leashes the Last Line Virtuous to a deployed Coanda, falling back to a Gust,
 * for the opening of combat, then holds it on Search and Destroy. The timer
 * begins with the first enemy deployment and advances only while combat is
 * unpaused.
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
    private boolean searchAndDestroyStarted;
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

        if (elapsed >= LEASH_DURATION) {
            maintainSearchAndDestroyAssignment(virtuous);
            return;
        }

        DeployedFleetMemberAPI target = findPreferredEscortTarget(virtuous);
        if (target == null) {
            removeEscortAssignment(false);
            return;
        }

        maintainEscortAssignment(virtuous, target);
    }

    private void maintainSearchAndDestroyAssignment(
            DeployedFleetMemberAPI virtuous
    ) {
        CombatFleetManagerAPI.AssignmentInfo current =
                taskManager.getAssignmentFor(virtuous.getShip());
        boolean searchAndDestroyIsCurrent = current != null
                && current.getType() == CombatAssignmentType.SEARCH_AND_DESTROY;

        if (searchAndDestroyIsCurrent) return;

        removeEscortAssignment(false);
        taskManager.orderSearchAndDestroy(virtuous, false);

        if (!searchAndDestroyStarted) {
            searchAndDestroyStarted = true;
            Global.getLogger(DALastLineOpeningEscortPlugin.class).info(
                    "Ended Last Line opening escort after 35 unpaused seconds; "
                            + "holding Virtuous on Search and Destroy"
            );
        } else {
            Global.getLogger(DALastLineOpeningEscortPlugin.class).info(
                    "Virtuous Search and Destroy was replaced by "
                            + describeAssignment(current)
                            + "; reapplying Search and Destroy"
            );
        }
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

    private DeployedFleetMemberAPI findPreferredEscortTarget(
            DeployedFleetMemberAPI virtuous
    ) {
        if (isValidEscortTarget(escortTarget, virtuous)) {
            return escortTarget;
        }

        DeployedFleetMemberAPI gust = null;

        for (DeployedFleetMemberAPI deployed
                : fleetManager.getDeployedCopyDFM()) {
            if (deployed == virtuous || !isActiveShip(deployed)) continue;

            FleetMemberAPI member = deployed.getMember();
            if (member == null) continue;

            String hullId = member.getHullId();
            if ("diableavionics_coanda".equals(hullId)) {
                return deployed;
            }
            if (gust == null && "diableavionics_gust".equals(hullId)) {
                gust = deployed;
            }
        }
        return gust;
    }

    private boolean isValidEscortTarget(
            DeployedFleetMemberAPI target,
            DeployedFleetMemberAPI virtuous
    ) {
        if (target == null || target == virtuous || !isActiveShip(target)) {
            return false;
        }

        FleetMemberAPI member = target.getMember();
        if (member == null) return false;

        String hullId = member.getHullId();
        return "diableavionics_coanda".equals(hullId)
                || "diableavionics_gust".equals(hullId);
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
        if (engine != null) {
            engine.removePlugin(this);
        }
    }
}
