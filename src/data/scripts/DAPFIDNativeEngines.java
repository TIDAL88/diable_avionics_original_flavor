package data.scripts;

import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.combat.entities.Ship;
import com.fs.starfarer.loading.specs.E;
import com.fs.starfarer.loading.specs.EngineSlot;
import com.fs.starfarer.loading.specs.classsuper;
import org.json.JSONObject;
import org.lwjgl.util.vector.Vector2f;
import java.util.*;

/** RC7 native engine layout adapter. Each layer gets its own normally initialized engine component. */
final class DAPFIDNativeEngines {
    private static final Vector2f ZERO = new Vector2f();
    interface Backend {
        List<EngineSlot> slots();
        void stop();
        void start();
        List<ShipEngineAPI> engines();
        void rebuild();
        void forget(Collection<EngineSlot> slots);
    }

    static Backend backend(Ship ship) {
        return new Backend() {
            public List<EngineSlot> slots() { return ship.getEngineLocations(); }
            public void stop() { ship.getEngineController().cleanup(); }
            public void start() { ship.getEngineController().init(); }
            public List<ShipEngineAPI> engines() { return ship.getEngineController().getShipEngines(); }
            public void rebuild() {
                ship.getEngineController().getEngines().clear();
                ship.getEngineController().initDamageableEngineComponents();
            }
            public void forget(Collection<EngineSlot> slots) {
                ship.getEngineController().getEngineGlow().ØO0000().keySet().removeAll(slots);
            }
        };
    }

    static final class Factory {
        private final E.o custom = E.o.valueOf("CUSTOM");
        private final JSONObject style;
        Factory(JSONObject style) {
            this.style = style;
        }
        EngineSlot create(int index, float width, float length, float angle, Vector2f position,
                          boolean systemActivated) {
            try {
                E definition = new E(new classsuper("PFID_" + index,
                        new Vector2f(position)), custom, width, length, angle);
                definition.o00000(style, "PFID_Engine");
                definition.Ò00000(systemActivated);
                return definition.Ô00000();
            } catch (org.json.JSONException ex) {
                throw new IllegalStateException("Unable to construct native PFID emitter", ex);
            }
        }
    }

    static final class Layer {
        final EngineSlot original, emitter;
        final float angleOffset, rearOffset;
        Layer(EngineSlot original, EngineSlot emitter, float angleOffset, float rearOffset) {
            this.original = original; this.emitter = emitter;
            this.angleOffset = angleOffset; this.rearOffset = rearOffset;
        }
        void follow() {
            Vector2f point = original.computePosition(ZERO, 0f);
            double angle = Math.toRadians(original.getAngle());
            point.x += (float) Math.cos(angle) * rearOffset;
            point.y += (float) Math.sin(angle) * rearOffset;
            emitter.getNode().Ò00000().set(point);
            emitter.setAngle(original.getAngle() + angleOffset);
            emitter.setSystemActivated(original.isSystemActivated());
        }
    }

    static List<Layer> layers(List<EngineSlot> originals, Factory factory) {
        List<Layer> layers = new ArrayList<>();
        List<EngineSlot> sorted = new ArrayList<>(originals);
        sorted.sort(Comparator.comparingDouble(slot -> slot.computePosition(ZERO, 0f).x));
        Map<EngineSlot, Float> sides = new IdentityHashMap<>();
        List<EngineSlot> bank = new ArrayList<>();
        float previousX = Float.NEGATIVE_INFINITY;
        for (EngineSlot slot : sorted) {
            float x = slot.computePosition(ZERO, 0f).x;
            if (!bank.isEmpty() && x - previousX > Math.max(10f, slot.getWidth() * 1.5f)) {
                assignSides(bank, sides); bank.clear();
            }
            bank.add(slot); previousX = x;
        }
        assignSides(bank, sides);
        float[] widths = {9f, 21f, 4f}, lengths = {21f, 7.5f, 58.5f};
        for (int layer = 0; layer < 3; layer++) {
            for (EngineSlot slot : originals) {
                float scale = slot.getWidth() / 9f;
                if (scale <= 0f) continue;
                float tilt = sides.get(slot) * (layer == 0 ? 12f : layer == 2 ? 2.5f : 0f);
                float rear = layer == 1 ? scale : 0f;
                EngineSlot emitter = factory.create(layers.size(), widths[layer] * scale,
                        lengths[layer] * scale, slot.getAngle() + tilt,
                        slot.computePosition(ZERO, 0f), slot.isSystemActivated());
                Layer result = new Layer(slot, emitter, tilt, rear);
                result.follow(); layers.add(result);
            }
        }
        return layers;
    }
    private static void assignSides(List<EngineSlot> bank, Map<EngineSlot, Float> sides) {
        if (bank.isEmpty()) return;
        List<Float> positions = new ArrayList<>();
        for (EngineSlot slot : bank) positions.add(slot.computePosition(ZERO, 0f).y);
        Collections.sort(positions);
        int n = positions.size();
        float middle = (positions.get((n - 1) / 2) + positions.get(n / 2)) * 0.5f;
        for (EngineSlot slot : bank) sides.put(slot, Math.signum(slot.computePosition(ZERO, 0f).y - middle));
    }

