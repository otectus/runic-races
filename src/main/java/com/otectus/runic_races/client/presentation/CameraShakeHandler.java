package com.otectus.runic_races.client.presentation;

import com.otectus.runic_races.RunicRacesMod;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Applies real camera shake (not a GUI overlay) while a {@link com.otectus.runic_races.presentation.CueType#SHAKE}
 * cue is active. Subscribes to Forge's {@link ViewportEvent.ComputeCameraAngles} so no mixin is
 * required — the view roll/pitch are perturbed each frame by a high-frequency oscillation whose
 * amplitude decays with the cue (see {@link ScreenCueRenderer#shakeAmplitude01()}).
 */
@Mod.EventBusSubscriber(modid = RunicRacesMod.MOD_ID, value = Dist.CLIENT)
public final class CameraShakeHandler {

    /** Peak shake magnitude in degrees at full intensity. */
    private static final float MAX_DEGREES = 2.4f;

    private CameraShakeHandler() {}

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!com.otectus.runic_races.config.RRClientConfig.CAMERA_SHAKE_ENABLED.get()) return;

        float amp = ScreenCueRenderer.shakeAmplitude01();
        if (amp <= 0f) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        float intensity = com.otectus.runic_races.config.RRClientConfig.SCREEN_CUE_INTENSITY.get().floatValue();
        if (intensity <= 0f) return;

        // Smooth per-frame time base so the shake doesn't stutter at low tick rates.
        double t = mc.level.getGameTime() + event.getPartialTick();
        float mag = amp * amp * MAX_DEGREES * intensity; // ease-out: falls off faster near the end

        float roll = (float) Math.sin(t * 39.0) * mag;
        float pitch = (float) Math.cos(t * 31.0) * mag * 0.5f;

        event.setRoll(event.getRoll() + roll);
        event.setPitch(event.getPitch() + pitch);
    }
}
