package data.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.campaign.BattleCreationPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import data.scripts.campaign.lastline.DiableLastLineFleetFactory;
import data.scripts.world.systems.Diableavionics_blackSite;

public class DACampaignPlugin extends BaseCampaignPlugin {
    @Override
    public PluginPick<BattleCreationPlugin> pickBattleCreationPlugin(
            SectorEntityToken opponent
    ) {
        if (opponent instanceof CampaignFleetAPI) {
            CampaignFleetAPI fleet = (CampaignFleetAPI) opponent;
            boolean isNormalLastLineSimulation =
                    hasMemoryInFleet(fleet, "$virtuous")
                    && hasMemoryInFleet(fleet, "$simulationRunning")
                    && fleet.getMemoryWithoutUpdate().contains(
                            DiableLastLineFleetFactory.FLEET_VERSION_MEMKEY
                    );
            if (isNormalLastLineSimulation) {
                return new PluginPick<BattleCreationPlugin>(
                        new DASubject71BattleCreationPlugin(),
                        PickPriority.HIGHEST
                );
            }
        }
        return super.pickBattleCreationPlugin(opponent);
    }

    @Override
    public PluginPick<InteractionDialogPlugin> pickInteractionDialogPlugin(SectorEntityToken interactionTarget) {
        if (Diableavionics_blackSite.RUPTURED_GATE_ID.equals(interactionTarget.getId())) {
            return new PluginPick<InteractionDialogPlugin>(
                    new DiableBlacksiteGateInteractionPlugin(),
                    PickPriority.MOD_SPECIFIC
            );
        }
        if (interactionTarget.getMemoryWithoutUpdate().contains("$virtuous")) {
            if (Global.getSettings().getModManager().isModEnabled("nexerelin")) {
                return new PluginPick<InteractionDialogPlugin>(new DANexVirtuousFleetInteractionDialogPluginImpl(), PickPriority.MOD_SPECIFIC);
            } else {
                return new PluginPick<InteractionDialogPlugin>(new DAVirtuousFleetInteractionDialogPluginImpl(), PickPriority.MOD_SPECIFIC);

            }
        }
        return super.pickInteractionDialogPlugin(interactionTarget);
    }

    public static boolean hasMemoryInFleet(CampaignFleetAPI fleet, String key) {
        if (fleet.getActivePerson() != null) {
            if (fleet.getActivePerson().getMemoryWithoutUpdate().contains(key)) {
                return true;
            }
        }
        if (fleet.getCommander() != null) {
            if (fleet.getCommander().getMemoryWithoutUpdate().contains(key)) {
                return true;
            }
        }
        if (fleet.getFlagship() != null && fleet.getFlagship().getCaptain() != null) {
            if (fleet.getFlagship().getCaptain().getMemoryWithoutUpdate().contains(key)) {
                return true;
            }
        }
        if (fleet.getMemoryWithoutUpdate().contains(key)) {
            return true;
        }
        return false;
    }
}
