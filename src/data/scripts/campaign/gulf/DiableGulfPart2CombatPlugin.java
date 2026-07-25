package data.scripts.campaign.gulf;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.input.InputEventAPI;
import data.scripts.util.Diableavionics_graphicLibEffects;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicLensFlare;
import org.magiclib.util.MagicRender;

import java.awt.Color;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_COLOR;

/**
 * Tahlan-style delayed station insertion for the Gulf Part II defender battle.
 */
public class DiableGulfPart2CombatPlugin extends BaseEveryFrameCombatPlugin {

    private static final Vector2f ARRIVAL_POINT = new Vector2f(0f, 0f);
    private static final float ARRIVAL_START = 18f;
    private static final float ARRIVAL_FINISH = 30f;
    private static final String REDACTED_COMBAT_MESSAGE = "CIC-EW : Phase-space interference. Source unknown.";
    private static final String REDACTED_COMBAT_MESSAGE2 = "CIC-EW : LARGE PHASE-SPACE SIGNATURE...";
    private static final String REDACTED_COMBAT_MESSAGE3 = "CIC-XO : All hands, prepare for turbulence";
    private static final String REDACTED_COMBAT_MESSAGE4 = "CIC-XO : Divide and conquer, that thing is not going anywhere.";



    private boolean checkedBattle;
    private boolean targetBattle;
    private boolean firstMessage;
    private boolean secondMessage;
    private boolean arrivalStarted;
    private boolean stationArrived;
    private float elapsed;

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (Global.getCurrentState() != GameState.COMBAT
                || Global.getSector() == null
                || Global.getSector().getPlayerFleet() == null) {
            return;
        }

        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return;

        if (!checkedBattle) {
            BattleAPI battle = Global.getSector().getPlayerFleet().getBattle();
            if (battle == null) return;
            checkedBattle = true;
            targetBattle = isGulfPartTwoBattle(battle);
            if (targetBattle) {
                // Guarantees the ambush can arrive even if all ten Vapors are destroyed quickly.
                engine.setCombatNotOverForAtLeast(32f);
                engine.getFleetManager(1).setCanForceShipsToEngageWhenBattleClearlyLost(true);
            }
        }

        if (!targetBattle || engine.isPaused()) return;

        elapsed += amount;

        if (!firstMessage && elapsed >= 6f) {
            firstMessage = true;
            engine.getCombatUI().addMessage(0, Color.ORANGE, REDACTED_COMBAT_MESSAGE);
        }

        if (!secondMessage && elapsed >= 12f) {
            secondMessage = true;
            engine.getCombatUI().addMessage(0, Color.RED, REDACTED_COMBAT_MESSAGE2);
        }

        if (!arrivalStarted && elapsed >= ARRIVAL_START) {
            beginArrival(engine);
        }

