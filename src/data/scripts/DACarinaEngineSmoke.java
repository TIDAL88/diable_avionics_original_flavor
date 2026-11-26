package data.scripts;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.magiclib.util.MagicRender;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class DACarinaEngineSmoke {
    // Spectrinho: system engine smoke only; normal exhaust and body effects are separate.
    private static final boolean SKIRMISHER_SYSTEM_ENGINE_SMOKE = false;
    private static final boolean BRAWLER_SYSTEM_ENGINE_SMOKE = true;
    private static final boolean GUARDIAN_SYSTEM_ENGINE_SMOKE = false;
    private static final boolean SCOUT_SYSTEM_ENGINE_SMOKE = true;

    private static boolean systemEngineSmokeEnabled(ShipAPI ship) {
        if (!isVirtuous(ship)) return true;
        String systemId = ship.getHullSpec().getShipSystemId();
        if (systemId == null) return true;
        return switch (systemId) {
            case "diableavionics_unlockedFlicker" -> SKIRMISHER_SYSTEM_ENGINE_SMOKE;
            case "diableavionics_temporalshell" -> BRAWLER_SYSTEM_ENGINE_SMOKE;
            case "diableavionics_citadel" -> GUARDIAN_SYSTEM_ENGINE_SMOKE;
            case "diableavionics_circus" -> SCOUT_SYSTEM_ENGINE_SMOKE;
            default -> true;
        };
    }

    static boolean isVirtuous(ShipAPI ship) {
        String hullId = ship.getHullSpec() == null ? null : ship.getHullSpec().getHullId();
        return hullId != null && hullId.startsWith("diableavionics_virtuous_");
    }

    private static boolean isSkirmisher(ShipAPI ship) {
        return isVirtuous(ship)
                && "diableavionics_unlockedFlicker".equals(ship.getHullSpec().getShipSystemId());
    }

    /// Ship-centred Virtuous effects. Create ONE of these per ship, not per engine.

    static final class VirtuousEffects {
        private static final Color DEFENDER_SMOKE = new Color(155, 202, 255, 13);
        private static final Color SKIRMISHER_SMOKE = new Color(255, 255, 255, 40);
        private static final Color SKIRMISHER_WISP = new Color(255, 255, 255, 16);
        private final IntervalUtil defenderInterval = new IntervalUtil(0.08f, 0.11f);
        private boolean defenderWasOn;
        private boolean skirmisherWasOn;

        void advance(float amount, CombatEngineAPI engine, ShipAPI ship) {
            if (engine == null || engine.isPaused() || !ship.isAlive() || !isVirtuous(ship)) return;
            String systemId = ship.getHullSpec().getShipSystemId();
            boolean systemOn = ship.getSystem() != null && ship.getSystem().isOn();
            boolean defenderOn = "diableavionics_citadel".equals(systemId) && systemOn && !ship.isPhased();
            if (defenderOn != defenderWasOn) defenderInterval.nextInterval();
            defenderWasOn = defenderOn;
            boolean skirmisher = isSkirmisher(ship);
            boolean skirmisherOn = skirmisher && systemOn;
            // Flicker's falling edge is after arrival; emit once at the ship's current position.
            if (skirmisher && skirmisherWasOn && !skirmisherOn) emitSkirmisherArrival(ship);
            skirmisherWasOn = skirmisherOn;
            if (!systemOn || systemId == null) return;

            switch (systemId) {
                case "diableavionics_citadel":
                    if (defenderOn) advanceDefender(amount, engine, ship);
                    break;
                case "diableavionics_temporalshell":
                    advanceBrawler(amount, engine, ship);
                    break;
                case "diableavionics_circus":
                    advanceScout(amount, engine, ship);
                    break;
                default:
                    break;
            }
        }

        // Defender / Guardian: Touraco-style cloud
        private void advanceDefender(float amount, CombatEngineAPI engine, ShipAPI ship) {
            defenderInterval.advance(amount);
            if (defenderInterval.intervalElapsed()) emitDefenderCloud(engine, ship);
        }
        private void emitDefenderCloud(CombatEngineAPI engine, ShipAPI ship) {
            Vector2f point = ship.getLocation();
            Vector2f shipVel = ship.getVelocity();
            float length = 120f;
            for (int i = 0; i < 7; i++) {
                float size = 60 + 120 * (float) Math.random();
                double theta = Math.toRadians(ship.getFacing() - 180f + 360f * Math.random());
                float r = (float) Math.random() * length;
                float x = (float) Math.cos(theta) * r;
                float y = (float) Math.sin(theta) * r;
                Vector2f loc = new Vector2f(point.x + x, point.y + y);
                Vector2f vel = new Vector2f(shipVel.x + x, shipVel.y + y);
                Vector2f.add(vel, Misc.getPointWithinRadius(new Vector2f(), length * 0.5f), vel);
                engine.addNebulaParticle(loc, vel, size, 2.5f, 0.1f, 0f,
                        1f, DEFENDER_SMOKE);
            }
        }

        private void emitSkirmisherArrival(ShipAPI ship) {
            Vector2f point = ship.getLocation();
            Vector2f shipVel = ship.getVelocity();
            float spawnRadius = 28.125f;
            for (int i = 0; i < 28; i++) {
                boolean wisp = i >= 22;
                float size = wisp ? 55f + 30f * (float) Math.random()
                        : 75f + 45f * (float) Math.random();
                double theta = Math.toRadians(ship.getFacing() - 180f + 360f * Math.random());
                float dirX = (float) Math.cos(theta);
                float dirY = (float) Math.sin(theta);
                float r = (float) Math.random() * spawnRadius;
                Vector2f loc = new Vector2f(point.x + dirX * r, point.y + dirY * r);
                // Keep the main puff compact, with a few slow wisps left at the arrival point.
                float outwardSpeed = wisp ? 15f + 30f * (float) Math.random()
                        : 110f + 90f * (float) Math.random();
                Vector2f vel = new Vector2f(shipVel.x + dirX * outwardSpeed,
                        shipVel.y + dirY * outwardSpeed);
                Vector2f.add(vel, Misc.getPointWithinRadius(new Vector2f(), 8f), vel);
                float duration = wisp ? 0.875f + 0.4375f * (float) Math.random()
                        : 0.475f + 0.2125f * (float) Math.random();
                // The native nebula API picks its own layer. Render the same 4x4 smoke atlas below ships.
                SpriteAPI sprite = Global.getSettings().getSprite("misc", "nebula_particles");
                float tileWidth = sprite.getTextureWidth() / 4f;
                float tileHeight = sprite.getTextureHeight() / 4f;
                sprite.setTexX((int) (Math.random() * 4) * tileWidth);
                sprite.setTexY((int) (Math.random() * 4) * tileHeight);
                sprite.setTexWidth(tileWidth);
                sprite.setTexHeight(tileHeight);
                float growth = size * ((wisp ? 1.5f : 1.8f) - 1f) / duration;
                float fadeIn = duration * (wisp ? 0.08f : 0.025f);
                float peakUntil = Math.max(fadeIn, duration * (wisp ? 0f : 0.08f));
                MagicRender.battlespace(sprite, loc, vel, new Vector2f(size, size),
                        new Vector2f(growth, growth), (float) Math.random() * 360f, 0f,
                        wisp ? SKIRMISHER_WISP : SKIRMISHER_SMOKE, true,
                        0f, 0f, 0f, 0f, 0f,
                        fadeIn, peakUntil - fadeIn, duration - peakUntil,
                        CombatEngineLayers.BELOW_SHIPS_LAYER);
            }
        }

        private void advanceBrawler(float amount, CombatEngineAPI engine, ShipAPI ship) {
            // Spectros: Temporal Shell effects go here. Give this mode its own timer if needed.
        }

        private void advanceScout(float amount, CombatEngineAPI engine, ShipAPI ship) {
            // Spectros: Itano Circus effects go here.
        }
    }

    private final IntervalUtil repeat = new IntervalUtil(0.5f, 1f);
    private final List<Burst> bursts = new ArrayList<>();
    private final IntervalUtil transitIdle = new IntervalUtil(0.18f, 0.25f);
    private final IntervalUtil systemIdle = new IntervalUtil(0.12f, 0.18f);
    private boolean wasBoosting;
    private boolean systemWasOn;
    private boolean systemEntryPending;
    private boolean engineSmokeSuppressed;
    private boolean versantInitialized;
    private boolean transitWasOn;
    private boolean transitEntryPending;
    //constants that can be reused to avoids gc churns
    private static final Color VENT = new Color(232, 165, 253, 120);
    private static final Color SYSTEM = new Color(255, 184, 158, 25);
    private static final Color SYSTEM_IDLE = new Color(255, 184, 158, 40);
    private static final Color TRANSIT_IDLE = new Color(255, 184, 158, 15);
    private static final Color BOOST = new Color(162, 195, 255, 70);
    private static final Color DEFAULT = new Color(162, 200, 253, 120);
    private static final Vector2f Location = new Vector2f();
    private static final Vector2f Velocity = new Vector2f();
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
        boolean systemOn = ship.getSystem() != null && ship.getSystem().isOn()
                && systemEngineSmokeEnabled(ship);
        boolean boosting = ship.isEngineBoostActive();
        boolean venting = ship.getFluxTracker() != null && ship.getFluxTracker().isVenting();
        boolean overloaded = ship.getFluxTracker() != null && ship.getFluxTracker().isOverloaded();
        if (systemOn != systemWasOn) {
            bursts.clear();
            systemEntryPending = systemOn;
            if (!systemOn) repeat.forceIntervalElapsed();
        }
        systemWasOn = systemOn;
        boolean suppressSmoke = isSkirmisher(ship) && ship.getSystem() != null && ship.getSystem().isOn();
        if (engineSmokeSuppressed && !suppressSmoke) repeat.forceIntervalElapsed();
        engineSmokeSuppressed = suppressSmoke;
        if (!emitting || suppressSmoke) {
            bursts.clear();
            if (suppressSmoke) systemEntryPending = false;
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
        //use constants instead of allocating a new color object per call
        Color color = DEFAULT;
        switch (profile) {
            case SYSTEM_IDLE:
                min = 12f; range = 18f; spread = 40f; length = 35f;
                duration = 0.75f; growth = 2f; count = 1;
                color = SYSTEM_IDLE;
                break;
            case TRANSIT_IDLE:
                min = 6f; range = 8f; spread = 7f; length = 25f;
                duration = 0.65f; growth = 2.5f; count = 1;
                color = TRANSIT_IDLE;
                break;
            case VENT:
                color = VENT;
                break;
            case BOOST:
                min = 15f; range = 22f; spread = 7f; length = 85f;
                duration = 0.75f; count = 3;
                color = BOOST;
                break;
            case SYSTEM:
                min = 25f; range = 35f; spread = 30f; length = 140f;
                duration = 2f; growth = 3.5f; count = 3;
                color = SYSTEM;
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
            //likewise avoid new vector allocations
            Location.set(exhaust.getLocation().x+x,exhaust.getLocation().y+y);
            Velocity.set(ship.getVelocity().x + x, ship.getVelocity().y + y);
            // Apply randomized drift independently of the narrow emission cone.
            double driftAngle = Math.random() * Math.PI * 2;
            float driftRadius = (float) Math.sqrt(Math.random()) * length * 0.5f;
            // explicit cast instead of implict
            Velocity.x += (float) (Math.cos(driftAngle) * driftRadius);
            Velocity.y += (float) (Math.sin(driftAngle) * driftRadius);
            engine.addNebulaParticle(Location, Velocity, size, growth, 0.1f, 0f, duration, color);
        }
    }
}
