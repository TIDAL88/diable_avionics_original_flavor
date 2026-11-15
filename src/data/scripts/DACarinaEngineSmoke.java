package data.scripts;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// Engine smoke timing and VFX by I Spectros I.
final class DACarinaEngineSmoke {
    private final IntervalUtil repeat = new IntervalUtil(0.5f, 1f);
    private final List<Burst> bursts = new ArrayList<>();
    private final IntervalUtil transitIdle = new IntervalUtil(0.18f, 0.25f);
    private final IntervalUtil systemIdle = new IntervalUtil(0.12f, 0.18f);
    private boolean wasBoosting;
    private boolean systemWasOn;
    private boolean systemEntryPending;
    private boolean versantInitialized;
    private boolean transitWasOn;
    private boolean transitEntryPending;

    static boolean isVersant(ShipAPI ship) {
        return ship.getHullSpec() != null
                && ("diableavionics_versant".equals(ship.getHullSpec().getHullId())
                || "diableavionics_versant".equals(ship.getHullSpec().getBaseHullId()));
    }

    static boolean isVersantInTransit(ShipAPI ship) {
        return isVersant(ship) && ship.getSystem() != null
                && (ship.getSystem().isChargeup() || ship.getSystem().isStateActive() || ship.getSystem().isOn());
    }

    void advance(float amount, CombatEngineAPI engine, ShipAPI ship, ShipEngineAPI exhaust) {
        advance(amount, engine, ship, exhaust, true);
    }

    void advance(float amount, CombatEngineAPI engine, ShipAPI ship, ShipEngineAPI exhaust, boolean emitting) {
        if (isVersant(ship)) {
            advanceVersant(amount, engine, ship, exhaust, emitting);
            return;
        }
        boolean systemOn = ship.getSystem() != null && ship.getSystem().isOn();
        boolean boosting = ship.isEngineBoostActive();
        boolean venting = ship.getFluxTracker() != null && ship.getFluxTracker().isVenting();
        boolean overloaded = ship.getFluxTracker() != null && ship.getFluxTracker().isOverloaded();
        if (systemOn != systemWasOn) {
            bursts.clear();
            systemEntryPending = systemOn;
            if (!systemOn) repeat.forceIntervalElapsed();
        }
        systemWasOn = systemOn;
        if (!emitting) {
            bursts.clear();
            wasBoosting = boosting;
            return;
        }
        if (systemEntryPending) {
            bursts.add(new Burst(Profile.SYSTEM, 25, 0.035f, 0.04f));
            systemEntryPending = false;
        }
        if (boosting && !wasBoosting && !systemOn) bursts.add(new Burst(Profile.BOOST, 8, 0.04f, 0.06f));
        wasBoosting = boosting;
        if (systemOn) {
            systemIdle.advance(amount);
            if (systemIdle.intervalElapsed() && !overloaded) {
                bursts.add(new Burst(Profile.SYSTEM_IDLE, 3, 0.04f, 0.05f));
            }
        } else {
            repeat.advance(amount);
            if (repeat.intervalElapsed() && !overloaded) {
                bursts.add(new Burst(venting ? Profile.VENT : Profile.NORMAL, 12, 0.04f, 0.05f));
            }
        }
        advanceBursts(amount, engine, ship, exhaust);
    }

    private void advanceVersant(float amount, CombatEngineAPI engine, ShipAPI ship,
                                ShipEngineAPI exhaust, boolean emitting) {
        boolean transit = isVersantInTransit(ship);
        if (!versantInitialized || transit != transitWasOn) {
            bursts.clear();
            transitEntryPending = transit;
            transitWasOn = transit;
            versantInitialized = true;
        }
        if (!emitting) {
            bursts.clear();
            return;
        }
        if (transit) {
            if (transitEntryPending) {
                bursts.add(new Burst(Profile.SYSTEM, 25, 0.035f, 0.04f));
                transitEntryPending = false;
            }
            transitIdle.advance(amount);
            if (transitIdle.intervalElapsed()) emit(engine, ship, exhaust, Profile.TRANSIT_IDLE);
        } else {
            repeat.advance(amount);
            if (repeat.intervalElapsed()) bursts.add(new Burst(Profile.NORMAL, 12, 0.04f, 0.05f));
        }
        advanceBursts(amount, engine, ship, exhaust);
    }

