package data.scripts;

import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.magiclib.paintjobs.MagicPaintjobManager;
import org.magiclib.paintjobs.MagicPaintjobSpec;

import java.util.Map;

/** applies the launching ship's Carina paintjob to an individual missile at spawn */
final class DACarinaMissilePaintjobs {
    private static final String MISSILES = "graphics/carinasentinel/missiles/";
    private static final Map<String, String> SPRITES = Map.ofEntries(
            Map.entry("diableavionics_banishmirv", MISSILES + "diableavionics_banishmirv.png"),
            Map.entry("diableavionics_magicmissile", MISSILES + "diableavionics_magicmissile.png"),
            Map.entry("diableavionics_micromissile", MISSILES + "diableavionics_micromissile.png"),
            Map.entry("diableavionics_itanomissile", MISSILES + "diableavionics_micromissile.png"),
            Map.entry("diableavionics_virtuousmissile", MISSILES + "diableavionics_micromissile.png"),
            Map.entry("diableavionics_plover", MISSILES + "diableavionics_plover.png"),
            Map.entry("diableavionics_thrushmirv", MISSILES + "diableavionics_thrush.png"),
            Map.entry("diableavionics_thunderbolt", MISSILES + "diableavionics_thunderbolt.png"),
            Map.entry("diableavionics_booster_proj",
                    "graphics/carinasentinel/ships/arbitrator/diableavionics_arbitratorBoosterB.png")
    );

    private DACarinaMissilePaintjobs() {}

    static void apply(MissileAPI missile, ShipAPI launchingShip) {
        if (missile == null || missile.getProjectileSpecId() == null) return;
        String texture = SPRITES.get(missile.getProjectileSpecId());
        if (texture == null) return;
        ShipAPI source = missile.getSource();
        if (source == null) source = launchingShip;
        if (source == null || source.getVariant() == null) return;
        // read at spawn so fighters can receive their inherited paintjob after deploymen
        MagicPaintjobSpec paintjob = MagicPaintjobManager.getCurrentShipPaintjob(source.getVariant());
        if (paintjob == null || !"carinasentinel".equals(paintjob.getPaintjobFamily())) return;
        missile.getSpriteAPI().setTexture(texture);
    }
}
