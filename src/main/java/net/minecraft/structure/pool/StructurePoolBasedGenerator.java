package net.minecraft.structure.pool;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import net.minecraft.block.JigsawBlock;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.*;
import net.minecraft.structure.pool.alias.StructurePoolAliasLookup;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.PriorityIterator;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.DimensionPadding;
import net.minecraft.world.gen.structure.JigsawStructure;
import net.minecraft.world.gen.structure.Structure;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Генератор структур на основе пулов шаблонов (Jigsaw).
 * Собирает структуру из кусков {@link PoolStructurePiece}, выбирая элементы
 * из зарегистрированных {@link StructurePool} и соединяя их через блоки Jigsaw.
 */
public class StructurePoolBasedGenerator {

	static final Logger LOGGER = LogUtils.getLogger();
	private static final int HEIGHT_NOT_SET = Integer.MIN_VALUE;

	/**
	 * Генерирует позицию структуры на основе пула шаблонов.
	 *
	 * @param context                 контекст генерации структуры
	 * @param structurePool           стартовый пул шаблонов
	 * @param id                      идентификатор стартового блока Jigsaw
	 * @param size                    максимальная глубина рекурсии
	 * @param pos                     начальная позиция
	 * @param useExpansionHack        использовать ли расширение ограничивающего блока
	 * @param projectStartToHeightmap тип карты высот для проецирования старта
	 * @param maxDistanceFromCenter   максимальное расстояние от центра
	 * @param aliasLookup             таблица псевдонимов пулов
	 * @param dimensionPadding        отступы по высоте измерения
	 * @param liquidSettings          настройки жидкостей
	 * @return позиция структуры или пустой Optional
	 */
	public static Optional<Structure.StructurePosition> generate(
			Structure.Context context,
			RegistryEntry<StructurePool> structurePool,
			Optional<Identifier> id,
			int size,
			BlockPos pos,
			boolean useExpansionHack,
			Optional<Heightmap.Type> projectStartToHeightmap,
			JigsawStructure.MaxDistanceFromCenter maxDistanceFromCenter,
			StructurePoolAliasLookup aliasLookup,
			DimensionPadding dimensionPadding,
			StructureLiquidSettings liquidSettings
	) {
		DynamicRegistryManager dynamicRegistryManager = context.dynamicRegistryManager();
		ChunkGenerator chunkGenerator = context.chunkGenerator();
		StructureTemplateManager structureTemplateManager = context.structureTemplateManager();
		HeightLimitView heightLimitView = context.world();
		ChunkRandom chunkRandom = context.random();
		Registry<StructurePool> registry = dynamicRegistryManager.getOrThrow(RegistryKeys.TEMPLATE_POOL);
		BlockRotation blockRotation = BlockRotation.random(chunkRandom);
		StructurePool structurePool2 = structurePool.getKey()
		                                            .flatMap(key -> registry.getOptionalValue(aliasLookup.lookup((RegistryKey<StructurePool>) key)))
		                                            .orElse(structurePool.value());
		StructurePoolElement structurePoolElement = structurePool2.getRandomElement(chunkRandom);
		if (structurePoolElement == EmptyPoolElement.INSTANCE) {
			return Optional.empty();
		}
		else {
			BlockPos blockPos;
			if (id.isPresent()) {
				Identifier identifier = id.get();
				Optional<BlockPos>
						optional =
						findStartingJigsawPos(
								structurePoolElement,
								identifier,
								pos,
								blockRotation,
								structureTemplateManager,
								chunkRandom
						);
				if (optional.isEmpty()) {
					LOGGER.error(
							"No starting jigsaw {} found in start pool {}",
							identifier,
							structurePool.getKey().map(key -> key.getValue().toString()).orElse("<unregistered>")
					);
					return Optional.empty();
				}

				blockPos = optional.get();
			}
			else {
				blockPos = pos;
			}

			Vec3i vec3i = blockPos.subtract(pos);
			BlockPos blockPos2 = pos.subtract(vec3i);
			PoolStructurePiece poolStructurePiece = new PoolStructurePiece(
					structureTemplateManager,
					structurePoolElement,
					blockPos2,
					structurePoolElement.getGroundLevelDelta(),
					blockRotation,
					structurePoolElement.getBoundingBox(structureTemplateManager, blockPos2, blockRotation),
					liquidSettings
			);
			BlockBox blockBox = poolStructurePiece.getBoundingBox();
			int i = (blockBox.getMaxX() + blockBox.getMinX()) / 2;
			int j = (blockBox.getMaxZ() + blockBox.getMinZ()) / 2;
			int k = projectStartToHeightmap.isEmpty()
			        ? blockPos2.getY()
			        : pos.getY() + chunkGenerator.getHeightOnGround(
					        i,
					        j,
					        projectStartToHeightmap.get(),
					        heightLimitView,
					        context.noiseConfig()
			        );
			int l = blockBox.getMinY() + poolStructurePiece.getGroundLevelDelta();
			poolStructurePiece.translate(0, k - l, 0);
			if (exceedsHeightLimit(heightLimitView, dimensionPadding, poolStructurePiece.getBoundingBox())) {
				LOGGER.debug(
						"Center piece {} with bounding box {} does not fit dimension padding {}",
						new Object[]{structurePoolElement, poolStructurePiece.getBoundingBox(), dimensionPadding}
				);
				return Optional.empty();
			}
			else {
				int m = k + vec3i.getY();
				return Optional.of(
						new Structure.StructurePosition(
								new BlockPos(i, m, j),
								(Consumer<StructurePiecesCollector>) (collector -> {
									List<PoolStructurePiece> list = Lists.newArrayList();
									list.add(poolStructurePiece);
									if (size > 0) {
										Box box = new Box(
												i - maxDistanceFromCenter.horizontal(),
												Math.max(
														m - maxDistanceFromCenter.vertical(),
														heightLimitView.getBottomY() + dimensionPadding.bottom()
												),
												j - maxDistanceFromCenter.horizontal(),
												i + maxDistanceFromCenter.horizontal() + 1,
												Math.min(
														m + maxDistanceFromCenter.vertical() + 1,
														heightLimitView.getTopYInclusive() + 1 - dimensionPadding.top()
												),
												j + maxDistanceFromCenter.horizontal() + 1
										);
										VoxelShape voxelShape = VoxelShapes.combineAndSimplify(
												VoxelShapes.cuboid(box),
												VoxelShapes.cuboid(Box.from(blockBox)),
												BooleanBiFunction.ONLY_FIRST
										);
										generate(
												context.noiseConfig(),
												size,
												useExpansionHack,
												chunkGenerator,
												structureTemplateManager,
												heightLimitView,
												chunkRandom,
												registry,
												poolStructurePiece,
												list,
												voxelShape,
												aliasLookup,
												liquidSettings
										);
										list.forEach(collector::addPiece);
									}
								}
								)
						)
				);
			}
		}
	}

