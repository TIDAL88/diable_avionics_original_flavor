package data.scripts.campaign.terrain;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.terrain.NebulaTerrainPlugin;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

/**
 * Visual-only amber nebula used at the perimeter of the Black Site. It keeps
 * the vanilla tiled-nebula renderer, but deliberately has no campaign effects,
 * tooltip, AI avoidance flag, or music suppression.
 */
public class DiableBlackSiteNebulaTerrainPlugin extends NebulaTerrainPlugin {

    private static final Color SYSTEM_COLOR = new Color(255, 190, 105, 145);
    private static final Color MAP_COLOR = new Color(255, 211, 132, 225);

    @Override
    public Color getRenderColor() {
        return SYSTEM_COLOR;
    }

    /** Keeps the distant system-map spiral legible without over-brightening flight view. */
    @Override
    public void preMapRender(float alphaMult) {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        int alpha = Math.max(0, Math.min(255, (int) (MAP_COLOR.getAlpha() * alphaMult)));
        GL11.glColor4ub(
                (byte) MAP_COLOR.getRed(),
                (byte) MAP_COLOR.getGreen(),
                (byte) MAP_COLOR.getBlue(),
                (byte) alpha
        );
    }

    @Override
    public void applyEffect(SectorEntityToken entity, float days) {
        // Cosmetic terrain: do not alter sensor range or maximum burn.
    }

    @Override
    public boolean hasTooltip() {
        return false;
    }

    @Override
    public boolean isTooltipExpandable() {
        return false;
    }

    @Override
    public boolean hasAIFlag(Object flag) {
        return false;
    }
}
