package data.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.AdmiralAIPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.mission.FleetSide;
import data.scripts.campaign.lastline.DiableLastLineFleetFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Guarantees that the authored key ships are selected before the enemy admiral
 * fills the rest of The Last Line's normal initial deployment batch.
 */
public final class DALastLineInitialDeploymentPlugin
        implements AdmiralAIPlugin.AdmiralPluginDelegate {

    private static final Set<String> REQUIRED_VARIANT_IDS =
            Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
                    DiableLastLineFleetFactory.VIRTUOUS_VARIANT_ID,
                    "diableavionics_lastline_maelstrom",
                    "diableavionics_lastline_vapor",
                    "diableavionics_lastline_draft"
            )));

    private final Set<FleetMemberAPI> requiredMembers =
            Collections.newSetFromMap(
                    new IdentityHashMap<FleetMemberAPI, Boolean>()
            );

    private boolean initialDeploymentFinished;

    private DALastLineInitialDeploymentPlugin() {
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
                new DALastLineInitialDeploymentPlugin();
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
        initialDeploymentFinished = true;
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
}
