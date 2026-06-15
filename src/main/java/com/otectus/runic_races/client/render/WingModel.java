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
 * Wing model using the same 64x32 texture layout and box geometry as vanilla elytra.
 * Two wing parts that animate based on player state: folded, spread (gliding), or flapping.
 */
@OnlyIn(Dist.CLIENT)
public class WingModel extends EntityModel<AbstractClientPlayer> {

    private final ModelPart leftWing;
    private final ModelPart rightWing;

    public WingModel(ModelPart root) {
        this.leftWing = root.getChild("left_wing");
        this.rightWing = root.getChild("right_wing");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("left_wing",
                CubeListBuilder.create()
                        .texOffs(22, 0)
                        .addBox(-10.0F, 0.0F, 0.0F, 10.0F, 20.0F, 2.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(5.0F, 0.0F, 0.0F,
                        Mth.PI / 12.0F, 0.0F, -Mth.PI / 12.0F));

        root.addOrReplaceChild("right_wing",
                CubeListBuilder.create()
                        .texOffs(22, 0).mirror()
                        .addBox(0.0F, 0.0F, 0.0F, 10.0F, 20.0F, 2.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-5.0F, 0.0F, 0.0F,
                        Mth.PI / 12.0F, 0.0F, Mth.PI / 12.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        leftWing.xRot = Mth.PI / 12.0F;
        leftWing.yRot = 0.0F;
        leftWing.zRot = -Mth.PI / 12.0F;
        rightWing.xRot = Mth.PI / 12.0F;
        rightWing.yRot = 0.0F;
        rightWing.zRot = Mth.PI / 12.0F;
    }

    /**
     * Apply pre-computed rotation values from the render layer's per-player animation state.
     */
    public void applyRotation(float leftXRot, float leftYRot, float leftZRot,
                              float rightXRot, float rightYRot, float rightZRot) {
        leftWing.xRot = leftXRot;
        leftWing.yRot = leftYRot;
        leftWing.zRot = leftZRot;
        rightWing.xRot = rightXRot;
        rightWing.yRot = rightYRot;
        rightWing.zRot = rightZRot;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        leftWing.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        rightWing.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
