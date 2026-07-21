package data.scripts.campaign.gulf;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.IntervalUtil;

/** Watches for the player acquiring a Gulf after a save has already been loaded. */
public class DiableGulfPart2TriggerScript implements EveryFrameScript {

    private final IntervalUtil checkInterval = new IntervalUtil(0.05f, 0.05f);

    @Override
    public void advance(float amount) {
        if (Global.getSector() == null) return;

        checkInterval.advance(Global.getSector().getClock().convertToDays(amount));
        if (checkInterval.intervalElapsed()) {
            DiableGulfPart2Intel.ensureStarted();
        }
    }

    @Override
    public boolean isDone() {
        if (Global.getSector() == null) return false;
        return Global.getSector().getMemoryWithoutUpdate().getBoolean(DiableGulfPart2Intel.STARTED_MEMKEY)
                || Global.getSector().getMemoryWithoutUpdate().getBoolean(DiableGulfPart2Intel.COMPLETE_MEMKEY);
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }
}
