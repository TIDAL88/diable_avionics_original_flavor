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
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import data.campaign.LastLineFID;
import org.json.JSONException;
import org.json.JSONObject;
import org.magiclib.util.MagicCampaign;

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
    public static final int FLEET_VERSION = 27;
    private static final String MIGRATION_VERSION_MEMKEY =
            "$da_last_line_migration_version";

    private static final String OFFICER_CONFIG =
            "data/config/modFiles/last_line_officers.json";
    private static final String FLEET_SKILLS_KEY = "subject71_fleet_skills";
    private static final String COMBAT_SKILLS_KEY = "subject71_combat_skills";
    private static final int SUBJECT_71_LEVEL = 8;
    private static final String NEX_AUTORESOLVE_STRENGTH_MULT_KEY =
            "$nex_autoresolve_strMult";
    private static final float NEX_AUTORESOLVE_STRENGTH_MULT = 3f;
    private static final String NEX_NO_KEEP_SMODS_KEY = "$nex_noKeepSMods";
    private static final String ESCORT_PORTRAIT =
            "graphics/da/portraits/diableavionics_thelastline.png";

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
                    "diableavionics_lastline_gust_blizzaia",
                    "DSF Last Line-04",
                    "diableavionics_gust",
                    "D4"
            ),
            new FleetEntry(
                    "diableavionics_lastline_gust_blizzaia",
                    "DSF Last Line-05",
                    "diableavionics_gust",
                    "D5"
            ),
            new FleetEntry(
                    "diableavionics_lastline_gust_blizzaia",
                    "DSF Last Line-06",
                    "diableavionics_gust",
                    "D6"
            ),
            new FleetEntry(
                    "diableavionics_lastline_gust_blizzaia",
                    "DSF Last Line-07",
                    "diableavionics_gust",
                    "D7"
            ),
            new FleetEntry(
                    "diableavionics_lastline_vapor",
                    "DSF Last Line-08",
                    "diableavionics_vapor",
                    "D8"
            ),
            new FleetEntry(
                    "diableavionics_lastline_vapor",
                    "DSF Last Line-09",
                    "diableavionics_vapor",
                    "D9"
            ),
            new FleetEntry(
                    "diableavionics_lastline_coanda",
                    "DSF Last Line-10",
                    "diableavionics_coanda",
                    "D10"
            ),
            new FleetEntry(
                    "diableavionics_lastline_coanda",
                    "DSF Last Line-11",
                    "diableavionics_coanda",
                    "D11"
            ),
            new FleetEntry(
                    "diableavionics_lastline_coanda",
                    "DSF Last Line-12",
                    "diableavionics_coanda",
                    "D12"
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

        CampaignFleetAPI fleet = createMagicFlagshipFleet(subject71);

        configureFleetIdentity(fleet, subject71);
        populateFleet(fleet, subject71, fleet.getFlagship());
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

        syncExistingFleetState();

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

                CampaignFleetAPI flagshipFleet =
                        createMagicFlagshipFleet(subject71);
                FleetMemberAPI virtuous = flagshipFleet.getFlagship();
                flagshipFleet.getFleetData().removeFleetMember(virtuous);

                clearFleetMembersAndOfficers(fleet);
                configureFleetIdentity(fleet, subject71);
                populateFleet(fleet, subject71, virtuous);
                finishFleet(fleet);
                markMigrationComplete();
                return fleet;
            }
        }

        // Destroyed, transferred, despawned, or never present: nothing to resurrect.
        markMigrationComplete();
        return null;
    }

    /**
     * Creates the fixed Virtuous preset through the same flagship path used by
     * Tartiflette's classic Last Line builder. The fleet remains unspawned until
     * the handmade escorts and officers have been installed.
     */
    private static CampaignFleetAPI createMagicFlagshipFleet(
            PersonAPI subject71
    ) {
        CampaignFleetAPI fleet = MagicCampaign.createFleetBuilder()
                .setFleetFaction("diableavionics")
                .setFleetName(txt("virtuousFleet"))
                .setFleetType(FleetTypes.TASK_FORCE)
                .setFlagshipName(txt("virtuousShip"))
                .setFlagshipVariant(VIRTUOUS_VARIANT_ID)
                .setFlagshipAutofit(false)
                .setCaptain(subject71)
                .setMinFP(0)
                .setQualityOverride(2f)
                .setIsImportant(false)
                .setTransponderOn(true)
                .create();

        if (fleet == null || fleet.getFlagship() == null) {
            throw new IllegalStateException(
                    "Unable to create The Last Line Virtuous"
            );
        }

        // MagicFleetBuilder generates commander skills and may generate officers.
        // Restore the save-authored Subject 71 build and retain no generated officers.
        configureSubject71Skills(subject71);
        for (OfficerDataAPI officer : fleet.getFleetData().getOfficersCopy()) {
            if (officer.getPerson() != subject71) {
                fleet.getFleetData().removeOfficer(officer.getPerson());
            }
        }

        fleet.getFlagship().setCaptain(subject71);
        fleet.setCommander(subject71);
        return fleet;
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
        fleet.getMemoryWithoutUpdate().set(
                NEX_AUTORESOLVE_STRENGTH_MULT_KEY,
                NEX_AUTORESOLVE_STRENGTH_MULT
        );
        fleet.getMemoryWithoutUpdate().set(NEX_NO_KEEP_SMODS_KEY, true);
        fleet.getMemoryWithoutUpdate().set(
                MemFlags.FLEET_INTERACTION_DIALOG_CONFIG_OVERRIDE_GEN,
                new LastLineFID()
        );
    }

    private static void populateFleet(
            CampaignFleetAPI fleet,
            PersonAPI subject71,
            FleetMemberAPI virtuous
    ) {
        if (!fleet.getFleetData().getMembersListCopy().contains(virtuous)) {
            fleet.getFleetData().addFleetMember(virtuous);
        }
        virtuous.setShipName(txt("virtuousShip"));
        virtuous.setCaptain(subject71);
        prepareMember(virtuous);
        fleet.getFleetData().setFlagship(virtuous);

        for (FleetEntry entry : ESCORTS) {
            FleetMemberAPI member = createAuthoredMember(entry.variantId);
            fleet.getFleetData().addFleetMember(member);
            member.setShipName(entry.shipName);

            if (entry.captainType != null) {
                member.setCaptain(createOfficer(entry.captainType, entry.captainName));
            }
            prepareMember(member);
        }
    }

    /**
     * Creates an exact clone of an authored variant and protects it from the
     * fleet autofit/quality pass. This is the same protection MagicFleetBuilder
     * applies to preset ships when support autofit is disabled.
     */
    private static FleetMemberAPI createAuthoredMember(String variantId) {
        ShipVariantAPI variant = Global.getSettings().getVariant(variantId);
        if (variant == null) {
            throw new IllegalStateException(
                    "Missing The Last Line variant " + variantId
            );
        }

        FleetMemberAPI member = Global.getFactory().createFleetMember(
                FleetMemberType.SHIP,
                variant
        );
        if (member == null) {
            throw new IllegalStateException(
                    "Unable to create The Last Line member " + variantId
            );
        }

        member.getVariant().addTag(Tags.TAG_NO_AUTOFIT);
        return member;
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
        if (findSubject71(fleet) != null) {
            return true;
        }

        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            if (member.getHullSpec() != null
                    && member.getHullSpec().getBaseHullId()
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

    /**
     * Subject 71 dies at the end of a successful fleet simulation. Keep the
     * commander object required by the encounter, but permanently present it
     * as the Maelstrom's D1 unit from this point onward.
     */
    public static void convertSubject71ToD1(CampaignFleetAPI fleet) {
        PersonAPI subject71 = findSubject71(fleet);
        if (subject71 == null) {
            Global.getLogger(DiableLastLineFleetFactory.class).warn(
                    "Unable to convert Subject 71 to D1 after simulation"
            );
            return;
        }

        subject71.setName(new FullName("D1", "", subject71.getGender()));
        subject71.setPortraitSprite(ESCORT_PORTRAIT);
        Global.getLogger(DiableLastLineFleetFactory.class).info(
                "Converted Subject 71 identity to Last Line D1"
        );
    }

    private static void markMigrationComplete() {
        Global.getSector().getMemoryWithoutUpdate().set(
                MIGRATION_VERSION_MEMKEY,
                FLEET_VERSION
        );
    }

    /**
     * Replaces the generated build with the level-eight DATA save build.
     * The existing authored admiral skills remain unchanged.
     */
    private static void configureSubject71Skills(PersonAPI subject71) {
        JSONObject fleetSkills = getOfficerConfig().optJSONObject(FLEET_SKILLS_KEY);
        JSONObject combatSkills = getOfficerConfig().optJSONObject(COMBAT_SKILLS_KEY);
        if (fleetSkills == null || combatSkills == null) {
            throw new IllegalStateException("Missing Subject 71 skill configuration");
        }

        subject71.setPersonality(Personalities.AGGRESSIVE);
        subject71.getStats().setSkipRefresh(true);
        try {
            subject71.getStats().setLevel(SUBJECT_71_LEVEL);
            for (MutableCharacterStatsAPI.SkillLevelAPI skill
                    : subject71.getStats().getSkillsCopy()) {
                if (skill.getSkill() != null) {
                    subject71.getStats().setSkillLevel(skill.getSkill().getId(), 0f);
                }
            }

            applySkillLevels(subject71, combatSkills);
            applySkillLevels(subject71, fleetSkills);
        } finally {
            subject71.getStats().setSkipRefresh(false);
        }
    }

    private static void prepareMember(FleetMemberAPI member) {
        // MagicFleetBuilder preserves enemy S-mods by default. Last Line boss
        // S-mods are combat-only and must be stripped by normal recovery.
        member.getVariant().removeTag(
                Tags.VARIANT_ALWAYS_RETAIN_SMODS_ON_SALVAGE
        );
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
        pilot.setPortraitSprite(ESCORT_PORTRAIT);
        pilot.setRankId(Ranks.SPACE_LIEUTENANT);
        pilot.setPostId(Ranks.POST_OFFICER);
        pilot.setPersonality(config.optString("officer_personality", "steady"));

        pilot.getStats().setSkipRefresh(true);
        pilot.getStats().setLevel(config.optInt("officer_level", 1));
        applySkillLevels(pilot, config.optJSONObject("officer_skills"));
        pilot.getStats().setSkipRefresh(false);
        return pilot;
    }

    /**
     * Presentation and recovery state are mutable campaign data, so update
     * existing Last Line fleets in place instead of rebuilding them.
     */
    private static void syncExistingFleetState() {
        for (LocationAPI location : Global.getSector().getAllLocations()) {
            for (CampaignFleetAPI fleet : location.getFleets()) {
                if (!isActiveLastLineFleet(fleet)) continue;

                fleet.getMemoryWithoutUpdate().set(
                        NEX_AUTORESOLVE_STRENGTH_MULT_KEY,
                        NEX_AUTORESOLVE_STRENGTH_MULT
                );
                fleet.getMemoryWithoutUpdate().set(NEX_NO_KEEP_SMODS_KEY, true);
                fleet.getMemoryWithoutUpdate().set(
                        MemFlags.FLEET_FIGHT_TO_THE_LAST,
                        true
                );
                fleet.getMemoryWithoutUpdate().set(
                        MemFlags.FLEET_INTERACTION_DIALOG_CONFIG_OVERRIDE_GEN,
                        new LastLineFID()
                );

                PersonAPI subject71 = findSubject71(fleet);
                if (subject71 != null) {
                    subject71.setPersonality(Personalities.AGGRESSIVE);
                    subject71.getStats().setSkipRefresh(true);
                    subject71.getStats().setLevel(SUBJECT_71_LEVEL);
                    subject71.getStats().setSkipRefresh(false);
                }

                for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
                    PersonAPI captain = member.getCaptain();
                    member.getVariant().removeTag(
                            Tags.VARIANT_ALWAYS_RETAIN_SMODS_ON_SALVAGE
                    );
                    if (captain != null
                            && member.getHullSpec() != null
                            && !member.getHullSpec().getBaseHullId()
                            .startsWith("diableavionics_virtuous")) {
                        captain.setPortraitSprite(ESCORT_PORTRAIT);
                    }
                }
            }
        }
    }

    private static void applySkillLevels(PersonAPI person, JSONObject skills) {
        if (person == null || skills == null) return;

        java.util.Iterator<?> keys = skills.keys();
        while (keys.hasNext()) {
            String skillId = String.valueOf(keys.next());
            person.getStats().setSkillLevel(
                    skillId,
                    (float) skills.optDouble(skillId, 0f)
            );
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
