package com.otectus.runic_races.ability;

import com.otectus.runic_races.config.RRServerConfig;
import com.otectus.runic_races.presentation.RaceColors;
import com.otectus.runic_races.presentation.ParticleBatch;
import com.otectus.runic_races.presentation.ParticleBudget;
import com.otectus.runic_races.registry.ModParticles;
import com.otectus.runic_races.registry.ModSounds;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;

/** Execution feedback: nearby observers see only confirmed effects and live fields. */
public final class AbilityFeedback {
    private static final FeedbackBudget BUDGET = new FeedbackBudget(160);

    private AbilityFeedback() {}

    private static int reserve(ServerPlayer owner, int authored) {
        int wanted = ParticleBudget.scale(authored, RRServerConfig.SIGNATURE_PARTICLE_DENSITY.get(), 1);
        return BUDGET.reserve(owner.getUUID(), AbilityService.now(owner), wanted);
    }

    public static void clear() { BUDGET.clear(); }

    private static ParticleOptions identity(AbilityKind kind) {
        return switch (kind) {
            case COLOSSAN, MOUNTAIN_ONE, BOVINE, CHELON -> ModParticles.ROCK_CHIP.get();
            case AURORAN -> ParticleTypes.END_ROD;
            case GROVE_ELF -> ModParticles.LEAF_PETAL.get();
            case TIDE_ELF -> ParticleTypes.SPLASH;
            case ASTRAL_ELF -> ModParticles.ARCANE_GLINT.get();
            case MOSS_ONE -> ModParticles.POLLEN_MOTE.get();
            case CRYSTAL_ONE -> ModParticles.MIRROR_SHARD.get();
            case SAURIAN, WYVERNKIN -> ModParticles.VENOM_DRIP.get();
            case ZEPHYR -> ModParticles.GALE_STREAK.get();
            case NIGHTBORN -> RaceColors.CRIMSON_BLOOD;
            case RETURNED, WAILER -> ModParticles.SOUL_WISP.get();
            case SCALEHEIR -> ModParticles.EMBER_SCALE.get();
        };
    }

    private static void cloud(ServerPlayer owner, Vec3 at, ParticleOptions particle, int authored,
                              double spread, double speed) {
        int count = reserve(owner, authored);
        if (count > 0) owner.serverLevel().sendParticles(particle, at.x, at.y, at.z,
                count, spread, spread, spread, speed);
    }

    private static void ring(ServerPlayer owner, Vec3 center, ParticleOptions particle, int authored,
                             double radius, double phase, double rise) {
        int count = reserve(owner, authored);
        ParticleBatch batch = ParticleBatch.directed(particle, false, count);
        for (int i = 0; i < count; i++) {
            double angle = phase + Math.PI * 2 * i / count;
            batch.add(center.x + Math.cos(angle) * radius,
                    center.y, center.z + Math.sin(angle) * radius,
                    -Math.sin(angle) * 0.15, rise, Math.cos(angle) * 0.15, 0.035);
        }
        batch.send(owner.serverLevel());
    }

    public static void impact(ServerPlayer owner, Vec3 at, AbilityKind kind) {
        cloud(owner, at, identity(kind), 9, 0.3, 0.075);
        cloud(owner, at, switch (kind) {
            case CRYSTAL_ONE, ASTRAL_ELF, AURORAN -> ParticleTypes.END_ROD;
            case RETURNED, WAILER, NIGHTBORN -> ParticleTypes.SOUL;
            default -> ParticleTypes.CRIT;
        }, 5, 0.16, 0.08);
    }

    public static void healing(ServerPlayer owner, LivingEntity recipient, AbilityKind kind) {
        ring(owner, recipient.position().add(0, 0.15, 0), identity(kind), 8,
                Math.max(0.35, recipient.getBbWidth() * 0.65), 0, 1);
        cloud(owner, recipient.getBoundingBox().getCenter(), ParticleTypes.HAPPY_VILLAGER, 4, 0.3, 0.015);
    }

