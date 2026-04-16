package com.otectus.runic_races.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Custom Apoli entity action: apply {@code minecraft:glowing} to hostile mobs
 * within {@code radius} blocks of the caster for {@code duration_ticks}. Hostility
 * is determined by the {@link Enemy} interface (or any {@link Mob} currently
 * targeting something), which correctly excludes players, passive animals, and
 * villagers without needing datapack tag plumbing.
 * <p>
 * JSON usage:
 * <pre>
 * {
 *   "type": "runic_races:glow_hostiles",
 *   "radius": 12.0,
 *   "duration_ticks": 200
 * }
 * </pre>
 * <p>
 * Used by Wood Elf Canopy Meld and Minotaur Labyrinthine Sense in place of
 * {@code origins:area_of_effect} so only threats light up, not every animal.
 */
public class GlowHostilesAction extends EntityAction<GlowHostilesAction.Configuration> {

    public record Configuration(double radius, int durationTicks) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.DOUBLE.optionalFieldOf("radius", 12.0).forGetter(Configuration::radius),
                        Codec.INT.optionalFieldOf("duration_ticks", 200).forGetter(Configuration::durationTicks)
                ).apply(instance, Configuration::new)
        );
    }

    public GlowHostilesAction() {
        super(Configuration.CODEC);
    }

    @Override
    public void execute(Configuration config, Entity entity) {
        if (!(entity instanceof LivingEntity caster)) return;

        AABB box = caster.getBoundingBox().inflate(config.radius());
        List<LivingEntity> nearby = caster.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != caster && e.isAlive() && isHostile(e));

        for (LivingEntity target : nearby) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, config.durationTicks(), 0, false, false));
        }
    }

    private static boolean isHostile(LivingEntity entity) {
        if (entity instanceof Enemy) return true;
        return entity instanceof Mob mob && mob.getTarget() != null;
    }
}
