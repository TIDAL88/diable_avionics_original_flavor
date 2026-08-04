package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;

import java.util.ArrayList;
import java.util.List;

public class DiableAvionicsRime_Master extends BaseHullMod {
    public static final String TOGGLE_ACTIVE_HULLMOD = "rime_active_bay";
    public static final String TOGGLE_STANDBY_HULLMOD = "rime_standby_bay";

    private static final List<String> MODE_LIST = new ArrayList<>();
    static {
        MODE_LIST.add(TOGGLE_STANDBY_HULLMOD);
        MODE_LIST.add(TOGGLE_ACTIVE_HULLMOD);
    }

    private static final String TAG_PREFIX = "rime_mode_";

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        ShipVariantAPI variant = (stats.getFleetMember() != null)
                ? stats.getFleetMember().getVariant()
                : stats.getVariant();
        if (variant == null) return;

        boolean switchMode = true;
        for (String mode : MODE_LIST) {
            if (variant.getHullMods().contains(mode)) {
                switchMode = false;
                break;
            }
        }


        if (switchMode) {
            int nextIndex = 0;

            for (int i = 0; i < MODE_LIST.size(); i++) {
                if (variant.hasTag(TAG_PREFIX + MODE_LIST.get(i))) {
                    nextIndex = i + 1;
                    if (nextIndex >= MODE_LIST.size()) {
                        nextIndex = 0;
                    }
                    break;
                }
            }

            String targetMode = MODE_LIST.get(nextIndex);
            variant.addMod(targetMode);
            setModeTag(variant, targetMode);
            return;
        }

        if (variant.hasHullMod(TOGGLE_STANDBY_HULLMOD) && variant.hasHullMod(TOGGLE_ACTIVE_HULLMOD)) {
            if (variant.hasTag(TAG_PREFIX + TOGGLE_STANDBY_HULLMOD)) {
                variant.removeMod(TOGGLE_STANDBY_HULLMOD);
                setModeTag(variant, TOGGLE_ACTIVE_HULLMOD);
            } else {
                variant.removeMod(TOGGLE_ACTIVE_HULLMOD);
                setModeTag(variant, TOGGLE_STANDBY_HULLMOD);
            }
        } else {
            for (String mode : MODE_LIST) {
                if (variant.hasHullMod(mode)) {
                    setModeTag(variant, mode);
                    break;
                }
            }
        }
    }

    private void setModeTag(ShipVariantAPI variant, String currentMode) {
        for (String mode : MODE_LIST) {
            variant.removeTag(TAG_PREFIX + mode);
        }
        variant.addTag(TAG_PREFIX + currentMode);
    }
}