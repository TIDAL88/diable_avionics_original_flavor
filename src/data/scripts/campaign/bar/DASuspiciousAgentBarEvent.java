package data.scripts.campaign.bar;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName.Gender;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Sounds;
import com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarData;
import com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarEvent;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventWithPerson;
import com.fs.starfarer.api.impl.campaign.rulecmd.SetStoryOption;
import com.fs.starfarer.api.impl.campaign.rulecmd.SetStoryOption.BaseOptionStoryPointActionDelegate;
import com.fs.starfarer.api.impl.campaign.rulecmd.SetStoryOption.StoryOptionParams;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.Map;
import java.util.Random;

public class DASuspiciousAgentBarEvent extends BaseBarEventWithPerson {

    public static final String EVENT_ID = "da_suspicious_agent";
    public static final String FACTION_ID = "diableavionics";
    public static final String DIALOGUE_PATH = "data/config/modFiles/suspicious_agent_dialogue.json";
    public static final String CONTRACT_OPTION = "da_suspicious_agent_contract";
    public static final String LEAVE_OPTION = "da_suspicious_agent_leave";
    public static final int REQUIRED_LEVEL = 6;
    public static final int STORY_POINT_COST = 1;

    private transient JSONObject dialogue;
    private transient boolean contractOpened;

    public static void register() {
        PortsideBarData data = PortsideBarData.getInstance();
        for (PortsideBarEvent event : data.getEvents()) {
            if (EVENT_ID.equals(event.getBarEventId())) return;
        }

        // A persistent event avoids the random creator pool and its global cooldowns.
        data.addEvent(new DASuspiciousAgentBarEvent());
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (FACTION_ID.equals(market.getFactionId())) {
                // Existing saves may have cached the bar menu before this event was installed.
                market.getMemoryWithoutUpdate().unset("$BarCMD_shownEvents");
            }
        }
    }

    @Override
    public String getBarEventId() {
        return EVENT_ID;
    }

    @Override
    public boolean isAlwaysShow() {
        return true;
    }

    @Override
    public boolean shouldShowAtMarket(MarketAPI market) {
        // BaseBarEvent binds an event to its first market; this agent serves all Diable bars.
        return market != null && FACTION_ID.equals(market.getFactionId()) && shouldShowAgent();
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
    protected String getPersonPortrait() {
        return "graphics/portraits/portrait_mercenary01.png";
    }

    @Override
    protected String getPersonRank() {
        return Ranks.POST_AGENT;
    }

    @Override
    protected String getPersonPost() {
        return Ranks.POST_AGENT;
    }

    @Override
    public void addPromptAndOption(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        regen(dialog.getInteractionTarget().getMarket());
        dialog.getTextPanel().addPara(line("barDescription"));
        dialog.getOptionPanel().addOption(line("approachOption"), this);
    }

    @Override
    public void init(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        super.init(dialog, memoryMap);
        done = false;
        noContinue = false;
        contractOpened = false;
        regen(dialog.getInteractionTarget().getMarket());
        if (!shouldShowAtMarket(market)) {
            done = true;
            noContinue = true;
            return;
        }

        dialog.getVisualPanel().showPersonInfo(person, true);
        JSONArray opening = dialogue().optJSONArray("opening");
        if (opening == null) throw new IllegalStateException("Missing suspicious agent opening dialogue");
        for (int i = 0; i < opening.length(); i++) {
            text.addPara(opening.optString(i), Misc.getHighlightColor(), "IBB", "ship");
        }
        showOptions();
    }

    private void showOptions() {
        options.clearOptions();
        options.addOption(String.format(line("contractOption"), REQUIRED_LEVEL), CONTRACT_OPTION);

        StoryOptionParams params = new StoryOptionParams(
                CONTRACT_OPTION, STORY_POINT_COST, CONTRACT_OPTION, Sounds.STORY_POINT_SPEND_COMBAT,
                line("storyPointLog")
        );
        SetStoryOption.set(dialog, params, new BaseOptionStoryPointActionDelegate(dialog, params) {
            @Override
            public void confirm() {
                if (done || contractOpened || !meetsLevelRequirement() || !isBountyAvailable()) return;
                // The story-option UI handles payment; do not check the remaining SP here.
                contractOpened = true;
                options.clearOptions();
                addLeaveOption();
                showGulfBounty();
            }

            @Override
            public void createDescription(TooltipMakerAPI info) {
                info.addPara(line("confirmationDescription"), 0f);
                info.addSpacer(20f);
                addActionCostSection(info);
            }
        });

        if (!meetsLevelRequirement()) {
            options.setEnabled(CONTRACT_OPTION, false);
            options.setTooltip(CONTRACT_OPTION, String.format(line("levelRequirement"), REQUIRED_LEVEL));
        } else if (!isBountyAvailable()) {
            options.setEnabled(CONTRACT_OPTION, false);
            options.setTooltip(CONTRACT_OPTION, line("contractUnavailable"));
        }
        addLeaveOption();
    }

    private boolean meetsLevelRequirement() {
        return Global.getSector().getPlayerStats().getLevel() >= REQUIRED_LEVEL;
    }

    private void addLeaveOption() {
        options.addOption(line("leaveOption"), LEAVE_OPTION);
        options.setShortcut(LEAVE_OPTION, Keyboard.KEY_ESCAPE, false, false, false, true);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (LEAVE_OPTION.equals(optionData)) {
            done = true;
            noContinue = true;
        }
        // The contract is opened by the story-point confirmation delegate, not a second click handler.
    }

    protected boolean shouldShowAgent() {
        // TODO TIDAL: add IBB visibility checks (enabled, Gulf not active/completed/already acquired).
        return true;
    }

    protected boolean isBountyAvailable() {
        // TODO TIDAL: enable only when showGulfBounty() can open the real SWP offer.
        return false;
    }

    protected void showGulfBounty() {
        // TODO TIDAL: hand off to the Gulf IBB briefing using dialog and memoryMap.
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
