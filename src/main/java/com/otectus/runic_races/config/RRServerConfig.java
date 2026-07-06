package com.otectus.runic_races.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class RRServerConfig {
    public static final ForgeConfigSpec SPEC;

    // Integration toggles
    public static final ForgeConfigSpec.BooleanValue ARS_NOUVEAU_INTEGRATION;
    public static final ForgeConfigSpec.BooleanValue IRONS_SPELLS_INTEGRATION;
    public static final ForgeConfigSpec.BooleanValue CURIOS_INTEGRATION;
    public static final ForgeConfigSpec.BooleanValue APOTHEOSIS_INTEGRATION;
    public static final ForgeConfigSpec.BooleanValue PEHKUI_INTEGRATION;
    public static final ForgeConfigSpec.BooleanValue FEATHERS_INTEGRATION;

    // Resource gating
    public static final ForgeConfigSpec.BooleanValue FAIL_CLOSED_WHEN_RESOURCE_MOD_MISSING;

    // Race-state notifications
    public static final ForgeConfigSpec.BooleanValue NOTIFICATIONS_ENABLED;
    public static final ForgeConfigSpec.BooleanValue NOTIFICATIONS_CHAT_MIRROR;
    public static final ForgeConfigSpec.BooleanValue NOTIFICATIONS_LEARNING_MODE;

    // Flight
    public static final ForgeConfigSpec.BooleanValue FLAP_STAMINA_COST;

    // Server-authored VFX
    public static final ForgeConfigSpec.DoubleValue BREATH_PARTICLE_DENSITY;
    public static final ForgeConfigSpec.DoubleValue SIGNATURE_PARTICLE_DENSITY;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Integration toggles — disable specific mod integrations").push("integration");
        ARS_NOUVEAU_INTEGRATION = builder
                .comment("Enable Ars Nouveau integration (Source cost modifiers, glyph affinity)")
                .define("arsNouveau", true);
        IRONS_SPELLS_INTEGRATION = builder
                .comment("Enable Iron's Spellbooks integration (school affinity, cooldown modifiers)")
                .define("ironsSpellbooks", true);
        CURIOS_INTEGRATION = builder
                .comment("Enable Curios integration (slot grants, affinity bonuses)")
                .define("curios", true);
        APOTHEOSIS_INTEGRATION = builder
                .comment("Enable Apotheosis integration (Forge Blessing, Appraise, loot modifiers)")
                .define("apotheosis", true);
        PEHKUI_INTEGRATION = builder
                .comment("Enable Pehkui integration (racial height scaling)")
                .define("pehkui", true);
        FEATHERS_INTEGRATION = builder
                .comment("Enable Feather's Mod integration (racial stamina pools for physical powers)")
                .define("feathers", true);
        builder.pop();

        builder.comment("Resource gating — controls behavior when optional resource mods are absent").push("resourceGating");
        FAIL_CLOSED_WHEN_RESOURCE_MOD_MISSING = builder
                .comment("Governs raw runic_races:has_mana / has_stamina conditions (and reflection-failure fallback) when the backing mod is absent:",
                        "true (default) = those conditions read 0, disabling anything gated on them; false = they read infinite, making gates free (in-pack mode).",
                        "The powers shipped with this mod wrap their gates with runic_races:resource_available so they degrade gracefully either way —",
                        "datapack authors should copy that pattern (see powers/magi/arcane_overflow.json).",
                        "A startup warning is logged either way when a known gating mod is missing.")
                .define("failClosedWhenResourceModMissing", true);
        builder.pop();

        builder.comment("Race-state notifications — start/stop action-bar banners for harmful race conditions").push("notifications");
        NOTIFICATIONS_ENABLED = builder
                .comment("Master switch for race-state start/stop banners (zombie sun, tight space, fire, etc.).",
                        "When false, the HUD state runes still render but no action-bar banners are sent.")
                .define("enabled", true);
        NOTIFICATIONS_CHAT_MIRROR = builder
                .comment("Also mirror each notification into the chat log (accessibility — easier to read mid-combat).")
                .define("chatMirror", false);
        NOTIFICATIONS_LEARNING_MODE = builder
                .comment("Also banner the informational states (home/hostile biome, night empowered, adaptation) that are normally rune-only.",
                        "Useful while learning a race; noisy for veterans.")
                .define("learningMode", false);
        builder.pop();

        builder.comment("Flight tuning").push("flight");
        FLAP_STAMINA_COST = builder
                .comment("When Feather's Mod is present, each wing flap costs feathers (1 for small wings, 2 for the",
                        "Wind Wyrm). Exhausted wings refuse to flap with a red banner. No effect without Feather's.")
                .define("flapStaminaCost", true);
        builder.pop();

        builder.comment("Server-authored VFX tuning").push("vfx");
        BREATH_PARTICLE_DENSITY = builder
                .comment("Density multiplier for draconic breath-weapon particles broadcast by the server (0.0 = none, 1.0 = default, 2.0 = double).",
                        "Below 0.5 the secondary accent particles are skipped entirely.")
                .defineInRange("breathParticleDensity", 1.0, 0.0, 2.0);
        SIGNATURE_PARTICLE_DENSITY = builder
                .comment("Density multiplier for signature-ability particles broadcast by the server (0.0 = none, 1.0 = default, 2.0 = double).",
                        "Shaped emissions (rings, domes, spokes, cones) keep a small floor so they stay readable at low values.")
                .defineInRange("signatureParticleDensity", 1.0, 0.0, 2.0);
        builder.pop();

        SPEC = builder.build();
    }
}
