package data.campaign;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;

import java.util.Collections;
import java.util.Map;

/**
 * Reserved interaction hook for the ruptured gate.
 *
 * The entity is currently non-clickable. If interaction is enabled before this plugin receives
 * its future content, close the empty dialog instead of falling through to vanilla gate logic.
 */
public class DiableBlacksiteGateInteractionPlugin implements InteractionDialogPlugin {

    @Override
    public void init(InteractionDialogAPI dialog) {
        dialog.dismiss();
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
    }

    @Override
    public void optionMousedOver(String optionText, Object optionData) {
    }

    @Override
    public void advance(float amount) {
    }

    @Override
    public void backFromEngagement(EngagementResultAPI battleResult) {
    }

    @Override
    public Object getContext() {
        return null;
    }

    @Override
    public Map<String, MemoryAPI> getMemoryMap() {
        return Collections.emptyMap();
    }
}
