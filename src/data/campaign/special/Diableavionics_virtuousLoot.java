package data.campaign.special;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import data.scripts.world.DiableavionicsGen;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicCampaign;

import java.util.List;

import static data.scripts.util.Diableavionics_stringsManager.txt;

/**
 * @author Tartiflette
 * @author After/Aero - Last Line protections
 */

public class Diableavionics_virtuousLoot implements FleetEventListener {

    private final String VIRTUOUS_DROP_ALREADY = "$virtuous_drop";
    private static final String VIRTUOUS_CLAIMED_MEMKEY =
            "$da_lastline_virtuous_claimed";
    private static final String VIRTUOUS_LOST_LAST_ENGAGEMENT =
            "$da_virtuous_lost_last_engagement";
    private static final String GUARDIAN_MAELSTROM_LOST_LAST_ENGAGEMENT =
            "$da_guardian_maelstrom_lost_last_engagement";
    private static final String GUARDIAN_MAELSTROM_NAME =
            "DSF Last Line-01";

    @Override
    public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
        boolean classicMode = DiableavionicsGen.useClassicLastLineFleet();

        // After the simulation claim this listener is normally removed. Keep
        // the guard for older serialized fleets: escort losses are handled by
        // Sivie's industry and can never count as another Virtuous death.
        if (!classicMode && Global.getSector().getMemoryWithoutUpdate().getBoolean(
                VIRTUOUS_CLAIMED_MEMKEY
        )) {
            return;
        }

        // ignore that whole ordeal if the Virtuous already dropped
        if (Global.getSector().getMemoryWithoutUpdate().contains(VIRTUOUS_DROP_ALREADY)
                && Global.getSector().getMemoryWithoutUpdate().getBoolean(VIRTUOUS_DROP_ALREADY)) {
            return;
        }

        if (!wasVirtuousLost(fleet)) {
            return;
        }

        // Remove surviving escorts after the Virtuous is lost.
        if (!fleet.getMembersWithFightersCopy().isEmpty()) {
            SectorEntityToken source = fleet.getCurrentAssignment() == null
                    ? fleet.getStarSystem().getCenter()
                    : fleet.getCurrentAssignment().getTarget();
            if (source == null) {
                source = fleet.getStarSystem().getCenter();
            }
            fleet.clearAssignments();
            fleet.addAssignment(
                    FleetAssignment.GO_TO_LOCATION_AND_DESPAWN,
                    source,
                    9999
            );
        }

        // NPC victories, including a battle where the player supported The
        // Last Line, must not consume or create the one-time player reward.
        if (!classicMode && (battle == null
                || !battle.isPlayerInvolved()
                || battle.onPlayerSide(fleet))) {
            return;
        }

        //boss is dead,
        boolean salvaged = false;
        for (FleetMemberAPI f : Global.getSector().getPlayerFleet()
                .getFleetData().getMembersListCopy()) {
            if (f.getHullId().startsWith("diableavionics_virtuous")) {
                salvaged = true;
            }
        }
        Global.getSector().getMemoryWithoutUpdate().set(
                VIRTUOUS_DROP_ALREADY,
                true
        );

        //spawn a derelict if it wasn't salvaged
        if (!salvaged) {

            //check around if there is an existing wreck to remove just in case
            List<SectorEntityToken> wrecks = fleet.getStarSystem()
                    .getEntitiesWithTag(Tags.WRECK);
            if (!wrecks.isEmpty()) {
                for (SectorEntityToken t : wrecks) {
                    if (t.getCustomEntitySpec() != null
                            && t.getCustomEntitySpec().getSpriteName()
                            .startsWith("diableavionics_virtuous")) {
                        fleet.getStarSystem().removeEntity(t);
                        break;
                    }
                }
            }

            //make sure there is a valid location to avoid spawning in the sun
            Vector2f location = fleet.getLocation();
            if (location.lengthSquared() == 0f && primaryWinner != null) {
                location = primaryWinner.getLocation();
            }

            //spawn the derelict object
            SectorEntityToken wreck = MagicCampaign.createDerelict(
                    "diableavionics_virtuous_destroyed_Hull",
                    ShipRecoverySpecial.ShipCondition.WRECKED,
                    false,
                    -1,
                    true,
                    //orbitCenter,angle,radius,period);
                    fleet.getStarSystem().getCenter(),
                    VectorUtils.getAngle(
                            fleet.getStarSystem().getCenter().getLocation(),
                            location
                    ),
                    MathUtils.getDistance(
                            fleet.getStarSystem().getCenter().getLocation(),
                            location
                    ),
                    360
            );
            MagicCampaign.placeOnStableOrbit(wreck, true);
            wreck.setName(txt("virtuousShip"));
            wreck.setFacing((float) Math.random() * 360f);
            wreck.getMemoryWithoutUpdate().set(
                    MemFlags.ENTITY_MISSION_IMPORTANT,
                    true
            );
        }

