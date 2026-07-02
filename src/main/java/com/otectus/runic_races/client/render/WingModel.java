package com.otectus.runic_races.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Articulated wing model. One bake contains all three silhouettes as separate
 * part groups; {@link #setSilhouette} toggles visibility so a single
 * {@code ModelLayerLocation} serves every winged race:
 *
 * <ul>
 *   <li><b>MEMBRANE</b> (wyvern + drakes) — inner arm box with a nested outer
 *       membrane tip that lags the arm (cascaded transform via child part).</li>
 *   <li><b>FEATHERED</b> (avian) — arm + lagging primary feathers + static
 *       covert overlay, layered for plumage depth.</li>
 *   <li><b>GOSSAMER</b> (sprite/faerie) — independent fore/hind wing pairs;
 *       the hindwing runs a phase behind the forewing.</li>
 * </ul>
 *
 * Texture sheet is 64x64 (grown from the 64x32 elytra layout in v1.4.0).
 * UV islands (see tools/generate_wings.py, which paints them):
 * <pre>
 *   membrane arm  (0,0)  5x20x2      membrane tip (14,0) 6x20x2
 *   feather arm   (30,0) 4x18x2      primaries    (42,0) 7x20x1
 *   coverts       (0,24) 5x12x1      forewing     (12,24) 8x14x1
 *   hindwing      (30,24) 6x10x1
 * </pre>
 */
@OnlyIn(Dist.CLIENT)
public class WingModel extends EntityModel<AbstractClientPlayer> {

    /** Which part group a {@link WingType} renders. */
    public enum Silhouette { MEMBRANE, FEATHERED, GOSSAMER }

    private static final float REST_X = Mth.PI / 12.0F;
    private static final float REST_Z = Mth.PI / 12.0F;

    private final ModelPart leftMembraneArm;
    private final ModelPart rightMembraneArm;
    private final ModelPart leftMembraneTip;
    private final ModelPart rightMembraneTip;

    private final ModelPart leftFeatherArm;
    private final ModelPart rightFeatherArm;
    private final ModelPart leftPrimaries;
    private final ModelPart rightPrimaries;

    private final ModelPart leftForewing;
    private final ModelPart rightForewing;
    private final ModelPart leftHindwing;
    private final ModelPart rightHindwing;

    public WingModel(ModelPart root) {
        this.leftMembraneArm = root.getChild("left_membrane_arm");
        this.rightMembraneArm = root.getChild("right_membrane_arm");
        this.leftMembraneTip = leftMembraneArm.getChild("left_membrane_tip");
        this.rightMembraneTip = rightMembraneArm.getChild("right_membrane_tip");

        this.leftFeatherArm = root.getChild("left_feather_arm");
        this.rightFeatherArm = root.getChild("right_feather_arm");
        this.leftPrimaries = leftFeatherArm.getChild("left_primaries");
        this.rightPrimaries = rightFeatherArm.getChild("right_primaries");

        this.leftForewing = root.getChild("left_forewing");
        this.rightForewing = root.getChild("right_forewing");
        this.leftHindwing = root.getChild("left_hindwing");
        this.rightHindwing = root.getChild("right_hindwing");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ===== MEMBRANE (wyvern/drakes): arm + lagging outer membrane =====
        PartDefinition leftMemArm = root.addOrReplaceChild("left_membrane_arm",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-5.0F, 0.0F, 0.0F, 5.0F, 20.0F, 2.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(5.0F, 0.0F, 0.0F, REST_X, 0.0F, -REST_Z));
        // Tip pivots at the arm's outer edge; +0.1 Z keeps its faces off the arm's (z-fight guard).
        leftMemArm.addOrReplaceChild("left_membrane_tip",
                CubeListBuilder.create().texOffs(14, 0)
                        .addBox(-6.0F, 0.0F, 0.0F, 6.0F, 20.0F, 2.0F, new CubeDeformation(-0.05F)),
                PartPose.offset(-5.0F, 0.0F, 0.1F));

        PartDefinition rightMemArm = root.addOrReplaceChild("right_membrane_arm",
                CubeListBuilder.create().texOffs(0, 0).mirror()
                        .addBox(0.0F, 0.0F, 0.0F, 5.0F, 20.0F, 2.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-5.0F, 0.0F, 0.0F, REST_X, 0.0F, REST_Z));
        rightMemArm.addOrReplaceChild("right_membrane_tip",
                CubeListBuilder.create().texOffs(14, 0).mirror()
                        .addBox(0.0F, 0.0F, 0.0F, 6.0F, 20.0F, 2.0F, new CubeDeformation(-0.05F)),
                PartPose.offset(5.0F, 0.0F, 0.1F));

        // ===== FEATHERED (avian): arm + lagging primaries + static coverts overlay =====
        PartDefinition leftFeaArm = root.addOrReplaceChild("left_feather_arm",
                CubeListBuilder.create().texOffs(30, 0)
                        .addBox(-4.0F, 0.0F, 0.0F, 4.0F, 18.0F, 2.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(5.0F, 0.0F, 0.0F, REST_X, 0.0F, -REST_Z));
        leftFeaArm.addOrReplaceChild("left_primaries",
                CubeListBuilder.create().texOffs(42, 0)
                        .addBox(-7.0F, 0.0F, 0.15F, 7.0F, 20.0F, 1.0F, new CubeDeformation(-0.05F)),
                PartPose.offset(-4.0F, 0.0F, 0.0F));
        leftFeaArm.addOrReplaceChild("left_coverts",
                CubeListBuilder.create().texOffs(0, 24)
                        .addBox(-5.0F, 0.0F, 0.3F, 5.0F, 12.0F, 1.0F, new CubeDeformation(-0.1F)),
                PartPose.offset(-1.0F, 0.0F, 0.0F));

        PartDefinition rightFeaArm = root.addOrReplaceChild("right_feather_arm",
                CubeListBuilder.create().texOffs(30, 0).mirror()
                        .addBox(0.0F, 0.0F, 0.0F, 4.0F, 18.0F, 2.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-5.0F, 0.0F, 0.0F, REST_X, 0.0F, REST_Z));
        rightFeaArm.addOrReplaceChild("right_primaries",
                CubeListBuilder.create().texOffs(42, 0).mirror()
                        .addBox(0.0F, 0.0F, 0.15F, 7.0F, 20.0F, 1.0F, new CubeDeformation(-0.05F)),
                PartPose.offset(4.0F, 0.0F, 0.0F));
        rightFeaArm.addOrReplaceChild("right_coverts",
                CubeListBuilder.create().texOffs(0, 24).mirror()
                        .addBox(0.0F, 0.0F, 0.3F, 5.0F, 12.0F, 1.0F, new CubeDeformation(-0.1F)),
                PartPose.offset(1.0F, 0.0F, 0.0F));

        // ===== GOSSAMER (sprite/faerie): independent fore/hind pairs =====
        root.addOrReplaceChild("left_forewing",
                CubeListBuilder.create().texOffs(12, 24)
                        .addBox(-8.0F, 0.0F, 0.0F, 8.0F, 14.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(5.0F, -1.0F, 0.0F, REST_X, 0.0F, -REST_Z));
        root.addOrReplaceChild("right_forewing",
                CubeListBuilder.create().texOffs(12, 24).mirror()
                        .addBox(0.0F, 0.0F, 0.0F, 8.0F, 14.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-5.0F, -1.0F, 0.0F, REST_X, 0.0F, REST_Z));
        root.addOrReplaceChild("left_hindwing",
                CubeListBuilder.create().texOffs(30, 24)
                        .addBox(-6.0F, 0.0F, 0.0F, 6.0F, 10.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(5.0F, 4.0F, 0.15F, REST_X, 0.0F, -REST_Z));
        root.addOrReplaceChild("right_hindwing",
                CubeListBuilder.create().texOffs(30, 24).mirror()
                        .addBox(0.0F, 0.0F, 0.0F, 6.0F, 10.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-5.0F, 4.0F, 0.15F, REST_X, 0.0F, REST_Z));

        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Show only the given silhouette's part group. */
    public void setSilhouette(Silhouette silhouette) {
        boolean membrane = silhouette == Silhouette.MEMBRANE;
        boolean feathered = silhouette == Silhouette.FEATHERED;
        boolean gossamer = silhouette == Silhouette.GOSSAMER;
        leftMembraneArm.visible = membrane;
        rightMembraneArm.visible = membrane;
        leftFeatherArm.visible = feathered;
        rightFeatherArm.visible = feathered;
        leftForewing.visible = gossamer;
        rightForewing.visible = gossamer;
        leftHindwing.visible = gossamer;
        rightHindwing.visible = gossamer;
    }

    @Override
    public void setupAnim(AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        applyPose(Silhouette.MEMBRANE, REST_X, 0.0F, -REST_Z, 0.0F, REST_Z, 0.0F, 0.0F, 0.0F, 0.0F);
    }

    /**
     * Applies the render layer's smoothed pose. Main rotations drive the arm (or
     * forewing); {@code tipDelta} is the cascaded-lag residual applied to the
     * nested tip/primaries part; {@code hindDelta} offsets the gossamer hindwing
     * behind the forewing. Deltas are radians relative to the parent part.
     */
    public void applyPose(Silhouette silhouette,
                          float xRot,
                          float leftYRot, float leftZRot,
                          float rightYRot, float rightZRot,
                          float leftTipDelta, float rightTipDelta,
                          float leftHindDelta, float rightHindDelta) {
        switch (silhouette) {
            case MEMBRANE -> {
                setPart(leftMembraneArm, xRot, leftYRot, leftZRot);
                setPart(rightMembraneArm, xRot, rightYRot, rightZRot);
                leftMembraneTip.zRot = leftTipDelta;
                rightMembraneTip.zRot = rightTipDelta;
            }
            case FEATHERED -> {
                setPart(leftFeatherArm, xRot, leftYRot, leftZRot);
                setPart(rightFeatherArm, xRot, rightYRot, rightZRot);
                leftPrimaries.zRot = leftTipDelta;
                rightPrimaries.zRot = rightTipDelta;
            }
            case GOSSAMER -> {
                setPart(leftForewing, xRot, leftYRot, leftZRot);
                setPart(rightForewing, xRot, rightYRot, rightZRot);
                setPart(leftHindwing, xRot, leftYRot, leftZRot + leftHindDelta);
                setPart(rightHindwing, xRot, rightYRot, rightZRot + rightHindDelta);
            }
        }
    }

    private static void setPart(ModelPart part, float xRot, float yRot, float zRot) {
        part.xRot = xRot;
        part.yRot = yRot;
        part.zRot = zRot;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        // ModelPart.render skips invisible parts, so rendering every root respects setSilhouette.
        leftMembraneArm.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        rightMembraneArm.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        leftFeatherArm.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        rightFeatherArm.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        leftForewing.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        rightForewing.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        leftHindwing.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        rightHindwing.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
