package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class Diableavionics_vaporAI implements ShipSystemAIScript {

    private CombatEngineAPI engine;
    private ShipAPI ship;
    private ShipSystemAPI system;

    private float timer = 4f;

    private float getAggroRange(ShipAPI enemy) {
        if (enemy == null) return 0f;

        return switch (enemy.getHullSize()) {
            case FRIGATE -> 500f;
            case DESTROYER -> 600f;
            case CRUISER -> 700f;
            case CAPITAL_SHIP -> 800f;
            default -> 400f;
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
    public void advance(
            float amount,
            Vector2f missileDangerDir,
            Vector2f collisionDangerDir,
            ShipAPI target
    ) {
        if (engine == null || ship == null || system == null || engine.isPaused()) return;

        int charges = ship.getSystem().getMaxAmmo();

        if (ship.getSystem().getAmmo() > charges - 1 && !ship.areAnyEnemiesInRange()) {
            ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
            return;
        }

        ShipAPI nearestEnemy = AIUtils.getNearestEnemy(ship);
        if (nearestEnemy == null) return;

        float aggroRange = getAggroRange(nearestEnemy);
        float distance = MathUtils.getDistance(ship, nearestEnemy);

        if (ship.getFluxTracker().getFluxLevel() >= 0.8f
                && ship.getSystem().getAmmo() >= 1) {
            ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
            return;
        }

        if (ship.getSystem().getAmmo() > charges - 1
                && distance > aggroRange
                && timer <= 0f) {
            ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
            timer = 4f;
            return;
        }

        if (ship.getSystem().getAmmo() > charges - 1
                && distance < aggroRange) {
            ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
            return;
        }

        if (timer > 0) timer -= amount;
    }
}