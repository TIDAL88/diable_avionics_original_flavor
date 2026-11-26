package data.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import org.json.JSONException;
import org.json.JSONObject;
import org.magiclib.bounty.ActiveBounty;
import org.magiclib.bounty.MagicBountyCoordinator;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DARiseOfTheDamned extends BaseCommandPlugin {
    private static final String TANIA_MOD_ID = "navarchy_of_tania";
    private static final String CREW_CONFIG = "data/config/modFiles/lost_and_found_crews.json";
    private static final String PAINTJOB_PREFIX = "ML_paintjob-diableavionics_carinasentinel_";

    public static void updateAvailability() {
        Global.getSector().getMemoryWithoutUpdate().set("$da_lost_and_found_tania_enabled",
                Global.getSettings().getModManager().isModEnabled(TANIA_MOD_ID));
    }

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog,
                           List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        String bountyId = params.get(0).getString(memoryMap);
        ActiveBounty bounty = MagicBountyCoordinator.getInstance().getActiveBounty(bountyId);
        if (bounty == null) return false;

        String edition = bountyId.endsWith("_tania") ? "tania" : "diable";
        String variantPrefix = "diableavionics_damned_" + edition + "_";
        try {
            JSONObject crews = Global.getSettings().loadJSON(CREW_CONFIG, "diableavionics");
            configureFleet(bounty.getFleet(), bounty.getCaptain(), bounty.getSpec().target_level,
                    bounty.getSpec().target_skills, crews, variantPrefix);
            return true;
        } catch (IOException | JSONException ex) {
            Global.getLogger(DARiseOfTheDamned.class).error("Unable to load Rise Of The Damned crews", ex);
            return false;
        }
    }

    static void configureFleet(CampaignFleetAPI fleet, PersonAPI commander, int commanderLevel,
                               Map<String, Integer> commanderSkills, JSONObject crews, String variantPrefix)
            throws JSONException {
        FleetDataAPI data = fleet.getFleetData();
        FactionAPI faction = fleet.getFaction();
        List<FleetMemberAPI> members = data.getMembersListCopy();
        Map<FleetMemberAPI, String> names = new LinkedHashMap<>();
        Map<FleetMemberAPI, PersonAPI> officers = new LinkedHashMap<>();
        Map<String, Integer> crewIndices = new LinkedHashMap<>();
        for (FleetMemberAPI member : members) {
            String variantId = member.getVariant().getHullVariantId();
            if (!variantId.startsWith(variantPrefix)) continue;
            String fit = variantId.substring(variantPrefix.length());
            int index = crewIndices.getOrDefault(fit, 0);
            JSONObject crew = crews.getJSONArray(fit).getJSONObject(index);
            crewIndices.put(fit, index + 1);
            names.put(member, crew.getString("ship_name"));
            JSONObject profile = crew.optJSONObject("officer");
            if (profile != null) {
                officers.put(member, createOfficer(faction, profile));
            }
        }

        for (OfficerDataAPI officer : data.getOfficersCopy()) {
            data.removeOfficer(officer.getPerson());
        }

        // Rodrigues commands the fleet while the Pandemonium keeps its saved combat pilot.
        commander.setFaction(faction.getId());
        applySkills(commander, commanderLevel, commanderSkills);
        fleet.setCommander(commander);
        for (Map.Entry<FleetMemberAPI, String> entry : names.entrySet()) {
            FleetMemberAPI member = entry.getKey();
            member.setShipName(entry.getValue());
            // MagicLib's optional-variant loader does not copy JSON variant tags.
            member.getVariant().addTag(PAINTJOB_PREFIX + member.getHullId().substring("diableavionics_".length()));
            PersonAPI officer = officers.get(member);
            if (officer != null) {
                data.addOfficer(officer);
            }
            member.setCaptain(officer);
        }
    }

    private static PersonAPI createOfficer(FactionAPI faction, JSONObject profile) throws JSONException {
        JSONObject configured = profile.getJSONObject("skills");
        Map<String, Integer> skills = new LinkedHashMap<>();
        for (Iterator<?> it = configured.keys(); it.hasNext(); ) {
            String id = (String) it.next();
            skills.put(id, configured.getInt(id));
        }

        PersonAPI officer = faction.createRandomPerson();
        officer.setName(new FullName(profile.getString("first_name"), profile.getString("last_name"),
                FullName.Gender.valueOf(profile.getString("gender"))));
        officer.setPortraitSprite(profile.getString("portrait"));
        officer.setRankId(Ranks.SPACE_LIEUTENANT);
        officer.setPostId(Ranks.POST_OFFICER);
        officer.setPersonality(profile.getString("personality"));
        applySkills(officer, profile.getInt("level"), skills);
        return officer;
    }

    private static void applySkills(PersonAPI person, int level, Map<String, Integer> skills) {
        MutableCharacterStatsAPI stats = person.getStats();
        stats.setSkipRefresh(true);
        try {
            stats.setLevel(level);
            for (MutableCharacterStatsAPI.SkillLevelAPI skill : stats.getSkillsCopy()) {
                String id = skill.getSkill().getId();
                if (!skills.containsKey(id)) stats.setSkillLevel(id, 0);
            }
            skills.forEach(stats::setSkillLevel);
        } finally {
            stats.setSkipRefresh(false);
        }
        stats.refreshCharacterStatsEffects();
    }
}
