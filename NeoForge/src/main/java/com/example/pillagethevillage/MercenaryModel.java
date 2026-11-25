// SPDX-License-Identifier: LGPL-3.0-only
package com.example.pillagethevillage;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.monster.illager.AbstractIllager;

public class MercenaryModel extends EntityModel<MercenaryRenderState> implements ArmedModel<MercenaryRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(PillagetheVillage.MODID, "mercenary"), "main");

    private final ModelPart head;
    private final ModelPart arms;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart rightArm;
    private final ModelPart leftArm;

    public MercenaryModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.arms = root.getChild("arms");
        this.rightLeg = root.getChild("RightLeg");
        this.leftLeg = root.getChild("LeftLeg");
        this.rightArm = root.getChild("RightArm");
        this.leftArm = root.getChild("LeftArm");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(24, 0).addBox(-1.0F, -1.0F, -6.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offset(0.0F, -2.0F, 0.0F));
        head.addOrReplaceChild("helmet",
                CubeListBuilder.create().texOffs(32, 0)
                        .addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.5F)),
                PartPose.ZERO);

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 20).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 12.0F, 6.0F)
                        .texOffs(0, 38).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 18.0F, 6.0F, new CubeDeformation(0.5F)),
                PartPose.ZERO);

        root.addOrReplaceChild("arms",
                CubeListBuilder.create().texOffs(44, 22).addBox(-8.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F)
                        .texOffs(44, 22).addBox(4.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F)
                        .texOffs(40, 38).addBox(-4.0F, 2.0F, -2.0F, 8.0F, 4.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 2.0F, 0.0F, -0.7854F, 0.0F, 0.0F));

        root.addOrReplaceChild("RightLeg",
                CubeListBuilder.create().texOffs(0, 22).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(-2.0F, 12.0F, 0.0F));
        root.addOrReplaceChild("LeftLeg",
                CubeListBuilder.create().texOffs(0, 22).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(2.0F, 12.0F, 0.0F));
        root.addOrReplaceChild("RightArm",
                CubeListBuilder.create().texOffs(40, 46).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("LeftArm",
                CubeListBuilder.create().texOffs(40, 46).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(5.0F, 2.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(MercenaryRenderState state) {
        super.setupAnim(state);
        this.head.yRot = state.yRot * (Mth.PI / 180.0F);
        this.head.xRot = state.xRot * (Mth.PI / 180.0F);
        if (state.isRiding) {
            this.rightArm.xRot = this.leftArm.xRot = -Mth.PI / 5.0F;
            this.rightArm.yRot = this.rightArm.zRot = this.leftArm.yRot = this.leftArm.zRot = 0.0F;
            this.rightLeg.xRot = this.leftLeg.xRot = -1.4137167F;
            this.rightLeg.yRot = Mth.PI / 10.0F; this.leftLeg.yRot = -Mth.PI / 10.0F;
            this.rightLeg.zRot = 0.07853982F; this.leftLeg.zRot = -0.07853982F;
        } else {
            float speed = state.walkAnimationSpeed, pos = state.walkAnimationPos;
            this.rightArm.xRot = Mth.cos(pos * 0.6662F + Mth.PI) * 2.0F * speed * 0.5F;
            this.leftArm.xRot = Mth.cos(pos * 0.6662F) * 2.0F * speed * 0.5F;
            this.rightLeg.xRot = Mth.cos(pos * 0.6662F) * 1.4F * speed * 0.5F;
            this.leftLeg.xRot = Mth.cos(pos * 0.6662F + Mth.PI) * 1.4F * speed * 0.5F;
            this.rightArm.yRot = this.rightArm.zRot = this.leftArm.yRot = this.leftArm.zRot = 0.0F;
            this.rightLeg.yRot = this.rightLeg.zRot = this.leftLeg.yRot = this.leftLeg.zRot = 0.0F;
        }
        AbstractIllager.IllagerArmPose pose = state.armPose;
        boolean crossed = pose == AbstractIllager.IllagerArmPose.CROSSED;
        if (pose == AbstractIllager.IllagerArmPose.ATTACKING) {
            if (state.getMainHandItemState().isEmpty()) {
                AnimationUtils.animateZombieArms(this.leftArm, this.rightArm, true, state);
                AnimationUtils.bobArms(this.rightArm, this.leftArm, state.ageInTicks);
            } else AnimationUtils.swingWeaponDown(this.rightArm, this.leftArm, state.mainArm, state.swingAnimation, state.ageInTicks);
        } else if (pose == AbstractIllager.IllagerArmPose.BOW_AND_ARROW) {
            this.rightArm.yRot = -0.1F + this.head.yRot;
            this.rightArm.xRot = -Mth.HALF_PI + this.head.xRot;
            this.leftArm.xRot = -0.9424779F + this.head.xRot;
            this.leftArm.yRot = this.head.yRot - 0.4F;
            this.leftArm.zRot = Mth.HALF_PI;
        } else if (pose == AbstractIllager.IllagerArmPose.CROSSBOW_HOLD) {
            AnimationUtils.animateCrossbowHold(this.rightArm, this.leftArm, this.head, true);
        } else if (pose == AbstractIllager.IllagerArmPose.CROSSBOW_CHARGE) {
            AnimationUtils.animateCrossbowCharge(this.rightArm, this.leftArm,
                    state.maxCrossbowChargeDuration, state.ticksUsingItem, true);
        }
        this.arms.visible = crossed;
        this.leftArm.visible = this.rightArm.visible = !crossed;
    }

    @Override
    public void translateToHand(MercenaryRenderState state, HumanoidArm arm, PoseStack poseStack) {
        this.root().translateAndRotate(poseStack);
        ModelPart armPart = arm == HumanoidArm.LEFT ? this.leftArm : this.rightArm;
        armPart.translateAndRotate(poseStack);
    }
}


