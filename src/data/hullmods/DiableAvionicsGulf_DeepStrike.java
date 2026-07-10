package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;

public class DiableAvionicsGulf_DeepStrike extends BaseHullMod {

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