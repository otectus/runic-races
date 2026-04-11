package com.otectus.runic_races.client.render;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Optional;

/**
 * Maps each winged race to its wing rendering configuration.
 */
@OnlyIn(Dist.CLIENT)
public enum WingType {
    SPRITE_WINGS(
            "textures/entity/pixie_wings.png",
            0.5f,
            -15f, -70f, -110f,
            8, true, false,
            1.5f, 25f, 0.3f
    ),
    WYVERN_WINGS(
            "textures/entity/wyvern_wings.png",
            1.0f,
            -15f, -75f, -120f,
            10, false, false,
            1.0f, 12f, 0.5f
    ),
    DRAKE_WINGS(
            "textures/entity/drake_wings.png",
            1.4f,
            -15f, -80f, -130f,
            14, false, false,
            0.7f, 8f, 0.7f
    ),
    DRAGONBORN_WINGS(
            "textures/entity/dragonborn_wings.png",
            0.65f,
            -10f, -40f, -60f,
            8, false, true,
            0.5f, 5f, 0.2f
    );

    private final String texturePath;
    private final float scale;
    private final float foldedZDeg;
    private final float spreadZDeg;
    private final float downbeatZDeg;
    private final int flapDurationTicks;
    private final boolean translucent;
    private final boolean cosmeticOnly;
    private final float bankingSensitivity;
    private final float hoverFlutterAmplitudeDeg;
    private final float windBuffetScale;

    WingType(String texturePath, float scale,
             float foldedZDeg, float spreadZDeg, float downbeatZDeg,
             int flapDurationTicks, boolean translucent, boolean cosmeticOnly,
             float bankingSensitivity, float hoverFlutterAmplitudeDeg, float windBuffetScale) {
        this.texturePath = texturePath;
        this.scale = scale;
        this.foldedZDeg = foldedZDeg;
        this.spreadZDeg = spreadZDeg;
        this.downbeatZDeg = downbeatZDeg;
        this.flapDurationTicks = flapDurationTicks;
        this.translucent = translucent;
        this.cosmeticOnly = cosmeticOnly;
        this.bankingSensitivity = bankingSensitivity;
        this.hoverFlutterAmplitudeDeg = hoverFlutterAmplitudeDeg;
        this.windBuffetScale = windBuffetScale;
    }

    public String getTexturePath() { return texturePath; }
    public float getScale() { return scale; }
    public float getFoldedZDeg() { return foldedZDeg; }
    public float getSpreadZDeg() { return spreadZDeg; }
    public float getDownbeatZDeg() { return downbeatZDeg; }
    public int getFlapDurationTicks() { return flapDurationTicks; }
    public boolean isTranslucent() { return translucent; }
    public boolean isCosmeticOnly() { return cosmeticOnly; }
    public float getBankingSensitivity() { return bankingSensitivity; }
    public float getHoverFlutterAmplitudeDeg() { return hoverFlutterAmplitudeDeg; }
    public float getWindBuffetScale() { return windBuffetScale; }

    /**
     * Returns the wing type for the given race name, or empty for non-winged races.
     */
    public static Optional<WingType> forRaceName(String raceName) {
        return Optional.ofNullable(switch (raceName) {
            case "sprite" -> SPRITE_WINGS;
            case "wyvern_blooded" -> WYVERN_WINGS;
            case "elder_drake" -> DRAKE_WINGS;
            case "dragonborn" -> DRAGONBORN_WINGS;
            default -> null;
        });
    }
}
