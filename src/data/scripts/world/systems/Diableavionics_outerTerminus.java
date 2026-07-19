package data.scripts.world.systems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;

import static data.scripts.util.Diableavionics_stringsManager.txt;

public class Diableavionics_outerTerminus implements SectorGeneratorPlugin {

//    public static SectorEntityToken getSectorAccess() {
//        return Global.getSector().getStarSystem(txt("star_C")).getEntityByName("");
//    }

    @Override
    public void generate(SectorAPI sector) {
        StarSystemAPI system = sector.createStarSystem(txt("star_C"));
        system.setOptionalUniqueId("diableavionics_outerTerminus");
        system.setBackgroundTextureFilename("graphics/da/backgrounds/diableavionics_outerTerminus.jpg");

        // create the star and generate the hyperspace anchor for this system
        PlanetAPI star = system.initStar(txt("star_C_star_1"), // unique id for this star
                "star_white", // id in planets.json
                450f,
                250);        // radius (in pixels at default zoom)
        system.setLightColor(new Color(255, 250, 250)); // light color in entire system, affects all entities

        system.getLocation().set(29000, -5000);
        PlanetAPI OT1 = system.addPlanet("OT_a",
                star,
                txt("star_C_planet_0"),
                "rocky_unstable",
                25,
                80,
                2000,
                150
        );

        //JUMP POINT
        JumpPointAPI jumpPoint1 = Global.getFactory().createJumpPoint("OT_jumpPointA",
                txt("star_C_jp_0")
        );
        OrbitAPI orbit = Global.getFactory().createCircularOrbit(star, 85, 2000, 150);
        OrbitAPI orbit2 = Global.getFactory().createCircularOrbit(star, 125, 2500, 250);
        jumpPoint1.setOrbit(orbit);
        jumpPoint1.setRelatedPlanet(OT1);
        jumpPoint1.setStandardWormholeToHyperspaceVisual();
        system.addEntity(jumpPoint1);
        SectorEntityToken gate = system.addCustomEntity("sivie_gate", "Sivie Gate", "inactive_gate", "neutral");
        gate.setOrbit(orbit2);
        system.addEntity(gate);
        //3000
        PlanetAPI ach2 = system.addPlanet("diableavionics_prison",
                star,
                txt("star_C_planet_1"),
                "terran",
                180,
                150,
                3000,
                250
        );
        ach2.setCustomDescriptionId("diableavionics_prison");

        //3750
        //ASTEROID BELT
        system.addAsteroidBelt(star, 750, 3750, 512, 310, 330);
        SectorEntityToken DA_piratePort = system.addCustomEntity("diableavionics_ressource",
                txt("star_C_station_1"),
                "diableavionics_station_ressource",
                "diableavionics");
        DA_piratePort.setCircularOrbitPointingDown(star, 62, 4000, 335);

        //OLD RELAY
        SectorEntityToken relay = system.addCustomEntity("OT_abandonned_relay", // unique id
                txt("star_C_relay"), // name - if null, defaultName from custom_entities.json will be used
                "comm_relay", // type of object, defined in custom_entities.json
                "diableavionics"); // faction
        relay.setCircularOrbit(star, 150, 4250, 350);

        system.autogenerateHyperspaceJumpPoints(true, true, true);
        system.setEnteredByPlayer(true);
        Misc.setAllPlanetsSurveyed(system, true);
        for (MarketAPI market : Global.getSector().getEconomy().getMarkets(system)) {
            market.setSurveyLevel(MarketAPI.SurveyLevel.FULL); // could also be a station, not a planet
        }

        cleanup(system);
    }

    void cleanup(StarSystemAPI system) {
        HyperspaceTerrainPlugin plugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
        NebulaEditor editor = new NebulaEditor(plugin);
        float minRadius = plugin.getTileSize() * 2f;

        float radius = system.getMaxRadiusInHyperspace();
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius * 0.5f, 0, 360f);
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f, 0.25f);
    }
}
