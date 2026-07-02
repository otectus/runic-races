package com.otectus.runic_races.client.presentation;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.config.RRClientConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Applies the cue-driven FOV kick (see {@link ScreenCueRenderer#fovKickDelta()}):
 * drake breaths push the view out a touch, impacts punch it in. Mirrors the
 * camera-shake pattern — the cue channel owns timing, this handler only reads it.
 *
 * Gated by {@code effects.fovEffects} and scaled by {@code effects.screenCueIntensity};
 * the total modifier delta is already clamped to ±6% at the source.
 */
@Mod.EventBusSubscriber(modid = RunicRacesMod.MOD_ID, value = Dist.CLIENT)
public final class FovKickHandler {

    private FovKickHandler() {}

    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        if (!RRClientConfig.FOV_EFFECTS_ENABLED.get()) return;
        float intensity = RRClientConfig.SCREEN_CUE_INTENSITY.get().floatValue();
        if (intensity <= 0f) return;

        float delta = ScreenCueRenderer.fovKickDelta() * intensity;
        if (delta != 0f) {
            event.setNewFovModifier(event.getNewFovModifier() * (1.0f + delta));
        }
    }
}
