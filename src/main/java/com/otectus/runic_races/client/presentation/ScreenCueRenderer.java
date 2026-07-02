package com.otectus.runic_races.client.presentation;

import com.mojang.blaze3d.systems.RenderSystem;
import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.presentation.CueType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumMap;
import java.util.Map;

/**
 * Client-side renderer for {@link CueType} screen overlays. State is a map of
 * active cue -> ticks remaining; {@code TickEvent.ClientTickEvent} decrements each tick,
 * {@code RenderGuiEvent.Pre} draws all non-expired cues on top of the HUD.
 *
 * Kept deliberately simple: each cue is a solid-color fullscreen quad with per-cue
 * alpha curve. The goal is a consistent audiovisual "stamp" for signature moments —
 * not a polished post-processing pipeline.
 */
@Mod.EventBusSubscriber(modid = RunicRacesMod.MOD_ID, value = Dist.CLIENT)
public final class ScreenCueRenderer {

    private static final Map<CueType, ActiveCue> ACTIVE = new EnumMap<>(CueType.class);

    private ScreenCueRenderer() {}

    public static void enqueue(CueType cue, int durationTicks) {
        if (cue == null || durationTicks <= 0) return;
        ACTIVE.put(cue, new ActiveCue(durationTicks, durationTicks));
        spawnHeavyFlourish(cue);
    }

