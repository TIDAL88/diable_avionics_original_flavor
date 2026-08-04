package data.scripts.world.systems;

import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.GateEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.impl.campaign.terrain.BaseTiledTerrain;
import com.fs.starfarer.api.impl.campaign.terrain.EventHorizonPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Diableavionics_blackSite {

    public static final String SYSTEM_ID = "diable_blacksite";
    public static final String STAR_ID = "88 Ra";
    public static final String PLANET_ID = "diableavionics_blacksite";
    public static final String STATION_ID = "diableavionics_blacksite_station";
    public static final String RUPTURED_GATE_ID = "diableavionics_blacksite_ruptured_gate";
    public static final String NASCENT_WELL_KEY = "$diable_well";
    private static final String MUSIC_SET_ID = "diableavionics_blacksite_ambience";
    private static final String COSMETIC_NEBULA_TYPE = "diableavionics_blacksite_nebula";
    private static final String RUPTURED_GATE_TYPE =
            "diableavionics_blacksite_ruptured_gate";
    private static final String DECORATIVE_WRECK_TYPE =
            "diableavionics_blacksite_decorative_wreck";
    private static final String GATE_WRECK_ID_PREFIX =
            "diableavionics_blacksite_gate_wreck_";

    private static final String FOB_PLANET_TYPE = "diableavionics_blacksite_burned";
    private static final String FOB_TEXTURE = "graphics/da/planets/diableavionics_blacksite_scorched.jpg";
    private static final String FOB_GLOW_TEXTURE =
            "graphics/da/planets/diableavionics_blacksite_scorched_glow.png";

    private static final String ACCRETION_INNER_ID = "diableavionics_blacksite_accretion_inner";
    private static final String ACCRETION_HOT_ID = "diableavionics_blacksite_accretion_hot";
    private static final String ACCRETION_MIDDLE_ID = "diableavionics_blacksite_accretion_middle";
    private static final String ACCRETION_OUTER_ID = "diableavionics_blacksite_accretion_outer";
    private static final String CINDER_RING_ID = "diableavionics_blacksite_cinder_ring";
    private static final String PERIMETER_NEBULA_ID = "diableavionics_blacksite_perimeter_nebula";
    private static final String PLANET_DEBRIS_RING_ID = "diableavionics_blacksite_planet_debris_ring";
    private static final String WRECKAGE_ID = "diableavionics_blacksite_wreckage";
    private static final String DEBRIS_TRAIL_TAG = "diableavionics_blacksite_debris_trail";
    private static final String FOB_DEBRIS_TAG = "diableavionics_blacksite_fob_debris";
    private static final String DEBRIS_TRAIL_ID_PREFIX = "diableavionics_blacksite_debris_trail_";
    private static final String FOB_DEBRIS_ID_PREFIX = "diableavionics_blacksite_fob_debris_";
    private static final int DEBRIS_TRAIL_COUNT = 320;
    private static final int FOB_DEBRIS_COUNT = 96;

    private static final String[] BURNED_VISUAL_IDS = {
            ACCRETION_INNER_ID,
            ACCRETION_HOT_ID,
            ACCRETION_MIDDLE_ID,
            ACCRETION_OUTER_ID,
            CINDER_RING_ID,
            PERIMETER_NEBULA_ID,
            PLANET_DEBRIS_RING_ID,
            WRECKAGE_ID
    };

    private static final float PLANET_RADIUS = 268f;
    private static final float PLANET_ORBIT_RADIUS = 3750f;
    private static final float PLANET_ORBIT_DAYS = 125f;
    private static final float CINDER_RING_RADIUS = 2450f;
    private static final float CINDER_RING_WIDTH = 280f;
    private static final float PLANET_ORBIT_ANGLE = 45f;
    private static final float GATE_ORBIT_RADIUS =
            CINDER_RING_RADIUS - CINDER_RING_WIDTH * 0.5f + 15f;
    private static final float GATE_SPIN_DEGREES_PER_DAY = 2f;
    private static final Color WELL_COLOR = new Color(181, 22, 62);

    private static final GateWreckSpec[] GATE_WRECKS = {
            // Capital wrecks clumped between the ruptured gate and the black hole.
            new GateWreckSpec(
                    "pandemonium_1", "diableavionics_pandemonium_willBreaker", -4f, -160f
            ),
            new GateWreckSpec(
                    "pandemonium_2",
                    "diableavionics_pandemonium_extinguisher_large",
                    4.1f,
                    -180f
            ),
            new GateWreckSpec(
                    "maelstrom_1", "diableavionics_maelstrom_standard", -1.5f, -310f
            ),
            new GateWreckSpec(
                    "maelstrom_2", "diableavionics_maelstrom_vanguard", 1.6f, -340f
            ),
            new GateWreckSpec(
                    "maelstrom_3", "diableavionics_maelstrom_beamer", -2.2f, -500f
            ),
            new GateWreckSpec(
                    "maelstrom_4", "diableavionics_maelstrom_brawler", 2.3f, -560f
            )
    };

    public void generate(SectorAPI sector) {
        StarSystemAPI system = sector.getStarSystem(SYSTEM_ID);
        if (system == null) {
            system = createSystem(sector);
        }
        ensureGateCluster(system);
    }

    private StarSystemAPI createSystem(SectorAPI sector) {
        StarSystemAPI system = sector.createStarSystem(SYSTEM_ID);
        system.setOptionalUniqueId("diable_blacksite");
        system.setName("88 Ra");
        PlanetAPI star = system.initStar(STAR_ID, StarTypes.BLACK_HOLE, 500f, 450f);
        system.getLocation().set(35000f, -7000f);
        star.setName("88 Ra");
        star.setCustomDescriptionId(STAR_ID);
        PlanetAPI planet = system.addPlanet(
                PLANET_ID,
                star,
                "88 Ra I",
                FOB_PLANET_TYPE,
                PLANET_ORBIT_ANGLE,
                PLANET_RADIUS,
                PLANET_ORBIT_RADIUS,
                PLANET_ORBIT_DAYS
        );
        CustomCampaignEntityAPI station;
        MarketAPI market = planet.getMarket();
        market.addCondition("irradiated");
        market.addCondition(Conditions.RUINS_VAST);
        system.setType(StarSystemGenerator.StarSystemType.DEEP_SPACE);
        system.addTag(Tags.THEME_UNSAFE);
        system.addTag(Tags.THEME_HIDDEN);
        system.addTag(Tags.THEME_SPECIAL);
        system.addTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER);
        system.setBackgroundTextureFilename("graphics/da/fx/da_riftbackground.png");
        system.setLightColor(new Color(218, 68, 34, 255));
        system.getMemoryWithoutUpdate().set(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY, MUSIC_SET_ID);

        planet.setCustomDescriptionId(PLANET_ID);

        styleFobPlanet(planet);
        planet.setCircularOrbit(star, PLANET_ORBIT_ANGLE, PLANET_ORBIT_RADIUS, PLANET_ORBIT_DAYS);

        styleBlackHole(star);
        replaceEventHorizon(system, star);
        rebuildBurnedSystemVisuals(system, star, planet);
        replacePlanetMagneticFields(system, planet);
        station = system.addCustomEntity(
                STATION_ID,
                "First Relay",
                STATION_ID,
                Factions.NEUTRAL
        );

        station.setCustomDescriptionId(STATION_ID);
        station.setCircularOrbitPointingDown(planet, 180f, 850f, 55f);
        station.setDiscoverable(true);
        station.setDiscoveryXP(500f);
        system.generateAnchorIfNeeded();
        system.updateAllOrbits();
        NascentGravityWellAPI well;
        LocationAPI hyperspace = sector.getHyperspace();
        well = sector.createNascentGravityWell(station, 50f);
        hyperspace.addEntity(well);
        sector.getMemoryWithoutUpdate().set(NASCENT_WELL_KEY, well);
        well.addTag(Tags.NO_ENTITY_TOOLTIP);
        well.setColorOverride(WELL_COLOR);
        well.autoUpdateHyperLocationBasedOnInSystemEntityAtRadius(station, 0f);
        system.generateAnchorIfNeeded();
        system.updateAllOrbits();
        return system;
    }

    private void ensureGateCluster(StarSystemAPI system) {
        PlanetAPI star = system.getStar();
        SectorEntityToken planet = system.getEntityById(PLANET_ID);
        if (star == null || planet == null) return;

        float gateOrbitAngle = (float) Math.toDegrees(Math.atan2(
                star.getLocation().y - planet.getLocation().y,
                star.getLocation().x - planet.getLocation().x
        ));
        if (gateOrbitAngle < 0f) {
            gateOrbitAngle += 360f;
        }

        SectorEntityToken existing = system.getEntityById(RUPTURED_GATE_ID);
        CustomCampaignEntityAPI gate;
        if (existing instanceof CustomCampaignEntityAPI
                && RUPTURED_GATE_TYPE.equals(existing.getCustomEntityType())) {
            gate = (CustomCampaignEntityAPI) existing;
        } else {
            if (existing != null) {
                if (existing.getCustomPlugin() instanceof GateEntityPlugin) {
                    existing.getMemoryWithoutUpdate().unset(GateEntityPlugin.GATE_SCANNED);
                    GateEntityPlugin.getGateData().scanned.remove(existing);
                }
                system.removeEntity(existing);
            }
            gate = system.addCustomEntity(
                    RUPTURED_GATE_ID,
                    "Ruptured Gate",
                    RUPTURED_GATE_TYPE,
                    Factions.NEUTRAL
            );
        }

        makeDecorativeEntity(gate);
        gate.setCircularOrbitWithSpin(
                star,
                gateOrbitAngle,
                GATE_ORBIT_RADIUS,
                PLANET_ORBIT_DAYS,
                GATE_SPIN_DEGREES_PER_DAY,
                GATE_SPIN_DEGREES_PER_DAY
        );

        for (GateWreckSpec spec : GATE_WRECKS) {
            ensureGateWreck(system, star, gateOrbitAngle, spec);
        }
        system.updateAllOrbits();
    }

    private void ensureGateWreck(
            StarSystemAPI system,
            PlanetAPI star,
            float gateOrbitAngle,
            GateWreckSpec spec
    ) {
        String id = GATE_WRECK_ID_PREFIX + spec.idSuffix;
        SectorEntityToken wreck = system.getEntityById(id);
        if (wreck != null && !DECORATIVE_WRECK_TYPE.equals(wreck.getCustomEntityType())) {
            system.removeEntity(wreck);
            wreck = null;
        }
        if (wreck == null) {
            ShipRecoverySpecial.PerShipData ship =
                    new ShipRecoverySpecial.PerShipData(
                            spec.variantId,
                            ShipRecoverySpecial.ShipCondition.WRECKED,
                            0f
                    );
            DerelictShipEntityPlugin.DerelictShipData data =
                    new DerelictShipEntityPlugin.DerelictShipData(ship, false);
            data.durationDays = Float.MAX_VALUE;
            wreck = system.addCustomEntity(
                    id,
                    null,
                    DECORATIVE_WRECK_TYPE,
                    Factions.NEUTRAL,
                    data
            );
        }

        makeDecorativeEntity(wreck);
        wreck.setCircularOrbit(
                star,
                gateOrbitAngle + spec.angleOffset,
                GATE_ORBIT_RADIUS + spec.radiusOffset,
                PLANET_ORBIT_DAYS
        );
    }

    private void makeDecorativeEntity(SectorEntityToken entity) {
        entity.addTag(Tags.NON_CLICKABLE);
        entity.addTag(Tags.NO_ENTITY_TOOLTIP);
        entity.addTag(Tags.NOT_RANDOM_MISSION_TARGET);
        entity.setSensorProfile(null);
        entity.setDiscoverable(false);
    }

    /**
     * scorched world.
     */
    private void styleFobPlanet(PlanetAPI planet) {
        if (!FOB_PLANET_TYPE.equals(planet.getTypeId())) {
            planet.changeType(FOB_PLANET_TYPE, StarSystemGenerator.random);
        }

        PlanetSpecAPI spec = planet.getSpec();
        spec.setTexture(FOB_TEXTURE);
        spec.setGlowTexture(FOB_GLOW_TEXTURE);
        spec.setGlowColor(Color.WHITE);
        spec.setUseReverseLightForGlow(false);
        spec.setPlanetColor(Color.WHITE);
        spec.setAtmosphereThickness(0.1f);
        spec.setAtmosphereThicknessMin(30f);
        spec.setAtmosphereColor(new Color(215, 50, 15, 60));
        spec.setIconColor(new Color(215, 115, 55, 255));
        spec.setTilt(-20f);
        spec.setPitch(-5f);
        spec.setRotation(-3.5f);
        planet.setRadius(PLANET_RADIUS);
        planet.applySpecChanges();
    }


    /**
     * black-hole sprite recolor
     */
    private void styleBlackHole(PlanetAPI star) {
        PlanetSpecAPI spec = star.getSpec();
        spec.setPlanetColor(new Color(0, 0, 0, 255));
        spec.setAtmosphereColor(new Color(255, 218, 135, 255));
        spec.setCoronaColor(new Color(255, 78, 24, 235));
        spec.setIconColor(new Color(225, 63, 27, 255));
        spec.setCoronaSize(6.1f);
        star.setLightColorOverrideIfStar(new Color(255, 104, 43, 255));
        star.applySpecChanges();
    }

    /**
     * Builds a layered, visual-only accretion flow from vanilla ring textures.
     * Known entities are rebuilt so old saves receive palette/layout revisions
     * without accumulating duplicate bands each time the game is loaded.
     */
    private void rebuildBurnedSystemVisuals(
            StarSystemAPI system,
            PlanetAPI star,
            PlanetAPI planet
    ) {
        for (String id : BURNED_VISUAL_IDS) {
            SectorEntityToken old = system.getEntityById(id);
            if (old != null) {
                system.removeEntity(old);
            }
        }
        removeTaggedEntities(system, DEBRIS_TRAIL_TAG);
        removeTaggedEntities(system, FOB_DEBRIS_TAG);

        addBurnedRing(
                system, star, ACCRETION_INNER_ID, "rings_special0", 1,
                new Color(255, 230, 154, 245), 180f, 720f, 15f,
                false, 0f, 0f
        );
        addBurnedRing(
                system, star, ACCRETION_HOT_ID, "rings_special0", 0,
                new Color(255, 137, 47, 220), 280f, 915f, -23f,
                false, 0f, 0f
        );
        addBurnedRing(
                system, star, ACCRETION_MIDDLE_ID, "rings_dust0", 3,
                new Color(236, 56, 22, 195), 460f, 1260f, 34f,
                true, 610f, 0.07f
        );
        addBurnedRing(
                system, star, ACCRETION_OUTER_ID, "rings_dust0", 1,
                new Color(154, 23, 13, 145), 760f, 1780f, -58f,
                true, 650f, -0.055f
        );
        addBurnedRing(
                system, star, CINDER_RING_ID, "rings_asteroids0", 0,
                new Color(105, 27, 20, 115), CINDER_RING_WIDTH, CINDER_RING_RADIUS, 125f,
                false, 0f, 0f
        );
        addPerimeterNebula(system, star);
        addFobDebrisEnvelope(system, planet);
        addPlanetToBeltStream(system, star);
    }

    /**
     * Spiral arms
     */
    private void addPerimeterNebula(StarSystemAPI system, PlanetAPI star) {
        int width = 160;
        int height = 160;
        BaseTiledTerrain.TileParams params = new BaseTiledTerrain.TileParams(
                buildPerimeterNebulaTiles(width, height),
                width,
                height,
                "terrain",
                "nebula_amber",
                4,
                4,
                null
        );

        SectorEntityToken nebula = system.addTerrain(COSMETIC_NEBULA_TYPE, params);
        nebula.setId(PERIMETER_NEBULA_ID);
        nebula.getLocation().set(star.getLocation());
        nebula.addTag(Tags.NON_CLICKABLE);
        nebula.addTag(Tags.NO_ENTITY_TOOLTIP);
        nebula.setSensorProfile(null);
        nebula.setDiscoverable(null);
    }

    private String buildPerimeterNebulaTiles(int width, int height) {
        StringBuilder result = new StringBuilder(width * height);
        float centerX = (width - 1) * 0.5f;
        float centerY = (height - 1) * 0.5f;
        float innerRadius = 7.2f;
        float armSpacing = (float) (Math.PI * 2d / 4d);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float dx = x - centerX;
                float dy = y - centerY;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                float angle = (float) Math.atan2(dy, dx);

                float radialProgress = clamp01((distance - innerRadius) / 76f);
                float spiralPhase = angle
                        + 1.9f * (float) Math.log(
                        1f + Math.max(0f, distance - innerRadius) / 8.5f
                )
                        + 0.33f;
                float wrappedPhase = positiveModulo(spiralPhase, armSpacing);
                float armDistance = Math.min(wrappedPhase, armSpacing - wrappedPhase);
                float distanceFromArm = armDistance * Math.max(distance, 1f);
                float cloudNoise = cloudNoise(x, y);

                // Four logarithmic arms: narrow tips at the collapsed star,
                // broad cloud banks at the map edge, with coherent raggedness.
                float armPhysicalHalfWidth = interpolate(
                        1f,
                        7f,
                        smoothStep(radialProgress)
                );
                armPhysicalHalfWidth *= 0.72f + 0.75f * cloudNoise;
                float armBody = clamp01(
                        1f - distanceFromArm / Math.max(0.4f, armPhysicalHalfWidth)
                );
                float mainArm = smoothStep(armBody) * (0.78f + 0.42f * cloudNoise);

                // A thinner trailing filament beside each main arm produces
                // natural-looking spurs instead of four mechanically clean bands.
                float spurOffset = 0.16f
                        + 0.05f * (float) Math.sin(distance * 0.11f + angle * 2f);
                float spurPhase = positiveModulo(spiralPhase + spurOffset, armSpacing);
                float spurDistance = Math.min(spurPhase, armSpacing - spurPhase)
                        * Math.max(distance, 1f);
                float spurHalfWidth = interpolate(
                        0.5f,
                        2.3f,
                        smoothStep(radialProgress)
                ) * (0.72f + 0.55f * cloudNoise);
                float spurBody = clamp01(
                        1f - spurDistance / Math.max(0.3f, spurHalfWidth)
                );
                float spur = smoothStep(spurBody)
                        * (0.38f + 0.42f * cloudNoise)
                        * smoothStep((distance - 15f) / 18f);

                // The basin remains empty; tiny detached outer wisps soften the silhouette.
                float innerFade = smoothStep((distance - innerRadius) / 4f);
                float outerHaze = smoothStep((distance - 50f) / 45f) * 0.01f;
                float fillChance = clamp01((mainArm + spur) * innerFade + outerHaze);
                float sample = unitNoise(y * width + x, 431.9f);
                result.append(sample < fillChance ? 'x' : ' ');
            }
        }
        return result.toString();
    }

    /**
     * Three octaves of smooth deterministic noise for large-scale cloud clumping.
     */
    private float cloudNoise(float x, float y) {
        return 0.55f * valueNoise(x * 0.12f, y * 0.12f, (float) 307.7)
                + 0.30f * valueNoise(x * 0.27f, y * 0.27f, (float) 307.7 + 13.7f)
                + 0.15f * valueNoise(x * 0.58f, y * 0.58f, (float) 307.7 + 29.1f);
    }

    private float valueNoise(float x, float y, float seed) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        float blendX = smoothStep(x - x0);
        float blendY = smoothStep(y - y0);

        float lower = interpolate(
                gridNoise(x0, y0, seed),
                gridNoise(x0 + 1, y0, seed),
                blendX
        );
        float upper = interpolate(
                gridNoise(x0, y0 + 1, seed),
                gridNoise(x0 + 1, y0 + 1, seed),
                blendX
        );
        return interpolate(lower, upper, blendY);
    }

    private float gridNoise(int x, int y, float seed) {
        return unitNoise(x * 1987 + y * 9277, seed);
    }

    private float positiveModulo(float value, float modulus) {
        float result = value % modulus;
        return result < 0f ? result + modulus : result;
    }


    /**
     * A close, thick envelope of individually placed rocks around FOB-01. This
     * deliberately uses asteroid tokens rather than debris-field terrain: the
     * rocks have no map interaction boxes, and the broad radial scatter keeps
     * them from reading as another neat planetary ring.
     */
    private void addFobDebrisEnvelope(StarSystemAPI system, PlanetAPI planet) {
        float innerRadius = PLANET_RADIUS + 24f;
        float outerRadius = PLANET_RADIUS + 155f;

        for (int i = 0; i < FOB_DEBRIS_COUNT; i++) {
            // Independent deterministic samples create natural gaps and small clumps.
            float orbitAngle = 360f * unitNoise(i, 104.7f);
            float radialSample = (float) Math.pow(unitNoise(i, 117.3f), 0.72f);
            float orbitRadius = interpolate(innerRadius, outerRadius, radialSample);

            // Break up the circular boundary with two broad, irregular shelves.
            orbitRadius += 15f * (float) Math.sin(Math.toRadians(orbitAngle * 2.3f + 17f));
            orbitRadius += 8f * (float) Math.sin(Math.toRadians(orbitAngle * 5.7f + 91f));
            orbitRadius = Math.max(innerRadius, Math.min(outerRadius, orbitRadius));

            float chunkRadius = 1.8f
                    + 5.4f * (float) Math.pow(unitNoise(i, 131.9f), 1.75f);
            if (unitNoise(i, 149.1f) > 0.84f) {
                chunkRadius = 7.5f + 4f * unitNoise(i, 162.5f);
            }

            SectorEntityToken chunk = system.addAsteroid(chunkRadius);
            chunk.setId(FOB_DEBRIS_ID_PREFIX + i);
            chunk.addTag(FOB_DEBRIS_TAG);
            makeDecorativeAsteroid(chunk);

            // Nearby fragments complete an orbit sooner, giving the envelope
            // subtle internal movement while keeping every rock bound to FOB-01.
            float orbitDays = interpolate(13f, 25f,
                    (orbitRadius - innerRadius) / (outerRadius - innerRadius));
            orbitDays *= 0.92f + 0.16f * unitNoise(i, 176.3f);
            chunk.setCircularOrbit(planet, orbitAngle, orbitRadius, orbitDays);
        }
    }


    /**
     * A loose tidal-transfer stream from FOB-01 to the inner asteroid belt.
     * Its planet-side cloud is broad and feathered, contracts into an irregular
     * neck, then opens and curves under apparent orbital shear. Every chunk has
     * the planet's period, keeping the overall silhouette rigid and permanent.
     */
    private void addPlanetToBeltStream(StarSystemAPI system, PlanetAPI star) {
        float neckProgress = 0.42f;
        // Both mouths flare broadly; only the middle contracts into a tidal neck.
        float sourceHalfWidth = PLANET_RADIUS + 175f;
        float neckHalfWidth = 105f;
        float beltHalfWidth = 335f;
        float streamStartRadius = PLANET_ORBIT_RADIUS - PLANET_RADIUS - 38f;
        float streamEndRadius = CINDER_RING_RADIUS + CINDER_RING_WIDTH * 0.5f - 20f;

        for (int i = 0; i < DEBRIS_TRAIL_COUNT; i++) {
            float evenProgress = (float) i / (float) (DEBRIS_TRAIL_COUNT - 1);
            float progressJitter = (unitNoise(i, 4.1f) - 0.5f) * 0.065f;
            float progress = clamp01(evenProgress + progressJitter);

            float halfWidth;
            if (progress < neckProgress) {
                float neckT = smoothStep(progress / neckProgress);
                halfWidth = interpolate(sourceHalfWidth, neckHalfWidth, neckT);
            } else {
                float beltT = smoothStep((progress - neckProgress) / (1f - neckProgress));
                halfWidth = interpolate(neckHalfWidth, beltHalfWidth, beltT);
            }
            // Uneven boundaries stop the stream from reading as a drawn geometric funnel.
            halfWidth *= 1f
                    + 0.17f * (float) Math.sin(progress * 8.7f + 0.4f)
                    + 0.10f * (float) Math.sin(progress * 21.3f + 1.7f);

            // Most fragments stay in the body; a minority forms sparse, ragged side wisps.
            float centralAcross = (
                    unitNoise(i, 1.3f)
                            + unitNoise(i, 7.7f)
                            + unitNoise(i, 15.1f)
                            - 1.5f
            ) / 1.5f;
            float edgeRoll = unitNoise(i, 22.9f);
            float across;
            if (edgeRoll < 0.27f) {
                float side = unitNoise(i, 31.7f) < 0.5f ? -1f : 1f;
                across = side * (0.67f + 0.48f * unitNoise(i, 44.3f));
            } else {
                across = centralAcross * 0.94f;
            }
            // A wandering centre line and sparse over-width wisps make the two
            // flared mouths look torn and accidental instead of mirrored.
            float centreDrift = halfWidth * (
                    0.10f * (float) Math.sin(progress * 10.1f + 1.2f)
                            + 0.055f * (float) Math.sin(progress * 25.7f + 4.3f)
            );
            float transverseOffset = across * halfWidth + centreDrift;

            float sourceDepth = 58f * unitNoise(i, 53.9f)
                    + 0.1f * Math.abs(across * sourceHalfWidth);
            float sourceRadius = streamStartRadius - sourceDepth;
            float radialDistance = interpolate(sourceRadius, streamEndRadius, progress)
                    + (unitNoise(i, 61.1f) - 0.5f) * 34f;

            float bend = -3.1f * smoothStep(progress)
                    + 0.55f * (float) Math.sin(progress * 7.1f + 0.5f)
                    + 0.22f * (float) Math.sin(progress * 19.7f + 2.1f);
            float transverseAngle = (float) Math.toDegrees(
                    Math.atan2(transverseOffset, radialDistance)
            );
            float orbitAngle = PLANET_ORBIT_ANGLE + bend + transverseAngle;
            float orbitRadius = (float) Math.sqrt(
                    radialDistance * radialDistance + transverseOffset * transverseOffset
            );

            // Mostly gravel, with a few belt-scale rocks and no old giant boulders.
            float chunkRadius = 2.2f
                    + 5.2f * (float) Math.pow(unitNoise(i, 73.3f), 2.2f);
            if (unitNoise(i, 88.7f) > 0.88f) {
                chunkRadius = 8.5f + 5.5f * unitNoise(i, 96.1f);
            }

            SectorEntityToken chunk = system.addAsteroid(chunkRadius);
            chunk.setId(DEBRIS_TRAIL_ID_PREFIX + i);
            chunk.addTag(DEBRIS_TRAIL_TAG);
            makeDecorativeAsteroid(chunk);
            chunk.setCircularOrbit(
                    star,
                    orbitAngle,
                    orbitRadius,
                    PLANET_ORBIT_DAYS
            );
        }
    }

    private void makeDecorativeAsteroid(SectorEntityToken asteroid) {
        asteroid.addTag(Tags.NON_CLICKABLE);
        asteroid.addTag(Tags.NO_ENTITY_TOOLTIP);
        asteroid.setSensorProfile(null);
        asteroid.setDiscoverable(null);
    }

    private float smoothStep(float value) {
        float clamped = clamp01(value);
        return clamped * clamped * (3f - 2f * clamped);
    }

    private float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private float unitNoise(int index, float seed) {
        double value = Math.sin((index + 1) * 12.9898d + seed * 78.233d) * 43758.5453d;
        return (float) (value - Math.floor(value));
    }

    private float interpolate(float from, float to, float progress) {
        return from + (to - from) * progress;
    }

    private void removeTaggedEntities(StarSystemAPI system, String tag) {
        List<SectorEntityToken> oldEntities =
                new ArrayList<SectorEntityToken>(system.getEntitiesWithTag(tag));
        for (SectorEntityToken entity : oldEntities) {
            system.removeEntity(entity);
        }
    }



    private void addBurnedRing(
            StarSystemAPI system,
            SectorEntityToken focus,
            String id,
            String texture,
            int bandIndex,
            Color color,
            float bandWidth,
            float middleRadius,
            float orbitDays,
            boolean spiral,
            float minSpiralRadius,
            float spiralFactor
    ) {
        RingBandAPI band = system.addRingBand(
                focus,
                "misc",
                texture,
                256f,
                bandIndex,
                color,
                bandWidth,
                middleRadius,
                orbitDays
        );
        band.setId(id);
        band.setSpiral(spiral);
        if (spiral) {
            band.setMinSpiralRadius(minSpiralRadius);
            band.setSpiralFactor(spiralFactor);
        }
    }

    private void replacePlanetMagneticFields(StarSystemAPI system, PlanetAPI planet) {
        List<CampaignTerrainAPI> terrainCopy = new ArrayList<CampaignTerrainAPI>(system.getTerrainCopy());
        for (CampaignTerrainAPI terrain : terrainCopy) {
            if (Terrain.MAGNETIC_FIELD.equals(terrain.getType())) {
                system.removeEntity(terrain);
            }
        }

        MagneticFieldTerrainPlugin.MagneticFieldParams fieldParams =
                new MagneticFieldTerrainPlugin.MagneticFieldParams(
                        150f,
                        500f,
                        planet,
                        350f,
                        650f,
                        new Color(84, 13, 8, 85),
                        1f,
                        new Color(128, 18, 10, 110),
                        new Color(174, 28, 11, 135),
                        new Color(220, 48, 15, 165),
                        new Color(255, 78, 19, 205),
                        new Color(255, 119, 31, 225),
                        new Color(194, 34, 12, 230),
                        new Color(255, 191, 82, 245)
                );
        SectorEntityToken magneticField = system.addTerrain(Terrain.MAGNETIC_FIELD, fieldParams);
        magneticField.setCircularOrbit(planet, 0f, 0f, 75f);
    }

    private void replaceEventHorizon(StarSystemAPI system, PlanetAPI star) {
        List<CampaignTerrainAPI> terrainCopy = new ArrayList<CampaignTerrainAPI>(system.getTerrainCopy());
        for (CampaignTerrainAPI terrain : terrainCopy) {
            if (Terrain.EVENT_HORIZON.equals(terrain.getType())) {
                system.removeEntity(terrain);
            }
        }

        system.addTerrain(
                Terrain.EVENT_HORIZON,
                new EventHorizonPlugin.CoronaParams(
                        1200f,
                        600f,
                        star,
                        -5f,
                        0f,
                        1f
                )
        );
    }

    private static final class GateWreckSpec {
        private final String idSuffix;
        private final String variantId;
        private final float angleOffset;
        private final float radiusOffset;

        private GateWreckSpec(
                String idSuffix,
                String variantId,
                float angleOffset,
                float radiusOffset
        ) {
            this.idSuffix = idSuffix;
            this.variantId = variantId;
            this.angleOffset = angleOffset;
            this.radiusOffset = radiusOffset;
        }
    }
}
