package data.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import java.util.List;

/** Plays The Last Line's battle-only soundtrack. */
public final class DASubject71CombatMusic extends BaseEveryFrameCombatPlugin {
    private static final String MUSIC_SET_ID = "diableavionics_lastline_combat";
    private CombatEngineAPI engine;
    boolean music_started = false;

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine=engine;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine==null)  return;
        if (!music_started){
            Global.getSoundPlayer().pauseMusic();
        }
        if (!music_started) {
            Global.getSoundPlayer().playCustomMusic(1,1,MUSIC_SET_ID,true);
            music_started=true;
        }
    }
}