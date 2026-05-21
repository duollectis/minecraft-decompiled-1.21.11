package net.minecraft.client.render.entity.feature;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.render.entity.equipment.EquipmentModelLoader;
import net.minecraft.client.render.entity.model.*;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.item.ItemStack;

@Environment(EnvType.CLIENT)
/**
 * {@code CapeFeatureRenderer}.
 */
public class CapeFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {

	private final BipedEntityModel<PlayerEntityRenderState> model;
	private final EquipmentModelLoader equipmentModelLoader;

	public CapeFeatureRenderer(
			FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context,
			LoadedEntityModels modelLoader,
			EquipmentModelLoader equipmentModelLoader
	) {
		super(context);
		this.model = new PlayerCapeModel(modelLoader.getModelPart(EntityModelLayers.PLAYER_CAPE));
		this.equipmentModelLoader = equipmentModelLoader;
	}

	private boolean hasCustomModelForLayer(ItemStack stack, EquipmentModel.LayerType layerType) {
		EquippableComponent equippableComponent = stack.get(DataComponentTypes.EQUIPPABLE);
		if (equippableComponent != null && !equippableComponent.assetId().isEmpty()) {
			EquipmentModel equipmentModel = this.equipmentModelLoader.get(equippableComponent.assetId().get());
			return !equipmentModel.getLayers(layerType).isEmpty();
		}
		else {
			return false;
		}
	}

	public void render(
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			int i,
			PlayerEntityRenderState playerEntityRenderState,
			float f,
			float g
	) {
		if (!playerEntityRenderState.invisible && playerEntityRenderState.capeVisible) {
			SkinTextures skinTextures = playerEntityRenderState.skinTextures;
			if (skinTextures.cape() != null) {
				if (!this.hasCustomModelForLayer(
						playerEntityRenderState.equippedChestStack,
						EquipmentModel.LayerType.WINGS
				)) {
					matrixStack.push();
					if (this.hasCustomModelForLayer(
							playerEntityRenderState.equippedChestStack,
							EquipmentModel.LayerType.HUMANOID
					)) {
						matrixStack.translate(0.0F, -0.053125F, 0.06875F);
					}

					orderedRenderCommandQueue.submitModel(
							this.model,
							playerEntityRenderState,
							matrixStack,
							RenderLayers.entitySolid(skinTextures.cape().texturePath()),
							i,
							OverlayTexture.DEFAULT_UV,
							playerEntityRenderState.outlineColor,
							null
					);
					matrixStack.pop();
				}
			}
		}
	}
}
