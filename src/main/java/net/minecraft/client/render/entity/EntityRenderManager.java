package net.minecraft.client.render.entity;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientMannequinEntity;
import net.minecraft.client.network.ClientPlayerLikeEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.MapRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.equipment.EquipmentModelLoader;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.AtlasManager;
import net.minecraft.client.texture.PlayerSkinCache;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class EntityRenderManager implements SynchronousResourceReloader {
   private Map<EntityType<?>, EntityRenderer<?, ?>> renderers = ImmutableMap.of();
   private Map<PlayerSkinType, PlayerEntityRenderer<AbstractClientPlayerEntity>> playerRenderers = Map.of();
   private Map<PlayerSkinType, PlayerEntityRenderer<ClientMannequinEntity>> mannequinRenderers = Map.of();
   public final TextureManager textureManager;
   public @Nullable Camera camera;
   public Entity targetedEntity;
   private final ItemModelManager itemModelManager;
   private final MapRenderer mapRenderer;
   private final BlockRenderManager blockRenderManager;
   private final HeldItemRenderer heldItemRenderer;
   private final AtlasManager atlasManager;
   private final TextRenderer textRenderer;
   public final GameOptions gameOptions;
   private final Supplier<LoadedEntityModels> entityModelsGetter;
   private final EquipmentModelLoader equipmentModelLoader;
   private final PlayerSkinCache skinCache;

   public <E extends Entity> int getLight(E entity, float tickProgress) {
      return this.getRenderer(entity).getLight(entity, tickProgress);
   }

   public EntityRenderManager(
      MinecraftClient client,
      TextureManager textureManager,
      ItemModelManager itemModelManager,
      MapRenderer mapRenderer,
      BlockRenderManager blockRenderManager,
      AtlasManager atlasManager,
      TextRenderer textRenderer,
      GameOptions gameOptions,
      Supplier<LoadedEntityModels> supplier,
      EquipmentModelLoader equipmentModelLoader,
      PlayerSkinCache playerSkinCache
   ) {
      this.textureManager = textureManager;
      this.itemModelManager = itemModelManager;
      this.mapRenderer = mapRenderer;
      this.atlasManager = atlasManager;
      this.skinCache = playerSkinCache;
      this.heldItemRenderer = new HeldItemRenderer(client, this, itemModelManager);
      this.blockRenderManager = blockRenderManager;
      this.textRenderer = textRenderer;
      this.gameOptions = gameOptions;
      this.entityModelsGetter = supplier;
      this.equipmentModelLoader = equipmentModelLoader;
   }

   @SuppressWarnings("unchecked")
   public <T extends Entity> EntityRenderer<? super T, ?> getRenderer(T entity) {
      return (EntityRenderer<? super T, ?>)(switch (entity) {
         case AbstractClientPlayerEntity abstractClientPlayerEntity -> this.getPlayerRenderer(
            (Map)(Map)this.playerRenderers, abstractClientPlayerEntity
         );
         case ClientMannequinEntity clientMannequinEntity -> this.getPlayerRenderer(
            (Map)(Map)this.mannequinRenderers, clientMannequinEntity
         );
         default -> (EntityRenderer)this.renderers.get(entity.getType());
      });
   }

   public PlayerEntityRenderer<AbstractClientPlayerEntity> getPlayerRenderer(AbstractClientPlayerEntity player) {
      return this.getPlayerRenderer(this.playerRenderers, player);
   }

   private <T extends PlayerLikeEntity & ClientPlayerLikeEntity> PlayerEntityRenderer<T> getPlayerRenderer(
      Map<PlayerSkinType, PlayerEntityRenderer<T>> skinTypeToRenderer, T player
   ) {
      PlayerSkinType playerSkinType = player.getSkin().model();
      PlayerEntityRenderer<T> playerEntityRenderer = skinTypeToRenderer.get(playerSkinType);
      return playerEntityRenderer != null ? playerEntityRenderer : skinTypeToRenderer.get(PlayerSkinType.WIDE);
   }

   public <S extends EntityRenderState> EntityRenderer<?, ? super S> getRenderer(S state) {
      if (state instanceof PlayerEntityRenderState playerEntityRenderState) {
         PlayerSkinType playerSkinType = playerEntityRenderState.skinTextures.model();
         EntityRenderer<? extends PlayerLikeEntity, ?> entityRenderer = (EntityRenderer<? extends PlayerLikeEntity, ?>)this.playerRenderers.get(playerSkinType);
         return (EntityRenderer<?, ? super S>)(entityRenderer != null ? entityRenderer : (EntityRenderer)this.playerRenderers.get(PlayerSkinType.WIDE));
      } else {
         return (EntityRenderer<?, ? super S>)this.renderers.get(state.entityType);
      }
   }

   public void configure(Camera camera, Entity targetedEntity) {
      this.camera = camera;
      this.targetedEntity = targetedEntity;
   }

   public <E extends Entity> boolean shouldRender(E entity, Frustum frustum, double x, double y, double z) {
      EntityRenderer<? super E, ?> entityRenderer = this.getRenderer(entity);
      return entityRenderer.shouldRender(entity, frustum, x, y, z);
   }

   public <E extends Entity> EntityRenderState getAndUpdateRenderState(E entity, float tickProgress) {
      EntityRenderer<? super E, ?> entityRenderer = this.getRenderer(entity);

      try {
         return entityRenderer.getAndUpdateRenderState(entity, tickProgress);
      } catch (Throwable var8) {
         CrashReport crashReport = CrashReport.create(var8, "Extracting render state for an entity in world");
         CrashReportSection crashReportSection = crashReport.addElement("Entity being extracted");
         entity.populateCrashReport(crashReportSection);
         CrashReportSection crashReportSection2 = this.addRendererDetails(entityRenderer, crashReport);
         crashReportSection2.add("Delta", tickProgress);
         throw new CrashException(crashReport);
      }
   }

   public <S extends EntityRenderState> void render(
      S renderState,
      CameraRenderState cameraRenderState,
      double d,
      double e,
      double f,
      MatrixStack matrixStack,
      OrderedRenderCommandQueue orderedRenderCommandQueue
   ) {
      EntityRenderer<?, ? super S> entityRenderer = this.getRenderer(renderState);

      try {
         Vec3d vec3d = entityRenderer.getPositionOffset(renderState);
         double g = d + vec3d.getX();
         double h = e + vec3d.getY();
         double i = f + vec3d.getZ();
         matrixStack.push();
         matrixStack.translate(g, h, i);
         entityRenderer.render(renderState, matrixStack, orderedRenderCommandQueue, cameraRenderState);
         if (renderState.onFire) {
            orderedRenderCommandQueue.submitFire(
               matrixStack, renderState, MathHelper.rotateAround(MathHelper.Y_AXIS, cameraRenderState.orientation, new Quaternionf())
            );
         }

         if (renderState instanceof PlayerEntityRenderState) {
            matrixStack.translate(-vec3d.getX(), -vec3d.getY(), -vec3d.getZ());
         }

         if (!renderState.shadowPieces.isEmpty()) {
            orderedRenderCommandQueue.submitShadowPieces(matrixStack, renderState.shadowRadius, renderState.shadowPieces);
         }

         if (!(renderState instanceof PlayerEntityRenderState)) {
            matrixStack.translate(-vec3d.getX(), -vec3d.getY(), -vec3d.getZ());
         }

         matrixStack.pop();
      } catch (Throwable var19) {
         CrashReport crashReport = CrashReport.create(var19, "Rendering entity in world");
         CrashReportSection crashReportSection = crashReport.addElement("EntityRenderState being rendered");
         renderState.addCrashReportDetails(crashReportSection);
         this.addRendererDetails(entityRenderer, crashReport);
         throw new CrashException(crashReport);
      }
   }

   private <S extends EntityRenderState> CrashReportSection addRendererDetails(EntityRenderer<?, S> renderer, CrashReport crashReport) {
      CrashReportSection crashReportSection = crashReport.addElement("Renderer details");
      crashReportSection.add("Assigned renderer", renderer);
      return crashReportSection;
   }

   public void clearCamera() {
      this.camera = null;
   }

   public double getSquaredDistanceToCamera(Entity entity) {
      return this.camera.getCameraPos().squaredDistanceTo(entity.getEntityPos());
   }

   public HeldItemRenderer getHeldItemRenderer() {
      return this.heldItemRenderer;
   }

   @Override
   public void reload(ResourceManager manager) {
      EntityRendererFactory.Context context = new EntityRendererFactory.Context(
         this,
         this.itemModelManager,
         this.mapRenderer,
         this.blockRenderManager,
         manager,
         this.entityModelsGetter.get(),
         this.equipmentModelLoader,
         this.atlasManager,
         this.textRenderer,
         this.skinCache
      );
      this.renderers = EntityRendererFactories.reloadEntityRenderers(context);
      this.playerRenderers = EntityRendererFactories.reloadPlayerRenderers(context);
      this.mannequinRenderers = EntityRendererFactories.reloadPlayerRenderers(context);
   }
}