	/**
	 * Проверяет, выходит ли ограничивающий блок за пределы высоты измерения с учётом отступов.
	 *
	 * @param world   вид ограничений высоты
	 * @param padding отступы измерения
	 * @param box     проверяемый ограничивающий блок
	 * @return {@code true}, если блок выходит за допустимые пределы
	 */
	private static boolean exceedsHeightLimit(HeightLimitView world, DimensionPadding padding, BlockBox box) {
		if (padding == DimensionPadding.NONE) {
			return false;
		}
		else {
			int i = world.getBottomY() + padding.bottom();
			int j = world.getTopYInclusive() - padding.top();
			return box.getMinY() < i || box.getMaxY() > j;
		}
	}

	/**
	 * Ищет стартовую позицию блока Jigsaw с заданным идентификатором в элементе пула.
	 *
	 * @param pool             элемент пула структур
	 * @param id               идентификатор искомого блока Jigsaw
	 * @param pos              позиция размещения
	 * @param rotation         поворот
	 * @param structureManager менеджер шаблонов структур
	 * @param random           генератор случайных чисел
	 * @return позиция найденного блока Jigsaw или пустой Optional
	 */
	private static Optional<BlockPos> findStartingJigsawPos(
			StructurePoolElement pool,
			Identifier id,
			BlockPos pos,
			BlockRotation rotation,
			StructureTemplateManager structureManager,
			ChunkRandom random
	) {
		for (StructureTemplate.JigsawBlockInfo jigsawBlockInfo : pool.getStructureBlockInfos(
				structureManager,
				pos,
				rotation,
				random
		)) {
			if (id.equals(jigsawBlockInfo.name())) {
				return Optional.of(jigsawBlockInfo.info().pos());
			}
		}

		return Optional.empty();
	}

