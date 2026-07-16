package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;

import java.awt.Color;

/**
 * Purely visual hullmod.
 *
 * Adds intermittent shield-only interference by briefly changing the
 * shield's inner and ring colors. It does not spawn particles and does not
 * apply any venting, hull jitter, afterimage, damage, EMP, or stat effect.
 */
public class Diableavionics_phaseVisuals extends BaseHullMod {

    private static final String SHIELD_SPRITE_CATEGORY = "shields";
    private static final String INNER_SHIELD_SPRITE_KEY = "phaseShieldInner";
    private static final String OUTER_SHIELD_SPRITE_KEY = "phaseShieldOuter";

    private static final String STATE_KEY =
            "diableavionics_phase_visuals_interference_state";

    // Time between interference bursts while the shield is active.
    private static final float DELAY_MIN = 1.8f;
    private static final float DELAY_MAX = 4.5f;

    // Length of one interference burst.
    private static final float BURST_DURATION_MIN = 0.18f;
    private static final float BURST_DURATION_MAX = 0.42f;

    // How quickly the shield jumps between interference states during a burst.
    private static final float FLICKER_STEP_MIN = 0.025f;
    private static final float FLICKER_STEP_MAX = 0.070f;

    // Pale, desaturated cyan used only during interference spikes.
    private static final Color INTERFERENCE_COLOR =
            new Color(175, 225, 230, 255);

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship == null || ship.getShield() == null) {
            return;
        }

        ShieldAPI shield = ship.getShield();
        shield.setRadius(
                shield.getRadius(),
                Global.getSettings().getSpriteName(
                        SHIELD_SPRITE_CATEGORY,
                        INNER_SHIELD_SPRITE_KEY
                ),
                Global.getSettings().getSpriteName(
                        SHIELD_SPRITE_CATEGORY,
                        OUTER_SHIELD_SPRITE_KEY
                )
        );
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null || ship.isHulk()) {
            return;
        }

        ShieldAPI shield = ship.getShield();
        if (shield == null) {
            return;
        }

        InterferenceState state = getOrCreateState(ship, shield);

        if (!shield.isOn() || shield.getActiveArc() <= 0f) {
            restoreBaseColors(shield, state);
            state.inBurst = false;
            state.burstRemaining = 0f;
            state.flickerStepRemaining = 0f;
            return;
        }

        if (!state.inBurst) {
            state.delayRemaining -= amount;

            if (state.delayRemaining <= 0f) {
                state.inBurst = true;
                state.burstRemaining = randomRange(
                        BURST_DURATION_MIN,
                        BURST_DURATION_MAX
                );
                state.flickerStepRemaining = 0f;
            } else {
                restoreBaseColors(shield, state);
                return;
            }
        }

        state.burstRemaining -= amount;
        state.flickerStepRemaining -= amount;

        if (state.burstRemaining <= 0f) {
            state.inBurst = false;
            state.delayRemaining = randomRange(DELAY_MIN, DELAY_MAX);
            restoreBaseColors(shield, state);
            return;
        }

        if (state.flickerStepRemaining <= 0f) {
            state.flickerStepRemaining = randomRange(
                    FLICKER_STEP_MIN,
                    FLICKER_STEP_MAX
            );
            applyRandomInterference(shield, state);
        }
    }

    private void applyRandomInterference(
            ShieldAPI shield,
            InterferenceState state
    ) {
        /*
         * Most steps are brief brightness/color spikes. A minority are
         * dropouts, creating an irregular "signal interference" rhythm.
         */
        boolean dropout = Math.random() < 0.28d;

        if (dropout) {
            float innerAlphaMultiplier = randomRange(0.45f, 0.75f);
            float ringAlphaMultiplier = randomRange(0.25f, 0.65f);

            shield.setInnerColor(scaleAlpha(
                    state.baseInnerColor,
                    innerAlphaMultiplier
            ));
            shield.setRingColor(scaleAlpha(
                    state.baseRingColor,
                    ringAlphaMultiplier
            ));
        } else {
            float colorBlend = randomRange(0.12f, 0.38f);
            float innerAlphaMultiplier = randomRange(1.05f, 1.40f);
            float ringAlphaMultiplier = randomRange(1.10f, 1.55f);

            Color inner = blendColors(
                    state.baseInnerColor,
                    INTERFERENCE_COLOR,
                    colorBlend
            );
            Color ring = blendColors(
                    state.baseRingColor,
                    INTERFERENCE_COLOR,
                    colorBlend * 0.75f
            );

            shield.setInnerColor(scaleAlpha(
                    inner,
                    innerAlphaMultiplier
            ));
            shield.setRingColor(scaleAlpha(
                    ring,
                    ringAlphaMultiplier
            ));
        }
    }

    private InterferenceState getOrCreateState(
            ShipAPI ship,
            ShieldAPI shield
    ) {
        Object existing = ship.getCustomData().get(STATE_KEY);

        if (existing instanceof InterferenceState) {
            return (InterferenceState) existing;
        }

        InterferenceState state = new InterferenceState(
                shield.getInnerColor(),
                shield.getRingColor(),
                randomRange(DELAY_MIN, DELAY_MAX)
        );

        ship.setCustomData(STATE_KEY, state);
        return state;
    }

    private void restoreBaseColors(
            ShieldAPI shield,
            InterferenceState state
    ) {
        shield.setInnerColor(state.baseInnerColor);
        shield.setRingColor(state.baseRingColor);
    }

    private Color blendColors(Color base, Color target, float amount) {
        amount = clamp(amount, 0f, 1f);

        int red = Math.round(
                base.getRed() + (target.getRed() - base.getRed()) * amount
        );
        int green = Math.round(
                base.getGreen() + (target.getGreen() - base.getGreen()) * amount
        );
        int blue = Math.round(
                base.getBlue() + (target.getBlue() - base.getBlue()) * amount
        );

        return new Color(
                clampColor(red),
                clampColor(green),
                clampColor(blue),
                base.getAlpha()
        );
    }

    private Color scaleAlpha(Color color, float multiplier) {
        int alpha = Math.round(color.getAlpha() * multiplier);

        return new Color(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                clampColor(alpha)
        );
    }

    private float randomRange(float minimum, float maximum) {
        return minimum + (float) Math.random() * (maximum - minimum);
    }

    private float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static final class InterferenceState {
        private final Color baseInnerColor;
        private final Color baseRingColor;

        private float delayRemaining;
        private float burstRemaining = 0f;
        private float flickerStepRemaining = 0f;
        private boolean inBurst = false;

        private InterferenceState(
                Color baseInnerColor,
                Color baseRingColor,
                float delayRemaining
        ) {
            this.baseInnerColor = baseInnerColor;
            this.baseRingColor = baseRingColor;
            this.delayRemaining = delayRemaining;
        }
    }
}
