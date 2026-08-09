package data.scripts.campaign.lastline;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import static data.scripts.util.Diableavionics_stringsManager.txt;

/**
 * Builds the save-authored version of The Last Line as a normal campaign fleet.
 *
 * The source save is an authoring tool only. Every variant, officer template, and
 * fleet skill used at runtime is stored in this mod.
 */
public final class DiableLastLineFleetFactory {

    public static final String FLEET_ID = "diableavionics_last_line_fleet";
    public static final String VIRTUOUS_VARIANT_ID =
            "diableavionics_lastline_virtuous";
    public static final String FLEET_VERSION_MEMKEY =
            "$da_last_line_fleet_version";
    public static final int FLEET_VERSION = 1;
    private static final String MIGRATION_VERSION_MEMKEY =
            "$da_last_line_migration_version";

    private static final String OFFICER_CONFIG =
            "data/config/modFiles/last_line_officers.json";
    private static final String FLEET_SKILLS_KEY = "subject71_fleet_skills";

    private static final FleetEntry[] ESCORTS = {
            new FleetEntry(
                    "diableavionics_lastline_maelstrom",
                    "DSF Last Line-01",
                    "diableavionics_maelstrom",
                    "D1"
            ),
            new FleetEntry(
                    "diableavionics_lastline_storm",
                    "DSF Last Line-02",
                    "diableavionics_storm",
                    "D2"
            ),
            new FleetEntry(
                    "diableavionics_lastline_storm",
                    "DSF Last Line-03",
                    "diableavionics_storm",
                    "D3"
            ),
            new FleetEntry(
                    "diableavionics_lastline_storm",
                    "DSF Last Line-04",
                    "diableavionics_storm",
                    "D4"
            ),
            new FleetEntry(
                    "diableavionics_lastline_daze",
                    "DSF Last Line-05",
                    "diableavionics_daze",
                    "D5"
            ),
            new FleetEntry(
                    "diableavionics_lastline_daze",
                    "DSF Last Line-06",
                    "diableavionics_daze",
                    "D6"
            ),
            new FleetEntry(
                    "diableavionics_lastline_coanda",
                    "DSF Last Line-07",
                    null,
                    null
            ),
            new FleetEntry(
                    "diableavionics_lastline_coanda",
                    "DSF Last Line-08",
                    null,
                    null
            ),
            new FleetEntry(
                    "diableavionics_lastline_gust_raven",
                    "DSF Last Line-09",
                    "diableavionics_gust",
                    "D7"
            ),
            new FleetEntry(
                    "diableavionics_lastline_gust_zephyr",
                    "DSF Last Line-10",
                    "diableavionics_gust",
                    "D8"
            ),
            new FleetEntry(
                    "diableavionics_lastline_gust_raven",
                    "DSF Last Line-11",
                    "diableavionics_gust",
                    "D9"
            ),
            new FleetEntry(
                    "diableavionics_lastline_minigust",
                    "DSF Last Line-12",
                    "diableavionics_miniGust",
                    "D10"
            ),
            new FleetEntry(
                    "diableavionics_lastline_minigust",
                    "DSF Last Line-13",
                    "diableavionics_miniGust",
                    "D11"
            ),
            new FleetEntry(
                    "diableavionics_lastline_minigust",
                    "DSF Last Line-14",
                    "diableavionics_miniGust",
                    "D12"
            ),
            new FleetEntry(
                    "diableavionics_lastline_vapor",
                    "DSF Last Line-15",
                    null,
                    null
            ),
            new FleetEntry(
                    "diableavionics_lastline_vapor",
                    "DSF Last Line-16",
                    null,
                    null
            ),
            new FleetEntry(
                    "diableavionics_lastline_rime_standby",
                    "DSF Last Line-17",
                    null,
                    null
            ),
            new FleetEntry(
                    "diableavionics_lastline_rime_standby",
                    "DSF Last Line-18",
                    null,
                    null
            ),
            new FleetEntry(
                    "diableavionics_lastline_rime_standby",
                    "DSF Last Line-19",
                    null,
                    null
            ),
            new FleetEntry(
                    "diableavionics_lastline_rime_standby",
                    "DSF Last Line-20",
                    null,
                    null
            )
    };

    private static JSONObject officerConfig;

    private DiableLastLineFleetFactory() {
    }

