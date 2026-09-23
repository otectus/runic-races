package com.otectus.runic_races.ability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import java.util.Map;
import java.util.Set;

/** Engine-level passive/weakness tuning, kept in the appropriate visible power bundle. */
public final class RacialTraits extends PowerFactory<RacialTraits.Configuration> {
    private static final Set<String> KEYS = Set.of("magic_reduction", "physical_extra", "projectile_extra", "explosion_extra", "fire_extra",
            "fall_reduction", "forest_speed", "swim_speed", "dry_after", "wet_refresh", "dry_slow", "stone_knockback",
            "extra_air", "cold_attack_slow", "cold_freeze_extra", "night_speed", "night_food", "sun_extra", "low_health_knockback",
            "food_bonus", "saturation_bonus", "sense_range", "sense_targets", "sprint_speed");
    private static double limit(String key) {
        return switch (key) {
            case "dry_after", "wet_refresh", "extra_air" -> 1200;
            case "night_food" -> 20;
            case "food_bonus", "saturation_bonus", "sense_range" -> 8;
            case "sense_targets" -> 3;
            default -> 1;
        };
    }
    public static double value(net.minecraft.world.entity.player.Player p, String key) {
        double result = 0;
        for (var holder : io.github.edwinmindcraft.apoli.api.component.IPowerContainer.getPowers(p,
                com.otectus.runic_races.registry.ModPowerFactories.RACIAL_TRAITS.get()))
            if (holder.isBound() && holder.value().isActive(p))
                result = Math.max(result, holder.value().getConfiguration().values().getOrDefault(key, 0.0));
        return result;
    }
    public record Configuration(Map<String, Double> values) implements IDynamicFeatureConfiguration {
        private static final Codec<Map<String, Double>> VALUES = Codec.unboundedMap(Codec.STRING, Codec.DOUBLE)
                .comapFlatMap(m -> m.size() <= KEYS.size() && m.entrySet().stream().allMatch(e -> KEYS.contains(e.getKey())
                        && Double.isFinite(e.getValue()) && e.getValue() >= 0 && e.getValue() <= limit(e.getKey()))
                        ? DataResult.success(Map.copyOf(m)) : DataResult.error(() -> "Invalid racial trait parameter"), m -> m);
        public static final Codec<Configuration> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
                VALUES.fieldOf("values").forGetter(Configuration::values)).apply(i, Configuration::new));
    }
    public RacialTraits() { super(Configuration.CODEC); }
}
