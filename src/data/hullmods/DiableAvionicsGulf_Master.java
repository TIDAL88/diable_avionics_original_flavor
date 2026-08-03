package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.loading.WeaponGroupSpec;
import com.fs.starfarer.api.loading.WeaponGroupType;

import java.util.ArrayList;
import java.util.List;

public class DiableAvionicsGulf_Master extends BaseHullMod {

    private static final String LARGE_SLOT = "WS0001";
    private static final String SPECIAL_SKIN = "diableavionics_IBBgulf_carrier";
    private static final String BASE_HULL = "diableavionics_IBBgulf";
    private static final String CUSTOM_WEAPON = "diableavionics_deep_strike_catapult";
    private static final String TOGGLE_HULLMOD = "gulf_deep_strike";
    private static final String WANZER_GANTRY = "diableavionics_universaldecksExtra";
    private static final String WANZER_DISRUPTION = "diableavionics_subsystem_wanzerdisruption";
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
            String installedWeapon = variant.getWeaponId(LARGE_SLOT);
            boolean needsCatapult = !CUSTOM_WEAPON.equals(installedWeapon);
            List<WeaponGroupSpec> weaponGroups = needsCatapult ? copyWeaponGroups(variant) : null;
            boolean largeSlotWasGrouped = needsCatapult && isSlotGrouped(weaponGroups, LARGE_SLOT);

            ShipHullSpecAPI specialSpec = Global.getSettings().getHullSpec(SPECIAL_SKIN);
            variant.setHullSpecAPI(specialSpec);
            variant.removeMod(WANZER_GANTRY);
            variant.removePermaMod(WANZER_GANTRY);

            if (needsCatapult) {
                if (installedWeapon != null) {
                    variant.clearSlot(LARGE_SLOT);
                    refundWeaponToCargo(installedWeapon);
                }

                variant.addWeapon(LARGE_SLOT, CUSTOM_WEAPON);
                restoreWeaponGroups(variant, weaponGroups);
                if (!largeSlotWasGrouped) {
                    WeaponGroupSpec catapultGroup = new WeaponGroupSpec(WeaponGroupType.LINKED);
                    catapultGroup.addSlot(LARGE_SLOT);
                    catapultGroup.setAutofireOnByDefault(false);
                    variant.addWeaponGroup(catapultGroup);
                }
            }
        } else {

            ShipHullSpecAPI baseSpec = Global.getSettings().getHullSpec(BASE_HULL);
            variant.setHullSpecAPI(baseSpec);
            variant.removeMod(WANZER_DISRUPTION);
            variant.removePermaMod(WANZER_DISRUPTION);
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

    private List<WeaponGroupSpec> copyWeaponGroups(ShipVariantAPI variant) {
        List<WeaponGroupSpec> copy = new ArrayList<WeaponGroupSpec>();
        for (WeaponGroupSpec group : variant.getWeaponGroups()) {
            copy.add(group.clone());
        }
        return copy;
    }

    private boolean isSlotGrouped(List<WeaponGroupSpec> groups, String slotId) {
        for (WeaponGroupSpec group : groups) {
            if (group.getSlots().contains(slotId)) return true;
        }
        return false;
    }

    private void restoreWeaponGroups(ShipVariantAPI variant, List<WeaponGroupSpec> groups) {
        variant.getWeaponGroups().clear();
        for (WeaponGroupSpec group : groups) {
            variant.addWeaponGroup(group);
        }
    }
}
