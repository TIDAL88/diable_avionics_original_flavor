package data.listeners;


import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.WeaponOPCostModifier;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import data.hullmods.ArtilleryConfig;

public class OP_REDUCTION_LARGE implements WeaponOPCostModifier {
    @Override
    public int getWeaponOPCost(MutableShipStatsAPI mutableShipStatsAPI, WeaponSpecAPI weaponSpecAPI, int currCost) {
        if (weaponSpecAPI == null) return currCost;
        if (weaponSpecAPI.getSize() == WeaponAPI.WeaponSize.LARGE)
            return Math.max(0,currCost- ArtilleryConfig.OP_REDUCTION_ARTILLERY);
        return currCost;
    }
}
