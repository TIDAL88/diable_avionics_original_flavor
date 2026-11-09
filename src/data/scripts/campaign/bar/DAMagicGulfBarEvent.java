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
    public static final String LEAVE_OPTION = "da_magic_gulf_leave";

    private final ActiveBounty bounty;
    private final JSONObject dialogue;

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
        if (!isAvailable() || market == null) return false;
        MagicBountySpec spec = MagicBountyLoader.getBountyData(BOUNTY_ID);
        return MagicCampaign.isAvailableAtMarket(market, spec.trigger_market_id, spec.trigger_marketFaction_any,
                spec.trigger_marketFaction_alliedWith, spec.trigger_marketFaction_none,
                spec.trigger_marketFaction_enemyWith, spec.trigger_market_minSize)
                && MagicCampaign.isAvailableToPlayer(spec.trigger_player_minLevel, spec.trigger_min_days_elapsed,
                spec.trigger_min_fleet_size, spec.trigger_memKeys_all, spec.trigger_memKeys_any,
                spec.trigger_memKeys_none, spec.trigger_playerRelationship_atLeast, spec.trigger_playerRelationship_atMost);
    }

    public static String requirementsText(JSONObject dialogue) {
        MagicBountySpec spec = MagicBountyLoader.getBountyData(BOUNTY_ID);
        if (spec == null) return dialogue.optString("contractUnavailable");
        float relationship = spec.trigger_playerRelationship_atLeast == null ? 0f
                : spec.trigger_playerRelationship_atLeast.getOrDefault(DASuspiciousAgentBarEvent.FACTION_ID, 0f);
        return String.format(dialogue.optString("magicRequirements"), Math.round(relationship * 100f), spec.trigger_min_fleet_size);
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
        if (!canAccept(dialog.getInteractionTarget().getMarket())) {
            finishUnavailable();
            return;
        }
        text.addPara(bounty.getSpec().job_name, Misc.getHighlightColor());
        bounty.addDescriptionToTextPanel(text);
        Float credits = bounty.getRewardCredits();
        if (credits != null && credits > 0) {
            String reward = Misc.getDGSCredits(credits);
            text.addPara(String.format(line("magicReward"), reward), Misc.getHighlightColor(), reward);
        }
        MagicBountySpec spec = bounty.getSpec();
        if (spec.job_reputation_reward > 0 && spec.job_forFaction != null) {
            String reputation = Integer.toString(Math.round(spec.job_reputation_reward * 100f));
            String faction = Global.getSector().getFaction(spec.job_forFaction).getDisplayName();
            text.addPara(String.format(line("magicReputation"), reputation, faction),
                    Misc.getHighlightColor(), reputation, faction);
        }
        if (bounty.getSpec().job_deadline > 0
                && Boolean.TRUE.equals(MagicBountyCoordinator.getInstance().getDeadlinesEnabled())) {
            String days = Integer.toString(bounty.getSpec().job_deadline);
            text.addPara(String.format(line("magicDeadline"), days), Misc.getHighlightColor(), days);
        }
        dialog.getVisualPanel().showMapMarker(bounty.getFleetSpawnLocation(), null, null, false, null, null, null);
        options.clearOptions();
        options.addOption(line("magicAcceptOption"), ACCEPT_OPTION);
        options.addOption(line("leaveOption"), LEAVE_OPTION);
        options.setShortcut(LEAVE_OPTION, Keyboard.KEY_ESCAPE, false, false, false, true);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (done) return;
        if (LEAVE_OPTION.equals(optionData)) {
            done = true;
            noContinue = true;
        } else if (ACCEPT_OPTION.equals(optionData)) {
            if (!canAccept(dialog.getInteractionTarget().getMarket())) {
                finishUnavailable();
                return;
            }
            MagicBountySpec spec = bounty.getSpec();
            bounty.acceptBounty(dialog.getInteractionTarget(), spec.job_reputation_reward, spec.job_forFaction);
            text.addPara(line("magicAccepted"));
            done = true;
            noContinue = false;
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
