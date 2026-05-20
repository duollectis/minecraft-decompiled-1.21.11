package net.minecraft.client.render.model;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.item.ItemAsset;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.client.render.item.model.MissingItemModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.PlayerSkinCache;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;
import net.minecraft.util.thread.AsyncHelper;
import org.joml.Vector3fc;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class ModelBaker {
   public static final SpriteIdentifier FIRE_0 = TexturedRenderLayers.BLOCK_SPRITE_MAPPER.mapVanilla("fire_0");
   public static final SpriteIdentifier FIRE_1 = TexturedRenderLayers.BLOCK_SPRITE_MAPPER.mapVanilla("fire_1");
   public static final SpriteIdentifier field_64594 = TexturedRenderLayers.BLOCK_SPRITE_MAPPER.mapVanilla("lava_still");
   public static final SpriteIdentifier LAVA_FLOW = TexturedRenderLayers.BLOCK_SPRITE_MAPPER.mapVanilla("lava_flow");
   public static final SpriteIdentifier field_64595 = TexturedRenderLayers.BLOCK_SPRITE_MAPPER.mapVanilla("water_still");
   public static final SpriteIdentifier WATER_FLOW = TexturedRenderLayers.BLOCK_SPRITE_MAPPER.mapVanilla("water_flow");
   public static final SpriteIdentifier WATER_OVERLAY = TexturedRenderLayers.BLOCK_SPRITE_MAPPER.mapVanilla("water_overlay");
   public static final SpriteIdentifier BANNER_BASE = new SpriteIdentifier(
      TexturedRenderLayers.BANNER_PATTERNS_ATLAS_TEXTURE, Identifier.ofVanilla("entity/banner_base")
   );
   public static final SpriteIdentifier SHIELD_BASE = new SpriteIdentifier(
      TexturedRenderLayers.SHIELD_PATTERNS_ATLAS_TEXTURE, Identifier.ofVanilla("entity/shield_base")
   );
   public static final SpriteIdentifier SHIELD_BASE_NO_PATTERN = new SpriteIdentifier(
      TexturedRenderLayers.SHIELD_PATTERNS_ATLAS_TEXTURE, Identifier.ofVanilla("entity/shield_base_nopattern")
   );
   public static final int MAX_BLOCK_DESTRUCTION_STAGE = 10;
   public static final List<Identifier> BLOCK_DESTRUCTION_STAGES = IntStream.range(0, 10)
      .mapToObj(stage -> Identifier.ofVanilla("block/destroy_stage_" + stage))
      .collect(Collectors.toList());
   public static final List<Identifier> BLOCK_DESTRUCTION_STAGE_TEXTURES = BLOCK_DESTRUCTION_STAGES.stream()
      .map(id -> id.withPath(path -> "textures/" + path + ".png"))
      .collect(Collectors.toList());
   public static final List<RenderLayer> BLOCK_DESTRUCTION_RENDER_LAYERS = BLOCK_DESTRUCTION_STAGE_TEXTURES.stream()
      .map(RenderLayers::crumbling)
      .collect(Collectors.toList());
   static final Logger LOGGER = LogUtils.getLogger();
   private final LoadedEntityModels entityModels;
   private final SpriteHolder field_61869;
   private final PlayerSkinCache field_62265;
   private final Map<BlockState, BlockStateModel.UnbakedGrouped> blockModels;
   private final Map<Identifier, ItemAsset> itemAssets;
   final Map<Identifier, BakedSimpleModel> simpleModels;
   final BakedSimpleModel missingModel;

   public ModelBaker(
      LoadedEntityModels entityModels,
      SpriteHolder spriteHolder,
      PlayerSkinCache playerSkinCache,
      Map<BlockState, BlockStateModel.UnbakedGrouped> map,
      Map<Identifier, ItemAsset> map2,
      Map<Identifier, BakedSimpleModel> map3,
      BakedSimpleModel bakedSimpleModel
   ) {
      this.entityModels = entityModels;
      this.field_61869 = spriteHolder;
      this.field_62265 = playerSkinCache;
      this.blockModels = map;
      this.itemAssets = map2;
      this.simpleModels = map3;
      this.missingModel = bakedSimpleModel;
   }

   public CompletableFuture<ModelBaker.BakedModels> bake(ErrorCollectingSpriteGetter spriteGetter, Executor executor) {
      ModelBaker.class_12357 lv = new ModelBaker.class_12357();
      ModelBaker.BlockItemModels blockItemModels = ModelBaker.BlockItemModels.bake(this.missingModel, spriteGetter, lv);
      ModelBaker.BakerImpl bakerImpl = new ModelBaker.BakerImpl(spriteGetter, lv, blockItemModels);
      CompletableFuture<Map<BlockState, BlockStateModel>> completableFuture = AsyncHelper.mapValues(this.blockModels, (state, unbaked) -> {
         try {
            return unbaked.bake(state, bakerImpl);
         } catch (Exception var4x) {
            LOGGER.warn("Unable to bake model: '{}': {}", state, var4x);
            return null;
         }
      }, executor);
      CompletableFuture<Map<Identifier, ItemModel>> completableFuture2 = AsyncHelper.mapValues(
         this.itemAssets,
         (state, asset) -> {
            try {
               return asset.model()
                  .bake(
                     new ItemModel.BakeContext(bakerImpl, this.entityModels, this.field_61869, this.field_62265, blockItemModels.item, asset.registrySwapper())
                  );
            } catch (Exception var6x) {
               LOGGER.warn("Unable to bake item model: '{}'", state, var6x);
               return null;
            }
         },
         executor
      );
      Map<Identifier, ItemAsset.Properties> map = new HashMap<>(this.itemAssets.size());
      this.itemAssets.forEach((id, asset) -> {
         ItemAsset.Properties properties = asset.properties();
         if (!properties.equals(ItemAsset.Properties.DEFAULT)) {
            map.put(id, properties);
         }
      });
      return completableFuture.thenCombine(
         completableFuture2,
         (blockStateModels, itemModels) -> new ModelBaker.BakedModels(
            blockItemModels, (Map<BlockState, BlockStateModel>)blockStateModels, (Map<Identifier, ItemModel>)itemModels, map
         )
      );
   }

   @Environment(EnvType.CLIENT)
   public record BakedModels(
      ModelBaker.BlockItemModels missingModels,
      Map<BlockState, BlockStateModel> blockStateModels,
      Map<Identifier, ItemModel> itemStackModels,
      Map<Identifier, ItemAsset.Properties> itemProperties
   ) {
   }

   @Environment(EnvType.CLIENT)
   class BakerImpl implements Baker {
      private final ErrorCollectingSpriteGetter spriteGetter;
      private final Baker.class_12356 field_64597;
      private final ModelBaker.BlockItemModels field_64598;
      private final Map<Baker.ResolvableCacheKey<Object>, Object> cache = new ConcurrentHashMap<>();
      private final Function<Baker.ResolvableCacheKey<Object>, Object> cacheValueFunction = key -> key.compute(this);

      BakerImpl(final ErrorCollectingSpriteGetter spriteGetter, final Baker.class_12356 arg, final ModelBaker.BlockItemModels blockItemModels) {
         this.spriteGetter = spriteGetter;
         this.field_64597 = arg;
         this.field_64598 = blockItemModels;
      }

      @Override
      public BlockModelPart method_76673() {
         return this.field_64598.blockPart;
      }

      @Override
      public ErrorCollectingSpriteGetter getSpriteGetter() {
         return this.spriteGetter;
      }

      @Override
      public Baker.class_12356 method_76674() {
         return this.field_64597;
      }

      @Override
      public BakedSimpleModel getModel(Identifier id) {
         BakedSimpleModel bakedSimpleModel = ModelBaker.this.simpleModels.get(id);
         if (bakedSimpleModel == null) {
            ModelBaker.LOGGER.warn("Requested a model that was not discovered previously: {}", id);
            return ModelBaker.this.missingModel;
         } else {
            return bakedSimpleModel;
         }
      }

      @Override
      @SuppressWarnings("unchecked")
      public <T> T compute(Baker.ResolvableCacheKey<T> key) {
         return (T)this.cache.computeIfAbsent((Baker.ResolvableCacheKey<Object>)(Baker.ResolvableCacheKey<?>)key, this.cacheValueFunction);
      }
   }

   @Environment(EnvType.CLIENT)
   public record BlockItemModels(BlockModelPart blockPart, BlockStateModel block, ItemModel item) {

      public static ModelBaker.BlockItemModels bake(BakedSimpleModel model, ErrorCollectingSpriteGetter errorCollectingSpriteGetter, Baker.class_12356 arg) {
         Baker baker = new Baker() {
            @Override
            public BakedSimpleModel getModel(Identifier id) {
               throw new IllegalStateException("Missing model can't have dependencies, but asked for " + id);
            }

            @Override
            public BlockModelPart method_76673() {
               throw new IllegalStateException();
            }

            @Override
            public <T> T compute(Baker.ResolvableCacheKey<T> key) {
               return key.compute(this);
            }

            @Override
            public ErrorCollectingSpriteGetter getSpriteGetter() {
               return errorCollectingSpriteGetter;
            }

            @Override
            public Baker.class_12356 method_76674() {
               return arg;
            }
         };
         ModelTextures modelTextures = model.getTextures();
         boolean bl = model.getAmbientOcclusion();
         boolean bl2 = model.getGuiLight().isSide();
         ModelTransformation modelTransformation = model.getTransformations();
         BakedGeometry bakedGeometry = model.bakeGeometry(modelTextures, baker, ModelRotation.IDENTITY);
         Sprite sprite = model.getParticleTexture(modelTextures, baker);
         GeometryBakedModel geometryBakedModel = new GeometryBakedModel(bakedGeometry, bl, sprite);
         BlockStateModel blockStateModel = new SimpleBlockStateModel(geometryBakedModel);
         ItemModel itemModel = new MissingItemModel(bakedGeometry.getAllQuads(), new ModelSettings(bl2, sprite, modelTransformation));
         return new ModelBaker.BlockItemModels(geometryBakedModel, blockStateModel, itemModel);
      }
   }

   @Environment(EnvType.CLIENT)
   static class class_12357 implements Baker.class_12356 {
      private final Interner<Vector3fc> field_64599 = Interners.newStrongInterner();

      @Override
      public Vector3fc method_76676(Vector3fc vector3fc) {
         return (Vector3fc)this.field_64599.intern(vector3fc);
      }
   }
}
