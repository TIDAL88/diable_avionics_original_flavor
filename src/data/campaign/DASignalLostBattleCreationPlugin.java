package data.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.combat.BattleCreationPluginImpl;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import data.campaign.rulecmd.DASignalLost;
import data.scripts.campaign.gulf.DiableGulfPart2Music;
import org.magiclib.bounty.ActiveBounty;
import org.magiclib.bounty.MagicBountyCoordinator;

public final class DASignalLostBattleCreationPlugin extends BattleCreationPluginImpl {
    static boolean isBountyFleet(CampaignFleetAPI fleet) {
        MagicBountyCoordinator coordinator = MagicBountyCoordinator.getInstance();
        if (coordinator == null) return false;
        ActiveBounty bounty = coordinator.getActiveBounty(DASignalLost.BOUNTY_ID);
        return bounty != null && bounty.getStage() == ActiveBounty.Stage.Accepted && bounty.getFleet() == fleet;
    }

    @Override
    public void initBattle(BattleCreationContext context, MissionDefinitionAPI loader) {
        super.initBattle(context, loader);
        // Preserve the battle settings applied by MagicLib's bounty plugin.
        if (context.getOtherFleet().getMemoryWithoutUpdate().getBoolean(MemFlags.FLEET_FIGHT_TO_THE_LAST)) {
            context.aiRetreatAllowed = false;
            context.fightToTheLast = true;
        }
    }

    @Override
    public void afterDefinitionLoad(CombatEngineAPI engine) {
        super.afterDefinitionLoad(engine);
        // The campaign calls this immediately before switching to the combat state.
        Global.getSector().addTransientScript(new RestoreCampaignMusic());
        DiableGulfPart2Music.start();
    }

    private static final class RestoreCampaignMusic implements EveryFrameScript {
        private boolean done;

        @Override
        public void advance(float amount) {
            if (Global.getCurrentState() != GameState.CAMPAIGN) return;
            done = true;
            DiableGulfPart2Music.stopAndRestoreCampaignMusic();
        }

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public boolean runWhilePaused() {
            return true;
        }
    }
}