	/**
	 * Запускает рекурсивную генерацию кусков структуры через {@link StructurePoolGenerator}.
	 *
	 * @param noiseConfig              конфигурация шума
	 * @param maxSize                  максимальная глубина рекурсии
	 * @param modifyBoundingBox        расширять ли ограничивающий блок
	 * @param chunkGenerator           генератор чанков
	 * @param structureTemplateManager менеджер шаблонов
	 * @param heightLimitView          ограничения высоты мира
	 * @param random                   генератор случайных чисел
	 * @param structurePoolRegistry    реестр пулов структур
	 * @param firstPiece               стартовый кусок структуры
	 * @param pieces                   список для накопления кусков
	 * @param pieceShape               форма занятого пространства
	 * @param aliasLookup              таблица псевдонимов пулов
	 * @param liquidSettings           настройки жидкостей
	 */
	private static void generate(
			NoiseConfig noiseConfig,
			int maxSize,
			boolean modifyBoundingBox,
			ChunkGenerator chunkGenerator,
			StructureTemplateManager structureTemplateManager,
			HeightLimitView heightLimitView,
			Random random,
			Registry<StructurePool> structurePoolRegistry,
			PoolStructurePiece firstPiece,
			List<PoolStructurePiece> pieces,
			VoxelShape pieceShape,
			StructurePoolAliasLookup aliasLookup,
			StructureLiquidSettings liquidSettings
	) {
		StructurePoolBasedGenerator.StructurePoolGenerator
				structurePoolGenerator =
				new StructurePoolBasedGenerator.StructurePoolGenerator(
						structurePoolRegistry, maxSize, chunkGenerator, structureTemplateManager, pieces, random
				);
		structurePoolGenerator.generatePiece(
				firstPiece,
				new MutableObject(pieceShape),
				0,
				modifyBoundingBox,
				heightLimitView,
				noiseConfig,
				aliasLookup,
				liquidSettings
		);

		while (structurePoolGenerator.structurePieces.hasNext()) {
			StructurePoolBasedGenerator.ShapedPoolStructurePiece
					shapedPoolStructurePiece =
					(StructurePoolBasedGenerator.ShapedPoolStructurePiece) structurePoolGenerator.structurePieces
							.next();
			structurePoolGenerator.generatePiece(
					shapedPoolStructurePiece.piece,
					shapedPoolStructurePiece.pieceShape,
					shapedPoolStructurePiece.depth,
					modifyBoundingBox,
					heightLimitView,
					noiseConfig,
					aliasLookup,
					liquidSettings
			);
		}
	}

