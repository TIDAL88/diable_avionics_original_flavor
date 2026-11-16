package data.scripts.campaign.bar;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarData;
import com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarEvent;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import data.scripts.SWPModPlugin;
import data.scripts.campaign.intel.SWP_IBBIntel;
import data.scripts.campaign.intel.SWP_IBBIntel.FamousBountyStage;
import data.scripts.campaign.intel.SWP_IBBTracker;
import data.scripts.campaign.intel.bar.events.SWP_IBBBarEvent;
import data.scripts.util.ReflectionUtils;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

// Loaded only when SWP is enabled; the persistent Diable event uses vanilla types.
public class DAIBBGulfBarEvent extends SWP_IBBBarEvent {

    private final PersonAPI agent;
    private final String unavailableText;

    public static PortsideBarEvent create(PersonAPI agent, String unavailableText) {
        return new DAIBBGulfBarEvent(agent, unavailableText);
    }

    public DAIBBGulfBarEvent(PersonAPI agent, String unavailableText) {
        this(agent, unavailableText, new SWP_IBBIntel());
    }

    DAIBBGulfBarEvent(PersonAPI agent, String unavailableText, SWP_IBBIntel bounty) {
        this.agent = agent;
        this.unavailableText = unavailableText;
        initializeGulf(bounty);
        ReflectionUtils.set(this, "intel", bounty, true);
        ReflectionUtils.set(this, "thisStage", FamousBountyStage.STAGE_GULF, true);
    }

    private static void initializeGulf(SWP_IBBIntel bounty) {
        FamousBountyStage stage = FamousBountyStage.STAGE_GULF;
        // init() would replace thisStage with a random/progression-based target.
        // Set the fields that init() fills before start(); SWP owns the fleet and reward logic.
        ReflectionUtils.set(bounty, "thisStage", stage, true);
        PersonAPI target = bounty.initPerson(Global.getSector().getFaction("famous_bounty"));
        ReflectionUtils.set(bounty, "person", target, true);
        ReflectionUtils.set(bounty, "it", stage.it, true);
        ReflectionUtils.set(bounty, "they", stage.they, true);
        ReflectionUtils.set(bounty, "bountyCredits", (float) stage.reward, true);
        ReflectionUtils.set(bounty, "payment", (float) stage.reward, true);
        // SWP 1.16.6's Gulf description; the other briefing paragraphs come from SWP's bar event.
        ReflectionUtils.set(bounty, "targetDescLong",
                "%s, wanted on charges of treason by the Diable Corporation. She"
                        + " thereafter carved a war-path through allied territory, costing local polities millions in damage. As such, "
                        + "she is officially wanted dead by all IBB member polities. " + target.getName().getLast()
                        + " commands a large Diable fleet from the helm of her unique Gust.", true);
    }

    public static boolean isAvailable() {
        if (!isIBBEnabled()) return false;
        SWP_IBBTracker tracker = SWP_IBBTracker.getTracker();
        return tracker != null && tracker.isStageAvailable(FamousBountyStage.STAGE_GULF)
                && !DAGulfBountyState.playerAcquiredGulf();
    }

    public static boolean isIBBEnabled() {
        return SWPModPlugin.isIBBEnabled();
    }

    @Override
    public void init(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        this.dialog = dialog;
        this.memoryMap = memoryMap;
        this.text = dialog.getTextPanel();
        this.options = dialog.getOptionPanel();
        this.market = dialog.getInteractionTarget().getMarket();
        this.person = agent;
        done = false;
        if (!isAvailable()) {
            finishUnavailable();
            return;
        }
        SWP_IBBTracker.getTracker().reportStagePosted(FamousBountyStage.STAGE_GULF);
        // The Diable introduction has already played. Start at SWP's target briefing.
        super.optionSelected(null, OPTION_CONTINUE);
    }

    @Override
    protected void addStoryOption() {
        // This contact offers only the Gulf, so omit SWP's random-target reroll.
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (done) return;
        if (!isAvailable()) {
            finishUnavailable();
            return;
        }
        super.optionSelected(optionText, optionData);
        if (OPTION_CONFIRM.equals(optionData)
                && SWP_IBBTracker.getTracker().isStageBegun(FamousBountyStage.STAGE_GULF)) {
            invalidateRegularOffers();
        }
    }

    private void finishUnavailable() {
        text.addPara(unavailableText);
        done = true;
        noContinue = false;
    }

    private void invalidateRegularOffers() {
        Set<PortsideBarEvent> offers = new LinkedHashSet<>(PortsideBarData.getInstance().getEvents());
        BarEventManager manager = BarEventManager.getInstance();
        if (manager != null) offers.addAll(manager.getActive().getItems());
        for (PortsideBarEvent offer : offers) {
            if (offer == this || !(offer instanceof SWP_IBBBarEvent)) continue;
            if (ReflectionUtils.get(offer, "thisStage", null, true) != FamousBountyStage.STAGE_GULF) continue;
            // SWP caches offers before acceptance and does not recheck them in start().
            ReflectionUtils.set(offer, "remove", true, true);
            ReflectionUtils.set(offer, "intel", null, true);
            ReflectionUtils.set(offer, "thisStage", null, true);
        }
    }
}
