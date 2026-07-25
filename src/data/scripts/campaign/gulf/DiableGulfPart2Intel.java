package data.scripts.campaign.gulf;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.world.systems.Diableavionics_blackSite;

import java.awt.Color;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Important-intel entry and campaign location for the second Gulf encounter.
 */
public class DiableGulfPart2Intel extends BaseIntelPlugin {

    public static final String STARTED_MEMKEY = "$da_gulf_part2_started";
    public static final String COMPLETE_MEMKEY = "$da_gulf_part2_complete";
    public static final String SITE_MEMKEY = "$da_gulf_part2_site";
    public static final String DEFENDER_MEMKEY = "$da_gulf_part2_defenders";
    public static final String LANDMARK_PLACEMENT_MEMKEY = "$da_gulf_part2_landmark_v1";

    /** Legacy randomly-generated relay ID; retained only for save migration and cleanup. */
    public static final String SITE_ID = "diableavionics_gulf_part2_site";
    public static final String ENEMY_FACTION_ID = "diableavionics_unknown";
    public static final String ENEMY_PORTRAIT = "graphics/da/portraits/scary.png";
    public static final String GULF_BASE_HULL_ID = "diableavionics_IBBgulf";
    public static final String STATION_VARIANT = "diableavionics_station_classic";
    public static final String REWARD_HULLMOD = "gulf_deep_strike";

    private static final String TITLE = "A Cold Plate of Revenge - Part II";
    private static final String UPDATE_COMPLETE = "completed";
    private static final String INTEL_SPRITE_CATEGORY = "diableavionics_intel";
    private static final String INTEL_IMAGE_KEY = "gulfPart2Signal";
    private static final String INTEL_ICON_KEY = "gulfPart2SignalIcon";

    private SectorEntityToken site;
    private String systemName;
    private boolean completionUpdateSent;

    public DiableGulfPart2Intel(SectorEntityToken site) {
        this.site = site;
        this.systemName = site.getStarSystem() == null ? "an unknown system" : site.getStarSystem().getName();
        setImportant(true);
    }

    /**
     * Starts Part II once when the player possesses a Gulf. STARTED_MEMKEY is a permanent latch:
     * losing the ship, acquiring another one, or using the console cannot start a second copy.
     */
    public static void ensureStarted() {
        if (Global.getSector() == null) return;

        if (Global.getSector().getMemoryWithoutUpdate().getBoolean(COMPLETE_MEMKEY)) {
            syncBlackSiteStation();
            return;
        }

        if (Global.getSector().getMemoryWithoutUpdate().getBoolean(STARTED_MEMKEY)) {
            syncBlackSiteStation();
            return;
        }

        if (Global.getSector().getIntelManager().hasIntelOfClass(DiableGulfPart2Intel.class)) {
            Global.getSector().getMemoryWithoutUpdate().set(STARTED_MEMKEY, true);
            syncBlackSiteStation();
            return;
        }

        if (!playerHasGulf()) return;

        SectorEntityToken site = getBlackSiteStation();
        if (site == null) return;

        Global.getSector().getMemoryWithoutUpdate().set(STARTED_MEMKEY, true);
        Global.getSector().getMemoryWithoutUpdate().set(LANDMARK_PLACEMENT_MEMKEY, true);
        configureActiveSite(site);
        removeLegacyRelay();
        addIntel(site);
    }

    /**
     * Save migration for the test builds that targeted a randomly generated relay. Existing intel is
     * retargeted to the permanent Black Site station and only the obsolete relay entity is removed.
     */
    public static void ensureLandmarkPlacement() {
        syncBlackSiteStation();
    }

    private static void syncBlackSiteStation() {
        if (Global.getSector() == null) return;

        SectorEntityToken station = getBlackSiteStation();
        boolean started = Global.getSector().getMemoryWithoutUpdate().getBoolean(STARTED_MEMKEY);
        boolean complete = Global.getSector().getMemoryWithoutUpdate().getBoolean(COMPLETE_MEMKEY);

        removeLegacyRelay();
        if (station == null) return;

        if (started && !complete) {
            configureActiveSite(station);
            retargetExistingIntel(station);
            Global.getSector().getMemoryWithoutUpdate().set(LANDMARK_PLACEMENT_MEMKEY, true);
        } else if (complete) {
            configureCompletedSite(station);
        } else {
            station.removeTag(Tags.HAS_INTERACTION_DIALOG);
            station.getMemoryWithoutUpdate().unset(SITE_MEMKEY);
            station.getMemoryWithoutUpdate().unset(MemFlags.ENTITY_MISSION_IMPORTANT);
            clearLegacySalvageState(station);
            DiableGulfPart2FleetFactory.removeFleet(station);
        }
    }

