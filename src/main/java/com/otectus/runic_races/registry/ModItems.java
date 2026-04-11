package com.otectus.runic_races.registry;

import com.otectus.runic_races.RunicRacesMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registers dedicated origin icon items so Origins can render them directly.
 */
public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, RunicRacesMod.MOD_ID);

    public static final RegistryObject<Item> HUMAN_ICON = registerIcon("human_icon");
    public static final RegistryObject<Item> HALFLING_ICON = registerIcon("halfling_icon");
    public static final RegistryObject<Item> NOMAD_ICON = registerIcon("nomad_icon");
    public static final RegistryObject<Item> GIANT_BLOODED_ICON = registerIcon("giant_blooded_icon");
    public static final RegistryObject<Item> HIGH_ELF_ICON = registerIcon("high_elf_icon");
    public static final RegistryObject<Item> WOOD_ELF_ICON = registerIcon("wood_elf_icon");
    public static final RegistryObject<Item> SPRITE_ICON = registerIcon("sprite_icon");
    public static final RegistryObject<Item> CHANGELING_ICON = registerIcon("changeling_icon");
    public static final RegistryObject<Item> DRYAD_ICON = registerIcon("dryad_icon");
    public static final RegistryObject<Item> WOLFKIN_ICON = registerIcon("wolfkin_icon");
    public static final RegistryObject<Item> DRAGONBORN_ICON = registerIcon("dragonborn_icon");
    public static final RegistryObject<Item> CATFOLK_ICON = registerIcon("catfolk_icon");
    public static final RegistryObject<Item> MINOTAUR_ICON = registerIcon("minotaur_icon");
    public static final RegistryObject<Item> SERPENTFOLK_ICON = registerIcon("serpentfolk_icon");
    public static final RegistryObject<Item> MOUNTAIN_DWARF_ICON = registerIcon("mountain_dwarf_icon");
    public static final RegistryObject<Item> DEEP_DWARF_ICON = registerIcon("deep_dwarf_icon");
    public static final RegistryObject<Item> GOBLIN_ICON = registerIcon("goblin_icon");
    public static final RegistryObject<Item> TROLL_ICON = registerIcon("troll_icon");
    public static final RegistryObject<Item> KOBOLD_ICON = registerIcon("kobold_icon");
    public static final RegistryObject<Item> WYVERN_BLOODED_ICON = registerIcon("wyvern_blooded_icon");
    public static final RegistryObject<Item> ELDER_DRAKE_ICON = registerIcon("elder_drake_icon");
    public static final RegistryObject<Item> VAMPIRE_ICON = registerIcon("vampire_icon");
    public static final RegistryObject<Item> LYCANTHROPE_ICON = registerIcon("lycanthrope_icon");
    public static final RegistryObject<Item> REVENANT_ICON = registerIcon("revenant_icon");

    private static RegistryObject<Item> registerIcon(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties().stacksTo(1)));
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
