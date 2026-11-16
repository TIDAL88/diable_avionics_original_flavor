package data.scripts.campaign.bar;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.gulf.DiableGulfPart2Intel;

final class DAGulfBountyState {
    private static final String ACQUIRED_KEY = "$da_ibb_gulf_acquired";

    private DAGulfBountyState() { }

    static boolean useIBB() {
        return Global.getSettings().getModManager().isModEnabled("swp") && DAIBBGulfBarEvent.isIBBEnabled();
    }

    static boolean isGulf(FleetMemberAPI ship) {
        return ship != null && ship.getHullSpec() != null
                && (DiableGulfPart2Intel.GULF_BASE_HULL_ID.equals(ship.getHullSpec().getBaseHullId())
                || DiableGulfPart2Intel.GULF_BASE_HULL_ID.equals(ship.getHullSpec().getHullId()));
    }

    static boolean playerAcquiredGulf() {
        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
        if (memory.getBoolean(ACQUIRED_KEY)) return true;
        boolean acquired = memory.getBoolean(DiableGulfPart2Intel.STARTED_MEMKEY)
                || memory.getBoolean(DiableGulfPart2Intel.COMPLETE_MEMKEY)
                || DiableGulfPart2Intel.playerHasGulf();
        if (!acquired) {
            for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
                CargoAPI storage = Misc.getStorageCargo(market);
                if (storage == null) continue;
                for (FleetMemberAPI ship : storage.getMothballedShips().getMembersListCopy()) {
                    if (isGulf(ship)) {
                        acquired = true;
                        break;
                    }
                }
                if (acquired) break;
            }
        }
        if (acquired) memory.set(ACQUIRED_KEY, true);
        return acquired;
    }
}
