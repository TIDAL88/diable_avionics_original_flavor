package data.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.AdmiralAIPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.mission.FleetSide;
import data.scripts.campaign.lastline.DiableLastLineFleetFactory;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Guarantees that the authored key ships are selected before the enemy admiral
 * fills the rest of The Last Line's normal initial deployment batch, then
 * deploys any ships the admiral left in reserve.
 */
public final class DALastLineInitialDeploymentPlugin
        implements AdmiralAIPlugin.AdmiralPluginDelegate {

    private static final float SPAWN_FACING = 270f;
    private static final float SPAWN_DELAY = 3f;
    private static final float SPAWN_PADDING = 150f;
    private static final float MAP_EDGE_PADDING = 100f;

    private static final Set<String> REQUIRED_VARIANT_IDS =
            Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
                    DiableLastLineFleetFactory.VIRTUOUS_VARIANT_ID,
                    "diableavionics_lastline_maelstrom",
                    "diableavionics_lastline_vapor"
            )));

    private final Set<FleetMemberAPI> requiredMembers =
            Collections.newSetFromMap(
                    new IdentityHashMap<FleetMemberAPI, Boolean>()
            );
    private final CombatEngineAPI engine;
    private final CombatFleetManagerAPI enemyFleetManager;

    private boolean initialDeploymentFinished;

    private DALastLineInitialDeploymentPlugin(
            CombatEngineAPI engine,
            CombatFleetManagerAPI enemyFleetManager
    ) {
        this.engine = engine;
        this.enemyFleetManager = enemyFleetManager;
    }

    public static void install(CombatEngineAPI engine) {
        CombatFleetManagerAPI enemyFleetManager =
                engine.getFleetManager(FleetSide.ENEMY);
        if (enemyFleetManager == null
                || enemyFleetManager.getAdmiralAI() == null) {
            Global.getLogger(DALastLineInitialDeploymentPlugin.class).warn(
                    "Unable to install The Last Line initial deployment gate: "
                            + "enemy admiral AI is unavailable"
            );
            return;
        }

        DALastLineInitialDeploymentPlugin delegate =
                new DALastLineInitialDeploymentPlugin(
                        engine,
                        enemyFleetManager
                );
        Set<String> foundVariantIds = new HashSet<String>();
        for (FleetMemberAPI member : enemyFleetManager.getDeployedCopy()) {
            delegate.addRequiredMember(member, foundVariantIds, false);
        }
        for (FleetMemberAPI member : enemyFleetManager.getReservesCopy()) {
            delegate.addRequiredMember(member, foundVariantIds, true);
        }

        if (!foundVariantIds.containsAll(REQUIRED_VARIANT_IDS)) {
            Set<String> missing = new HashSet<String>(REQUIRED_VARIANT_IDS);
            missing.removeAll(foundVariantIds);
            Global.getLogger(DALastLineInitialDeploymentPlugin.class).warn(
                    "The Last Line initial deployment is missing required variants: "
                            + missing
            );
        }

        if (!delegate.requiredMembers.isEmpty()) {
            enemyFleetManager.getAdmiralAI().setDelegate(delegate);
            Global.getLogger(DALastLineInitialDeploymentPlugin.class).info(
                    "Installed The Last Line initial deployment gate for "
                            + delegate.requiredMembers.size() + " key ships"
            );
        }
    }

    @Override
    public boolean allowedToDeploy(
            List<FleetMemberAPI> chosenSoFar,
            FleetMemberAPI candidate
    ) {
        if (initialDeploymentFinished || requiredMembers.isEmpty()) {
            return true;
        }
        if (requiredMembers.contains(candidate)) {
            return true;
        }
        return chosenSoFar != null
                && chosenSoFar.containsAll(requiredMembers);
    }

    @Override
    public void doAdditionalInitialDeployment() {
        if (initialDeploymentFinished) return;

        try {
            List<ShipAPI> forcedShips = new ArrayList<ShipAPI>();
            List<String> forcedVariantIds = new ArrayList<String>();
            List<FleetMemberAPI> reserves =
                    enemyFleetManager.getReservesCopy();

            for (FleetMemberAPI member : reserves) {
                if (enemyFleetManager.getShipFor(member) != null) continue;

                String variantId = getVariantId(member);
                if (!reserves.contains(member)) {
                    Global.getLogger(
                            DALastLineInitialDeploymentPlugin.class
                    ).warn(
                            "Unable to force-deploy remaining Last Line ship "
                                    + variantId + ": member is not in reserves"
                    );
                    continue;
                }

                Vector2f initialLocation = new Vector2f(
                        0f,
                        engine.getMapHeight() / 2f - MAP_EDGE_PADDING
                );
                ShipAPI spawned = enemyFleetManager.spawnFleetMember(
                        member,
                        initialLocation,
                        SPAWN_FACING,
                        SPAWN_DELAY
                );

                if (spawned != null) {
                    enemyFleetManager.removeFromReserves(member);
                    forcedShips.add(spawned);
                    forcedVariantIds.add(variantId);
                } else {
                    Global.getLogger(
                            DALastLineInitialDeploymentPlugin.class
                    ).warn(
                            "Unable to force-deploy remaining Last Line ship "
                                    + variantId + ": spawn returned null"
                    );
                }
            }

            moveToSafeSpawnLocations(forcedShips);
            if (!forcedVariantIds.isEmpty()) {
                Global.getLogger(
                        DALastLineInitialDeploymentPlugin.class
                ).info(
                        "Force-deployed remaining Last Line initial ships: "
                                + forcedVariantIds
                );
            }
        } finally {
            // Only force the initial reserve once. Later reinforcements should
            // remain under Vanilla's normal battle logic.
            initialDeploymentFinished = true;
        }
    }

    private void moveToSafeSpawnLocations(List<ShipAPI> ships) {
        for (ShipAPI ship : ships) {
            Vector2f safeLocation = findSafeSpawnLocation(ship);
            if (safeLocation != null) {
                ship.getLocation().set(safeLocation.x, safeLocation.y);
            } else {
                Global.getLogger(
                        DALastLineInitialDeploymentPlugin.class
                ).warn(
                        "Unable to find collision-free initial position for "
                                + ship.getName()
                );
            }
        }
    }

    private Vector2f findSafeSpawnLocation(ShipAPI ship) {
        float radius = Math.max(50f, ship.getCollisionRadius());
        float halfWidth = engine.getMapWidth() / 2f;
        float halfHeight = engine.getMapHeight() / 2f;
        float minX = -halfWidth + radius + MAP_EDGE_PADDING;
        float maxX = halfWidth - radius - MAP_EDGE_PADDING;
        float minY = -halfHeight + radius + MAP_EDGE_PADDING;
        float maxY = halfHeight - radius - MAP_EDGE_PADDING;
        float step = radius * 2f + SPAWN_PADDING;

        for (float y = maxY; y >= minY; y -= step) {
            for (float x = minX; x <= maxX; x += step) {
                Vector2f candidate = new Vector2f(x, y);
                if (isCollisionFree(ship, candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private boolean isCollisionFree(ShipAPI ship, Vector2f candidate) {
        for (ShipAPI other : engine.getShips()) {
            if (other == ship || !other.isAlive()) continue;

            float clearance = ship.getCollisionRadius()
                    + other.getCollisionRadius()
                    + SPAWN_PADDING;
            float dx = candidate.x - other.getLocation().x;
            float dy = candidate.y - other.getLocation().y;
            if (dx * dx + dy * dy < clearance * clearance) {
                return false;
            }
        }
        return true;
    }

    private void addRequiredMember(
            FleetMemberAPI member,
            Set<String> foundVariantIds,
            boolean requireSelection
    ) {
        String variantId = getRequiredVariantId(member);
        if (variantId == null) return;

        foundVariantIds.add(variantId);
        if (requireSelection) {
            requiredMembers.add(member);
        }
    }

    private String getRequiredVariantId(FleetMemberAPI member) {
        if (member == null) return null;

        ShipVariantAPI variant = member.getVariant();
        if (variant != null) {
            String variantId = variant.getHullVariantId();
            if (REQUIRED_VARIANT_IDS.contains(variantId)) {
                return variantId;
            }
        }

        // Protect the Virtuous requirement if its mutable combat variant gets
        // a generated ID, without mistaking a nearby fleet's flagship for it.
        if (member.isFlagship()
                && member.getHullSpec() != null
                && member.getHullSpec().getBaseHullId()
                .startsWith("diableavionics_virtuous")) {
            return DiableLastLineFleetFactory.VIRTUOUS_VARIANT_ID;
        }
        return null;
    }

    private String getVariantId(FleetMemberAPI member) {
        if (member == null) return "unknown";

        ShipVariantAPI variant = member.getVariant();
        if (variant != null && variant.getHullVariantId() != null) {
            return variant.getHullVariantId();
        }
        if (member.getHullSpec() != null) {
            return member.getHullSpec().getHullId();
        }
        return "unknown";
    }
}
