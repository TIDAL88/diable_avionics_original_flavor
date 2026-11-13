package data.scripts.campaign.bar;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarEvent;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEvent;
import com.fs.starfarer.api.util.Misc;
import org.json.JSONObject;
import org.lwjgl.input.Keyboard;
import org.magiclib.bounty.ActiveBounty;
import org.magiclib.bounty.MagicBountyCoordinator;
import org.magiclib.bounty.MagicBountyLoader;
import org.magiclib.bounty.MagicBountySpec;
import org.magiclib.util.MagicCampaign;

import java.util.Map;

/** Offers the existing standalone Gulf contract through MagicLib's public API. */
public class DAMagicGulfBarEvent extends BaseBarEvent {
    public static final String BOUNTY_ID = "diable_gulf";
    public static final String ACCEPT_OPTION = "da_magic_gulf_accept";
    public static final String POSTING_OPTION = "da_magic_gulf_posting";
    public static final String LEAVE_OPTION = "da_magic_gulf_leave";

    private final ActiveBounty bounty;
    private final JSONObject dialogue;
    private boolean accessKeyGiven;

    private DAMagicGulfBarEvent(ActiveBounty bounty, JSONObject dialogue) {
        this.bounty = bounty;
        this.dialogue = dialogue;
    }

    public static boolean isAvailable() {
        if (DAGulfBountyState.useIBB()) return false;
        MagicBountyCoordinator coordinator = MagicBountyCoordinator.getInstance();
        MagicBountySpec spec = MagicBountyLoader.getBountyData(BOUNTY_ID);
        if (coordinator == null || spec == null || !"$gulf".equals(spec.existing_target_memkey)) return false;
        if (coordinator.getCompletedBounties().contains(BOUNTY_ID)) return false;
        ActiveBounty active = coordinator.getActiveBounty(BOUNTY_ID);
        if (active != null && active.getStage() != ActiveBounty.Stage.NotAccepted) return false;
        if (DAGulfBountyState.playerAcquiredGulf()) return false;
        if (active != null) {
            // MagicLib already retains the target. Validate it locally instead of searching the sector again.
            CampaignFleetAPI target = active.getFleet();
            return isTarget(target) && target.getContainingLocation() != null
                    && target.getContainingLocation().getFleets().contains(target);
        }
        return findTarget() != null;
    }

    private static boolean isTarget(CampaignFleetAPI fleet) {
        return fleet != null && fleet.getMemoryWithoutUpdate().getBoolean("$gulf")
                && DAGulfBountyState.isGulf(fleet.getFlagship());
    }

    private static CampaignFleetAPI findTarget() {
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            for (CampaignFleetAPI fleet : system.getFleets()) {
                if (isTarget(fleet)) {
                    return fleet;
                }
            }
        }
        return null;
    }

    public static boolean canOfferAt(MarketAPI market) {
        if (market == null) return false;
        MagicBountySpec spec = MagicBountyLoader.getBountyData(BOUNTY_ID);
        if (spec == null) return false;
        return MagicCampaign.isAvailableAtMarket(market, spec.trigger_market_id, spec.trigger_marketFaction_any,
                spec.trigger_marketFaction_alliedWith, spec.trigger_marketFaction_none,
                spec.trigger_marketFaction_enemyWith, spec.trigger_market_minSize)
                && MagicCampaign.isAvailableToPlayer(spec.trigger_player_minLevel, spec.trigger_min_days_elapsed,
                spec.trigger_min_fleet_size, spec.trigger_memKeys_all, spec.trigger_memKeys_any,
                spec.trigger_memKeys_none, spec.trigger_playerRelationship_atLeast, spec.trigger_playerRelationship_atMost)
                && isAvailable();
    }

    public static PortsideBarEvent create(MarketAPI market, JSONObject dialogue) {
        if (!canOfferAt(market)) throw new IllegalStateException("Standalone Gulf contract is not available");
        MagicBountyCoordinator coordinator = MagicBountyCoordinator.getInstance();
        ActiveBounty active = coordinator.getActiveBounty(BOUNTY_ID);
        if (active == null) {
            // existing_target_memkey is checked above: this attaches to the existing fleet.
            active = coordinator.createActiveBounty(BOUNTY_ID, MagicBountyLoader.getBountyData(BOUNTY_ID));
        }
        if (active == null || active.getStage() != ActiveBounty.Stage.NotAccepted) {
            throw new IllegalStateException("Unable to prepare standalone Gulf contract");
        }
        return new DAMagicGulfBarEvent(active, dialogue);
    }

    private boolean canAccept(MarketAPI market) {
        return canOfferAt(market) && MagicBountyCoordinator.getInstance().getActiveBounty(BOUNTY_ID) == bounty;
    }

    @Override
    public void init(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        super.init(dialog, memoryMap);
        done = false;
        noContinue = false;
        accessKeyGiven = false;
        if (!canAccept(dialog.getInteractionTarget().getMarket())) {
            finishUnavailable();
        }
    }

    private void showBriefing() {
        text.addPara(bounty.getSpec().job_name, Misc.getHighlightColor());
        bounty.addDescriptionToTextPanel(text);
        Float credits = bounty.getRewardCredits();
        if (credits != null && credits > 0) {
            String reward = Misc.getDGSCredits(credits);
            text.addPara(String.format(line("magicReward"), reward), Misc.getHighlightColor(), reward);
        }
        MagicBountySpec spec = bounty.getSpec();
        if (spec.job_reputation_reward > 0 && spec.job_forFaction != null) {
            text.addPara(line("magicReputation"));
        }
        dialog.getVisualPanel().showMapMarker(bounty.getFleetSpawnLocation(), null, null, false, null, null, null);
        text.addPara(line("magicBriefingEnd"));
        options.clearOptions();
        options.addOption(line("leaveOption"), LEAVE_OPTION);
        options.setShortcut(LEAVE_OPTION, Keyboard.KEY_ESCAPE, false, false, false, true);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (done) return;
        if (LEAVE_OPTION.equals(optionData)) {
            done = true;
            noContinue = true;
        } else if (POSTING_OPTION.equals(optionData) && accessKeyGiven) {
            showBriefing();
            done = true;
            noContinue = false;
        } else if (ACCEPT_OPTION.equals(optionData)) {
            if (accessKeyGiven) return;
            if (!canAccept(dialog.getInteractionTarget().getMarket())) {
                finishUnavailable();
                return;
            }
            MagicBountySpec spec = bounty.getSpec();
            bounty.acceptBounty(dialog.getInteractionTarget(), spec.job_reputation_reward, spec.job_forFaction);
            accessKeyGiven = true;
            text.addPara(line("magicAccessKey"));
            options.clearOptions();
            options.addOption(line("magicOpenPosting"), POSTING_OPTION);
        }
    }

    private void finishUnavailable() {
        text.addPara(line("bountyUnavailable"));
        done = true;
        noContinue = false;
    }

    private String line(String key) {
        return dialogue.optString(key);
    }
}
