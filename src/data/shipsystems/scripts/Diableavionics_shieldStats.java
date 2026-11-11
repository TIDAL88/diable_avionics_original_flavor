package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import java.awt.Color;
import java.util.List;
import static data.scripts.util.Diableavionics_stringsManager.txt;

public class Diableavionics_shieldStats extends BaseShipSystemScript {

    private static final float SHIELD_COLOR_FADE_TIME = 0.5f;
    private static final Color SHIELD_INNER_COLOR = new Color(100, 255, 200, 75);
    private static final Color SHIELD_RING_COLOR = Color.WHITE;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        stats.getShieldTurnRateMult().modifyMult(id, 2f);
        stats.getShieldDamageTakenMult().modifyMult(id, 1f - .9f * effectLevel);		
        stats.getShieldUpkeepMult().modifyMult(id, 0f);

        if (stats.getEntity() instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) stats.getEntity();
            if (ship.getShield() == null) return;
            List<ShieldColorFadeListener> listeners = ship.getListeners(ShieldColorFadeListener.class);
            ShieldColorFadeListener listener;
            if (listeners.isEmpty()) {
                listener = new ShieldColorFadeListener(ship);
                ship.addListener(listener);
            } else {
                listener = listeners.get(0);
            }
            listener.active = true;
        }
    }

    private static Color blendColor(Color from, Color to, float level) {
        return new Color(
                Math.round(from.getRed() + (to.getRed() - from.getRed()) * level),
                Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * level),
                Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * level),
                Math.round(from.getAlpha() + (to.getAlpha() - from.getAlpha()) * level));
    }

    private static final class ShieldColorFadeListener implements AdvanceableListener {

        private final ShipAPI ship;
        private final ShieldAPI shield;
        private final Color originalInnerColor;
        private final Color originalRingColor;
        private boolean active;
        private float colorLevel;

        private ShieldColorFadeListener(ShipAPI ship) {
            this.ship = ship;
            shield = ship.getShield();
            originalInnerColor = shield.getInnerColor();
            originalRingColor = shield.getRingColor();
        }

        @Override
        public void advance(float amount) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null || engine.isPaused()) return;

            float step = amount / SHIELD_COLOR_FADE_TIME;
            colorLevel = active ? Math.min(1f, colorLevel + step) : Math.max(0f, colorLevel - step);
            if (!active && colorLevel > 0f) {
                // Keep the native 0.1-second shield-effect reset from interrupting this fade.
                shield.applyShieldEffects(null, null, 0f, 0f, 0f);
            }
            shield.setInnerColor(blendColor(originalInnerColor, SHIELD_INNER_COLOR, colorLevel));
            shield.setRingColor(blendColor(originalRingColor, SHIELD_RING_COLOR, colorLevel));
            if (!active && colorLevel <= 0f) {
                ship.removeListener(this);
            }
        }

        private void stop() {
            active = false;
            shield.applyShieldEffects(null, null, 0f, 0f, 0f);
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getShieldDamageTakenMult().unmodify(id);
        stats.getShieldTurnRateMult().unmodify(id);
        stats.getShieldUpkeepMult().unmodify(id);
        if (stats.getEntity() instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) stats.getEntity();
            for (ShieldColorFadeListener listener : ship.getListeners(ShieldColorFadeListener.class)) {
                listener.stop();
            }
        }
    }

    private final String TXT1 = txt("shield");
    private final String TXT2 = txt("%");
    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        
        int effect = Math.round(1000 * effectLevel);
        
        if (index == 0) {
            return new StatusData(TXT1+ effect +TXT2, false);
        }
        return null;
    }
}