    public static CampaignFleetAPI createFleet(
            SectorEntityToken target,
            PersonAPI subject71
    ) {
        if (target == null || target.getContainingLocation() == null) {
            throw new IllegalArgumentException(
                    "The Last Line requires a valid campaign spawn target"
            );
        }
        if (subject71 == null) {
            throw new IllegalArgumentException(
                    "The Last Line requires Subject 71"
            );
        }

        configureSubject71FleetSkills(subject71);

        CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(
                "diableavionics",
                FleetTypes.TASK_FORCE,
                true
        );
        if (fleet == null) {
            throw new IllegalStateException("Unable to create The Last Line fleet");
        }

        configureFleetIdentity(fleet, subject71);
        populateFleet(fleet, subject71);
        finishFleet(fleet);

        LocationAPI location = target.getContainingLocation();
        location.addEntity(fleet);
        fleet.setLocation(target.getLocation().x, target.getLocation().y);
        fleet.setFacing(target.getFacing());
        fleet.addAssignment(
                FleetAssignment.PATROL_SYSTEM,
                target,
                Float.MAX_VALUE
        );

        return fleet;
    }

    /**
     * Rebuilds a still-active classic Last Line fleet in place once per campaign.
     * Keeping the CampaignFleetAPI object preserves quest and bounty references.
     */
    public static CampaignFleetAPI migrateExistingFleet(boolean keepClassicFleet) {
        if (Global.getSector() == null) return null;

        int migrationVersion = Global.getSector().getMemoryWithoutUpdate()
                .getInt(MIGRATION_VERSION_MEMKEY);
        if (migrationVersion >= FLEET_VERSION) return null;

        if (keepClassicFleet) {
            markMigrationComplete();
            return null;
        }

        for (LocationAPI location : Global.getSector().getAllLocations()) {
            for (CampaignFleetAPI fleet : new ArrayList<CampaignFleetAPI>(
                    location.getFleets()
            )) {
                if (!isActiveLastLineFleet(fleet)) continue;

                if (fleet.getMemoryWithoutUpdate().getInt(FLEET_VERSION_MEMKEY)
                        >= FLEET_VERSION) {
                    markMigrationComplete();
                    return null;
                }

                PersonAPI subject71 = findSubject71(fleet);
                if (subject71 == null) {
                    // Do not risk replacing a transferred or otherwise resolved boss.
                    markMigrationComplete();
                    return null;
                }

                configureSubject71FleetSkills(subject71);
                clearFleetMembersAndOfficers(fleet);
                configureFleetIdentity(fleet, subject71);
                populateFleet(fleet, subject71);
                finishFleet(fleet);
                markMigrationComplete();
                return fleet;
            }
        }

        // Destroyed, transferred, despawned, or never present: nothing to resurrect.
        markMigrationComplete();
        return null;
    }

    private static void configureFleetIdentity(
            CampaignFleetAPI fleet,
            PersonAPI subject71
    ) {
        fleet.setId(FLEET_ID);
        fleet.setName(txt("virtuousFleet"));
        fleet.setNoAutoDespawn(true);
        fleet.setTransponderOn(true);
        fleet.setCommander(subject71);
    }

    private static void populateFleet(
            CampaignFleetAPI fleet,
            PersonAPI subject71
    ) {
        FleetMemberAPI virtuous = fleet.getFleetData().addFleetMember(
                VIRTUOUS_VARIANT_ID
        );
        virtuous.setShipName(txt("virtuousShip"));
        virtuous.setCaptain(subject71);
        prepareMember(virtuous);
        fleet.getFleetData().setFlagship(virtuous);

        for (FleetEntry entry : ESCORTS) {
            FleetMemberAPI member = fleet.getFleetData().addFleetMember(entry.variantId);
            member.setShipName(entry.shipName);

            if (entry.captainType != null) {
                member.setCaptain(createOfficer(entry.captainType, entry.captainName));
            }
            prepareMember(member);
        }
    }

