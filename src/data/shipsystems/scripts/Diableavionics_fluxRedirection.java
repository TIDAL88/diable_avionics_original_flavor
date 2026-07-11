package data.shipsystems.scripts;

//import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
//import com.fs.starfarer.api.combat.ShipAPI;
//import com.fs.starfarer.api.combat.WeaponAPI;
//import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import static data.scripts.util.Diableavionics_stringsManager.txt;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import org.lazywizard.lazylib.FastTrig;

public class Diableavionics_fluxRedirection extends BaseShipSystemScript {

    private final float ROF_BONUS_PERCENT = 50;
    private final float FLUX_REDUCTION = 0.5f;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

        float BEAM_BONUS_PERCENT = 50;
        float beamPercent = BEAM_BONUS_PERCENT * effectLevel;
        stats.getBeamWeaponDamageMult().modifyPercent(id, beamPercent);                

        float rofPercent = ROF_BONUS_PERCENT * effectLevel;
        stats.getBallisticRoFMult().modifyPercent(id, rofPercent);
        stats.getEnergyRoFMult().modifyPercent(id, rofPercent);

        float fluxMult = 1-(FLUX_REDUCTION* effectLevel);
        stats.getBallisticWeaponFluxCostMod().modifyMult(id,fluxMult);
        stats.getEnergyWeaponFluxCostMod().modifyMult(id,fluxMult);
        stats.getBeamWeaponFluxCostMult().modifyMult(id,fluxMult);
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getBallisticRoFMult().unmodify(id);
        stats.getBallisticWeaponFluxCostMod().unmodify(id);
        stats.getEnergyRoFMult().unmodify(id);
        stats.getEnergyWeaponFluxCostMod().unmodify(id);
        stats.getBeamWeaponDamageMult().unmodify(id);
    }

    
    private final String TXT1 = txt("redirection1");
    private final String TXT2 = txt("%");
    private final String TXT3 = txt("redirection2");
    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        int mult = Math.round(ROF_BONUS_PERCENT * effectLevel);

        int flux = Math.round(FLUX_REDUCTION*100 * effectLevel);
        if (index == 0) {
                return new StatusData(TXT1 + mult + TXT2, false);
        }
        if (index == 1) {
                return new StatusData(TXT3 + flux + TXT2, false);
        }

        return null;
    }
}
