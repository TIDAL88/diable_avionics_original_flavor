package data.campaign;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.impl.combat.BattleCreationPluginImpl;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;

/**
 * Combat setup shared by the normal-mode Last Line encounter and simulator.
 */
public class DASubject71BattleCreationPlugin extends BattleCreationPluginImpl {
    @Override
    public void initBattle(
            BattleCreationContext context,
            MissionDefinitionAPI loader
    ) {
        context.enemyDeployAll = true;
        super.initBattle(context, loader);
    }

    @Override
    public void afterDefinitionLoad(CombatEngineAPI engine) {
        super.afterDefinitionLoad(engine);

        if (context == null
                || context.getOtherFleet() == null
                || !DACampaignPlugin.hasMemoryInFleet(
                        context.getOtherFleet(),
                        "$simulationRunning"
                )) {
            return;
        }

        DASimulacrumBackgroundRenderer background =
                new DASimulacrumBackgroundRenderer(true);
        background.setMapActiveState(1f);
        engine.addLayeredRenderingPlugin(background);
    }
}
