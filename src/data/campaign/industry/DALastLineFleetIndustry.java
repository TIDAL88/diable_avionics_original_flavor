package data.campaign.industry;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import data.campaign.special.Diableavionics_virtuousLoot;
import data.scripts.campaign.lastline.DiableLastLineFleetFactory;
import data.scripts.world.DiableavionicsGen;
import org.magiclib.bounty.MagicBountyCoordinator;

/**
 * Hidden Sivie infrastructure that owns The Last Line campaign fleet.
 *
 * NPC Virtuous losses are rebuilt after thirty days. A simulation transfer or
 * player-created Virtuous wreck instead leaves a renewable escort-only
 * guardian fleet behind.
 *
 * @author After/Aero
 */
public class DALastLineFleetIndustry extends BaseIndustry
        implements FleetEventListener {

    public static final String INDUSTRY_ID =
            "diableavionics_last_line_support";
    public static final String RESOLVED_MEMKEY =
            "$da_last_line_permanently_resolved";
    public static final String RETIRING_FLEET_MEMKEY =
            "$da_last_line_retiring";

    private static final String DIABLE_FACTION_ID = "diableavionics";
    private static final String VIRTUOUS_CLAIMED_MEMKEY =
            "$da_lastline_virtuous_claimed";
    private static final String VIRTUOUS_DROP_MEMKEY = "$virtuous_drop";
    private static final String SAVE_THE_CHILDREN_BOUNTY_KEY =
            "diable_virtuous";
    private static final float RESPAWN_DELAY_DAYS = 30f;
    private static final float PLAYER_ATTRITION_RESTORE_DELAY_DAYS = 10f;

    private CampaignFleetAPI fleet;
    private CampaignFleetAPI fleetPendingListenerRemoval;
    private float respawnDelayDays;
    private boolean resetBountyOnNextSpawn;
    private float playerAttritionRestoreDelayDays;
    private boolean playerAttritionStateInitialized;

    @Override
    public void apply() {
        super.apply(true);
    }

    @Override
    public void unapply() {
        super.unapply();
    }

    @Override
    public boolean isHidden() {
        return true;
    }

    @Override
    public boolean isAvailableToBuild() {
        return false;
    }

    @Override
    public boolean showWhenUnavailable() {
        return false;
    }

    @Override
    public boolean canBeDisrupted() {
        return false;
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);
        if (Global.getSector() == null) {
            return;
        }

        detachPendingListener();
        if (Global.getSector().getEconomy().isSimMode()) {
            return;
        }
        if (DiableavionicsGen.useClassicLastLineFleet()) {
            disableForClassicMode();
            return;
        }

        synchronizeResolvedState();
        if (isPermanentlyResolved() || !canSupportFleet()) {
            return;
        }

        if (fleet != null) {
            if (isActiveLastLineFleet(fleet)) {
                ensureListener(fleet);
                if (isVirtuousClaimed()) {
                    if (fleet.getMemoryWithoutUpdate().getInt(
                            DiableLastLineFleetFactory.FLEET_VERSION_MEMKEY
                    ) < DiableLastLineFleetFactory.FLEET_VERSION
                            && Diableavionics_virtuousLoot
                            .wasGuardianMaelstromLost(fleet)) {
                        retireSurvivingFleet(fleet);
                        scheduleRespawn(false);
                        return;
                    }
                    DiableLastLineFleetFactory
                            .upgradeGuardianFleetIfNeeded(fleet);
                }
                initializePlayerAttritionState();
                advancePlayerAttritionRestoration(amount);
                return;
            }
            scheduleRespawn(!isVirtuousClaimed());
        }

        if (respawnDelayDays > 0f) {
            respawnDelayDays -= Global.getSector().getClock()
                    .convertToDays(amount);
            if (respawnDelayDays > 0f) {
                return;
            }
            respawnDelayDays = 0f;
        }

        CampaignFleetAPI existing = findExistingFleet();
        if (existing != null) {
            adoptFleet(existing);
            return;
        }

        CampaignFleetAPI spawned = DiableavionicsGen.spawnVirtuous(
                market.getPrimaryEntity()
        );
        if (spawned == null) {
            respawnDelayDays = 1f;
            return;
        }

        adoptFleet(spawned);
        if (resetBountyOnNextSpawn && !isVirtuousClaimed()) {
            MagicBountyCoordinator.getInstance().resetBounty(
                    SAVE_THE_CHILDREN_BOUNTY_KEY
            );
            resetBountyOnNextSpawn = false;
        }
    }

    /** Attaches this industry to a pre-manager fleet in an existing save. */
    public void adoptExistingFleetIfPresent() {
        if (DiableavionicsGen.useClassicLastLineFleet()) {
            disableForClassicMode();
            return;
        }

        synchronizeResolvedState();
        if (isPermanentlyResolved()) return;

        CampaignFleetAPI existing = findExistingFleet();
        if (existing != null) {
            if (fleet != existing) {
                adoptFleet(existing);
            } else {
                ensureListener(existing);
            }
        }
    }

    @Override
    public void reportBattleOccurred(
            CampaignFleetAPI reportedFleet,
            CampaignFleetAPI primaryWinner,
            BattleAPI battle
    ) {
        if (DiableavionicsGen.useClassicLastLineFleet()) {
            disableForClassicMode();
            return;
        }
        if (reportedFleet != fleet) return;

        if (isVirtuousClaimed()) {
            if (Diableavionics_virtuousLoot
                    .wasGuardianMaelstromLost(reportedFleet)) {
                retireSurvivingFleet(reportedFleet);
                scheduleRespawn(false);
                return;
            }
            if (reportedFleet.getMembersWithFightersCopy().isEmpty()) {
                scheduleRespawn(false);
            } else {
                handleSurvivingFleetBattle(reportedFleet, battle);
            }
            return;
        }

        if (!Diableavionics_virtuousLoot.wasVirtuousLost(reportedFleet)) {
            handleSurvivingFleetBattle(reportedFleet, battle);
            return;
        }

        boolean playerDestroyedLastLine = battle != null
                && battle.isPlayerInvolved()
                && !battle.onPlayerSide(reportedFleet);
        if (playerDestroyedLastLine) {
            // If the loot listener creates the unique wreck, it switches the
            // manager to the renewable escort-only guardian state.
            scheduleRespawn(false);
            return;
        }

        scheduleRespawn(true);
    }

    @Override
    public void reportFleetDespawnedToListener(
            CampaignFleetAPI reportedFleet,
            CampaignEventListener.FleetDespawnReason reason,
            Object param
    ) {
        if (DiableavionicsGen.useClassicLastLineFleet()) {
            disableForClassicMode();
            return;
        }
        if (reportedFleet != fleet) return;

        if (isPermanentlyResolved()) {
            queueListenerRemoval(reportedFleet);
            fleet = null;
        } else {
            scheduleRespawn(!isVirtuousClaimed());
        }
    }

    private void handleSurvivingFleetBattle(
            CampaignFleetAPI reportedFleet,
            BattleAPI battle
    ) {
        playerAttritionStateInitialized = true;
        if (battle != null && battle.isPlayerInvolved()) {
            if (DiableLastLineFleetFactory.hasMissingAuthoredEscorts(
                    reportedFleet
            )) {
                playerAttritionRestoreDelayDays =
                        PLAYER_ATTRITION_RESTORE_DELAY_DAYS;
            } else {
                playerAttritionRestoreDelayDays = 0f;
            }
        } else if (playerAttritionRestoreDelayDays <= 0f) {
            DiableLastLineFleetFactory.restoreMissingAuthoredEscorts(
                    reportedFleet
            );
        }
    }

    private void scheduleRespawn(boolean resetBounty) {
        if (fleet != null) {
            queueListenerRemoval(fleet);
        }
        fleet = null;
        respawnDelayDays = RESPAWN_DELAY_DAYS;
        resetBountyOnNextSpawn |= resetBounty;
        playerAttritionRestoreDelayDays = 0f;
        playerAttritionStateInitialized = false;
    }

    private void retireSurvivingFleet(CampaignFleetAPI retiringFleet) {
        retiringFleet.getMemoryWithoutUpdate().set(
                RETIRING_FLEET_MEMKEY,
                true
        );
        if (retiringFleet.getMembersWithFightersCopy().isEmpty()) return;

        SectorEntityToken destination = market == null
                ? null
                : market.getPrimaryEntity();
        if (destination == null
                && retiringFleet.getCurrentAssignment() != null) {
            destination = retiringFleet.getCurrentAssignment().getTarget();
        }
        if (destination == null) return;

        retiringFleet.clearAssignments();
        retiringFleet.addAssignment(
                FleetAssignment.GO_TO_LOCATION_AND_DESPAWN,
                destination,
                9999f
        );
    }

    private void disableForClassicMode() {
        if (fleet != null) {
            queueListenerRemoval(fleet);
            DiableLastLineFleetFactory.removeManagedStateForClassicFleet(
                    fleet
            );
        }
        fleet = null;
        respawnDelayDays = 0f;
        resetBountyOnNextSpawn = false;
        playerAttritionRestoreDelayDays = 0f;
        playerAttritionStateInitialized = false;
    }

    private void queueListenerRemoval(CampaignFleetAPI targetFleet) {
        if (targetFleet != null) {
            fleetPendingListenerRemoval = targetFleet;
        }
    }

    private void detachPendingListener() {
        CampaignFleetAPI pending = fleetPendingListenerRemoval;
        fleetPendingListenerRemoval = null;
        if (pending != null) {
            pending.removeEventListener(this);
        }
    }

    private void adoptFleet(CampaignFleetAPI newFleet) {
        fleet = newFleet;
        respawnDelayDays = 0f;
        if (isVirtuousClaimed()) {
            DiableLastLineFleetFactory.upgradeGuardianFleetIfNeeded(newFleet);
        }
        playerAttritionRestoreDelayDays =
                DiableLastLineFleetFactory.hasMissingAuthoredEscorts(newFleet)
                        ? PLAYER_ATTRITION_RESTORE_DELAY_DAYS
                        : 0f;
        playerAttritionStateInitialized = true;
        ensureListener(newFleet);
    }

    private void initializePlayerAttritionState() {
        if (playerAttritionStateInitialized) return;

        playerAttritionStateInitialized = true;
        if (DiableLastLineFleetFactory.hasMissingAuthoredEscorts(fleet)) {
            playerAttritionRestoreDelayDays =
                    PLAYER_ATTRITION_RESTORE_DELAY_DAYS;
        }
    }

    private void advancePlayerAttritionRestoration(float amount) {
        if (playerAttritionRestoreDelayDays <= 0f) return;

        playerAttritionRestoreDelayDays -= Global.getSector().getClock()
                .convertToDays(amount);
        if (playerAttritionRestoreDelayDays > 0f) return;

        playerAttritionRestoreDelayDays = 0f;
        DiableLastLineFleetFactory.restoreMissingAuthoredEscorts(fleet);
    }

    private void ensureListener(CampaignFleetAPI targetFleet) {
        for (FleetEventListener listener : targetFleet.getEventListeners()) {
            if (listener == this) return;
        }
        targetFleet.addEventListener(this);
    }

    private boolean canSupportFleet() {
        return market != null
                && market.getPrimaryEntity() != null
                && DIABLE_FACTION_ID.equals(market.getFactionId());
    }

    private void synchronizeResolvedState() {
        if (isVirtuousClaimed()) {
            // A transfer or spawned wreck resolves the unique ship, not
            // Sivie's renewable escort guard. Clear only the obsolete fleet
            // resolution flag; the wreck/drop flag must remain one-time.
            Global.getSector().getMemoryWithoutUpdate().unset(RESOLVED_MEMKEY);
            resetBountyOnNextSpawn = false;
            return;
        }

        if (isPermanentlyResolved()) return;

        if (Global.getSector().getMemoryWithoutUpdate().getBoolean(
                VIRTUOUS_DROP_MEMKEY
        )) {
            markPermanentlyResolved();
        }
    }

    private boolean isVirtuousClaimed() {
        return Global.getSector() != null
                && Global.getSector().getMemoryWithoutUpdate().getBoolean(
                VIRTUOUS_CLAIMED_MEMKEY
        );
    }

    private boolean isPermanentlyResolved() {
        return Global.getSector() != null
                && Global.getSector().getMemoryWithoutUpdate().getBoolean(
                RESOLVED_MEMKEY
        );
    }

    public static void markPermanentlyResolved() {
        if (Global.getSector() != null) {
            Global.getSector().getMemoryWithoutUpdate().set(
                    RESOLVED_MEMKEY,
                    true
            );
        }
    }

    private CampaignFleetAPI findExistingFleet() {
        if (Global.getSector() == null) return null;

        for (LocationAPI location : Global.getSector().getAllLocations()) {
            for (CampaignFleetAPI candidate : location.getFleets()) {
                if (isActiveLastLineFleet(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private boolean isActiveLastLineFleet(CampaignFleetAPI candidate) {
        if (candidate == null
                || candidate.isPlayerFleet()
                || candidate.isDespawning()
                || !candidate.isAlive()
                || candidate.getMemoryWithoutUpdate().getBoolean(
                RETIRING_FLEET_MEMKEY
        )) {
            return false;
        }

        boolean managedGuardian = candidate.getMemoryWithoutUpdate()
                .getBoolean(DiableLastLineFleetFactory.GUARDIAN_FLEET_MEMKEY)
                || DiableLastLineFleetFactory.FLEET_ID.equals(candidate.getId())
                || candidate.getMemoryWithoutUpdate().getInt(
                DiableLastLineFleetFactory.FLEET_VERSION_MEMKEY
        ) > 0;
        if (!managedGuardian) return false;

        candidate.getMemoryWithoutUpdate().set(
                DiableLastLineFleetFactory.GUARDIAN_FLEET_MEMKEY,
                true
        );

        if (isVirtuousClaimed()) {
            return !hasVirtuous(candidate);
        }
        return candidate.getMemoryWithoutUpdate().getBoolean("$virtuous")
                && hasVirtuous(candidate);
    }

    private boolean hasVirtuous(CampaignFleetAPI candidate) {
        if (candidate == null) return false;

        for (FleetMemberAPI member
                : candidate.getFleetData().getMembersListCopy()) {
            if (member.getHullSpec() != null
                    && member.getHullSpec().getBaseHullId()
                    .startsWith("diableavionics_virtuous")) {
                return true;
            }
        }
        return false;
    }
}