    private void advanceBursts(float amount, CombatEngineAPI engine, ShipAPI ship, ShipEngineAPI exhaust) {
        for (Iterator<Burst> iterator = bursts.iterator(); iterator.hasNext();) {
            Burst burst = iterator.next();
            burst.advance(amount, engine, ship, exhaust);
            if (burst.finished()) iterator.remove();
        }
    }

    enum Profile {
        NORMAL, VENT, BOOST, SYSTEM, TRANSIT_IDLE, SYSTEM_IDLE
    }

    static final class Burst {
        private final Profile profile;
        private final int limit;
        private final IntervalUtil interval;
        private int ticks;

        Burst(Profile profile, int limit, float minInterval, float maxInterval) {
            this.profile = profile;
            this.limit = limit;
            interval = new IntervalUtil(minInterval, maxInterval);
            interval.forceIntervalElapsed();
        }

        void advance(float amount, CombatEngineAPI engine, ShipAPI ship, ShipEngineAPI exhaust) {
            if (finished()) return;
            interval.advance(amount);
            if (!interval.intervalElapsed()) return;
            emit(engine, ship, exhaust, profile);
            ticks++;
        }

        boolean finished() {
            return ticks >= limit;
        }
    }

    private static void emit(CombatEngineAPI engine, ShipAPI ship, ShipEngineAPI exhaust, Profile profile) {
        float scale = Math.max(0.3f, Math.min(3f, exhaust.getEngineSlot().getWidth() / 12f));
        float flux = ship.getFluxLevel();
        float min = 6f + 5f * flux, range = 12f + 5f * flux;
        float spread = 2f + 24f * flux, length = 30f + 24f * flux;
        float duration = 0.5f, growth = 3f;
        int count = 2;
        Color color = new Color(162, 200, 253, 120);
        switch (profile) {
            case SYSTEM_IDLE:
                min = 12f; range = 18f; spread = 40f; length = 35f;
                duration = 0.75f; growth = 2f; count = 1;
                color = new Color(255, 184, 158, 40);
                break;
            case TRANSIT_IDLE:
                min = 6f; range = 8f; spread = 7f; length = 25f;
                duration = 0.65f; growth = 2.5f; count = 1;
                color = new Color(255, 184, 158, 15);
                break;
            case VENT:
                color = new Color(232, 165, 253, 120);
                break;
            case BOOST:
                min = 15f; range = 22f; spread = 7f; length = 85f;
                duration = 0.75f; count = 3;
                color = new Color(162, 195, 255, 70);
                break;
            case SYSTEM:
                min = 25f; range = 35f; spread = 30f; length = 140f;
                duration = 2f; growth = 3.5f; count = 3;
                color = new Color(255, 184, 158, 25);
                break;
            default:
                break;
        }
        length *= scale;
        float angle = ship.getFacing() + exhaust.getEngineSlot().getAngle();
        for (int i = 0; i < count; i++) {
            float size = (min + (float) Math.random() * range) * scale;
            double theta = Math.toRadians(angle + (Math.random() - 0.5) * spread);
            float distance = (float) Math.random() * length;
            float x = (float) Math.cos(theta) * distance, y = (float) Math.sin(theta) * distance;
            Vector2f location = new Vector2f(exhaust.getLocation().x + x, exhaust.getLocation().y + y);
            Vector2f velocity = new Vector2f(ship.getVelocity().x + x, ship.getVelocity().y + y);
            // Apply randomized drift independently of the narrow emission cone.
            double driftAngle = Math.random() * Math.PI * 2;
            float driftRadius = (float) Math.sqrt(Math.random()) * length * 0.5f;
            velocity.x += Math.cos(driftAngle) * driftRadius;
            velocity.y += Math.sin(driftAngle) * driftRadius;
            engine.addNebulaParticle(location, velocity, size, growth, 0.1f, 0f, duration, color);
        }
    }
}
