package com.otectus.runic_races.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Shared client particle for all runic_races custom particle types. Each
 * {@link Behavior} tunes lifetime, gravity, drift, and glow so one class covers
 * the six identity particles without six near-identical subclasses.
 */
@OnlyIn(Dist.CLIENT)
public class RunicParticle extends TextureSheetParticle {

    public enum Behavior {
        /** Rune glyph: hangs in place, drifts gently upward, emissive. */
        RUNE,
        /** Soul wisp: slow upward wander with sinusoidal sway, emissive. */
        WISP,
        /** Fae sparkle: short-lived twinkle that oscillates in size, emissive. */
        SPARKLE,
        /** Ember scale: flutters downward like a burning flake, emissive. */
        EMBER,
        /** Frost mote: gentle crystalline fall, soft glow. */
        MOTE,
        /** Venom drip: falls under gravity and fades. */
        DRIP,
        /** Web strand: shoots out laterally, brakes hard, then hangs with a slight sag. */
        STRAND,
        /** Leaf/petal/feather: tumbling fall with wide sideways sway, world-lit. */
        LEAF,
        /** Shadow wisp: soul-wisp sway but sinking instead of rising, world-lit. */
        SHADE,
        /** Bone chip: sharp physical shard, heavy fall with physics, world-lit. */
        CHIP,
        /** Mirror shard: glassy fragment, tumbles fast under gravity with physics, world-lit. */
        SHARD,
        /** Gale streak: fast elongated dash that brakes and fades quickly, emissive. */
        STREAK,
        /** Foxfire: spirit flame with a nervous flicker and lateral wander, flares before dying, emissive. */
        FOXFIRE
    }

    private final SpriteSet sprites;
    private final Behavior behavior;
    private final float baseQuadSize;

