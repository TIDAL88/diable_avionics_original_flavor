package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;

public class DiableAvionicsGulf_Master extends BaseHullMod {

    private static final String LARGE_SLOT = "WS0001";
    private static final String SPECIAL_SKIN = "diableavionics_IBBgulf_carrier";
    private static final String BASE_HULL = "diableavionics_IBBgulf";
    private static final String CUSTOM_WEAPON = "diableavionics_deep_strike_catapult";
    private static final String TOGGLE_HULLMOD = "gulf_deep_strike";
    private static final String WANZER_GANTRY = "diableavionics_universaldecksExtra";
    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        ShipVariantAPI variant = null;
        if (stats.getFleetMember() != null) {
            variant = stats.getFleetMember().getVariant();
        } else {
            variant = stats.getVariant();
        }

        if (variant == null) return;

        boolean hasHullmod = variant.getHullMods().contains(TOGGLE_HULLMOD);

        if (hasHullmod) {
            ShipHullSpecAPI specialSpec = Global.getSettings().getHullSpec(SPECIAL_SKIN);
            variant.setHullSpecAPI(specialSpec);
            variant.removeMod(WANZER_GANTRY);
            variant.removePermaMod(WANZER_GANTRY);

            if (variant.getWeaponId(LARGE_SLOT) != null &&
                    !variant.getWeaponId(LARGE_SLOT).equals(CUSTOM_WEAPON)) {

                String installedWeapon = variant.getWeaponId(LARGE_SLOT);
                variant.clearSlot(LARGE_SLOT);
                refundWeaponToCargo(installedWeapon);
            }

            variant.addWeapon(LARGE_SLOT, CUSTOM_WEAPON);
            variant.autoGenerateWeaponGroups();
        } else {

            ShipHullSpecAPI baseSpec = Global.getSettings().getHullSpec(BASE_HULL);
            variant.setHullSpecAPI(baseSpec);
            if (CUSTOM_WEAPON.equals(variant.getWeaponId(LARGE_SLOT))) {
                variant.clearSlot(LARGE_SLOT);
                if (Global.getSector() != null && Global.getSector().getPlayerFleet() != null) {
                    Global.getSector().getPlayerFleet().getCargo().removeWeapons(CUSTOM_WEAPON, 1);
                }
            }


            if (Global.getSector() != null && Global.getSector().getPlayerFleet() != null) {
                Global.getSector().getPlayerFleet().getCargo().removeWeapons(CUSTOM_WEAPON, 1);
            }
        }
    }

    private void refundWeaponToCargo(String weaponId) {
        if (Global.getSector() != null && Global.getSector().getPlayerFleet() != null) {
            Global.getSector().getPlayerFleet().getCargo().addWeapons(weaponId, 1);
        }
    }
}

