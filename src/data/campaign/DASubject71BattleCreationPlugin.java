package data.campaign;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.impl.combat.BattleCreationPluginImpl;

/**
 * Combat setup used only by the normal-mode Last Line simulator battle.
 */
public class DASubject71BattleCreationPlugin extends BattleCreationPluginImpl {
    @Override
    public void afterDefinitionLoad(CombatEngineAPI engine) {
        super.afterDefinitionLoad(engine);

        DASimulacrumBackgroundRenderer background =
                new DASimulacrumBackgroundRenderer(true);
        background.setMapActiveState(1f);
        engine.addLayeredRenderingPlugin(background);
    }
}
