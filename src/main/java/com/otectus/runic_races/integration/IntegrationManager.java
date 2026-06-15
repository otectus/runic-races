package com.otectus.runic_races.integration;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.config.RRServerConfig;
import com.otectus.runic_races.util.RaceHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages optional mod integrations using reflection-based loading.
 * Integration classes that reference mod-specific types are only loaded
 * when their target mod is present, preventing NoClassDefFoundError.
 */
public class IntegrationManager {

    private static final String LAST_SYNCED_RACE = "runic_races:last_synced_race";
    private static final List<ModIntegration> loadedIntegrations = new ArrayList<>();
    private static boolean syncHandlerRegistered = false;
    private static boolean initialized = false;

    public static void init() {
        if (initialized) {
            RunicRacesMod.LOGGER.warn("[RunicRaces] IntegrationManager.init() called more than once, ignoring");
            return;
        }
        initialized = true;
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

        tryLoad("pehkui", "Pehkui",
                "com.otectus.runic_races.integration.pehkui.PehkuiIntegration",
                () -> RRServerConfig.PEHKUI_INTEGRATION.get());

        tryLoad("feathers", "Feather's Mod",
                "com.otectus.runic_races.integration.feathers.FeathersIntegration",
                () -> RRServerConfig.FEATHERS_INTEGRATION.get());

        RunicRacesMod.LOGGER.info("[RunicRaces] {} integrations loaded", loadedIntegrations.size());

        logMissingGatingMods();

        if (!syncHandlerRegistered) {
            MinecraftForge.EVENT_BUS.register(new SyncHandler());
            syncHandlerRegistered = true;
        }
    }

    /**
     * Warn the pack author when a known resource-gating mod (Iron's Spellbooks, Feather's)
     * is absent so they notice degraded-but-silent behavior. Message severity is chosen
     * based on the {@code failClosed} posture: WARN when fail-open (surprise power swings),
     * INFO when fail-closed (predictable — just informational).
     */
    private static void logMissingGatingMods() {
        List<String> missing = new ArrayList<>();
        if (!ModList.get().isLoaded("irons_spellbooks")) missing.add("Iron's Spellbooks (mana gating)");
        if (!ModList.get().isLoaded("feathers")) missing.add("Feather's Mod (stamina gating)");

        if (missing.isEmpty()) return;

        boolean failClosed = RRServerConfig.FAIL_CLOSED_WHEN_RESOURCE_MOD_MISSING.get();
        String posture = failClosed ? "failClosed=true → gated powers disabled" : "failClosed=false → gated powers FREE";

        if (failClosed) {
            RunicRacesMod.LOGGER.info("[RunicRaces] Resource gating mods not present ({}): {}",
                    posture, String.join(", ", missing));
        } else {
            RunicRacesMod.LOGGER.warn("[RunicRaces] Resource gating mods not present ({}): {}. " +
                            "Flip 'failClosedWhenResourceModMissing' in runic_races-server.toml for predictable standalone behavior.",
                    posture, String.join(", ", missing));
        }
    }

    private static void tryLoad(String modId, String modName, String className, java.util.function.BooleanSupplier configEnabled) {
        if (!ModList.get().isLoaded(modId)) {
            RunicRacesMod.debug("[RunicRaces] {} not present, skipping integration", modName);
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
            RunicRacesMod.LOGGER.error("[RunicRaces] Failed to load {} integration", modName, e);
        }
    }

    public static List<ModIntegration> getLoadedIntegrations() {
        return loadedIntegrations;
    }

    public static void syncPlayer(ServerPlayer player) {
        for (ModIntegration integration : loadedIntegrations) {
            try {
                integration.syncPlayer(player);
            } catch (Exception e) {
                RunicRacesMod.LOGGER.error("[RunicRaces] Failed to sync {} for {}: {}",
                        integration.getName(), player.getGameProfile().getName(), e.getMessage());
            }
        }

        String raceId = RaceHelper.getRaceId(player).map(Object::toString).orElse("");
        player.getPersistentData().putString(LAST_SYNCED_RACE, raceId);
    }

    private static class SyncHandler {

        @SubscribeEvent
        public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                syncPlayer(player);
            }
        }

        @SubscribeEvent
        public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                syncPlayer(player);
            }
        }

        @SubscribeEvent
        public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                syncPlayer(player);
            }
        }

        @SubscribeEvent
        public void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
                return;
            }
            if (player.tickCount % 20 != 0) {
                return;
            }

            // Compare against the last synced id without the intermediate Optional.map allocation;
            // the race lookup itself is memoized per-tick in RaceHelper.
            net.minecraft.resources.ResourceLocation raceId = RaceHelper.getRaceId(player).orElse(null);
            String currentRace = raceId == null ? "" : raceId.toString();
            String lastSyncedRace = player.getPersistentData().getString(LAST_SYNCED_RACE);
            if (!currentRace.equals(lastSyncedRace)) {
                syncPlayer(player);
            }
        }
    }
}
