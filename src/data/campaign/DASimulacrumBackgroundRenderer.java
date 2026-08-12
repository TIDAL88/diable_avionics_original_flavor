package data.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.EnumSet;

/**
 * Map-sized, stencil-backed backdrop for Subject 71's fleet simulator.
 */
public class DASimulacrumBackgroundRenderer
        extends BaseCombatLayeredRenderingPlugin {

    private static final String DOMAIN_BACKGROUND_PRIMARY =
            "graphics/da/backgrounds/diableavionics_simulacrum.png";
    private static final Vector2f MAP_CENTER = new Vector2f(0f, 0f);
    private static final Color BASE_COLOR = Color.WHITE;

    private final SpriteAPI background;

    private float currentRadius = 0f;

    public DASimulacrumBackgroundRenderer() {
        this.background = loadSpriteOrFallback();
    }

    private SpriteAPI loadSpriteOrFallback() {
        SpriteAPI sprite = tryLoadSprite(DOMAIN_BACKGROUND_PRIMARY);
        if (sprite != null) return sprite;
        sprite = tryLoadSprite("graphics/backgrounds/hyperspace1.jpg");
        if (sprite != null) return sprite;
        return Global.getSettings().getSprite("misc", "nebula_particles");
    }

    private SpriteAPI tryLoadSprite(String path) {
        if (path == null || path.isEmpty()) return null;
        try {
            Global.getSettings().loadTexture(path);
            return Global.getSettings().getSprite(path);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public void setMapActiveState(float effectLevel) {
        float clampedLevel = Math.max(0f, Math.min(1f, effectLevel));
        currentRadius = computeMapRadius() * clampedLevel;
    }

    private float computeMapRadius() {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null
                || engine.getMapWidth() <= 0f
                || engine.getMapHeight() <= 0f) {
            return 12000f;
        }
        float halfWidth = engine.getMapWidth() * 0.5f;
        float halfHeight = engine.getMapHeight() * 0.5f;
        return (float) Math.sqrt(
                (halfWidth * halfWidth) + (halfHeight * halfHeight)
        ) + 2000f;
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        if (viewport == null
                || currentRadius <= 5f
                || layer != CombatEngineLayers.BELOW_PLANETS) {
            return;
        }
        startStencil(MAP_CENTER, currentRadius, 160);
        try {
            renderDomainBackdrop(viewport);
        } finally {
            endStencil();
        }
    }

    private void renderDomainBackdrop(ViewportAPI viewport) {
        float width = viewport.getVisibleWidth();
        float height = viewport.getVisibleHeight();
        float x = viewport.getLLX() + (width * 0.5f);
        float y = viewport.getLLY() + (height * 0.5f);

        background.setNormalBlend();
        background.setColor(BASE_COLOR);
        background.setAlphaMult(viewport.getAlphaMult());
        background.setSize(width, height);
        background.setAngle(0f);
        background.renderAtCenter(x, y);
    }

    private void startStencil(Vector2f center, float radius, int segments) {
        GL11.glClearStencil(0);
        GL11.glStencilMask(0xff);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glColorMask(false, false, false, false);
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xff);
        GL11.glStencilMask(0xff);
        GL11.glStencilOp(
                GL11.GL_REPLACE,
                GL11.GL_REPLACE,
                GL11.GL_REPLACE
        );
        GL11.glBegin(GL11.GL_POLYGON);
        for (int i = 0; i <= segments; i++) {
            double angle = (2d * Math.PI * i) / segments;
            GL11.glVertex2d(
                    center.x + Math.cos(angle) * radius,
                    center.y + Math.sin(angle) * radius
            );
        }
        GL11.glEnd();
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        GL11.glColorMask(true, true, true, true);
        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xff);
    }

    private void endStencil() {
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    @Override
    public boolean isExpired() {
        return false;
    }

    @Override
    public float getRenderRadius() {
        return 100000f;
    }

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.of(CombatEngineLayers.BELOW_PLANETS);
    }
}
