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
 * Pixel-scale articulated wings. A shared 64x64 atlas supplies three physical
 * silhouettes: ribbed/scalloped membranes, overlapping individual feathers, and
 * stepped fore/hind gossamer lobes. Every dimension and UV stays on Minecraft's
 * one-texel-per-model-unit grid; small depth offsets separate overlapping layers.
 * UV boxes are mirrored by tools/generate_wings.py's ISLANDS contract.
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
        for (boolean mirror : new boolean[]{false, true}) {
            addMembrane(mesh.getRoot(), mirror);
            addFeathers(mesh.getRoot(), mirror);
            addGossamer(mesh.getRoot(), mirror);
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void addMembrane(PartDefinition root, boolean mirror) {
        String side = mirror ? "right" : "left";
        float sign = mirror ? -1.0F : 1.0F;
        PartDefinition arm = root.addOrReplaceChild(side + "_membrane_arm",
                box(mirror, 0, 0, -5, 1, 0.35F, 5, 15, 1),
                PartPose.offsetAndRotation(sign * 5, 0, 0, REST_X, 0, -sign * REST_Z));
        arm.addOrReplaceChild(side + "_shoulder",
                box(mirror, 30, 0, -1.5F, -0.5F, 0.1F, 3, 7, 2), PartPose.ZERO);
        arm.addOrReplaceChild(side + "_inner_spar",
                box(mirror, 42, 0, -5.3F, 0, 0.85F, 2, 13, 1), PartPose.ZERO);
        PartDefinition tip = arm.addOrReplaceChild(side + "_membrane_tip",
                box(mirror, 14, 0, -6, 0, 0.45F, 6, 17, 1),
                PartPose.offset(sign * -5, 0, 0));
        // Raised finger bones follow the outer membrane, framing its cutout
        // scallops; the projecting claw adds another step to the silhouette.
        for (int i = 0; i < 3; i++) {
            tip.addOrReplaceChild(side + "_finger_" + i,
                    box(mirror, 50, 0, -0.5F, 0, 1.05F, 1, 15, 1),
                    PartPose.offsetAndRotation(sign * (-5.5F + i * 2.6F), 0, 0,
                            0, 0, sign * (i - 1) * 0.07F));
        }
        tip.addOrReplaceChild(side + "_wing_claw",
                box(mirror, 56, 0, -6.25F, -2.5F, 0.9F, 2, 4, 1), PartPose.ZERO);
    }

    private static void addFeathers(PartDefinition root, boolean mirror) {
        String side = mirror ? "right" : "left";
        float sign = mirror ? -1.0F : 1.0F;
        PartDefinition arm = root.addOrReplaceChild(side + "_feather_arm",
                box(mirror, 0, 24, -4, 0, 0, 4, 14, 2),
                PartPose.offsetAndRotation(sign * 5, 0, 0, REST_X, 0, -sign * REST_Z));
        PartDefinition primaries = arm.addOrReplaceChild(side + "_primaries",
                CubeListBuilder.create(), PartPose.offset(sign * -3.5F, 1, 0));
        // Four separated cuboid flight feathers make a stepped fan with real depth.
        int[] lengths = {12, 15, 17, 15};
        int[] uv = {30, 22, 14, 22};
        for (int i = 0; i < lengths.length; i++) {
            primaries.addOrReplaceChild(side + "_primary_" + i,
                    box(mirror, uv[i], 24, -2, 0, 0, 2, lengths[i], 1),
                    PartPose.offsetAndRotation(sign * -i * 1.65F, -i * 0.65F, 0.3F + i * 0.12F,
                            0, 0, sign * (i - 1) * 0.075F));
        }
        // Coverts sit above the arm surface and overlap the primary roots.
        for (int i = 0; i < 3; i++) {
            arm.addOrReplaceChild(side + "_covert_" + i,
                    box(mirror, 38, 24, -2, 0, 0, 2, 7, 1),
                    PartPose.offsetAndRotation(sign * -i * 1.6F, 1 + i * 0.8F, 1.65F + i * 0.06F,
                            0, 0, sign * 0.12F));
        }
    }

    private static void addGossamer(PartDefinition root, boolean mirror) {
        String side = mirror ? "right" : "left";
        float sign = mirror ? -1.0F : 1.0F;
        PartDefinition fore = root.addOrReplaceChild(side + "_forewing",
                box(mirror, 0, 48, -6, 0, 0, 6, 12, 1),
                PartPose.offsetAndRotation(sign * 5, -1, 0, REST_X, 0, -sign * REST_Z));
        fore.addOrReplaceChild(side + "_fore_tip",
                box(mirror, 16, 48, -8.5F, -1.5F, 0.12F, 3, 7, 1), PartPose.ZERO);
        fore.addOrReplaceChild(side + "_fore_vein",
                box(mirror, 48, 48, -5.65F, 0.5F, 0.6F, 1, 9, 1), PartPose.ZERO);
        PartDefinition hind = root.addOrReplaceChild(side + "_hindwing",
                box(mirror, 26, 48, -5, 0, 0, 5, 8, 1),
                PartPose.offsetAndRotation(sign * 5, 6, 0.45F, REST_X, 0, -sign * REST_Z));
        hind.addOrReplaceChild(side + "_hind_tip",
                box(mirror, 40, 48, -6.5F, 5, 0.12F, 2, 5, 1), PartPose.ZERO);
    }

    /** Reflect geometry and UVs together so both wings keep their outward-facing art. */
    private static CubeListBuilder box(boolean mirror, int u, int v,
                                       float x, float y, float z, float width, float height, float depth) {
        return CubeListBuilder.create().texOffs(u, v).mirror(mirror)
                .addBox(mirror ? -x - width : x, y, z, width, height, depth, CubeDeformation.NONE);
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
                leftMembraneTip.zRot = Mth.clamp(leftTipDelta, -0.65F, 0.65F);
                rightMembraneTip.zRot = Mth.clamp(rightTipDelta, -0.65F, 0.65F);
                leftMembraneTip.yRot = -0.08F + leftTipDelta * 0.16F;
                rightMembraneTip.yRot = 0.08F + rightTipDelta * 0.16F;
            }
            case FEATHERED -> {
                setPart(leftFeatherArm, xRot, leftYRot, leftZRot);
                setPart(rightFeatherArm, xRot, rightYRot, rightZRot);
                leftPrimaries.zRot = Mth.clamp(leftTipDelta, -0.6F, 0.6F);
                rightPrimaries.zRot = Mth.clamp(rightTipDelta, -0.6F, 0.6F);
                leftPrimaries.yRot = -0.12F;
                rightPrimaries.yRot = 0.12F;
            }
            case GOSSAMER -> {
                setPart(leftForewing, xRot, leftYRot, leftZRot);
                setPart(rightForewing, xRot, rightYRot, rightZRot);
                setPart(leftHindwing, xRot + 0.08F, leftYRot - 0.12F, leftZRot + leftHindDelta - 0.24F);
                setPart(rightHindwing, xRot + 0.08F, rightYRot + 0.12F, rightZRot + rightHindDelta + 0.24F);
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
