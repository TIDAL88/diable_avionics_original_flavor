package data.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.impl.combat.BattleCreationPluginImpl;
import data.scripts.campaign.gulf.DiableGulfPart2CombatPlugin;

/** Installs the First Relay arrival sequence only for its authored battle. */
public final class DiableGulfPart2BattleCreationPlugin
        extends BattleCreationPluginImpl {

    private static final String INSTALL_KEY =
            "diableavionics_gulf_part2_combat_plugin_installed";

    @Override
    public void afterDefinitionLoad(CombatEngineAPI engine) {
        super.afterDefinitionLoad(engine);
        if (engine.getCustomData().containsKey(INSTALL_KEY)) return;

        engine.getCustomData().put(INSTALL_KEY, Boolean.TRUE);
        engine.addPlugin(new DiableGulfPart2CombatPlugin());
        Global.getLogger(DiableGulfPart2BattleCreationPlugin.class).info(
                "Installed First Relay combat sequence"
        );
    }
}
