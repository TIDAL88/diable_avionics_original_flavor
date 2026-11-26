package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.combat.entities.Ship;
import org.magiclib.paintjobs.MagicPaintjobManager;
import org.magiclib.paintjobs.MagicPaintjobSpec;
import java.util.*;

/** Named native PFID engine profiles, shared by ships and inherited fighter paintjobs. */
public class DAPFIDEngineVisuals extends BaseEveryFrameCombatPlugin {
    private static final String FAILED_KEY = "diableavionics_pfid_native_failed";
    private CombatEngineAPI engine;
    private String defaultProfile = "bright";
    private final Map<String, DAPFIDNativeEngines.Factory> factories = new HashMap<>();
    private final Map<String, String> hullProfiles = new HashMap<>();
    private final Map<ShipAPI, DAPFIDNativeEngines.Binding> bindings = new IdentityHashMap<>();
    private static final Map<String, List<MagicPaintjobSpec>> fighterPaintjobs = new HashMap<>();
    private static final Map<String, Integer> fighterTextureIds = new HashMap<>();
    private long frame;

    @Override
    public void init(CombatEngineAPI engine) {
        bindings.clear();
        frame = 0;
        fighterPaintjobs.clear();
        fighterTextureIds.clear();
        factories.clear();
        hullProfiles.clear();
        this.engine = engine;
        try {
            // Keep runtime styles beside their layouts, independent of the shared engine_styles.json path.
            org.json.JSONObject config = Global.getSettings()
                    .loadJSON("data/config/pfid_engine_profiles.json", "diableavionics");
            org.json.JSONObject styles = config.getJSONObject("engineStyles");
            defaultProfile = config.getString("defaultProfile");
            org.json.JSONObject profiles = config.getJSONObject("profiles");
            for (Iterator<?> names = profiles.keys(); names.hasNext();) {
                String name = (String) names.next();
                org.json.JSONObject profile = profiles.getJSONObject(name);
                factories.put(name, new DAPFIDNativeEngines.Factory(
                        styles.getJSONObject(profile.getString("engineStyle")), profile));
            }
            org.json.JSONObject assignments = config.getJSONObject("hullProfiles");
            for (Iterator<?> hulls = assignments.keys(); hulls.hasNext();) {
                String hull = (String) hulls.next();
                String profile = assignments.getString(hull);
                if (!factories.containsKey(profile)) throw new IllegalArgumentException("Unknown PFID profile: " + profile);
                hullProfiles.put(hull, profile);
            }
            if (!factories.containsKey(defaultProfile)) throw new IllegalArgumentException("Unknown default PFID profile");
        } catch (Exception | LinkageError ex) {
            // Exhaust is cosmetic: a bad configuration must not prevent the title screen or combat loading.
            this.engine = null;
            factories.clear();
            hullProfiles.clear();
            Global.getLogger(DAPFIDEngineVisuals.class).error(
                    "PFID native exhaust disabled for this combat; original engines retained. "
                            + "Check data/config/pfid_engine_profiles.json and the game libraries.", ex);
        }
    }

    static boolean isPFID(ShipAPI ship) {
        if (ship == null) return false;
        if (DAPaintjobDeepStrikeSprites.isPFIDPod(ship)) return true;
        if (DAPaintjobDroneSprites.isPFIDDrone(ship)) return true;
        if (ship.getVariant() != null) {
            MagicPaintjobSpec paintjob = MagicPaintjobManager.getCurrentShipPaintjob(ship.getVariant());
            if (paintjob != null && "PFID".equals(paintjob.getPaintjobFamily())) return true;
        }
        if (!ship.isFighter() || ship.getHullSpec() == null) return false;
        SpriteAPI sprite = ship.getSpriteAPI();
        if (sprite == null) return false;
        int textureId = sprite.getTextureId();
        if (textureId == 0) return false;
        for (MagicPaintjobSpec paintjob : fighterPaintjobs.computeIfAbsent(ship.getHullSpec().getHullId(),
                id -> MagicPaintjobManager.getPaintjobsForHull(id, true, true))) {
            if (!"PFID".equals(paintjob.getPaintjobFamily())) continue;
            Integer paintedTextureId = fighterTextureIds.computeIfAbsent(paintjob.getSpriteId(),
                    DAPFIDEngineVisuals::getLoadedTextureId);
            if (paintedTextureId != null && paintedTextureId == textureId) return true;
        }
        return false;
    }

    private static Integer getLoadedTextureId(String spriteId) {
        SpriteAPI sprite = Global.getSettings().getSprite(spriteId);
        int textureId = sprite == null ? 0 : sprite.getTextureId();
        // A texture that isn't ready yet should be checked again next time.
        return textureId == 0 ? null : textureId;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine == null || engine.isPaused()) return;
        long currentFrame = ++frame;
        for (ShipAPI ship : engine.getShips()) {
            if (!ship.isAlive() || !isPFID(ship) || !(ship instanceof Ship nativeShip)) continue;
            if (Boolean.TRUE.equals(ship.getCustomData().get(FAILED_KEY))) continue;
            DAPFIDNativeEngines.Binding binding = bindings.get(ship);
            if (binding == null && !nativeShip.getEngineLocations().isEmpty()) {
                try {
                    binding = new DAPFIDNativeEngines.Binding(DAPFIDNativeEngines.backend(nativeShip),
                            factories.get(profileFor(ship)));
                    bindings.put(ship, binding);
                } catch (RuntimeException ex) {
                    ship.setCustomData(FAILED_KEY, true);
                    Global.getLogger(DAPFIDEngineVisuals.class).error("Native PFID exhaust could not be applied to "
                            + ship.getHullSpec().getHullId() + "; original engines retained", ex);
                }
            }
            if (binding != null) binding.lastSeenFrame = currentFrame;
        }
        Iterator<DAPFIDNativeEngines.Binding> iterator = bindings.values().iterator();
        while (iterator.hasNext()) {
            DAPFIDNativeEngines.Binding binding = iterator.next();
            if (binding.lastSeenFrame != currentFrame) {
                binding.restore();
                iterator.remove();
            }
        }
    }

    String profileFor(ShipAPI ship) {
        ShipHullSpecAPI hull = ship.getHullSpec();
        if (hull == null) return defaultProfile;
        String profile = hullProfiles.get(hull.getHullId());
        if (profile == null) profile = hullProfiles.get(hull.getBaseHullId());
        return profile == null ? defaultProfile : profile;
    }
}
