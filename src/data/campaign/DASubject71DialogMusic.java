package data.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundPlayerAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.BaseFIDDelegate;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.FIDConfig;

/**
 * Plays The Last Line's interaction music through Starsector's native music
 * engine and restores normal music when the fleet dialog is dismissed.
 */
public final class DASubject71DialogMusic implements EveryFrameScript {

    private static final String MUSIC_SET_ID =
            "diableavionics_lastline_dialog";
    private static boolean playRequested;

    public DASubject71DialogMusic() {
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
        playRequested = true;
    }

    @Override
    public void advance(float amount) {
        if (!playRequested) return;

        InteractionDialogAPI dialog = Global.getSector()
                .getCampaignUI().getCurrentInteractionDialog();
        if (!isLastLineDialog(dialog)) return;

        // Clear before calling the sound engine so an audio error cannot turn
        // this transient script into an every-frame retry loop.
        playRequested = false;
        SoundPlayerAPI soundPlayer = Global.getSoundPlayer();
        if (soundPlayer == null) return;

        try {
            soundPlayer.pauseMusic();
            soundPlayer.playCustomMusic(1, 1, MUSIC_SET_ID, true);
            Global.getLogger(DASubject71DialogMusic.class).info(
                    "Requested Last Line dialog music"
            );
        } catch (RuntimeException ex) {
            Global.getLogger(DASubject71DialogMusic.class).warn(
                    "Unable to play Last Line dialog music",
                    ex
            );
        }
    }

    private static boolean isLastLineDialog(InteractionDialogAPI dialog) {
        if (dialog == null
                || !(dialog.getInteractionTarget() instanceof CampaignFleetAPI)) {
            return false;
        }
        return DACampaignPlugin.hasMemoryInFleet(
                (CampaignFleetAPI) dialog.getInteractionTarget(),
                "$virtuous"
        );
    }

    @Override
    public boolean isDone() {
        // Registered once per game load as a transient (non-save-persistent)
        // campaign service; individual playback requests are one-shot.
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        // Interaction dialogs pause campaign time.
        return true;
    }

    public static void stopAndRestoreCampaignMusic() {
        playRequested = false;
        SoundPlayerAPI soundPlayer = Global.getSoundPlayer();
        if (soundPlayer == null) return;
        soundPlayer.setSuspendDefaultMusicPlayback(false);
        soundPlayer.restartCurrentMusic();
    }
}
