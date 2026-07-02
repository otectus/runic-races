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
        DRIP
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
        // All identity particles are magical/emissive except the venom drip.
        return behavior == Behavior.DRIP ? super.getLightColor(partialTick) : 0xF000F0;
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
