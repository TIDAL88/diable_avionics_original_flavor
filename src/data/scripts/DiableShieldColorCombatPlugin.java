package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import java.awt.Color;
import java.util.List;

/**
 * Applies the optional LunaLib shield color to live Diable ships.
 *
 * This follows the same reliable pattern used by faction shield hullmods:
 * correct the live shield after combat initialization. The color comparison
 * keeps the per-frame work negligible, and the Virtuous Citadel is allowed to
 * control its own temporary shield effect while active.
 */
public class DiableShieldColorCombatPlugin extends BaseEveryFrameCombatPlugin {

    private static final String LUNALIB_ID = "lunalib";
    private static final String HULL_ID_PREFIX = "diableavionics_";
    private static final String SHIELD_COLOR_RED = "Advanced Avionics Red";
    private static final String SHIELD_COLOR_CYAN = "Phase Grazer Cyan";
    private static final String VIRTUOUS_CITADEL_HULLMOD = "diableavionics_virtuous_citadel";
    private static final Color RED_SHIELD_INNER_COLOR = new Color(74, 5, 5, 100);
    private static final Color CYAN_SHIELD_INNER_COLOR = new Color(0, 110, 140, 100);

    private CombatEngineAPI engine;
    private Color configuredColor;
    private boolean settingRead;

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine == null || engine.isPaused()) return;

        if (!settingRead) {
            settingRead = true;
            configuredColor = readConfiguredColor();
        }
        if (configuredColor == null) return;

        applyToLiveShips();
    }

    private Color readConfiguredColor() {
        if (!Global.getSettings().getModManager().isModEnabled(LUNALIB_ID)) return null;

        try {
            String mode = DAOptionalLunaSettings.getShieldColorMode();
            if (SHIELD_COLOR_RED.equals(mode)) return RED_SHIELD_INNER_COLOR;
            if (SHIELD_COLOR_CYAN.equals(mode)) return CYAN_SHIELD_INNER_COLOR;
        } catch (Throwable ignored) {
            // LunaLib is optional; retain stock colors if its setting is unavailable.
        }
        return null;
    }

    private void applyToLiveShips() {
        if (engine == null || configuredColor == null) return;

        for (ShipAPI ship : engine.getShips()) {
            if (ship == null
                    || ship.getHullSpec() == null
                    || ship.getHullSpec().getHullId() == null
                    || !ship.getHullSpec().getHullId().startsWith(HULL_ID_PREFIX)
                    || ship.getShield() == null) {
                continue;
            }

            if (ship.getVariant() != null
                    && ship.getVariant().hasHullMod(VIRTUOUS_CITADEL_HULLMOD)
                    && ship.getSystem() != null
                    && ship.getSystem().isActive()) {
                continue;
            }

            if (!configuredColor.equals(ship.getShield().getInnerColor())) {
                ship.getShield().setInnerColor(configuredColor);
            }
        }
    }
}