    public static void ward(ServerPlayer owner, LivingEntity recipient, WardLedger.Kind kind) {
        ParticleOptions particle = switch (kind) {
            case DAWN -> ParticleTypes.END_ROD;
            case PRISM -> ModParticles.MIRROR_SHARD.get();
            case SHELL -> ParticleTypes.WAX_ON;
            case DOMINION -> ModParticles.EMBER_SCALE.get();
        };
        ring(owner, recipient.getBoundingBox().getCenter(), particle, 12,
                Math.max(0.5, recipient.getBbWidth() * 0.8), 0, 0.6);
    }

    /** The ward coming down: the same ring, sinking instead of rising. */
    public static void wardEnd(ServerPlayer owner, LivingEntity recipient, WardLedger.Kind kind) {
        ParticleOptions particle = switch (kind) {
            case DAWN -> ParticleTypes.END_ROD;
            case PRISM -> ModParticles.MIRROR_SHARD.get();
            case SHELL -> ParticleTypes.WAX_OFF;
            case DOMINION -> ModParticles.EMBER_SCALE.get();
        };
        ring(owner, recipient.getBoundingBox().getCenter(), particle, 10,
                Math.max(0.5, recipient.getBbWidth() * 0.8), 0, -0.6);
    }

    public static void field(ServerPlayer owner, Vec3 center, double radius) {
        ring(owner, center.add(0, 0.12, 0), ModParticles.POLLEN_MOTE.get(), 20,
                radius, AbilityService.now(owner) * 0.035, 0.2);
        cloud(owner, center.add(0, 0.4, 0), ParticleTypes.SPORE_BLOSSOM_AIR, 3, 0.5, 0.01);
    }

    public static void anchor(ServerPlayer owner, Vec3 center) {
        ring(owner, center.add(0, 0.1, 0), ModParticles.ARCANE_GLINT.get(), 12,
                0.7, AbilityService.now(owner) * 0.04, 0.3);
        cloud(owner, center.add(0, 0.65, 0), ParticleTypes.END_ROD, 2, 0.1, 0.012);
    }

    public static void recall(ServerPlayer owner, Vec3 from, Vec3 to) {
        ring(owner, from.add(0, 0.2, 0), ModParticles.MOON_SLIVER.get(), 16, 0.8, 0, 0.8);
        ring(owner, to.add(0, 0.2, 0), ModParticles.ARCANE_GLINT.get(), 16, 0.8, 0, 1.2);
        cloud(owner, to.add(0, owner.getBbHeight() * 0.5, 0), ParticleTypes.END_ROD, 8, 0.4, 0.05);
    }

    /** At most six streaks along the movement that actually succeeded, every other tick. */
    public static void movement(ServerPlayer owner, Vec3 from, Vec3 to, AbilityKind kind) {
        trail(owner, from.add(0, 0.35, 0), to.add(0, 0.35, 0), identity(kind), 6);
    }

    public static void arrow(ServerPlayer owner, Vec3 from, Vec3 to) {
        trail(owner, from, to, ModParticles.LEAF_PETAL.get(), 4);
        cloud(owner, to, ModParticles.POLLEN_MOTE.get(), 1, 0.035, 0.005);
    }

    public static void drain(ServerPlayer owner, LivingEntity victim) {
        trail(owner, victim.getBoundingBox().getCenter(), owner.getBoundingBox().getCenter(),
                RaceColors.CRIMSON_BLOOD, 12);
    }

    public static void affliction(ServerPlayer owner, LivingEntity target, String race) {
        ParticleOptions particle = switch (race) {
            case "ice_elf", "frost_one" -> ModParticles.FROST_MOTE.get();
            case "demon" -> ParticleTypes.FLAME;
            case "kitsune" -> ModParticles.FOXFIRE.get();
            case "nymph" -> ModParticles.FAE_SPARKLE.get();
            case "wraith", "reaper" -> ModParticles.SOUL_WISP.get();
            default -> ModParticles.ARCANE_GLINT.get();
        };
        ring(owner, target.position().add(0, 0.15, 0), particle, 8,
                Math.max(0.4, target.getBbWidth() * 0.7), 0, 0.8);
        cloud(owner, target.getBoundingBox().getCenter(), particle, 4, 0.25, 0.025);
    }

