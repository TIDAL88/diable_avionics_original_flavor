package data.scripts.campaign.bar;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName.Gender;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Sounds;
import com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarEvent;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventWithPerson;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseGetCommodityBarEvent;
import com.fs.starfarer.api.impl.campaign.rulecmd.SetStoryOption;
import com.fs.starfarer.api.impl.campaign.rulecmd.SetStoryOption.BaseOptionStoryPointActionDelegate;
import com.fs.starfarer.api.impl.campaign.rulecmd.SetStoryOption.StoryOptionParams;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.AddBarEvent;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.AddBarEvent.BarEventData;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class DASuspiciousAgentBarEvent extends BaseBarEventWithPerson {

    public static final String EVENT_ID = "da_suspicious_agent";
    public static final String INSTANCE_KEY = "$da_ibb_gulf_agent";
    public static final String APPROACH_OPTION = "da_suspicious_agent_approach";
    public static final String FACTION_ID = "diableavionics";
    public static final String DIALOGUE_PATH = "data/config/modFiles/suspicious_agent_dialogue.json";
    public static final String CONTRACT_OPTION = "da_suspicious_agent_contract";
    public static final String LEAVE_OPTION = "da_suspicious_agent_leave";
    public static final String ACCESS_PAID_KEY = "$da_ibb_gulf_access_paid";
    public static final int DISCOVERY_LEVEL = 6;
    public static final int STORY_POINT_COST = 1;

    private String stationedAt;
    private transient JSONObject dialogue;
    private transient boolean contractOpened;
    private transient boolean ibbConversation;
    private transient PortsideBarEvent preparedBounty;
    private transient PortsideBarEvent activeBounty;

    public static DASuspiciousAgentBarEvent getInstance() {
        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
        DASuspiciousAgentBarEvent agent = (DASuspiciousAgentBarEvent) memory.get(INSTANCE_KEY);
        if (agent == null) {
            agent = new DASuspiciousAgentBarEvent();
            memory.set(INSTANCE_KEY, agent);
        }
        return agent;
    }

    @Override
    public String getBarEventId() {
        return EVENT_ID;
    }

    public boolean hasBeenApproached() {
        return stationedAt != null;
    }

    @Override
    public boolean shouldShowAtMarket(MarketAPI market) {
        if (market == null || !FACTION_ID.equals(market.getFactionId())) return false;
        if (hasBeenApproached()) {
            if (!stationedAt.equals(market.getId())) return false;
        } else if (Global.getSector().getPlayerStats().getLevel() < DISCOVERY_LEVEL) {
            return false;
        }
        // The discovery rule applies the monthly 30% roll before adding the option.
        return useIBB() ? DAIBBGulfBarEvent.isAvailable() : DAMagicGulfBarEvent.canOfferAt(market);
    }

    private void stayAtMarket() {
        if (!hasBeenApproached()) stationedAt = market.getId();
    }

    @Override
    protected void regen(MarketAPI market) {
        this.market = market;
        if (random == null) random = new Random(seed);
        if (person == null) person = createPerson();
    }

    @Override
    protected Gender getPersonGender() {
        return Gender.MALE;
    }

    @Override
    protected String getPersonFaction() {
        return Factions.INDEPENDENT;
    }

    @Override
    protected String getPersonRank() {
        return Ranks.POST_AGENT;
    }

    @Override
    protected String getPersonPost() {
        return Ranks.POST_AGENT;
    }

    public void addToBar(MarketAPI market) {
        if (!shouldShowAtMarket(market)) return;
        BarEventData entry = new BarEventData(APPROACH_OPTION, line("approachOption"), line("barDescription"));
        entry.optionColor = Misc.getHighlightColor();
        Map<String, BarEventData> events = AddBarEvent.getTempEvents(market).events;
        // Reserve a visible slot ahead of ordinary bar events; keep other entries in order.
        Map<String, BarEventData> others = new LinkedHashMap<>(events);
        others.remove(APPROACH_OPTION);
        events.clear();
        events.put(APPROACH_OPTION, entry);
        events.putAll(others);
    }

    @Override
    public void init(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        super.init(dialog, memoryMap);
        done = false;
        noContinue = false;
        contractOpened = false;
        preparedBounty = null;
        activeBounty = null;
        ibbConversation = useIBB();
        regen(dialog.getInteractionTarget().getMarket());
        if (!shouldShowAtMarket(market)) {
            done = true;
            noContinue = true;
            return;
        }
        stayAtMarket();

        dialog.getVisualPanel().showPersonInfo(person, true);
        JSONArray opening = dialogue().optJSONArray(ibbConversation ? "opening" : "magicOpening");
        if (opening == null) throw new IllegalStateException("Missing suspicious agent opening dialogue");
        for (int i = 0; i < opening.length(); i++) {
            text.addPara(opening.optString(i), Misc.getHighlightColor(),
                    "IBB", "ship", "I'll be on this station for the time being");
        }
        showOptions();
    }

    private void showOptions() {
        options.clearOptions();
        options.addOption(line("contractOption"), CONTRACT_OPTION);

        // Prepare the selected offer before enabling acceptance.
        if (isBountyAvailable()) {
            try {
                preparedBounty = createGulfOffer();
            } catch (RuntimeException | LinkageError ex) {
                Global.getLogger(DASuspiciousAgentBarEvent.class).error("Unable to prepare the Gulf contract", ex);
            }
        }

        if (ibbConversation) {
            if (hasContractAccess()) {
                options.setTooltip(CONTRACT_OPTION, line("accessAlreadyPaid"));
            } else {
                addPaidContractOption();
            }
        }

        if (preparedBounty == null) {
            options.setEnabled(CONTRACT_OPTION, false);
            options.setTooltip(CONTRACT_OPTION, line("contractUnavailable"));
        }
        addLeaveOption();
    }

    private void addPaidContractOption() {
        StoryOptionParams params = new StoryOptionParams(
                CONTRACT_OPTION, STORY_POINT_COST, CONTRACT_OPTION, Sounds.STORY_POINT_SPEND_COMBAT,
                line("storyPointLog")
        );
        SetStoryOption.set(dialog, params, new BaseOptionStoryPointActionDelegate(dialog, params) {
            @Override
            public void confirm() {
                openContract(true);
            }

            @Override
            public void createDescription(TooltipMakerAPI info) {
                info.addPara(line("confirmationDescription"), 0f);
                info.addSpacer(20f);
                addActionCostSection(info);
            }
        });
    }

    private boolean hasContractAccess() {
        return !ibbConversation || Global.getSector().getMemoryWithoutUpdate().getBoolean(ACCESS_PAID_KEY);
    }

    private void openContract(boolean paid) {
        if (done || contractOpened || preparedBounty == null || !isBountyAvailable()) return;
        if (!paid && !hasContractAccess()) return;
        // The story-option UI handles payment; do not check the remaining SP here.
        if (paid) Global.getSector().getMemoryWithoutUpdate().set(ACCESS_PAID_KEY, true);
        contractOpened = true;
        options.clearOptions();
        addLeaveOption();
        showGulfBounty();
    }

    private void addLeaveOption() {
        options.addOption(line("leaveOption"), LEAVE_OPTION);
        options.setShortcut(LEAVE_OPTION, Keyboard.KEY_ESCAPE, false, false, false, true);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (activeBounty != null) {
            activeBounty.optionSelected(optionText, optionData);
            return;
        }
        if (LEAVE_OPTION.equals(optionData)) {
            done = true;
            noContinue = true;
        } else if (CONTRACT_OPTION.equals(optionData) && hasContractAccess()) {
            openContract(false);
        }
    }

    protected boolean useIBB() {
        // An unavailable IBB stage does not switch the player to the standalone contract.
        return DAGulfBountyState.useIBB();
    }

    protected boolean isBountyAvailable() {
        if (ibbConversation != useIBB()) return false;
        return ibbConversation ? DAIBBGulfBarEvent.isAvailable() : DAMagicGulfBarEvent.canOfferAt(market);
    }

    protected PortsideBarEvent createGulfOffer() {
        return ibbConversation ? DAIBBGulfBarEvent.create(person, line("bountyUnavailable"))
                : DAMagicGulfBarEvent.create(market, dialogue());
    }

    protected void showGulfBounty() {
        activeBounty = preparedBounty;
        activeBounty.init(dialog, memoryMap);
        activeBounty.optionSelected(null, ibbConversation
                ? BaseGetCommodityBarEvent.OPTION_CONFIRM : DAMagicGulfBarEvent.ACCEPT_OPTION);
    }

    @Override
    public boolean isDialogFinished() {
        return activeBounty != null ? activeBounty.isDialogFinished() : super.isDialogFinished();
    }

    @Override
    public boolean endWithContinue() {
        return activeBounty != null ? activeBounty.endWithContinue() : super.endWithContinue();
    }

    private String line(String key) {
        try {
            return dialogue().getString(key);
        } catch (JSONException ex) {
            throw new IllegalStateException("Missing suspicious agent dialogue: " + key, ex);
        }
    }

    private JSONObject dialogue() {
        if (dialogue == null) {
            try {
                dialogue = Global.getSettings().loadJSON(DIALOGUE_PATH, FACTION_ID);
            } catch (IOException | JSONException ex) {
                throw new IllegalStateException("Unable to load suspicious agent dialogue", ex);
            }
        }
        return dialogue;
    }
}
