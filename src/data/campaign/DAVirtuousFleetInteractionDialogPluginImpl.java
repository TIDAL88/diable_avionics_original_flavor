package data.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CombatDamageData;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import data.campaign.special.Diableavionics_virtuousLoot;
import data.scripts.campaign.lastline.DiableLastLineFleetFactory;
import data.scripts.world.DiableavionicsGen;
import org.magiclib.achievements.MagicAchievementManager;

public class DAVirtuousFleetInteractionDialogPluginImpl extends FleetInteractionDialogPluginImpl {
    private static final String DUELIST_ACHIEVEMENT_ID =
            "diableavionics_duelist";

    public DAVirtuousFleetInteractionDialogPluginImpl() {
        super(DASubject71DialogMusic.createFleetInteractionConfig());
    }

    @Override
    public void init(InteractionDialogAPI dialog) {
        super.init(dialog);
        if (DACampaignPlugin.hasMemoryInFleet(otherFleet, "$virtuous")) {
            DASubject71DialogMusic.start();
        }
    }

    @Override
    public void backFromEngagement(EngagementResultAPI result) {
        if (DACampaignPlugin.hasMemoryInFleet(otherFleet, "$virtuous")
                && DASubject71BattleCreationPlugin.isSimulationBattleActive()) {
            DASubject71BattleCreationPlugin.restoreSimulationBackground();
            restoreOrigCaptains();
            if (origFlagship != null) {
                if (selectedFlagship != null) {
                    PersonAPI captain = origFlagship.getCaptain();
                    if (captain != null && !captain.isPlayer()) {
                        selectedFlagship.setCaptain(captain);
                    }
                }
                Global.getSector().getPlayerFleet().getFleetData().setFlagship(origFlagship);
            }

            otherFleet.getMemoryWithoutUpdate().set("$simulationSuccessful", result.didPlayerWin());
            otherFleet.getCommander().getMemoryWithoutUpdate().set("$simulationSuccessful", result.didPlayerWin());
            if (result.didPlayerWin()) {
                if (!DiableavionicsGen.useClassicLastLineFleet()
                        && otherFleet.getMemoryWithoutUpdate().getInt(
                        DiableLastLineFleetFactory.FLEET_VERSION_MEMKEY
                ) > 0) {
                    MagicAchievementManager.getInstance().completeAchievement(
                            DUELIST_ACHIEVEMENT_ID
                    );
                }
                DiableLastLineFleetFactory.convertSubject71ToD1(otherFleet);
            }
            Global.getCombatEngine().removePlugin(new DASubject71CombatMusic());
            result.setLastCombatDamageData(new CombatDamageData());

            result.getWinnerResult().getReserves().clear();
            result.getWinnerResult().getDeployed().clear();
            result.getWinnerResult().getDisabled().clear();
            result.getWinnerResult().getRetreated().clear();
            result.getWinnerResult().getDestroyed().clear();

            result.getLoserResult().getReserves().clear();
            result.getLoserResult().getDeployed().clear();
            result.getLoserResult().getDisabled().clear();
            result.getLoserResult().getRetreated().clear();
            result.getLoserResult().getDestroyed().clear();

            context.getDataFor(playerFleet).getCrewLossesDuringLastEngagement().removeAllCrew();
            context.getDataFor(otherFleet).getCrewLossesDuringLastEngagement().removeAllCrew();

            context.getDataFor(playerFleet).getDestroyedInLastEngagement().clear();
            context.getDataFor(playerFleet).getDisabledInLastEngagement().clear();
            context.getDataFor(playerFleet).getRetreatedFromLastEngagement().clear();
            context.getDataFor(playerFleet).getMemberToDeployedMap().clear();

            context.getDataFor(otherFleet).getDestroyedInLastEngagement().clear();
            context.getDataFor(otherFleet).getDisabledInLastEngagement().clear();
            context.getDataFor(otherFleet).getRetreatedFromLastEngagement().clear();

            for (FleetMemberAPI member : playerFleet.getMembersWithFightersCopy()) {
                context.getDataFor(playerFleet).removeOwnCasualty(member);
                context.getDataFor(otherFleet).removeEnemyCasualty(member);
                context.getDataFor(playerFleet).changeOwn(member, FleetEncounterContextPlugin.Status.NORMAL);
                context.getDataFor(otherFleet).changeEnemy(member, FleetEncounterContextPlugin.Status.NORMAL);
                member.getStatus().setHullFraction(1);
                member.getStatus().repairFully();
            }

            for (FleetMemberAPI member : otherFleet.getMembersWithFightersCopy()) {
                context.getDataFor(otherFleet).removeOwnCasualty(member);
                context.getDataFor(playerFleet).removeEnemyCasualty(member);
                context.getDataFor(otherFleet).changeOwn(member, FleetEncounterContextPlugin.Status.NORMAL);
                context.getDataFor(playerFleet).changeEnemy(member, FleetEncounterContextPlugin.Status.NORMAL);
                member.getStatus().setHullFraction(1);
                member.getStatus().repairFully();
            }

            context.setEngagedInActualBattle(false);
            context.setEngagedInHostilities(false);

            showFleetInfo();
            openPostSimulationTransferComm();
            DASubject71DialogMusic.start();
            return;
        }
        Diableavionics_virtuousLoot.recordLastLineEngagementOutcome(
                otherFleet,
                result
        );
        try {
            super.backFromEngagement(result);
        } finally {
            Diableavionics_virtuousLoot.clearLastLineEngagementOutcome(
                    otherFleet
            );
        }
        if (DACampaignPlugin.hasMemoryInFleet(otherFleet, "$virtuous")) {
            DASubject71DialogMusic.start();
        }
    }

    public void pullFleets() {
        pullInNearbyFleets();
    }

    /**
     * Vanilla redraws the fleet commander after OpenCommLink rules finish.
     * Suppress that redraw only for this call so the transfer contact selected
     * by the rules remains visible, then restore the original memory state.
     */
    private void openPostSimulationTransferComm() {
        MemoryAPI memory = dialog.getInteractionTarget().getMemoryWithoutUpdate();
        boolean hadPreviousValue = memory.contains(DO_NOT_AUTO_SHOW_FC_PORTRAIT);
        Object previousValue = memory.get(DO_NOT_AUTO_SHOW_FC_PORTRAIT);

        memory.set(DO_NOT_AUTO_SHOW_FC_PORTRAIT, true);
        try {
            optionSelected("", OptionId.OPEN_COMM);
        } finally {
            if (hadPreviousValue) {
                memory.set(DO_NOT_AUTO_SHOW_FC_PORTRAIT, previousValue);
            } else {
                memory.unset(DO_NOT_AUTO_SHOW_FC_PORTRAIT);
            }
        }
    }
}
