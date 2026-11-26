package data.scripts;

import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.combat.entities.Ship;
import com.fs.starfarer.loading.specs.O0OO;
import com.fs.starfarer.loading.specs.EngineSlot;
import com.fs.starfarer.loading.specs.o0Oo;
import org.json.JSONObject;
import org.lwjgl.util.vector.Vector2f;
import java.util.*;

/** RC8 native engine layout adapter. Each layer gets its own normally initialized engine component. */
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
                ship.getEngineController().getEngineGlow().ÒO0000().keySet().removeAll(slots);
            }
        };
    }

    static final class Factory {
        private final O0OO.o custom = O0OO.o.valueOf("CUSTOM");
        private final JSONObject style;
        private final String styleId;
        private final boolean mapped;
        private final float referenceWidth, bankGap, bankWidthMultiplier;
        private final float[] widths, lengths, angles, rearOffsets;
        private final List<ReferenceNozzle> referenceNozzles = new ArrayList<>();
        Factory(JSONObject style, JSONObject profile) {
            this.style = style;
            try {
                styleId = profile.getString("engineStyle");
                String layout = profile.getString("layout");
                if (!"mapped".equals(layout) && !"layered".equals(layout))
                    throw new IllegalArgumentException("Unknown PFID engine layout: " + layout);
                mapped = "mapped".equals(layout);
                referenceWidth = (float) profile.optDouble("referenceWidth", 9);
                bankGap = (float) profile.optDouble("bankGap", 10);
                bankWidthMultiplier = (float) profile.optDouble("bankWidthMultiplier", 1.5);
                if (mapped) {
                    var nozzles = profile.getJSONArray("referenceNozzles");
                    for (int i = 0; i < nozzles.length(); i++) {
                        JSONObject nozzle = nozzles.getJSONObject(i);
                        referenceNozzles.add(new ReferenceNozzle((float) nozzle.getDouble("lateral"),
                                (float) nozzle.getDouble("width"), (float) nozzle.getDouble("length"),
                                (float) nozzle.getDouble("angle")));
                    }
                    if (referenceNozzles.isEmpty()) throw new IllegalArgumentException("Empty PFID clean profile");
                    widths = lengths = angles = rearOffsets = new float[0];
                } else {
                    widths = numbers(profile, "widths"); lengths = numbers(profile, "lengths");
                    angles = numbers(profile, "angles"); rearOffsets = numbers(profile, "rearOffsets");
                    if (widths.length == 0 || widths.length != lengths.length || widths.length != angles.length
                            || widths.length != rearOffsets.length || referenceWidth <= 0)
                        throw new IllegalArgumentException("Invalid PFID bright layer dimensions");
                }
            } catch (org.json.JSONException ex) {
                throw new IllegalArgumentException("Invalid PFID engine profile", ex);
            }
        }
        private static float[] numbers(JSONObject profile, String key) throws org.json.JSONException {
            var array = profile.getJSONArray(key);
            float[] result = new float[array.length()];
            for (int i = 0; i < result.length; i++) result[i] = (float) array.getDouble(i);
            return result;
        }
        EngineSlot create(int index, float width, float length, float angle, Vector2f position,
                          boolean systemActivated) {
            try {
                O0OO definition = new O0OO(new o0Oo("PFID_" + index,
                        new Vector2f(position)), custom, width, length, angle);
                definition.o00000(style, styleId);
                definition.Ò00000(systemActivated);
                return definition.Ô00000();
            } catch (org.json.JSONException ex) {
                throw new IllegalStateException("Unable to construct native PFID emitter", ex);
            }
        }
    }

    private record ReferenceNozzle(float lateral, float width, float length, float angle) {}

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
        if (factory.mapped) {
            if (originals.isEmpty()) return layers;
            float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY;
            float referenceMin = Float.POSITIVE_INFINITY, referenceMax = Float.NEGATIVE_INFINITY;
            for (EngineSlot slot : originals) {
                float lateral = slot.computePosition(ZERO, 0f).y;
                min = Math.min(min, lateral); max = Math.max(max, lateral);
            }
            for (ReferenceNozzle nozzle : factory.referenceNozzles) {
                referenceMin = Math.min(referenceMin, nozzle.lateral);
                referenceMax = Math.max(referenceMax, nozzle.lateral);
            }
            for (EngineSlot slot : originals) {
                if (slot.getWidth() <= 0f) continue;
                float fraction = max > min ? (slot.computePosition(ZERO, 0f).y - min) / (max - min) : 0.5f;
                float lateral = referenceMin + fraction * (referenceMax - referenceMin);
                ReferenceNozzle nearest = factory.referenceNozzles.get(0);
                for (ReferenceNozzle candidate : factory.referenceNozzles)
                    if (Math.abs(candidate.lateral - lateral) < Math.abs(nearest.lateral - lateral)) nearest = candidate;
                float length = Math.round(nearest.length * slot.getWidth() / nearest.width * 10000f) / 10000f;
                float tilt = nearest.angle - 180f;
                EngineSlot emitter = factory.create(layers.size(), slot.getWidth(), length,
                        slot.getAngle() + tilt, slot.computePosition(ZERO, 0f), slot.isSystemActivated());
                layers.add(new Layer(slot, emitter, tilt, 0f));
            }
            return layers;
        }
        List<EngineSlot> sorted = new ArrayList<>(originals);
        sorted.sort(Comparator.comparingDouble(slot -> slot.computePosition(ZERO, 0f).x));
        Map<EngineSlot, Float> sides = new IdentityHashMap<>();
        List<EngineSlot> bank = new ArrayList<>();
        float previousX = Float.NEGATIVE_INFINITY;
        for (EngineSlot slot : sorted) {
            float x = slot.computePosition(ZERO, 0f).x;
            if (!bank.isEmpty() && x - previousX > Math.max(factory.bankGap, slot.getWidth() * factory.bankWidthMultiplier)) {
                assignSides(bank, sides); bank.clear();
            }
            bank.add(slot); previousX = x;
        }
        assignSides(bank, sides);
        for (int layer = 0; layer < factory.widths.length; layer++) {
            for (EngineSlot slot : originals) {
                float scale = slot.getWidth() / factory.referenceWidth;
                if (scale <= 0f) continue;
                float tilt = sides.get(slot) * factory.angles[layer];
                float rear = factory.rearOffsets[layer] * scale;
                EngineSlot emitter = factory.create(layers.size(), factory.widths[layer] * scale,
                        factory.lengths[layer] * scale, slot.getAngle() + tilt,
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
        long lastSeenFrame;
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