        if (!classicMode) {
            // Virtuous is permanently gone from The Last Line whether Tart's
            // existing ownership check created or suppressed a duplicate wreck.
            Global.getSector().getMemoryWithoutUpdate().set(
                    VIRTUOUS_CLAIMED_MEMKEY,
                    true
            );
        }
    }

    public static void recordLastLineEngagementOutcome(
            CampaignFleetAPI fleet,
            EngagementResultAPI result
    ) {
        FleetMemberAPI virtuous = findVirtuous(fleet);
        boolean lost = virtuous != null
                && (containsMember(
                        result.getWinnerResult().getDestroyed(),
                        virtuous
                ) || containsMember(
                        result.getWinnerResult().getDisabled(),
                        virtuous
                ) || containsMember(
                        result.getLoserResult().getDestroyed(),
                        virtuous
                ) || containsMember(
                        result.getLoserResult().getDisabled(),
                        virtuous
                ));
        fleet.getMemoryWithoutUpdate().set(
                VIRTUOUS_LOST_LAST_ENGAGEMENT,
                lost
        );

        FleetMemberAPI maelstrom = findGuardianMaelstrom(fleet);
        boolean maelstromLost = maelstrom != null
                && (containsMember(
                        result.getWinnerResult().getDestroyed(),
                        maelstrom
                ) || containsMember(
                        result.getWinnerResult().getDisabled(),
                        maelstrom
                ) || containsMember(
                        result.getLoserResult().getDestroyed(),
                        maelstrom
                ) || containsMember(
                        result.getLoserResult().getDisabled(),
                        maelstrom
                ));
        fleet.getMemoryWithoutUpdate().set(
                GUARDIAN_MAELSTROM_LOST_LAST_ENGAGEMENT,
                maelstromLost
        );
    }

    public static void clearLastLineEngagementOutcome(CampaignFleetAPI fleet) {
        fleet.getMemoryWithoutUpdate().unset(
                VIRTUOUS_LOST_LAST_ENGAGEMENT
        );
        fleet.getMemoryWithoutUpdate().unset(
                GUARDIAN_MAELSTROM_LOST_LAST_ENGAGEMENT
        );
    }

    public static boolean wasVirtuousLost(CampaignFleetAPI fleet) {
        if (fleet.getMemoryWithoutUpdate().contains(
                VIRTUOUS_LOST_LAST_ENGAGEMENT
        )) {
            return fleet.getMemoryWithoutUpdate().getBoolean(
                    VIRTUOUS_LOST_LAST_ENGAGEMENT
            );
        }
        return findVirtuous(fleet) == null;
    }

    public static boolean wasGuardianMaelstromLost(CampaignFleetAPI fleet) {
        if (fleet.getMemoryWithoutUpdate().contains(
                GUARDIAN_MAELSTROM_LOST_LAST_ENGAGEMENT
        )) {
            return fleet.getMemoryWithoutUpdate().getBoolean(
                    GUARDIAN_MAELSTROM_LOST_LAST_ENGAGEMENT
            );
        }
        return findGuardianMaelstrom(fleet) == null;
    }

    private static boolean containsMember(
            List<FleetMemberAPI> members,
            FleetMemberAPI target
    ) {
        for (FleetMemberAPI member : members) {
            if (member == target || member.getId().equals(target.getId())) {
                return true;
            }
        }
        return false;
    }

    private static FleetMemberAPI findVirtuous(CampaignFleetAPI fleet) {
        for (FleetMemberAPI member
                : fleet.getFleetData().getMembersListCopy()) {
            if (member.getHullSpec() != null
                    && member.getHullSpec().getBaseHullId()
                    .startsWith("diableavionics_virtuous")) {
                return member;
            }
        }
        return null;
    }

    private static FleetMemberAPI findGuardianMaelstrom(
            CampaignFleetAPI fleet
    ) {
        for (FleetMemberAPI member
                : fleet.getFleetData().getMembersListCopy()) {
            if (GUARDIAN_MAELSTROM_NAME.equals(member.getShipName())) {
                return member;
            }
        }
        return null;
    }

    @Override
    public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, CampaignEventListener.FleetDespawnReason reason, Object param) {
        fleet.removeEventListener(this);
    }
}
