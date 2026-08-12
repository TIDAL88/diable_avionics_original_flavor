package data.campaign;

import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.impl.combat.BattleCreationPluginImpl;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;

/**
 * Combat setup used only by the normal-mode Last Line simulator battle.
 */
public class DASubject71BattleCreationPlugin extends BattleCreationPluginImpl {
    private static final String SIMULACRUM_BACKGROUND =
            "graphics/da/backgrounds/diableavionics_simulacrum.png";

    @Override
    public void initBattle(
            BattleCreationContext context,
            MissionDefinitionAPI loader
    ) {
        super.initBattle(context, loader);
        loader.setBackgroundSpriteName(SIMULACRUM_BACKGROUND);
    }

    @Override
    public void afterDefinitionLoad(CombatEngineAPI engine) {
        super.afterDefinitionLoad(engine);
        engine.setRenderStarfield(true);

        DASimulacrumBackgroundRenderer background =
                new DASimulacrumBackgroundRenderer(true);
        background.setMapActiveState(1f);
        engine.addLayeredRenderingPlugin(background);
    }
}
