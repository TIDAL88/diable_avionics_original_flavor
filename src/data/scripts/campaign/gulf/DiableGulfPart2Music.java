package data.scripts.campaign.gulf;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundPlayerAPI;

/**
 * Keeps the Gulf Part II battle track continuous from the commitment dialog into combat.
 */
public final class DiableGulfPart2Music {

    private static final String COMBAT_MUSIC_SET = "diableavionics_blacksite_combat";

    private DiableGulfPart2Music() {
    }

    public static void start() {
        SoundPlayerAPI soundPlayer = Global.getSoundPlayer();
        if (soundPlayer == null) return;

        soundPlayer.setSuspendDefaultMusicPlayback(true);
        soundPlayer.playCustomMusic(1, 1, COMBAT_MUSIC_SET, true);
    }

    public static void stopAndRestoreCampaignMusic() {
        SoundPlayerAPI soundPlayer = Global.getSoundPlayer();
        if (soundPlayer == null) return;

        soundPlayer.setSuspendDefaultMusicPlayback(false);
        soundPlayer.playCustomMusic(1, 0, null, false);
        soundPlayer.restartCurrentMusic();
    }
}
