package com.otectus.runic_races.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.otectus.runic_races.util.Hostility;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Custom Apoli entity action: tremor ping (Deep One "Tremorsense", Terra Drake
 * "Seismic Breath" rider). Marks hostile mobs within {@code radius} blocks with
 * {@code minecraft:glowing} for {@code duration_ticks}, emits a dust ring at the
 * caster's feet and a bass thrum sound.
 * <p>
 * Deliberately ungated: an earlier version required sneaking/stone/underground and
 * silently no-oped otherwise — but the calling JSON had already consumed the ability
 * cooldown and played the full presentation, so the "gate" just ate charges. If an
 * environmental gate returns, it must live in the power JSON's activation condition
 * so the key-press is rejected before the cooldown is set.
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
                        Codec.doubleRange(0.0, 128.0).optionalFieldOf("radius", 16.0).forGetter(Configuration::radius),
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

        AABB box = caster.getBoundingBox().inflate(config.radius());
        double radiusSquared = config.radius() * config.radius();
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != caster && e.isAlive() && e.distanceToSqr(caster) <= radiusSquared
                        && Hostility.isThreatTo(caster, e));

        for (LivingEntity target : nearby) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, config.durationTicks(), 0, false, false));
        }

        // Dust ring at caster's feet — 24 single-particle puffs, delivered as one batch.
        double cx = caster.getX();
        double cy = caster.getY() + 0.1;
        double cz = caster.getZ();
        var ring = com.otectus.runic_races.presentation.ParticleBatch.jitter(
                ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, false, 24, 0.05, 0.02, 0.05, 0.0);
        for (int i = 0; i < 24; i++) {
            double angle = (Math.PI * 2 * i) / 24.0;
            double r = 1.5;
            ring.add(cx + Math.cos(angle) * r, cy, cz + Math.sin(angle) * r);
        }
        ring.send(level);

        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.6f, 0.5f);
    }

}
