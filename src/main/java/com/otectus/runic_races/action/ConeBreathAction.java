package com.otectus.runic_races.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.otectus.runic_races.config.RRServerConfig;
import com.otectus.runic_races.util.Hostility;
import com.otectus.runic_races.registry.ModParticles;
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

    private static final Codec<Element> ELEMENT_CODEC = Codec.STRING.comapFlatMap(s -> {
        try { return com.mojang.serialization.DataResult.success(Element.valueOf(s.toUpperCase(Locale.ROOT))); }
        catch (IllegalArgumentException ex) { return com.mojang.serialization.DataResult.error(() -> "Unknown breath element: " + s); }
    }, Element::serialName);

    public record Configuration(
            double range,
            double halfAngleDegrees,
            float damage,
            int fireSeconds,
            Element element
    ) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.doubleRange(0.5, 16).optionalFieldOf("range", 6.0).forGetter(Configuration::range),
                        Codec.doubleRange(1, 60).optionalFieldOf("half_angle_degrees", 25.0).forGetter(Configuration::halfAngleDegrees),
                        Codec.floatRange(0, 40).optionalFieldOf("damage", 8.0f).forGetter(Configuration::damage),
                        Codec.intRange(0, 30).optionalFieldOf("fire_seconds", 5).forGetter(Configuration::fireSeconds),
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

        ParticleOptions secondary = secondaryParticle(config.element());

        // Broadcast one snapshot; clients animate the 16-tick torrent locally.
        // Damage remains one instantaneous cone, using this same origin and aim.
        double density = RRServerConfig.BREATH_PARTICLE_DENSITY.get();
        boolean skipSecondary = density < 0.5;
        if (density > 0) {
            com.otectus.runic_races.network.NetworkHandler.sendNear(level, origin, 64,
                    new com.otectus.runic_races.network.S2CBreathVfxPacket(caster.getId(),
                            level.dimension().location(), config.element(), origin, look,
                            range, config.halfAngleDegrees(), density));
        }

        // Gather candidate living entities in a bounding box that contains the cone.
        AABB box = new AABB(origin, origin.add(look.scale(range)))
                .inflate(range * Math.sin(Math.toRadians(config.halfAngleDegrees())) + 1.0);
        // Breath is a physical cone: it deliberately hits neutral and passive mobs
        // (aim discipline is the counterplay), but never teammates or owned pets.
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != caster && e.isAlive() && !Hostility.isProtectedAlly(caster, e));

        DamageSource source = new com.otectus.runic_races.ability.RacialDamageSource(caster, false);
        int impactBursts = 0;
        for (LivingEntity target : candidates) {
            Vec3 toTarget = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(origin);
            double dist = toTarget.length();
            if (dist > range || dist < 1.0e-3) continue;

            double cosAngle = toTarget.normalize().dot(look);
            if (cosAngle < halfAngleCos) continue;
            if (!com.otectus.runic_races.ability.TargetPolicy.visible(caster, target)) continue;
            float before = target.getHealth() + target.getAbsorptionAmount();
            if (!target.hurt(source, config.damage()) || target.getHealth() + target.getAbsorptionAmount() >= before) continue;
            applyElementRider(config.element(), target, look, config.fireSeconds());

            // Small on-hit burst so the breath visibly "lands" — capped at 4 targets
            // to respect the budget in crowds.
            if (!skipSecondary && impactBursts < 4) {
                impactBursts++;
                level.sendParticles(secondary,
                        target.getX(), target.getY(0.5), target.getZ(),
                        Math.max(1, (int) Math.round(3 * density)),
                        target.getBbWidth() * 0.3, target.getBbHeight() * 0.3, target.getBbWidth() * 0.3,
                        0.05);
            }
        }
    }

    private static ParticleOptions secondaryParticle(Element element) {
        return switch (element) {
            // Drifting ember flakes make fire breath read as dragonfire, not a campfire.
            case FIRE -> ModParticles.EMBER_SCALE.get();
            // Crystalline rime motes give frost breath a glitter the snowball item lacked.
            case FROST -> ModParticles.FROST_MOTE.get();
            // Splashes remain visible out of water, unlike bubble-column particles.
            case WATER -> ParticleTypes.SPLASH;
            // Stone chips tumble out of the seismic wave with real physics.
            case EARTH -> ModParticles.ROCK_CHIP.get();
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