	/**
	 * Генерирует структуру непосредственно в мире (используется командами и отладкой).
	 *
	 * @param world         серверный мир
	 * @param structurePool стартовый пул структур
	 * @param id            идентификатор стартового блока Jigsaw
	 * @param size          максимальная глубина рекурсии
	 * @param pos           начальная позиция
	 * @param keepJigsaws   сохранять ли блоки Jigsaw после генерации
	 * @return {@code true}, если структура успешно сгенерирована
	 */
	public static boolean generate(
			ServerWorld world,
			RegistryEntry<StructurePool> structurePool,
			Identifier id,
			int size,
			BlockPos pos,
			boolean keepJigsaws
	) {
		ChunkGenerator chunkGenerator = world.getChunkManager().getChunkGenerator();
		StructureTemplateManager structureTemplateManager = world.getStructureTemplateManager();
		StructureAccessor structureAccessor = world.getStructureAccessor();
		Random random = world.getRandom();
		Structure.Context context = new Structure.Context(
				world.getRegistryManager(),
				chunkGenerator,
				chunkGenerator.getBiomeSource(),
				world.getChunkManager().getNoiseConfig(),
				structureTemplateManager,
				world.getSeed(),
				new ChunkPos(pos),
				world,
				biome -> true
		);
		Optional<Structure.StructurePosition> optional = generate(
				context,
				structurePool,
				Optional.of(id),
				size,
				pos,
				false,
				Optional.empty(),
				new JigsawStructure.MaxDistanceFromCenter(128),
				StructurePoolAliasLookup.EMPTY,
				JigsawStructure.DEFAULT_DIMENSION_PADDING,
				JigsawStructure.DEFAULT_LIQUID_SETTINGS
		);
		if (optional.isPresent()) {
			StructurePiecesCollector structurePiecesCollector = optional.get().generate();

			for (StructurePiece structurePiece : structurePiecesCollector.toList().pieces()) {
				if (structurePiece instanceof PoolStructurePiece poolStructurePiece) {
					poolStructurePiece.generate(
							world,
							structureAccessor,
							chunkGenerator,
							random,
							BlockBox.infinite(),
							pos,
							keepJigsaws
					);
				}
			}

			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Вспомогательная запись, связывающая кусок структуры с его формой и глубиной рекурсии.
	 *
	 * @param piece      кусок структуры
	 * @param pieceShape форма занятого пространства
	 * @param depth      текущая глубина рекурсии
	 */
	record ShapedPoolStructurePiece(PoolStructurePiece piece, MutableObject<VoxelShape> pieceShape, int depth) {
	}

	/**
	 * Внутренний генератор, выполняющий рекурсивную сборку структуры из кусков пула.
	 * Использует очередь с приоритетами для обхода в порядке приоритета размещения.
	 */
	static final class StructurePoolGenerator {

		private final Registry<StructurePool> registry;
		private final int maxSize;
		private final ChunkGenerator chunkGenerator;
		private final StructureTemplateManager structureTemplateManager;
		private final List<? super PoolStructurePiece> children;
		private final Random random;
		final PriorityIterator<StructurePoolBasedGenerator.ShapedPoolStructurePiece>
				structurePieces =
				new PriorityIterator<>();

		/**
		 * @param poolRegistry    реестр пулов структур
		 * @param initialMaxSize  максимальная глубина рекурсии
		 * @param generator       генератор чанков
		 * @param templateManager менеджер шаблонов структур
		 * @param pieceList       список для накопления кусков
		 * @param rng             генератор случайных чисел
		 */
		StructurePoolGenerator(
				Registry<StructurePool> poolRegistry,
				int initialMaxSize,
				ChunkGenerator generator,
				StructureTemplateManager templateManager,
				List<? super PoolStructurePiece> pieceList,
				Random rng
		) {
			this.registry = poolRegistry;
			this.maxSize = initialMaxSize;
			this.chunkGenerator = generator;
			this.structureTemplateManager = templateManager;
			this.children = pieceList;
			this.random = rng;
		}

		/**
		 * Генерирует дочерние куски для заданного куска структуры, соединяя их через блоки Jigsaw.
		 *
		 * @param piece             родительский кусок структуры
		 * @param pieceShape        форма занятого пространства родителя
		 * @param depth             текущая глубина рекурсии
		 * @param modifyBoundingBox расширять ли ограничивающий блок
		 * @param world             ограничения высоты мира
		 * @param noiseConfig       конфигурация шума
		 * @param aliasLookup       таблица псевдонимов пулов
		 * @param liquidSettings    настройки жидкостей
		 */
		void generatePiece(
				PoolStructurePiece piece,
				MutableObject<VoxelShape> pieceShape,
				int depth,
				boolean modifyBoundingBox,
				HeightLimitView world,
				NoiseConfig noiseConfig,
				StructurePoolAliasLookup aliasLookup,
				StructureLiquidSettings liquidSettings
		) {
			StructurePoolElement structurePoolElement = piece.getPoolElement();
			BlockPos blockPos = piece.getPos();
			BlockRotation blockRotation = piece.getRotation();
			StructurePool.Projection projection = structurePoolElement.getProjection();
			boolean bl = projection == StructurePool.Projection.RIGID;
			MutableObject<VoxelShape> mutableObject = new MutableObject();
			BlockBox blockBox = piece.getBoundingBox();
			int i = blockBox.getMinY();

			label129:
			for (StructureTemplate.JigsawBlockInfo jigsawBlockInfo : structurePoolElement.getStructureBlockInfos(
					this.structureTemplateManager, blockPos, blockRotation, this.random
			)) {
				StructureTemplate.StructureBlockInfo structureBlockInfo = jigsawBlockInfo.info();
				Direction direction = JigsawBlock.getFacing(structureBlockInfo.state());
				BlockPos blockPos2 = structureBlockInfo.pos();
				BlockPos blockPos3 = blockPos2.offset(direction);
				int j = blockPos2.getY() - i;
				int k = Integer.MIN_VALUE;
				RegistryKey<StructurePool> registryKey = aliasLookup.lookup(jigsawBlockInfo.pool());
				Optional<? extends RegistryEntry<StructurePool>> optional = this.registry.getOptional(registryKey);
				if (optional.isEmpty()) {
					StructurePoolBasedGenerator.LOGGER.warn("Empty or non-existent pool: {}", registryKey.getValue());
				}
				else {
					RegistryEntry<StructurePool> registryEntry = (RegistryEntry<StructurePool>) optional.get();
					if (registryEntry.value().getElementCount() == 0
							&& !registryEntry.matchesKey(StructurePools.EMPTY)) {
						StructurePoolBasedGenerator.LOGGER.warn(
								"Empty or non-existent pool: {}",
								registryKey.getValue()
						);
					}
					else {
						RegistryEntry<StructurePool> registryEntry2 = registryEntry.value().getFallback();
						if (registryEntry2.value().getElementCount() == 0
								&& !registryEntry2.matchesKey(StructurePools.EMPTY)) {
							StructurePoolBasedGenerator.LOGGER
									.warn(
											"Empty or non-existent fallback pool: {}",
											registryEntry2
													.getKey()
													.map(key -> key.getValue().toString())
													.orElse("<unregistered>")
									);
						}
						else {
							boolean bl2 = blockBox.contains(blockPos3);
							MutableObject<VoxelShape> mutableObject2;
							if (bl2) {
								mutableObject2 = mutableObject;
								if (mutableObject.get() == null) {
									mutableObject.setValue(VoxelShapes.cuboid(Box.from(blockBox)));
								}
							}
							else {
								mutableObject2 = pieceShape;
							}

							List<StructurePoolElement> list = Lists.newArrayList();
							if (depth != this.maxSize) {
								list.addAll(registryEntry.value().getElementIndicesInRandomOrder(this.random));
							}

							list.addAll(registryEntry2.value().getElementIndicesInRandomOrder(this.random));
							int l = jigsawBlockInfo.placementPriority();

							for (StructurePoolElement structurePoolElement2 : list) {
								if (structurePoolElement2 == EmptyPoolElement.INSTANCE) {
									break;
								}

								for (BlockRotation blockRotation2 : BlockRotation.randomRotationOrder(this.random)) {
									List<StructureTemplate.JigsawBlockInfo>
											list2 =
											structurePoolElement2.getStructureBlockInfos(
													this.structureTemplateManager,
													BlockPos.ORIGIN,
													blockRotation2,
													this.random
											);
									BlockBox
											blockBox2 =
											structurePoolElement2.getBoundingBox(
													this.structureTemplateManager,
													BlockPos.ORIGIN,
													blockRotation2
											);
									int m;
									if (modifyBoundingBox && blockBox2.getBlockCountY() <= 16) {
										m = list2.stream().mapToInt(jigsawInfo -> {
											StructureTemplate.StructureBlockInfo
													structureBlockInfox =
													jigsawInfo.info();
											if (!blockBox2.contains(structureBlockInfox
													.pos()
													.offset(JigsawBlock.getFacing(structureBlockInfox.state())))) {
												return 0;
											}
											else {
												RegistryKey<StructurePool>
														registryKeyx =
														aliasLookup.lookup(jigsawInfo.pool());
												Optional<? extends RegistryEntry<StructurePool>>
														optionalx =
														this.registry.getOptional(registryKeyx);
												Optional<RegistryEntry<StructurePool>>
														optional2 =
														optionalx.map(entry -> entry.value().getFallback());
												int
														ix =
														optionalx
																.<Integer>map(entry -> entry
																		.value()
																		.getHighestY(this.structureTemplateManager))
																.orElse(0);
												int
														jx =
														optional2
																.<Integer>map(entry -> entry
																		.value()
																		.getHighestY(this.structureTemplateManager))
																.orElse(0);
												return Math.max(ix, jx);
											}
										}).max().orElse(0);
									}
									else {
										m = 0;
									}

									for (StructureTemplate.JigsawBlockInfo jigsawBlockInfo2 : list2) {
										if (JigsawBlock.attachmentMatches(jigsawBlockInfo, jigsawBlockInfo2)) {
											BlockPos blockPos4 = jigsawBlockInfo2.info().pos();
											BlockPos blockPos5 = blockPos3.subtract(blockPos4);
											BlockBox
													blockBox3 =
													structurePoolElement2.getBoundingBox(
															this.structureTemplateManager,
															blockPos5,
															blockRotation2
													);
											int n = blockBox3.getMinY();
											StructurePool.Projection
													projection2 =
													structurePoolElement2.getProjection();
											boolean bl3 = projection2 == StructurePool.Projection.RIGID;
											int o = blockPos4.getY();
											int
													p =
													j - o + JigsawBlock
															.getFacing(structureBlockInfo.state())
															.getOffsetY();
											int q;
											if (bl && bl3) {
												q = i + p;
											}
											else {
												if (k == Integer.MIN_VALUE) {
													k = this.chunkGenerator
															.getHeightOnGround(
																	blockPos2.getX(),
																	blockPos2.getZ(),
																	Heightmap.Type.WORLD_SURFACE_WG,
																	world,
																	noiseConfig
															);
												}

												q = k - o;
											}

											int r = q - n;
											BlockBox blockBox4 = blockBox3.offset(0, r, 0);
											BlockPos blockPos6 = blockPos5.add(0, r, 0);
											if (m > 0) {
												int s = Math.max(m + 1, blockBox4.getMaxY() - blockBox4.getMinY());
												blockBox4.encompass(new BlockPos(
														blockBox4.getMinX(),
														blockBox4.getMinY() + s,
														blockBox4.getMinZ()
												));
											}

											if (!VoxelShapes.matchesAnywhere(
													(VoxelShape) mutableObject2.get(),
													VoxelShapes.cuboid(Box.from(blockBox4).contract(0.25)),
													BooleanBiFunction.ONLY_SECOND
											)) {
												mutableObject2.setValue(
														VoxelShapes.combine(
																(VoxelShape) mutableObject2.get(),
																VoxelShapes.cuboid(Box.from(blockBox4)),
																BooleanBiFunction.ONLY_FIRST
														)
												);
												int s = piece.getGroundLevelDelta();
												int t;
												if (bl3) {
													t = s - p;
												}
												else {
													t = structurePoolElement2.getGroundLevelDelta();
												}

												PoolStructurePiece poolStructurePiece = new PoolStructurePiece(
														this.structureTemplateManager,
														structurePoolElement2,
														blockPos6,
														t,
														blockRotation2,
														blockBox4,
														liquidSettings
												);
												int u;
												if (bl) {
													u = i + j;
												}
												else if (bl3) {
													u = q + o;
												}
												else {
													if (k == Integer.MIN_VALUE) {
														k = this.chunkGenerator
																.getHeightOnGround(
																		blockPos2.getX(),
																		blockPos2.getZ(),
																		Heightmap.Type.WORLD_SURFACE_WG,
																		world,
																		noiseConfig
																);
													}

													u = k + p / 2;
												}

												piece.addJunction(new JigsawJunction(
														blockPos3.getX(),
														u - j + s,
														blockPos3.getZ(),
														p,
														projection2
												));
												poolStructurePiece.addJunction(new JigsawJunction(
														blockPos2.getX(),
														u - o + t,
														blockPos2.getZ(),
														-p,
														projection
												));
												this.children.add(poolStructurePiece);
												if (depth + 1 <= this.maxSize) {
													StructurePoolBasedGenerator.ShapedPoolStructurePiece
															shapedPoolStructurePiece =
															new StructurePoolBasedGenerator.ShapedPoolStructurePiece(
																	poolStructurePiece, mutableObject2, depth + 1
															);
													this.structurePieces.enqueue(shapedPoolStructurePiece, l);
												}
												continue label129;
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
}
