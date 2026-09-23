package com.otectus.runic_races.client.presentation;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.action.ConeBreathAction.Element;
import com.otectus.runic_races.config.RRClientConfig;
import com.otectus.runic_races.network.S2CBreathVfxPacket;
import com.otectus.runic_races.presentation.ParticleBudget;
import com.otectus.runic_races.presentation.EffectGeometry;
import com.otectus.runic_races.registry.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Pixel-sprite torrents: a bright moving core, corkscrewing elemental edges and a
 * short impact plume. Snapshot aim matches the instantaneous server hit test;
 * turning after firing cannot suggest that a second, undamaged cone was hit.
 */
@Mod.EventBusSubscriber(modid = RunicRacesMod.MOD_ID, value = Dist.CLIENT)
public final class BreathVfxRenderer {
    private static final int DURATION_TICKS = 16;
    private static final int MAX_STREAMS = 12;
    private static final int MAX_PARTICLES_PER_TICK = 192;
    private static final List<Stream> STREAMS = new ArrayList<>();
    private static ClientLevel currentLevel;

    private static final class Stream {
        final S2CBreathVfxPacket cast;
        final EffectGeometry frame;
        final Vec3 right;
        final Vec3 up;
        int age;

        Stream(S2CBreathVfxPacket cast) {
            this.cast = cast;
            frame = EffectGeometry.along(cast.direction());
            right = frame.right();
            up = frame.up();
        }
    }

    private BreathVfxRenderer() {}

    public static void enqueue(S2CBreathVfxPacket cast) {
        Minecraft mc = Minecraft.getInstance();
        resetForLevel(mc.level);
        if (mc.level == null || !mc.level.dimension().location().equals(cast.dimension()) || cast.density() <= 0) return;
        STREAMS.removeIf(stream -> stream.cast.casterId() == cast.casterId());
        if (STREAMS.size() >= MAX_STREAMS) STREAMS.remove(0);
        STREAMS.add(new Stream(cast));
    }

