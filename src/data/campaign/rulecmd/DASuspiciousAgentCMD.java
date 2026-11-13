package data.campaign.rulecmd;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.intel.bar.BarEventDialogPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.missions.BarCMD;
import com.fs.starfarer.api.util.Misc.Token;
import data.scripts.campaign.bar.DASuspiciousAgentBarEvent;

import java.util.List;
import java.util.Map;

public class DASuspiciousAgentCMD extends BaseCommandPlugin {

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params,
                           Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || dialog.getInteractionTarget() == null || params.isEmpty()) return false;
        MarketAPI market = dialog.getInteractionTarget().getMarket();
        DASuspiciousAgentBarEvent agent = DASuspiciousAgentBarEvent.getInstance();
        switch (params.get(0).getString(memoryMap)) {
            case "canDiscover":
                return !agent.hasBeenApproached() && agent.shouldShowAtMarket(market);
            case "isStationed":
                return agent.hasBeenApproached() && agent.shouldShowAtMarket(market);
            case "add":
                agent.addToBar(market);
                return true;
            case "open":
                if (!agent.shouldShowAtMarket(market)) return false;
                MemoryAPI memory = getEntityMemory(memoryMap);
                if (memory == null || !(memory.get("$BarCMD") instanceof BarCMD bar)) return false;
                memory.unset("$BarCMD");
                BarEventDialogPlugin plugin = new BarEventDialogPlugin(bar, bar, agent, memoryMap);
                dialog.setPlugin(plugin);
                plugin.init(dialog);
                return true;
            default:
                return false;
        }
    }
}
