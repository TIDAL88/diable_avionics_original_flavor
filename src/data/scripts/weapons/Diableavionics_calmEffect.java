/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DroneLauncherShipSystemAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

import org.magiclib.paintjobs.MagicPaintjobManager;
import org.magiclib.paintjobs.MagicPaintjobSpec;

import static data.scripts.util.Diableavionics_stringsManager.txt;

/**
 *
 * @author Tartiflette
 */
public class Diableavionics_calmEffect implements EveryFrameWeaponEffectPlugin {

    private boolean runOnce = false, refit = false;
    private DroneLauncherShipSystemAPI system;
    private ShipAPI ship;

    private final String ID = "diableavionics_calmEffect";
    private final String DRONE_PAINTJOB_KEY = "diableavionics_calm_drone_paintjob";

    private int mode = 0;

    private final float SPEED = 20;
    private final float SHIELD = -0.2f;
    private final float WEAPONS = -0.2f;


    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        if (!runOnce) {
            runOnce = true;
            ship = weapon.getShip();
            system = (DroneLauncherShipSystemAPI) ship.getSystem();

            if (ship.getOriginalOwner() == -1) {
                refit = true;
            }
        }

        if (engine.isPaused() || refit) {
            return;
        }


        // Apply the Calm's current MagicLib paintjob family
        // to newly-deployed system drones.
        syncDronePaintjobs();


        switch (system.getDroneOrders()) {

            case RECALL:
            {
                // drones recalled, speed boost
                if (mode != 1) {
                    mode = 1;

                    ship.getMutableStats().getShieldAbsorptionMult().unmodify(ID);
                    ship.getMutableStats().getBeamWeaponFluxCostMult().unmodify(ID);
                    ship.getMutableStats().getEnergyWeaponFluxCostMod().unmodify(ID);
                    ship.getMutableStats().getBallisticWeaponFluxCostMod().unmodify(ID);
                    ship.getMutableStats().getMissileWeaponFluxCostMod().unmodify(ID);

                    ship.getMutableStats().getMaxSpeed().modifyFlat(ID, SPEED);
                }

                if (ship == engine.getPlayerShip()) {
                    engine.maintainStatusForPlayerShip(
                            ID + "engines",
                            "graphics/icons/hullsys/burn_drive.png",
                            txt("stm_calm_0"),
                            "+" + SPEED + txt("stm_calm_1"),
                            false
                    );
                }

                break;
            }


            case DEPLOY:
            {
                // drones holding position, shield boost
                if (mode != 2) {
                    mode = 2;

                    ship.getMutableStats().getMaxSpeed().unmodify(ID);
                    ship.getMutableStats().getBeamWeaponFluxCostMult().unmodify(ID);
                    ship.getMutableStats().getEnergyWeaponFluxCostMod().unmodify(ID);
                    ship.getMutableStats().getBallisticWeaponFluxCostMod().unmodify(ID);
                    ship.getMutableStats().getMissileWeaponFluxCostMod().unmodify(ID);

                    ship.getMutableStats().getShieldAbsorptionMult().modifyMult(ID, 1 + SHIELD);
                }

                if (ship == engine.getPlayerShip()) {
                    engine.maintainStatusForPlayerShip(
                            ID + "shield",
                            "graphics/icons/hullsys/damper_field.png",
                            txt("stm_calm_0"),
                            (int) (SHIELD * 100) + txt("stm_calm_2"),
                            false
                    );
                }

                break;
            }


            case ATTACK:
            {
                // drones attacking, weapon boost
                if (mode != 3) {
                    mode = 3;

                    ship.getMutableStats().getMaxSpeed().unmodify(ID);
                    ship.getMutableStats().getShieldAbsorptionMult().unmodify(ID);

                    ship.getMutableStats().getBeamWeaponFluxCostMult().modifyMult(ID, 1 + WEAPONS);
                    ship.getMutableStats().getEnergyWeaponFluxCostMod().modifyMult(ID, 1 + WEAPONS);
                    ship.getMutableStats().getBallisticWeaponFluxCostMod().modifyMult(ID, 1 + WEAPONS);
                    ship.getMutableStats().getMissileWeaponFluxCostMod().modifyMult(ID, 1 + WEAPONS);
                }

                if (ship == engine.getPlayerShip()) {
                    engine.maintainStatusForPlayerShip(
                            ID + "weapons",
                            "graphics/icons/hullsys/ammo_feeder.png",
                            txt("stm_calm_0"),
                            (int) (WEAPONS * 100) + txt("stm_calm_3"),
                            false
                    );
                }

                break;
            }


            default:
            {
                // drones recalled, speed boost
                if (mode != 1) {
                    mode = 1;

                    ship.getMutableStats().getShieldAbsorptionMult().unmodify(ID);
                    ship.getMutableStats().getBeamWeaponFluxCostMult().unmodify(ID);
                    ship.getMutableStats().getEnergyWeaponFluxCostMod().unmodify(ID);
                    ship.getMutableStats().getBallisticWeaponFluxCostMod().unmodify(ID);
                    ship.getMutableStats().getMissileWeaponFluxCostMod().unmodify(ID);

                    ship.getMutableStats().getMaxSpeed().modifyFlat(ID, SPEED);
                }

                if (ship == engine.getPlayerShip()) {
                    engine.maintainStatusForPlayerShip(
                            ID + "engines",
                            "graphics/icons/hullsys/burn_drive.png",
                            txt("stm_calm_0"),
                            "+" + SPEED + txt("stm_calm_1"),
                            false
                    );
                }

                break;
            }
        }
    }


    private void syncDronePaintjobs() {

        MagicPaintjobSpec calmPaintjob =
                MagicPaintjobManager.getCurrentShipPaintjob(ship.getVariant());

        // Normal/unpainted Calm: nothing to propagate.
        if (calmPaintjob == null) {
            return;
        }

        String family = calmPaintjob.getPaintjobFamily();

        if (family == null || family.isEmpty()) {
            return;
        }


        for (ShipAPI drone : ship.getDeployedDrones()) {

            if (drone == null) {
                continue;
            }

            // This particular drone has already been handled.
            if (drone.getCustomData().containsKey(DRONE_PAINTJOB_KEY)) {
                continue;
            }


            for (MagicPaintjobSpec dronePaintjob :
                    MagicPaintjobManager.getPaintjobsForHull(drone.getHullSpec())) {

                if (family.equals(dronePaintjob.getPaintjobFamily())) {

                    MagicPaintjobManager.applyPaintjob(drone, dronePaintjob);

                    drone.setCustomData(
                            DRONE_PAINTJOB_KEY,
                            true
                    );

                    break;
                }
            }
        }
    }
}