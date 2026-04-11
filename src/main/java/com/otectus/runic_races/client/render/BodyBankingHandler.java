package com.otectus.runic_races.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.util.RaceHelper;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.WeakHashMap;

/**
 * Applies a subtle body roll (Z-axis banking) to the player model when turning during elytra flight.
 * Operates via {@code RenderLivingEvent.Pre} to rotate the PoseStack before the player model renders.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = RunicRacesMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BodyBankingHandler {

    private static final WeakHashMap<AbstractClientPlayer, BankState> bankStates = new WeakHashMap<>();
    private static final float DEG_TO_RAD = Mth.PI / 180.0F;
    private static final float MAX_BANK_DEGREES = 20.0F;
    private static final float BANK_SENSITIVITY = 0.8F;
    private static final float SMOOTH_FACTOR = 0.12F;

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) return;
        if (!player.isFallFlying()) return;

        // Only apply to players with non-cosmetic wings
        boolean hasFlightWings = RaceHelper.getRaceName(player)
                .flatMap(WingType::forRaceName)
                .map(wt -> !wt.isCosmeticOnly())
                .orElse(false);
        if (!hasFlightWings) return;

        BankState bs = bankStates.computeIfAbsent(player, p -> new BankState());

        float currentYaw = player.yBodyRot;
        if (Float.isNaN(bs.prevYaw)) {
            bs.prevYaw = currentYaw;
        }

        float yawDelta = Mth.wrapDegrees(currentYaw - bs.prevYaw);
        bs.smoothedYawDelta += (yawDelta - bs.smoothedYawDelta) * SMOOTH_FACTOR;
        bs.prevYaw = currentYaw;

        float bankDeg = Mth.clamp(bs.smoothedYawDelta * BANK_SENSITIVITY, -MAX_BANK_DEGREES, MAX_BANK_DEGREES);
        float bankRad = bankDeg * DEG_TO_RAD;

        if (Math.abs(bankRad) > 0.001F) {
            PoseStack poseStack = event.getPoseStack();
            // Pivot around torso center for natural roll
            poseStack.translate(0.0, 0.9, 0.0);
            poseStack.mulPose(Axis.ZP.rotation(bankRad));
            poseStack.translate(0.0, -0.9, 0.0);
        }
    }

    private static class BankState {
        float prevYaw = Float.NaN;
        float smoothedYawDelta = 0.0F;
    }
}
