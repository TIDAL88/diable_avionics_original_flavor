package data.scripts;

import com.fs.starfarer.api.Global;

import java.lang.reflect.Method;

/**
 * Isolated optional LunaLib access. This class is only loaded when LunaLib is enabled.
 */
final class DAOptionalLunaSettings {

    private static final String MOD_ID = "diableavionics";
    private static final String SHIELD_COLOR_SETTING_ID = "diable_shield_color";

    private DAOptionalLunaSettings() {
    }

    static String getShieldColorMode() {
        try {
            Class<?> lunaSettings = Global.getSettings().getScriptClassLoader()
                    .loadClass("lunalib.lunaSettings.LunaSettings");
            Method getString = lunaSettings.getMethod("getString", String.class, String.class);
            return (String) getString.invoke(null, MOD_ID, SHIELD_COLOR_SETTING_ID);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to read the optional LunaLib setting.", ex);
        }
    }
}
