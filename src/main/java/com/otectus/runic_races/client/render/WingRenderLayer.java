package com.otectus.runic_races.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.util.RaceHelper;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.WeakHashMap;

/**
 * Renders racial wings on the player's back based on their selected Origin race.
 * <p>
 * Wing animation has four states (priority order):
 * <ul>
 *   <li><b>FLAP</b> — Sharp downbeat triggered by upward velocity spike.</li>
 *   <li><b>HOVER</b> — Rapid flutter when airborne with low velocity.</li>
 *   <li><b>GLIDE</b> — Wings spread with dynamic oscillation, banking, and sweep.</li>
 *   <li><b>GROUND</b> — Wings folded with walk sway and breathing idle.</li>
 * </ul>
 * All states support banking on turns, velocity-responsive pitch, and wind buffeting.
 */
@OnlyIn(Dist.CLIENT)
public class WingRenderLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public static final ModelLayerLocation WING_MODEL_LAYER =
            new ModelLayerLocation(new ResourceLocation(RunicRacesMod.MOD_ID, "wings"), "main");

    private final WingModel wingModel;
    private final WeakHashMap<AbstractClientPlayer, WingAnimState> animStates = new WeakHashMap<>();

    private static final float DEG_TO_RAD = Mth.PI / 180.0F;
    private static final int RACE_CACHE_TICKS = 100;

    private static final double FLAP_VELOCITY_THRESHOLD = 0.15;
    private static final float STATE_LERP_SPEED = 0.12F;
    private static final float MAX_BANK_DEG = 25.0F;

    public WingRenderLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                           EntityModelSet modelSet) {
        super(parent);
        this.wingModel = new WingModel(modelSet.bakeLayer(WING_MODEL_LAYER));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {

        if (player.isInvisible()) return;
        if (player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) return;

        WingAnimState state = animStates.computeIfAbsent(player, p -> new WingAnimState());
        int currentTick = player.tickCount;

        // --- Refresh cached wing type periodically ---
        if (state.wingType == null && state.lastCheckTick == 0
                || currentTick - state.lastCheckTick > RACE_CACHE_TICKS) {
            state.wingType = RaceHelper.getRaceName(player)
                    .flatMap(WingType::forRaceName)
                    .orElse(null);
            state.lastCheckTick = currentTick;
        }

        WingType wingType = state.wingType;
        if (wingType == null) return;

        // --- State detection ---
        boolean airborne = !player.onGround();
        boolean isFallFlying = player.isFallFlying();
        boolean isGliding = airborne && isFallFlying && !wingType.isCosmeticOnly();
        double currentY = player.getDeltaMovement().y;

        // --- Flap detection ---
        if (airborne && !wingType.isCosmeticOnly()
                && currentY > FLAP_VELOCITY_THRESHOLD
                && state.prevDeltaY <= FLAP_VELOCITY_THRESHOLD
                && state.flapTicksRemaining <= 0) {
            state.flapTicksRemaining = wingType.getFlapDurationTicks();
        }
        state.prevDeltaY = currentY;

        // --- Per-tick updates (not per-frame) ---
        if (currentTick != state.lastAnimTick) {
            if (state.flapTicksRemaining > 0) {
                state.flapTicksRemaining--;
            }

            // Velocity smoothing
            double dx = player.getDeltaMovement().x;
            double dz = player.getDeltaMovement().z;
            float currentHorizSpeed = (float) Math.sqrt(dx * dx + dz * dz);
            state.smoothedHorizSpeed += (currentHorizSpeed - state.smoothedHorizSpeed) * 0.1F;
            state.smoothedVertSpeed += ((float) currentY - state.smoothedVertSpeed) * 0.1F;

            // Yaw tracking for banking
            float currentYBodyRot = player.yBodyRot;
            if (Float.isNaN(state.prevYBodyRot)) {
                state.prevYBodyRot = currentYBodyRot;
            }
            float rawYawDelta = Mth.wrapDegrees(currentYBodyRot - state.prevYBodyRot);
            state.smoothedYawDelta += (rawYawDelta - state.smoothedYawDelta) * 0.15F;
            state.prevYBodyRot = currentYBodyRot;

            // Breathing phase (always ticks, used in ground state)
            state.breathPhase += 0.025F;

            // Noise seed init
            if (state.noiseSeed == 0) {
                state.noiseSeed = (long) player.getId() * 31L + 7L;
            }

            state.lastAnimTick = currentTick;
        }

        // --- Hover detection ---
        boolean isHovering = airborne && isFallFlying && !wingType.isCosmeticOnly()
                && state.smoothedHorizSpeed < 0.05F
                && Math.abs(state.smoothedVertSpeed) < 0.2F;

        // --- Compute targets ---
        float targetXRot;
        float targetLeftZRot;
        float targetRightZRot;
        float targetLeftYRot = 0.0F;
        float targetRightYRot = 0.0F;

        // Banking offset (shared across glide/hover states)
        float bankAngle = state.smoothedYawDelta * wingType.getBankingSensitivity() * DEG_TO_RAD;
        bankAngle = Mth.clamp(bankAngle, -MAX_BANK_DEG * DEG_TO_RAD, MAX_BANK_DEG * DEG_TO_RAD);

        if (state.flapTicksRemaining > 0) {
            // ===== FLAP STATE =====
            float flapDuration = wingType.getFlapDurationTicks();
            float progress = 1.0F - (state.flapTicksRemaining / flapDuration);
            float downbeatZ = wingType.getDownbeatZDeg() * DEG_TO_RAD;
            float spreadZ = wingType.getSpreadZDeg() * DEG_TO_RAD;

            float baseZ;
            if (progress < 0.3F) {
                float t = progress / 0.3F;
                baseZ = Mth.lerp(t, spreadZ, downbeatZ);
            } else {
                float t = (progress - 0.3F) / 0.7F;
                t = 1.0F - (1.0F - t) * (1.0F - t); // ease-out
                baseZ = Mth.lerp(t, downbeatZ, spreadZ);
            }
            targetXRot = Mth.PI / 12.0F - 0.25F;
            targetLeftZRot = baseZ;
            targetRightZRot = -baseZ;

            // Wing sweep during flap: forward on downstroke, back on return
            float flapSweep;
            if (progress < 0.3F) {
                flapSweep = Mth.lerp(progress / 0.3F, 0.0F, 10.0F * DEG_TO_RAD);
            } else {
                flapSweep = Mth.lerp((progress - 0.3F) / 0.7F, 10.0F * DEG_TO_RAD, 0.0F);
            }
            targetLeftYRot = -flapSweep;
            targetRightYRot = flapSweep;

            // Apply directly (no lerp) for snappy flap response
            state.smoothedXRot = targetXRot;
            state.smoothedLeftZRot = targetLeftZRot;
            state.smoothedRightZRot = targetRightZRot;
            state.smoothedLeftYRot = targetLeftYRot;
            state.smoothedRightYRot = targetRightYRot;

        } else if (isHovering) {
            // ===== HOVER STATE =====
            float flutterAmp = wingType.getHoverFlutterAmplitudeDeg() * DEG_TO_RAD;
            float flutter = Mth.sin(ageInTicks * 0.6F) * flutterAmp;
            float hoverBase = (wingType.getFoldedZDeg() + wingType.getSpreadZDeg()) * 0.5F * DEG_TO_RAD;

            targetXRot = Mth.PI / 12.0F - 0.1F;
            targetLeftZRot = hoverBase + flutter - bankAngle;
            targetRightZRot = -(hoverBase + flutter) + bankAngle;
            targetLeftYRot = 0.0F;
            targetRightYRot = 0.0F;

            state.smoothedXRot += (targetXRot - state.smoothedXRot) * STATE_LERP_SPEED;
            state.smoothedLeftZRot += (targetLeftZRot - state.smoothedLeftZRot) * STATE_LERP_SPEED * 2.0F;
            state.smoothedRightZRot += (targetRightZRot - state.smoothedRightZRot) * STATE_LERP_SPEED * 2.0F;
            state.smoothedLeftYRot += (targetLeftYRot - state.smoothedLeftYRot) * STATE_LERP_SPEED;
            state.smoothedRightYRot += (targetRightYRot - state.smoothedRightYRot) * STATE_LERP_SPEED;

        } else if (isGliding) {
            // ===== GLIDE STATE =====

            // Dynamic oscillation: speed-responsive frequency and amplitude
            float speedNorm = Mth.clamp(state.smoothedHorizSpeed * 5.0F, 0.0F, 1.0F);
            float oscFreq = Mth.lerp(speedNorm, 0.08F, 0.03F);
            float oscAmp = Mth.lerp(speedNorm, 5.0F * DEG_TO_RAD, 2.0F * DEG_TO_RAD);
            float glideOsc = Mth.sin(ageInTicks * oscFreq) * oscAmp;

            // Velocity-responsive pitch
            float vertPitchOffset = Mth.clamp(-state.smoothedVertSpeed * 0.4F, -0.5F, 0.5F);
            targetXRot = Mth.PI / 12.0F + vertPitchOffset;

            // Base Z rotation with oscillation
            float baseZ = wingType.getSpreadZDeg() * DEG_TO_RAD + glideOsc;
            targetLeftZRot = baseZ - bankAngle;
            targetRightZRot = -baseZ + bankAngle;

            // Wind buffeting at high speed
            if (state.smoothedHorizSpeed > 0.3F) {
                float speedFactor = Mth.clamp((state.smoothedHorizSpeed - 0.3F) * 3.0F, 0.0F, 1.0F);
                float buffetScale = wingType.getWindBuffetScale() * 4.0F * DEG_TO_RAD * speedFactor;
                targetLeftZRot += pseudoNoise(ageInTicks, state.noiseSeed) * buffetScale;
                targetRightZRot += pseudoNoise(ageInTicks + 100, state.noiseSeed) * buffetScale;
            }

            // Wing sweep Y: swept back at speed, forward when slow
            float sweepAngle = Mth.lerp(Mth.clamp(state.smoothedHorizSpeed * 4.0F, 0.0F, 1.0F),
                    5.0F * DEG_TO_RAD, -20.0F * DEG_TO_RAD);
            targetLeftYRot = -sweepAngle;
            targetRightYRot = sweepAngle;

            state.smoothedXRot += (targetXRot - state.smoothedXRot) * STATE_LERP_SPEED;
            state.smoothedLeftZRot += (targetLeftZRot - state.smoothedLeftZRot) * STATE_LERP_SPEED;
            state.smoothedRightZRot += (targetRightZRot - state.smoothedRightZRot) * STATE_LERP_SPEED;
            state.smoothedLeftYRot += (targetLeftYRot - state.smoothedLeftYRot) * STATE_LERP_SPEED;
            state.smoothedRightYRot += (targetRightYRot - state.smoothedRightYRot) * STATE_LERP_SPEED;

        } else {
            // ===== GROUND STATE =====
            float walkSway = Mth.sin(limbSwing * 0.6F) * limbSwingAmount * 5.0F * DEG_TO_RAD;

            // Breathing idle: subtle wing movement when stationary
            float breathFactor = 1.0F - Mth.clamp(limbSwingAmount * 10.0F, 0.0F, 1.0F);
            float breathOffset = Mth.sin(state.breathPhase) * 2.5F * DEG_TO_RAD * breathFactor;

            targetXRot = Mth.PI / 12.0F;
            float baseZ = wingType.getFoldedZDeg() * DEG_TO_RAD + walkSway + breathOffset;
            targetLeftZRot = baseZ;
            targetRightZRot = -baseZ;
            targetLeftYRot = 0.0F;
            targetRightYRot = 0.0F;

            if (player.isCrouching()) {
                targetLeftZRot *= 0.7F;
                targetRightZRot *= 0.7F;
                targetXRot += 0.15F;
            }

            state.smoothedXRot += (targetXRot - state.smoothedXRot) * STATE_LERP_SPEED;
            state.smoothedLeftZRot += (targetLeftZRot - state.smoothedLeftZRot) * STATE_LERP_SPEED;
            state.smoothedRightZRot += (targetRightZRot - state.smoothedRightZRot) * STATE_LERP_SPEED;
            state.smoothedLeftYRot += (targetLeftYRot - state.smoothedLeftYRot) * STATE_LERP_SPEED;
            state.smoothedRightYRot += (targetRightYRot - state.smoothedRightYRot) * STATE_LERP_SPEED;
        }

        // --- Apply to model ---
        wingModel.applyRotation(
                state.smoothedXRot, state.smoothedLeftYRot, state.smoothedLeftZRot,
                state.smoothedXRot, state.smoothedRightYRot, state.smoothedRightZRot
        );

        // --- Render ---
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, 0.125F);

        float scale = wingType.getScale();
        poseStack.scale(scale, scale, scale);

        ResourceLocation texture = new ResourceLocation(RunicRacesMod.MOD_ID, wingType.getTexturePath());
        RenderType renderType = wingType.isTranslucent()
                ? RenderType.entityTranslucent(texture)
                : RenderType.entityCutoutNoCull(texture);
        VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);

        float alpha = wingType.isTranslucent() ? 0.85F : 1.0F;
        wingModel.renderToBuffer(poseStack, vertexConsumer, packedLight,
                OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, alpha);

        poseStack.popPose();
    }

    /**
     * Deterministic pseudo-random noise from layered sine waves at irrational frequency ratios.
     */
    private static float pseudoNoise(float time, long seed) {
        float a = Mth.sin(time * 0.37F + (seed & 0xFF) * 0.1F);
        float b = Mth.sin(time * 0.71F + ((seed >> 8) & 0xFF) * 0.1F);
        float c = Mth.sin(time * 1.13F + ((seed >> 16) & 0xFF) * 0.1F);
        return (a + b * 0.7F + c * 0.3F) / 2.0F;
    }

    /**
     * Per-player animation state for smooth wing transitions and flight dynamics.
     */
    private static class WingAnimState {
        WingType wingType;
        int lastCheckTick;

        // Smoothed rotations (per-wing)
        float smoothedXRot = Mth.PI / 12.0F;
        float smoothedLeftZRot = -Mth.PI / 12.0F;
        float smoothedRightZRot = Mth.PI / 12.0F;
        float smoothedLeftYRot = 0.0F;
        float smoothedRightYRot = 0.0F;

        // Flap detection
        double prevDeltaY;
        int flapTicksRemaining;
        int lastAnimTick;

        // Velocity tracking
        float smoothedHorizSpeed;
        float smoothedVertSpeed;

        // Yaw tracking for banking
        float prevYBodyRot = Float.NaN;
        float smoothedYawDelta;

        // Wind buffeting
        long noiseSeed;

        // Breathing idle
        float breathPhase;
    }
}
