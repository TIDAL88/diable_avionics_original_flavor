package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class Diableavionics_grazerAI implements ShipSystemAIScript {
    private CombatEngineAPI engine;
    private ShipAPI ship;
    private ShipSystemAPI system;
    private float getAggroRange(ShipAPI enemy) {
        if (enemy == null) return 0f;
        return switch (enemy.getHullSize()) {
            case FRIGATE    -> 800f;
            case DESTROYER  ->  1000f;
            case CRUISER    -> 1200f;
            case CAPITAL_SHIP-> 1400f;
            default         -> 600f;
        };
    }

    @Override
    public void init(
            ShipAPI shipAPI,
            ShipSystemAPI shipSystemAPI,
            ShipwideAIFlags shipwideAIFlags,
            CombatEngineAPI combatEngineAPI
    ) {
        this.engine = combatEngineAPI;
        this.ship = shipAPI;
        this.system = shipSystemAPI;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        float timer = 0f;
        if (engine == null || ship == null || system == null || engine.isPaused()) {
            return;
        }
        ShipAPI nearestEnemy= AIUtils.getNearestEnemy(ship);
        if (nearestEnemy==null){
            return;
        }
        float aggroRange = getAggroRange(nearestEnemy);
        float distance = MathUtils.getDistance(ship, nearestEnemy);
        if (ship.getSystem().getAmmo() >= 3 && !ship.areAnyEnemiesInRange()) {
            ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
            return;
        }

        if (ship.getFluxTracker().getFluxLevel() >= 0.8f && ship.getSystem().getAmmo() >= 1) {
            if (ship.getShield() != null) {
                ship.getShield().toggleOff();
            }

            while (timer <= 3) {
                ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
                timer++;
            }
            ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
            ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK,null,0);
        }
        if (ship.getSystem().getAmmo()>=3 && distance>aggroRange){
            ship.giveCommand(ShipCommand.USE_SYSTEM,null,1);
        }
    }
}