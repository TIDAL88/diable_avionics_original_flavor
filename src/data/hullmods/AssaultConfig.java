package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.listeners.OP_REDUCTION_SMALL;

public class AssaultConfig extends BaseHullMod {
    public static final String HULLMOD_ID = "assault_configuration";
    public static final int SPEED_FLAT = 15;
    public static final float ROF = 20f;
    public static final float FLUX_COST = 0.80F;
    public static final float RANGE_REDUCTION = 0.80f;
    public static final float DAMAGE_REDUCTION = 0.90f;
    public static final int OP_REDUCTION_ASSAULT = 2;
    private static final String MOD_ID = HULLMOD_ID + "_stats";

    @Override
    public boolean affectsOPCosts() {
        return true;
    }

    private void ensureListener(MutableShipStatsAPI stats) {
        if (stats != null && !stats.hasListenerOfClass(OP_REDUCTION_SMALL.class)) {
            stats.addListener(new OP_REDUCTION_SMALL());
        }
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        ensureListener(stats);
        if (stats == null) {
            return;
        }
        applyBonuses(stats);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float pad = 10f;
        tooltip.addSectionHeading("Assault Configuration", Alignment.MID, pad);

        tooltip.addPara("Reduces the OP cost of all small and medium weapons by 2. " +
                        "Increases top speed by 15 su and weapon rate of fire by 20%%, also reduce flux cost by 20%%.", pad,
                Misc.getHighlightColor(),
                "2", "15 su", "30%", "30%");

        tooltip.addPara("Base weapon range is reduced by 20%% and overall weapon damage is reduced by 10%%.", pad,
                Misc.getNegativeHighlightColor(),
                "20%", "10%");
    }

    private void applyBonuses(MutableShipStatsAPI stats) {
        if (stats == null) {
            return;
        }

        stats.getMaxSpeed().modifyFlat(MOD_ID, SPEED_FLAT);
        stats.getEnergyRoFMult().modifyPercent(MOD_ID, ROF);
        stats.getBallisticRoFMult().modifyPercent(MOD_ID, ROF);
        stats.getMissileRoFMult().modifyPercent(MOD_ID, ROF);
        stats.getEnergyWeaponFluxCostMod().modifyMult(MOD_ID, FLUX_COST);
        stats.getBallisticWeaponFluxCostMod().modifyMult(MOD_ID, FLUX_COST);
        stats.getMissileWeaponFluxCostMod().modifyMult(MOD_ID, FLUX_COST);


        stats.getBallisticWeaponRangeBonus().modifyMult(MOD_ID, RANGE_REDUCTION);
        stats.getEnergyWeaponRangeBonus().modifyMult(MOD_ID, RANGE_REDUCTION);
        stats.getMissileWeaponRangeBonus().modifyMult(MOD_ID, RANGE_REDUCTION);


        stats.getEnergyWeaponDamageMult().modifyMult(MOD_ID, DAMAGE_REDUCTION);
        stats.getBallisticWeaponDamageMult().modifyMult(MOD_ID, DAMAGE_REDUCTION);
        stats.getMissileWeaponDamageMult().modifyMult(MOD_ID, DAMAGE_REDUCTION);
    }
}