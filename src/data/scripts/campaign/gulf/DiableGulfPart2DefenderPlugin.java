package data.scripts.campaign.gulf;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageGenFromSeed;

import java.util.Random;

/**
 * Legacy salvage-defender plugin retained only so old saves can deserialize and remove it.
 * Gulf Part II no longer registers or calls this plugin.
 */
@Deprecated
public class DiableGulfPart2DefenderPlugin implements SalvageGenFromSeed.SalvageDefenderModificationPlugin {

    private static final String[] VAPOR_VARIANTS = {
            "diableavionics_vapor_standard",
            "diableavionics_vapor_attack",
            "diableavionics_vapor_brawler",
            "diableavionics_vapor_closeRange",
            "diableavionics_vapor_closequarter"
    };

    @Override
    public int getHandlingPriority(Object params) {
        if (!(params instanceof SalvageGenFromSeed.SDMParams)) return -1;
        SalvageGenFromSeed.SDMParams p = (SalvageGenFromSeed.SDMParams) params;
        if (p.entity != null && p.entity.getMemoryWithoutUpdate().getBoolean(DiableGulfPart2Intel.SITE_MEMKEY)) {
            return 1000;
        }
        return -1;
    }

    @Override
    public float getStrength(SalvageGenFromSeed.SDMParams p, float strength, Random random, boolean withOverride) {
        return strength;
    }

    @Override
    public float getProbability(SalvageGenFromSeed.SDMParams p, float probability, Random random, boolean withOverride) {
        return 1f;
    }

    @Override
    public float getQuality(SalvageGenFromSeed.SDMParams p, float quality, Random random, boolean withOverride) {
        return quality;
    }

    @Override
    public float getMaxSize(SalvageGenFromSeed.SDMParams p, float maxSize, Random random, boolean withOverride) {
        return 10f;
    }

    @Override
    public float getMinSize(SalvageGenFromSeed.SDMParams p, float minSize, Random random, boolean withOverride) {
        return 10f;
    }

    @Override
    public void modifyFleet(SalvageGenFromSeed.SDMParams p, CampaignFleetAPI fleet, Random random, boolean withOverride) {
        if (fleet == null) return;
        if (random == null) random = new Random();

        fleet.getFleetData().clear();
        fleet.setFaction(DiableGulfPart2Intel.ENEMY_FACTION_ID, true);
        fleet.setName("Diable Avionics ?");
        fleet.setNoFactionInName(true);

        FleetMemberAPI flagship = null;
        PersonAPI commander = null;
        for (int i = 0; i < 10; i++) {
            String variantId = VAPOR_VARIANTS[random.nextInt(VAPOR_VARIANTS.length)];
            FleetMemberAPI member = fleet.getFleetData().addFleetMember(variantId);
            member.setShipName(Global.getSector().getFaction("diableavionics").pickRandomShipName(random));
            PersonAPI pilot = createUnknownPilot();
            member.setCaptain(pilot);
            member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());

            if (flagship == null) {
                flagship = member;
                commander = pilot;
            }
        }

        if (flagship != null) fleet.getFleetData().setFlagship(flagship);
        if (commander != null) fleet.setCommander(commander);
        fleet.getFleetData().sort();
        fleet.forceSync();
        fleet.updateCounts();

        fleet.getMemoryWithoutUpdate().set(DiableGulfPart2Intel.DEFENDER_MEMKEY, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_FIGHT_TO_THE_LAST, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
    }

    private static PersonAPI createUnknownPilot() {
        PersonAPI pilot = Global.getFactory().createPerson();
        pilot.setFaction(DiableGulfPart2Intel.ENEMY_FACTION_ID);
        pilot.setPortraitSprite(DiableGulfPart2Intel.ENEMY_PORTRAIT);
        pilot.setRankId(Ranks.SPACE_COMMANDER);
        pilot.setPostId(Ranks.POST_OFFICER);
        pilot.setPersonality(Personalities.AGGRESSIVE);
        return pilot;
    }

    @Override
    public void reportDefeated(SalvageGenFromSeed.SDMParams p, SectorEntityToken entity, CampaignFleetAPI fleet) {
        // Completion and the guaranteed reward are handled by the post-defender rules dialog.
    }
}
