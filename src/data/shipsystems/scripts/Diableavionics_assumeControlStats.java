package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.combat.CombatEngine;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.subsystems.MagicSubsystem;
import org.magiclib.subsystems.MagicSubsystemsManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.magiclib.plugins.MagicTrailPlugin;

import java.util.HashMap;
import java.util.Map;

import static data.scripts.util.Diableavionics_stringsManager.txt;
/*
xeds
 */
public class Diableavionics_assumeControlStats extends BaseShipSystemScript {
    private static final Color WEAPON_GLOW_COLOR = new Color(150, 100, 50, 50);
    private static final Color CYAN_JITTER_COLOR = new Color(220, 250, 255, 170);
    private static final float MAX_TIME_MULT = 1.25f;
    private static final float PLAYER_CONTROL_DAMAGE_TAKEN_MULT = 0.80f;
    private static final float PLAYER_CONTROL_HANDLING_MULT = 1.25f;
    private static final String PLAYER_CONTROL_DR_ID =
            "diableavionics_assumeControl_player_dr";
    private static final String PLAYER_CONTROL_HANDLING_ID =
            "diableavionics_assumeControl_player_handling";
    private static final String WANZER_TAG = "wanzer";
    public static final String SYSTEM_ID = "diableavionics_assumeControl";
    private static final String PLAYER_CONTROL_KEY = SYSTEM_ID + "_player_control";
    private static final String TIME_FLOW_KEY = SYSTEM_ID + "_time_flow";

    @Override
    public void apply(MutableShipStatsAPI stats, String id, ShipSystemStatsScript.State state, float effectLevel) {
        ShipAPI ship;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        for (ShipAPI fighter : getFighters(ship)) {
            if (fighter.isHulk()) continue;
            if (!fighter.hasListenerOfClass(AfterimageVisualTracker.class)) {
                fighter.addListener(new AfterimageVisualTracker(fighter, ship));
            }

            //accelerate fighters
            float mult = stats.getSystemRangeBonus().getBonusMult();
            float shipTimeMult = 1f + (MAX_TIME_MULT + mult - 2f) * effectLevel;
            fighter.getMutableStats().getTimeMult().modifyMult(SYSTEM_ID, shipTimeMult);
            updatePlayerControlDamageReduction(fighter);

            //visual effect
            fighter.setWeaponGlow(effectLevel, Misc.setAlpha(WEAPON_GLOW_COLOR, 50), EnumSet.allOf(WeaponType.class));
            fighter.setJitter( fighter,
                    CYAN_JITTER_COLOR,
                    0.25f*effectLevel,
                    3,
                    1+3f*effectLevel,
                    3+7f*effectLevel
            );
        }

        if (!hasControlSubsystem(ship)) {
            MagicSubsystemsManager.addSubsystemToShip(ship, new ControlFighterSwitch(ship, ship));
        }
    }

