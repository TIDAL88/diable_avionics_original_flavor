package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;

public class RimeFlightDeckController extends BaseHullMod {
    public static final String RIME_HULL_ID = "diableavionics_rime";
    public static final String ACTIVE_HULLMOD_ID = "diableavionics_rime_flightdeck_active";
    public static final String STANDBY_HULLMOD_ID = "diableavionics_rime_flightdeck_standby";

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getNumFighterBays().unmodify(id);

        ShipVariantAPI variant = stats.getVariant();
        if (variant == null) return;

        boolean active = variant.hasHullMod(ACTIVE_HULLMOD_ID) || hasFittedWing(variant);
        if (!active) {
            stats.getNumFighterBays().modifyFlat(id, -1f);
        }
    }

    public static boolean hasFittedWing(ShipVariantAPI variant) {
        if (variant == null || variant.getWings() == null) return false;
        for (String wingId : variant.getWings()) {
            if (wingId != null && !wingId.trim().isEmpty()) return true;
        }
        return false;
    }
}
