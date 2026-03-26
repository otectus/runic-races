package com.otectus.runic_races;

import com.otectus.runic_races.command.RRCommands;
import com.otectus.runic_races.config.RRCommonConfig;
import com.otectus.runic_races.config.RRServerConfig;
import com.otectus.runic_races.event.RacialEventHandler;
import com.otectus.runic_races.integration.IntegrationManager;
import com.otectus.runic_races.registry.ModPowerFactories;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
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

    public RunicRacesMod() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register custom Apoli power factories
        ModPowerFactories.load(modBus);

        modBus.addListener(this::onCommonSetup);

        // Register event handlers on the Forge event bus
        MinecraftForge.EVENT_BUS.register(new RacialEventHandler());
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, RRCommonConfig.SPEC, "runic_races/runic_races-common.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, RRServerConfig.SPEC, "runic_races/runic_races-server.toml");

        LOGGER.info("[RunicRaces] Mod constructor complete — power factories registered");
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("[RunicRaces] Common setup — 24 races loaded via Origins data");

        // Initialize optional mod integrations
        event.enqueueWork(IntegrationManager::init);
    }

    private void onRegisterCommands(final RegisterCommandsEvent event) {
        RRCommands.register(event.getDispatcher());
        LOGGER.debug("[RunicRaces] Commands registered");
    }
}
