package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

public class DiableAvionicsGulf_DeepStrike extends BaseHullMod {
    private static final String HULLMOD_ID = "gulf_deep_strike";

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        // Save compatibility: remove the obsolete S-mod marker from existing variants.
        // This remains a harmless no-op once all old saves have migrated.
        stats.getVariant().getSMods().remove(HULLMOD_ID);
        stats.getVariant().getSModdedBuiltIns().remove(HULLMOD_ID);
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        if (ship == null || ship.getHullSpec() == null) return false;
        String baseHullId = ship.getHullSpec().getBaseHullId();
        return "diableavionics_IBBgulf".equals(baseHullId);
    }

    @Override
    public boolean showInRefitScreenModPickerFor(ShipAPI ship) {
        if (ship == null || ship.getHullSpec() == null) return false;
        String baseHullId = ship.getHullSpec().getBaseHullId();
        return "diableavionics_IBBgulf".equals(baseHullId);
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        return "This upgrade can only be installed on the Gulf-class Cruiser.";
    }
}
