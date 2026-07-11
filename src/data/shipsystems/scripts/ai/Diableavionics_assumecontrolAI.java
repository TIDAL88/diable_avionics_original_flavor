package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class Diableavionics_assumecontrolAI implements ShipSystemAIScript {
    private CombatEngineAPI engine;
    private ShipAPI ship;
    private ShipSystemAPI system;
    @Override
    public void init(ShipAPI shipAPI, ShipSystemAPI shipSystemAPI, ShipwideAIFlags shipwideAIFlags, CombatEngineAPI combatEngineAPI) {
        this.ship = shipAPI;
        this.engine = combatEngineAPI;
        this.system = shipSystemAPI;
    }

    @Override
    public void advance(float v, Vector2f vector2f, Vector2f vector2f1, ShipAPI shipAPI) {
        if (ship == null || engine == null || engine.isPaused() || !ship.isAlive() || system == null) {
            return;

        }
        boolean dead_wing = false;
        List<FighterWingAPI> wings = ship.getAllWings();
        for (FighterWingAPI wing : wings) {
            if (wing.isDestroyed()) {
                dead_wing = true;
            }
        }
        if (ship.areAnyEnemiesInRange() && !dead_wing && system.canBeActivated()) {
            ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
            }
        }
    }
