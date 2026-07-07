package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.listeners.OP_REDUCTION_LARGE;

public class ArtilleryConfig extends BaseHullMod {
    public static final String HULLMOD_ID = "artillery_configuration";
    public static final int SPEED_FLAT = -10;
    public static final int OP_REDUCTION_ARTILLERY = 4;
    private static final String MOD_ID = HULLMOD_ID + "_stats";
    private static final float RANGE_BONUS = 1.20F;
    private static final float DAMAGE_BUFF = 1.10F;
    private static final float ROF_REDUCTION = 0.90F;

    @Override
    public boolean affectsOPCosts() {
        return true;
    }

    private void ensureListener(MutableShipStatsAPI stats) {
        if (stats != null && !stats.hasListenerOfClass(OP_REDUCTION_LARGE.class)) {
            stats.addListener(new OP_REDUCTION_LARGE());
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
        tooltip.addSectionHeading("Artillery Configuration", Alignment.MID, pad);

        tooltip.addPara("Reduces top speed by %s su and rate of fire by %s.", pad,
                Misc.getNegativeHighlightColor(),
                String.valueOf(Math.abs(SPEED_FLAT)),
                "10%");

        tooltip.addPara("Increases weapon range by %s and weapon damage by %s. Also reduces OP cost for large weapons by %s.", pad,
                Misc.getHighlightColor(),
                "20%",
                "10%",
                String.valueOf(OP_REDUCTION_ARTILLERY));
    }

    private void applyBonuses(MutableShipStatsAPI stats) {
        if (stats == null) {
            return;
        }
        stats.getMaxSpeed().modifyFlat(MOD_ID, SPEED_FLAT);
        stats.getBallisticWeaponRangeBonus().modifyMult(MOD_ID, RANGE_BONUS);
        stats.getEnergyWeaponRangeBonus().modifyMult(MOD_ID, RANGE_BONUS);
        stats.getMissileWeaponRangeBonus().modifyMult(MOD_ID, RANGE_BONUS);
        stats.getEnergyRoFMult().modifyMult(MOD_ID, ROF_REDUCTION);
        stats.getBallisticRoFMult().modifyMult(MOD_ID, ROF_REDUCTION);
        stats.getMissileRoFMult().modifyMult(MOD_ID, ROF_REDUCTION);
        stats.getEnergyWeaponDamageMult().modifyMult(MOD_ID, DAMAGE_BUFF);
        stats.getBallisticWeaponDamageMult().modifyMult(MOD_ID, DAMAGE_BUFF);
        stats.getMissileWeaponDamageMult().modifyMult(MOD_ID, DAMAGE_BUFF);
    }
}
