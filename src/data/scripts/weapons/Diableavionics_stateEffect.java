package data.scripts.weapons;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;

public class Diableavionics_stateEffect implements BeamEffectPlugin {

    private boolean runOnce = false;
    private float time = 0f;
    private float offset = 0f;
    private WeaponSpecAPI specs;

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {

        if (engine.isPaused()
                || beam.getWeapon().getShip().getOriginalOwner() == -1) {
            return;
        }

        if (!runOnce) {
            runOnce = true;
            beam.getWeapon().ensureClonedSpec();
            specs = beam.getWeapon().getSpec();
            offset = MathUtils.getRandomNumberInRange(0f, 100f);
        }

        time += amount * 2f;

        float angle = (float) (
                FastTrig.sin((time + offset) * 1.1f) / 2f
                        + FastTrig.sin((time + offset) * 2.9f) / 3
        ) * 0.5f;

        specs.getHardpointAngleOffsets().set(0, angle);
        specs.getTurretAngleOffsets().set(0, angle);
        specs.getHiddenAngleOffsets().set(0, angle);
    }
}
