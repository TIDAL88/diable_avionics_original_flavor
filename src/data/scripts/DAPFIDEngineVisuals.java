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

    @Override
    public void init(CombatEngineAPI engine) {
        bindings.clear();
        fighterPaintjobs.clear();
        factories.clear();
        hullProfiles.clear();
        this.engine = engine;
        try {
            org.json.JSONObject styles = Global.getSettings()
                    .loadJSON("data/config/engine_styles.json", "diableavionics");
            org.json.JSONObject config = Global.getSettings()
                    .loadJSON("data/config/pfid_engine_profiles.json", "diableavionics");
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
        } catch (Exception ex) {
            throw new RuntimeException("Unable to load native PFID engine settings", ex);
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
        if (!ship.isFighter() || ship.getHullSpec() == null || ship.getSpriteAPI() == null) return false;
        SpriteAPI sprite = ship.getSpriteAPI();
        if (sprite.getTextureId() == 0) return false;
        for (MagicPaintjobSpec paintjob : fighterPaintjobs.computeIfAbsent(ship.getHullSpec().getHullId(),
                id -> MagicPaintjobManager.getPaintjobsForHull(id, true, true))) {
            if (!"PFID".equals(paintjob.getPaintjobFamily())) continue;
            SpriteAPI painted = Global.getSettings().getSprite(paintjob.getSpriteId());
            if (painted != null && painted.getTextureId() == sprite.getTextureId()) return true;
        }
        return false;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine == null || engine.isPaused()) return;
        Set<ShipAPI> present = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ShipAPI ship : engine.getShips()) {
            if (!ship.isAlive() || !isPFID(ship) || !(ship instanceof Ship nativeShip)) continue;
            if (Boolean.TRUE.equals(ship.getCustomData().get(FAILED_KEY))) continue;
            present.add(ship);
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
        }
        Iterator<Map.Entry<ShipAPI, DAPFIDNativeEngines.Binding>> iterator = bindings.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ShipAPI, DAPFIDNativeEngines.Binding> entry = iterator.next();
            if (!present.contains(entry.getKey())) {
                entry.getValue().restore();
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
