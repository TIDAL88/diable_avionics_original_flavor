package data.scripts.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.listeners.CampaignInputListener;
import com.fs.starfarer.api.campaign.listeners.CoreUITabListener;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import data.hullmods.RimeFlightDeckController;

import java.util.List;

public class RimeFlightDeckManager implements EveryFrameScript, CoreUITabListener, CampaignInputListener {
    private static final float CHECK_INTERVAL = 0.5f;
    private static final int INPUT_GRACE_FRAMES = 2;

    private float elapsed;
    private boolean refitOpen;
    private boolean refitObserved;
    private int inputGraceFrames;

    public void synchronize() {
        refitOpen = isRefitOpen();
        refitObserved = refitOpen;
        synchronizePlayerRimes(refitOpen);
    }

    @Override
    public void reportAboutToOpenCoreTab(CoreUITabId tab, Object param) {
        if (tab == CoreUITabId.REFIT) {
            refitOpen = true;
            refitObserved = false;
            inputGraceFrames = 0;
            synchronizePlayerRimes(true);
        } else {
            boolean restoreStandby = refitOpen || inputGraceFrames > 0;
            refitOpen = false;
            refitObserved = false;
            inputGraceFrames = 0;
            if (restoreStandby) synchronizePlayerRimes(false);
        }
    }

    @Override
    public int getListenerInputPriority() {
        return 100;
    }

    @Override
    public void processCampaignInputPreCore(List<InputEventAPI> events) {
        if (refitOpen || isRefitOpen() || !couldOpenRefit(events)) return;

        inputGraceFrames = INPUT_GRACE_FRAMES;
        synchronizePlayerRimes(true);
    }

    @Override
    public void processCampaignInputPreFleetControl(List<InputEventAPI> events) {
    }

    @Override
    public void processCampaignInputPostCore(List<InputEventAPI> events) {
        if (refitOpen || isRefitOpen()) inputGraceFrames = 0;
    }

    @Override
    public void advance(float amount) {
        if (Global.getSector() == null) return;

        boolean currentlyInRefit = isRefitOpen();
        if (refitOpen) {
            if (currentlyInRefit) {
                refitObserved = true;
                return;
            }
            if (!refitObserved) return;

            refitOpen = false;
            refitObserved = false;
            elapsed = 0f;
            synchronizePlayerRimes(false);
            return;
        }

        if (currentlyInRefit) {
            refitOpen = true;
            refitObserved = true;
            inputGraceFrames = 0;
            elapsed = 0f;
            synchronizePlayerRimes(true);
            return;
        }

        if (inputGraceFrames > 0) {
            inputGraceFrames--;
            if (inputGraceFrames == 0) synchronizePlayerRimes(false);
            return;
        }

        elapsed += amount;
        if (elapsed >= CHECK_INTERVAL) {
            elapsed = 0f;
            synchronizePlayerRimes(false);
        }
    }

    private void synchronizePlayerRimes(boolean forceActive) {
        if (Global.getSector() == null) return;

        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null || fleet.getFleetData() == null) return;

        FleetDataAPI fleetData = fleet.getFleetData();
        boolean fleetChanged = false;

        for (FleetMemberAPI member : fleetData.getMembersListCopy()) {
            if (!isLogisticsRime(member)) continue;

            ShipVariantAPI variant = member.getVariant();
            if (variant == null) continue;

            boolean active = forceActive || RimeFlightDeckController.hasFittedWing(variant);
            boolean changed = setState(variant, active);
            if (changed) {
                member.updateStats();
                fleetChanged = true;
            }
        }

        if (fleetChanged) {
            fleetData.setSyncNeeded();
            fleetData.syncIfNeeded();
        }
    }

    private boolean setState(ShipVariantAPI variant, boolean active) {
        String wanted = active
                ? RimeFlightDeckController.ACTIVE_HULLMOD_ID
                : RimeFlightDeckController.STANDBY_HULLMOD_ID;
        String unwanted = active
                ? RimeFlightDeckController.STANDBY_HULLMOD_ID
                : RimeFlightDeckController.ACTIVE_HULLMOD_ID;

        boolean changed = false;
        if (variant.getPermaMods().contains(unwanted)) {
            variant.removePermaMod(unwanted);
            changed = true;
        }
        if (!variant.hasHullMod(wanted)) {
            variant.addPermaMod(wanted);
            changed = true;
        }
        return changed;
    }

    private boolean isLogisticsRime(FleetMemberAPI member) {
        if (member == null || member.getHullSpec() == null) return false;

        ShipHullSpecAPI spec = member.getHullSpec();
        return RimeFlightDeckController.RIME_HULL_ID.equals(spec.getHullId())
                || RimeFlightDeckController.RIME_HULL_ID.equals(spec.getBaseHullId());
    }

    private boolean isRefitOpen() {
        return Global.getSector() != null
                && Global.getSector().getCampaignUI() != null
                && Global.getSector().getCampaignUI().getCurrentCoreTab() == CoreUITabId.REFIT;
    }

    private boolean couldOpenRefit(List<InputEventAPI> events) {
        if (events == null) return false;
        for (InputEventAPI event : events) {
            if (event == null || event.isConsumed()) continue;
            if (event.isLMBDownEvent() || event.isControlDownEvent("CORE_REFIT")) return true;
        }
        return false;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }
}