        if (arrivalStarted && !stationArrived) {
            advanceArrivalEffect(engine);
            if (elapsed >= ARRIVAL_FINISH) {
                finishArrival(engine);
            }
        }

    }

    private boolean isGulfPartTwoBattle(BattleAPI battle) {
        for (CampaignFleetAPI fleet : battle.getNonPlayerSide()) {
            if (fleet.getMemoryWithoutUpdate().getBoolean(DiableGulfPart2Intel.DEFENDER_MEMKEY)) {
                return true;
            }
        }
        return false;
    }

    private void beginArrival(CombatEngineAPI engine) {
        arrivalStarted = true;
        engine.setCombatNotOverForAtLeast(13f);
        engine.getCombatUI().addMessage(0, Color.BLUE, REDACTED_COMBAT_MESSAGE3);
    }

    private ShipAPI spawnStation(CombatEngineAPI engine) {
        FleetMemberAPI member = Global.getFactory().createFleetMember(
                FleetMemberType.SHIP,
                DiableGulfPart2Intel.STATION_VARIANT
        );
        member.setShipName("Diable Classic Station");
        member.setCaptain(DiableGulfPart2FleetFactory.createStationCommander());
        member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
        member.setOwner(1);

        CombatFleetManagerAPI enemy = engine.getFleetManager(1);
        ShipAPI spawned = enemy.spawnFleetMember(member, new Vector2f(ARRIVAL_POINT), 0f, 0f);
        enemy.setDeployedStation(true);

        return spawned;
    }

    private void advanceArrivalEffect(CombatEngineAPI engine) {
        // Keep the arrival zone clear while the station's deep-strike drive is charging.
        // This mirrors Tahlan Additions' Bushwacker arrival: mass-proportional force
        // gives ships roughly the same outward acceleration regardless of hull size.
        for (CombatEntityAPI entity : CombatUtils.getEntitiesWithinRange(ARRIVAL_POINT, 1500f)) {
            CombatUtils.applyForce(
                    entity,
                    VectorUtils.getDirectionalVector(ARRIVAL_POINT, entity.getLocation()),
                    entity.getMass() / 10f
            );
        }

        float intensity = Math.max(0f, Math.min(1f,
                (elapsed - ARRIVAL_START) / (ARRIVAL_FINISH - ARRIVAL_START)));

        // SEEKER's ominous boss-arrival bed. Calling playLoop every frame keeps it
        // alive only for the telegraph and lets it stop naturally when the station arrives.
        Global.getSoundPlayer().playLoop(
                "diableavionics_gulf_arrival_shadow",
                this,
                1f,
                1f,
                ARRIVAL_POINT,
                new Vector2f()
        );

        renderArrivalTelegraph(engine, intensity);
    }

    /** boss warp-zone presentation */
    private void renderArrivalTelegraph(CombatEngineAPI engine, float intensity) {
        if (Math.random() < 0.1f + intensity * 0.25f) {
            engine.addHitParticle(
                    ARRIVAL_POINT,
                    new Vector2f(),
                    MathUtils.getRandomNumberInRange(200f, 300f + 500f * intensity),
                    0.25f + 0.25f * intensity,
                    MathUtils.getRandomNumberInRange(0.05f, 0.1f + 0.1f * intensity),
                    new Color(
                            0.02f + 0.05f * MathUtils.getRandomNumberInRange(0f, 0.5f + 0.5f * intensity),
                            0.55f + 0.35f * MathUtils.getRandomNumberInRange(0f, 0.5f + 0.5f * intensity),
                            0.75f + 0.25f * MathUtils.getRandomNumberInRange(0f, 0.5f + 0.5f * intensity)
                    )
            );
        }

        if (Math.random() < 0.25f + intensity * 0.5f) {
            Vector2f offset = MathUtils.getRandomPointInCircle(
                    new Vector2f(),
                    50f + 150f * intensity
            );
            engine.addHitParticle(
                    Vector2f.sub(ARRIVAL_POINT, offset, new Vector2f()),
                    offset,
                    MathUtils.getRandomNumberInRange(3f, 6f + 6f * intensity),
                    0.5f + 0.5f * intensity,
                    MathUtils.getRandomNumberInRange(0.5f, 1f + intensity),
                    new Color(
                            0.02f + 0.04f * MathUtils.getRandomNumberInRange(0f, 0.5f + 0.5f * intensity),
                            0.65f + 0.3f * MathUtils.getRandomNumberInRange(0f, 0.5f + 0.5f * intensity),
                            0.8f + 0.2f * MathUtils.getRandomNumberInRange(0f, 0.5f + 0.5f * intensity)
                    )
            );
        }

        if (Math.random() < 0.1f + intensity * 0.15f) {
            Vector2f flarePoint = MathUtils.getRandomPointInCircle(
                    ARRIVAL_POINT,
                    200f - 175f * intensity
            );
            ShipAPI effectAnchor = getEffectAnchor(engine);
            if (effectAnchor != null) {
                MagicLensFlare.createSharpFlare(
                        engine,
                        effectAnchor,
                        flarePoint,
                        MathUtils.getRandomNumberInRange(2f, 3f + 3f * intensity),
                        MathUtils.getRandomNumberInRange(50f, 100f + 350f * intensity),
                        0f,
                        new Color(20, 105, 145),
                        Color.WHITE
                );
            }

            engine.addHitParticle(
                    flarePoint,
                    new Vector2f(),
                    MathUtils.getRandomNumberInRange(30f, 80f + 80f * intensity),
                    0.5f + 0.5f * intensity,
                    MathUtils.getRandomNumberInRange(0.25f, 0.5f + 0.5f * intensity),
                    new Color(75, 155, 170, 170)
            );

            if (Math.random() < 0.1f) {
                Global.getSoundPlayer().playSound(
                        "diableavionics_gulf_arrival_ripple",
                        0.8f + 0.4f * intensity,
                        0.5f + 0.5f * intensity,
                        ARRIVAL_POINT,
                        new Vector2f()
                );
            }
        }
    }

    private void finishArrival(CombatEngineAPI engine) {
        stationArrived = true;
        ShipAPI station = spawnStation(engine);
        if (station == null) return;

        station.getVelocity().set(0f, 0f);

        renderArrivalBurst(engine, station);

        engine.getCombatUI().addMessage(0, Color.BLUE, REDACTED_COMBAT_MESSAGE4);
    }

    private ShipAPI getEffectAnchor(CombatEngineAPI engine) {
        ShipAPI playerShip = engine.getPlayerShip();
        if (playerShip != null) return playerShip;

        for (ShipAPI ship : engine.getShips()) {
            if (ship != null && ship.isAlive()) return ship;
        }
        return null;
    }

    /** boss arrival burst */
    private void renderArrivalBurst(CombatEngineAPI engine, ShipAPI arrivingStation) {
        Global.getSoundPlayer().playSound(
                "diableavionics_gulf_station_arrival",
                1f,
                1f,
                ARRIVAL_POINT,
                new Vector2f()
        );
        Global.getSoundPlayer().playUISound("diableavionics_gulf_station_arrival", 1f, 0.25f);

        if (Global.getSettings().getModManager().isModEnabled("shaderLib")) {
            try {
                Diableavionics_graphicLibEffects.CustomRippleDistortion(
                        new Vector2f(ARRIVAL_POINT),
                        new Vector2f(),
                        720f,
                        30f,
                        false,
                        0f,
                        360f,
                        0f,
                        0.1f,
                        0.6f,
                        0.3f,
                        0.5f,
                        0f
                );
            } catch (RuntimeException ex) {
                // GraphicsLib is optional;
            } catch (LinkageError ex) {
                // Handles a missing or incompatible optional GraphicsLib installation.
            }
        }

        float facing = arrivingStation.getFacing();
        Vector2f location = new Vector2f(arrivingStation.getLocation());

        for (int i = 0; i < 24; i++) {
            MagicRender.battlespace(
                    Global.getSettings().getSprite("fx", "gulfArrivalRay"),
                    MathUtils.getRandomPointInCone(location, 720f, facing + 140f, facing + 220f),
                    MathUtils.getPoint(
                            new Vector2f(),
                            MathUtils.getRandomNumberInRange(256f, 360f),
                            facing
                    ),
                    new Vector2f(
                            MathUtils.getRandomNumberInRange(16f, 32f),
                            MathUtils.getRandomNumberInRange(512f, 1024f)
                    ),
                    new Vector2f(
                            MathUtils.getRandomNumberInRange(-2.5f, -0.5f),
                            MathUtils.getRandomNumberInRange(-500f, -400f)
                    ),
                    facing - 90f,
                    0f,
                    Color.WHITE,
                    true,
                    0f,
                    0.1f,
                    MathUtils.getRandomNumberInRange(0.5f, 5f)
            );
        }

        for (int i = 0; i < 12; i++) {
            float size = MathUtils.getRandomNumberInRange(512f, 1024f);
            float growth = MathUtils.getRandomNumberInRange(256f, 512f);
            MagicRender.battlespace(
                    Global.getSettings().getSprite("fx", "gulfArrivalSwoosh"),
                    MathUtils.getRandomPointInCircle(location, 512f - size / 2f),
                    MathUtils.getRandomPointInCone(
                            new Vector2f(),
                            64f,
                            facing + 150f,
                            facing + 210f
                    ),
                    new Vector2f(size, size),
                    new Vector2f(growth, growth),
                    MathUtils.getRandomNumberInRange(facing - 120f, facing - 60f),
                    MathUtils.getRandomNumberInRange(-15f, 15f),
                    Color.WHITE,
                    true,
                    0f,
                    0.2f,
                    MathUtils.getRandomNumberInRange(0.5f, 1.5f)
            );
        }

        MagicRender.battlespace(
                Global.getSettings().getSprite("fx", "gulfArrivalCloud"),
                location,
                new Vector2f(),
                new Vector2f(1024f, 1027f),
                new Vector2f(512f, 512f),
                (float) Math.random() * 360f,
                MathUtils.getRandomNumberInRange(-5f, 5f),
                Color.WHITE,
                true,
                0f, 0f, 0f, 0f, 0f,
                0f,
                0.1f,
                0.2f,
                CombatEngineLayers.ABOVE_SHIPS_LAYER
        );
        MagicRender.battlespace(
                Global.getSettings().getSprite("fx", "gulfArrivalCloud"),
                location,
                new Vector2f(),
                new Vector2f(720f, 720f),
                new Vector2f(512f, 512f),
                (float) Math.random() * 360f,
                MathUtils.getRandomNumberInRange(-5f, 5f),
                Color.WHITE,
                0f, 0f, 0f, 0f, 0f,
                0f,
                0.1f,
                0.1f,
                CombatEngineLayers.ABOVE_SHIPS_LAYER,
                GL_ONE_MINUS_SRC_COLOR,
                GL_ONE_MINUS_SRC_ALPHA
        );

        for (int i = 1; i < 50; i++) {
            Vector2f point = MathUtils.getPoint(new Vector2f(), i * (i + 5f), facing + 180f);
            Vector2f velocity = MathUtils.getPoint(new Vector2f(), 5f * i - 4f, facing);
            float duration = 5.1f - 0.1f * i;

            for (ShipAPI module : arrivingStation.getChildModulesCopy()) {
                if (module.isAlive()) {
                    module.addAfterimage(
                            new Color(0, 200, 255, 38),
                            point.x, point.y,
                            velocity.x, velocity.y,
                            0.1f,
                            0f, 0.1f, duration,
                            false, true, false
                    );
                }
            }
            arrivingStation.addAfterimage(
                    new Color(0, 200, 255, 38),
                    point.x, point.y,
                    velocity.x, velocity.y,
                    0.1f,
                    0f, 0.1f, duration,
                    false, true, false
            );
        }
    }
}
