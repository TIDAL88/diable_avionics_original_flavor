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
    private static boolean combatOverride;
    private static boolean playRequested;

    private DASubject71DialogMusic() {
    }

    public static void start(InteractionDialogAPI dialog) {
        if (dialog == null) return;

        if (activeDialog != dialog) {
            activeDialog = dialog;
            Global.getSector().addScript(new DialogMusicWatcher(dialog));
        }

        combatOverride = false;
        playRequested = true;
    }

    public static void suspendForCombat() {
        combatOverride = true;
        playRequested = false;
    }

    private static void playOnce() {
        SoundPlayerAPI soundPlayer = Global.getSoundPlayer();
        if (soundPlayer == null) return;

        try {
            soundPlayer.setSuspendDefaultMusicPlayback(true);
            soundPlayer.playCustomMusic(1, 0, MUSIC_SET_ID, true);
        } catch (RuntimeException ex) {
            Global.getLogger(DASubject71DialogMusic.class).warn(
                    "Unable to play Last Line dialog music",
                    ex
            );
            soundPlayer.setSuspendDefaultMusicPlayback(false);
        }
    }

    private static void stop(InteractionDialogAPI dialog) {
        if (activeDialog != dialog) return;
        activeDialog = null;
        combatOverride = false;
        playRequested = false;

        SoundPlayerAPI soundPlayer = Global.getSoundPlayer();
        if (soundPlayer == null) return;

        soundPlayer.setSuspendDefaultMusicPlayback(false);
        soundPlayer.restartCurrentMusic();
    }

    private static final class DialogMusicWatcher implements EveryFrameScript {
        private final InteractionDialogAPI dialog;
        private boolean done;
        private boolean dialogSeen;

        private DialogMusicWatcher(InteractionDialogAPI dialog) {
            this.dialog = dialog;
        }

        @Override
        public void advance(float amount) {
            if (done || combatOverride
                    || Global.getCurrentState() == GameState.COMBAT) {
                return;
            }

            InteractionDialogAPI current = Global.getSector()
                    .getCampaignUI().getCurrentInteractionDialog();

            // init() may run just before the UI publishes the new dialog.
            // Do not mistake that brief gap for the dialog being dismissed.
            if (!dialogSeen) {
                if (current != dialog) return;
                dialogSeen = true;
            }

            if (current != dialog) {
                stop(dialog);
                done = true;
                return;
            }

            if (playRequested) {
                // Clear this before calling into the sound engine. Even if
                // OpenAL rejects the request, it must never retry every frame.
                playRequested = false;
                playOnce();
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
