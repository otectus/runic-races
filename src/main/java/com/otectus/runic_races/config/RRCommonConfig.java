package com.otectus.runic_races.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class RRCommonConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue DEBUG_LOGGING;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("General settings").push("general");
        DEBUG_LOGGING = builder
                .comment("Enable debug logging for race power application")
                .define("debugLogging", false);
        builder.pop();

        SPEC = builder.build();
    }
}
