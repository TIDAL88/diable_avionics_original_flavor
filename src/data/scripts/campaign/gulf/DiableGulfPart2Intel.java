package data.scripts.campaign.gulf;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.procgen.DefenderDataOverride;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicVariables;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
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

    public static final String SITE_ID = "diableavionics_gulf_part2_site";
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

        if (Global.getSector().getMemoryWithoutUpdate().getBoolean(STARTED_MEMKEY)
                || Global.getSector().getMemoryWithoutUpdate().getBoolean(COMPLETE_MEMKEY)) {
            return;
        }

        if (Global.getSector().getIntelManager().hasIntelOfClass(DiableGulfPart2Intel.class)) {
            Global.getSector().getMemoryWithoutUpdate().set(STARTED_MEMKEY, true);
            return;
        }

        if (!playerHasGulf()) return;

        SectorEntityToken existing = Global.getSector().getEntityById(SITE_ID);
        if (existing != null) {
            addIntel(existing);
            Global.getSector().getMemoryWithoutUpdate().set(STARTED_MEMKEY, true);
            return;
        }

        StarSystemAPI system = pickRemoteSystem();
        if (system == null) return;

        Random random = new Random(Misc.genRandomSeed());
        SectorEntityToken site = BaseThemeGenerator.addSalvageEntity(
                random,
                system,
                Entities.STATION_RESEARCH,
                Factions.NEUTRAL
        );

        site.setId(SITE_ID);
        site.setName("Silent Diable Relay");
        placeAtPrimaryStar(site, system, random);
        site.setSensorProfile(3000f);
        site.setDiscoverable(false);
        site.setDiscoveryXP(0f);
        site.addTag(Tags.SALVAGE_ENTITY_NO_REMOVE);
        site.getMemoryWithoutUpdate().set(SITE_MEMKEY, true);
        site.getMemoryWithoutUpdate().set(MemFlags.ENTITY_MISSION_IMPORTANT, true);

        // The generated fleet is replaced wholesale by DiableGulfPart2DefenderPlugin.
        Misc.setDefenderOverride(site, new DefenderDataOverride(
                MagicVariables.BOUNTY_FACTION,
                1f,
                10f,
                10f,
                10
        ));

        system.addTag(Tags.SYSTEM_ALREADY_USED_FOR_STORY);
        Global.getSector().getMemoryWithoutUpdate().set(STARTED_MEMKEY, true);
        Global.getSector().getMemoryWithoutUpdate().set(LANDMARK_PLACEMENT_MEMKEY, true);
        addIntel(site);
    }

    /** Moves sites created by the first test build from an arbitrary system-center orbit to a star. */
    public static void ensureLandmarkPlacement() {
        if (Global.getSector() == null
                || Global.getSector().getMemoryWithoutUpdate().getBoolean(COMPLETE_MEMKEY)
                || Global.getSector().getMemoryWithoutUpdate().getBoolean(LANDMARK_PLACEMENT_MEMKEY)) {
            return;
        }

        SectorEntityToken site = Global.getSector().getEntityById(SITE_ID);
        if (site == null) return;

        StarSystemAPI destination = site.getStarSystem();
        if (!hasSafePrimaryStar(destination)) {
            StarSystemAPI replacement = pickRemoteSystem();
            if (replacement != null) {
                if (site.getContainingLocation() != null) {
                    site.getContainingLocation().removeEntity(site);
                }
                replacement.addEntity(site);
                replacement.addTag(Tags.SYSTEM_ALREADY_USED_FOR_STORY);
                destination = replacement;
            }
        }

        if (!hasSafePrimaryStar(destination)) return;

        placeAtPrimaryStar(site, destination, new Random(Misc.genRandomSeed()));
        site.setSensorProfile(3000f);
        site.setDiscoverable(false);
        site.getMemoryWithoutUpdate().set(MemFlags.ENTITY_MISSION_IMPORTANT, true);
        Global.getSector().getMemoryWithoutUpdate().set(LANDMARK_PLACEMENT_MEMKEY, true);
    }

    private static void placeAtPrimaryStar(SectorEntityToken site, StarSystemAPI system, Random random) {
        float orbitRadius = system.getStar().getRadius() + 2000f;
        site.setCircularOrbitPointingDown(
                system.getStar(),
                random.nextFloat() * 360f,
                orbitRadius,
                120f
        );
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

    private static StarSystemAPI pickRemoteSystem() {
        List<StarSystemAPI> preferred = new ArrayList<StarSystemAPI>();
        List<StarSystemAPI> fallback = new ArrayList<StarSystemAPI>();
        Vector2f sectorCenter = new Vector2f(0f, 0f);

        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (!isAccessible(system)) continue;
            fallback.add(system);

            float distance = Misc.getDistanceLY(system.getLocation(), sectorCenter);
            if (distance >= 8f && distance <= 40f
                    && !system.hasTag(Tags.THEME_CORE)
                    && !system.hasTag(Tags.THEME_CORE_POPULATED)
                    && !system.hasTag(Tags.THEME_CORE_UNPOPULATED)) {
                preferred.add(system);
            }
        }

        Random random = new Random(Misc.genRandomSeed());
        Collections.shuffle(preferred, random);
        Collections.shuffle(fallback, random);
        if (!preferred.isEmpty()) return preferred.get(0);
        if (!fallback.isEmpty()) return fallback.get(0);
        return null;
    }

    private static boolean isAccessible(StarSystemAPI system) {
        return system != null
                && system.getCenter() != null
                && system.getStar() != null
                && system.getHyperspaceAnchor() != null
                && !system.hasPulsar()
                && !system.hasBlackHole()
                && !system.hasTag(Tags.THEME_HIDDEN)
                && !system.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER)
                && !system.hasTag(Tags.SYSTEM_ABYSSAL)
                && !system.hasTag(Tags.PK_SYSTEM)
                && !system.hasTag(Tags.SYSTEM_ALREADY_USED_FOR_STORY);
    }

    private static boolean hasSafePrimaryStar(StarSystemAPI system) {
        return system != null
                && system.getStar() != null
                && !system.hasPulsar()
                && !system.hasBlackHole();
    }

    private String getCurrentSystemName() {
        if (site != null && site.getStarSystem() != null) return site.getStarSystem().getName();
        return systemName;
    }

    private String getCurrentLandmarkName() {
        if (site != null && site.getStarSystem() != null && site.getStarSystem().getStar() != null) {
            return site.getStarSystem().getStar().getName();
        }
        return "the system's primary star";
    }

    @Override
    protected void advanceImpl(float amount) {
        if (!Global.getSector().getMemoryWithoutUpdate().getBoolean(COMPLETE_MEMKEY)) return;

        if (!completionUpdateSent) {
            completionUpdateSent = true;
            setImportant(false);
            sendUpdateIfPlayerHasIntel(UPDATE_COMPLETE, false);

            if (site != null && site.getContainingLocation() != null) {
                site.getContainingLocation().removeEntity(site);
            }
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
            String landmarkName = getCurrentLandmarkName();
            info.addPara("Investigate the relay orbiting " + landmarkName + " in the "
                            + currentSystemName + " system.",
                    5f, Misc.getHighlightColor(), landmarkName, currentSystemName);
        }
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        Color highlight = Misc.getHighlightColor();
        String currentSystemName = getCurrentSystemName();
        String landmarkName = getCurrentLandmarkName();
        info.addPara("A surviving data partition aboard the Gulf contains a set of coordinates. "
                + "The source is a silent Diable relay orbiting " + landmarkName + " in the "
                + currentSystemName + " system.",
                10f, highlight, "Gulf", landmarkName, currentSystemName);

        if (Global.getSector().getMemoryWithoutUpdate().getBoolean(COMPLETE_MEMKEY)) {
            info.addPara("The hidden station was destroyed. Your salvage crews recovered a complete "
                    + "Deep Strike Catapult hullmod specification from its remains.",
                    10f, Misc.getPositiveHighlightColor(), "Deep Strike Catapult");
        } else {
            info.addPara("Travel to the marked point and investigate the relay. The signal identifies "
                    + "a ten-ship Vapor screen, but its deep-strike control traffic suggests that the "
                    + "sensor picture is incomplete.",
                    10f, highlight, "ten-ship Vapor screen", "sensor picture is incomplete");
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
