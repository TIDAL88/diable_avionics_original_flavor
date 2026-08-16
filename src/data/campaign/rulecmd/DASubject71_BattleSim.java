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
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class DASubject71_BattleSim extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        BattleCreationContext bcc = getBattleCreationContext(dialog);
        bcc.enemyDeployAll = true;
        CampaignFleetAPI player=Global.getSector().getPlayerFleet();
        float total_dp=0f;
        for (FleetMemberAPI member: player.getFleetData().getMembersListCopy()){
            if (member.isCivilian()) continue;
            total_dp+=member.getDeploymentPointsCost();
        }
        if (total_dp>=240f) return false;
        dialog.getVisualPanel().fadeVisualOut();
        DASubject71BattleCreationPlugin.prepareSimulationBackground(
                Global.getSector().getPlayerFleet().getContainingLocation()
        );
        try {
            dialog.startBattle(bcc);
        } catch (RuntimeException ex) {
            DASubject71BattleCreationPlugin.restoreSimulationBackground();
            throw ex;
        }
        return true;
    }

    @NotNull
    private static BattleCreationContext getBattleCreationContext(InteractionDialogAPI dialog) {
        CampaignFleetAPI targetFleet = (CampaignFleetAPI) dialog.getInteractionTarget();
        BattleCreationContext bcc = new BattleCreationContext(Global.getSector().getPlayerFleet(), FleetGoal.ATTACK, targetFleet, FleetGoal.ATTACK);
        bcc.setPlayerCommandPoints((int) Global.getSector().getPlayerFleet().getCommanderStats().getCommandPoints().getModifiedValue());
        return bcc;
    }
}