    private static SectorEntityToken getBlackSiteStation() {
        return Global.getSector().getEntityById(Diableavionics_blackSite.STATION_ID);
    }

    private static void configureActiveSite(SectorEntityToken site) {
        site.addTag(Tags.HAS_INTERACTION_DIALOG);
        site.getMemoryWithoutUpdate().set(SITE_MEMKEY, true);
        site.getMemoryWithoutUpdate().set(MemFlags.ENTITY_MISSION_IMPORTANT, true);
        // Save migration: Part II originally used the vanilla salvage/probe defender backend.
        // Clear every trace of that backend before the normal campaign fleet is created.
        clearLegacySalvageState(site);
    }

    private static void configureCompletedSite(SectorEntityToken site) {
        site.addTag(Tags.HAS_INTERACTION_DIALOG);
        site.getMemoryWithoutUpdate().unset(SITE_MEMKEY);
        site.getMemoryWithoutUpdate().unset(MemFlags.ENTITY_MISSION_IMPORTANT);
        clearLegacySalvageState(site);
        DiableGulfPart2FleetFactory.removeFleet(site);
    }

    private static void clearLegacySalvageState(SectorEntityToken site) {
        site.getMemoryWithoutUpdate().unset(MemFlags.SALVAGE_DEFENDER_OVERRIDE);
        site.getMemoryWithoutUpdate().unset(MemFlags.SALVAGE_SPEC_ID_OVERRIDE);
        site.getMemoryWithoutUpdate().unset("$hasDefenders");
        site.getMemoryWithoutUpdate().unset("$defenderFleet");
        site.getMemoryWithoutUpdate().unset("$defenderFleetDefeated");
    }

