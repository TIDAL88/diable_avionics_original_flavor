package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.magiclib.paintjobs.MagicPaintjobManager;
import org.magiclib.paintjobs.MagicPaintjobSpec;

/** Combat-instance paintjob inheritance for Deep Strike pods, doors and their Wanzers. */
public final class DAPaintjobDeepStrikeSprites {
    private static final String POD_KEY = "diableavionics_deep_strike_paintjob_family";
    private static final String FIGHTER_KEY = "diableavionics_deep_strike_fighter_family";

    private DAPaintjobDeepStrikeSprites() {}

    public static boolean isCarinaPod(ShipAPI ship) {
        return "carinasentinel".equals(podFamily(ship));
    }

    public static boolean isPFIDPod(ShipAPI ship) {
        return "PFID".equals(podFamily(ship));
    }

    private static String podFamily(ShipAPI ship) {
        if (ship == null || ship.getCustomData() == null) return null;
        Object family = ship.getCustomData().get(POD_KEY);
        return "carinasentinel".equals(family) || "PFID".equals(family) ? (String) family : null;
    }

    public static void inherit(ShipAPI source, ShipAPI pod) {
        if (pod == null) return;
        String family = DACarinaSentinelVisuals.isCarina(source) ? "carinasentinel"
                : DAPFIDEngineVisuals.isPFID(source) ? "PFID" : null;
        if (family == null) return;
        MagicPaintjobSpec paintjob = MagicPaintjobManager.getPaintjob("diableavionics_" + family + "_deepStrike");
        if (paintjob == null) return;
        // Spawned pods share stored variants. Apply only to this combat instance.
        MagicPaintjobManager.applyPaintjob(pod, paintjob);
        pod.setCustomData(POD_KEY, family);
        advancePod(pod);
    }

    public static void advancePod(ShipAPI pod) {
        String family = podFamily(pod);
        if (family == null) return;
        for (FighterWingAPI wing : pod.getAllWings()) {
            for (ShipAPI fighter : wing.getWingMembers()) {
                if (family.equals(fighter.getCustomData().get(FIGHTER_KEY))) continue;
                for (MagicPaintjobSpec paintjob : MagicPaintjobManager.getPaintjobsForHull(
                        fighter.getHullSpec(), true, true)) {
                    if (family.equals(paintjob.getPaintjobFamily())) {
                        MagicPaintjobManager.applyPaintjob(fighter, paintjob);
                        fighter.setCustomData(FIGHTER_KEY, family);
                        break;
                    }
                }
            }
        }
    }

    public static SpriteAPI doorSprite(ShipAPI pod, String side) {
        String family = podFamily(pod);
        if (family != null) {
            return Global.getSettings().getSprite("graphics/" + family
                    + "/ships/station/diableavionics_deepStrikeDoor" + side + ".png");
        }
        return Global.getSettings().getSprite("diableavionics", "DS_door" + side);
    }
}
