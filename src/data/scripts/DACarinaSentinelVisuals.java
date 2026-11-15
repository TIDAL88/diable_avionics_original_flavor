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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class DACarinaSentinelVisuals extends BaseEveryFrameCombatPlugin {
    static final String FAMILY = "carinasentinel";
    static final String STATE_KEY = "diableavionics_carina_visuals";
    static final String FX = "graphics/carinasentinel/fx/";
    static final Color SHIELD_INNER = new Color(171, 226, 255, 75);
    static final Color ENGINE_COLOR = new Color(180, 230, 250, 200);
    private CombatEngineAPI engine;
    // must be cleaned every time to prevent gc leaks
    private static final IdentityHashMap<ShipAPI,Boolean> eligibleForPaintjob = new IdentityHashMap<>();
    public static boolean isCarina(ShipAPI ship) {
        if (ship == null || ship.getVariant() == null) return false;
        if (eligibleForPaintjob.containsKey(ship)) return eligibleForPaintjob.get(ship);
        MagicPaintjobSpec paintjob = MagicPaintjobManager.getCurrentShipPaintjob(ship.getVariant());
        //avoid repetitive lookups by caching eligibility once
        if (paintjob != null && FAMILY.equals(paintjob.getPaintjobFamily())){
            eligibleForPaintjob.put(ship,true);
            return true;
        }
        else{
            eligibleForPaintjob.put(ship,false);
            return false;
        }
    }

    @Override
    public void init(CombatEngineAPI engine) {
        //cleared once per battle to prevent gc leaks
        eligibleForPaintjob.clear();
        this.engine = engine;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine == null || engine.isPaused()) return;
        for (ShipAPI ship : engine.getShips()) {
            if (!ship.isAlive() || !isCarina(ship)) continue;
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
        private final SpriteAPI glow;
        private final SpriteAPI outline;
        private final Map<EngineSlotAPI, Float> originalGlow = new IdentityHashMap<>();
        private final Map<ShipEngineAPI, DACarinaEngineSmoke> smokeEmitters = new IdentityHashMap<>();
        private ShieldAPI shield;
        private float innerRotation;
        private float ringRotation;
        private float appliedRadius = -1f;
        private boolean shieldWasControlledBySystem;

        State(ShipAPI ship) {
            this.ship = ship;
            glow = Global.getSettings().getSprite("carina_sentinel", "engine");
            outline = Global.getSettings().getSprite("carina_sentinel", "engine_outline");
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
            if (engine == null || engine.isPaused() || isExpired() || ship.getEngineController() == null) return;
            for (ShipEngineAPI exhaust : ship.getEngineController().getShipEngines()) {
                EngineSlotAPI slot = exhaust.getEngineSlot();
                originalGlow.putIfAbsent(slot, slot.getGlowSizeMult());
                // Replace the stock glow without changing engine geometry or thrust.
                slot.setGlowSizeMult(0f);
                // Keep mode edges while idle, so resuming thrust does not retrigger the system-entry burst.
                smokeEmitters.computeIfAbsent(exhaust, key -> new DACarinaEngineSmoke())
                        .advance(amount, engine, ship, exhaust, canEmitSmoke(ship, exhaust));
            }
        }

        private boolean visible(ShipEngineAPI exhaust) {
            return exhaust.isActive() && engineAvailable(ship, exhaust);
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
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            if (isExpired() || ship.getEngineController() == null) return;
            if (entity != null) entity.getLocation().set(ship.getLocation());
            for (ShipEngineAPI exhaust : ship.getEngineController().getShipEngines()) {
                if (!visible(exhaust)) continue;
                EngineSlotAPI slot = exhaust.getEngineSlot();
                float boost = ship.isEngineBoostActive() ? 1.2f : 1f;
                float alpha = ship.getExtraAlphaMult() * viewport.getAlphaMult();
                drawGlow(outline, exhaust, slot.getWidth() * 3f * boost,
                        slot.getLength() * 1.6f * boost, alpha * 0.25f);
                drawGlow(glow, exhaust, slot.getWidth() * 2f * boost,
                        slot.getLength() * 1.3f * boost, alpha);
            }
        }

        private void drawGlow(SpriteAPI sprite, ShipEngineAPI exhaust, float width, float height, float alpha) {
            sprite.setAdditiveBlend();
            sprite.setColor(ENGINE_COLOR);
            sprite.setAlphaMult(alpha);
            sprite.setSize(width, height);
            sprite.setAngle(ship.getFacing() + exhaust.getEngineSlot().getAngle() - 90f);
            sprite.renderAtCenter(exhaust.getLocation().x, exhaust.getLocation().y);
        }

        @Override
        public boolean isExpired() {
            return !ship.isAlive() || !isCarina(ship);
        }

        @Override
        public void cleanup() {
            originalGlow.forEach(EngineSlotAPI::setGlowSizeMult);
            ship.removeCustomData(STATE_KEY);
        }
    }
}
