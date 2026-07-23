package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.magiclib.plugins.MagicTrailPlugin;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * Cosmetic-only addon hullmod for Diable Avionics ships.
 *
 * Uses the legacy save-compatible hullmod id as the main hullmod:
 * abyss_diable_afterimage_suite
 *
 * Adds a removable refit-screen mode selector hullmod:
 * - abyss_diable_afterimage_mode_red
 * - abyss_diable_afterimage_mode_blue
 *
 * Removing the current selector cycles to the other trail color.
 */
public class DiableAfterimageSuiteCosmetic extends BaseHullMod {

    private static final String MAIN_ID = "abyss_diable_afterimage_suite";
    private static final String MODE_RED_ID = "abyss_diable_afterimage_mode_red";
    private static final String MODE_BLUE_ID = "abyss_diable_afterimage_mode_blue";

    private static final String TAG_LAST_RED = "abyss_diable_afterimage_last_red";
    private static final String TAG_LAST_BLUE = "abyss_diable_afterimage_last_blue";

    private static final Color ENGINE_COLOR = new Color(190, 220, 255);
    private static final Color EMPTY_COLOR = new Color(0, 0, 0, 0);

    private static final Color RED_TRAIL_COLOR = new Color(255, 20, 20, 255);
    // Saturated cyan-blue, deliberately darker than the engine glow so it does not wash out white.
    private static final Color BLUE_TRAIL_COLOR = new Color(94, 255, 255, 255);

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        if (stats == null || stats.getVariant() == null) return;

        boolean hasRed = stats.getVariant().hasHullMod(MODE_RED_ID);
        boolean hasBlue = stats.getVariant().hasHullMod(MODE_BLUE_ID);

        // Clean up impossible double-mode state.
        if (hasRed && hasBlue) {
            stats.getVariant().removeMod(MODE_BLUE_ID);
            hasBlue = false;
        }

        // If a selector is present, remember it. If the player removes the selector,
        // the remembered value lets us cycle to the other one next refresh.
        if (hasRed) {
            stats.getVariant().addTag(TAG_LAST_RED);
            stats.getVariant().removeTag(TAG_LAST_BLUE);
            return;
        }
        if (hasBlue) {
            stats.getVariant().addTag(TAG_LAST_BLUE);
            stats.getVariant().removeTag(TAG_LAST_RED);
            return;
        }

        // No selector present: either first install, or player clicked minus to switch modes.
        if (stats.getVariant().hasTag(TAG_LAST_RED)) {
            stats.getVariant().addMod(MODE_BLUE_ID);
            stats.getVariant().addTag(TAG_LAST_BLUE);
            stats.getVariant().removeTag(TAG_LAST_RED);
        } else {
            stats.getVariant().addMod(MODE_RED_ID);
            stats.getVariant().addTag(TAG_LAST_RED);
            stats.getVariant().removeTag(TAG_LAST_BLUE);
        }
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship == null) return;

        Color trailColor = RED_TRAIL_COLOR;
        boolean blueMode = ship.getVariant() != null &&
                ship.getVariant().hasHullMod(MODE_BLUE_ID);

        if (blueMode) {
            trailColor = BLUE_TRAIL_COLOR;
        }

        if (ship.getEngineController() == null) return;
        ship.addListener(new AfterimageVisualTracker(
                ship,
                trailColor
        ));
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return isDiableHull(ship);
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        if (!isDiableHull(ship)) {
            return "Can only be installed on Diable Avionics hulls.";
        }
        return null;
    }

    private boolean isDiableHull(ShipAPI ship) {
        return ship != null
                && ship.getHullSpec() != null
                && ship.getHullSpec().getHullId() != null
                && ship.getHullSpec().getHullId().startsWith("diable");
    }

    @Override
    public boolean affectsOPCosts() {
        return false;
    }

    @Override
    public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
        return !isForModSpec;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static class AfterimageVisualTracker implements AdvanceableListener {
        private final ShipAPI ship;
        private final Color trailColor;
        private final IntervalUtil effectInterval = new IntervalUtil(0.05f, 0.05f);
        private final Map<ShipEngineControllerAPI.ShipEngineAPI, Float> trailIdByEngine = new HashMap<ShipEngineControllerAPI.ShipEngineAPI, Float>();
        private final SpriteAPI trailSprite = Global.getSettings().getSprite("fx", "beamRough2Core");

        private float engineEffectLevel = 0f;

        private AfterimageVisualTracker(
                ShipAPI ship,
                Color trailColor
        ) {
            this.ship = ship;
            this.trailColor = trailColor;
        }

        @Override
        public void advance(float amount) {
            if (ship == null || !ship.isAlive() || ship.getEngineController() == null) return;

            engineEffectLevel = clamp(engineEffectLevel + amount, 0f, 1f);
            ship.getEngineController().fadeToOtherColor(this, ENGINE_COLOR, EMPTY_COLOR, engineEffectLevel, 0.33f);

            effectInterval.advance(amount);
            if (!effectInterval.intervalElapsed()) return;

            Vector2f velocity = ship.getVelocity();
            float angle = Misc.getAngleInDegrees(new Vector2f(velocity));
            float maxSpeed = Math.max(ship.getMaxSpeed(), 1f);
            float opacity = clamp((velocity.length() / maxSpeed) * 1.65f, 0f, 1f);

            if (opacity <= 0.02f) return;

            for (ShipEngineControllerAPI.ShipEngineAPI engine : ship.getEngineController().getShipEngines()) {
                if (!trailIdByEngine.containsKey(engine)) {
                    trailIdByEngine.put(engine, MagicTrailPlugin.getUniqueID());
                }

                MagicTrailPlugin.addTrailMemberSimple(
                        ship,
                        ((Float) trailIdByEngine.get(engine)).floatValue(),
                        trailSprite,
                        engine.getLocation(),
                        0f,
                        angle,
                        5f,
                        5f,
                        trailColor,
                        opacity,
                        0f,
                        0f,
                        2f,
                        true
                );
            }
        }

    }
}
