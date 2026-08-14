package data.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundPlayerAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;

/**
 * Plays the Last Line conversation track only while its interaction dialog is
 * open. A small watcher removes itself when that specific dialog is dismissed.
 */
public final class DASubject71DialogMusic {

    private static final String MUSIC_SET_ID =
            "diableavionics_lastline_dialog";

    private static InteractionDialogAPI activeDialog;

    private DASubject71DialogMusic() {
    }

    public static void start(InteractionDialogAPI dialog) {
        if (dialog == null) return;

        if (activeDialog != dialog) {
            activeDialog = dialog;
            Global.getSector().addScript(new DialogMusicWatcher(dialog));
        }

        SoundPlayerAPI soundPlayer = Global.getSoundPlayer();
        if (soundPlayer == null) return;

        soundPlayer.setSuspendDefaultMusicPlayback(true);
        soundPlayer.playCustomMusic(1, 1, MUSIC_SET_ID, true);
    }

    private static void stop(InteractionDialogAPI dialog) {
        if (activeDialog != dialog) return;
        activeDialog = null;

        SoundPlayerAPI soundPlayer = Global.getSoundPlayer();
        if (soundPlayer == null) return;

        soundPlayer.setSuspendDefaultMusicPlayback(false);
        soundPlayer.playCustomMusic(1, 0, null, false);
        soundPlayer.restartCurrentMusic();
    }

    private static final class DialogMusicWatcher implements EveryFrameScript {
        private final InteractionDialogAPI dialog;
        private boolean done;

        private DialogMusicWatcher(InteractionDialogAPI dialog) {
            this.dialog = dialog;
        }

        @Override
        public void advance(float amount) {
            if (done || Global.getCurrentState() == GameState.COMBAT) return;

            if (Global.getSector().getCampaignUI().getCurrentInteractionDialog()
                    != dialog) {
                stop(dialog);
                done = true;
            }
        }

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public boolean runWhilePaused() {
            return true;
        }
    }
}
