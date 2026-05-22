package net.minecraft.client.render.model;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
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
import net.minecraft.client.texture.*;
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

import java.io.Reader;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Центральный менеджер запечённых моделей блоков и предметов.
 * Управляет полным циклом перезагрузки: загрузка JSON-моделей → разрешение зависимостей →
 * запекание геометрии → загрузка атласов → применение результата.
 */
@Environment(EnvType.CLIENT)
public class BakedModelManager implements ResourceReloader, FabricBakedModelManager {

	public static final Identifier BLOCK_OR_ITEM_ATLAS_ID = Identifier.ofVanilla("block_or_item");
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final ResourceFinder MODELS_FINDER = ResourceFinder.json("models");
	private Map<Identifier, ItemModel> bakedItemModels = Map.of();
	private Map<Identifier, ItemAsset.Properties> itemProperties = Map.of();
	private final AtlasManager atlasManager;
	private final PlayerSkinCache playerSkinCache;
	private final BlockModels blockModelCache;
	private final BlockColors colorMap;
	private LoadedEntityModels entityModels = LoadedEntityModels.EMPTY;
	private LoadedBlockEntityModels blockEntityModels = LoadedBlockEntityModels.EMPTY;
	private ModelBaker.BlockItemModels missingModels;
	private Object2IntMap<BlockState> modelGroups = Object2IntMaps.emptyMap();

