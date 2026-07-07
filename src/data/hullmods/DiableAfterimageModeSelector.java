package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;

/**
 * Cosmetic selector hullmod added automatically by DiableAfterimageSuiteCosmetic.
 * It has no gameplay effects and only exists as a removable refit-screen toggle.
 */
public class DiableAfterimageModeSelector extends BaseHullMod {

    private static final String MAIN_ID = "abyss_diable_afterimage_suite";
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return ship != null && ship.getVariant() != null &&
                ship.getVariant().hasHullMod(MAIN_ID);
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        return "Installed automatically by Diable Aesthetics.";
    }

    @Override
    public boolean affectsOPCosts() {
        return false;
    }
}
