package com.otectus.runic_races.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class RRServerConfig {
    public static final ForgeConfigSpec SPEC;

    // Integration toggles
    public static final ForgeConfigSpec.BooleanValue ARS_NOUVEAU_INTEGRATION;
    public static final ForgeConfigSpec.BooleanValue IRONS_SPELLS_INTEGRATION;
    public static final ForgeConfigSpec.BooleanValue CURIOS_INTEGRATION;
    public static final ForgeConfigSpec.BooleanValue APOTHEOSIS_INTEGRATION;
    public static final ForgeConfigSpec.BooleanValue RUNIC_SKILLS_INTEGRATION;
    public static final ForgeConfigSpec.BooleanValue RUNIC_GODS_INTEGRATION;
    public static final ForgeConfigSpec.BooleanValue PEHKUI_INTEGRATION;
    public static final ForgeConfigSpec.BooleanValue FEATHERS_INTEGRATION;

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
        RUNIC_SKILLS_INTEGRATION = builder
                .comment("Enable Runic Skills integration (starting skill bonuses, perk affinity)")
                .define("runicSkills", true);
        RUNIC_GODS_INTEGRATION = builder
                .comment("Enable Runic Gods integration (divine affinity/antipathy, worship restrictions)")
                .define("runicGods", true);
        PEHKUI_INTEGRATION = builder
                .comment("Enable Pehkui integration (racial height scaling)")
                .define("pehkui", true);
        FEATHERS_INTEGRATION = builder
                .comment("Enable Feather's Mod integration (racial stamina pools for physical powers)")
                .define("feathers", true);
        builder.pop();

        SPEC = builder.build();
    }
}
