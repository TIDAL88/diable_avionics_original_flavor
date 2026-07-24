package data.scripts.campaign.gulf;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import org.json.JSONException;
import org.json.JSONObject;
import org.lwjgl.util.vector.Vector2f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates and maintains the real campaign fleet used by the Gulf Part II encounter.
 *
 * The station is only the quest interaction point. The save-authored escort is a normal campaign
 * fleet,
 * while the Classic station continues to enter the battle through DiableGulfPart2CombatPlugin.
 */
public final class DiableGulfPart2FleetFactory {

    public static final String FLEET_ID = "diableavionics_gulf_part2_encounter_fleet";
    public static final String SITE_FLEET_MEMKEY = "$da_gulf_part2_encounter_fleet";

    private static final float SPAWN_DISTANCE = 850f;
    private static final String FLEET_VERSION_MEMKEY = "$da_gulf_part2_fleet_version";
    private static final int FLEET_VERSION = 3;
    private static final String OFFICER_CONFIG =
            "data/config/modFiles/gulf_part2_officers.json";
    private static final String STATION_CAPTAIN_KEY = "station";
    private static final String FLEET_SKILLS_KEY = "fleet_commander_skills";

    /**
     * Exact combat ships from the DIABLE CLASSIC FLEET save. The Dram and Phaeton were templates:
     * the Dram supplied fleetwide skills and the Phaeton supplied the station captain.
     */
    private static final FleetEntry[] FLEET = {
            new FleetEntry(
                    "diableavionics_gulf2_coanda",
                    "ISS Stellar Rose",
                    "diableavionics_coanda"
            ),
            new FleetEntry(
                    "diableavionics_gulf2_coanda",
                    "ISS Blueshift",
                    "diableavionics_coanda"
            ),
            new FleetEntry(
                    "diableavionics_gulf2_coanda",
                    "ISS Red Dwarf",
                    "diableavionics_coanda"
            ),
            new FleetEntry(
                    "diableavionics_gulf2_coanda",
                    "ISS Aloft Parabolic",
                    "diableavionics_coanda"
            ),
            new FleetEntry(
                    "diableavionics_gulf2_minigust_valiant",
                    "ISS Stars Below",
                    "diableavionics_miniGust"
            ),
            new FleetEntry(
                    "diableavionics_gulf2_minigust_valiant",
                    "ISS Interstitial Breeze",
                    "diableavionics_miniGust"
            ),
            new FleetEntry(
                    "diableavionics_gulf2_minigust_valiant",
                    "ISS Redshift",
                    "diableavionics_miniGust"
            ),
            new FleetEntry(
                    "diableavionics_gulf2_minigust_valiant",
                    "ISS Lightspeed+",
                    "diableavionics_miniGust"
            ),
            new FleetEntry(
                    "diableavionics_gulf2_minigust_strife",
                    "ISS 7000 Lightyears",
                    "diableavionics_miniGust"
            ),
            new FleetEntry(
                    "diableavionics_gulf2_vapor",
                    "ISS Stars Below",
                    "diableavionics_vapor"
            ),
            new FleetEntry(
                    "diableavionics_gulf2_vapor",
                    "ISS Mercurial",
                    "diableavionics_vapor"
            ),
            new FleetEntry(
                    "diableavionics_gulf2_vapor",
                    "DSF Kilbeggan",
                    "diableavionics_vapor"
            )
    };

    private static JSONObject officerConfig;

    private DiableGulfPart2FleetFactory() {
    }

    public static CampaignFleetAPI getOrCreateFleet(SectorEntityToken site) {
        if (site == null || site.getContainingLocation() == null || Global.getSector() == null) {
            return null;
        }
        if (Global.getSector().getMemoryWithoutUpdate()
                .getBoolean(DiableGulfPart2Intel.COMPLETE_MEMKEY)) {
            return null;
        }

        CampaignFleetAPI existing = findFleet(site);
        if (isUsable(existing) && isCurrentVersion(existing)) {
            site.getMemoryWithoutUpdate().set(SITE_FLEET_MEMKEY, existing);
            prepareForDialog(existing);
            return existing;
        }

        // Also clears an old placeholder composition or an interrupted test encounter.
        removeFleet(site);

        CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(
                DiableGulfPart2Intel.ENEMY_FACTION_ID,
                "task_force",
                true
        );
        if (fleet == null) return null;

        fleet.setId(FLEET_ID);
        fleet.setName("Diable Avionics ?");
        fleet.setNoFactionInName(true);
        fleet.setFaction(DiableGulfPart2Intel.ENEMY_FACTION_ID, true);
        fleet.setNoAutoDespawn(true);
        fleet.setTransponderOn(true);
        fleet.setCommander(createStationCommander());

        FleetMemberAPI flagship = null;

        for (FleetEntry entry : FLEET) {
            FleetMemberAPI member = fleet.getFleetData().addFleetMember(entry.variantId);
            member.setShipName(entry.shipName);

            PersonAPI pilot = createOfficer(entry.captainType, false);
            member.setCaptain(pilot);
            member.getRepairTracker().setMothballed(false);
            member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());

            if (flagship == null) flagship = member;
        }