	public BakedModelManager(BlockColors blockColors, AtlasManager atlasManager, PlayerSkinCache playerSkinCache) {
		this.colorMap = blockColors;
		this.atlasManager = atlasManager;
		this.playerSkinCache = playerSkinCache;
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
	public final CompletableFuture<Void> reload(
			ResourceReloader.Store store,
			Executor executor,
			ResourceReloader.Synchronizer synchronizer,
			Executor executor2
	) {
		ResourceManager resourceManager = store.getResourceManager();
		CompletableFuture<LoadedEntityModels>
				completableFuture =
				CompletableFuture.supplyAsync(LoadedEntityModels::copy, executor);
		CompletableFuture<LoadedBlockEntityModels> completableFuture2 = completableFuture.thenApplyAsync(
				loadedEntityModels -> LoadedBlockEntityModels.fromModels(
						new SpecialModelRenderer.BakeContext.Simple(
								loadedEntityModels,
								this.atlasManager,
								this.playerSkinCache
						)
				),
				executor
		);
		CompletableFuture<Map<Identifier, UnbakedModel>> completableFuture3 = reloadModels(resourceManager, executor);
		CompletableFuture<BlockStatesLoader.LoadedModels>
				completableFuture4 =
				BlockStatesLoader.load(resourceManager, executor);
		CompletableFuture<ItemAssetsLoader.Result>
				completableFuture5 =
				ItemAssetsLoader.load(resourceManager, executor);
		CompletableFuture<BakedModelManager.Models>
				completableFuture6 =
				CompletableFuture.allOf(completableFuture3, completableFuture4, completableFuture5)
				                 .thenApplyAsync(
						                 async -> collect(
								                 completableFuture3.join(),
								                 completableFuture4.join(),
								                 completableFuture5.join()
						                 ), executor
				                 );
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
					                        Set<Identifier>
							                        set =
							                        Sets.difference(
									                        completableFuture3.join().keySet(),
									                        models.models.keySet()
							                        );
					                        if (!set.isEmpty()) {
						                        LOGGER.debug(
								                        "Unreferenced models: \n{}",
								                        set
										                        .stream()
										                        .sorted()
										                        .map(id -> "\t" + id + "\n")
										                        .collect(Collectors.joining())
						                        );
					                        }

					                        ModelBaker modelBaker = new ModelBaker(
							                        completableFuture.join(),
							                        this.atlasManager,
							                        this.playerSkinCache,
							                        completableFuture4.join().models(),
							                        completableFuture5.join().contents(),
							                        models.models(),
							                        models.missing()
					                        );
					                        return bake(
							                        stitchResult,
							                        stitchResult2,
							                        modelBaker,
							                        object2IntMap,
							                        completableFuture.join(),
							                        completableFuture2.join(),
							                        executor
					                        );
				                        },
				                        executor
		                        )
		                        .thenCompose(synchronizer::whenPrepared)
		                        .thenAcceptAsync(this::upload, executor2);
	}

	/**
	 * Асинхронно загружает все JSON-модели из ресурсов и десериализует их.
	 * Ошибки загрузки отдельных моделей логируются, но не прерывают процесс.
	 */
	private static CompletableFuture<Map<Identifier, UnbakedModel>> reloadModels(
			ResourceManager resourceManager,
			Executor executor
	) {
		return CompletableFuture
				.<Map<Identifier, Resource>>supplyAsync(() -> MODELS_FINDER.findResources(resourceManager), executor)
				.thenCompose(
						models -> {
							List<CompletableFuture<Pair<Identifier, JsonUnbakedModel>>>
									futures =
									new ArrayList<>(models.size());

							for (Entry<Identifier, Resource> entry : models.entrySet()) {
								futures.add(CompletableFuture.supplyAsync(
										() -> {
											Identifier modelId = MODELS_FINDER.toResourceId(entry.getKey());

											try {
												Pair<Identifier, JsonUnbakedModel> result;
												try (Reader reader = entry.getValue().getReader()) {
													result = Pair.of(modelId, JsonUnbakedModel.deserialize(reader));
												}

												return result;
											}
											catch (Exception exception) {
												LOGGER.error("Failed to load model {}", entry.getKey(), exception);
												return null;
											}
										}, executor
								));
							}

							return Util.combineSafe(futures)
							           .thenApply(loaded -> loaded
									           .stream()
									           .filter(Objects::nonNull)
									           .collect(Collectors.toUnmodifiableMap(Pair::getFirst, Pair::getSecond)));
						}
				);
	}

	/**
	 * Собирает граф зависимостей моделей и возвращает только те, на которые есть ссылки.
	 * Добавляет специальную модель {@code builtin/generated} для предметов.
	 */
	private static BakedModelManager.Models collect(
			Map<Identifier, UnbakedModel> modelMap,
			BlockStatesLoader.LoadedModels stateDefinition,
			ItemAssetsLoader.Result result
	) {
		try (ScopedProfiler scopedProfiler = Profilers.get().scoped("dependencies")) {
			ReferencedModelsCollector
					referencedModelsCollector =
					new ReferencedModelsCollector(modelMap, MissingModel.create());
			referencedModelsCollector.addSpecialModel(GeneratedItemModel.GENERATED, new GeneratedItemModel());
			stateDefinition.models().values().forEach(referencedModelsCollector::resolve);
			result.contents().values().forEach(asset -> referencedModelsCollector.resolve(asset.model()));
			return new BakedModelManager.Models(
					referencedModelsCollector.getMissingModel(),
					referencedModelsCollector.collectModels()
			);
		}
	}

	/**
	 * Запекает все модели асинхронно, собирая ошибки отсутствующих текстур в отдельные мультимапы.
	 * После запекания логирует все пропущенные текстуры и строит итоговую карту состояний блоков.
	 */
	private static CompletableFuture<BakedModelManager.BakingResult> bake(
			SpriteLoader.StitchResult blockStitchResult,
			SpriteLoader.StitchResult itemStitchResult,
			ModelBaker modelBaker,
			Object2IntMap<BlockState> modelGroups,
			LoadedEntityModels loadedEntityModels,
			LoadedBlockEntityModels loadedBlockEntityModels,
			Executor executor
	) {
		final Multimap<String, SpriteIdentifier> missingSprites = Multimaps.synchronizedMultimap(HashMultimap.create());
		final Multimap<String, String> missingRefs = Multimaps.synchronizedMultimap(HashMultimap.create());
		return modelBaker.bake(
				                 new ErrorCollectingSpriteGetter() {
					                 private final Sprite missingSprite = blockStitchResult.missing();
					                 private final Sprite missingItemSprite = itemStitchResult.missing();

					                 @Override
					                 public Sprite get(SpriteIdentifier id, SimpleModel model) {
						                 Identifier atlasId = id.getAtlasId();
						                 boolean isBlockOrItem = atlasId.equals(BakedModelManager.BLOCK_OR_ITEM_ATLAS_ID);
						                 boolean isItemAtlas = atlasId.equals(SpriteAtlasTexture.ITEMS_ATLAS_TEXTURE);
						                 boolean isBlockAtlas = atlasId.equals(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);

						                 if (isBlockOrItem || isItemAtlas) {
							                 Sprite sprite = itemStitchResult.getSprite(id.getTextureId());
							                 if (sprite != null) {
								                 return sprite;
							                 }
						                 }

						                 if (isBlockOrItem || isBlockAtlas) {
							                 Sprite sprite = blockStitchResult.getSprite(id.getTextureId());
							                 if (sprite != null) {
								                 return sprite;
							                 }
						                 }

						                 missingSprites.put(model.name(), id);
						                 return isItemAtlas ? this.missingItemSprite : this.missingSprite;
					                 }

					                 @Override
					                 public Sprite getMissing(String name, SimpleModel model) {
						                 missingRefs.put(model.name(), name);
						                 return this.missingSprite;
					                 }
				                 }, executor
		                 )
		                 .thenApply(
				                 bakedModels -> {
					                 missingSprites.asMap()
					                              .forEach(
							                              (modelName, sprites) -> LOGGER.warn(
									                              "Missing textures in model {}:\n{}",
									                              modelName,
									                              sprites.stream()
									                                     .sorted(SpriteIdentifier.COMPARATOR)
									                                     .map(spriteId -> "    " + spriteId.getAtlasId()
											                                     + ":" + spriteId.getTextureId())
									                                     .collect(Collectors.joining("\n"))
							                              )
					                              );
					                 missingRefs.asMap()
					                            .forEach(
							                            (modelName, textureIds) -> LOGGER.warn(
									                            "Missing texture references in model {}:\n{}",
									                            modelName,
									                            textureIds
											                            .stream()
											                            .sorted()
											                            .map(ref -> "    " + ref)
											                            .collect(Collectors.joining("\n"))
							                            )
					                            );
					                 Map<BlockState, BlockStateModel> stateMap = toStateMap(
							                 bakedModels.blockStateModels(),
							                 bakedModels.missingModels().block()
					                 );
					                 return new BakedModelManager.BakingResult(
							                 bakedModels,
							                 modelGroups,
							                 stateMap,
							                 loadedEntityModels,
							                 loadedBlockEntityModels
					                 );
				                 }
		                 );
	}

	/**
	 * Строит карту {@code BlockState → BlockStateModel} для всех зарегистрированных блоков.
	 * Состояния без модели получают модель-заглушку и логируют предупреждение.
	 */
	private static Map<BlockState, BlockStateModel> toStateMap(
			Map<BlockState, BlockStateModel> blockStateModels,
			BlockStateModel missingModel
	) {
		try (ScopedProfiler scopedProfiler = Profilers.get().scoped("block state dispatch")) {
			Map<BlockState, BlockStateModel> result = new IdentityHashMap<>(blockStateModels);

			for (Block block : Registries.BLOCK) {
				block.getStateManager().getStates().forEach(state -> {
					if (blockStateModels.putIfAbsent(state, missingModel) == null) {
						LOGGER.warn("Missing model for variant: '{}'", state);
					}
				});
			}

			return result;
		}
	}

	private static Object2IntMap<BlockState> group(BlockColors colors, BlockStatesLoader.LoadedModels definition) {
		try (ScopedProfiler scopedProfiler = Profilers.get().scoped("block groups")) {
			return ModelGrouper.group(colors, definition);
		}
	}

	private void upload(BakedModelManager.BakingResult bakingResult) {
		ModelBaker.BakedModels bakedModels = bakingResult.bakedModels;
		bakedItemModels = bakedModels.itemStackModels();
		itemProperties = bakedModels.itemProperties();
		modelGroups = bakingResult.modelGroups;
		missingModels = bakedModels.missingModels();
		blockModelCache.setModels(bakingResult.modelCache);
		blockEntityModels = bakingResult.specialBlockModelRenderer;
		entityModels = bakingResult.entityModelSet;
	}

	/**
	 * Определяет, нужно ли перерисовывать чанк при смене состояния блока.
	 * Блоки в одной группе модели не требуют перерисовки, если не изменилось состояние жидкости.
	 *
	 * @param from исходное состояние блока
	 * @param to новое состояние блока
	 * @return {@code true}, если чанк нужно перерисовать
	 */
	public boolean shouldRerender(BlockState from, BlockState to) {
		if (from == to) {
			return false;
		}

		int fromGroup = modelGroups.getInt(from);
		if (fromGroup != ModelGrouper.NO_GROUP) {
			int toGroup = modelGroups.getInt(to);
			if (fromGroup == toGroup) {
				return from.getFluidState() != to.getFluidState();
			}
		}

		return true;
	}

	public LoadedBlockEntityModels getBlockEntityModelsSupplier() {
		return this.blockEntityModels;
	}

	public Supplier<LoadedEntityModels> getEntityModelsSupplier() {
		return () -> this.entityModels;
	}

	/**
	 * Промежуточный результат запекания: содержит все запечённые модели, группы и загруженные рендереры.
	 */
	@Environment(EnvType.CLIENT)
	record BakingResult(
			ModelBaker.BakedModels bakedModels,
			Object2IntMap<BlockState> modelGroups,
			Map<BlockState, BlockStateModel> modelCache,
			LoadedEntityModels entityModelSet,
			LoadedBlockEntityModels specialBlockModelRenderer
	) {
	}

	/**
	 * Результат сбора зависимостей: модель-заглушка и карта всех используемых моделей.
	 */
	@Environment(EnvType.CLIENT)
	record Models(BakedSimpleModel missing, Map<Identifier, BakedSimpleModel> models) {
	}
}
