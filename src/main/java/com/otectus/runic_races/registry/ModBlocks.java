package com.otectus.runic_races.registry;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.block.TrapMarkerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * DeferredRegister for Runic Races blocks. Currently just the Kobold trap marker;
 * future blocks (rune altars, etc.) should register here.
 */
public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, RunicRacesMod.MOD_ID);

    public static final RegistryObject<TrapMarkerBlock> TRAP_MARKER =
            BLOCKS.register("trap_marker", () -> new TrapMarkerBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(0.1f)
                            .noCollission()
                            .noOcclusion()
                            .sound(SoundType.WOOL)
                            .pushReaction(PushReaction.DESTROY)
                            .instabreak()
            ));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }

    private ModBlocks() {}
}
