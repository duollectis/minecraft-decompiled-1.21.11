package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.MapRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.entity.equipment.EquipmentModelLoader;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.texture.AtlasManager;
import net.minecraft.client.texture.PlayerSkinCache;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.entity.Entity;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Atlases;
import net.minecraft.util.Identifier;

/**
 * Фабрика для создания рендереров сущностей.
 * <p>
 * Функциональный интерфейс: принимает {@link Context} со всеми необходимыми
 * зависимостями и возвращает готовый {@link EntityRenderer}. Регистрируется
 * в {@code EntityRenderers} для каждого типа сущности.
 */
@FunctionalInterface
@Environment(EnvType.CLIENT)
public interface EntityRendererFactory<T extends Entity> {

	EntityRenderer<T, ?> create(EntityRendererFactory.Context ctx);

	/**
	 * Контейнер зависимостей, передаваемый фабрике при создании рендерера.
	 * <p>
	 * Агрегирует все клиентские сервисы, необходимые для инициализации
	 * рендереров сущностей: менеджеры моделей, текстур, шрифтов и т.д.
	 */
	@Environment(EnvType.CLIENT)
	public static class Context {

		private final EntityRenderManager renderDispatcher;
		private final ItemModelManager itemModelManager;
		private final MapRenderer mapRenderer;
		private final BlockRenderManager blockRenderManager;
		private final ResourceManager resourceManager;
		private final LoadedEntityModels entityModels;
		private final EquipmentModelLoader equipmentModelLoader;
		private final TextRenderer textRenderer;
		private final EquipmentRenderer equipmentRenderer;
		private final AtlasManager spriteHolder;
		private final PlayerSkinCache playerSkinCache;

		public Context(
				EntityRenderManager renderDispatcher,
				ItemModelManager itemRenderer,
				MapRenderer mapRenderer,
				BlockRenderManager blockRenderManager,
				ResourceManager resourceManager,
				LoadedEntityModels entityModels,
				EquipmentModelLoader equipmentModelLoader,
				AtlasManager atlasManager,
				TextRenderer textRenderer,
				PlayerSkinCache playerSkinCache
		) {
			this.renderDispatcher = renderDispatcher;
			this.itemModelManager = itemRenderer;
			this.mapRenderer = mapRenderer;
			this.blockRenderManager = blockRenderManager;
			this.resourceManager = resourceManager;
			this.entityModels = entityModels;
			this.equipmentModelLoader = equipmentModelLoader;
			this.textRenderer = textRenderer;
			this.spriteHolder = atlasManager;
			this.playerSkinCache = playerSkinCache;
			this.equipmentRenderer =
					new EquipmentRenderer(equipmentModelLoader, atlasManager.getAtlasTexture(Atlases.ARMOR_TRIMS));
		}

		public EntityRenderManager getRenderDispatcher() {
			return renderDispatcher;
		}

		public ItemModelManager getItemModelManager() {
			return itemModelManager;
		}

		public MapRenderer getMapRenderer() {
			return mapRenderer;
		}

		public BlockRenderManager getBlockRenderManager() {
			return blockRenderManager;
		}

		public ResourceManager getResourceManager() {
			return resourceManager;
		}

		public LoadedEntityModels getEntityModels() {
			return entityModels;
		}

		public EquipmentModelLoader getEquipmentModelLoader() {
			return equipmentModelLoader;
		}

		public EquipmentRenderer getEquipmentRenderer() {
			return equipmentRenderer;
		}

		public SpriteHolder getSpriteHolder() {
			return spriteHolder;
		}

		public SpriteAtlasTexture getSpriteAtlasTexture(Identifier id) {
			return spriteHolder.getAtlasTexture(id);
		}

		public ModelPart getPart(EntityModelLayer layer) {
			return entityModels.getModelPart(layer);
		}

		public TextRenderer getTextRenderer() {
			return textRenderer;
		}

		public PlayerSkinCache getPlayerSkinCache() {
			return playerSkinCache;
		}
	}
}
