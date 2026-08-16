package data.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Map;

/** Displays the contact used by the post-simulation transfer sequence. */
public class DASubject71_ShowTransferContact extends BaseCommandPlugin {
    @Override
    public boolean execute(
            String ruleId,
            InteractionDialogAPI dialog,
            List<Misc.Token> params,
            Map<String, MemoryAPI> memoryMap
    ) {
        if (dialog == null) return false;

        PersonAPI contact = Global.getFactory().createPerson();
        contact.setFaction("diableavionics");
        contact.setName(new FullName("Redacted", "", FullName.Gender.ANY));
        contact.setRankId("dsfTll");
        contact.setPostId(Ranks.POST_UNKNOWN);
        contact.setPortraitSprite(
                Global.getSettings().getSpriteName("characters", "da_lastline")
        );

        dialog.showVisualPanel();
        dialog.getVisualPanel().showPersonInfo(contact);
        return true;
    }
}
