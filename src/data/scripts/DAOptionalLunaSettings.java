package data.scripts;

import lunalib.lunaSettings.LunaSettings;

/**
 * Isolated optional LunaLib access. This class is only loaded when LunaLib is enabled.
 */
public final class DAOptionalLunaSettings {

    private static final String MOD_ID = "diableavionics";
    private static final String SHIELD_COLOR_SETTING_ID = "diable_shield_color";
    private static final String LAST_LINE_EASY_SETTING_ID = "diable_last_line_easy";

    private DAOptionalLunaSettings() {
    }

    public static String getShieldColorMode() {
        return LunaSettings.getString(MOD_ID, SHIELD_COLOR_SETTING_ID);
    }

    public static boolean useClassicLastLineFleet() {
        return Boolean.TRUE.equals(
                LunaSettings.getBoolean(MOD_ID, LAST_LINE_EASY_SETTING_ID)
        );
    }
}
