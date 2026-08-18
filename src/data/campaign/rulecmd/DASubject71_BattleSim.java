package data.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import data.campaign.DASubject71BattleCreationPlugin;

import java.util.List;
import java.util.Map;

public class DASubject71_BattleSim extends BaseCommandPlugin {
    private static final float MAX_COMBAT_DP = 240f;
    private static final String SIMULATION_RUNNING_KEY = "$simulationRunning";

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        CampaignFleetAPI targetFleet = (CampaignFleetAPI) dialog.getInteractionTarget();
        float combatDp = getCombatDp(playerFleet);

        if (combatDp > MAX_COMBAT_DP) {
            String currentDp = formatDp(combatDp);
            String maximumDp = formatDp(MAX_COMBAT_DP);
            dialog.getTextPanel().addPara(
                    "Simulation access denied: combat fleet strength is " + currentDp
                            + " DP; the maximum is " + maximumDp
                            + " DP. Civilian, logistics, and mothballed ships are excluded from this total.",
                    Misc.getNegativeHighlightColor(),
                    currentDp + " DP",
                    maximumDp + " DP"
            );
            return false;
        }

        targetFleet.getMemoryWithoutUpdate().set(SIMULATION_RUNNING_KEY, true);
        String combatDpSummary = formatDp(combatDp) + "/" + formatDp(MAX_COMBAT_DP) + " DP";
        dialog.getTextPanel().addPara(
                "Entering simulation... Combat fleet strength: " + combatDpSummary + ".",
                Misc.getHighlightColor(),
                combatDpSummary
        );

        BattleCreationContext bcc = getBattleCreationContext(playerFleet, targetFleet);
        dialog.getVisualPanel().fadeVisualOut();
        DASubject71BattleCreationPlugin.prepareSimulationBattle(
                bcc,
                playerFleet.getContainingLocation()
        );
        try {
            dialog.startBattle(bcc);
        } catch (RuntimeException ex) {
            targetFleet.getMemoryWithoutUpdate().unset(SIMULATION_RUNNING_KEY);
            DASubject71BattleCreationPlugin.restoreSimulationBackground();
            throw ex;
        }
        return true;
    }

    private static float getCombatDp(CampaignFleetAPI playerFleet) {
        float totalDp = 0f;
        for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
            if (member.isCivilian() || member.isMothballed()) continue;
            totalDp += member.getDeploymentPointsCost();
        }
        return totalDp;
    }

    private static String formatDp(float dp) {
        if (dp == (int) dp) return Integer.toString((int) dp);
        return Float.toString(dp);
    }

    private static BattleCreationContext getBattleCreationContext(
            CampaignFleetAPI playerFleet,
            CampaignFleetAPI targetFleet
    ) {
        BattleCreationContext bcc = new BattleCreationContext(
                playerFleet,
                FleetGoal.ATTACK,
                targetFleet,
                FleetGoal.ATTACK
        );
        bcc.setPlayerCommandPoints((int) playerFleet.getCommanderStats().getCommandPoints().getModifiedValue());
        return bcc;
    }
}
