package data.campaign.rulecmd;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageDefenderInteraction;
import com.fs.starfarer.api.util.Misc;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/** Uses the vanilla salvage-defender battle flow with encounter-specific human-fleet wording. */
public class DiableGulfPart2DefenderInteraction extends SalvageDefenderInteraction {

    private static final String ENGAGE_TEXT = "Engage the unidentified Diable fleet";

    @Override
    public boolean execute(
            String ruleId,
            InteractionDialogAPI dialog,
            List<Misc.Token> params,
            Map<String, MemoryAPI> memoryMap
    ) {
        boolean result = super.execute(ruleId, dialog, params, memoryMap);
        if (!result || dialog == null) return result;

        configureEncounter(dialog);

        if (dialog.getOptionPanel().hasOption(FleetInteractionDialogPluginImpl.OptionId.INITIATE_BATTLE)) {
            dialog.getOptionPanel().setOptionText(
                    ENGAGE_TEXT,
                    FleetInteractionDialogPluginImpl.OptionId.INITIATE_BATTLE
            );
        }
        if (dialog.getOptionPanel().hasOption(FleetInteractionDialogPluginImpl.OptionId.ENGAGE)) {
            dialog.getOptionPanel().setOptionText(
                    ENGAGE_TEXT,
                    FleetInteractionDialogPluginImpl.OptionId.ENGAGE
            );
        }
        return true;
    }

    /**
     * SalvageDefenderInteraction does not expose its FIDConfig and hardcodes automated-defense copy.
     * Adjusting the live config keeps both first engagement and retry text correct, and explicitly
     * disables the reputation hooks in addition to the fleet's NO_REP_IMPACT memory flag.
     */
    private static void configureEncounter(InteractionDialogAPI dialog) {
        if (!(dialog.getPlugin() instanceof FleetInteractionDialogPluginImpl)) return;

        try {
            Field field = FleetInteractionDialogPluginImpl.class.getDeclaredField("config");
            field.setAccessible(true);
            FleetInteractionDialogPluginImpl.FIDConfig config =
                    (FleetInteractionDialogPluginImpl.FIDConfig) field.get(dialog.getPlugin());
            if (config == null) return;

            config.firstTimeEngageOptionText = ENGAGE_TEXT;
            config.afterFirstTimeEngageOptionText = "Re-engage the unidentified Diable fleet";
            config.impactsAllyReputation = false;
            config.impactsEnemyReputation = false;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            System.err.println("Diable Avionics: unable to customize the Gulf Part II defender "
                    + "encounter config.");
            ex.printStackTrace(System.err);
        }
    }
}
