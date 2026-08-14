package data.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import org.lwjgl.opengl.GL11;

/**
 * Lightweight, screen-locked simulator display treatment.
 * Uses only fixed-function alpha blending: no shaders, stencil, or libraries.
 */
public class DASimulacrumScanlineOverlay
        extends BaseEveryFrameCombatPlugin {

    private static final int LINE_SPACING_PIXELS = 5;
    private static final float LINE_WIDTH_PIXELS = 1.5f;

    private static final float TINT_RED = 10f / 255f;
    private static final float TINT_GREEN = 31f / 255f;
    private static final float TINT_BLUE = 36f / 255f;
    private static final float TINT_ALPHA = 0.10f;

    private static final float LINE_RED = 2f / 255f;
    private static final float LINE_GREEN = 10f / 255f;
    private static final float LINE_BLUE = 12f / 255f;
    private static final float LINE_ALPHA = 0.20f;

    private boolean renderLogged;

    @Override
    public void init(CombatEngineAPI engine) {
        Global.getLogger(DASimulacrumScanlineOverlay.class).info(
                "Simulacrum scanline overlay attached"
        );
    }

    @Override
    public void renderInUICoords(ViewportAPI viewport) {
        if (viewport == null) return;

        if (!renderLogged) {
            renderLogged = true;
            Global.getLogger(DASimulacrumScanlineOverlay.class).info(
                    "Simulacrum scanline overlay rendering"
            );
        }

        float left = 0f;
        float bottom = 0f;
        float right = Global.getSettings().getScreenWidth();
        float top = Global.getSettings().getScreenHeight();

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(
                GL11.GL_SRC_ALPHA,
                GL11.GL_ONE_MINUS_SRC_ALPHA
        );

        GL11.glBegin(GL11.GL_QUADS);

        // A restrained dark teal wash ties the combat scene to the display.
        GL11.glColor4f(TINT_RED, TINT_GREEN, TINT_BLUE, TINT_ALPHA);
        drawQuad(left, bottom, right, top);

        // UI coordinates keep the pattern fixed to the physical display;
        // camera zoom and movement cannot alter its width or spacing.
        GL11.glColor4f(LINE_RED, LINE_GREEN, LINE_BLUE, LINE_ALPHA);
        for (float pixelY = 0f;
             pixelY < top;
             pixelY += LINE_SPACING_PIXELS) {
            drawQuad(
                    left,
                    pixelY,
                    right,
                    Math.min(top, pixelY + LINE_WIDTH_PIXELS)
            );
        }

        GL11.glEnd();
        GL11.glPopAttrib();
    }

    private void drawQuad(float left, float bottom, float right, float top) {
        GL11.glVertex2f(left, bottom);
        GL11.glVertex2f(right, bottom);
        GL11.glVertex2f(right, top);
        GL11.glVertex2f(left, top);
    }

}
