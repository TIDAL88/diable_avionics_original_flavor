package data.listeners;


import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.WeaponOPCostModifier;
import com.fs.starfarer.api.loading.WeaponSpecAPI;

import data.hullmods.AssaultConfig;
public class OP_REDUCTION_SMALL implements WeaponOPCostModifier {
    @Override
    public int getWeaponOPCost(MutableShipStatsAPI mutableShipStatsAPI, WeaponSpecAPI weaponSpecAPI, int currCost) {
        if (weaponSpecAPI == null) return currCost;
        if (weaponSpecAPI.getSize() == WeaponAPI.WeaponSize.SMALL || weaponSpecAPI.getSize() == WeaponAPI.WeaponSize.MEDIUM) {
            return Math.max(0, currCost - AssaultConfig.OP_REDUCTION_ASSAULT);
        }
        return currCost;
    }
}