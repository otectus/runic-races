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
            ResourceLocation texture,
            ItemStack icon,
            String name,
            int sortOrder,
            boolean keyActivated,
            FamilyAccent accent
    ) {}

    private static final Map<String, List<AbilityIcon>> RACE_ABILITIES = new LinkedHashMap<>();

    static {
        // Human
        register("primian", icon("primian/stroke_of_fortune_cooldown_timer", Items.GOLDEN_APPLE, "Stroke of Fortune", 1));
        register("celeron", icon("celeron/messengers_dash_cooldown_timer", Items.FEATHER, "Messenger's Dash", 1));
        register("magi", icon("magi/arcane_overflow_cooldown_timer", Items.AMETHYST_SHARD, "Arcane Overflow", 1));
        register("valen", icon("valen/unbreakable_stand_cooldown_timer", Items.SHIELD, "Unbreakable Stand", 1));

        // Elven
        register("high_elf", icon("high_elf/arcane_reflex_cooldown_timer", Items.AMETHYST_SHARD, "Arcane Reflex", 1));
        register("dark_elf", icon("dark_elf/shadowmeld_cooldown_timer", Items.INK_SAC, "Shadowmeld", 1));
        register("moon_elf", icon("moon_elf/moonlit_veil_cooldown_timer", Items.ENDER_PEARL, "Moonlit Veil", 1));
        register("blood_elf", icon("blood_elf/blood_frenzy_cooldown_timer", Items.REDSTONE, "Blood Frenzy", 1));
        register("ice_elf", icon("ice_elf/frostbind_cooldown_timer", Items.BLUE_ICE, "Frostbind", 1));

        // Dwarven
        register("deep_one", icon("deep_one/tremorsense_cooldown_timer", Items.SCULK_SENSOR, "Tremorsense", 1));
        register("forge_one", icon("forge_one/forge_blessing_cooldown_timer", Items.ANVIL, "Forge Blessing", 1));
        register("frost_one", icon("frost_one/glacial_resolve_cooldown_timer", Items.PACKED_ICE, "Glacial Resolve", 1));
        register("iron_one", icon("iron_one/shield_wall_cooldown_timer", Items.IRON_BLOCK, "Shield Wall", 1));
        register("sky_one", icon("sky_one/mountain_leap_cooldown_timer", Items.FEATHER, "Mountain Leap", 1));
        register("runic_one", icon("runic_one/rune_of_warding_cooldown_timer", Items.ENCHANTED_BOOK, "Rune of Warding", 1));

        // Bestial
        register("arachnid", icon("arachnid/web_snare_cooldown_timer", Items.COBWEB, "Web Snare", 1));
        register("avian",
                icon("avian/wind_burst_cooldown_timer", Items.FEATHER, "Wind Burst", 1),
                icon("avian/skyborne_flap_cooldown_timer", Items.PHANTOM_MEMBRANE, "Skyborne Wings", 2));
        register("canine", icon("canine/howl_of_the_pack_cooldown_timer", Items.BONE, "Howl of the Pack", 1));
        register("feline",
                // Nine Lives is a passive cheat-death, not the primary-active keybind ability.
                icon("feline/nine_lives_cooldown_timer", Items.TOTEM_OF_UNDYING, "Nine Lives", 1, false),
                icon("feline/pounce_cooldown_timer", Items.RABBIT_FOOT, "Pounce", 2, true));
        register("kitsune", icon("kitsune/foxfire_illusion_cooldown_timer", Items.SOUL_LANTERN, "Foxfire Illusion", 1));
        register("serpen", icon("serpen/shed_skin_cooldown_timer", Items.LEATHER, "Shed Skin", 1));

        // Faeborne
        register("changeling", icon("changeling/mirror_shift_cooldown_timer", Items.GLASS, "Mirror Shift", 1));
        register("dryad", icon("dryad/verdant_bloom_cooldown_timer", Items.OAK_SAPLING, "Verdant Bloom", 1));
        register("sprite",
                icon("sprite/phase_shift_cooldown_timer", Items.END_ROD, "Phase Shift", 1),
                icon("sprite/gossamer_wings_flap_cooldown_timer", Items.FEATHER, "Gossamer Wings", 2));
        register("nymph", icon("nymph/sirens_charm_cooldown_timer", Items.HEART_OF_THE_SEA, "Siren's Charm", 1));
        register("faerie",
                icon("faerie/faerie_bargain_cooldown_timer", Items.FIREWORK_ROCKET, "Faerie Bargain", 1),
                icon("faerie/pixie_flight_flap_cooldown_timer", Items.FEATHER, "Pixie Flight", 2));

        // Undead
        register("zombie", icon("zombie/undying_hunger_cooldown_timer", Items.ROTTEN_FLESH, "Undying Hunger", 1));
        register("skeleton", icon("skeleton/conscript_the_dead_cooldown_timer", Items.BONE, "Conscript the Dead", 1));
        register("wraith", icon("wraith/spectral_phase_cooldown_timer", Items.SOUL_LANTERN, "Spectral Phase", 1));
        register("demon", icon("demon/infernal_wrath_cooldown_timer", Items.BLAZE_POWDER, "Infernal Wrath", 1));
        register("reaper", icon("reaper/soul_harvest_cooldown_timer", Items.WITHER_ROSE, "Soul Harvest", 1));

        // Draconic
        register("fire_drake", icon("fire_drake/dragonfire_breath_cooldown_timer", Items.DRAGON_BREATH, "Dragonfire Breath", 1));
        register("ice_drake", icon("ice_drake/frost_breath_cooldown_timer", Items.PACKED_ICE, "Frost Breath", 1));
        register("sea_serpen", icon("sea_serpen/tidal_breath_cooldown_timer", Items.HEART_OF_THE_SEA, "Tidal Breath", 1));
        register("terra_drake", icon("terra_drake/seismic_breath_cooldown_timer", Items.DIRT, "Seismic Breath", 1));
        register("volt_drake", icon("volt_drake/lightning_breath_cooldown_timer", Items.LIGHTNING_ROD, "Lightning Breath", 1));
        register("wind_wyrm",
                icon("wind_wyrm/galeforce_breath_cooldown_timer", Items.ELYTRA, "Galeforce Breath", 1),
                icon("wind_wyrm/skylord_flap_cooldown_timer", Items.PHANTOM_MEMBRANE, "Skylord Wings", 2));

        int totalAbilities = RACE_ABILITIES.values().stream().mapToInt(List::size).sum();
        RunicRacesMod.LOGGER.info("[RunicRaces] AbilityIconRegistry: {} races, {} abilities. Races: {}",
                RACE_ABILITIES.size(), totalAbilities, RACE_ABILITIES.keySet());
    }

    public static List<AbilityIcon> getForRace(String raceName) {
        return RACE_ABILITIES.getOrDefault(raceName, Collections.emptyList());
    }

    /** The ability fired by Origins' primary-active keybind, or empty for races without one. */
    public static Optional<AbilityIcon> getPrimaryActive(String raceName) {
        return getForRace(raceName).stream().filter(AbilityIcon::keyActivated).findFirst();
    }

    public static Set<String> getAllRaces() {
        return RACE_ABILITIES.keySet();
    }

    private static AbilityIcon icon(String resourcePath, net.minecraft.world.item.Item item, String name, int order) {
        // By convention the first slot is the primary-active keybind ability; passives and
        // flap timers pass an explicit keyActivated via the 5-arg overload.
        return icon(resourcePath, item, name, order, order == 1);
    }

    private static AbilityIcon icon(String resourcePath, net.minecraft.world.item.Item item, String name, int order,
                                    boolean keyActivated) {
        // resourcePath is "<race>/<ability>_cooldown_timer"; the custom HUD art lives at
        // textures/gui/ability/<race>/<ability>.png (same stem, minus the cooldown suffix).
        String stem = resourcePath.endsWith("_cooldown_timer")
                ? resourcePath.substring(0, resourcePath.length() - "_cooldown_timer".length())
                : resourcePath;
        return new AbilityIcon(
                new ResourceLocation("runic_races", resourcePath),
                new ResourceLocation("runic_races", "textures/gui/ability/" + stem + ".png"),
                new ItemStack(item),
                name,
                order,
                keyActivated,
                FamilyAccent.UNKNOWN // overwritten in register() once we know the owning race
        );
    }

    private static void register(String race, AbilityIcon... icons) {
        FamilyAccent accent = FamilyAccent.forFamily(RaceRegistry.getFamily(race));
        List<AbilityIcon> tinted = new ArrayList<>(icons.length);
        for (AbilityIcon raw : icons) {
            tinted.add(new AbilityIcon(raw.resourceId(), raw.texture(), raw.icon(), raw.name(),
                    raw.sortOrder(), raw.keyActivated(), accent));
        }
        RACE_ABILITIES.put(race, List.copyOf(tinted));
    }
}
