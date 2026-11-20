package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;

/** Source-paintjob inheritance for system-launched Snap Drones, shared by Carina Sentinel and PFID. */
final class DAPaintjobDroneSprites {
    private static final String KEY = "diableavionics_carina_snap_drone";
    private static final String HULL = "diableavionics_warlust_drone";

    private DAPaintjobDroneSprites() {}

    static boolean isCarinaDrone(ShipAPI ship) {
        return hasFamily(ship, "carinasentinel");
    }

    static boolean isPFIDDrone(ShipAPI ship) {
        return hasFamily(ship, "PFID");
    }

    private static boolean hasFamily(ShipAPI ship, String family) {
        return ship != null && ship.getCustomData() != null
                && ship.getCustomData().get(KEY) instanceof Original original && family.equals(original.family);
    }

    static void update(ShipAPI drone) {
        if (!drone.isAlive() || drone.getHullSpec() == null || !HULL.equals(drone.getHullSpec().getHullId())) return;
        ShipAPI source = drone.getDroneSource();
        // The engine may assign the source after the drone's first combat frame.
        if (source == null || source == drone) return;
        Original original = (Original) drone.getCustomData().get(KEY);
        String family = DACarinaSentinelVisuals.isCarina(source) ? "carinasentinel"
                : DAPFIDEngineVisuals.isPFID(source) ? "PFID" : null;
        if (family == null) {
            if (original != null) {
                drone.setSprite(original.sprite);
                drone.removeCustomData(KEY);
            }
            return;
        }
        if ((original != null && family.equals(original.family)) || drone.getSpriteAPI() == null) return;
        SpriteAPI current = drone.getSpriteAPI();
        SpriteAPI stock = original == null ? current : original.sprite;
        String texture = "graphics/" + family + "/ships/warlust/diableavionics_warlust_drone.png";
        try {
            Global.getSettings().loadTexture(texture);
        } catch (java.io.IOException ex) {
            throw new RuntimeException("Unable to load " + family + " Snap Drone sprite", ex);
        }
        SpriteAPI painted = Global.getSettings().getSprite(texture);
        painted.setSize(current.getWidth(), current.getHeight());
        painted.setCenter(current.getCenterX(), current.getCenterY());
        painted.setAlphaMult(current.getAlphaMult());
        painted.setAngle(current.getAngle());
        painted.setColor(current.getColor());
        // Touch only the combat instance; system drones can share a stored variant.
        drone.setSprite(painted);
        drone.setCustomData(KEY, new Original(stock, family));
    }

    private record Original(SpriteAPI sprite, String family) {}
}
