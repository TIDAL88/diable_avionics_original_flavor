package data.scripts.world.systems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.StarTypes;
import com.fs.starfarer.api.impl.campaign.terrain.EventHorizonPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin;

import java.awt.*;
import java.util.Random;

public class Diableavionics_blackSite {
    public static final String NASCENT_WELL_KEY = "$diable_well";
    public void generate(SectorAPI sector) {
        StarSystemAPI system = sector.createStarSystem("diable_blacksite");
        SectorEntityToken star = system.initStar("diableavionics_blackhole", StarTypes.BLACK_HOLE, 500f, 450f);
        system.getLocation().set(35000f, -7000f);
        system.setBackgroundTextureFilename("graphics/da/backgrounds/diableavionics_86rn.png");
        PlanetAPI planet = system.addPlanet("diableavionics_blacksite", star, "FOB-01", "lava", 45f, 400f, 250f, 10f);
        MarketAPI market = planet.getMarket();
        market.addCondition("irradiated");
        planet.setOrbit(null);
        planet.setLocation(500.0F, 800.0F);
        MagneticFieldTerrainPlugin.MagneticFieldParams fieldParams =
                new MagneticFieldTerrainPlugin.MagneticFieldParams(
                        150.0F,
                        500.0F,
                        planet,
                        350.0F,
                        650.0F,
                        new Color(60, 60, 150, 90),
                        1.0F,
                        new Color(130, 60, 150, 130),
                        new Color(150, 30, 120, 150),
                        new Color(200, 50, 130, 190),
                        new Color(250, 70, 150, 240),
                        new Color(200, 80, 130, 255),
                        new Color(75, 0, 160, 255),
                        new Color(127, 0, 255, 255));
        StarCoronaTerrainPlugin.CoronaParams fieldParams2=new StarCoronaTerrainPlugin.CoronaParams(
                150f,200f,star,7,20f,0.20f
        );
        SectorEntityToken magneticField = system.addTerrain("magnetic_field", fieldParams);
        SectorEntityToken eventHorizon= system.addTerrain("event_horizon",fieldParams2);
        eventHorizon.setCircularOrbit(star,45f,400f,70F);
        magneticField.setCircularOrbit(planet, 0.0F, 0.0F, 75.0F);
        system.generateAnchorIfNeeded();
        NascentGravityWellAPI well = Global.getSector().createNascentGravityWell(planet, 50.0F);
        well.addTag("no_entity_tooltip");
        well.setColorOverride(new Color(181, 22, 62));
        LocationAPI hyperspace = Global.getSector().getHyperspace();
        hyperspace.addEntity(well);
        well.autoUpdateHyperLocationBasedOnInSystemEntityAtRadius(planet, 0.0F);
        Global.getSector().getMemoryWithoutUpdate().set(NASCENT_WELL_KEY, well);
        CampaignFleetAPI fleet= Global.getFactory().createEmptyFleet("diableavionics","Test",true);
        fleet.getFleetData().addFleetMember("diableavionics_vapor_standard");
        fleet.getFleetData().setShipNameRandom(new Random());
        system.spawnFleet(planet, 500f, 450f, fleet);
        system.autogenerateHyperspaceJumpPoints(false,false, true);
    }
}
