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
    public static final ForgeConfigSpec.BooleanValue RACIAL_PVP_CONTROL;

    // Server-authored VFX
    public static final ForgeConfigSpec.DoubleValue BREATH_PARTICLE_DENSITY;
    public static final ForgeConfigSpec.DoubleValue SIGNATURE_PARTICLE_DENSITY;

    // Network transport (compatibility fallbacks; defaults are the optimized paths)
    public static final ForgeConfigSpec.BooleanValue COOLDOWN_DELTA_SYNC;
    public static final ForgeConfigSpec.BooleanValue BATCHED_PARTICLES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("combat");
        RACIAL_PVP_CONTROL = builder.comment("Allow new racial crowd control against legal PvP targets. Half duration, with a 3-second reapplication guard; bosses resist control regardless.")
                .define("racialPvpControl", false);
        builder.pop();

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
                .comment("Density multiplier for draconic breath torrents and impact particles (0.0 = none, 1.0 = default, 2.0 = double).",
                        "Clients animate a 16-tick cast snapshot, bounded to 12 streams / 192 new particles per tick.",
                        "Below 0.5 secondary accents are skipped. Client particle settings and heavyEffects also reduce detail.")
                .defineInRange("breathParticleDensity", 1.0, 0.0, 2.0);
        SIGNATURE_PARTICLE_DENSITY = builder
                .comment("Density multiplier for signature-ability particles broadcast by the server (0.0 = none, 1.0 = default, 2.0 = double).",
                        "Shaped emissions (rings, domes, spokes, cones) keep a small floor so they stay readable at low values.")
                .defineInRange("signatureParticleDensity", 1.0, 0.0, 2.0);
        builder.pop();

        builder.comment("Network transport. Both options only change how the same state and effects are delivered;",
                "turn one off to fall back to the pre-1.7.2 packets if a modpack interaction is suspected.").push("network");
        COOLDOWN_DELTA_SYNC = builder
                .comment("Send each racial cooldown decay step as a small value update to the owner and the players",
                        "tracking them, instead of re-sending the owner's entire Origins power container every step.",
                        "Activation, login, respawn, dimension change and new trackers still receive the full container.")
                .define("cooldownDeltaSync", true);
        BATCHED_PARTICLES = builder
                .comment("Deliver each shaped racial particle emission (rings, lines, domes, cones) as one packet per",
                        "nearby player instead of one vanilla particle packet per particle. Positions, motion, recipients",
                        "and the 32-block visibility range are unchanged.")
                .define("batchedParticles", true);
        builder.pop();

        SPEC = builder.build();
    }
}
