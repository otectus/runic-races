package com.otectus.runic_races.registry;

import com.otectus.runic_races.RunicRacesMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registers dedicated origin icon items so Origins can render them directly.
 * One icon per race (37). Family origins reuse their first race's icon item.
 */
public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, RunicRacesMod.MOD_ID);

    // Human
    public static final RegistryObject<Item> PRIMIAN_ICON = registerIcon("primian_icon");
    public static final RegistryObject<Item> CELERON_ICON = registerIcon("celeron_icon");
    public static final RegistryObject<Item> MAGI_ICON = registerIcon("magi_icon");
    public static final RegistryObject<Item> VALEN_ICON = registerIcon("valen_icon");
    // Elven
    public static final RegistryObject<Item> HIGH_ELF_ICON = registerIcon("high_elf_icon");
    public static final RegistryObject<Item> DARK_ELF_ICON = registerIcon("dark_elf_icon");
    public static final RegistryObject<Item> MOON_ELF_ICON = registerIcon("moon_elf_icon");
    public static final RegistryObject<Item> BLOOD_ELF_ICON = registerIcon("blood_elf_icon");
    public static final RegistryObject<Item> ICE_ELF_ICON = registerIcon("ice_elf_icon");
    // Dwarven
    public static final RegistryObject<Item> DEEP_ONE_ICON = registerIcon("deep_one_icon");
    public static final RegistryObject<Item> FORGE_ONE_ICON = registerIcon("forge_one_icon");
    public static final RegistryObject<Item> FROST_ONE_ICON = registerIcon("frost_one_icon");
    public static final RegistryObject<Item> IRON_ONE_ICON = registerIcon("iron_one_icon");
    public static final RegistryObject<Item> SKY_ONE_ICON = registerIcon("sky_one_icon");
    public static final RegistryObject<Item> RUNIC_ONE_ICON = registerIcon("runic_one_icon");
    // Bestial
    public static final RegistryObject<Item> ARACHNID_ICON = registerIcon("arachnid_icon");
    public static final RegistryObject<Item> AVIAN_ICON = registerIcon("avian_icon");
    public static final RegistryObject<Item> CANINE_ICON = registerIcon("canine_icon");
    public static final RegistryObject<Item> FELINE_ICON = registerIcon("feline_icon");
    public static final RegistryObject<Item> KITSUNE_ICON = registerIcon("kitsune_icon");
    public static final RegistryObject<Item> SERPEN_ICON = registerIcon("serpen_icon");
    // Faeborne
    public static final RegistryObject<Item> CHANGELING_ICON = registerIcon("changeling_icon");
    public static final RegistryObject<Item> DRYAD_ICON = registerIcon("dryad_icon");
    public static final RegistryObject<Item> SPRITE_ICON = registerIcon("sprite_icon");
    public static final RegistryObject<Item> NYMPH_ICON = registerIcon("nymph_icon");
    public static final RegistryObject<Item> FAERIE_ICON = registerIcon("faerie_icon");
    // Undead
    public static final RegistryObject<Item> ZOMBIE_ICON = registerIcon("zombie_icon");
    public static final RegistryObject<Item> SKELETON_ICON = registerIcon("skeleton_icon");
    public static final RegistryObject<Item> WRAITH_ICON = registerIcon("wraith_icon");
    public static final RegistryObject<Item> DEMON_ICON = registerIcon("demon_icon");
    public static final RegistryObject<Item> REAPER_ICON = registerIcon("reaper_icon");
    // Draconic
    public static final RegistryObject<Item> FIRE_DRAKE_ICON = registerIcon("fire_drake_icon");
    public static final RegistryObject<Item> ICE_DRAKE_ICON = registerIcon("ice_drake_icon");
    public static final RegistryObject<Item> SEA_SERPEN_ICON = registerIcon("sea_serpen_icon");
    public static final RegistryObject<Item> TERRA_DRAKE_ICON = registerIcon("terra_drake_icon");
    public static final RegistryObject<Item> VOLT_DRAKE_ICON = registerIcon("volt_drake_icon");
    public static final RegistryObject<Item> WIND_WYRM_ICON = registerIcon("wind_wyrm_icon");

    private static RegistryObject<Item> registerIcon(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties().stacksTo(1)));
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
