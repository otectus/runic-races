package com.otectus.runic_races.race;

import com.otectus.runic_races.race.RaceDefinition.SlotGrant;

import java.util.*;

/**
 * Single source of truth for all 37 Runic Races across 7 families.
 * Other systems (integrations, commands, HUD) should query this registry
 * rather than maintaining their own hardcoded maps.
 *
 * Families: human, elven, dwarven, bestial, faeborne, undead, draconic.
 */
public final class RaceRegistry {

    private static final Map<String, RaceDefinition> RACES = new LinkedHashMap<>();
    private static final Map<String, List<RaceDefinition>> FAMILIES = new LinkedHashMap<>();

    // Curios slot grant UUIDs — stable across sessions. One slot type per family.
    private static final UUID ELVEN_NECKLACE_UUID  = UUID.fromString("c1d2e3f4-5678-9abc-def0-123456789001");
    private static final UUID DWARVEN_BELT_UUID     = UUID.fromString("c1d2e3f4-5678-9abc-def0-123456789002");
    private static final UUID FAEBORNE_RING_UUID    = UUID.fromString("c1d2e3f4-5678-9abc-def0-123456789003");
    private static final UUID UNDEAD_CHARM_UUID     = UUID.fromString("c1d2e3f4-5678-9abc-def0-123456789004");

    static {
        // === Human === gold · generalists, fortune, versatility
        register(new RaceDefinition("primian",   "human",   "Primian", 1.00f, 20,  1.0, RaceDefinition.NO_SLOTS));
        register(new RaceDefinition("celeron",   "human",   "Celeron", 0.95f, 22,  0.5, RaceDefinition.NO_SLOTS));
        register(new RaceDefinition("magi",      "human",   "Magi",    1.00f, 16,  0.0, RaceDefinition.NO_SLOTS));
        register(new RaceDefinition("valen",     "human",   "Valen",   1.10f, 26,  0.0, RaceDefinition.NO_SLOTS));
        // === Elven === magenta · grace + arcane, frail bodies (tall & lithe)
        register(new RaceDefinition("high_elf",  "elven",   "High Elf",  1.08f, 16, 0.0, elvenSlots()));
        register(new RaceDefinition("dark_elf",  "elven",   "Dark Elf",  1.04f, 18, 0.0, elvenSlots()));
        register(new RaceDefinition("moon_elf",  "elven",   "Moon Elf",  1.06f, 16, 0.0, elvenSlots()));
        register(new RaceDefinition("blood_elf", "elven",   "Blood Elf", 1.05f, 18, 0.0, elvenSlots()));
        register(new RaceDefinition("ice_elf",   "elven",   "Ice Elf",   1.06f, 16, 0.0, elvenSlots()));
        // === Dwarven === slate · tough, armored, subterranean
        register(new RaceDefinition("deep_one",  "dwarven", "Deep One",  0.68f, 24, 0.5, dwarvenSlots()));
        register(new RaceDefinition("forge_one", "dwarven", "Forge One", 0.72f, 24, 0.0, dwarvenSlots()));
        register(new RaceDefinition("frost_one", "dwarven", "Frost One", 0.72f, 26, 0.0, dwarvenSlots()));
        register(new RaceDefinition("iron_one",  "dwarven", "Iron One",  0.70f, 26, 0.0, dwarvenSlots()));
        register(new RaceDefinition("sky_one",   "dwarven", "Sky One",   0.74f, 22, 0.0, dwarvenSlots()));
        register(new RaceDefinition("runic_one", "dwarven", "Runic One", 0.70f, 22, 0.5, dwarvenSlots()));
        // === Bestial === green · senses + agility, predatory
        register(new RaceDefinition("arachnid",  "bestial", "Arachnid", 0.90f, 18, 0.0, RaceDefinition.NO_SLOTS));
        register(new RaceDefinition("avian",     "bestial", "Avian",    0.90f, 18, 0.0, RaceDefinition.NO_SLOTS));
        register(new RaceDefinition("canine",    "bestial", "Canine",   1.00f, 24, 0.0, RaceDefinition.NO_SLOTS));
        register(new RaceDefinition("feline",    "bestial", "Feline",   0.85f, 20, 0.0, RaceDefinition.NO_SLOTS));
        register(new RaceDefinition("kitsune",   "bestial", "Kitsune",  0.92f, 18, 0.5, RaceDefinition.NO_SLOTS));
        register(new RaceDefinition("serpen",    "bestial", "Serpen",   0.95f, 18, 0.0, RaceDefinition.NO_SLOTS));
        // === Faeborne === teal · magic + illusion, small/fragile
        register(new RaceDefinition("changeling","faeborne","Changeling", 0.90f, 18, -0.5, faeborneSlots()));
        register(new RaceDefinition("dryad",     "faeborne","Dryad",      0.95f, 16,  0.0, faeborneSlots()));
        register(new RaceDefinition("sprite",    "faeborne","Sprite",     0.45f, 12,  0.0, faeborneSlots()));
        register(new RaceDefinition("nymph",     "faeborne","Nymph",      0.95f, 16,  0.0, faeborneSlots()));
        register(new RaceDefinition("faerie",    "faeborne","Faerie",     0.50f, 12,  1.0, faeborneSlots()));
        // === Undead === purple · undeath immunities + night power
        register(new RaceDefinition("zombie",    "undead",  "Zombie",   1.00f, 24,  0.0, undeadSlots()));
        register(new RaceDefinition("skeleton",  "undead",  "Skeleton", 0.95f, 22,  0.0, undeadSlots()));
        register(new RaceDefinition("wraith",    "undead",  "Wraith",   0.95f, 20,  0.0, undeadSlots()));
        register(new RaceDefinition("demon",     "undead",  "Demon",    1.20f, 26, -1.0, undeadSlots()));
        register(new RaceDefinition("reaper",    "undead",  "Reaper",   1.10f, 22, -1.0, undeadSlots()));
        // === Draconic === red · elemental breath + scaled armor + flight
        register(new RaceDefinition("fire_drake", "draconic","Fire Drake", 1.20f, 26, -1.0, RaceDefinition.NO_SLOTS));
        register(new RaceDefinition("ice_drake",  "draconic","Ice Drake",  1.15f, 24, -1.0, RaceDefinition.NO_SLOTS));
        register(new RaceDefinition("sea_serpen", "draconic","Sea Serpen", 1.20f, 24, -0.5, RaceDefinition.NO_SLOTS));
        register(new RaceDefinition("terra_drake","draconic","Terra Drake",1.30f, 28, -1.0, RaceDefinition.NO_SLOTS));
        register(new RaceDefinition("volt_drake", "draconic","Volt Drake", 1.10f, 24, -0.5, RaceDefinition.NO_SLOTS));
        register(new RaceDefinition("wind_wyrm",  "draconic","Wind Wyrm",  1.15f, 24, -2.0, RaceDefinition.NO_SLOTS));
    }

    private static SlotGrant[] elvenSlots() {
        return new SlotGrant[]{new SlotGrant("necklace", ELVEN_NECKLACE_UUID, 1)};
    }

    private static SlotGrant[] dwarvenSlots() {
        return new SlotGrant[]{new SlotGrant("belt", DWARVEN_BELT_UUID, 1)};
    }

    private static SlotGrant[] faeborneSlots() {
        return new SlotGrant[]{new SlotGrant("ring", FAEBORNE_RING_UUID, 1)};
    }

    private static SlotGrant[] undeadSlots() {
        return new SlotGrant[]{new SlotGrant("charm", UNDEAD_CHARM_UUID, 1)};
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
