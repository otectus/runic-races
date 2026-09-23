package com.otectus.runic_races.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.otectus.runic_races.ability.RacialEnvironment;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityCondition;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class RacialEnvironmentCondition extends EntityCondition<RacialEnvironmentCondition.Configuration> {
    public record Configuration(String predicate) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.comapFlatMap(s -> java.util.Set.of("night", "low_light").contains(s)
                ? DataResult.success(s) : DataResult.error(() -> "Unknown racial environment predicate: " + s), s -> s)
                .fieldOf("predicate").forGetter(Configuration::predicate)).apply(i, Configuration::new));
    }
    public RacialEnvironmentCondition() { super(Configuration.CODEC); }
    @Override public boolean check(Configuration config, Entity entity) {
        return entity instanceof LivingEntity living && (config.predicate().equals("night")
                ? RacialEnvironment.night(living) : RacialEnvironment.lowLight(living));
    }
}
