package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.combat.entities.Ship;
import org.magiclib.paintjobs.MagicPaintjobManager;
import org.magiclib.paintjobs.MagicPaintjobSpec;
import java.util.*;

/** Mitsukurina-style native exhaust for PFID combat instances, including painted fighters. */
public class DAPFIDEngineVisuals extends BaseEveryFrameCombatPlugin {
    private static final String FAILED_KEY = "diableavionics_pfid_native_failed";
    private CombatEngineAPI engine;
    private DAPFIDNativeEngines.Factory factory;
    private final Map<ShipAPI, DAPFIDNativeEngines.Binding> bindings = new IdentityHashMap<>();
    private static final Map<String, List<MagicPaintjobSpec>> fighterPaintjobs = new HashMap<>();

    @Override
    public void init(CombatEngineAPI engine) {
        bindings.clear();
        fighterPaintjobs.clear();
        this.engine = engine;
        try {
            factory = new DAPFIDNativeEngines.Factory(Global.getSettings()
                    .loadJSON("data/config/engine_styles.json", "diableavionics").getJSONObject("PFID_Engine"));
        } catch (Exception ex) {
            throw new RuntimeException("Unable to load native PFID engine settings", ex);
        }
    }

    static boolean isPFID(ShipAPI ship) {
        if (ship == null) return false;
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
            // Baseline Maelstrom test already has the complete native layered layout.
            if (usesBaselinePFIDEngines(ship)) continue;
            if (!ship.isAlive() || !isPFID(ship) || !(ship instanceof Ship nativeShip)) continue;
            if (Boolean.TRUE.equals(ship.getCustomData().get(FAILED_KEY))) continue;
            present.add(ship);
            DAPFIDNativeEngines.Binding binding = bindings.get(ship);
            if (binding == null && !nativeShip.getEngineLocations().isEmpty()) {
                try {
                    binding = new DAPFIDNativeEngines.Binding(DAPFIDNativeEngines.backend(nativeShip), factory);
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

    static boolean usesBaselinePFIDEngines(ShipAPI ship) {
        ShipHullSpecAPI hull = ship.getHullSpec();
        return hull != null && ("diableavionics_maelstrom".equals(hull.getHullId())
                || "diableavionics_maelstrom".equals(hull.getBaseHullId()));
    }
}