    protected RunicParticle(ClientLevel level, double x, double y, double z,
                            double dx, double dy, double dz,
                            SpriteSet sprites, Behavior behavior) {
        super(level, x, y, z, dx, dy, dz);
        this.sprites = sprites;
        this.behavior = behavior;
        // The Particle super ctor randomizes velocity; restore the exact requested motion.
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;

        switch (behavior) {
            case RUNE -> {
                this.lifetime = 28 + this.random.nextInt(8);
                this.gravity = -0.01f;
                this.friction = 0.92f;
                this.quadSize = 0.14f;
                this.hasPhysics = false;
            }
            case WISP -> {
                this.lifetime = 36 + this.random.nextInt(12);
                this.gravity = -0.008f;
                this.friction = 0.95f;
                this.quadSize = 0.12f;
                this.hasPhysics = false;
            }
            case SPARKLE -> {
                this.lifetime = 16 + this.random.nextInt(8);
                this.gravity = 0.0f;
                this.friction = 0.90f;
                this.quadSize = 0.09f;
                this.hasPhysics = false;
            }
            case EMBER -> {
                this.lifetime = 30 + this.random.nextInt(10);
                this.gravity = 0.02f;
                this.friction = 0.96f;
                this.quadSize = 0.10f;
                this.hasPhysics = false;
            }
            case MOTE -> {
                this.lifetime = 26 + this.random.nextInt(8);
                this.gravity = 0.015f;
                this.friction = 0.94f;
                this.quadSize = 0.09f;
                this.hasPhysics = false;
            }
            case DRIP -> {
                this.lifetime = 20 + this.random.nextInt(6);
                this.gravity = 0.20f;
                this.friction = 0.98f;
                this.quadSize = 0.08f;
                this.hasPhysics = true;
            }
            case STRAND -> {
                this.lifetime = 40 + this.random.nextInt(14);
                this.gravity = 0.004f;
                this.friction = 0.72f; // hard brake: flies out, then hangs like anchored silk
                this.quadSize = 0.11f;
                this.hasPhysics = false;
            }
            case LEAF -> {
                this.lifetime = 44 + this.random.nextInt(16);
                this.gravity = 0.018f;
                this.friction = 0.97f;
                this.quadSize = 0.11f;
                this.hasPhysics = false;
            }
            case SHADE -> {
                this.lifetime = 36 + this.random.nextInt(12);
                this.gravity = 0.008f; // sinks — darkness pools downward
                this.friction = 0.95f;
                this.quadSize = 0.12f;
                this.hasPhysics = false;
            }
            case CHIP -> {
                this.lifetime = 18 + this.random.nextInt(6);
                this.gravity = 0.28f;
                this.friction = 0.98f;
                this.quadSize = 0.07f;
                this.hasPhysics = true;
            }
            case SHARD -> {
                this.lifetime = 22 + this.random.nextInt(8);
                this.gravity = 0.18f;
                this.friction = 0.98f;
                this.quadSize = 0.08f;
                this.hasPhysics = true;
            }
            case STREAK -> {
                this.lifetime = 8 + this.random.nextInt(5);
                this.gravity = 0.0f;
                this.friction = 0.88f;
                this.quadSize = 0.16f;
                this.hasPhysics = false;
            }
            case FOXFIRE -> {
                this.lifetime = 24 + this.random.nextInt(8);
                this.gravity = -0.004f;
                this.friction = 0.93f;
                this.quadSize = 0.11f;
                this.hasPhysics = false;
            }
        }
        this.baseQuadSize = this.quadSize;
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed) {
            setSpriteFromAge(sprites);
            float life = (float) age / (float) lifetime;
            switch (behavior) {
                case WISP -> {
                    // Sinusoidal sideways sway so wisps feel like drifting spirits.
                    xd += Math.sin((age + 7) * 0.35) * 0.0015;
                    zd += Math.cos((age + 3) * 0.30) * 0.0015;
                }
                case SPARKLE -> quadSize = baseQuadSize * (0.7f + 0.5f * Mth.sin(age * 1.1f));
                case EMBER -> xd += Math.sin(age * 0.45) * 0.001;
                case LEAF -> {
                    // Wide falling-leaf sway plus a roll oscillation that reads as tumbling.
                    xd += Math.sin((age + 5) * 0.25) * 0.003;
                    zd += Math.cos((age + 11) * 0.22) * 0.003;
                    oRoll = roll;
                    roll = Mth.sin(age * 0.18f) * 0.6f;
                }
                case SHADE -> {
                    xd += Math.sin((age + 7) * 0.35) * 0.0015;
                    zd += Math.cos((age + 3) * 0.30) * 0.0015;
                }
                case SHARD -> {
                    // Fast tumble so the glass catches light at changing angles.
                    oRoll = roll;
                    roll += 0.45f;
                }
                case FOXFIRE -> {
                    // Nervous spirit-flame flicker plus a wide lateral wander; the flame
                    // flares up just before it gutters out.
                    xd += Math.sin((age + 13) * 0.55) * 0.0035;
                    zd += Math.cos((age + 2) * 0.50) * 0.0035;
                    float flicker = 0.85f + 0.25f * Mth.sin(age * 1.7f) * Mth.cos(age * 0.9f);
                    quadSize = baseQuadSize * (life > 0.8f ? flicker * 1.6f : flicker);
                }
                default -> { }
            }
            // Universal fade-out over the last third of the lifetime.
            if (life > 0.66f) {
                alpha = Mth.clamp(1.0f - (life - 0.66f) / 0.34f, 0.0f, 1.0f);
            }
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        // Magical particles are emissive; physical matter (drips, leaves, shadows,
        // bone chips) uses world lighting so it sits naturally in the scene.
        return switch (behavior) {
            case DRIP, LEAF, SHADE, CHIP, SHARD -> super.getLightColor(partialTick);
            default -> 0xF000F0;
        };
    }

    public record Provider(SpriteSet sprites, Behavior behavior) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new RunicParticle(level, x, y, z, dx, dy, dz, sprites, behavior);
        }
    }
}
