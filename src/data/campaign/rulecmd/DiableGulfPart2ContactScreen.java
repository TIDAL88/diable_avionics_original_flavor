package data.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.gulf.DiableGulfPart2FleetFactory;
import data.scripts.campaign.gulf.DiableGulfPart2Intel;

import java.util.List;
import java.util.Map;

/**
 * Locked contact screen shown after the player commits to investigating the Blacksite signal.
 */
public class DiableGulfPart2ContactScreen extends BaseCommandPlugin {

    public static final String ATTEMPT_CONTACT_OPTION_ID = "DAGulfPart2AttemptContact";
    private static final String COMBAT_MUSIC_SET = "diableavionics_blacksite_combat";
    @Override
    public boolean execute(
            String ruleId,
            InteractionDialogAPI dialog,
            List<Misc.Token> params,
            Map<String, MemoryAPI> memoryMap
    ) {
        if (dialog == null) return false;

        CampaignFleetAPI defenderFleet =
                DiableGulfPart2FleetFactory.getOrCreateFleet(dialog.getInteractionTarget());

        if (defenderFleet == null) {
            // Do not soft-lock a save if another mod prevents the campaign fleet from spawning.
            dialog.getTextPanel().addPara(
                    "The drive signatures disappear before your sensors can resolve the contact."
            );
            dialog.getOptionPanel().clearOptions();
            dialog.getOptionPanel().addOption("Leave", "defaultLeave");
            return false;
        }
        Global.getSoundPlayer().playCustomMusic(1, 1, COMBAT_MUSIC_SET, true);

        PersonAPI contact = defenderFleet.getCommander();
        if (contact == null) {
            contact = Global.getFactory().createPerson();
            defenderFleet.setCommander(contact);
        }
        // This is deliberately still the station's rules dialog: only the contact portrait and
        // scarydacrest faction crest are shown here, with no fleet-comparison panel.
        contact.setFaction(DiableGulfPart2Intel.ENEMY_FACTION_ID);
        contact.setPortraitSprite(DiableGulfPart2Intel.ENEMY_PORTRAIT);
        dialog.showVisualPanel();
        dialog.getVisualPanel().showPersonInfo(contact);

        // From this point onward there is deliberately no leave or back option.
        dialog.getOptionPanel().clearOptions();
        dialog.getOptionPanel().addOption(
                "\"Recall the survey team. Now.\"",
                ATTEMPT_CONTACT_OPTION_ID
        );
        dialog.setOptionOnEscape("", null);
        return true;
    }
}
