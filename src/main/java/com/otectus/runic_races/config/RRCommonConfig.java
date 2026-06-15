package com.otectus.runic_races.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class RRCommonConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue DEBUG_LOGGING;
    public static final ForgeConfigSpec.BooleanValue DISABLE_DEFAULT_ORIGIN_LAYER;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("General settings").push("general");
        DEBUG_LOGGING = builder
                .comment("Enable debug logging for race power application")
                .define("debugLogging", false);
        builder.pop();

        builder.comment("Origins layer compatibility").push("origins");
        DISABLE_DEFAULT_ORIGIN_LAYER = builder
                .comment("When true (default), the vanilla origins:origin layer is force-disabled so players only see the",
                        "Runic Races Family and Race screens. Origin add-ons that register into the default layer will be hidden.",
                        "Set false to let other Origins mods coexist — their origins reappear as the standard Origins screen",
                        "alongside Family/Race. (Built-in datapack toggle; applies to newly created worlds — existing worlds can be",
                        "switched with /datapack enable|disable or the runic_races_coexistence example datapack.)")
                .define("disableDefaultOriginLayer", true);
        builder.pop();

        SPEC = builder.build();
    }
}
