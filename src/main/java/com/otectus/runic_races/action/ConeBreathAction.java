package com.otectus.runic_races.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Custom Apoli entity action: forward-cone "breath weapon" aligned to the caster's
 * look vector. Any {@link LivingEntity} whose position lies within {@code range} metres
 * and within {@code half_angle_degrees} of the look direction takes {@code damage} points
 * and is ignited for {@code fire_seconds}. Fills the cone with dragon-breath particles.
 * <p>
 * JSON usage:
 * <pre>
 * {
 *   "type": "runic_races:cone_breath",
 *   "range": 6.0,
 *   "half_angle_degrees": 25.0,
 *   "damage": 8.0,
 *   "fire_seconds": 5
 * }
 * </pre>
 * <p>
 * Replaces the Dragonborn Dragon Breath radius-AoE implementation with a proper
 * shape-matching breath.
 */
public class ConeBreathAction extends EntityAction<ConeBreathAction.Configuration> {

    public record Configuration(
            double range,
            double halfAngleDegrees,
            float damage,
            int fireSeconds
    ) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.DOUBLE.optionalFieldOf("range", 6.0).forGetter(Configuration::range),
                        Codec.DOUBLE.optionalFieldOf("half_angle_degrees", 25.0).forGetter(Configuration::halfAngleDegrees),
                        Codec.FLOAT.optionalFieldOf("damage", 8.0f).forGetter(Configuration::damage),
                        Codec.INT.optionalFieldOf("fire_seconds", 5).forGetter(Configuration::fireSeconds)
                ).apply(instance, Configuration::new)
        );
    }

    public ConeBreathAction() {
        super(Configuration.CODEC);
    }

    @Override
    public void execute(Configuration config, Entity entity) {
        if (!(entity instanceof LivingEntity caster)) return;
        if (!(entity.level() instanceof ServerLevel level)) return;

        Vec3 origin = caster.getEyePosition();
        Vec3 look = caster.getLookAngle().normalize();
        double range = config.range();
        double halfAngleCos = Math.cos(Math.toRadians(config.halfAngleDegrees()));

        // Spawn directional particles along the cone axis.
        int steps = Math.max(4, (int) (range * 3));
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            Vec3 step = origin.add(look.scale(range * t));
            double spread = 0.15 + (config.halfAngleDegrees() / 90.0) * t * 0.8;
            level.sendParticles(ParticleTypes.DRAGON_BREATH,
                    step.x, step.y, step.z,
                    4, spread, spread, spread, 0.02);
            level.sendParticles(ParticleTypes.FLAME,
                    step.x, step.y, step.z,
                    2, spread * 0.6, spread * 0.6, spread * 0.6, 0.01);
        }

        // Gather candidate living entities in a bounding box that contains the cone.
        AABB box = new AABB(origin, origin.add(look.scale(range)))
                .inflate(range * Math.sin(Math.toRadians(config.halfAngleDegrees())) + 1.0);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != caster && e.isAlive());

        DamageSource source = level.damageSources().mobAttack(caster);
        for (LivingEntity target : candidates) {
            Vec3 toTarget = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(origin);
            double dist = toTarget.length();
            if (dist > range || dist < 1.0e-3) continue;

            double cosAngle = toTarget.normalize().dot(look);
            if (cosAngle < halfAngleCos) continue;

            target.hurt(source, config.damage());
            if (config.fireSeconds() > 0) {
                target.setSecondsOnFire(config.fireSeconds());
            }
        }
    }
}
