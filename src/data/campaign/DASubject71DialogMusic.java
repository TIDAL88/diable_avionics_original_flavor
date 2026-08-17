package data.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundPlayerAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.BaseFIDDelegate;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.FIDConfig;

/**
 * Plays The Last Line's interaction music through Starsector's native music
 * engine and restores normal music when the fleet dialog is dismissed.
 */
public final class DASubject71DialogMusic {

    private static final String MUSIC_SET_ID =
            "diableavionics_lastline_dialog";

    private DASubject71DialogMusic() {
    }

    public static FIDConfig createFleetInteractionConfig() {
        FIDConfig config = new FIDConfig();
        config.delegate = new BaseFIDDelegate() {
            @Override
            public void notifyLeave(InteractionDialogAPI dialog) {
                stopAndRestoreCampaignMusic();
            }
        };
        return config;
    }

    public static void start() {
        SoundPlayerAPI soundPlayer = Global.getSoundPlayer();
        if (soundPlayer == null) return;

        try {
            soundPlayer.playCustomMusic(1, 1, MUSIC_SET_ID, true);
        } catch (RuntimeException ex) {
            Global.getLogger(DASubject71DialogMusic.class).warn(
                    "Unable to play Last Line dialog music",
                    ex
            );
        }
    }

    public static void stopAndRestoreCampaignMusic() {
        SoundPlayerAPI soundPlayer = Global.getSoundPlayer();
        if (soundPlayer == null) return;

        soundPlayer.setSuspendDefaultMusicPlayback(false);
        soundPlayer.restartCurrentMusic();
    }
}
