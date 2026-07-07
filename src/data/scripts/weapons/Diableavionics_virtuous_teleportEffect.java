//By Tartiflette
package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.scripts.util.MagicRender;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public class Diableavionics_virtuous_teleportEffect implements EveryFrameWeaponEffectPlugin {
    private ShipAPI ship;
    private ShipSystemAPI system;
    private final Map<Integer, String> bottomBubble = new HashMap<>();
    private final Map<Integer, String> topBubble;
    private Vector2f from;
    private boolean runOnce;

    public Diableavionics_virtuous_teleportEffect() {
        this.bottomBubble.put(0, "BUBBLE_bottom00");
        this.bottomBubble.put(1, "BUBBLE_bottom01");
        this.bottomBubble.put(2, "BUBBLE_bottom02");
        this.bottomBubble.put(3, "BUBBLE_bottom03");
        this.bottomBubble.put(4, "BUBBLE_bottom04");
        this.bottomBubble.put(5, "BUBBLE_bottom05");
        this.bottomBubble.put(6, "BUBBLE_bottom06");
        this.bottomBubble.put(7, "BUBBLE_bottom07");
        this.bottomBubble.put(8, "BUBBLE_bottom08");
        this.bottomBubble.put(9, "BUBBLE_bottom09");
        this.bottomBubble.put(10, "BUBBLE_bottom10");
        this.bottomBubble.put(11, "BUBBLE_bottom11");
        this.bottomBubble.put(12, "BUBBLE_bottom12");
        this.bottomBubble.put(13, "BUBBLE_bottom13");
        this.bottomBubble.put(14, "BUBBLE_bottom14");
        this.bottomBubble.put(15, "BUBBLE_bottom15");
        this.bottomBubble.put(16, "BUBBLE_bottom16");
        this.bottomBubble.put(17, "BUBBLE_bottom17");
        this.bottomBubble.put(18, "BUBBLE_bottom18");
        this.bottomBubble.put(19, "BUBBLE_bottom19");
        this.bottomBubble.put(20, "BUBBLE_bottom20");
        this.bottomBubble.put(21, "BUBBLE_bottom21");
        this.bottomBubble.put(22, "BUBBLE_bottom22");
        this.bottomBubble.put(23, "BUBBLE_bottom23");
        this.bottomBubble.put(24, "BUBBLE_bottom24");
        this.bottomBubble.put(25, "BUBBLE_bottom25");
        this.bottomBubble.put(26, "BUBBLE_bottom26");
        this.bottomBubble.put(27, "BUBBLE_bottom27");
        this.bottomBubble.put(28, "BUBBLE_bottom28");
        this.bottomBubble.put(29, "BUBBLE_bottom29");
        this.topBubble = new HashMap<>();
        this.topBubble.put(0, "BUBBLE_top00");
        this.topBubble.put(1, "BUBBLE_top01");
        this.topBubble.put(2, "BUBBLE_top02");
        this.topBubble.put(3, "BUBBLE_top03");
        this.topBubble.put(4, "BUBBLE_top04");
        this.topBubble.put(5, "BUBBLE_top05");
        this.topBubble.put(6, "BUBBLE_top06");
        this.topBubble.put(7, "BUBBLE_top07");
        this.topBubble.put(8, "BUBBLE_top08");
        this.topBubble.put(9, "BUBBLE_top09");
        this.topBubble.put(10, "BUBBLE_top10");
        this.topBubble.put(11, "BUBBLE_top11");
        this.topBubble.put(12, "BUBBLE_top12");
        this.topBubble.put(13, "BUBBLE_top13");
        this.topBubble.put(14, "BUBBLE_top14");
        this.topBubble.put(15, "BUBBLE_top15");
        this.topBubble.put(16, "BUBBLE_top16");
        this.topBubble.put(17, "BUBBLE_top17");
        this.topBubble.put(18, "BUBBLE_top18");
        this.topBubble.put(19, "BUBBLE_top19");
        this.topBubble.put(20, "BUBBLE_top20");
        this.topBubble.put(21, "BUBBLE_top21");
        this.topBubble.put(22, "BUBBLE_top22");
        this.topBubble.put(23, "BUBBLE_top23");
        this.topBubble.put(24, "BUBBLE_top24");
        this.topBubble.put(25, "BUBBLE_top25");
        this.topBubble.put(26, "BUBBLE_top26");
        this.topBubble.put(27, "BUBBLE_top27");
        this.topBubble.put(28, "BUBBLE_top28");
        this.topBubble.put(29, "BUBBLE_top29");
        this.from = new Vector2f();
        this.runOnce = false;
    }

    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (!Global.getCombatEngine().isPaused()) {
            if (this.runOnce && this.ship != null && this.system != null) {
                if (this.system.isActive()) {
                    this.flickerEffect();
                }

            } else {
                this.ship = weapon.getShip();
                this.system = this.ship.getSystem();
                this.runOnce = true;
            }
        }
    }

    private void flickerEffect() {
        float level = this.system.getEffectLevel();
        if (MagicRender.screenCheck(1.0F, this.ship.getLocation())) {
            float anim;
            if (this.system.isChargeup()) {
                anim = level * 14.5F;
                this.from = new Vector2f(this.ship.getLocation());
            } else if (this.system.isChargedown()) {
                anim = (1.0F - level) * 14.5F + 14.5F;
            } else {
                anim = 14.5F;
            }

            MagicRender.singleframe(Global.getSettings().getSprite("diableavionics", (String)this.topBubble.get((int)anim)), this.ship.getLocation(), new Vector2f(256.0F, 256.0F), VectorUtils.getFacing(this.ship.getVelocity()), new Color(1.0F, 1.0F, 1.0F, 0.5F + 0.25F * level), true, CombatEngineLayers.ABOVE_SHIPS_LAYER);
            MagicRender.singleframe(Global.getSettings().getSprite("diableavionics", (String)this.bottomBubble.get((int)anim)), this.ship.getLocation(), new Vector2f(256.0F, 256.0F), VectorUtils.getFacing(this.ship.getVelocity()), new Color(1.0F, 1.0F, 1.0F, 1.0F - 0.5F * level), false, CombatEngineLayers.BELOW_SHIPS_LAYER);
            if (level == 1.0F) {
                MagicRender.battlespace(Global.getSettings().getSprite("diableavionics", "BUBBLE_trail"), MathUtils.getMidpoint(this.ship.getLocation(), this.from), new Vector2f(), new Vector2f(512.0F, 256.0F), new Vector2f(-128.0F, 0.0F), VectorUtils.getAngle(this.from, this.ship.getLocation()), 0.0F, Color.white, true, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.05F, 0.1F, CombatEngineLayers.BELOW_SHIPS_LAYER);
            }
        }

    }

    public String getSection() {
        return "diableavionics";
    }

    public String getTrail() {
        return "BUBBLE_trail";
    }
}
