package com.otectus.runic_races.registry;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.power.BiomeAffinityPower;
import com.otectus.runic_races.power.ScalingAttributePower;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import io.github.edwinmindcraft.apoli.api.registry.ApoliRegistries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registers custom Apoli power factories for Runic Races.
 * These enable JSON-configured powers backed by Java logic.
 */
public class ModPowerFactories {

    public static final DeferredRegister<PowerFactory<?>> POWER_FACTORIES =
            DeferredRegister.create(ApoliRegistries.POWER_FACTORY_KEY, RunicRacesMod.MOD_ID);

    // Custom power: Biome affinity — grants buffs/debuffs based on biome tags
    public static final RegistryObject<BiomeAffinityPower> BIOME_AFFINITY =
            POWER_FACTORIES.register("biome_affinity", BiomeAffinityPower::new);

    // Custom power: Scaling attribute — attribute modifier that varies by condition
    public static final RegistryObject<ScalingAttributePower> SCALING_ATTRIBUTE =
            POWER_FACTORIES.register("scaling_attribute", ScalingAttributePower::new);

    public static void load(IEventBus modBus) {
        POWER_FACTORIES.register(modBus);
        RunicRacesMod.LOGGER.info("[RunicRaces] Registered {} custom power factories", POWER_FACTORIES.getEntries().size());
    }
}
