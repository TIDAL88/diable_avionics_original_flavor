package data.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.impl.combat.BattleCreationPluginImpl;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;

/**
 * Combat setup shared by the normal-mode Last Line encounter and simulator.
 */
public class DASubject71BattleCreationPlugin extends BattleCreationPluginImpl {
    public static final String SIMULATION_RUNNING_KEY = "$simulationRunning";
    private static final String SIMULACRUM_BACKGROUND =
            "graphics/da/backgrounds/diableavionics_simulacrum.png";
    private static final String COMBAT_SETUP_KEY =
            "diableavionics_lastline_combat_setup";

    private static LocationAPI swappedLocation;
    private static String originalBackground;

    private boolean simulation;

    /**
     * Must be called before InteractionDialogAPI.startBattle(). Vanilla's
     * battle builder copies the containing location's background during
     * combat definition creation.
     */
    public static void prepareSimulationBattle(
            BattleCreationContext context,
            LocationAPI location
    ) {
        restoreSimulationBackground();
        context.getOtherFleet().getMemoryWithoutUpdate().set(SIMULATION_RUNNING_KEY, true);
        if (location == null) return;

        swappedLocation = location;
        originalBackground = location.getBackgroundTextureFilename();
        location.setBackgroundTextureFilename(SIMULACRUM_BACKGROUND);
    }

    public static void restoreSimulationBackground() {
        if (swappedLocation != null) {
            swappedLocation.setBackgroundTextureFilename(originalBackground);
        }
        swappedLocation = null;
        originalBackground = null;
    }

    public static boolean isSimulationBattleActive(CampaignFleetAPI fleet) {
        // The fleet flag survives combat setup and is also consumed by the
        // post-simulation dialog rules. Background cleanup must not disable
        // the ship/reputation reset in backFromEngagement().
        return fleet != null && fleet.getMemoryWithoutUpdate().getBoolean(SIMULATION_RUNNING_KEY);
    }

    @Override
    public void initBattle(
            BattleCreationContext context,
            MissionDefinitionAPI loader
    ) {
        context.aiRetreatAllowed = false;
        context.fightToTheLast = true;
        context.objectivesAllowed = true;
        simulation = isSimulationBattleActive(context.getOtherFleet());
        if (!simulation) {
            // Clean up any swap left behind by an interrupted simulation.
            restoreSimulationBackground();
        }
        super.initBattle(context, loader);
    }

    @Override
    public void afterDefinitionLoad(CombatEngineAPI engine) {
        super.afterDefinitionLoad(engine);
        if (engine.getCustomData().containsKey(COMBAT_SETUP_KEY)) return;
        engine.getCustomData().put(COMBAT_SETUP_KEY, Boolean.TRUE);
        engine.addPlugin(new DASubject71CombatMusic());
        engine.addPlugin(new DALastLineOpeningEscortPlugin());
        if (simulation) {
            engine.addPlugin(
                    new DASimulacrumScanlineOverlay()
            );
        }
    }

    @Override
    protected void addClosestPlanet() {
        if (!simulation) {
            super.addClosestPlanet();
        }
    }

}