    /**
     * Client-only theatrical extra for the mythic cues (revival, nine lives, glamour):
     * a burst of identity particles around the local player, on top of the
     * server-broadcast VFX. Gated by {@code effects.heavyEffects}.
     */
    private static void spawnHeavyFlourish(CueType cue) {
        if (!com.otectus.runic_races.config.RRClientConfig.HEAVY_EFFECTS_ENABLED.get()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        net.minecraft.core.particles.ParticleOptions particle = switch (cue) {
            case LIFE_RUNE_FLASH -> com.otectus.runic_races.registry.ModParticles.FAE_SPARKLE.get();
            case VIGNETTE_PULSE -> com.otectus.runic_races.registry.ModParticles.SOUL_WISP.get();
            default -> null;
        };
        if (particle == null) return;

        // Rising spiral: 24 particles over two turns around the player.
        double px = mc.player.getX();
        double py = mc.player.getY();
        double pz = mc.player.getZ();
        for (int i = 0; i < 24; i++) {
            double angle = (i / 24.0) * Math.PI * 4;
            double radius = 0.9;
            mc.level.addParticle(particle,
                    px + Math.cos(angle) * radius,
                    py + 0.1 + (i / 24.0) * 2.2,
                    pz + Math.sin(angle) * radius,
                    0, 0.02, 0);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ACTIVE.entrySet().removeIf(e -> {
            e.getValue().ticksRemaining--;
            return e.getValue().ticksRemaining <= 0;
        });
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (ACTIVE.isEmpty() || Minecraft.getInstance().level == null) return;
        GuiGraphics graphics = event.getGuiGraphics();
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        for (Map.Entry<CueType, ActiveCue> e : ACTIVE.entrySet()) {
            ActiveCue cue = e.getValue();
            float progress = 1.0f - ((float) cue.ticksRemaining / (float) cue.totalTicks);
            drawCue(graphics, e.getKey(), w, h, progress);
        }

        RenderSystem.disableBlend();
    }

    // White-with-alpha overlay textures (tools/generate_overlays.py) tinted per cue
    // via setShaderColor. simpleCues config falls back to the flat-rect path.
    private static final net.minecraft.resources.ResourceLocation VIGNETTE_TEX =
            new net.minecraft.resources.ResourceLocation(RunicRacesMod.MOD_ID, "textures/gui/overlay/vignette_radial.png");
    private static final net.minecraft.resources.ResourceLocation RIME_TEX =
            new net.minecraft.resources.ResourceLocation(RunicRacesMod.MOD_ID, "textures/gui/overlay/frost_rime.png");
    private static final net.minecraft.resources.ResourceLocation HAZE_TEX =
            new net.minecraft.resources.ResourceLocation(RunicRacesMod.MOD_ID, "textures/gui/overlay/heat_haze.png");

    /** Fullscreen blit of a white overlay texture tinted (rgb, alpha). */
    private static void blitTinted(GuiGraphics graphics, net.minecraft.resources.ResourceLocation tex,
                                   int w, int h, int rgb, float alpha) {
        RenderSystem.setShaderColor(
                ((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f, alpha);
        graphics.blit(tex, 0, 0, w, h, 0f, 0f, 256, 256, 256, 256);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    /** Radial vignette (textured) or flat edge bars (simpleCues) in the cue's tint. */
    private static void vignette(GuiGraphics graphics, int w, int h, int rgb, float alpha, int edgeDiv) {
        if (!com.otectus.runic_races.config.RRClientConfig.SIMPLE_CUES.get()) {
            blitTinted(graphics, VIGNETTE_TEX, w, h, rgb, alpha);
            return;
        }
        int color = (int) (alpha * 255) << 24 | rgb;
        int edge = Math.min(w, h) / edgeDiv;
        graphics.fill(0, 0, w, edge, color);
        graphics.fill(0, h - edge, w, h, color);
        graphics.fill(0, 0, edge, h, color);
        graphics.fill(w - edge, 0, w, h, color);
    }

    private static void drawCue(GuiGraphics graphics, CueType cue, int w, int h, float progress) {
        // progress is 0.0 at enqueue, 1.0 at expiry
        float intensity = com.otectus.runic_races.config.RRClientConfig.SCREEN_CUE_INTENSITY.get().floatValue();
        if (intensity <= 0f) return;
        boolean simple = com.otectus.runic_races.config.RRClientConfig.SIMPLE_CUES.get();
        switch (cue) {
            case FREEZE_FRAME -> {
                // Brief white flash, fades out fast
                float alpha = clamp((1.0f - progress) * 0.5f * intensity);
                int color = (int) (alpha * 255) << 24 | 0xFFFFFF;
                graphics.fill(0, 0, w, h, color);
            }
            case HEARTBEAT_FLASH -> {
                float pulse = (float) Math.sin(progress * Math.PI);
                vignette(graphics, w, h, 0xAA0000, clamp(pulse * 0.6f * intensity), 6);
            }
            case VIGNETTE_PULSE -> {
                float pulse = (float) Math.sin(progress * Math.PI);
                vignette(graphics, w, h, 0x220033, clamp(pulse * 0.45f * intensity), 4);
            }
            case HEAT_SHIMMER -> {
                float alpha = clamp((1.0f - progress) * 0.3f * intensity);
                if (simple) {
                    int color = (int) (alpha * 255) << 24 | 0xFF4400;
                    graphics.fill(0, 0, w, h, color);
                    break;
                }
                // Faint orange radial glow plus three haze strips wobbling sideways and
                // scrolling upward across the lower two-thirds — honest wobble, no shaders.
                blitTinted(graphics, VIGNETTE_TEX, w, h, 0xFF4400, alpha * 0.5f);
                long time = Minecraft.getInstance().level != null
                        ? Minecraft.getInstance().level.getGameTime() : 0L;
                RenderSystem.setShaderColor(1f, 0.45f, 0.2f, alpha * 0.8f);
                for (int strip = 0; strip < 3; strip++) {
                    int stripH = h / 5;
                    int y = h / 3 + strip * stripH + stripH / 2;
                    int xOff = (int) (Math.sin((time + strip * 7) * 0.35 + strip * 2.1) * 6);
                    float vScroll = (time * 3 + strip * 40) % 256;
                    graphics.blit(HAZE_TEX, xOff - 8, y, w + 16, stripH, 0f, vScroll, 64, 64, 64, 256);
                }
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            }
            case SHAKE -> {
                // No screen overlay — real camera shake is applied by CameraShakeHandler,
                // which reads this cue's remaining-ticks via shakeAmplitude01().
            }
            case LIFE_RUNE_FLASH -> {
                float pulse = (float) Math.sin(progress * Math.PI);
                float alpha = clamp(pulse * 0.55f * intensity);
                int color = (int) (alpha * 255) << 24 | 0xFFDD55;
                int size = Math.min(w, h) / 4;
                int cx = w / 2 - size / 2;
                int cy = h / 2 - size / 2;
                graphics.fill(cx, cy, cx + size, cy + size, color);
            }
            case MOON_GLOW -> {
                float pulse = (float) Math.sin(progress * Math.PI);
                vignette(graphics, w, h, 0xCFE0FF, clamp(pulse * 0.40f * intensity), 5);
            }
            case FROST_RIME -> {
                // Crystalline rime creeping in from the edges, peaking mid-cue.
                float pulse = (float) Math.sin(progress * Math.PI);
                float alpha = clamp(pulse * 0.55f * intensity);
                if (simple) {
                    vignette(graphics, w, h, 0xA9E8FF, clamp(pulse * 0.45f * intensity), 4);
                } else {
                    blitTinted(graphics, RIME_TEX, w, h, 0xA9E8FF, alpha);
                }
            }
            case WIND_STREAK -> {
                // Horizontal speed streaks along the left/right screen edges, fading fast.
                // Reads as air rushing past during a dash/leap without obscuring the center.
                float alpha = clamp((1.0f - progress) * 0.45f * intensity);
                int color = (int) (alpha * 255) << 24 | 0xE8F4FF;
                int band = Math.max(2, h / 90);
                int edgeWidth = w / 4;
                for (int i = 0; i < 5; i++) {
                    // Streak rows spread vertically; length shrinks toward screen center.
                    int y = h / 6 + i * h / 8;
                    int len = edgeWidth - i * (edgeWidth / 7);
                    graphics.fill(0, y, len, y + band, color);
                    graphics.fill(w - len, h - y - band, w, h - y, color);
                }
            }
        }
    }

    /**
     * Current camera-shake intensity in [0,1] — 1.0 just after a {@link CueType#SHAKE} cue is
     * enqueued, decaying linearly to 0 at expiry. Read by {@code CameraShakeHandler} each frame.
     */
    public static float shakeAmplitude01() {
        ActiveCue cue = ACTIVE.get(CueType.SHAKE);
        if (cue == null || cue.totalTicks <= 0) return 0f;
        return clamp((float) cue.ticksRemaining / (float) cue.totalTicks);
    }

    /**
     * Signed FOV kick derived from active cues, read each frame by {@code FovKickHandler}.
     * Positive pushes the FOV out (breath heat, wind rush); negative punches in (impacts).
     * Each cue's contribution eases out over its lifetime; the sum is clamped to ±6%.
     */
    public static float fovKickDelta() {
        float delta = 0f;
        delta += kick(CueType.HEAT_SHIMMER, 0.04f);
        delta += kick(CueType.WIND_STREAK, 0.03f);
        delta += kick(CueType.SHAKE, -0.03f);
        delta += kick(CueType.FREEZE_FRAME, -0.05f);
        return Mth.clamp(delta, -0.06f, 0.06f);
    }

    private static float kick(CueType cue, float magnitude) {
        ActiveCue active = ACTIVE.get(cue);
        if (active == null || active.totalTicks <= 0) return 0f;
        float remaining = clamp((float) active.ticksRemaining / (float) active.totalTicks);
        return magnitude * remaining * remaining; // ease-out square
    }

    private static float clamp(float v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    private static final class ActiveCue {
        final int totalTicks;
        int ticksRemaining;

        ActiveCue(int totalTicks, int ticksRemaining) {
            this.totalTicks = totalTicks;
            this.ticksRemaining = ticksRemaining;
        }
    }
}
