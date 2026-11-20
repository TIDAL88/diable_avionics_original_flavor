package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.magiclib.paintjobs.MagicPaintjobManager;
import org.magiclib.paintjobs.MagicPaintjobSpec;

public final class DACarinaDeepStrikePaintjobs {
    private static final String POD_KEY = "diableavionics_carina_deep_strike";
    private static final String FIGHTER_KEY = "diableavionics_carina_deep_strike_fighter";
    private static final String SPRITES = "graphics/carinasentinel/ships/station/";

    private DACarinaDeepStrikePaintjobs() {}

    public static boolean isCarinaPod(ShipAPI ship) {
        return ship != null && ship.getCustomData() != null
                && Boolean.TRUE.equals(ship.getCustomData().get(POD_KEY));
    }

    public static void inherit(ShipAPI source, ShipAPI pod) {
        if (pod == null || !DACarinaSentinelVisuals.isCarina(source)) return;
        MagicPaintjobSpec paintjob = MagicPaintjobManager.getPaintjob("diableavionics_carinasentinel_deepStrike");
        if (paintjob == null) return;
        // Spawned pods share stored variants. Apply only to this combat instance.
        MagicPaintjobManager.applyPaintjob(pod, paintjob);
        pod.setCustomData(POD_KEY, true);
        advancePod(pod);
    }

    public static void advancePod(ShipAPI pod) {
        if (!isCarinaPod(pod)) return;
        for (FighterWingAPI wing : pod.getAllWings()) {
            for (ShipAPI fighter : wing.getWingMembers()) {
                if (Boolean.TRUE.equals(fighter.getCustomData().get(FIGHTER_KEY))) continue;
                for (MagicPaintjobSpec paintjob : MagicPaintjobManager.getPaintjobsForHull(
                        fighter.getHullSpec(), true, true)) {
                    if (DACarinaSentinelVisuals.FAMILY.equals(paintjob.getPaintjobFamily())) {
                        MagicPaintjobManager.applyPaintjob(fighter, paintjob);
                        break;
                    }
                }
                fighter.setCustomData(FIGHTER_KEY, true);
            }
        }
    }

    public static SpriteAPI doorSprite(ShipAPI pod, String side) {
        if (isCarinaPod(pod)) {
            return Global.getSettings().getSprite(SPRITES + "diableavionics_deepStrikeDoor" + side + ".png");
        }
        return Global.getSettings().getSprite("diableavionics", "DS_door" + side);
    }
}
