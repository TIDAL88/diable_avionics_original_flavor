package data.shipsystems.scripts.ai

import com.fs.starfarer.api.combat.*
import org.lwjgl.util.vector.Vector2f

class Diableavionics_grazerAI : ShipSystemAIScript {
    private var engine: CombatEngineAPI? = null
    private var ship: ShipAPI? = null
    private var system: ShipSystemAPI? = null
    override fun init(
        shipAPI: ShipAPI?,
        shipSystemAPI: ShipSystemAPI?,
        shipwideAIFlags: ShipwideAIFlags?,
        combatEngineAPI: CombatEngineAPI?
    ) {
        this.engine = combatEngineAPI
        this.ship = shipAPI
        this.system = shipSystemAPI
    }

    override fun advance(v: Float, vector2f: Vector2f?, vector2f1: Vector2f?, shipAPI: ShipAPI?) {
        var timer = 0f
        if (engine == null || ship == null || system == null || engine!!.isPaused) {
            return
        }
        if (ship!!.system.ammo >= 3 && !ship!!.areAnyEnemiesInRange()) {
            ship!!.giveCommand(ShipCommand.USE_SYSTEM, null, 0)
            return
        }
        if (ship!!.fluxTracker.fluxLevel >= 0.8f && ship!!.system.ammo >= 1) {
            ship!!.shield.toggleOff()
            while (timer <= 3) {
                ship!!.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK)
                timer++
            }
            ship!!.giveCommand(ShipCommand.USE_SYSTEM, null, 0)
        } else {
            ship!!.blockCommandForOneFrame(ShipCommand.USE_SYSTEM)
        }
    }
}