    public static void summon(ServerPlayer owner, Vec3 position) {
        ring(owner, position.add(0, 0.1, 0), ModParticles.BONE_CHIP.get(), 12, 0.65, 0, 0.3);
        cloud(owner, position.add(0, 0.6, 0), ModParticles.SOUL_WISP.get(), 8, 0.35, 0.055);
    }

    public static void snare(ServerPlayer owner, LivingEntity victim) {
        for (int layer = 0; layer < 3; layer++)
            ring(owner, victim.position().add(0, 0.15 + layer * 0.35, 0), ModParticles.WEB_STRAND.get(),
                    8, Math.max(0.45, victim.getBbWidth() * 0.75), layer * 0.4, 0.1);
        cloud(owner, victim.getBoundingBox().getCenter(), ModParticles.VENOM_DRIP.get(), 6, 0.25, 0.025);
    }

    private static void trail(ServerPlayer owner, Vec3 from, Vec3 to, ParticleOptions particle, int authored) {
        Vec3 delta = to.subtract(from);
        if (delta.lengthSqr() < 1.0e-6 || delta.lengthSqr() > 256) return;
        int count = reserve(owner, authored);
        Vec3 direction = delta.normalize();
        ParticleBatch batch = ParticleBatch.directed(particle, false, count);
        for (int i = 0; i < count; i++) {
            Vec3 at = from.add(delta.scale((i + 0.5) / count));
            batch.add(at.x, at.y, at.z, direction.x, direction.y, direction.z, 0.055);
        }
        batch.send(owner.serverLevel());
    }

    /** Three expanding sound fronts, emitted only after the interruptible windup succeeds. */
    public static void cry(ServerPlayer owner, double range, double angleDegrees) {
        owner.serverLevel().playSound(null, owner.blockPosition(), ModSounds.WAILER_CRY.get(),
                SoundSource.PLAYERS, 0.8f, 0.85f);
        Vec3 origin = owner.getEyePosition();
        Vec3 aim = owner.getLookAngle().normalize();
        Vec3 side = aim.cross(Math.abs(aim.y) > 0.98 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
        Vec3 up = side.cross(aim).normalize();
        int count = reserve(owner, 36);
        // Two particle types alternate; each travels as its own batch, in authored order.
        ParticleBatch wisps = ParticleBatch.directed(ModParticles.SOUL_WISP.get(), false, count / 2 + 1);
        ParticleBatch streaks = ParticleBatch.directed(ModParticles.GALE_STREAK.get(), false, count / 2 + 1);
        for (int i = 0; i < count; i++) {
            int front = i % 3;
            double distance = range * (front + 1) / 3.0;
            double phase = Math.PI * 2 * (i / 3.0) / Math.max(1, count / 3.0);
            double halfAngle = Math.toRadians(angleDegrees);
            Vec3 ray = aim.scale(Math.cos(halfAngle)).add(side.scale(Math.cos(phase) * Math.sin(halfAngle)))
                    .add(up.scale(Math.sin(phase) * Math.sin(halfAngle)));
            Vec3 requested = origin.add(ray.scale(distance));
            // A visual front cannot continue through a wall that blocks the real cry.
            Vec3 at = owner.serverLevel().clip(new ClipContext(origin, requested,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner)).getLocation();
            Vec3 direction = at.subtract(origin).normalize();
            (i % 2 == 0 ? wisps : streaks).add(at.x, at.y, at.z, direction.x, direction.y, direction.z, 0.12);
        }
        wisps.send(owner.serverLevel());
        streaks.send(owner.serverLevel());
    }
}
