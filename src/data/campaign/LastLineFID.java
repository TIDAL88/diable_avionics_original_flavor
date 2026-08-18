package data.campaign;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;

public class LastLineFID implements FleetInteractionDialogPluginImpl.FIDConfigGen {
    @Override
    public FleetInteractionDialogPluginImpl.FIDConfig createConfig() {
        var lastLineConfig = new FleetInteractionDialogPluginImpl.FIDConfig();
        lastLineConfig.delegate = new FleetInteractionDialogPluginImpl.BaseFIDDelegate() {
            @Override
            public void battleContextCreated(InteractionDialogAPI dialog, BattleCreationContext bcc) {
                    bcc.aiRetreatAllowed=false;
                    bcc.fightToTheLast=true;
                    bcc.objectivesAllowed=true;
            }
        };
        return lastLineConfig;
    }
}

