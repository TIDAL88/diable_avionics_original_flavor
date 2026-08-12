package data.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.util.Misc;
import org.dark.shaders.util.ShaderLib;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Vector2f;

import java.util.EnumSet;

/**
 * Color grading for Subject 71's native-rendered simulator background.
 */
public class DASimulacrumBackgroundRenderer
        extends BaseCombatLayeredRenderingPlugin {

    private static final String DOMAIN_SHADER =
            "data/shaders/diableavionics_simulacrum.shader";
    private static final Vector2f MAP_CENTER = new Vector2f(0f, 0f);
    private static final float SHADER_STRENGTH = 0.70f;

    private static int shader = 0;
    private static boolean shaderInit = false;

    private final boolean mapScale;
    private float currentRadius = 0f;
    private float effectLevel = 0f;

    public DASimulacrumBackgroundRenderer(boolean mapScale) {
        this.mapScale = mapScale;
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

    public void setActiveState(float effectLevel, float radius) {
        this.effectLevel = Math.max(0f, Math.min(1f, effectLevel));
        float desiredRadius = mapScale
                ? computeMapRadius()
                : Math.max(0f, radius);
        currentRadius = desiredRadius * this.effectLevel;
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
        if (viewport == null
                || layer != CombatEngineLayers.JUST_BELOW_WIDGETS) {
            return;
        }
        renderShaderMask();
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
                effectLevel * SHADER_STRENGTH
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
        return EnumSet.of(CombatEngineLayers.JUST_BELOW_WIDGETS);
    }
}
