package com.otectus.runic_races.client.render;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Optional;

/**
 * Maps each winged race to its wing rendering configuration.
 *
 * Per-race textures are emitted by {@code tools/generate_wings.py} from the three
 * hand-made bases (pixie/wyvern/drake); {@code drake_wings.png} is kept as the
 * generator source even though no type references it directly.
 */
@OnlyIn(Dist.CLIENT)
public enum WingType {
    SPRITE_WINGS(
            "textures/entity/pixie_wings.png",
            0.5f,
            -15f, -70f, -110f,
            8, true, false, true,
            1.5f, 25f, 0.3f,
            WingModel.Silhouette.GOSSAMER, 0.9f, 1.15f
    ),
    FAERIE_WINGS(
            "textures/entity/faerie_wings.png",
            0.6f,
            -15f, -72f, -112f,
            8, true, false, true,
            1.4f, 22f, 0.3f,
            WingModel.Silhouette.GOSSAMER, 0.9f, 1.15f
    ),
    AVIAN_WINGS(
            "textures/entity/avian_wings.png",
            0.95f,
            -15f, -78f, -125f,
            9, false, false, false,
            1.1f, 14f, 0.45f,
            WingModel.Silhouette.FEATHERED, 0.55f, 1.25f
    ),
    WYVERN_WINGS(
            "textures/entity/wyvern_wings.png",
            1.0f,
            -15f, -75f, -120f,
            10, false, false, false,
            1.0f, 12f, 0.5f,
            WingModel.Silhouette.MEMBRANE, 0.5f, 1.3f
    ),
    FIRE_DRAKE_WINGS(
            "textures/entity/fire_drake_wings.png",
            1.4f,
            -15f, -80f, -130f,
            14, false, false, false,
            0.7f, 8f, 0.7f,
            WingModel.Silhouette.MEMBRANE, 0.45f, 1.35f
    ),
    ICE_DRAKE_WINGS(
            "textures/entity/ice_drake_wings.png",
            1.35f,
            -15f, -80f, -130f,
            14, false, false, false,
            0.7f, 8f, 0.7f,
            WingModel.Silhouette.MEMBRANE, 0.45f, 1.35f
    ),
    TERRA_DRAKE_WINGS(
            "textures/entity/terra_drake_wings.png",
            1.5f,
            -15f, -82f, -132f,
            16, false, false, false,
            0.6f, 6f, 0.8f,
            WingModel.Silhouette.MEMBRANE, 0.4f, 1.3f
    ),
    VOLT_DRAKE_WINGS(
            "textures/entity/volt_drake_wings.png",
            1.3f,
            -15f, -78f, -128f,
            12, false, false, false,
            0.8f, 10f, 0.6f,
            WingModel.Silhouette.MEMBRANE, 0.5f, 1.35f
    );

    private final String texturePath;
    private final float scale;
    private final float foldedZDeg;
    private final float spreadZDeg;
    private final float downbeatZDeg;
    private final int flapDurationTicks;
    private final boolean translucent;
    private final boolean cosmeticOnly;
    private final boolean fullBright;
    private final float bankingSensitivity;
    private final float hoverFlutterAmplitudeDeg;
    private final float windBuffetScale;
    private final WingModel.Silhouette silhouette;
    private final float tipLagFactor;
    private final float tipOvershoot;

    WingType(String texturePath, float scale,
             float foldedZDeg, float spreadZDeg, float downbeatZDeg,
             int flapDurationTicks, boolean translucent, boolean cosmeticOnly, boolean fullBright,
             float bankingSensitivity, float hoverFlutterAmplitudeDeg, float windBuffetScale,
             WingModel.Silhouette silhouette, float tipLagFactor, float tipOvershoot) {
        this.texturePath = texturePath;
        this.scale = scale;
        this.foldedZDeg = foldedZDeg;
        this.spreadZDeg = spreadZDeg;
        this.downbeatZDeg = downbeatZDeg;
        this.flapDurationTicks = flapDurationTicks;
        this.translucent = translucent;
        this.cosmeticOnly = cosmeticOnly;
        this.fullBright = fullBright;
        this.bankingSensitivity = bankingSensitivity;
        this.hoverFlutterAmplitudeDeg = hoverFlutterAmplitudeDeg;
        this.windBuffetScale = windBuffetScale;
        this.silhouette = silhouette;
        this.tipLagFactor = tipLagFactor;
        this.tipOvershoot = tipOvershoot;
    }

    public String getTexturePath() { return texturePath; }
    public float getScale() { return scale; }
    public float getFoldedZDeg() { return foldedZDeg; }
    public float getSpreadZDeg() { return spreadZDeg; }
    public float getDownbeatZDeg() { return downbeatZDeg; }
    public int getFlapDurationTicks() { return flapDurationTicks; }
    public boolean isTranslucent() { return translucent; }
    public boolean isCosmeticOnly() { return cosmeticOnly; }
    /** Emissive rendering — gossamer fae wings glow softly in the dark. */
    public boolean isFullBright() { return fullBright; }
    public float getBankingSensitivity() { return bankingSensitivity; }
    public float getHoverFlutterAmplitudeDeg() { return hoverFlutterAmplitudeDeg; }
    public float getWindBuffetScale() { return windBuffetScale; }
    /** Which articulated part group this race renders. */
    public WingModel.Silhouette getSilhouette() { return silhouette; }
    /** How quickly the outer tip chases the arm (fraction of the base lerp speed). */
    public float getTipLagFactor() { return tipLagFactor; }
    /** How far past the arm's angle the tip settles — reads as wing curvature and whip. */
    public float getTipOvershoot() { return tipOvershoot; }

    /**
     * Returns the wing type for the given race name, or empty for non-winged races.
     */
    public static Optional<WingType> forRaceName(String raceName) {
        return Optional.ofNullable(switch (raceName) {
            case "sprite" -> SPRITE_WINGS;
            case "faerie" -> FAERIE_WINGS;
            case "avian" -> AVIAN_WINGS;
            case "wind_wyrm" -> WYVERN_WINGS;
            case "fire_drake" -> FIRE_DRAKE_WINGS;
            case "ice_drake" -> ICE_DRAKE_WINGS;
            case "terra_drake" -> TERRA_DRAKE_WINGS;
            case "volt_drake" -> VOLT_DRAKE_WINGS;
            default -> null;
        });
    }
}