    private static void finishFleet(CampaignFleetAPI fleet) {
        fleet.getFleetData().sort();
        fleet.forceSync();
        fleet.updateCounts();
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
        }
        fleet.getMemoryWithoutUpdate().set(FLEET_VERSION_MEMKEY, FLEET_VERSION);
    }

    private static void clearFleetMembersAndOfficers(CampaignFleetAPI fleet) {
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            fleet.getFleetData().removeFleetMember(member);
        }
        for (OfficerDataAPI officer : fleet.getFleetData().getOfficersCopy()) {
            fleet.getFleetData().removeOfficer(officer.getPerson());
        }
    }

    private static boolean isActiveLastLineFleet(CampaignFleetAPI fleet) {
        if (fleet == null || fleet.isPlayerFleet() || fleet.isDespawning()) {
            return false;
        }
        if (!fleet.getMemoryWithoutUpdate().getBoolean("$virtuous")) {
            return false;
        }

        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            if (member.getHullSpec().getBaseHullId()
                    .startsWith("diableavionics_virtuous")) {
                return true;
            }
        }
        return false;
    }

    private static PersonAPI findSubject71(CampaignFleetAPI fleet) {
        PersonAPI commander = fleet.getCommander();
        if (commander != null
                && commander.getMemoryWithoutUpdate().getBoolean("$virtuous")) {
            return commander;
        }

        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            PersonAPI captain = member.getCaptain();
            if (captain != null
                    && captain.getMemoryWithoutUpdate().getBoolean("$virtuous")) {
                return captain;
            }
        }
        return null;
    }

    private static void markMigrationComplete() {
        Global.getSector().getMemoryWithoutUpdate().set(
                MIGRATION_VERSION_MEMKEY,
                FLEET_VERSION
        );
    }

    /**
     * Replaces old rng admiral skills while preserving Subject 71's ten
     * randomly selected elite combat skills. this one's for you tarti
     */
    private static void configureSubject71FleetSkills(PersonAPI subject71) {
        JSONObject skills = getOfficerConfig().optJSONObject(FLEET_SKILLS_KEY);
        if (skills == null) {
            throw new IllegalStateException("Missing Subject 71 fleet skills");
        }

        subject71.getStats().setSkipRefresh(true);
        try {
            for (MutableCharacterStatsAPI.SkillLevelAPI skill
                    : subject71.getStats().getSkillsCopy()) {
                if (skill.getSkill() != null && skill.getSkill().isAdmiralSkill()) {
                    subject71.getStats().setSkillLevel(skill.getSkill().getId(), 0f);
                }
            }

            java.util.Iterator<?> keys = skills.keys();
            while (keys.hasNext()) {
                String skillId = String.valueOf(keys.next());
                subject71.getStats().setSkillLevel(
                        skillId,
                        (float) skills.optDouble(skillId, 0f)
                );
            }
        } finally {
            subject71.getStats().setSkipRefresh(false);
        }
    }

    private static void prepareMember(FleetMemberAPI member) {
        member.getRepairTracker().setMothballed(false);
        member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
    }

    /**
     * Instantiates one independent officer from the hull's save-authored template.
     */
    private static PersonAPI createOfficer(String specKey, String serialName) {
        JSONObject config = getOfficerConfig().optJSONObject(specKey);
        if (config == null) {
            throw new IllegalStateException(
                    "Missing The Last Line officer template for " + specKey
            );
        }

        String factionId = config.optString("officer_faction", "diableavionics");
        FactionAPI faction = Global.getSector().getFaction(factionId);
        if (faction == null) {
            throw new IllegalStateException(
                    "Missing The Last Line officer faction " + factionId
            );
        }

        PersonAPI pilot = faction.createRandomPerson();
        pilot.setName(new FullName(serialName, "", pilot.getGender()));
        pilot.setFaction(factionId);
        pilot.setRankId(Ranks.SPACE_LIEUTENANT);
        pilot.setPostId(Ranks.POST_OFFICER);
        pilot.setPersonality(config.optString("officer_personality", "aggressive"));

        pilot.getStats().setSkipRefresh(true);
        pilot.getStats().setLevel(config.optInt("officer_level", 1));
        applySkills(pilot, config.optJSONObject("officer_skills"));
        pilot.getStats().setSkipRefresh(false);
        return pilot;
    }

    private static void applySkills(PersonAPI person, JSONObject skills) {
        if (person == null || skills == null) return;

        person.getStats().setSkipRefresh(true);
        java.util.Iterator<?> keys = skills.keys();
        while (keys.hasNext()) {
            String skillId = String.valueOf(keys.next());
            person.getStats().setSkillLevel(
                    skillId,
                    (float) skills.optDouble(skillId, 0f)
            );
        }
        person.getStats().setSkipRefresh(false);
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
                    "Unable to load The Last Line officer templates",
                    ex
            );
        }
    }

    private record FleetEntry(
            String variantId,
            String shipName,
            String captainType,
            String captainName
    ) {
    }
}
