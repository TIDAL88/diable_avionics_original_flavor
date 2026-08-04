package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;

public class DiableAvionicsRime_Master extends BaseHullMod {
    public static final String TOGGLE_ACTIVE_HULLMOD = "rime_active_bay";
    public static final String TOGGLE_STANDBY_HULLMOD = "rime_standby_bay";

    private static final String TAG_ACTIVE = "rime_was_active";
    private static final String TAG_STANDBY = "rime_was_standby";

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        ShipVariantAPI variant = (stats.getFleetMember() != null)
                ? stats.getFleetMember().getVariant()
                : stats.getVariant();
        if (variant == null) return;

        boolean hasStandby = variant.hasHullMod(TOGGLE_STANDBY_HULLMOD);
        boolean hasActive = variant.hasHullMod(TOGGLE_ACTIVE_HULLMOD);

        if (!hasStandby && !hasActive) {
            if (variant.hasTag(TAG_STANDBY)) {

                variant.addMod(TOGGLE_ACTIVE_HULLMOD);
                setTag(variant, TAG_ACTIVE);
            } else {

                variant.addMod(TOGGLE_STANDBY_HULLMOD);
                setTag(variant, TAG_STANDBY);
            }
            return;
        }


        if (hasStandby && hasActive) {
            if (variant.hasTag(TAG_STANDBY)) {

                variant.removeMod(TOGGLE_STANDBY_HULLMOD);
                setTag(variant, TAG_ACTIVE);
            } else {

                variant.removeMod(TOGGLE_ACTIVE_HULLMOD);
                setTag(variant, TAG_STANDBY);
            }
            return;
        }

        if (hasStandby) {
            setTag(variant, TAG_STANDBY);
        } else if (hasActive) {
            setTag(variant, TAG_ACTIVE);
        }
    }

    private void setTag(ShipVariantAPI variant, String activeTag) {
        variant.removeTag(TAG_ACTIVE);
        variant.removeTag(TAG_STANDBY);
        variant.addTag(activeTag);
    }
}