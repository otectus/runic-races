package com.otectus.runic_races.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Custom Apoli entity action: Deep Dwarf "Tremor Ping". Only fires when the
 * caster is underground, sneaking, or standing on stone. Marks hostile mobs
 * within {@code radius} blocks with {@code minecraft:glowing} for
 * {@code duration_ticks}, emits a dust ring at the caster's feet and a bass
 * thrum sound. Safely no-ops when the gate fails.
 * <p>
 * JSON usage:
 * <pre>
 * {
 *   "type": "runic_races:tremor_ping",
 *   "radius": 16.0,
 *   "duration_ticks": 40
 * }
 * </pre>
 * <p>
 * Replaces the "always-on wallhack" implementation of Tremorsense.
 */
public class TremorPingAction extends EntityAction<TremorPingAction.Configuration> {

    public record Configuration(double radius, int durationTicks) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.DOUBLE.optionalFieldOf("radius", 16.0).forGetter(Configuration::radius),
                        Codec.INT.optionalFieldOf("duration_ticks", 40).forGetter(Configuration::durationTicks)
                ).apply(instance, Configuration::new)
        );
    }

    public TremorPingAction() {
        super(Configuration.CODEC);
    }

    @Override
    public void execute(Configuration config, Entity entity) {
        if (!(entity instanceof LivingEntity caster)) return;
        if (!(entity.level() instanceof ServerLevel level)) return;

        if (!canPing(level, caster)) return;

        AABB box = caster.getBoundingBox().inflate(config.radius());
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != caster && e.isAlive() && isHostile(e));

        for (LivingEntity target : nearby) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, config.durationTicks(), 0, false, false));
        }

        // Dust ring at caster's feet
        double cx = caster.getX();
        double cy = caster.getY() + 0.1;
        double cz = caster.getZ();
        for (int i = 0; i < 24; i++) {
            double angle = (Math.PI * 2 * i) / 24.0;
            double r = 1.5;
            level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                    cx + Math.cos(angle) * r, cy, cz + Math.sin(angle) * r,
                    1, 0.05, 0.02, 0.05, 0.0);
        }

        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.6f, 0.5f);
    }

    private static boolean canPing(ServerLevel level, LivingEntity caster) {
        if (caster.isCrouching()) return true;

        BlockPos feet = caster.blockPosition();
        BlockState below = level.getBlockState(feet.below());
        if (below.is(BlockTags.BASE_STONE_OVERWORLD)
                || below.is(BlockTags.BASE_STONE_NETHER)
                || below.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)) {
            return true;
        }

        // "Underground" heuristic: no direct sky light above caster's head.
        return !level.canSeeSky(caster.blockPosition());
    }

    private static boolean isHostile(LivingEntity entity) {
        if (entity instanceof Enemy) return true;
        if (entity instanceof Mob mob && mob.getTarget() != null) return true;
        return false;
    }
}
