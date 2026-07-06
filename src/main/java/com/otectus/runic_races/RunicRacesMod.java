package com.otectus.runic_races;

import com.otectus.runic_races.command.RRCommands;
import com.otectus.runic_races.config.RRClientConfig;
import com.otectus.runic_races.config.RRCommonConfig;
import com.otectus.runic_races.config.RRServerConfig;
import com.otectus.runic_races.event.RacialEventHandler;
import com.otectus.runic_races.integration.IntegrationManager;
import com.otectus.runic_races.network.NetworkHandler;
import com.otectus.runic_races.entity.GraveServantEntity;
import com.otectus.runic_races.registry.ModBlockEntities;
import com.otectus.runic_races.registry.ModBlocks;
import com.otectus.runic_races.registry.ModEntities;
import com.otectus.runic_races.registry.ModEntityActions;
import com.otectus.runic_races.registry.ModEntityConditions;
import com.otectus.runic_races.registry.ModItems;
import com.otectus.runic_races.registry.ModPowerFactories;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(RunicRacesMod.MOD_ID)
public class RunicRacesMod {
    public static final String MOD_ID = "runic_races";
    public static final String MOD_NAME = "Runic Races";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static void debug(String message, Object... args) {
        if (RRCommonConfig.DEBUG_LOGGING.get()) {
            LOGGER.info(message, args);
        }
    }

    public RunicRacesMod() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register custom Apoli power factories, conditions, and actions
        ModPowerFactories.load(modBus);
        ModEntityConditions.register(modBus);
        ModEntityActions.register(modBus);
        ModItems.register(modBus);
        ModEntities.register(modBus);
        ModBlocks.register(modBus);
        ModBlockEntities.register(modBus);
        com.otectus.runic_races.registry.ModParticles.register(modBus);
        com.otectus.runic_races.registry.ModSounds.register(modBus);

        modBus.addListener(this::onCommonSetup);
        modBus.addListener(this::onEntityAttributeCreation);
        modBus.addListener(this::onConfigReloading);

        // Register event handlers on the Forge event bus
        MinecraftForge.EVENT_BUS.register(new RacialEventHandler());
        MinecraftForge.EVENT_BUS.register(new com.otectus.runic_races.presentation.PresentationScheduler());
        MinecraftForge.EVENT_BUS.register(new com.otectus.runic_races.presentation.ProcDebounce());
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, RRCommonConfig.SPEC, "runic_races/runic_races-common.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, RRServerConfig.SPEC, "runic_races/runic_races-server.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, RRClientConfig.SPEC, "runic_races/runic_races-client.toml");

        LOGGER.info("[RunicRaces] Mod constructor complete — power factories registered");
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("[RunicRaces] Common setup — {} races across {} families loaded via Origins data",
                com.otectus.runic_races.race.RaceRegistry.raceCount(),
                com.otectus.runic_races.race.RaceRegistry.allFamilies().size());

        // Initialize network and optional mod integrations
        event.enqueueWork(NetworkHandler::init);
        event.enqueueWork(IntegrationManager::init);
    }

    private void onConfigReloading(final net.minecraftforge.fml.event.config.ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != RRServerConfig.SPEC) {
            return;
        }
        // Server config changed at runtime (e.g. pehkui toggle, breath density) — re-sync
        // every online player so integrations pick up the new values immediately.
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            for (var player : server.getPlayerList().getPlayers()) {
                IntegrationManager.syncPlayer(player);
            }
            debug("[RunicRaces] Server config reloaded — re-synced {} players", server.getPlayerList().getPlayerCount());
        });
    }

    private void onRegisterCommands(final RegisterCommandsEvent event) {
        RRCommands.register(event.getDispatcher());
        debug("[RunicRaces] Commands registered");
    }

    private void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.GRAVE_SERVANT.get(), GraveServantEntity.createAttributes().build());
    }
}
