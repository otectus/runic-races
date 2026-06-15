package com.otectus.runic_races.registry;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.block.TrapMarkerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, RunicRacesMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<TrapMarkerBlockEntity>> TRAP_MARKER =
            BLOCK_ENTITIES.register("trap_marker",
                    () -> BlockEntityType.Builder.of(TrapMarkerBlockEntity::new, ModBlocks.TRAP_MARKER.get())
                            .build(null));

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }

    private ModBlockEntities() {}
}