    private static boolean hasControlSubsystem(ShipAPI ship) {
        List<MagicSubsystem> subsystems = MagicSubsystemsManager.getSubsystemsForShipCopy(ship);
        if (subsystems != null) {
            for (MagicSubsystem subsystem : subsystems) {
                if (subsystem instanceof ControlCarrierSwitch || subsystem instanceof ControlFighterSwitch) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<ShipAPI> getFighters(ShipAPI carrier) {
        List<ShipAPI> result = new ArrayList<>();
        if (!carrier.getAllWings().isEmpty()) {
            for (FighterWingAPI w : carrier.getAllWings()) {
                result.addAll(w.getWingMembers());
            }
        }
        return result;
    }

    private static void updatePlayerControlDamageReduction(ShipAPI fighter) {
        CombatEngineAPI engine = Global.getCombatEngine();
        FighterWingAPI wing = fighter.getWing();
        boolean playerPilotedWanzer = engine != null
                && engine.getPlayerShip() == fighter
                && wing != null
                && wing.getSpec() != null
                && wing.getSpec().hasTag(WANZER_TAG);

        MutableShipStatsAPI fighterStats = fighter.getMutableStats();
        if (playerPilotedWanzer) {
            fighterStats.getHullDamageTakenMult().modifyMult(
                    PLAYER_CONTROL_DR_ID,
                    PLAYER_CONTROL_DAMAGE_TAKEN_MULT
            );
            fighterStats.getArmorDamageTakenMult().modifyMult(
                    PLAYER_CONTROL_DR_ID,
                    PLAYER_CONTROL_DAMAGE_TAKEN_MULT
            );
            fighterStats.getShieldDamageTakenMult().modifyMult(
                    PLAYER_CONTROL_DR_ID,
                    PLAYER_CONTROL_DAMAGE_TAKEN_MULT
            );
            fighterStats.getAcceleration().modifyMult(
                    PLAYER_CONTROL_HANDLING_ID,
                    PLAYER_CONTROL_HANDLING_MULT
            );
            fighterStats.getDeceleration().modifyMult(
                    PLAYER_CONTROL_HANDLING_ID,
                    PLAYER_CONTROL_HANDLING_MULT
            );
            fighterStats.getTurnAcceleration().modifyMult(
                    PLAYER_CONTROL_HANDLING_ID,
                    PLAYER_CONTROL_HANDLING_MULT
            );
        } else {
            removePlayerControlDamageReduction(fighter);
        }
    }

    private static void removePlayerControlDamageReduction(ShipAPI fighter) {
        MutableShipStatsAPI fighterStats = fighter.getMutableStats();
        fighterStats.getHullDamageTakenMult().unmodify(PLAYER_CONTROL_DR_ID);
        fighterStats.getArmorDamageTakenMult().unmodify(PLAYER_CONTROL_DR_ID);
        fighterStats.getShieldDamageTakenMult().unmodify(PLAYER_CONTROL_DR_ID);
        fighterStats.getAcceleration().unmodify(PLAYER_CONTROL_HANDLING_ID);
        fighterStats.getDeceleration().unmodify(PLAYER_CONTROL_HANDLING_ID);
        fighterStats.getTurnAcceleration().unmodify(PLAYER_CONTROL_HANDLING_ID);
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship;

        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        for (ShipAPI fighter : getFighters(ship)) {
            removePlayerControlDamageReduction(fighter);

            // Return control before the hulk check.
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine != null
                    && engine.getPlayerShip() == fighter
                    && ship.isAlive()) {
                switchToCarrier(ship, fighter);
            }

            if (fighter.isHulk()) continue;
            fighter.setWeaponGlow(0, Color.BLACK, EnumSet.allOf(WeaponType.class));
            fighter.getMutableStats().getTimeMult().unmodify(id);

            MagicSubsystemsManager.removeSubsystemFromShip(fighter, ControlCarrierSwitch.class);
            MagicSubsystemsManager.removeSubsystemFromShip(fighter, ControlFighterSwitch.class);

        }

        if (hasControlSubsystem(ship)) {
            MagicSubsystemsManager.removeSubsystemFromShip(ship, ControlFighterSwitch.class);
        }

        List<MagicSubsystem> subsystems = MagicSubsystemsManager.getSubsystemsForShipCopy(ship);
        if (subsystems != null && subsystems.isEmpty()) {
            ship.getCustomData().remove(MagicSubsystemsManager.CUSTOM_DATA_KEY);
        }
    }

    private final String TXT = txt("assumeControl");

    @Override
    public ShipSystemStatsScript.StatusData getStatusData(int index, ShipSystemStatsScript.State state, float effectLevel) {
        if (index == 0) {
            return new ShipSystemStatsScript.StatusData(TXT, false);
        }
        return null;
    }

    private final String READY = txt("ready");
    private final String TARGET = txt("target");

    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        if (system.getState() != ShipSystemAPI.SystemState.IDLE) return null;

        ShipAPI target = findTarget(ship, ship);
        if (target != null) {
            return READY;
        } else {
            return TARGET;
        }
    }

    private static boolean isFighterReturning(FighterWingAPI wing, ShipAPI fighter) {
        for (FighterWingAPI.ReturningFighter returningFighter : wing.getReturning()) {
            if (returningFighter.fighter == fighter) {
                return true;
            }
        }
        return false;
    }

    protected static ShipAPI findTarget(ShipAPI carrier, ShipAPI currentFighter) {
        Vector2f worldCursorPosition = CombatUtils.toWorldCoordinates(new Vector2f(Mouse.getX(), Mouse.getY()));
        ShipAPI closestValidFighter = null;
        float closestDist = Float.MAX_VALUE;

        for (FighterWingAPI w : carrier.getAllWings()) {
            for (ShipAPI f : w.getWingMembers()) {
                if (f == currentFighter) continue;
                if (f.isAlive() && !f.isLanding() && !isFighterReturning(w, f)) {
                    float dist = MathUtils.getDistanceSquared(f, worldCursorPosition);
                    if (dist < closestDist) {
                        closestValidFighter = f;
                        closestDist = dist;
                    }
                }
            }
        }

        return closestValidFighter;
    }

    private void switchToCarrier(ShipAPI carrier, ShipAPI fighter) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine.getPlayerShip() != fighter) return;