    private static void retargetExistingIntel(SectorEntityToken site) {
        for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getIntel(DiableGulfPart2Intel.class)) {
            if (intel instanceof DiableGulfPart2Intel) {
                ((DiableGulfPart2Intel) intel).setSite(site);
            }
        }
    }

    private static void removeLegacyRelay() {
        SectorEntityToken legacy = Global.getSector().getEntityById(SITE_ID);
        if (legacy != null && legacy.getContainingLocation() != null) {
            legacy.getContainingLocation().removeEntity(legacy);
        }
    }

    private void setSite(SectorEntityToken site) {
        this.site = site;
        this.systemName = site.getStarSystem() == null ? "an unknown system" : site.getStarSystem().getName();
    }

    public static boolean playerHasGulf() {
        if (Global.getSector() == null || Global.getSector().getPlayerFleet() == null) return false;

        for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
            if (member.getHullSpec() == null) continue;
            if (GULF_BASE_HULL_ID.equals(member.getHullSpec().getBaseHullId())
                    || GULF_BASE_HULL_ID.equals(member.getHullSpec().getHullId())) {
                return true;
            }
        }
        return false;
    }

    private static void addIntel(SectorEntityToken site) {
        DiableGulfPart2Intel intel = new DiableGulfPart2Intel(site);
        Global.getSector().getIntelManager().addIntel(intel, true);
    }

    private String getCurrentSystemName() {
        if (site != null && site.getStarSystem() != null) return site.getStarSystem().getName();
        return systemName;
    }

    @Override
    protected void advanceImpl(float amount) {
        if (!Global.getSector().getMemoryWithoutUpdate().getBoolean(COMPLETE_MEMKEY)) return;

        if (!completionUpdateSent) {
            completionUpdateSent = true;
            setImportant(false);
            sendUpdateIfPlayerHasIntel(UPDATE_COMPLETE, false);
            syncBlackSiteStation();
            endAfterDelay();
        }
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        Color factionColor = getFactionForUIColors().getBaseUIColor();
        info.addTitle(TITLE, factionColor);

        if (Global.getSector().getMemoryWithoutUpdate().getBoolean(COMPLETE_MEMKEY)) {
            info.addPara("Station destroyed; Deep Strike Catapult modspec recovered.", 5f,
                    Misc.getPositiveHighlightColor(), "Deep Strike Catapult");
        } else {
            String currentSystemName = getCurrentSystemName();
            info.addPara("Investigate the research station orbiting 88 Ra I in the "
                            + currentSystemName + " system.",
                    5f, Misc.getHighlightColor(), "research station", "88 Ra I", currentSystemName);
        }
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        Color highlight = Misc.getHighlightColor();
        info.addImage(
                Global.getSettings().getSpriteName(INTEL_SPRITE_CATEGORY, INTEL_IMAGE_KEY),
                width,
                width * 5f / 8f,
                0f
        );

        info.addPara("Your comms team isolates a single repeating signal buried deep in the Gulf's "
                + "digital underbelly.", 10f, highlight, "Gulf");

        info.addPara("\u201cNothing unusual,\u201d your comms officer says at first. \u201cJust an obsolete ship "
                + "recognizing a legacy handshake.\u201d", 10f);

        info.addPara("What remains of the signal is less a message than the faint memory of one, "
                + "recalled from a dream and lost again upon waking.", 10f);

        info.addPara("Before long, a quiet obsession takes hold across the comms team. Specialists "
                + "remain at their stations long after their shifts have ended, their man-machine "
                + "interfaces still saturated with fragments of the transmission. The team reports "
                + "that its encryption bears the unmistakable architecture of the Admiralty yet "
                + "matches no current corporate standard.", 10f, highlight, "the Admiralty");

        info.addPara("Once decrypted, it resolves into a list:", 10f);
        info.addPara("86 Rn\n99 Es\nOT\n88 Ra - LOST", 5f, highlight,
                "86 Rn", "99 Es", "OT", "88 Ra - LOST");

        info.addPara("Before the team can quarantine the decoded protocol, something in the Gulf "
                + "remembers the proper reply.", 10f, highlight, "Gulf");

        info.addPara("The bridge lights dim. Relays close somewhere beneath the decks. The silence "
                + "stretches; a moment's hesitation is all it takes.", 10f);

        info.addPara("Your comms officer reaches for the emergency cutoff, but the transmission is "
                + "already gone: a narrow, encrypted pulse aimed directly back along the signal's "
                + "origin vector.", 10f);

        info.addPara("The automated navigation display lights up with a set of coordinates. A return "
                + "pulse flashes across the comms board.", 10f, highlight, "a set of coordinates");

        info.addPara("Your comms officer stares at it.", 10f);
        info.addPara("\u201cThey're pinging back.\u201d", 5f, Misc.getNegativeHighlightColor(),
                "\u201cThey're pinging back.\u201d");

        if (Global.getSector().getMemoryWithoutUpdate().getBoolean(COMPLETE_MEMKEY)) {
            info.addPara("The hidden station was destroyed. Your salvage crews recovered a complete "
                    + "Deep Strike Catapult hullmod specification from its remains.",
                    10f, Misc.getPositiveHighlightColor(), "Deep Strike Catapult");
        }
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        return site;
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = new LinkedHashSet<String>(super.getIntelTags(map));
        tags.add(Tags.INTEL_IMPORTANT);
        tags.add(Tags.INTEL_ACCEPTED);
        tags.add(Tags.INTEL_MISSIONS);
        tags.add(Tags.INTEL_BOUNTY);
        tags.add(Tags.INTEL_STORY);
        return tags;
    }

    @Override
    public FactionAPI getFactionForUIColors() {
        return Global.getSector().getFaction("diableavionics");
    }

    @Override
    public String getIcon() {
        return Global.getSettings().getSpriteName(INTEL_SPRITE_CATEGORY, INTEL_ICON_KEY);
    }

    @Override
    protected String getName() {
        return TITLE;
    }

    @Override
    public String getSmallDescriptionTitle() {
        return TITLE;
    }

    @Override
    public IntelInfoPlugin.IntelSortTier getSortTier() {
        return IntelInfoPlugin.IntelSortTier.TIER_2;
    }

    @Override
    public String getSortString() {
        return TITLE;
    }

    @Override
    public boolean autoAddCampaignMessage() {
        return true;
    }

    @Override
    public String getCommMessageSound() {
        return getSoundMajorPosting();
    }

    @Override
    public boolean shouldRemoveIntel() {
        return isEnded();
    }
}