        if (flagship != null) fleet.getFleetData().setFlagship(flagship);

        fleet.getFleetData().sort();
        fleet.forceSync();
        fleet.updateCounts();
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
        }

        configureMemory(fleet);

        LocationAPI location = site.getContainingLocation();
        location.addEntity(fleet);
        placeBeyondStation(fleet, site);
        prepareForDialog(fleet);
        site.getMemoryWithoutUpdate().set(SITE_FLEET_MEMKEY, fleet);
        return fleet;
    }

    public static CampaignFleetAPI findFleet(SectorEntityToken site) {
        if (site == null) return null;

        Object stored = site.getMemoryWithoutUpdate().get(SITE_FLEET_MEMKEY);
        if (stored instanceof CampaignFleetAPI && isUsable((CampaignFleetAPI) stored)) {
            return (CampaignFleetAPI) stored;
        }

        LocationAPI location = site.getContainingLocation();
        if (location == null) return null;
        for (CampaignFleetAPI fleet : location.getFleets()) {
            if (fleet == null || fleet.isPlayerFleet()) continue;
            if (FLEET_ID.equals(fleet.getId())
                    || fleet.getMemoryWithoutUpdate()
                    .getBoolean(DiableGulfPart2Intel.DEFENDER_MEMKEY)) {
                if (isUsable(fleet)) return fleet;
            }
        }
        return null;
    }

    public static void prepareForDialog(CampaignFleetAPI fleet) {
        if (fleet == null) return;
        fleet.setDoNotAdvanceAI(true);
        fleet.clearAssignments();
        fleet.setVelocity(0f, 0f);
    }

    public static void resumeIntercept(CampaignFleetAPI fleet) {
        if (!isUsable(fleet) || Global.getSector() == null
                || Global.getSector().getPlayerFleet() == null) {
            return;
        }

        fleet.setDoNotAdvanceAI(false);
        fleet.clearAssignments();
        fleet.addAssignment(
                FleetAssignment.INTERCEPT,
                Global.getSector().getPlayerFleet(),
                Float.MAX_VALUE,
                "intercepting your fleet"
        );
    }

    public static void removeFleet(SectorEntityToken site) {
        if (site == null) return;

        LocationAPI location = site.getContainingLocation();
        if (location != null) {
            List<CampaignFleetAPI> fleets = new ArrayList<CampaignFleetAPI>(location.getFleets());
            for (CampaignFleetAPI fleet : fleets) {
                if (fleet == null || fleet.isPlayerFleet()) continue;
                if (FLEET_ID.equals(fleet.getId())
                        || fleet.getMemoryWithoutUpdate()
                        .getBoolean(DiableGulfPart2Intel.DEFENDER_MEMKEY)) {
                    location.removeEntity(fleet);
                }
            }
        }
        site.getMemoryWithoutUpdate().unset(SITE_FLEET_MEMKEY);
    }

    private static void configureMemory(CampaignFleetAPI fleet) {
        fleet.getMemoryWithoutUpdate().set(FLEET_VERSION_MEMKEY, FLEET_VERSION);
        fleet.getMemoryWithoutUpdate().set(DiableGulfPart2Intel.DEFENDER_MEMKEY, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_PREVENT_DISENGAGE, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_FIGHT_TO_THE_LAST, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_DO_NOT_IGNORE_PLAYER, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORES_OTHER_FLEETS, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_JUMP, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MISSION_IMPORTANT, true);
        fleet.getMemoryWithoutUpdate().set("$nex_noKeepSMods", true);
    }

    /**
     * The Phaeton captain from the template save, augmented with the Dram player's fleetwide
     * skills. This person commands the campaign fleet and an equivalent copy pilots the station
     * when it arrives, ensuring the delayed combat spawn receives the same profile.
     */
    public static PersonAPI createStationCommander() {
        PersonAPI commander = createOfficer(STATION_CAPTAIN_KEY, true);
        applySkills(commander, getOfficerConfig().optJSONObject(FLEET_SKILLS_KEY));
        commander.getMemoryWithoutUpdate().set("$exceptionalSleeperPodOfficer", true);
        return commander;
    }

    /**
     * Adversary-style officer construction: one JSON captain template per hull, instantiated as a
     * new person for every ship.
     */
    private static PersonAPI createOfficer(String specKey, boolean fleetCommander) {
        JSONObject config = getOfficerConfig().optJSONObject(specKey);
        if (config == null) {
            throw new IllegalStateException(
                    "Missing Gulf Part II officer template for " + specKey
            );
        }

        String factionId = config.optString(
                "officer_faction",
                DiableGulfPart2Intel.ENEMY_FACTION_ID
        );
        FactionAPI faction = Global.getSector().getFaction(factionId);
        if (faction == null) {
            throw new IllegalStateException(
                    "Missing Gulf Part II officer faction " + factionId
            );
        }

        PersonAPI pilot = faction.createRandomPerson();
        pilot.setName(new FullName("REDACTED", "", pilot.getGender()));
        pilot.setFaction(factionId);
        pilot.setPortraitSprite(DiableGulfPart2Intel.ENEMY_PORTRAIT);
        pilot.setRankId(
                fleetCommander ? Ranks.SPACE_COMMANDER : Ranks.SPACE_LIEUTENANT
        );
        pilot.setPostId(
                fleetCommander ? Ranks.POST_FLEET_COMMANDER : Ranks.POST_OFFICER
        );
        pilot.setPersonality(config.optString("officer_personality", "steady"));

        pilot.getStats().setSkipRefresh(true);
        pilot.getStats().setLevel(config.optInt("officer_level", 1));
        applySkills(pilot, config.optJSONObject("officer_skills"));
        pilot.getStats().setSkipRefresh(false);
        return pilot;
    }

    private static void applySkills(PersonAPI person, JSONObject skills) {
        if (person == null || skills == null) return;

        boolean restoreRefresh = false;
        try {
            person.getStats().setSkipRefresh(true);
            restoreRefresh = true;
            java.util.Iterator<?> keys = skills.keys();
            while (keys.hasNext()) {
                String skillId = String.valueOf(keys.next());
                person.getStats().setSkillLevel(
                        skillId,
                        (float) skills.optDouble(skillId, 0f)
                );
            }
        } finally {
            if (restoreRefresh) person.getStats().setSkipRefresh(false);
        }
    }

    private static JSONObject getOfficerConfig() {
        if (officerConfig != null) return officerConfig;

        try {
            officerConfig = Global.getSettings().loadJSON(
                    OFFICER_CONFIG,
                    "diableavionics"
            );
            return officerConfig;
        } catch (IOException | JSONException ex) {
            throw new IllegalStateException(
                    "Unable to load Gulf Part II officer templates",
                    ex
            );
        }
    }

    private static void placeBeyondStation(CampaignFleetAPI fleet, SectorEntityToken site) {
        Vector2f siteLocation = site.getLocation();
        Vector2f playerLocation = Global.getSector().getPlayerFleet() == null
                ? null
                : Global.getSector().getPlayerFleet().getLocation();

        float x = 1f;
        float y = 0f;
        if (playerLocation != null) {
            x = siteLocation.x - playerLocation.x;
            y = siteLocation.y - playerLocation.y;
            float length = (float) Math.sqrt(x * x + y * y);
            if (length > 0.001f) {
                x /= length;
                y /= length;
            } else {
                x = 1f;
                y = 0f;
            }
        }

        fleet.setLocation(
                siteLocation.x + x * SPAWN_DISTANCE,
                siteLocation.y + y * SPAWN_DISTANCE
        );
    }

    private static boolean isUsable(CampaignFleetAPI fleet) {
        return fleet != null
                && !fleet.isEmpty()
                && !fleet.isDespawning()
                && fleet.getContainingLocation() != null;
    }

    private static boolean isCurrentVersion(CampaignFleetAPI fleet) {
        return fleet != null
                && fleet.getMemoryWithoutUpdate().getInt(FLEET_VERSION_MEMKEY)
                == FLEET_VERSION;
    }

    private static final class FleetEntry {
        private final String variantId;
        private final String shipName;
        private final String captainType;

        private FleetEntry(String variantId, String shipName, String captainType) {
            this.variantId = variantId;
            this.shipName = shipName;
            this.captainType = captainType;
        }
    }
}
