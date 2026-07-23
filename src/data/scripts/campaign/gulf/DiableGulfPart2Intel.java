package data.scripts.campaign.gulf;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.procgen.DefenderDataOverride;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.world.systems.Diableavionics_blackSite;
import org.magiclib.util.MagicVariables;

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
        } else {
            station.removeTag(Tags.HAS_INTERACTION_DIALOG);
            station.getMemoryWithoutUpdate().unset(SITE_MEMKEY);
            station.getMemoryWithoutUpdate().unset(MemFlags.ENTITY_MISSION_IMPORTANT);
            station.getMemoryWithoutUpdate().unset(MemFlags.SALVAGE_DEFENDER_OVERRIDE);
            station.getMemoryWithoutUpdate().unset(MemFlags.SALVAGE_SPEC_ID_OVERRIDE);
        }
    }

    private static SectorEntityToken getBlackSiteStation() {
        return Global.getSector().getEntityById(Diableavionics_blackSite.STATION_ID);
    }

    private static void configureActiveSite(SectorEntityToken site) {
        site.addTag(Tags.HAS_INTERACTION_DIALOG);
        site.getMemoryWithoutUpdate().set(SITE_MEMKEY, true);
        site.getMemoryWithoutUpdate().set(MemFlags.ENTITY_MISSION_IMPORTANT, true);
        // The Black Site uses a custom visual entity type, which is not itself a salvage-data ID.
        // Tell SalvageGenFromSeed to use vanilla's research-station data instead.
        site.getMemoryWithoutUpdate().set(MemFlags.SALVAGE_SPEC_ID_OVERRIDE, Entities.STATION_RESEARCH);

        // The generated fleet is replaced wholesale by DiableGulfPart2DefenderPlugin.
        // Use MagicLib's populated bounty faction only to seed that temporary fleet: the deliberately
        // empty hidden encounter faction cannot generate one for the defender plugin to replace.
        Misc.setDefenderOverride(site, new DefenderDataOverride(
                MagicVariables.BOUNTY_FACTION,
                1f,
                10f,
                10f,
                10
        ));
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
            info.addPara("Investigate the research station orbiting FOB-01 in the "
                            + currentSystemName + " system.",
                    5f, Misc.getHighlightColor(), "research station", "FOB-01", currentSystemName);
        }
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        Color highlight = Misc.getHighlightColor();
        String currentSystemName = getCurrentSystemName();
        info.addPara("A surviving data partition aboard the Gulf contains a signal trace from a "
                        + "research station orbiting FOB-01. The transmission carries Diable identification "
                        + "codes, but its coordinates correspond to no charted system or registered facility.",
                10f, highlight, "Gulf", "research station", "FOB-01", "Diable identification codes");

        if (Global.getSector().getMemoryWithoutUpdate().getBoolean(COMPLETE_MEMKEY)) {
            info.addPara("The hidden station was destroyed. Your salvage crews recovered a complete "
                    + "Deep Strike Catapult hullmod specification from its remains.",
                    10f, Misc.getPositiveHighlightColor(), "Deep Strike Catapult");
        } else {
            info.addPara("Travel to the marked station in the " + currentSystemName + " system and "
                            + "investigate the impossible signal.",
                    10f, highlight, currentSystemName, "impossible signal");
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
        return Global.getSettings().getHullModSpec(REWARD_HULLMOD).getSpriteName();
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