    private static void resetForLevel(ClientLevel level) {
        if (currentLevel != level) {
            STREAMS.clear();
            currentLevel = level;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        resetForLevel(mc.level);
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        int budget = MAX_PARTICLES_PER_TICK;
        int effectsLeft = STREAMS.size();
        for (var iterator = STREAMS.iterator(); iterator.hasNext();) {
            Stream stream = iterator.next();
            var caster = mc.level.getEntity(stream.cast.casterId());
            if (stream.age >= DURATION_TICKS || caster == null || !caster.isAlive()
                    || mc.player.distanceToSqr(stream.cast.origin()) > 64 * 64) {
                iterator.remove();
                effectsLeft--;
                continue;
            }
            // Keep a readable core under reduced settings; the halo is optional.
            double detail = switch (mc.options.particles().get()) {
                case ALL -> 1.0;
                case DECREASED -> 0.55;
                case MINIMAL -> 0.25;
            };
            boolean flourish = RRClientConfig.HEAVY_EFFECTS_ENABLED.get()
                    && mc.options.particles().get() != ParticleStatus.MINIMAL && stream.cast.density() >= 0.5;
            int count = ParticleBudget.fairShare(ParticleBudget.scale(flourish ? 16 : 9,
                    stream.cast.density() * detail, 2), budget, effectsLeft--);
            emit(mc, stream, count, flourish);
            budget -= count;
            stream.age++;
        }
    }

    private static void emit(Minecraft mc, Stream stream, int count, boolean flourish) {
        var cast = stream.cast;
        Vec3 direction = cast.direction();
        // Reach the full cone immediately (damage is instantaneous), then roll
        // successive moving billows through it and taper the final four ticks.
        double taper = Math.min(1, (DURATION_TICKS - stream.age) / 4.0);
        for (int i = 0; i < count; i++) {
            double t = (i + mc.level.random.nextDouble()) / Math.max(1, count);
            double start = Math.min(0.65, cast.range() * 0.4);
            double distance = start + (cast.range() - start) * t;
            double angle = i * 2.399963 + stream.age * 0.62;
            boolean accent = flourish && i % 4 == 0;
            // The hot core stays tight; the secondary corkscrew shows the cone width.
            double width = Math.min(2.4, Math.tan(Math.toRadians(cast.halfAngle())) * distance)
                    * (accent ? 0.65 : 0.28) * taper;
            Vec3 radial = stream.right.scale(Math.cos(angle)).add(stream.up.scale(Math.sin(angle)));
            Vec3 offset = stream.frame.coneOffset(distance, width, angle);
            Vec3 proposed = cast.origin().add(offset);
            var hit = mc.level.clip(new ClipContext(cast.origin(), proposed, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, mc.level.getEntity(cast.casterId())));
            boolean blocked = hit.getType() != HitResult.Type.MISS;
            Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
            Vec3 pos = blocked ? hit.getLocation().add(normal.scale(0.06)) : proposed;
            // Leave breathing room in first person, including when pressed against a wall.
            if (mc.options.getCameraType().isFirstPerson() && cast.casterId() == mc.player.getId()
                    && pos.distanceToSqr(mc.player.getEyePosition()) < 0.9 * 0.9) continue;
            Vec3 velocity = blocked ? radial.subtract(normal.scale(radial.dot(normal))).scale(0.035).add(normal.scale(0.025))
                    : offset.normalize().scale((accent ? 0.18 : 0.32) * taper);
            int lifetime = blocked ? 9 : 7 + mc.level.random.nextInt(5);
            if (!blocked) {
                // Fade at the configured hit range instead of implying extra reach.
                double remaining = cast.range() - distance;
                double speed = Math.min(velocity.length(), remaining / 2);
                velocity = velocity.normalize().scale(speed);
                lifetime = Math.min(lifetime, Math.max(1, (int) (remaining / Math.max(1.0e-6, speed))));
            }
            ParticleOptions sprite = particle(cast.element(), accent || blocked, stream.age);
            var particle = mc.particleEngine.createParticle(sprite, pos.x, pos.y, pos.z,
                    velocity.x, velocity.y, velocity.z);
            if (particle != null) {
                particle.setParticleSpeed(velocity.x, velocity.y, velocity.z);
                particle.scale((accent ? 0.95f : 1.65f) * (float) taper);
                // Shorten the tail at solid surfaces so moving particles do not leak through walls.
                if (velocity.lengthSqr() > 1.0e-6) {
                    var tail = mc.level.clip(new ClipContext(pos, pos.add(velocity.scale(11)),
                            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.level.getEntity(cast.casterId())));
                    if (tail.getType() != HitResult.Type.MISS) {
                        lifetime = Math.min(lifetime, Math.max(1, (int) (pos.distanceTo(tail.getLocation()) / velocity.length())));
                    }
                }
                particle.setLifetime(lifetime);
            }
        }
    }

    private static ParticleOptions particle(Element element, boolean accent, int age) {
        return switch (element) {
            case FIRE -> accent ? (age % 3 == 0 ? ParticleTypes.SMOKE : ModParticles.EMBER_SCALE.get()) : ParticleTypes.FLAME;
            case FROST -> accent ? ModParticles.FROST_MOTE.get() : ParticleTypes.SNOWFLAKE;
            // BUBBLE vanishes immediately in air: splashes and blue rime remain visible on land.
            case WATER -> accent ? ParticleTypes.SPLASH : ParticleTypes.FALLING_WATER;
            case EARTH -> accent ? ModParticles.ROCK_CHIP.get()
                    : new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
            case SHOCK -> accent ? ModParticles.ARCANE_GLINT.get() : ParticleTypes.ELECTRIC_SPARK;
            case WIND -> accent ? ModParticles.GALE_STREAK.get() : ParticleTypes.CLOUD;
        };
    }
}
