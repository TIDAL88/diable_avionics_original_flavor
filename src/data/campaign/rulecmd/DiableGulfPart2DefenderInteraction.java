package data.campaign.rulecmd;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.RuleBasedDialog;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.gulf.DiableGulfPart2FleetFactory;
import data.scripts.campaign.gulf.DiableGulfPart2Music;

import java.util.List;
import java.util.Map;

/**
 * Starts Gulf Part II as a normal fleet encounter.
 *
 * This intentionally does not extend SalvageDefenderInteraction: that class owns the probe and
 * automated-defense copy which kept leaking into the quest presentation.
 */
public class DiableGulfPart2DefenderInteraction extends BaseCommandPlugin {

    private static final String ENGAGE_TEXT = "\"Action stations.\"";
    private static final String FLEET_DESC_SHOWN_KEY = "$shownFleetDescAlready";
    private static final String VICTORY_TRIGGER = "DAGulfPart2FleetDefeated";

    @Override
    public boolean execute(
            String ruleId,
            final InteractionDialogAPI dialog,
            List<Misc.Token> params,
            final Map<String, MemoryAPI> memoryMap
    ) {
        if (dialog == null) return false;

        final SectorEntityToken station = dialog.getInteractionTarget();
        final CampaignFleetAPI enemyFleet =
                DiableGulfPart2FleetFactory.getOrCreateFleet(station);
        if (enemyFleet == null) {
            DiableGulfPart2Music.stopAndRestoreCampaignMusic();
            return false;
        }

        DiableGulfPart2FleetFactory.prepareForDialog(enemyFleet);
        enemyFleet.getMemoryWithoutUpdate().set(FLEET_DESC_SHOWN_KEY, true);

        final InteractionDialogPlugin originalPlugin = dialog.getPlugin();
        final FleetInteractionDialogPluginImpl.FIDConfig config =
                createEncounterConfig();
        final FleetInteractionDialogPluginImpl plugin =
                new PostSkillReportFleetDialogPlugin(config);

        config.delegate = new FleetInteractionDialogPluginImpl.BaseFIDDelegate() {
            @Override
            public void notifyLeave(InteractionDialogAPI encounterDialog) {
                boolean playerWon = enemyFleet.isEmpty();
                if (plugin.getContext() instanceof FleetEncounterContext) {
                    FleetEncounterContext context =
                            (FleetEncounterContext) plugin.getContext();
                    playerWon |= context.didPlayerWinEncounterOutright();
                }

                DiableGulfPart2Music.stopAndRestoreCampaignMusic();

                if (!playerWon) {
                    DiableGulfPart2FleetFactory.resumeIntercept(enemyFleet);
                    encounterDialog.dismiss();
                    return;
                }

                DiableGulfPart2FleetFactory.removeFleet(station);
                encounterDialog.setPlugin(originalPlugin);
                encounterDialog.setInteractionTarget(station);

                Map<String, MemoryAPI> currentMemoryMap = memoryMap;
                if (originalPlugin instanceof RuleBasedDialog) {
                    RuleBasedDialog rules = (RuleBasedDialog) originalPlugin;
                    rules.updateMemory();
                    currentMemoryMap = rules.getMemoryMap();
                }
                FireBest.fire(null, encounterDialog, currentMemoryMap, VICTORY_TRIGGER);
            }

            @Override
            public void battleContextCreated(
                    InteractionDialogAPI encounterDialog,
                    BattleCreationContext context
            ) {
                context.aiRetreatAllowed = false;
                context.enemyDeployAll = true;
                context.fightToTheLast = true;
            }

            @Override
            public void postPlayerSalvageGeneration(
                    InteractionDialogAPI encounterDialog,
                    FleetEncounterContext context,
                    CargoAPI salvage
            ) {
                // The quest's guaranteed hullmod is awarded by the station dialog after victory.
            }
        };

        dialog.setInteractionTarget(enemyFleet);
        dialog.setPlugin(plugin);
        plugin.init(dialog);
        return true;
    }

    private static final class PostSkillReportFleetDialogPlugin
            extends FleetInteractionDialogPluginImpl {

        private int advanceCount;
        private boolean postSkillReportTextAdded;

        private PostSkillReportFleetDialogPlugin(FIDConfig config) {
            super(config);
        }

        @Override
        public void advance(float amount) {
            super.advance(amount);

            // Other campaign listeners may append commander skill panels on the first frame after
            // the interaction target becomes a fleet. Wait one additional frame so this stays last.
            if (!postSkillReportTextAdded && ++advanceCount >= 2) {
                postSkillReportTextAdded = true;
                addPostSkillReportText(dialog);
            }
        }
    }

    private static void addPostSkillReportText(InteractionDialogAPI dialog) {
        dialog.getTextPanel().addPara(
                "The surveyors are hurried aboard the nearest ship. Weary faces and dazed "
                        + "expressions crowd the loading-bay feed."
        );
        dialog.getTextPanel().addPara(
                "Your XO grips a TriPad with both hands and looks to you for an order."
        );
    }

    private static FleetInteractionDialogPluginImpl.FIDConfig createEncounterConfig() {
        FleetInteractionDialogPluginImpl.FIDConfig config =
                new FleetInteractionDialogPluginImpl.FIDConfig();
        config.leaveAlwaysAvailable = false;
        config.showCommLinkOption = false;
        config.showEngageText = false;
        config.showFleetAttitude = false;
        config.showTransponderStatus = false;
        config.showWarningDialogWhenNotHostile = false;
        config.alwaysAttackVsAttack = true;
        config.alwaysPursue = true;
        config.impactsAllyReputation = false;
        config.impactsEnemyReputation = false;
        config.pullInAllies = false;
        config.pullInEnemies = false;
        config.pullInStations = false;
        config.withSalvage = false;
        config.lootCredits = false;
        config.showVictoryText = false;
        config.firstTimeEngageOptionText = ENGAGE_TEXT;
        config.afterFirstTimeEngageOptionText = ENGAGE_TEXT;
        config.noSalvageLeaveOptionText = "Continue";
        config.noLeaveOptionOnFirstEngagement = true;
        config.dismissOnLeave = false;
        config.printXPToDialog = true;
        return config;
    }
}
