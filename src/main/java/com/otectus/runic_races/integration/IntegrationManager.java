package com.otectus.runic_races.integration;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.config.RRServerConfig;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages optional mod integrations using reflection-based loading.
 * Integration classes that reference mod-specific types are only loaded
 * when their target mod is present, preventing NoClassDefFoundError.
 */
public class IntegrationManager {

    private static final List<ModIntegration> loadedIntegrations = new ArrayList<>();

    public static void init() {
        RunicRacesMod.LOGGER.info("[RunicRaces] Initializing integration manager...");

        tryLoad("ars_nouveau", "Ars Nouveau",
                "com.otectus.runic_races.integration.ars.ArsNouveauIntegration",
                () -> RRServerConfig.ARS_NOUVEAU_INTEGRATION.get());

        tryLoad("irons_spellbooks", "Iron's Spellbooks",
                "com.otectus.runic_races.integration.irons.IronsSpellsIntegration",
                () -> RRServerConfig.IRONS_SPELLS_INTEGRATION.get());

        tryLoad("curios", "Curios",
                "com.otectus.runic_races.integration.curios.CuriosIntegration",
                () -> RRServerConfig.CURIOS_INTEGRATION.get());

        tryLoad("apotheosis", "Apotheosis",
                "com.otectus.runic_races.integration.apotheosis.ApotheosisIntegration",
                () -> RRServerConfig.APOTHEOSIS_INTEGRATION.get());

        tryLoad("runicskills", "Runic Skills",
                "com.otectus.runic_races.integration.runicskills.RunicSkillsIntegration",
                () -> RRServerConfig.RUNIC_SKILLS_INTEGRATION.get());

        tryLoad("runic_gods", "Runic Gods",
                "com.otectus.runic_races.integration.spellsngods.SpellsNGodsIntegration",
                () -> RRServerConfig.RUNIC_GODS_INTEGRATION.get());

        RunicRacesMod.LOGGER.info("[RunicRaces] {} integrations loaded", loadedIntegrations.size());
    }

    private static void tryLoad(String modId, String modName, String className, java.util.function.BooleanSupplier configEnabled) {
        if (!ModList.get().isLoaded(modId)) {
            RunicRacesMod.LOGGER.debug("[RunicRaces] {} not present, skipping integration", modName);
            return;
        }
        if (!configEnabled.getAsBoolean()) {
            RunicRacesMod.LOGGER.info("[RunicRaces] {} integration disabled by config", modName);
            return;
        }
        try {
            Class<?> clazz = Class.forName(className);
            ModIntegration integration = (ModIntegration) clazz.getDeclaredConstructor().newInstance();
            integration.init();
            loadedIntegrations.add(integration);
            RunicRacesMod.LOGGER.info("[RunicRaces] {} integration loaded successfully", modName);
        } catch (Exception e) {
            RunicRacesMod.LOGGER.error("[RunicRaces] Failed to load {} integration: {}", modName, e.getMessage());
        }
    }

    public static List<ModIntegration> getLoadedIntegrations() {
        return loadedIntegrations;
    }
}
