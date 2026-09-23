package com.otectus.runic_races.integration;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.common.state.RaceStateTracker;
import com.otectus.runic_races.config.RRServerConfig;
import com.otectus.runic_races.diagnostics.RRMetrics;
import com.otectus.runic_races.util.RaceHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Manages optional mod integrations using reflection-based loading.
 * Integration classes that reference mod-specific types are only loaded
 * when their target mod is present, preventing NoClassDefFoundError.
 *
 * Two lifetimes are kept apart:
 * <ul>
 *   <li><b>JVM</b> — an adapter is constructed and {@link ModIntegration#init() initialized}
 *       once per present mod, whatever its toggle says, so its listeners and any
 *       third-party registry entries are never registered twice.</li>
 *   <li><b>Server session / config generation</b> — whether an integration is
 *       <em>active</em> is read live from {@code runic_races-server.toml}. A second world in
 *       the same client, or a config edit on a running server, therefore takes effect, and
 *       a disabled adapter still runs {@code syncPlayer} to remove the state it owns.</li>
 * </ul>
 */
public class IntegrationManager {

    private static final String LAST_SYNCED_RACE = "runic_races:last_synced_race";

    private record Adapter(ModIntegration integration, BooleanSupplier enabled) {}

    private static final List<Adapter> adapters = new ArrayList<>();
    /** Toggle values the online players were last synchronized against. */
    private static final Map<String, Boolean> syncedActivation = new HashMap<>();
    private static boolean registered = false;

    /** Common setup runs on dedicated servers and clients; no SERVER config may be read here. */
    public static void registerCommonTypes() {
        if (!ModList.get().isLoaded("pehkui")) return;
        try {
            Class.forName("com.otectus.runic_races.integration.pehkui.PehkuiIntegration")
                    .getMethod("registerScaleTypes").invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not register racial Pehkui scale types", e);
        }
    }

    /** Called at every server start, after that world's server config has loaded. */
    public static void init() {
        if (!registered) {
            registered = true;
            com.otectus.runic_races.ability.ShellCasting.init();
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
                    () -> true); // Keep the adapter available to remove persisted owned scales when disabled.

            tryLoad("feathers", "Feather's Mod",
                    "com.otectus.runic_races.integration.feathers.FeathersIntegration",
                    () -> RRServerConfig.FEATHERS_INTEGRATION.get());

            MinecraftForge.EVENT_BUS.register(new SyncHandler());
        }

        syncedActivation.clear();
        int active = 0;
        for (Adapter adapter : adapters) {
            boolean enabled = adapter.enabled().getAsBoolean();
            syncedActivation.put(adapter.integration().getName(), enabled);
            if (enabled) {
                active++;
            } else {
                RunicRacesMod.LOGGER.info("[RunicRaces] {} integration disabled by config", adapter.integration().getName());
            }
        }
        RunicRacesMod.LOGGER.info("[RunicRaces] {} integrations active ({} adapters loaded)", active, adapters.size());

        logMissingGatingMods();
    }

    /**
     * Server config changed on a running server. Only integrations whose toggle actually
     * flipped are re-synchronized, so an unrelated edit (a particle density) no longer
     * re-applies every integration to every online player.
     */
    public static void onServerConfigReloaded(MinecraftServer server) {
        if (!registered) return;
        List<ModIntegration> changed = new ArrayList<>();
        for (Adapter adapter : adapters) {
            String name = adapter.integration().getName();
            boolean enabled = adapter.enabled().getAsBoolean();
            Boolean previous = syncedActivation.put(name, enabled);
            if (previous == null || previous != enabled) {
                changed.add(adapter.integration());
                RunicRacesMod.LOGGER.info("[RunicRaces] {} integration {} by config reload", name, enabled ? "enabled" : "disabled");
            }
        }
        if (changed.isEmpty()) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (ModIntegration integration : changed) {
                syncOne(integration, player);
            }
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

    private static void tryLoad(String modId, String modName, String className, BooleanSupplier configEnabled) {
        if (!ModList.get().isLoaded(modId)) {
            RunicRacesMod.debug("[RunicRaces] {} not present, skipping integration", modName);
            return;
        }
        try {
            Class<?> clazz = Class.forName(className);
            ModIntegration integration = (ModIntegration) clazz.getDeclaredConstructor().newInstance();
            integration.init();
            adapters.add(new Adapter(integration, configEnabled));
            RunicRacesMod.LOGGER.info("[RunicRaces] {} integration loaded successfully", modName);
        } catch (Exception e) {
            RunicRacesMod.LOGGER.error("[RunicRaces] Failed to load {} integration", modName, e);
        }
    }

    /** Adapters for present mods, including ones their config toggle currently disables. */
    public static List<ModIntegration> getLoadedIntegrations() {
        List<ModIntegration> loaded = new ArrayList<>(adapters.size());
        for (Adapter adapter : adapters) loaded.add(adapter.integration());
        return loaded;
    }

    /**
     * Whether the integration with this display name is loaded and enabled by the current
     * server config. "Present on the classpath" is not the same question: callers that
     * change gameplay need to know what is actually in effect.
     */
    public static boolean isIntegrationActive(String name) {
        for (Adapter adapter : adapters) {
            if (adapter.integration().getName().equals(name)) {
                return adapter.enabled().getAsBoolean();
            }
        }
        return false;
    }

    public static void syncPlayer(ServerPlayer player) {
        for (Adapter adapter : adapters) {
            syncOne(adapter.integration(), player);
        }

        String raceId = RaceHelper.getRaceId(player).map(Object::toString).orElse("");
        player.getPersistentData().putString(LAST_SYNCED_RACE, raceId);
    }

    private static void syncOne(ModIntegration integration, ServerPlayer player) {
        try {
            integration.syncPlayer(player);
            RRMetrics.add(RRMetrics.Counter.INTEGRATION_SYNCS);
        } catch (Exception e) {
            RunicRacesMod.LOGGER.error("[RunicRaces] Failed to sync {} for {}: {}",
                    integration.getName(), player.getGameProfile().getName(), e.getMessage());
        }
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
            boolean resizePending = player.getPersistentData().getBoolean("runic_races:resize_pending");
            if (resizePending && !isIntegrationActive("Pehkui")) {
                player.getPersistentData().remove("runic_races:resize_pending");
                resizePending = false;
            }
            if (!currentRace.equals(lastSyncedRace)) {
                // The race changed under the player — stale state flags belong to the old
                // race, so drop them before the integrations re-apply.
                RaceStateTracker.clear(player);
                syncPlayer(player);
            } else if (resizePending) {
                // A resize retry is not a race change: preserve live HUD flags and
                // avoid replaying state-entry notifications each second near a wall.
                syncPlayer(player);
            }
        }
    }
}
