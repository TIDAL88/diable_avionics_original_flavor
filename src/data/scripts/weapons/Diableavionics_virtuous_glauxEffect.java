package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPlugin;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.ReadableVector2f;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

public class Diableavionics_virtuous_glauxEffect implements BeamEffectPlugin {
    private boolean hasFired = false;
    private float random = 1.0F;
    private final float WIDTH = 25.0F;
    private final float PARTICLES = 5.0F;
    private final IntervalUtil timer = new IntervalUtil(0.1F, 0.1F);
    private final String id = "diableavionics_zephyr_firing";
    private final List<Vector2f> PODS = new ArrayList();

    public Diableavionics_virtuous_glauxEffect() {
        this.PODS.add(new Vector2f(17.5F, 25.5F));
        this.PODS.add(new Vector2f(27.5F, 24.5F));
        this.PODS.add(new Vector2f(37.5F, 23.5F));
    }

    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        if (!engine.isPaused()) {
            if (beam.getBrightness() == 1.0F) {
                Vector2f start = beam.getFrom();
                Vector2f end = beam.getTo();
                if (beam.didDamageThisFrame() && beam.getDamageTarget().getCollisionClass() == CollisionClass.FIGHTER) {
                    float damage = beam.getDamage().computeDamageDealt(0.1F);
                    engine.applyDamage(beam.getDamageTarget(), end, damage, DamageType.ENERGY, damage / 2.0F, false, true, beam.getSource());
                }

                if (MathUtils.getDistanceSquared(start, end) == 0.0F) {
                    return;
                }

                this.timer.advance(amount);
                if (this.timer.intervalElapsed()) {
                    this.hasFired = false;
                    if (MagicRender.screenCheck(0.1F, start)) {
                        WeaponAPI weapon = beam.getWeapon();
                        ShipAPI ship = beam.getSource();
                        Vector2f loc = new Vector2f((ReadableVector2f)this.PODS.get(MathUtils.getRandomNumberInRange(0, 2)));
                        VectorUtils.rotate(loc, weapon.getCurrAngle());
                        Vector2f.add(loc, weapon.getLocation(), loc);
                        loc = MathUtils.getRandomPointInCircle(loc, 5.0F);
                        Vector2f vel = MathUtils.getPoint(new Vector2f(ship.getVelocity()), (float)MathUtils.getRandomNumberInRange(20, 50), weapon.getCurrAngle() + 45.0F);
                        float size = (float)MathUtils.getRandomNumberInRange(8, 16);
                        float glowth = (float)MathUtils.getRandomNumberInRange(32, 64);
                        MagicRender.battlespace(Global.getSettings().getSprite("fx", "zap_0" + MathUtils.getRandomNumberInRange(0, 7)), new Vector2f(loc), new Vector2f(vel), new Vector2f(size, size), new Vector2f(glowth, glowth), (float)MathUtils.getRandomNumberInRange(0, 360), (float)MathUtils.getRandomNumberInRange(-15, 15), new Color(100, 255, 255, 255), true, 0.0F, 0.0F, 2.0F, 1.0F, 0.0F, 0.0F, MathUtils.getRandomNumberInRange(0.1F, 0.2F), MathUtils.getRandomNumberInRange(0.15F, 0.25F), CombatEngineLayers.FIGHTERS_LAYER);

                        for(int i = 0; (float)i < 5.0F; ++i) {
                            Vector2f point = MathUtils.getPointOnCircumference(start, (float)Math.random() * 300.0F, weapon.getCurrAngle());
                            Vector2f.add(point, MathUtils.getRandomPointInCircle(new Vector2f(), 8.333333F), point);
                            vel = MathUtils.getPointOnCircumference(ship.getVelocity(), 12.5F + (float)Math.random() * 25.0F, ship.getFacing());
                            engine.addHitParticle(point, vel, 3.0F + 7.0F * (float)Math.random(), 0.5F, 0.1F + (float) Math.random(), new Color(255, 200, 90, 255));
                        }

                        engine.addHitParticle(start, beam.getSource().getVelocity(), 50.0F + 50.0F * (float)Math.random(), 1.0F, 0.1F + 0.2F * (float)Math.random(), new Color(100, 150, 255, 255));
                        engine.addHitParticle(start, beam.getSource().getVelocity(), 40.0F, 1.0F, 0.05F, new Color(255, 255, 255, 255));
                    }
                }

                float theWidth = 25.0F * Math.min(1.0F, (float)FastTrig.cos((double)(56.548668F * Math.min(this.timer.getElapsed(), 0.05F))) + 1.0F);
                beam.setWidth(this.random * theWidth);
                if (!this.hasFired) {
                    this.hasFired = true;
                    Global.getSoundPlayer().playSound("diableavionics_zephyr_firing", 0.6F + 0.20F * (float)Math.random(), 3.0F, start, beam.getSource().getVelocity());
                }
            } else {
                this.hasFired = false;
            }

        }
    }
}
