package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.impl.campaign.skills.NeuralLinkScript;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public class StationPhaseAnchor extends BaseHullMod {

    public static class StationPhaseAnchorScript implements AdvanceableListener, HullDamageAboutToBeTakenListener {
        public ShipAPI ship;
        public boolean isDiving = false;
        public float diveProgress = 0f;
        public FaderUtil diveFader = new FaderUtil(1f, 1f);

        public StationPhaseAnchorScript(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public void advance(float amount) {
            if (!isDiving) return;

            String id = "station_phase_anchor_modifier";
            Color c = ship.getShield() != null ? ship.getShield().getInnerColor() : Color.CYAN;
            c = Misc.setAlpha(c, 255);
            c = Misc.interpolateColor(c, Color.white, 0.5f);

            if (diveProgress == 0f) {
                if (ship.getFluxTracker().showFloaty()) {
                    float timeMult = ship.getMutableStats().getTimeMult().getModifiedValue();
                    Global.getCombatEngine().addFloatingTextAlways(
                            ship.getLocation(),
                            "Emergency dive!",
                            NeuralLinkScript.getFloatySize(ship),
                            c,
                            ship,
                            16f * timeMult,
                            3.2f / timeMult,
                            1f / timeMult,
                            0f, 0f, 1f
                    );
                }
            }


            diveProgress += amount * 0.5f;

            ship.setRetreating(true, false);
            ship.blockCommandForOneFrame(ShipCommand.USE_SYSTEM);
            ship.setPhased(true);
            ship.getMutableStats().getHullDamageTakenMult().modifyMult(id, 0f);

            if (diveProgress >= 1f) {
                if (diveFader.isIdle()) {
                    Global.getSoundPlayer().playSound("phase_anchor_vanish", 1f, 1f, ship.getLocation(), ship.getVelocity());
                    diveFader.fadeOut();
                }

                diveFader.advance(amount);
                float b = diveFader.getBrightness();
                ship.setExtraAlphaMult2(b);

                float r = ship.getCollisionRadius() * 5f;
                ship.setJitter(this, c, b, 20, r * (1f - b));

                if (diveFader.isFadedOut()) {

                    ship.getLocation().set(0, -1000000f);
                }
            }
        }

        @Override
        public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {
            if (isDiving) return true;

            float hull = ship.getHitpoints();
            if (damageAmount >= hull) {
                ship.setHitpoints(1f);
                isDiving = true;

                String key = "phaseAnchor_canDive";
                Global.getCombatEngine().getCustomData().put(key, true);

                if (!ship.isPhased()) {
                    Global.getSoundPlayer().playSound("system_phase_cloak_activate", 1f, 1f, ship.getLocation(), ship.getVelocity());
                }

                return true;
            }

            return false;
        }
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.addListener(new StationPhaseAnchorScript(ship));
    }
}