package net.minecraft.client.render.block.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.texture.PlayerSkinCache;
import net.minecraft.client.texture.SpriteHolder;

@FunctionalInterface
@Environment(EnvType.CLIENT)
/**
 * {@code BlockEntityRendererFactory}.
 */
public interface BlockEntityRendererFactory<T extends BlockEntity, S extends BlockEntityRenderState> {

	BlockEntityRenderer<T, S> create(BlockEntityRendererFactory.Context ctx);

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Context}.
	 */
	public record Context(
			BlockEntityRenderManager renderDispatcher,
			BlockRenderManager renderManager,
			ItemModelManager itemModelManager,
			ItemRenderer itemRenderer,
			EntityRenderManager entityRenderDispatcher,
			LoadedEntityModels loadedEntityModels,
			TextRenderer textRenderer,
			SpriteHolder spriteHolder,
			PlayerSkinCache playerSkinRenderCache
	) {

		public ModelPart getLayerModelPart(EntityModelLayer modelLayer) {
			return this.loadedEntityModels.getModelPart(modelLayer);
		}
	}
}
