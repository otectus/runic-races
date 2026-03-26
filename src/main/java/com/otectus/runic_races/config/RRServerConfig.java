package com.otectus.runic_races.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class RRServerConfig {
    public static final ForgeConfigSpec SPEC;

    // Balance multipliers
    public static final ForgeConfigSpec.DoubleValue COOLDOWN_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue HEALING_MULTIPLIER;

    // Integration toggles
    public static final ForgeConfigSpec.BooleanValue ARS_NOUVEAU_INTEGRATION;
    public static final ForgeConfigSpec.BooleanValue IRONS_SPELLS_INTEGRATION;
    public static final ForgeConfigSpec.BooleanValue CURIOS_INTEGRATION;
    public static final ForgeConfigSpec.BooleanValue APOTHEOSIS_INTEGRATION;
    public static final ForgeConfigSpec.BooleanValue RUNIC_SKILLS_INTEGRATION;
    public static final ForgeConfigSpec.BooleanValue RUNIC_GODS_INTEGRATION;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Balance multipliers — applied globally to all racial powers").push("balance");
        COOLDOWN_MULTIPLIER = builder
                .comment("Multiplier for all racial ability cooldowns (1.0 = default, 2.0 = double cooldowns)")
                .defineInRange("cooldownMultiplier", 1.0, 0.1, 10.0);
        DAMAGE_MULTIPLIER = builder
                .comment("Multiplier for all racial damage effects (1.0 = default)")
                .defineInRange("damageMultiplier", 1.0, 0.1, 5.0);
        HEALING_MULTIPLIER = builder
                .comment("Multiplier for all racial healing effects (1.0 = default)")
                .defineInRange("healingMultiplier", 1.0, 0.1, 5.0);
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
        RUNIC_SKILLS_INTEGRATION = builder
                .comment("Enable Runic Skills integration (starting skill bonuses, perk affinity)")
                .define("runicSkills", true);
        RUNIC_GODS_INTEGRATION = builder
                .comment("Enable Runic Gods integration (divine affinity/antipathy, worship restrictions)")
                .define("runicGods", true);
        builder.pop();

        SPEC = builder.build();
    }
}
