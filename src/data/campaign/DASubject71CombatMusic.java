package data.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundPlayerAPI;

/**
 * Owns Last Line's battle-only soundtrack and returns control to campaign
 * music once the encounter is resolved.
 */
public final class DASubject71CombatMusic {

    private static final String MUSIC_SET_ID =
            "diableavionics_lastline_combat";

    private DASubject71CombatMusic() {
    }

    public static void start() {
        SoundPlayerAPI soundPlayer = Global.getSoundPlayer();
        if (soundPlayer == null) return;

        try {
            DASubject71DialogMusic.suspendForCombat();
            soundPlayer.setSuspendDefaultMusicPlayback(true);
            soundPlayer.playCustomMusic(1, 1, MUSIC_SET_ID, true);
        } catch (RuntimeException ex) {
            Global.getLogger(DASubject71CombatMusic.class).warn(
                    "Unable to play Last Line combat music",
                    ex
            );
            soundPlayer.setSuspendDefaultMusicPlayback(false);
        }
    }

    public static void stopAndRestoreCampaignMusic() {
        SoundPlayerAPI soundPlayer = Global.getSoundPlayer();
        if (soundPlayer == null) return;

        soundPlayer.setSuspendDefaultMusicPlayback(false);
        soundPlayer.restartCurrentMusic();
    }
}
