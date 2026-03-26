package com.otectus.runic_races.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class RRCommonConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue DEBUG_LOGGING;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> DISABLED_RACES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("General settings").push("general");
        DEBUG_LOGGING = builder
                .comment("Enable debug logging for race power application")
                .define("debugLogging", false);
        builder.pop();

        builder.comment("Race availability").push("races");
        DISABLED_RACES = builder
                .comment("List of race IDs to disable (e.g. [\"runic_races:vampire\", \"runic_races:elder_drake\"])")
                .defineListAllowEmpty(List.of("disabledRaces"), List::of, o -> o instanceof String);
        builder.pop();

        SPEC = builder.build();
    }
}
