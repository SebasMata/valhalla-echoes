package com.mataflex;

import com.mataflex.entity.DraugrRenderer;
import com.mataflex.entity.MysticalVikingRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.resources.Identifier;

public class ValhallaEchoesClient implements ClientModInitializer {

	public static final net.minecraft.client.model.geom.ModelLayerLocation HORNED_HELMET_LAYER =
			new net.minecraft.client.model.geom.ModelLayerLocation(
					Identifier.fromNamespaceAndPath(ValhallaEchoes.MOD_ID, "horned_helmet"), "main"
			);

	@Override
	public void onInitializeClient() {

		EntityRendererRegistry.register(
				ValhallaEchoes.MYSTICAL_VIKING,
				MysticalVikingRenderer::new
		);

		EntityRendererRegistry.register(
				ValhallaEchoes.DRAUGR,
				DraugrRenderer::new
		);

		ModelLayerRegistry.registerModelLayer(
				HORNED_HELMET_LAYER,
				com.mataflex.model.HornedHelmetModel::createArmorLayer
		);

		net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer.register((poseStack, submitNodeCollector, itemStack, humanoidRenderState, equipmentSlot, light, humanoidModel) -> {

			com.mataflex.model.HornedHelmetModel helmetModel = new com.mataflex.model.HornedHelmetModel(
					net.minecraft.client.Minecraft.getInstance().getEntityModels().bakeLayer(HORNED_HELMET_LAYER)
			);

			helmetModel.head.visible = true;
			helmetModel.hat.visible = false;

			Identifier texture = Identifier.fromNamespaceAndPath(
					ValhallaEchoes.MOD_ID,
					"textures/entity/equipment/humanoid/horned_helmet_texture.png"
			);

			net.minecraft.client.renderer.rendertype.RenderType renderLayer = helmetModel.renderType(texture);

            net.minecraft.client.renderer.OrderedSubmitNodeCollector orderedCollector = (net.minecraft.client.renderer.OrderedSubmitNodeCollector) submitNodeCollector;
            ArmorRenderer.submitTransformCopyingModel(
                    humanoidModel,
                    humanoidRenderState,
                    helmetModel,
                    humanoidRenderState,
                    true,
                    orderedCollector,
                    poseStack,
                    renderLayer,
                    light,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                    0xFFFFFFFF,
                    null,
                    0
            );

        }, com.mataflex.item.ModItems.HORNED_HELMET);
	}
}