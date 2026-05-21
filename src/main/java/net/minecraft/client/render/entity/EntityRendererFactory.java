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

@FunctionalInterface
@Environment(EnvType.CLIENT)
/**
 * {@code EntityRendererFactory}.
 */
public interface EntityRendererFactory<T extends Entity> {

	EntityRenderer<T, ?> create(EntityRendererFactory.Context ctx);

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Context}.
	 */
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
			return this.renderDispatcher;
		}

		public ItemModelManager getItemModelManager() {
			return this.itemModelManager;
		}

		public MapRenderer getMapRenderer() {
			return this.mapRenderer;
		}

		public BlockRenderManager getBlockRenderManager() {
			return this.blockRenderManager;
		}

		public ResourceManager getResourceManager() {
			return this.resourceManager;
		}

		public LoadedEntityModels getEntityModels() {
			return this.entityModels;
		}

		public EquipmentModelLoader getEquipmentModelLoader() {
			return this.equipmentModelLoader;
		}

		public EquipmentRenderer getEquipmentRenderer() {
			return this.equipmentRenderer;
		}

		public SpriteHolder getSpriteHolder() {
			return this.spriteHolder;
		}

		public SpriteAtlasTexture getSpriteAtlasTexture(Identifier id) {
			return this.spriteHolder.getAtlasTexture(id);
		}

		public ModelPart getPart(EntityModelLayer layer) {
			return this.entityModels.getModelPart(layer);
		}

		public TextRenderer getTextRenderer() {
			return this.textRenderer;
		}

		public PlayerSkinCache getPlayerSkinCache() {
			return this.playerSkinCache;
		}
	}
}
