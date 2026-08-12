package data.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import org.dark.shaders.util.ShaderLib;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
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
    private static final String DOMAIN_SHADER =
            "data/shaders/diableavionics_simulacrum.shader";
    private static final Vector2f MAP_CENTER = new Vector2f(0f, 0f);
    private static final Color BASE_COLOR = Color.WHITE;
    private static final float BACKGROUND_OVERSCAN = 1.20f;
    private static final float PARALLAX_STRENGTH = 0.85f;

    private static int shader = 0;
    private static boolean shaderInit = false;

    private final boolean mapScale;
    private final SpriteAPI background;
    private final float backgroundAspectRatio;

    private float targetRadius = 0f;
    private float currentRadius = 0f;
    private float effectLevel = 0f;

    public DASimulacrumBackgroundRenderer(boolean mapScale) {
        this.mapScale = mapScale;
        this.background = loadSpriteOrFallback();
        this.backgroundAspectRatio = background.getHeight() > 0f
                ? background.getWidth() / background.getHeight()
                : 1f;
        ensureShaderLoaded();
    }

    private static void ensureShaderLoaded() {
        if (shaderInit) return;
        shaderInit = true;
        try {
            shader = ShaderLib.loadShader(
                    Global.getSettings().loadText(
                            "data/shaders/baseVertex.shader"
                    ),
                    Global.getSettings().loadText(DOMAIN_SHADER)
            );
            if (shader != 0) {
                GL20.glUseProgram(shader);
                GL20.glUniform1i(
                        GL20.glGetUniformLocation(shader, "tex"),
                        0
                );
                GL20.glUseProgram(0);
            }
        } catch (Throwable ignored) {
            shader = 0;
        }
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

    public void setActiveState(float effectLevel, float radius) {
        this.effectLevel = Math.max(0f, Math.min(1f, effectLevel));
        float desiredRadius = mapScale
                ? computeMapRadius()
                : Math.max(0f, radius);
        targetRadius = desiredRadius * this.effectLevel;

        // The simulator background must already be present on deployment.
        currentRadius = targetRadius;
    }

    public void setMapActiveState(float effectLevel) {
        setActiveState(effectLevel, 0f);
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
        if (viewport == null) return;
        if (layer == CombatEngineLayers.JUST_BELOW_WIDGETS) {
            renderShaderMask();
            return;
        }
        if (currentRadius <= 5f) return;
        if (layer == CombatEngineLayers.ABOVE_PLANETS) {
            startStencil(MAP_CENTER, currentRadius, mapScale ? 160 : 96);
            try {
                renderDomainBackdrop(viewport);
            } finally {
                endStencil();
            }
        }
    }

    private void renderShaderMask() {
        if (shader == 0
                || ShaderLib.getScreenTexture() == 0
                || currentRadius <= 5f) {
            return;
        }

        Vector2f worldEdge = Misc.getUnitVectorAtDegreeAngle(0f);
        worldEdge.scale(currentRadius);
        worldEdge = Vector2f.add(MAP_CENTER, worldEdge, new Vector2f());
        Vector2f screenCenter = ShaderLib.transformWorldToScreen(MAP_CENTER);
        Vector2f screenEdge = ShaderLib.transformWorldToScreen(worldEdge);
        float radiusPx = Misc.getDistance(screenCenter, screenEdge);
        Vector2f centerUV = ShaderLib.transformScreenToUV(screenCenter);

        ShaderLib.beginDraw(shader);
        GL20.glUniform1f(
                GL20.glGetUniformLocation(shader, "intensity"),
                Math.max(0.35f, effectLevel)
        );
        GL20.glUniform2f(
                GL20.glGetUniformLocation(shader, "centerUV"),
                centerUV.x,
                centerUV.y
        );
        GL20.glUniform1f(
                GL20.glGetUniformLocation(shader, "radiusPx"),
                radiusPx
        );
        GL20.glUniform1f(
                GL20.glGetUniformLocation(shader, "screenWidth"),
                Global.getSettings().getScreenWidthPixels()
        );
        GL20.glUniform1f(
                GL20.glGetUniformLocation(shader, "screenHeight"),
                Global.getSettings().getScreenHeightPixels()
        );
        GL20.glUniform1f(
                GL20.glGetUniformLocation(shader, "visibleU"),
                ShaderLib.getVisibleU()
        );
        GL20.glUniform1f(
                GL20.glGetUniformLocation(shader, "visibleV"),
                ShaderLib.getVisibleV()
        );
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(
                GL11.GL_TEXTURE_2D,
                ShaderLib.getScreenTexture()
        );
        GL11.glDisable(GL11.GL_BLEND);
        ShaderLib.screenDraw(
                ShaderLib.getScreenTexture(),
                GL13.GL_TEXTURE0
        );
        ShaderLib.exitDraw();
    }

    private void renderDomainBackdrop(ViewportAPI viewport) {
        float viewportWidth = viewport.getVisibleWidth();
        float viewportHeight = viewport.getVisibleHeight();
        float viewportAspect = viewportWidth / viewportHeight;

        // Cover the viewport without distorting the square source image,
        // leaving enough overdraw for smooth, bounded camera parallax.
        float renderWidth;
        float renderHeight;
        if (backgroundAspectRatio > viewportAspect) {
            renderHeight = viewportHeight * BACKGROUND_OVERSCAN;
            renderWidth = renderHeight * backgroundAspectRatio;
        } else {
            renderWidth = viewportWidth * BACKGROUND_OVERSCAN;
            renderHeight = renderWidth / backgroundAspectRatio;
        }

        Vector2f cameraCenter = viewport.getCenter();
        CombatEngineAPI engine = Global.getCombatEngine();
        float cameraX = 0f;
        float cameraY = 0f;
        if (engine != null) {
            float horizontalTravel = Math.max(
                    1f,
                    (engine.getMapWidth() - viewportWidth) * 0.5f
            );
            float verticalTravel = Math.max(
                    1f,
                    (engine.getMapHeight() - viewportHeight) * 0.5f
            );
            cameraX = clamp(cameraCenter.x / horizontalTravel, -1f, 1f);
            cameraY = clamp(cameraCenter.y / verticalTravel, -1f, 1f);
        }

        float horizontalMargin = (renderWidth - viewportWidth) * 0.5f;
        float verticalMargin = (renderHeight - viewportHeight) * 0.5f;
        float x = cameraCenter.x
                - cameraX * horizontalMargin * PARALLAX_STRENGTH;
        float y = cameraCenter.y
                - cameraY * verticalMargin * PARALLAX_STRENGTH;

        background.setNormalBlend();
        background.setColor(BASE_COLOR);
        background.setAlphaMult(effectLevel);
        background.setSize(renderWidth, renderHeight);
        background.setAngle(0f);
        background.renderAtCenter(x, y);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
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
        return EnumSet.of(
                CombatEngineLayers.ABOVE_PLANETS,
                CombatEngineLayers.JUST_BELOW_WIDGETS
        );
    }
}
