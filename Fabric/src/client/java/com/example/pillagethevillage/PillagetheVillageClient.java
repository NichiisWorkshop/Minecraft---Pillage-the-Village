// SPDX-License-Identifier: LGPL-3.0-only
package com.example.pillagethevillage;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.item.CrossbowItem;

public final class PillagetheVillageClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModelLayerRegistry.registerModelLayer(MercenaryModel.LAYER_LOCATION, MercenaryModel::createBodyLayer);
        EntityRendererRegistry.register(PillagetheVillage.MERCENARY, MercenaryRenderer::new);
    }

    private static final class MercenaryRenderer extends MobRenderer<Mercenary, MercenaryRenderState, MercenaryModel> {
        private static final Identifier BASE = Identifier.fromNamespaceAndPath(PillagetheVillage.MODID,
                "textures/entity/mercenary/mercenary.png");

        private MercenaryRenderer(net.minecraft.client.renderer.entity.EntityRendererProvider.Context context) {
            super(context, new MercenaryModel(context.bakeLayer(MercenaryModel.LAYER_LOCATION)), 0.5F);
            this.addLayer(new MercenaryVariantLayer(this));
            this.addLayer(new MercenaryItemInHandLayer(this));
        }

        @Override
        public void extractRenderState(Mercenary entity, MercenaryRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            ArmedEntityRenderState.extractArmedEntityRenderState(entity, state, this.itemModelResolver, partialTicks);
            state.isRiding = entity.isPassenger();
            state.isAggressive = entity.isAggressive();
            state.armPose = entity.getMainHandItem().getItem() instanceof CrossbowItem
                    ? entity.getArmPose()
                    : state.isAggressive ? entity.getArmPose() : AbstractIllager.IllagerArmPose.CROSSED;
            state.maxCrossbowChargeDuration = state.armPose == AbstractIllager.IllagerArmPose.CROSSBOW_CHARGE
                    ? CrossbowItem.getChargeDuration(entity.getUseItem(), entity) : 0;
            state.ticksUsingItem = entity.getTicksUsingItem(partialTicks);
            state.variant = entity.getSkinVariant();
        }

        @Override
        public MercenaryRenderState createRenderState() {
            return new MercenaryRenderState();
        }

        @Override
        protected void scale(MercenaryRenderState state, PoseStack poseStack) {
            poseStack.scale(0.98F, 0.98F, 0.98F);
        }

        @Override
        public Identifier getTextureLocation(MercenaryRenderState state) {
            return BASE;
        }
    }

    private static final class MercenaryVariantLayer extends RenderLayer<MercenaryRenderState, MercenaryModel> {
        private MercenaryVariantLayer(MercenaryRenderer renderer) { super(renderer); }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                MercenaryRenderState state, float yRot, float xRot) {
            if (state.variant == null || state.variant.isBlank()) return;
            Identifier texture = Identifier.fromNamespaceAndPath(PillagetheVillage.MODID,
                    "textures/entity/mercenary/types/" + state.variant + ".png");
            coloredCutoutModelCopyLayerRender(this.getParentModel(), texture, poseStack, collector, light, state, -1, 1);
        }
    }

    private static final class MercenaryItemInHandLayer extends ItemInHandLayer<MercenaryRenderState, MercenaryModel> {
        private MercenaryItemInHandLayer(MercenaryRenderer renderer) { super(renderer); }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, MercenaryRenderState state,
                float yRot, float xRot) {
            if (state.armPose != AbstractIllager.IllagerArmPose.CROSSED) {
                super.submit(poseStack, collector, light, state, yRot, xRot);
            }
        }
    }
}
