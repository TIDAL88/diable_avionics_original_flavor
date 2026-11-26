package data.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
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

public class DASignalLost extends BaseCommandPlugin {
    private static final String BOUNTY_ID = "diable_pressure_check";
    private static final String OFFICER_CONFIG = "data/config/modFiles/pressure_check_officers.json";
    private static final List<String> SHIP_DESIGNATIONS = List.of(
            "Alpha", "Beta", "Gamma", "Delta", "Epsilon", "Zeta", "Eta", "Theta",
            "Iota", "Kappa", "Lambda", "Mu", "Nu", "Xi", "Omicron", "Pi",
            "Rho", "Sigma", "Tau", "Upsilon", "Phi", "Chi", "Psi", "Omega"
    );

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog,
                           List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        ActiveBounty bounty = MagicBountyCoordinator.getInstance().getActiveBounty(BOUNTY_ID);
        if (bounty == null || bounty.getFleet() == null || bounty.getCaptain() == null) {
            return false;
        }

        try {
            JSONObject profiles = Global.getSettings().loadJSON(OFFICER_CONFIG, "diableavionics");
            configureFleet(bounty.getFleet(), bounty.getCaptain(), bounty.getSpec().target_level,
                    bounty.getSpec().target_skills, profiles);
            return true;
        } catch (IOException | JSONException ex) {
            Global.getLogger(DASignalLost.class).error("Unable to load SIGNAL LOST officers", ex);
            return false;
        }
    }

    static void configureFleet(CampaignFleetAPI fleet, PersonAPI commander, int commanderLevel,
                               Map<String, Integer> commanderSkills, JSONObject profiles) throws JSONException {
        FleetDataAPI data = fleet.getFleetData();
        FleetMemberAPI flagship = fleet.getFlagship();
        List<FleetMemberAPI> members = data.getMembersListCopy();
        FactionAPI faction = fleet.getFaction();

        Map<FleetMemberAPI, PersonAPI> escorts = new LinkedHashMap<>();
        for (FleetMemberAPI member : members) {
            if (member == flagship) continue;
            JSONObject profile = profiles.optJSONObject(member.getVariant().getHullVariantId());
            if (profile != null) {
                escorts.put(member, createOfficer(faction, profile));
            }
        }

        // Replace MagicLib's generated escorts and any extra doctrine skills on the commander.
        for (OfficerDataAPI officer : data.getOfficersCopy()) {
            if (officer.getPerson() != commander) {
                data.removeOfficer(officer.getPerson());
            }
        }

        commander.setFaction(faction.getId());
        applySkills(commander, commanderLevel, commanderSkills);
        fleet.setCommander(commander);
        String fleetPortrait = commander.getPortraitSprite();
        int gauntletIndex = 0;
        int velvetIndex = 0;
        int ironIndex = 0;
        for (FleetMemberAPI member : members) {
            PersonAPI captain = member == flagship ? commander : escorts.get(member);
            if (captain != null && captain != commander) {
                captain.setPortraitSprite(fleetPortrait);
                data.addOfficer(captain);
            }
            member.setCaptain(captain);
            if (member == flagship) {
                member.setShipName("Gauntlet Actual");
            } else if ("diableavionics_maelstrom".equals(member.getHullId())) {
                member.setShipName(getShipName("Gauntlet", gauntletIndex++));
            } else if (captain != null) {
                member.setShipName(getShipName("Velvet Recon", velvetIndex++));
            } else {
                member.setShipName(getShipName("Iron Recon", ironIndex++));
            }
        }
    }

    private static String getShipName(String group, int index) {
        int count = SHIP_DESIGNATIONS.size();
        String designation = SHIP_DESIGNATIONS.get(index % count);
        int series = index / count + 1;
        return group + " " + designation + (series == 1 ? "" : " " + series);
    }

    private static PersonAPI createOfficer(FactionAPI faction, JSONObject profile) throws JSONException {
        JSONObject configuredSkills = profile.getJSONObject("officer_skills");
        Map<String, Integer> skills = new LinkedHashMap<>();
        for (Iterator<?> it = configuredSkills.keys(); it.hasNext(); ) {
            String id = (String) it.next();
            skills.put(id, configuredSkills.getInt(id));
        }

        PersonAPI officer = faction.createRandomPerson();
        officer.setRankId(Ranks.SPACE_LIEUTENANT);
        officer.setPostId(Ranks.POST_OFFICER);
        officer.setPersonality(profile.getString("officer_personality"));
        applySkills(officer, profile.getInt("officer_level"), skills);
        return officer;
    }

    private static void applySkills(PersonAPI person, int level, Map<String, Integer> skills) {
        MutableCharacterStatsAPI stats = person.getStats();
        stats.setSkipRefresh(true);
        try {
            stats.setLevel(level);
            for (MutableCharacterStatsAPI.SkillLevelAPI skill : stats.getSkillsCopy()) {
                String id = skill.getSkill().getId();
                if (!skills.containsKey(id)) {
                    stats.setSkillLevel(id, 0);
                }
            }
            skills.forEach(stats::setSkillLevel);
        } finally {
            stats.setSkipRefresh(false);
        }
        stats.refreshCharacterStatsEffects();
    }
}
