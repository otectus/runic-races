package com.otectus.runic_races.ability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import java.util.Map;
import java.util.Set;

/** Validated, bounded datapack inputs. No TOML duplicate of individual kit values. */
public record AbilityTuning(AbilityKind kind, int cooldownTicks, int durationTicks,
                            Map<String, Double> parameters) implements IDynamicFeatureConfiguration {
    private static final Set<String> KEYS = Set.of("damage", "bonus", "fraction", "budget", "range",
            "land_range", "radius", "targets", "charges", "windup", "move_ticks", "interval",
            "effect_ticks", "angle", "speed", "healing", "final_cap", "stationary_ticks", "min_charge",
            "recency_ticks", "prevent_fraction", "reply_damage", "reply_range");
    private static final Codec<AbilityKind> KIND = Codec.STRING.comapFlatMap(
            s -> AbilityKind.forRace(s).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Unknown racial ability: " + s)),
            AbilityKind::race);
    private static final Codec<Map<String, Double>> PARAMETERS = Codec.unboundedMap(Codec.STRING, Codec.DOUBLE)
            .comapFlatMap(values -> values.size() <= KEYS.size() && values.entrySet().stream().allMatch(e ->
                    KEYS.contains(e.getKey()) && Double.isFinite(e.getValue()) && e.getValue() >= 0 && e.getValue() <= limit(e.getKey()))
                    ? DataResult.success(Map.copyOf(values)) : DataResult.error(() -> "Unknown or out-of-range racial ability parameter"), values -> values);
    public static final Codec<AbilityTuning> CODEC = RecordCodecBuilder.<AbilityTuning>mapCodec(i -> i.group(
            KIND.fieldOf("kind").forGetter(AbilityTuning::kind),
            Codec.intRange(10, 72000).fieldOf("cooldown_ticks").forGetter(AbilityTuning::cooldownTicks),
            Codec.intRange(1, 1200).fieldOf("duration_ticks").forGetter(AbilityTuning::durationTicks),
            PARAMETERS.fieldOf("parameters").forGetter(AbilityTuning::parameters)
    ).apply(i, AbilityTuning::new)).flatXmap(t -> t.valid()
            ? DataResult.success(t) : DataResult.error(() -> "Missing, fractional or inconsistent racial ability parameters for " + t.kind()), DataResult::success).codec();
    private boolean valid() {
        String required = switch (kind) {
            case COLOSSAN -> "bonus,min_charge";
            case AURORAN -> "budget,prevent_fraction,range,targets";
            case SCALEHEIR -> "budget,prevent_fraction,range,targets,effect_ticks";
            case GROVE_ELF -> "fraction,bonus,effect_ticks";
            case TIDE_ELF -> "range,land_range,move_ticks";
            case ASTRAL_ELF -> "range";
            case MOUNTAIN_ONE -> "fraction,charges";
            case MOSS_ONE -> "radius,targets,interval,healing,budget,final_cap";
            case CRYSTAL_ONE -> "budget,prevent_fraction,reply_damage,reply_range";
            case BOVINE -> "range,windup,move_ticks,damage";
            case SAURIAN -> "stationary_ticks,bonus,min_charge,effect_ticks";
            case CHELON -> "budget,prevent_fraction";
            case ZEPHYR -> "range,move_ticks";
            case NIGHTBORN -> "speed,charges,interval,fraction,healing,budget,final_cap,min_charge";
            case RETURNED -> "bonus,speed,recency_ticks,range,min_charge";
            case WAILER -> "windup,range,angle,targets,damage,effect_ticks,speed";
            case WYVERNKIN -> "range,land_range,move_ticks,damage,effect_ticks";
        };
        if (!parameters.keySet().containsAll(java.util.List.of(required.split(",")))) return false;
        for (String key : java.util.List.of("targets", "charges", "windup", "move_ticks", "interval", "effect_ticks", "stationary_ticks", "recency_ticks"))
            if (number(key) != Math.rint(number(key))) return false;
        for (String key : java.util.List.of("targets", "charges", "move_ticks", "interval"))
            if (parameters.containsKey(key) && number(key) < 1) return false;
        return !parameters.containsKey("move_ticks") || Math.max(number("range"), number("land_range")) <= number("move_ticks") * .8;
    }
    private static double limit(String key) {
        return switch (key) {
            case "fraction", "prevent_fraction", "min_charge", "speed" -> 1;
            case "targets" -> 6;
            case "charges" -> 10;
            case "range", "land_range", "radius", "reply_range" -> 16;
            case "angle" -> 60;
            case "windup", "move_ticks", "stationary_ticks" -> 100;
            case "interval", "effect_ticks", "recency_ticks" -> 1200;
            default -> 40;
        };
    }
    public double number(String key) { return parameters.getOrDefault(key, 0.0); }
    public float amount(String key) { return (float) number(key); }
    public int ticks(String key) { return (int) number(key); }
}
