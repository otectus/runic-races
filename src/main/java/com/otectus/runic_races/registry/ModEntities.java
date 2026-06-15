package com.otectus.runic_races.registry;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.entity.GraveServantEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registers entity types added by Runic Races. Currently just the Grave Servant
 * (Revenant minion summoned via {@code runic_races:summon_minion}); additional
 * entities should register here with stable save-id strings.
 */
public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RunicRacesMod.MOD_ID);

    public static final RegistryObject<EntityType<GraveServantEntity>> GRAVE_SERVANT =
            ENTITY_TYPES.register("grave_servant",
                    () -> EntityType.Builder.of(GraveServantEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.95f)
                            .clientTrackingRange(8)
                            .build("grave_servant"));

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }

    private ModEntities() {}
}
