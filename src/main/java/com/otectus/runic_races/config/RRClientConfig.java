package com.otectus.runic_races.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Client-only configuration for the Runic Races HUD and presentation layer.
 * Registered via {@code ModConfig.Type.CLIENT} — exists on physical clients only.
 */
public class RRClientConfig {

    public enum HudAnchor {
        ABOVE_HOTBAR,
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue HUD_ENABLED;
    public static final ForgeConfigSpec.EnumValue<HudAnchor> HUD_ANCHOR;
    public static final ForgeConfigSpec.IntValue HUD_OFFSET_X;
    public static final ForgeConfigSpec.IntValue HUD_OFFSET_Y;
    public static final ForgeConfigSpec.DoubleValue HUD_SCALE;
    public static final ForgeConfigSpec.DoubleValue HUD_OPACITY;
    public static final ForgeConfigSpec.BooleanValue HUD_MINIMAL_MODE;
    public static final ForgeConfigSpec.BooleanValue HUD_SHOW_NAMES;
    public static final ForgeConfigSpec.BooleanValue HUD_READY_GLOW;
    public static final ForgeConfigSpec.BooleanValue AMBIENT_STATE_PARTICLES;
    public static final ForgeConfigSpec.BooleanValue SCREEN_CUES_ENABLED;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Racial cooldown HUD overlay").push("hud");

        HUD_ENABLED = builder
                .comment("Show the racial cooldown/state HUD at all.")
                .define("enabled", true);

        HUD_ANCHOR = builder
                .comment("Screen anchor the HUD attaches to.",
                        "Values: ABOVE_HOTBAR, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT")
                .defineEnum("anchor", HudAnchor.ABOVE_HOTBAR);

        HUD_OFFSET_X = builder
                .comment("Horizontal offset from the anchor, in pixels.")
                .defineInRange("offsetX", 0, -400, 400);

        HUD_OFFSET_Y = builder
                .comment("Vertical offset from the anchor, in pixels.")
                .defineInRange("offsetY", 0, -400, 400);

        HUD_SCALE = builder
                .comment("Uniform scale multiplier for the HUD (1.0 = default).")
                .defineInRange("scale", 1.0, 0.5, 3.0);

        HUD_OPACITY = builder
                .comment("Opacity of the HUD (0.0 = invisible, 1.0 = solid).")
                .defineInRange("opacity", 1.0, 0.0, 1.0);

        HUD_MINIMAL_MODE = builder
                .comment("Minimal mode: hide key-hint microtext and ability names; icons only.")
                .define("minimalMode", false);

        HUD_SHOW_NAMES = builder
                .comment("Show ability names under icons (learning mode). Ignored when minimalMode is true.")
                .define("showNames", false);

        HUD_READY_GLOW = builder
                .comment("Pulse icon alpha when an ability is ready and draw a spark on the cooldown→ready transition.")
                .define("readyGlow", true);

        builder.pop();

        builder.comment("Ambient state effects").push("ambient");
        AMBIENT_STATE_PARTICLES = builder
                .comment("Spawn subtle ambient particles when passive states are active (home biome, night empowered, etc.).")
                .define("stateParticles", true);
        SCREEN_CUES_ENABLED = builder
                .comment("Render screen-overlay cues (vignettes, freeze-flash) for signature moments.")
                .define("screenCues", true);
        builder.pop();

        SPEC = builder.build();
    }

    private RRClientConfig() {}
}
