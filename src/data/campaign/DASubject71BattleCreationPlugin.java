package data.campaign;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.impl.combat.BattleCreationPluginImpl;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;

/**
 * Combat setup shared by the normal-mode Last Line encounter and simulator.
 */
public class DASubject71BattleCreationPlugin extends BattleCreationPluginImpl {
    private static final String SIMULACRUM_BACKGROUND =
            "graphics/da/backgrounds/diableavionics_simulacrum.png";

    private static LocationAPI swappedLocation;
    private static String originalBackground;

    private boolean simulation;

    /**
     * Must be called before InteractionDialogAPI.startBattle(). Vanilla's
     * battle builder copies the containing location's background during
     * combat definition creation.
     */
    public static void prepareSimulationBackground(LocationAPI location) {
        restoreSimulationBackground();
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

    @Override
    public void initBattle(
            BattleCreationContext context,
            MissionDefinitionAPI loader
    ) {
        context.enemyDeployAll = true;
        simulation = swappedLocation != null || isSimulation(context);
        super.initBattle(context, loader);
    }

    @Override
    public void afterDefinitionLoad(CombatEngineAPI engine) {
        super.afterDefinitionLoad(engine);
        DASubject71CombatMusic.start();
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

    private boolean isSimulation(BattleCreationContext context) {
        return context != null
                && context.getOtherFleet() != null
                && DACampaignPlugin.hasMemoryInFleet(
                        context.getOtherFleet(),
                        "$simulationRunning"
                );
    }
}
