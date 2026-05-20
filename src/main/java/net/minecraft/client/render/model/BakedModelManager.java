package net.minecraft.client.render.model;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import java.io.Reader;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricBakedModelManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.item.ItemAsset;
import net.minecraft.client.item.ItemAssetsLoader;
import net.minecraft.client.render.block.BlockModels;
import net.minecraft.client.render.block.entity.LoadedBlockEntityModels;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import net.minecraft.client.render.model.json.GeneratedItemModel;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.texture.AtlasManager;
import net.minecraft.client.texture.PlayerSkinCache;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.SpriteLoader;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.util.Atlases;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.util.profiler.ScopedProfiler;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class BakedModelManager implements ResourceReloader, FabricBakedModelManager {
   public static final Identifier field_64468 = Identifier.ofVanilla("block_or_item");
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ResourceFinder MODELS_FINDER = ResourceFinder.json("models");
   private Map<Identifier, ItemModel> bakedItemModels = Map.of();
   private Map<Identifier, ItemAsset.Properties> itemProperties = Map.of();
   private final AtlasManager field_61870;
   private final PlayerSkinCache field_62266;
   private final BlockModels blockModelCache;
   private final BlockColors colorMap;
   private LoadedEntityModels entityModels = LoadedEntityModels.EMPTY;
   private LoadedBlockEntityModels blockEntityModels = LoadedBlockEntityModels.EMPTY;
   private ModelBaker.BlockItemModels missingModels;
   private Object2IntMap<BlockState> modelGroups = Object2IntMaps.emptyMap();

   public BakedModelManager(BlockColors blockColors, AtlasManager atlasManager, PlayerSkinCache playerSkinCache) {
      this.colorMap = blockColors;
      this.field_61870 = atlasManager;
      this.field_62266 = playerSkinCache;
      this.blockModelCache = new BlockModels(this);
   }

   public BlockStateModel getMissingModel() {
      return this.missingModels.block();
   }

   public ItemModel getItemModel(Identifier id) {
      return this.bakedItemModels.getOrDefault(id, this.missingModels.item());
   }

   public ItemAsset.Properties getItemProperties(Identifier id) {
      return this.itemProperties.getOrDefault(id, ItemAsset.Properties.DEFAULT);
   }

   public BlockModels getBlockModels() {
      return this.blockModelCache;
   }

   @Override
   public final CompletableFuture<Void> reload(ResourceReloader.Store store, Executor executor, ResourceReloader.Synchronizer synchronizer, Executor executor2) {
      ResourceManager resourceManager = store.getResourceManager();
      CompletableFuture<LoadedEntityModels> completableFuture = CompletableFuture.supplyAsync(LoadedEntityModels::copy, executor);
      CompletableFuture<LoadedBlockEntityModels> completableFuture2 = completableFuture.thenApplyAsync(
         loadedEntityModels -> LoadedBlockEntityModels.fromModels(
            new SpecialModelRenderer.BakeContext.Simple(loadedEntityModels, this.field_61870, this.field_62266)
         ),
         executor
      );
      CompletableFuture<Map<Identifier, UnbakedModel>> completableFuture3 = reloadModels(resourceManager, executor);
      CompletableFuture<BlockStatesLoader.LoadedModels> completableFuture4 = BlockStatesLoader.load(resourceManager, executor);
      CompletableFuture<ItemAssetsLoader.Result> completableFuture5 = ItemAssetsLoader.load(resourceManager, executor);
      CompletableFuture<BakedModelManager.Models> completableFuture6 = CompletableFuture.allOf(completableFuture3, completableFuture4, completableFuture5)
         .thenApplyAsync(async -> collect(completableFuture3.join(), completableFuture4.join(), completableFuture5.join()), executor);
      CompletableFuture<Object2IntMap<BlockState>> completableFuture7 = completableFuture4.thenApplyAsync(
         definition -> group(this.colorMap, definition), executor
      );
      AtlasManager.Stitch stitch = store.getOrThrow(AtlasManager.stitchKey);
      CompletableFuture<SpriteLoader.StitchResult> completableFuture8 = stitch.getPreparations(Atlases.BLOCKS);
      CompletableFuture<SpriteLoader.StitchResult> completableFuture9 = stitch.getPreparations(Atlases.ITEMS);
      return CompletableFuture.allOf(
            completableFuture8,
            completableFuture9,
            completableFuture6,
            completableFuture7,
            completableFuture4,
            completableFuture5,
            completableFuture,
            completableFuture2,
            completableFuture3
         )
         .thenComposeAsync(
            void_ -> {
               SpriteLoader.StitchResult stitchResult = completableFuture8.join();
               SpriteLoader.StitchResult stitchResult2 = completableFuture9.join();
               BakedModelManager.Models models = completableFuture6.join();
               Object2IntMap<BlockState> object2IntMap = completableFuture7.join();
               Set<Identifier> set = Sets.difference(completableFuture3.join().keySet(), models.models.keySet());
               if (!set.isEmpty()) {
                  LOGGER.debug("Unreferenced models: \n{}", set.stream().sorted().map(id -> "\t" + id + "\n").collect(Collectors.joining()));
               }

               ModelBaker modelBaker = new ModelBaker(
                  completableFuture.join(),
                  this.field_61870,
                  this.field_62266,
                  completableFuture4.join().models(),
                  completableFuture5.join().contents(),
                  models.models(),
                  models.missing()
               );
               return bake(stitchResult, stitchResult2, modelBaker, object2IntMap, completableFuture.join(), completableFuture2.join(), executor);
            },
            executor
         )
         .thenCompose(synchronizer::whenPrepared)
         .thenAcceptAsync(this::upload, executor2);
   }

   private static CompletableFuture<Map<Identifier, UnbakedModel>> reloadModels(ResourceManager resourceManager, Executor executor) {
      return CompletableFuture.<Map<Identifier, Resource>>supplyAsync(() -> MODELS_FINDER.findResources(resourceManager), executor)
         .thenCompose(
            models -> {
               List<CompletableFuture<Pair<Identifier, JsonUnbakedModel>>> list = new ArrayList<>(models.size());

               for (Entry<Identifier, Resource> entry : models.entrySet()) {
                  list.add(CompletableFuture.supplyAsync(() -> {
                     Identifier identifier = MODELS_FINDER.toResourceId(entry.getKey());

                     try {
                        Pair var3;
                        try (Reader reader = entry.getValue().getReader()) {
                           var3 = Pair.of(identifier, JsonUnbakedModel.deserialize(reader));
                        }

                        return var3;
                     } catch (Exception var7) {
                        LOGGER.error("Failed to load model {}", entry.getKey(), var7);
                        return null;
                     }
                  }, executor));
               }

               return Util.combineSafe(list)
                  .thenApply(modelsx -> modelsx.stream().filter(Objects::nonNull).collect(Collectors.toUnmodifiableMap(Pair::getFirst, Pair::getSecond)));
            }
         );
   }

   private static BakedModelManager.Models collect(
      Map<Identifier, UnbakedModel> modelMap, BlockStatesLoader.LoadedModels stateDefinition, ItemAssetsLoader.Result result
   ) {
      BakedModelManager.Models var5;
      try (ScopedProfiler scopedProfiler = Profilers.get().scoped("dependencies")) {
         ReferencedModelsCollector referencedModelsCollector = new ReferencedModelsCollector(modelMap, MissingModel.create());
         referencedModelsCollector.addSpecialModel(GeneratedItemModel.GENERATED, new GeneratedItemModel());
         stateDefinition.models().values().forEach(referencedModelsCollector::resolve);
         result.contents().values().forEach(asset -> referencedModelsCollector.resolve(asset.model()));
         var5 = new BakedModelManager.Models(referencedModelsCollector.getMissingModel(), referencedModelsCollector.collectModels());
      }

      return var5;
   }

   private static CompletableFuture<BakedModelManager.BakingResult> bake(
      SpriteLoader.StitchResult stitchResult,
      SpriteLoader.StitchResult stitchResult2,
      ModelBaker modelBaker,
      Object2IntMap<BlockState> object2IntMap,
      LoadedEntityModels loadedEntityModels,
      LoadedBlockEntityModels loadedBlockEntityModels,
      Executor executor
   ) {
      final Multimap<String, SpriteIdentifier> multimap = Multimaps.synchronizedMultimap(HashMultimap.create());
      final Multimap<String, String> multimap2 = Multimaps.synchronizedMultimap(HashMultimap.create());
      return modelBaker.bake(new ErrorCollectingSpriteGetter() {
            private final Sprite missingSprite = stitchResult.missing();
            private final Sprite field_64470 = stitchResult2.missing();

            @Override
            public Sprite get(SpriteIdentifier id, SimpleModel model) {
               Identifier identifier = id.getAtlasId();
               boolean bl = identifier.equals(BakedModelManager.field_64468);
               boolean bl2 = identifier.equals(SpriteAtlasTexture.ITEMS_ATLAS_TEXTURE);
               boolean bl3 = identifier.equals(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
               if (bl || bl2) {
                  Sprite sprite = stitchResult2.getSprite(id.getTextureId());
                  if (sprite != null) {
                     return sprite;
                  }
               }

               if (bl || bl3) {
                  Sprite sprite = stitchResult.getSprite(id.getTextureId());
                  if (sprite != null) {
                     return sprite;
                  }
               }

               multimap.put(model.name(), id);
               return bl2 ? this.field_64470 : this.missingSprite;
            }

            @Override
            public Sprite getMissing(String name, SimpleModel model) {
               multimap2.put(model.name(), name);
               return this.missingSprite;
            }
         }, executor)
         .thenApply(
            bakedModels -> {
               multimap.asMap()
                  .forEach(
                     (modelName, sprites) -> LOGGER.warn(
                        "Missing textures in model {}:\n{}",
                        modelName,
                        sprites.stream()
                           .sorted(SpriteIdentifier.COMPARATOR)
                           .map(spriteId -> "    " + spriteId.getAtlasId() + ":" + spriteId.getTextureId())
                           .collect(Collectors.joining("\n"))
                     )
                  );
               multimap2.asMap()
                  .forEach(
                     (modelName, textureIds) -> LOGGER.warn(
                        "Missing texture references in model {}:\n{}",
                        modelName,
                        textureIds.stream().sorted().map(string -> "    " + string).collect(Collectors.joining("\n"))
                     )
                  );
               Map<BlockState, BlockStateModel> map = toStateMap(bakedModels.blockStateModels(), bakedModels.missingModels().block());
               return new BakedModelManager.BakingResult(bakedModels, object2IntMap, map, loadedEntityModels, loadedBlockEntityModels);
            }
         );
   }

   private static Map<BlockState, BlockStateModel> toStateMap(Map<BlockState, BlockStateModel> blockStateModels, BlockStateModel missingModel) {
      Object var8;
      try (ScopedProfiler scopedProfiler = Profilers.get().scoped("block state dispatch")) {
         Map<BlockState, BlockStateModel> map = new IdentityHashMap<>(blockStateModels);

         for (Block block : Registries.BLOCK) {
            block.getStateManager().getStates().forEach(state -> {
               if (blockStateModels.putIfAbsent(state, missingModel) == null) {
                  LOGGER.warn("Missing model for variant: '{}'", state);
               }
            });
         }

         var8 = map;
      }

      return (Map<BlockState, BlockStateModel>)var8;
   }

   private static Object2IntMap<BlockState> group(BlockColors colors, BlockStatesLoader.LoadedModels definition) {
      Object2IntMap var3;
      try (ScopedProfiler scopedProfiler = Profilers.get().scoped("block groups")) {
         var3 = ModelGrouper.group(colors, definition);
      }

      return var3;
   }

   private void upload(BakedModelManager.BakingResult bakingResult) {
      ModelBaker.BakedModels bakedModels = bakingResult.bakedModels;
      this.bakedItemModels = bakedModels.itemStackModels();
      this.itemProperties = bakedModels.itemProperties();
      this.modelGroups = bakingResult.modelGroups;
      this.missingModels = bakedModels.missingModels();
      this.blockModelCache.setModels(bakingResult.modelCache);
      this.blockEntityModels = bakingResult.specialBlockModelRenderer;
      this.entityModels = bakingResult.entityModelSet;
   }

   public boolean shouldRerender(BlockState from, BlockState to) {
      if (from == to) {
         return false;
      } else {
         int i = this.modelGroups.getInt(from);
         if (i != -1) {
            int j = this.modelGroups.getInt(to);
            if (i == j) {
               FluidState fluidState = from.getFluidState();
               FluidState fluidState2 = to.getFluidState();
               return fluidState != fluidState2;
            }
         }

         return true;
      }
   }

   public LoadedBlockEntityModels getBlockEntityModelsSupplier() {
      return this.blockEntityModels;
   }

   public Supplier<LoadedEntityModels> getEntityModelsSupplier() {
      return () -> this.entityModels;
   }

   @Environment(EnvType.CLIENT)
   record BakingResult(
      ModelBaker.BakedModels bakedModels,
      Object2IntMap<BlockState> modelGroups,
      Map<BlockState, BlockStateModel> modelCache,
      LoadedEntityModels entityModelSet,
      LoadedBlockEntityModels specialBlockModelRenderer
   ) {
   }

   @Environment(EnvType.CLIENT)
   record Models(BakedSimpleModel missing, Map<Identifier, BakedSimpleModel> models) {
   }
}
