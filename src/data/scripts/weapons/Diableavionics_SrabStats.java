package data.scripts.weapons;

import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.CombatListenerUtil;
import com.fs.starfarer.api.combat.listeners.WeaponRangeModifier;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.combat.entities.Missile;

/** Blocks missile stat bonuses on SRAB except ECCM maneuverability and flare resistance. */
public final class Diableavionics_SrabStats implements WeaponRangeModifier {
    private static final String WEAPON_ID = "diableavionics_srab";

    public static void install(ShipAPI ship) {
        if (ship != null && !ship.hasListenerOfClass(Diableavionics_SrabStats.class)) {
            ship.addListener(new Diableavionics_SrabStats());
        }
    }

    /** Called once at creation, before the guidance AI caches the missile's speed. */
    public static void normalizeProjectile(MissileAPI missile) {
        MutableShipStatsAPI engines = missile.getEngineStats();
        engines.getMaxSpeed().unmodify();
        retainEccmModifier(engines.getAcceleration());
        retainEccmModifier(engines.getDeceleration());
        retainEccmModifier(engines.getMaxTurnRate());
        retainEccmModifier(engines.getTurnAcceleration());
        missile.updateMaxSpeed();

        float hitpoints = missile.getSpec().getHullSpec().getHitpoints();
        // MissileAPI exposes only the current-HP setter; the engine's public
        // setter also keeps maximum HP and hull level consistent.
        if (missile instanceof Missile) {
            ((Missile) missile).setMaxHitpoints(hitpoints);
        }
        missile.setHitpoints(hitpoints);

        WeaponAPI weapon = missile.getWeapon();
        ShipAPI ship = missile.getSource();
        if (weapon != null && ship != null) {
            install(ship);
            // Rebuild the normal missile lifetime using energy range bonuses only.
            // Our range listener compensates the launcher's speed-derived range;
            // that compensation must not shorten the projectile's lifetime.
            StatBonus range = ship.getMutableStats().getEnergyWeaponRangeBonus();
            float mult = range.getMult() * otherRangeMult(ship, weapon);
            float percent = range.getPercentMod()
                    + CombatListenerUtil.getWeaponRangePercentMod(ship, weapon);
            float flat = range.getFlatBonus()
                    + CombatListenerUtil.getWeaponRangeFlatMod(ship, weapon);
            missile.setMaxFlightTime(missile.getSpec().getMaxFlightTime()
                    * (1f + percent / 100f) * mult
                    + flat * mult / Math.max(1f, missile.getMaxSpeed()));
            missile.setMaxRange(weapon.getRange());
        }
        // Guidance and ECCM chance are deliberately untouched: flares still work
        // normally, including ECCM's protection against distraction.
    }

    private static void retainEccmModifier(MutableStat stat) {
        // Preserve the actual hullmod modifiers rather than duplicating ECCM's
        // tuning values. This only changes this projectile's engine stats.
        StatMod flat = stat.getFlatStatMod(HullMods.ECCM);
        StatMod percent = stat.getPercentStatMod(HullMods.ECCM);
        StatMod mult = stat.getMultStatMod(HullMods.ECCM);
        stat.unmodify();
        if (flat != null) stat.modifyFlat(HullMods.ECCM, flat.getValue(), flat.getDesc());
        if (percent != null) stat.modifyPercent(HullMods.ECCM, percent.getValue(), percent.getDesc());
        if (mult != null) stat.modifyMult(HullMods.ECCM, mult.getValue(), mult.getDesc());
    }

    private static float otherRangeMult(ShipAPI ship, WeaponAPI weapon) {
        float mult = 1f;
        for (WeaponRangeModifier listener : ship.getListeners(WeaponRangeModifier.class)) {
            if (!(listener instanceof Diableavionics_SrabStats)) {
                mult *= listener.getWeaponRangeMultMod(ship, weapon);
            }
        }
        return mult;
    }

    @Override
    public float getWeaponRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
        return 0f;
    }

    @Override
    public float getWeaponRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
        return 0f;
    }

    @Override
    public float getWeaponRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
        if (!WEAPON_ID.equals(weapon.getId())) return 1f;

        MissileSpecAPI projectile = (MissileSpecAPI) weapon.getSpec().getProjectileSpec();
        float speed = projectile.getHullSpec().getEngineSpec().getMaxSpeed();
        if (speed <= 0f) return 1f;
        float speedMult = ship.getMutableStats().getMissileMaxSpeedBonus()
                .computeEffective(speed) / speed;
        if (speedMult <= 0f || speedMult == 1f) return 1f;

        // The engine multiplies missile-launcher range by missile speed even for
        // ENERGY weapons. Cancel only that factor, preserving energy range mods
        // and the range threshold used by hullmods such as ITU.
        StatBonus range = ship.getMutableStats().getEnergyWeaponRangeBonus();
        float base = Misc.getAdjustedBaseRange(weapon.getSpec().getMaxRange(), ship, weapon);
        float percent = range.getPercentMod()
                + CombatListenerUtil.getWeaponRangePercentMod(ship, weapon);
        float flat = range.getFlatBonus()
                + CombatListenerUtil.getWeaponRangeFlatMod(ship, weapon);
        float raw = (base * (1f + percent / 100f) + flat)
                * range.getMult() * otherRangeMult(ship, weapon);
        if (raw <= 0f) return 1f / speedMult;

        MutableShipStatsAPI stats = ship.getMutableStats();
        float threshold = stats.getWeaponRangeThreshold().getModifiedValue();
        float pastThreshold = stats.getWeaponRangeMultPastThreshold().getModifiedValue();
        float intended = raw <= threshold ? raw : threshold + (raw - threshold) * pastThreshold;
        float compensated = intended / speedMult;
        if (compensated > threshold && pastThreshold > 0f) {
            compensated = threshold + (compensated - threshold) / pastThreshold;
        }
        return compensated / raw;
    }
}
