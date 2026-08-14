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

    private static boolean active;

    private DASubject71CombatMusic() {
    }

    public static void start() {
        if (active) return;

        SoundPlayerAPI soundPlayer = Global.getSoundPlayer();
        if (soundPlayer == null) return;

        try {
            soundPlayer.setSuspendDefaultMusicPlayback(true);
            soundPlayer.playCustomMusic(1, 1, MUSIC_SET_ID, true);
            active = true;
        } catch (RuntimeException ex) {
            Global.getLogger(DASubject71CombatMusic.class).warn(
                    "Unable to play Last Line combat music",
                    ex
            );
            soundPlayer.setSuspendDefaultMusicPlayback(false);
        }
    }

    public static void stopAndRestoreCampaignMusic() {
        if (!active) return;
        active = false;

        SoundPlayerAPI soundPlayer = Global.getSoundPlayer();
        if (soundPlayer == null) return;

        soundPlayer.setSuspendDefaultMusicPlayback(false);
        soundPlayer.playCustomMusic(1, 0, null, false);
        soundPlayer.restartCurrentMusic();
    }
}
