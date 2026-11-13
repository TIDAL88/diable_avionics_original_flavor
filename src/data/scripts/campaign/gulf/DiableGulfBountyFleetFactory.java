package data.scripts.campaign.gulf;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FactionDoctrineAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.util.WeightedRandomPicker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static data.scripts.util.Diableavionics_stringsManager.txt;

/** Standalone Gulf fleet, using the combat parameters of SWP's Gulf IBB stage. */
public final class DiableGulfBountyFleetFactory {
    private static final int ESCORT_COMBAT_FP = 245;
    private static final float ESCORT_QUALITY = 1.5f;
    private static final int ESCORT_SMODS = 2;
    private static final int CAPTAIN_LEVEL = 10;
    private static final int CAPTAIN_ELITES = 5;
    private static final int COMMANDER_SKILLS = 2;
    private static final int OFFICER_COUNT = 11;
    private static final int OFFICER_LEVEL = 7;
    private static final int OFFICER_ELITES = 3;

    private DiableGulfBountyFleetFactory() { }

    public static CampaignFleetAPI createFleet() {
        Random random = new Random();
        ShipVariantAPI variant = Global.getSettings().getVariant("diableavionics_IBBgulf_boss").clone();
        variant.setSource(VariantSource.REFIT);
        variant.addTag(Tags.TAG_NO_AUTOFIT);
        variant.addTag(Tags.VARIANT_ALWAYS_RETAIN_SMODS_ON_SALVAGE);
        variant.addTag(Tags.VARIANT_ALWAYS_RECOVERABLE);
        FleetMemberAPI flagship = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant);
        flagship.setShipName(txt("gulfShip"));

        FactionAPI faction = Global.getSector().getFaction(Factions.TRITACHYON);
        PersonAPI captain = OfficerManagerEvent.createOfficer(faction, CAPTAIN_LEVEL,
                FleetFactoryV3.getSkillPrefForShip(flagship), true, null, true, true, CAPTAIN_ELITES, random);
        captain.getName().setFirst(txt("gulfFN"));
        captain.getName().setLast(txt("gulfLN"));
        captain.setGender(FullName.Gender.FEMALE);
        captain.setPortraitSprite(Global.getSettings().getSpriteName("characters", "da_gulf"));
        captain.setRankId(Ranks.SPECIAL_AGENT);
        captain.setPostId(Ranks.POST_FLEET_COMMANDER);
        captain.setPersonality(Personalities.STEADY);

        CampaignFleetAPI fleet = FleetFactoryV3.createFleet(createEscortParams(captain));
        if (fleet == null || fleet.isEmpty()) {
            Global.getLogger(DiableGulfBountyFleetFactory.class).warn("Unable to create the standalone Gulf escort");
            return null;
        }
        fleet.getFleetData().addFleetMember(flagship);
        fleet.getFleetData().setFlagship(flagship);
        flagship.setCaptain(captain);
        fleet.setCommander(captain);
        fleet.setName(txt("gulfFleet"));
        fleet.setNoFactionInName(true);
        fleet.forceSync();

        addEscortOfficers(fleet, random);
        addCommanderSkills(captain, random);
        fleet.getFleetData().sort();
        fleet.forceSync();
        fleet.updateCounts();
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
        }
        return fleet;
    }

    private static FleetParamsV3 createEscortParams(PersonAPI captain) {
        FactionDoctrineAPI doctrine = Global.getSector().getFaction(Factions.TRITACHYON).getDoctrine().clone();
        // Match IBB's preference for larger hulls at this combat budget.
        if (doctrine.getShipSize() <= 2) doctrine.setShipSize(doctrine.getShipSize() + 1);
        FleetParamsV3 params = new FleetParamsV3(null, null, Factions.TRITACHYON,
                ESCORT_QUALITY, FleetTypes.TASK_FORCE, ESCORT_COMBAT_FP, 0f, 0f, 0f, 0f, 0f, 0f);
        params.doctrineOverride = doctrine;
        params.commander = captain;
        params.withOfficers = false;
        params.noCommanderSkills = true;
        params.ignoreMarketFleetSizeMult = true;
        params.forceAllowPhaseShipsEtc = true;
        params.modeOverride = FactionAPI.ShipPickMode.PRIORITY_THEN_ALL;
        params.doNotPrune = true;
        params.averageSMods = ESCORT_SMODS;
        return params;
    }

    private static void addEscortOfficers(CampaignFleetAPI fleet, Random random) {
        WeightedRandomPicker<FleetMemberAPI> ships = new WeightedRandomPicker<>(random);
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            if (member.isFighterWing() || member.isFlagship() || !member.getCaptain().isDefault()) continue;
            float weight = member.getFleetPointCost() * (member.isCivilian() ? 0.0001f : 1f);
            ships.add(member, weight);
        }
        for (int i = 0; i < OFFICER_COUNT; i++) {
            FleetMemberAPI ship = ships.pickAndRemove();
            if (ship == null) break;
            PersonAPI officer = OfficerManagerEvent.createOfficer(fleet.getFaction(), OFFICER_LEVEL,
                    FleetFactoryV3.getSkillPrefForShip(ship), true, fleet, true, true, OFFICER_ELITES, random);
            if (Personalities.TIMID.equals(officer.getPersonalityAPI().getId())) {
                officer.setPersonality(Personalities.CAUTIOUS);
            }
            ship.setCaptain(officer);
        }
    }

    private static void addCommanderSkills(PersonAPI captain, Random random) {
        // The vanilla skill pool used by SWP's famous_bounty doctrine.
        List<String> skills = new ArrayList<>(Arrays.asList(Skills.CREW_TRAINING, Skills.COORDINATED_MANEUVERS,
                Skills.ELECTRONIC_WARFARE, Skills.FIGHTER_UPLINK, Skills.CARRIER_GROUP, Skills.FLUX_REGULATION));
        Collections.shuffle(skills, random);
        captain.getStats().setSkipRefresh(true);
        for (int i = 0; i < COMMANDER_SKILLS; i++) captain.getStats().setSkillLevel(skills.get(i), 1);
        captain.getStats().setSkipRefresh(false);
        captain.getStats().refreshCharacterStatsEffects();
    }
}
