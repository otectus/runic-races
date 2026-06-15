package com.otectus.runic_races.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

/**
 * Custom Apoli entity action: forward-cone "breath weapon" aligned to the caster's
 * look vector. Any {@link LivingEntity} whose position lies within {@code range} metres
 * and within {@code half_angle_degrees} of the look direction takes {@code damage} points
 * and is affected by the configured {@code element}. Fills the cone with element-themed
 * particles.
 * <p>
 * The {@code element} controls particles and the on-hit rider effect:
 * <ul>
 *   <li>{@code fire} — ignite for {@code fire_seconds} (default; legacy behavior)</li>
 *   <li>{@code frost} — freeze ticks + Slowness</li>
 *   <li>{@code water} — knockback + Slowness</li>
 *   <li>{@code earth} — Mining Fatigue + Slowness</li>
 *   <li>{@code shock} — brief stun (Slowness IV) + Glowing</li>
 *   <li>{@code wind} — Levitation + strong knockback</li>
 * </ul>
 * JSON usage:
 * <pre>
 * {
 *   "type": "runic_races:cone_breath",
 *   "range": 7.0,
 *   "half_angle_degrees": 22.0,
 *   "damage": 6.0,
 *   "fire_seconds": 8,
 *   "element": "fire"
 * }
 * </pre>
 */
public class ConeBreathAction extends EntityAction<ConeBreathAction.Configuration> {

    public enum Element {
        FIRE, FROST, WATER, EARTH, SHOCK, WIND;

        public static Element fromString(String s) {
            if (s == null) return FIRE;
            try {
                return valueOf(s.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return FIRE;
            }
        }

        public String serialName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private static final Codec<Element> ELEMENT_CODEC =
            Codec.STRING.xmap(Element::fromString, Element::serialName);

    public record Configuration(
            double range,
            double halfAngleDegrees,
            float damage,
            int fireSeconds,
            Element element
    ) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.DOUBLE.optionalFieldOf("range", 6.0).forGetter(Configuration::range),
                        Codec.DOUBLE.optionalFieldOf("half_angle_degrees", 25.0).forGetter(Configuration::halfAngleDegrees),
                        Codec.FLOAT.optionalFieldOf("damage", 8.0f).forGetter(Configuration::damage),
                        Codec.INT.optionalFieldOf("fire_seconds", 5).forGetter(Configuration::fireSeconds),
                        ELEMENT_CODEC.optionalFieldOf("element", Element.FIRE).forGetter(Configuration::element)
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

        ParticleOptions primary = primaryParticle(config.element());
        ParticleOptions secondary = secondaryParticle(config.element());

        // Spawn directional particles along the cone axis.
        int steps = Math.max(4, (int) (range * 3));
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            Vec3 step = origin.add(look.scale(range * t));
            double spread = 0.15 + (config.halfAngleDegrees() / 90.0) * t * 0.8;
            level.sendParticles(primary,
                    step.x, step.y, step.z,
                    4, spread, spread, spread, 0.02);
            level.sendParticles(secondary,
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
            applyElementRider(config.element(), target, look, config.fireSeconds());
        }
    }

    private static ParticleOptions primaryParticle(Element element) {
        return switch (element) {
            case FIRE -> ParticleTypes.DRAGON_BREATH;
            case FROST -> ParticleTypes.SNOWFLAKE;
            case WATER -> ParticleTypes.BUBBLE;
            case EARTH -> ParticleTypes.POOF;
            case SHOCK -> ParticleTypes.ELECTRIC_SPARK;
            case WIND -> ParticleTypes.CLOUD;
        };
    }

    private static ParticleOptions secondaryParticle(Element element) {
        return switch (element) {
            case FIRE -> ParticleTypes.FLAME;
            case FROST -> ParticleTypes.ITEM_SNOWBALL;
            case WATER -> ParticleTypes.SPLASH;
            case EARTH -> ParticleTypes.CRIT;
            case SHOCK -> ParticleTypes.CRIT;
            case WIND -> ParticleTypes.POOF;
        };
    }

    private static void applyElementRider(Element element, LivingEntity target, Vec3 look, int fireSeconds) {
        switch (element) {
            case FIRE -> {
                if (fireSeconds > 0) target.setSecondsOnFire(fireSeconds);
            }
            case FROST -> {
                // Push frozen ticks past the freeze-damage threshold (default 140).
                target.setTicksFrozen(target.getTicksFrozen() + 140);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
            }
            case WATER -> {
                target.knockback(0.6, -look.x, -look.z);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            }
            case EARTH -> {
                target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 120, 2));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
            }
            case SHOCK -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 4));
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0));
            }
            case WIND -> {
                target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 0));
                target.knockback(1.0, -look.x, -look.z);
            }
        }
    }
}
