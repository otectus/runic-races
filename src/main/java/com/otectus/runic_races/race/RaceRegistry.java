package com.otectus.runic_races.race;

import com.otectus.runic_races.race.RaceDefinition.SlotGrant;

import java.util.*;

/**
 * Single source of truth for all 24 Runic Races.
 * Other systems (integrations, commands, HUD) should query this registry
 * rather than maintaining their own hardcoded maps.
 */
public final class RaceRegistry {

    private static final Map<String, RaceDefinition> RACES = new LinkedHashMap<>();
    private static final Map<String, List<RaceDefinition>> FAMILIES = new LinkedHashMap<>();

    // Curios slot grant UUIDs — stable across sessions
    private static final UUID GOBLIN_RING_UUID = UUID.fromString("c1d2e3f4-5678-9abc-def0-123456789001");
    private static final UUID GOBLIN_CHARM_UUID = UUID.fromString("c1d2e3f4-5678-9abc-def0-123456789002");
    private static final UUID DWARF_BELT_UUID = UUID.fromString("c1d2e3f4-5678-9abc-def0-123456789003");
    private static final UUID ELF_NECKLACE_UUID = UUID.fromString("c1d2e3f4-5678-9abc-def0-123456789004");

    static {
        // === Mortal ===
        register(new RaceDefinition("human",          "mortal",    "Human",          1.00f, 20,  0.5, RaceDefinition.NO_SLOTS));
        register(new RaceDefinition("halfling",       "mortal",    "Halfling",       0.60f, 18,  1.5, RaceDefinition.NO_SLOTS));
        register(new RaceDefinition("nomad",          "mortal",    "Nomad",          0.95f, 22,  0.0, RaceDefinition.NO_SLOTS));
        register(new RaceDefinition("giant_blooded",  "mortal",    "Giant-Blooded",  1.40f, 28,  0.0, RaceDefinition.NO_SLOTS));
        // === Fae ===
        register(new RaceDefinition("high_elf",       "fae",       "High Elf",       1.05f, 16,  0.0, elfSlots()));
        register(new RaceDefinition("wood_elf",       "fae",       "Wood Elf",       0.95f, 18,  0.0, elfSlots()));
        register(new RaceDefinition("sprite",         "fae",       "Sprite",         0.425f,14,  0.0, RaceDefinition.NO_SLOTS));
        register(new RaceDefinition("changeling",     "fae",       "Changeling",     0.90f, 18,  0.0, RaceDefinition.NO_SLOTS));
        register(new RaceDefinition("dryad",          "fae",       "Dryad",          0.90f, 16,  0.0, RaceDefinition.NO_SLOTS));
        // === Beast ===
        register(new RaceDefinition("wolfkin",        "beast",     "Wolfkin",        1.00f, 22,  0.0, RaceDefinition.NO_SLOTS));
        register(new RaceDefinition("dragonborn",     "beast",     "Dragonborn",     1.15f, 22,  0.0, RaceDefinition.NO_SLOTS));
        register(new RaceDefinition("catfolk",        "beast",     "Catfolk",        0.85f, 20,  0.0, RaceDefinition.NO_SLOTS));
        register(new RaceDefinition("minotaur",       "beast",     "Minotaur",       1.25f, 26,  0.0, RaceDefinition.NO_SLOTS));
        register(new RaceDefinition("serpentfolk",    "beast",     "Serpentfolk",     0.90f, 18,  0.0, RaceDefinition.NO_SLOTS));
        // === Underfolk ===
        register(new RaceDefinition("mountain_dwarf", "underfolk", "Mountain Dwarf", 0.70f, 24,  1.0, dwarfSlots()));
        register(new RaceDefinition("deep_dwarf",     "underfolk", "Deep Dwarf",     0.65f, 22,  0.5, dwarfSlots()));
        register(new RaceDefinition("goblin",         "underfolk", "Goblin",         0.55f, 16,  2.0, goblinSlots()));
        register(new RaceDefinition("troll",          "underfolk", "Troll",          1.25f, 28, -1.0, RaceDefinition.NO_SLOTS));
        register(new RaceDefinition("kobold",         "underfolk", "Kobold",         0.50f, 16,  0.0, RaceDefinition.NO_SLOTS));
        // === Dragon ===
        register(new RaceDefinition("wyvern_blooded", "dragon",    "Wyvern-Blooded", 1.10f, 22,  0.0, RaceDefinition.NO_SLOTS));
        register(new RaceDefinition("elder_drake",    "dragon",    "Elder Drake",    1.30f, 26, -2.0, RaceDefinition.NO_SLOTS));
        // === Cursed ===
        register(new RaceDefinition("vampire",        "cursed",    "Vampire",        1.00f, 18,  0.0, RaceDefinition.NO_SLOTS));
        register(new RaceDefinition("lycanthrope",    "cursed",    "Lycanthrope",    1.00f, 22,  0.0, RaceDefinition.NO_SLOTS));
        register(new RaceDefinition("revenant",       "cursed",    "Revenant",       1.00f, 20,  0.0, RaceDefinition.NO_SLOTS));
    }