    static final class Binding {
        final Backend backend;
        final List<EngineSlot> originals, emitters;
        final List<Layer> layers;
        boolean active;
        Binding(Backend backend, Factory factory) {
            this.backend = backend;
            originals = new ArrayList<>(backend.slots());
            layers = layers(originals, factory);
            emitters = new ArrayList<>();
            for (Layer layer : layers) emitters.add(layer.emitter);
            Map<EngineSlot, Damage> damage = damageBySlot(backend.engines());
            backend.stop();
            backend.slots().clear(); backend.slots().addAll(emitters);
            try {
                backend.rebuild();
                Map<EngineSlot, Damage> inherited = new IdentityHashMap<>();
                for (Layer layer : layers) inherited.put(layer.emitter, damage.get(layer.original));
                restoreDamage(backend.engines(), inherited);
                backend.start(); active = true;
            } catch (RuntimeException ex) {
                backend.stop();
                backend.slots().clear(); backend.slots().addAll(originals);
                backend.forget(emitters); backend.rebuild();
                restoreDamage(backend.engines(), damage); backend.start();
                throw ex;
            }
        }
        void restore() {
            if (!active) return;
            Map<EngineSlot, Damage> current = damageBySlot(backend.engines());
            Map<EngineSlot, Damage> restored = new IdentityHashMap<>();
            for (Layer layer : layers) {
                Damage value = current.get(layer.emitter);
                if (value != null) restored.merge(layer.original, value, Damage::merge);
            }
            backend.stop();
            backend.slots().clear(); backend.slots().addAll(originals);
            backend.forget(emitters); backend.rebuild();
            restoreDamage(backend.engines(), restored);
            backend.start(); active = false;
        }
    }

    private record Damage(float fraction, boolean disabled, boolean permanent) {
        Damage merge(Damage other) {
            return new Damage(Math.min(fraction, other.fraction), disabled || other.disabled,
                    permanent || other.permanent);
        }
    }

    private static Map<EngineSlot, Damage> damageBySlot(List<ShipEngineAPI> engines) {
        Map<EngineSlot, Damage> result = new IdentityHashMap<>();
        for (ShipEngineAPI engine : engines) {
            float fraction = engine.getMaxHitpoints() > 0f
                    ? Math.max(0f, Math.min(1f, engine.getHitpoints() / engine.getMaxHitpoints())) : 1f;
            result.put((EngineSlot) engine.getEngineSlot(),
                    new Damage(fraction, engine.isDisabled(), engine.isPermanentlyDisabled()));
        }
        return result;
    }

    private static void restoreDamage(List<ShipEngineAPI> engines, Map<EngineSlot, Damage> damage) {
        for (ShipEngineAPI engine : engines) {
            Damage value = damage.get(engine.getEngineSlot());
            if (value == null) continue;
            if (value.disabled || value.permanent) engine.disable(value.permanent);
            else engine.setHitpoints(engine.getMaxHitpoints() * value.fraction);
        }
    }
}