        //return to the command shuttle if the carrier is lost
        //use the engine's shuttle, setPlayerShipExternal doesn't accept null
        ShipAPI destination = isCarrierAvailable(engine, carrier, fighter.getOriginalOwner())
                ? carrier : ((CombatEngine) engine).getFakePlayerShip();
        stopPlayerControl(engine);
        //restore the neural link after leaving the fighter
        //otherwise vanilla forgets the pair and asks for a physical transfer
        if (destination == carrier && carrier.getMutableStats().getDynamic()
                .getMod(Stats.HAS_NEURAL_LINK).computeEffective(0f) > 0f) {
            engine.setShipPlayerLastTransferredCommandTo(carrier);
        }
        engine.setPlayerShipExternal(destination);
        startTimeFlow(engine, destination, 1f);

        if (destination == carrier) {
            engine.spawnEmpArcVisual(fighter.getLocation(), fighter, carrier.getLocation(), carrier, 3f, Color.RED, Color.WHITE);
        }
    }

    private void switchToFighter(ShipAPI carrier, ShipAPI oldFighter, ShipAPI fighter) {
        CombatEngineAPI engine = Global.getCombatEngine();
        ShipAPI source = oldFighter == null ? carrier : oldFighter;
        if (engine.getPlayerShip() != source
                || !isCarrierAvailable(engine, carrier, source.getOriginalOwner())
                || !fighter.isAlive() || !engine.isEntityInPlay(fighter)
                || fighter.getOwner() != source.getOriginalOwner()) return;

        stopPlayerControl(engine);
        engine.setPlayerShipExternal(fighter);
        PlayerWingControlRecovery recovery = new PlayerWingControlRecovery(engine, carrier, fighter);
        engine.getCustomData().put(PLAYER_CONTROL_KEY, recovery);
        engine.addPlugin(recovery);

        ControlFighterSwitch subsystem1 = new ControlFighterSwitch(fighter, carrier);
        ControlCarrierSwitch subsystem2 = new ControlCarrierSwitch(fighter, carrier);
        MagicSubsystemsManager.addSubsystemToShip(fighter, subsystem1);
        MagicSubsystemsManager.addSubsystemToShip(fighter, subsystem2);
        subsystem1.setState(MagicSubsystem.State.COOLDOWN);
        subsystem2.setState(MagicSubsystem.State.COOLDOWN);

        if (oldFighter != null) {
            Global.getCombatEngine().spawnEmpArcVisual(oldFighter.getLocation(), oldFighter, fighter.getLocation(), fighter, 3f, Color.RED, Color.WHITE);
            MagicSubsystemsManager.removeSubsystemFromShip(oldFighter, ControlFighterSwitch.class);
            MagicSubsystemsManager.removeSubsystemFromShip(oldFighter, ControlCarrierSwitch.class);
        } else {
            Global.getCombatEngine().spawnEmpArcVisual(carrier.getLocation(), carrier, fighter.getLocation(), fighter, 3f, Color.RED, Color.WHITE);
        }
        startTimeFlow(engine, fighter, 0.75f);

        ShipSystemAPI system = fighter.getSystem();
        if (system != null) {
            if (system.getCooldownRemaining() > 0f && system.isCoolingDown()) {
                system.setCooldownRemaining(system.getCooldownRemaining() / 2f);
            }
            if (system.getAmmo() < system.getMaxAmmo() && system.getAmmoPerSecond() > 0) {
                system.setAmmoReloadProgress(system.getAmmoReloadProgress() * 2f);
            }
        }

    }

    private static boolean isCarrierAvailable(CombatEngineAPI engine, ShipAPI carrier, int owner) {
        return carrier.isAlive() && engine.isEntityInPlay(carrier) && carrier.getOwner() == owner;
    }

    private void stopPlayerControl(CombatEngineAPI engine) {
        Object recovery = engine.getCustomData().get(PLAYER_CONTROL_KEY);
        if (recovery instanceof PlayerWingControlRecovery) {
            ((PlayerWingControlRecovery) recovery).stop();
        }
    }

    //keep checking after the fighter is destroyed
    //removed fighters no longer get their subsystem updates
    private class PlayerWingControlRecovery extends BaseEveryFrameCombatPlugin {
        private final CombatEngineAPI engine;
        private final ShipAPI carrier;
        private final ShipAPI fighter;
        private boolean stopped;

        private PlayerWingControlRecovery(CombatEngineAPI engine, ShipAPI carrier, ShipAPI fighter) {
            this.engine = engine;
            this.carrier = carrier;
            this.fighter = fighter;
        }

        @Override
        public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {
            if (stopped || engine.getPlayerShip() != fighter || engine.isCombatOver()) return;
            CombatUIAPI ui = engine.getCombatUI();
            if (ui == null || ui.isAutopilotOn() || ui.isShowingCommandUI()
                    || ui.isShowingDeploymentDialog() || engine.isUIShowingDialog()) return;

            //reserve T before the target-view control sees it
            //MagicLib still reads the physical key for wanzer switching
            for (InputEventAPI event : events) {
                if (!event.isConsumed() && event.isKeyboardEvent()
                        && event.getEventValue() == Keyboard.KEY_T) {
                    event.consume();
                }
            }
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (stopped) return;
            if (engine.getPlayerShip() != fighter || engine.isCombatOver()) {
                stop();
                return;
            }
            if (engine.isPaused()) return;

            ShipSystemAPI system = carrier.getSystem();
            if (!fighter.isAlive() || !engine.isEntityInPlay(fighter)
                    || fighter.getOwner() != fighter.getOriginalOwner()
                    || !isCarrierAvailable(engine, carrier, fighter.getOriginalOwner())
                    || system == null
                    || system.getState() == ShipSystemAPI.SystemState.IDLE
                    || system.getState() == ShipSystemAPI.SystemState.COOLDOWN) {
                switchToCarrier(carrier, fighter);
            }
        }

        private void stop() {
            if (stopped) return;
            stopped = true;
            if (engine.getCustomData().get(PLAYER_CONTROL_KEY) == this) {
                engine.getCustomData().remove(PLAYER_CONTROL_KEY);
                cancelTimeFlow(engine);
                engine.getTimeMult().unmodify(SYSTEM_ID);
            }
            removePlayerControlDamageReduction(fighter);
            MagicSubsystemsManager.removeSubsystemFromShip(fighter, ControlCarrierSwitch.class);
            MagicSubsystemsManager.removeSubsystemFromShip(fighter, ControlFighterSwitch.class);
            engine.removePlugin(this);
        }
    }

    private void cancelTimeFlow(CombatEngineAPI engine) {
        Object transition = engine.getCustomData().remove(TIME_FLOW_KEY);
        if (transition instanceof GradualTimeFlowScript) {
            engine.removePlugin((GradualTimeFlowScript) transition);
        }
    }

    private void startTimeFlow(CombatEngineAPI engine, ShipAPI ship, float endMult) {
        cancelTimeFlow(engine);
        GradualTimeFlowScript transition = new GradualTimeFlowScript(ship, endMult);
        engine.getCustomData().put(TIME_FLOW_KEY, transition);
        engine.addPlugin(transition);
    }

    private class GradualTimeFlowScript extends BaseEveryFrameCombatPlugin {
        private final float startMult = 0.25f;
        private final float endMult;
        private final float startTime = 0.5f;
        private float time = startTime;
        private boolean inited = false;
        private final ShipAPI ship;

        private GradualTimeFlowScript(ShipAPI ship, float endMult) {
            this.ship = ship;
            this.endMult = endMult;
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            CombatEngineAPI engine = Global.getCombatEngine();
            //ignore an old transition still waiting to run this frame
            if (engine.getCustomData().get(TIME_FLOW_KEY) != this) return;
            if (engine.getPlayerShip() != ship || engine.isCombatOver()) {
                cancelTimeFlow(engine);
                engine.getTimeMult().unmodify(SYSTEM_ID);
                return;
            }
            if (engine.isPaused()) return;
            if (!inited) {
                inited = true;
                ship.giveCommand(ShipCommand.SELECT_GROUP, null, 0);
            }

            time -= amount * Global.getCombatEngine().getTimeMult().getModifiedValue();
            if (time < 0) {
                if (endMult == 1) {
                    engine.getTimeMult().unmodify(SYSTEM_ID);
                } else {
                    engine.getTimeMult().modifyMult(SYSTEM_ID, endMult);
                }
                engine.getCustomData().remove(TIME_FLOW_KEY);
                engine.removePlugin(this);
            } else {
                float newMult = endMult - (endMult - startMult) * (time / startTime);
                engine.getTimeMult().modifyMult(SYSTEM_ID, newMult);
            }
        }
    }

    private abstract class PlayerControlSwitch extends MagicSubsystem {
        private boolean keyHeld = true;

        private PlayerControlSwitch(ShipAPI ship) {
            super(ship);
        }

        @Override
        public boolean isKeyDown() {
            //wait for a fresh press after changing ships
            boolean pressed = super.isKeyDown();
            boolean activate = pressed && !keyHeld;
            keyHeld = pressed;
            return activate;
        }
    }

    private class ControlFighterSwitch extends PlayerControlSwitch {
        private ShipAPI carrier;
        public ControlFighterSwitch(ShipAPI ship, ShipAPI carrier) {
            super(ship);
            this.carrier = carrier;
            if (ship != carrier) setKey("T");
        }

        @Override
        public int getOrder() {
            return ship == carrier ? ORDER_SHIP_UNIQUE : ORDER_SHIP_MODULAR;
        }

        @Override
        public String getDisplayText() {
            return txt(ship == carrier ? "wanzerLinkSubsystemName" : "wanzerSwitchSubsystemName");
        }

        @Override
        public float getBaseActiveDuration() {
            return 1f;
        }

        @Override
        public float getBaseCooldownDuration() {
            return 4f;
        }

        @Override
        public boolean isToggle() {
            return false;
        }

        @Override
        public boolean shouldActivateAI(float amount) {
            return false;
        }

        @Override
        public boolean canActivate() {
            return carrier.getSystem().isActive();
        }

        @Override
        public void onActivate() {
            ShipAPI target = findTarget(carrier, ship);
            if (target != null) {
                if (carrier == ship) {
                    switchToFighter(carrier, null, target);
                } else {
                    switchToFighter(carrier, ship, target);
                }
            }
        }
    }

    private class ControlCarrierSwitch extends PlayerControlSwitch {
        private ShipAPI carrier;

        public ControlCarrierSwitch(ShipAPI ship, ShipAPI carrier) {
            super(ship);
            this.carrier = carrier;
        }

        //return to the carrier gets the first subsystem key
        @Override
        public int getOrder() {
            return ORDER_SHIP_UNIQUE;
        }

        @Override
        public String getDisplayText() {
            return txt("wanzerLinkedSubsystemName");
        }

        @Override
        public float getBaseActiveDuration() {
            return 1f;
        }

        @Override
        public float getBaseCooldownDuration() {
            return 1f;
        }

        @Override
        public boolean isToggle() {
            return false;
        }

        @Override
        public boolean shouldActivateAI(float amount) {
            return false;
        }

        @Override
        public boolean canActivate() {
            return Global.getCombatEngine().getPlayerShip() == ship;
        }

        @Override
        public void onActivate() {
            switchToCarrier(carrier, ship);
        }


        @Override
        public void advance(float amount, boolean isPaused) {
            ShipAPI closest = AIUtils.getNearestEnemy(ship);

            if (closest != null && MathUtils.getDistance(closest, ship) <= 0) {
                Vector2f force = MathUtils.getPointOnCircumference(new Vector2f(), closest.getCollisionRadius(), VectorUtils.getAngle(closest.getLocation(), ship.getLocation()));
                Vector2f dist = new Vector2f();
                Vector2f.sub(closest.getLocation(), ship.getLocation(), dist);
                Vector2f.add(force, dist, force);
                force.scale(amount * 2);

                Vector2f vel = ship.getVelocity();
                Vector2f.add(vel, force, vel);
            }
        }
    }
    private static class AfterimageVisualTracker implements AdvanceableListener {

        private final ShipAPI ship;
        private final ShipAPI carrier;

        private final IntervalUtil effectInterval = new IntervalUtil(0.05f, 0.05f);
        private final IntervalUtil initialEmpArcDelay = new IntervalUtil(0.5f, 2f);
        private final IntervalUtil empArcInterval = new IntervalUtil(8f, 11f);
        private final Map<ShipEngineControllerAPI.ShipEngineAPI, Float> trailIdByEngine = new HashMap<>();

        private final SpriteAPI trailSprite =
                Global.getSettings().getSprite("fx", "beamRough2Core");

        private static final String MODE_BLUE_ID = "abyss_diable_afterimage_mode_blue";

        private static final Color ENGINE_COLOR = new Color(190,220,255);
        private static final Color EMPTY_COLOR = new Color(0,0,0,0);

        private static final Color RED_TRAIL_COLOR = new Color(255,20,20,255);
        private static final Color BLUE_TRAIL_COLOR = new Color(94,255,255,255);
        private static final Color RED_EMP_FRINGE_COLOR = Color.RED;
        private static final Color EMP_CORE_COLOR = new Color(0,160,200,255);

        private float engineEffectLevel = 0f;
        private boolean initialEmpArcSpawned = false;

        public AfterimageVisualTracker(ShipAPI ship, ShipAPI carrier) {
            this.ship = ship;
            this.carrier = carrier;
        }

        private float clamp(float v,float min,float max){
            return Math.max(min, Math.min(max,v));
        }

        private Color getTrailColor(ShipAPI carrier) {
            if (carrier != null &&
                    carrier.getVariant() != null &&
                    carrier.getVariant().hasHullMod(MODE_BLUE_ID)) {
                return BLUE_TRAIL_COLOR;
            }

            return RED_TRAIL_COLOR;
        }

        private void spawnEmpArc() {
            float arcRadius = Math.max(ship.getCollisionRadius() * 0.75f, 10f);
            float arcAngle = MathUtils.getRandomNumberInRange(0f, 360f);
            Vector2f arcFrom = MathUtils.getPointOnCircumference(
                    ship.getLocation(),
                    arcRadius,
                    arcAngle
            );
            Vector2f arcTo = MathUtils.getPointOnCircumference(
                    ship.getLocation(),
                    arcRadius,
                    arcAngle + MathUtils.getRandomNumberInRange(120f, 240f)
            );

            Global.getCombatEngine().spawnEmpArcVisual(
                    arcFrom,
                    ship,
                    arcTo,
                    ship,
                    0.2f,
                    RED_EMP_FRINGE_COLOR,
                    EMP_CORE_COLOR
            );
            Global.getSoundPlayer().playSound(
                    "diableavionics_nocturne_hit",
                    1f,
                    0.15f,
                    ship.getLocation(),
                    ship.getVelocity()
            );
        }

        @Override
        public void advance(float amount) {

            if (ship == null || !ship.isAlive()) {
                return;
            }

            // Remove ourselves when the carrier stops using Assume Wing Control.
            ShipSystemAPI sys = carrier.getSystem();

            if (carrier == null || sys == null ||
                    sys.getState() == ShipSystemAPI.SystemState.IDLE ||
                    sys.getState() == ShipSystemAPI.SystemState.COOLDOWN) {
                ship.removeListener(this);
                return;
            }

            if (!initialEmpArcSpawned) {
                initialEmpArcDelay.advance(amount);
                if (initialEmpArcDelay.intervalElapsed()) {
                    initialEmpArcSpawned = true;
                    spawnEmpArc();
                }
            } else {
                empArcInterval.advance(amount);
                if (empArcInterval.intervalElapsed()) {
                    spawnEmpArc();
                }
            }

            engineEffectLevel = clamp(engineEffectLevel + amount,0f,1f);

            ship.getEngineController().fadeToOtherColor(
                    this,
                    ENGINE_COLOR,
                    EMPTY_COLOR,
                    engineEffectLevel,
                    0.33f);

            effectInterval.advance(amount);

            if (!effectInterval.intervalElapsed()) return;

            Vector2f velocity = ship.getVelocity();
            float angle = Misc.getAngleInDegrees(new Vector2f(velocity));

            float maxSpeed = Math.max(ship.getMaxSpeed(),1f);

            float opacity = clamp(
                    (velocity.length() / maxSpeed) * 1.65f,
                    0f,
                    1f);

            opacity *= 0.45f * carrier.getSystem().getEffectLevel();

            for (ShipEngineControllerAPI.ShipEngineAPI engine :
                    ship.getEngineController().getShipEngines()) {

                if (!trailIdByEngine.containsKey(engine)) {
                    trailIdByEngine.put(engine, MagicTrailPlugin.getUniqueID());
                }

                List<ShipEngineControllerAPI.ShipEngineAPI> engines =
                        ship.getEngineController().getShipEngines();

                if (engines == null || engines.isEmpty()) {
                    Vector2f loc = MathUtils.getPointOnCircumference(
                            ship.getLocation(),
                            ship.getCollisionRadius() * 0.7f,
                            ship.getFacing() + 180f
                    );

                    MagicTrailPlugin.addTrailMemberSimple(
                            ship,
                            MagicTrailPlugin.getUniqueID(),
                            trailSprite,
                            loc,
                            0f,
                            angle,
                            5f,
                            5f,
                            getTrailColor(carrier),
                            opacity,
                            0f,
                            0f,
                            2f,
                            true
                    );

                    return;
                }

                MagicTrailPlugin.addTrailMemberSimple(
                        ship,
                        trailIdByEngine.get(engine),
                        trailSprite,
                        engine.getLocation(),
                        0f,
                        angle,
                        5f,
                        5f,
                        getTrailColor(carrier),
                        opacity,
                        0f,
                        0f,
                        2f,
                        true
                );
            }
        }
    }
}
