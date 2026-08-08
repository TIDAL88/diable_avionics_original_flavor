package data.scripts.world.systems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.NascentGravityWellAPI;
import com.fs.starfarer.api.campaign.listeners.SlipstreamBlockerUpdater;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.impl.campaign.velfield.SlipstreamManager;

public class Diableavionics_blackSiteSlipstreamBlocker implements SlipstreamBlockerUpdater {

    @Override
    public void updateSlipstreamBlockers(CollisionGridAPI grid, SlipstreamManager manager) {
        Object storedWell = Global.getSector().getMemoryWithoutUpdate()
                .get(Diableavionics_blackSite.NASCENT_WELL_KEY);
        if (!(storedWell instanceof NascentGravityWellAPI)) return;

        NascentGravityWellAPI well = (NascentGravityWellAPI) storedWell;
        float radius = Diableavionics_blackSite.HYPERSPACE_PROTECTION_RADIUS + well.getRadius();
        SlipstreamManager.CustomStreamBlocker blocker =
                new SlipstreamManager.CustomStreamBlocker(well.getLocation(), radius);
        grid.addObject(blocker, well.getLocation(), radius * 2f, radius * 2f);
    }
}
