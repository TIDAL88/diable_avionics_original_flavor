package data.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import data.campaign.DANexVirtuousFleetInteractionDialogPluginImpl;
import data.campaign.DAVirtuousFleetInteractionDialogPluginImpl;
import data.campaign.ids.Diableavionics_ids;
import data.scripts.DAModPlugin;
import org.magiclib.bounty.ActiveBounty;
import org.magiclib.bounty.MagicBountyCoordinator;

import java.util.List;
import java.util.Map;

public class DASubject71_TransferVirtuous extends BaseCommandPlugin {
    private static final String PLAYER_VIRTUOUS_VARIANT_ID =
            "diableavionics_lastline_virtuous_player";
    private static final String SAVE_THE_CHILDREN_BOUNTY_KEY =
            "diable_virtuous";
    private static final String VIRTUOUS_CLAIMED_KEY = "$da_lastline_virtuous_claimed";
    private static final String SIMULATION_REWARD_CLAIMED_KEY = "$da_lastline_simulation_reward_claimed";

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        MemoryAPI sectorMemory = Global.getSector().getMemoryWithoutUpdate();
        if (sectorMemory.getBoolean(VIRTUOUS_CLAIMED_KEY)) {
            return false;
        }

        CampaignFleetAPI targetFleet = (CampaignFleetAPI) dialog.getInteractionTarget();
        FleetMemberAPI virtuousMember = targetFleet.getFleetData().getMemberWithCaptain(targetFleet.getCommander());
        if (virtuousMember == null) {
            return false;
        }

        targetFleet.getFleetData().removeOfficer(targetFleet.getCommander());
        virtuousMember.setCaptain(null);
        virtuousMember.setFlagship(false);
        virtuousMember.setVariant(
                Global.getSettings().getVariant(PLAYER_VIRTUOUS_VARIANT_ID).clone(),
                false,
                true
        );
        virtuousMember.getStats().getDynamic().getMod(Stats.INDIVIDUAL_SHIP_RECOVERY_MOD).unmodify(Diableavionics_ids.UNIQUE);
        virtuousMember.getVariant().removeTag(Tags.VARIANT_UNBOARDABLE);
        virtuousMember.getVariant().addTag(Tags.VARIANT_ALWAYS_RECOVERABLE);
        targetFleet.getFleetData().removeFleetMember(virtuousMember);
        targetFleet.getMemoryWithoutUpdate().unset("$virtuous");
        targetFleet.getFleetData().ensureHasFlagship();

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        playerFleet.getFleetData().addFleetMember(virtuousMember);
        sectorMemory.set(VIRTUOUS_CLAIMED_KEY, true);
        cancelSaveTheChildrenBounty();
        grantSimulationReward(targetFleet, playerFleet.getCargo());

        //reset battle so that the visual fleet also updates, showing no damage to the fleet.
        FleetEncounterContext context = (FleetEncounterContext) dialog.getPlugin().getContext();
        context.getBattle().leave(playerFleet, false);

        BattleAPI battle = Global.getFactory().createBattle(playerFleet, targetFleet);
        context.setBattle(battle);
        if (DAModPlugin.haveNexerelin) {
            DANexVirtuousFleetInteractionDialogPluginImpl plugin = (DANexVirtuousFleetInteractionDialogPluginImpl) dialog.getPlugin();
            plugin.pullFleets();
        } else if (dialog.getPlugin() instanceof DAVirtuousFleetInteractionDialogPluginImpl plugin) {
            plugin.pullFleets();
        }
        return true;
    }

    private void cancelSaveTheChildrenBounty() {
        MagicBountyCoordinator coordinator = MagicBountyCoordinator.getInstance();
        ActiveBounty bounty = coordinator.getActiveBounty(SAVE_THE_CHILDREN_BOUNTY_KEY);
        if (bounty == null) {
            return;
        }
        if (bounty.getStage().ordinal() > ActiveBounty.Stage.Accepted.ordinal()) {
            return;
        }

        bounty.endBounty(new ActiveBounty.BountyResult.EndedWithoutPlayerInvolvement());
        if (bounty.getIntel() != null) {
            bounty.getIntel().endImmediately();
        }
        coordinator.getActiveBounties();
        Global.getLogger(DASubject71_TransferVirtuous.class).info(
                "Cancelled MagicBounty " + SAVE_THE_CHILDREN_BOUNTY_KEY
                        + " because the Virtuous was claimed through simulation"
        );
    }

    private void grantSimulationReward(CampaignFleetAPI targetFleet, CargoAPI playerCargo) {
        if (targetFleet.getMemoryWithoutUpdate().getBoolean(SIMULATION_REWARD_CLAIMED_KEY)) {
            return;
        }

        playerCargo.addSpecial(new SpecialItemData(Items.SHIP_BP, "diableavionics_maelstrom"), 1);
        targetFleet.getMemoryWithoutUpdate().set(SIMULATION_REWARD_CLAIMED_KEY, true);
    }

}
