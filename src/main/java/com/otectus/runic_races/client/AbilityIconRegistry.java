package com.otectus.runic_races.client;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.presentation.FamilyAccent;
import com.otectus.runic_races.race.RaceRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

/**
 * Maps each race's cooldown abilities to thematic vanilla item icons
 * for the custom HUD overlay. Each icon carries its owning family accent
 * (resolved at registration time from {@link RaceRegistry}) so the overlay
 * can tint frames without doing a second lookup per frame.
 */
public class AbilityIconRegistry {

    public record AbilityIcon(
            ResourceLocation resourceId,
            ItemStack icon,
            String name,
            int sortOrder,
            FamilyAccent accent
    ) {}

    private static final Map<String, List<AbilityIcon>> RACE_ABILITIES = new LinkedHashMap<>();

    static {
        // Mortal
        register("human",
                icon("human/determination_cooldown_timer", Items.GOLDEN_APPLE, "Determination", 1));
        register("halfling",
                icon("halfling/lucky_dodge_cooldown_timer", Items.RABBIT_FOOT, "Lucky Dodge", 1),
                icon("halfling/lightfoot_cooldown_timer", Items.FEATHER, "Lightfoot", 2));
        register("nomad",
                icon("nomad/sand_shield_cooldown_timer", Items.SANDSTONE, "Sand Shield", 1),
                icon("nomad/pathfinders_mark_cooldown_timer", Items.COMPASS, "Pathfinder's Mark", 2));

        // Fae
        register("high_elf",
                icon("high_elf/arcane_reflex_cooldown_timer", Items.AMETHYST_SHARD, "Arcane Reflex", 1));
        register("wood_elf",
                icon("wood_elf/thornguard_cooldown_timer", Items.SWEET_BERRIES, "Thornguard", 1),
                icon("wood_elf/canopy_meld_cooldown_timer", Items.OAK_LEAVES, "Canopy Meld", 2));
        register("sprite",
                icon("sprite/phase_shift_cooldown_timer", Items.END_ROD, "Phase Shift", 1),
                icon("sprite/fae_wings_flap_cooldown_timer", Items.FEATHER, "Fae Wings", 2));
        register("changeling",
                icon("changeling/mirror_form_cooldown_timer", Items.GLASS, "Mirror Form", 1),
                icon("changeling/assume_form_cooldown_timer", Items.PLAYER_HEAD, "Assume Form", 2));
        register("dryad",
                icon("dryad/bark_skin_cooldown_timer", Items.OAK_LOG, "Bark Skin", 1),
                icon("dryad/overgrowth_cooldown_timer", Items.OAK_SAPLING, "Overgrowth", 2));

        // Beast
        register("wolfkin",
                icon("wolfkin/pack_howl_cooldown_timer", Items.BONE, "Pack Howl", 1),
                icon("wolfkin/predators_rush_cooldown_timer", Items.LEATHER_BOOTS, "Predator's Rush", 2));
        register("dragonborn",
                icon("dragonborn/draconic_fury_cooldown_timer", Items.BLAZE_POWDER, "Draconic Fury", 1),
                icon("dragonborn/dragon_breath_cooldown_timer", Items.DRAGON_BREATH, "Dragon Breath", 2));
        register("catfolk",
                icon("catfolk/nine_lives_cooldown_timer", Items.TOTEM_OF_UNDYING, "Nine Lives", 1),
                icon("catfolk/pounce_cooldown_timer", Items.RABBIT_FOOT, "Pounce", 2));
        register("minotaur",
                icon("minotaur/labyrinthine_sense_cooldown_timer", Items.ENDER_EYE, "Labyrinthine Sense", 1));
        register("serpentfolk",
                icon("serpentfolk/shed_skin_cooldown_timer", Items.LEATHER, "Shed Skin", 1));

        // Underfolk
        register("mountain_dwarf",
                icon("mountain_dwarf/stonewall_cooldown_timer", Items.COBBLESTONE, "Stonewall", 1));
        register("deep_dwarf",
                icon("deep_dwarf/stoneshape_cooldown_timer", Items.DIAMOND_PICKAXE, "Stoneshape", 1));
        register("goblin",
                icon("goblin/scatter_cooldown_timer", Items.GUNPOWDER, "Scatter", 1));
        register("troll",
                icon("troll/regrowth_upgrade_cooldown", Items.GHAST_TEAR, "Regrowth", 1));
        register("kobold",
                icon("kobold/improvised_trap_cooldown_timer", Items.TRIPWIRE_HOOK, "Improvised Trap", 1));

        // Dragon
        register("wyvern_blooded",
                icon("wyvern_blooded/tailstrike_cooldown_timer", Items.PHANTOM_MEMBRANE, "Tailstrike", 1),
                icon("wyvern_blooded/updraft_cooldown_timer", Items.ELYTRA, "Updraft", 2),
                icon("wyvern_blooded/wyvern_wings_flap_cooldown_timer", Items.PHANTOM_MEMBRANE, "Wyvern Wings", 3));
        register("elder_drake",
                icon("elder_drake/ancient_wrath_cooldown_timer", Items.MAGMA_CREAM, "Ancient Wrath", 1),
                icon("elder_drake/primordial_roar_cooldown_timer", Items.DRAGON_HEAD, "Primordial Roar", 2),
                icon("elder_drake/ancient_wings_flap_cooldown_timer", Items.ELYTRA, "Ancient Wings", 3));

        // Cursed
        register("vampire",
                icon("vampire/blood_frenzy_cooldown_timer", Items.REDSTONE, "Blood Frenzy", 1),
                icon("vampire/mesmerize_cooldown_timer", Items.ENDER_PEARL, "Mesmerize", 2));
        register("lycanthrope",
                icon("lycanthrope/transformation_cooldown_timer", Items.FERMENTED_SPIDER_EYE, "Transformation", 1),
                icon("lycanthrope/howl_of_the_hunt_cooldown_timer", Items.GOAT_HORN, "Howl of the Hunt", 2));
        register("revenant",
                icon("revenant/deathless_spite_cooldown_timer", Items.WITHER_ROSE, "Deathless Spite", 1),
                icon("revenant/grave_call_cooldown_timer", Items.SOUL_LANTERN, "Grave Call", 2));

        int totalAbilities = RACE_ABILITIES.values().stream().mapToInt(List::size).sum();
        RunicRacesMod.LOGGER.info("[RunicRaces] AbilityIconRegistry: {} races, {} abilities. Races: {}",
                RACE_ABILITIES.size(), totalAbilities, RACE_ABILITIES.keySet());
    }

    public static List<AbilityIcon> getForRace(String raceName) {
        return RACE_ABILITIES.getOrDefault(raceName, Collections.emptyList());
    }

    public static Set<String> getAllRaces() {
        return RACE_ABILITIES.keySet();
    }

    private static AbilityIcon icon(String resourcePath, net.minecraft.world.item.Item item, String name, int order) {
        return new AbilityIcon(
                new ResourceLocation("runic_races", resourcePath),
                new ItemStack(item),
                name,
                order,
                FamilyAccent.UNKNOWN // overwritten in register() once we know the owning race
        );
    }

    private static void register(String race, AbilityIcon... icons) {
        FamilyAccent accent = FamilyAccent.forFamily(RaceRegistry.getFamily(race));
        List<AbilityIcon> tinted = new ArrayList<>(icons.length);
        for (AbilityIcon raw : icons) {
            tinted.add(new AbilityIcon(raw.resourceId(), raw.icon(), raw.name(), raw.sortOrder(), accent));
        }
        RACE_ABILITIES.put(race, List.copyOf(tinted));
    }
}
