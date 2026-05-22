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

/**
 * Фабрика рендереров блок-энтити. Функциональный интерфейс, создающий
 * {@link BlockEntityRenderer} из контекста с зависимостями рендеринга.
 * Регистрируется в {@code BlockEntityRendererFactories} для каждого типа блок-энтити.
 *
 * @param <T> тип блок-энтити
 * @param <S> тип состояния рендеринга блок-энтити
 */
@FunctionalInterface
@Environment(EnvType.CLIENT)
public interface BlockEntityRendererFactory<T extends BlockEntity, S extends BlockEntityRenderState> {

	BlockEntityRenderer<T, S> create(BlockEntityRendererFactory.Context ctx);

	/**
	 * Контекст с зависимостями, доступными рендереру блок-энтити при создании.
	 * Предоставляет доступ к менеджерам рендеринга, шрифтам, моделям и текстурам.
	 */
	@Environment(EnvType.CLIENT)
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
			return loadedEntityModels.getModelPart(modelLayer);
		}
	}
}
