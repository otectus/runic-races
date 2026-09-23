package com.otectus.runic_races.ability;

import java.util.Arrays;
import java.util.Optional;

/** Stable identities only. Gameplay tuning is supplied by the owning Apoli power. */
public enum AbilityKind {
    COLOSSAN("colossan", "colossal_heave"), AURORAN("auroran", "dawnward"),
    GROVE_ELF("grove_elf", "stillleaf_aim"), TIDE_ELF("tide_elf", "currentstep"),
    ASTRAL_ELF("astral_elf", "starbound_thread"), MOUNTAIN_ONE("mountain_one", "quarry_rhythm"),
    MOSS_ONE("moss_one", "mycelial_respite"), CRYSTAL_ONE("crystal_one", "prism_reprisal"),
    BOVINE("bovine", "hornrush"), SAURIAN("saurian", "patient_ambush"),
    CHELON("chelon", "shellfast"), ZEPHYR("zephyr", "crosswind"),
    NIGHTBORN("nightborn", "crimson_hunt"), RETURNED("returned", "unfinished_purpose"),
    WAILER("wailer", "keening_cry"), SCALEHEIR("scaleheir", "dominion_roar"),
    WYVERNKIN("wyvernkin", "venom_swoop");

    private final String race;
    private final String power;
    AbilityKind(String race, String power) { this.race = race; this.power = power; }
    public String race() { return race; }
    public String power() { return power; }
    public String path() { return race + "/" + power; }
    public String cooldownId() { return "runic_races:" + path() + "_cooldown_timer"; }
    public static Optional<AbilityKind> forRace(String race) {
        return Arrays.stream(values()).filter(k -> k.race.equals(race)).findFirst();
    }
}
