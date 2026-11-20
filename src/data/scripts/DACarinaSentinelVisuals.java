package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import data.shipsystems.scripts.Diableavionics_shieldStats;
import org.magiclib.paintjobs.MagicPaintjobManager;
import org.magiclib.paintjobs.MagicPaintjobSpec;

import java.awt.Color;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class DACarinaSentinelVisuals extends BaseEveryFrameCombatPlugin {
    static final String FAMILY = "carinasentinel";
    static final String STATE_KEY = "diableavionics_carina_visuals";
    static final String FX = "graphics/carinasentinel/fx/";
    static final Color SHIELD_INNER = new Color(171, 226, 255, 75);
    private CombatEngineAPI engine;
    // Cache definitions by hull, never combat ship instances.
    private static final Map<String, List<MagicPaintjobSpec>> fighterPaintjobs = new HashMap<>();
    public static boolean isCarina(ShipAPI ship) {
        if (ship == null) return false;
        if (DAPaintjobDeepStrikeSprites.isCarinaPod(ship)) return true;
        if (DAPaintjobDroneSprites.isCarinaDrone(ship)) return true;
        if (ship.getVariant() != null) {
            MagicPaintjobSpec paintjob = MagicPaintjobManager.getCurrentShipPaintjob(ship.getVariant());
            if (paintjob != null && FAMILY.equals(paintjob.getPaintjobFamily())) return true;
        }
        // MagicLib's carrier inheritance paints the combat fighter without tagging its variant.
        if (!ship.isFighter() || ship.getHullSpec() == null) return false;
        SpriteAPI sprite = ship.getSpriteAPI();
        if (sprite == null || sprite.getTextureId() == 0) return false;
        String hullId = ship.getHullSpec().getHullId();
        if (hullId == null) return false;
        List<MagicPaintjobSpec> candidates = fighterPaintjobs.computeIfAbsent(hullId,
                id -> MagicPaintjobManager.getPaintjobsForHull(id, true, true));
        for (MagicPaintjobSpec candidate : candidates) {
            if (!FAMILY.equals(candidate.getPaintjobFamily())) continue;
            SpriteAPI painted = Global.getSettings().getSprite(candidate.getSpriteId());
            if (painted != null && painted.getTextureId() == sprite.getTextureId()) return true;
        }
        return false;
    }

    @Override
    public void init(CombatEngineAPI engine) {
        fighterPaintjobs.clear();
        this.engine = engine;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine == null || engine.isPaused()) return;
        for (ShipAPI ship : engine.getShips()) {
            DAPaintjobDroneSprites.update(ship);
            // Fighters may inherit the carrier's paintjob after their first combat frame.
            boolean carina = ship.isAlive() && isCarina(ship);
            DAPaintjobMissileSprites.updateLoaded(ship, carina);
            if (!carina) continue;
            State state = (State) ship.getCustomData().get(STATE_KEY);
            if (state == null) {
                state = new State(ship);
                ship.setCustomData(STATE_KEY, state);
                engine.addLayeredRenderingPlugin(state);
            }
            state.updateShield();
        }
    }

    static boolean shieldEffectActive(ShipAPI ship) {
        return (ship.getSystem() != null && ship.getSystem().isActive())
                || Diableavionics_shieldStats.isShieldColorEffectActive(ship);
    }

    private static boolean engineAvailable(ShipAPI ship, ShipEngineAPI exhaust) {
        return !ship.isPhased() && !exhaust.isDisabled() && !exhaust.isPermanentlyDisabled()
                && !ship.getEngineController().isFlamedOut();
    }

    static boolean canEmitSmoke(ShipAPI ship, ShipEngineAPI exhaust) {
        return engineAvailable(ship, exhaust)
                && (exhaust.isActive() || DACarinaEngineSmoke.isVersantInTransit(ship));
    }

    static final class State extends BaseCombatLayeredRenderingPlugin {
        private final ShipAPI ship;
        private final Map<EngineSlotAPI, Float> originalGlow = new IdentityHashMap<>();
        private final Map<ShipEngineAPI, DACarinaEngineSmoke> smokeEmitters = new IdentityHashMap<>();
        private final DACarinaEngineSmoke.VirtuousEffects virtuousEffects;
        private ShieldAPI shield;
        private float innerRotation;
        private float ringRotation;
        private float appliedRadius = -1f;
        private boolean shieldWasControlledBySystem;

        State(ShipAPI ship) {
            this.ship = ship;
            virtuousEffects = DACarinaEngineSmoke.isVirtuous(ship) ? new DACarinaEngineSmoke.VirtuousEffects() : null;
        }

        void updateShield() {
            ShieldAPI current = ship.getShield();
            if (current == null) return;
            if (current != shield) {
                shield = current;
                innerRotation = shield.getInnerRotationRate();
                ringRotation = shield.getRingRotationRate();
                appliedRadius = -1f;
            }
            if (shieldEffectActive(ship)) {
                shieldWasControlledBySystem = true;
                return;
            }
            float radius = shield.getRadius();
            if (radius != appliedRadius || shieldWasControlledBySystem) {
                String inner = radius >= 256f ? "tania_shields256.png"
                        : radius >= 128f ? "tania_shields128c.png" : "tania_shields64.png";
                String rim = radius >= 256f ? "shields256ringd.png"
                        : radius >= 128f ? "shields128ringc.png" : "shields64ring.png";
                shield.setRadius(radius, FX + inner, "graphics/fx/" + rim);
                appliedRadius = radius;
                shieldWasControlledBySystem = false;
            }
            shield.setInnerColor(SHIELD_INNER);
            shield.setRingColor(Color.WHITE);
            // Derive from the original rates, rather than multiplying them every frame.
            shield.setInnerRotationRate(innerRotation * 0.5f);
            shield.setRingRotationRate(ringRotation * 1.5f);
        }

        @Override
        public void advance(float amount) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (entity != null) entity.getLocation().set(ship.getLocation());
            if (engine == null || engine.isPaused() || isExpired()) return;
            // Body effects run once, including while Guardian has no active engine thrust.
            if (virtuousEffects != null) virtuousEffects.advance(amount, engine, ship);
            if (ship.getEngineController() == null) return;
            for (ShipEngineAPI exhaust : ship.getEngineController().getShipEngines()) {
                EngineSlotAPI slot = exhaust.getEngineSlot();
                originalGlow.putIfAbsent(slot, slot.getGlowSizeMult());
                // Keep the native flame and timed smoke, without an additional static halo.
                slot.setGlowSizeMult(0f);
                // Keep mode edges while idle, so resuming thrust does not retrigger the system-entry burst.
                smokeEmitters.computeIfAbsent(exhaust, key -> new DACarinaEngineSmoke())
                        .advance(amount, engine, ship, exhaust, canEmitSmoke(ship, exhaust));
            }
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.BELOW_SHIPS_LAYER);
        }

        @Override
        public float getRenderRadius() {
            return ship.getCollisionRadius() + 500f;
        }

        @Override
        public boolean isExpired() {
            return !ship.isAlive() || !isCarina(ship);
        }

        @Override
        public void cleanup() {
            DAPaintjobMissileSprites.updateLoaded(ship, false);
            originalGlow.forEach(EngineSlotAPI::setGlowSizeMult);
            ship.removeCustomData(STATE_KEY);
        }
    }
}