    private static SlotGrant[] goblinSlots() {
        return new SlotGrant[]{
                new SlotGrant("ring", GOBLIN_RING_UUID, 1),
                new SlotGrant("charm", GOBLIN_CHARM_UUID, 1)
        };
    }

    private static SlotGrant[] dwarfSlots() {
        return new SlotGrant[]{new SlotGrant("belt", DWARF_BELT_UUID, 1)};
    }

    private static SlotGrant[] elfSlots() {
        return new SlotGrant[]{new SlotGrant("necklace", ELF_NECKLACE_UUID, 1)};
    }

    private static void register(RaceDefinition def) {
        RACES.put(def.name(), def);
        FAMILIES.computeIfAbsent(def.family(), k -> new ArrayList<>()).add(def);
    }

    // === Public API ===

    public static Optional<RaceDefinition> get(String raceName) {
        return Optional.ofNullable(RACES.get(raceName));
    }

    public static Collection<RaceDefinition> allRaces() {
        return Collections.unmodifiableCollection(RACES.values());
    }

    public static Set<String> allRaceNames() {
        return Collections.unmodifiableSet(RACES.keySet());
    }

    public static List<RaceDefinition> racesInFamily(String family) {
        return FAMILIES.getOrDefault(family, Collections.emptyList());
    }

    public static Set<String> allFamilies() {
        return Collections.unmodifiableSet(FAMILIES.keySet());
    }

    public static float getScale(String raceName) {
        RaceDefinition def = RACES.get(raceName);
        return def != null ? def.scale() : 1.0f;
    }

    public static String getFamily(String raceName) {
        RaceDefinition def = RACES.get(raceName);
        return def != null ? def.family() : "unknown";
    }

    public static int getMaxFeathers(String raceName) {
        RaceDefinition def = RACES.get(raceName);
        return def != null ? def.maxFeathers() : 20;
    }

    public static double getLuckBonus(String raceName) {
        RaceDefinition def = RACES.get(raceName);
        return def != null ? def.luckBonus() : 0.0;
    }

    public static SlotGrant[] getSlotGrants(String raceName) {
        RaceDefinition def = RACES.get(raceName);
        return def != null ? def.curiosSlotGrants() : RaceDefinition.NO_SLOTS;
    }

    /** Returns all distinct SlotGrant entries across all races, for cleanup purposes. */
    public static Set<SlotGrant> allSlotGrants() {
        Set<SlotGrant> all = new LinkedHashSet<>();
        for (RaceDefinition def : RACES.values()) {
            Collections.addAll(all, def.curiosSlotGrants());
        }
        return all;
    }

    public static int raceCount() {
        return RACES.size();
    }

    private RaceRegistry() {}
}
