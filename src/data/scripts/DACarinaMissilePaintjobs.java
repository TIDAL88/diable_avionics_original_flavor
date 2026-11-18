package data.scripts;

import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MissileRenderDataAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Applies Carina textures to launched missiles and the loaded missiles on their weapons. */
final class DACarinaMissilePaintjobs {
    private static final String LOADED_KEY = "diableavionics_carina_loaded_missiles";
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

    static void updateLoaded(ShipAPI ship, boolean carina) {
        LoadedState state = (LoadedState) ship.getCustomData().get(LOADED_KEY);
        if (!carina) {
            if (state != null) {
                for (LoadedSprite original : state.originals) original.sprite.setTexture(original.texture);
                ship.removeCustomData(LOADED_KEY);
            }
            return;
        }
        List<WeaponAPI> weapons = ship.getAllWeapons();
        if (weapons == null || weapons.isEmpty()) return;
        if (state == null) {
            state = new LoadedState();
            ship.setCustomData(LOADED_KEY, state);
        }
        for (WeaponAPI weapon : weapons) {
            if (!state.weapons.add(weapon)) continue;
            // These sprites live in a separate list, outside MagicLib's weapon sprite mappings.
            List<MissileRenderDataAPI> loaded = weapon.getMissileRenderData();
            if (loaded == null) continue;
            for (MissileRenderDataAPI data : loaded) {
                String id = data.getMissileSpecId();
                String texture = id == null ? null : SPRITES.get(id);
                if (texture == null) continue;
                SpriteAPI sprite = data.getSprite();
                state.originals.add(new LoadedSprite(sprite,
                        texture.replace("graphics/carinasentinel/", "graphics/da/")));
                sprite.setTexture(texture);
            }
        }
    }

    // Owned by the ship, so destroyed fighters cannot be retained by a static cache.
    private static final class LoadedState {
        final Set<WeaponAPI> weapons = Collections.newSetFromMap(new IdentityHashMap<>());
        final List<LoadedSprite> originals = new ArrayList<>();
    }

    private record LoadedSprite(SpriteAPI sprite, String texture) {}

    static void apply(MissileAPI missile, ShipAPI launchingShip) {
        if (missile == null || missile.getProjectileSpecId() == null) return;
        String texture = SPRITES.get(missile.getProjectileSpecId());
        if (texture == null) return;
        ShipAPI source = missile.getSource();
        if (source == null) source = launchingShip;
        if (!DACarinaSentinelVisuals.isCarina(source)) return;
        missile.getSpriteAPI().setTexture(texture);
    }
}
