package net.minecraft.world.gen.structure;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.RuinedPortalStructurePiece;
import net.minecraft.structure.StructurePiecesCollector;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Структура разрушенного портала. Поддерживает несколько конфигураций размещения ({@link Setup})
 * с весовым выбором. Выбирает случайный шаблон из обычных или редких (5%) порталов,
 * вычисляет высоту размещения в зависимости от типа вертикального размещения.
 */
public class RuinedPortalStructure extends Structure {

	private static final String[] COMMON_PORTAL_STRUCTURE_IDS = new String[]{
			"ruined_portal/portal_1",
			"ruined_portal/portal_2",
			"ruined_portal/portal_3",
			"ruined_portal/portal_4",
			"ruined_portal/portal_5",
			"ruined_portal/portal_6",
			"ruined_portal/portal_7",
			"ruined_portal/portal_8",
			"ruined_portal/portal_9",
			"ruined_portal/portal_10"
	};
	private static final String[] RARE_PORTAL_STRUCTURE_IDS = new String[]{
			"ruined_portal/giant_portal_1", "ruined_portal/giant_portal_2", "ruined_portal/giant_portal_3"
	};
	private static final float RARE_PORTAL_CHANCE = 0.05F;
	private static final int MIN_BLOCKS_ABOVE_WORLD_BOTTOM = 15;
	private final List<RuinedPortalStructure.Setup> setups;
	public static final MapCodec<RuinedPortalStructure> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					                    configCodecBuilder(instance),
					                    Codecs
							                    .nonEmptyList(RuinedPortalStructure.Setup.CODEC.listOf())
							                    .fieldOf("setups")
							                    .forGetter(structure -> structure.setups)
			                    )
			                    .apply(instance, RuinedPortalStructure::new)
	);

	public RuinedPortalStructure(Structure.Config config, List<RuinedPortalStructure.Setup> setups) {
		super(config);
		this.setups = setups;
	}

	public RuinedPortalStructure(Structure.Config config, RuinedPortalStructure.Setup setup) {
		this(config, List.of(setup));
	}

	@Override
	public Optional<Structure.StructurePosition> getStructurePosition(Structure.Context context) {
		RuinedPortalStructurePiece.Properties properties = new RuinedPortalStructurePiece.Properties();
		ChunkRandom chunkRandom = context.random();
		RuinedPortalStructure.Setup selectedSetup = setups.size() > 1
				? pickWeightedSetup(chunkRandom)
				: setups.get(0);

		if (selectedSetup == null) {
			throw new IllegalStateException();
		}

		properties.airPocket = shouldPlaceAirPocket(chunkRandom, selectedSetup.airPocketProbability());
		properties.mossiness = selectedSetup.mossiness();
		properties.overgrown = selectedSetup.overgrown();
		properties.vines = selectedSetup.vines();
		properties.replaceWithBlackstone = selectedSetup.replaceWithBlackstone();

		Identifier templateId = chunkRandom.nextFloat() < RARE_PORTAL_CHANCE
				? Identifier.ofVanilla(RARE_PORTAL_STRUCTURE_IDS[chunkRandom.nextInt(RARE_PORTAL_STRUCTURE_IDS.length)])
				: Identifier.ofVanilla(COMMON_PORTAL_STRUCTURE_IDS[chunkRandom.nextInt(COMMON_PORTAL_STRUCTURE_IDS.length)]);

		StructureTemplate structureTemplate = context.structureTemplateManager().getTemplateOrBlank(templateId);
		BlockRotation blockRotation = Util.getRandom(BlockRotation.values(), chunkRandom);
		BlockMirror blockMirror = chunkRandom.nextFloat() < 0.5F ? BlockMirror.NONE : BlockMirror.FRONT_BACK;
		BlockPos templateCenter = new BlockPos(
				structureTemplate.getSize().getX() / 2,
				0,
				structureTemplate.getSize().getZ() / 2
		);
		ChunkGenerator chunkGenerator = context.chunkGenerator();
		HeightLimitView heightLimitView = context.world();
		NoiseConfig noiseConfig = context.noiseConfig();
		BlockPos chunkStartPos = context.chunkPos().getStartPos();
		BlockBox blockBox = structureTemplate.calculateBoundingBox(chunkStartPos, blockRotation, templateCenter, blockMirror);
		BlockPos centerPos = blockBox.getCenter();
		int surfaceY = chunkGenerator.getHeight(
				centerPos.getX(),
				centerPos.getZ(),
				RuinedPortalStructurePiece.getHeightmapType(selectedSetup.placement()),
				heightLimitView,
				noiseConfig
		) - 1;
		int floorY = getFloorHeight(
				chunkRandom,
				chunkGenerator,
				selectedSetup.placement(),
				properties.airPocket,
				surfaceY,
				blockBox.getBlockCountY(),
				blockBox,
				heightLimitView,
				noiseConfig
		);
		BlockPos placementPos = new BlockPos(chunkStartPos.getX(), floorY, chunkStartPos.getZ());

		return Optional.of(
				new Structure.StructurePosition(
						placementPos,
						collector -> {
							if (selectedSetup.canBeCold()) {
								properties.cold = isColdAt(
										placementPos,
										context.chunkGenerator()
										       .getBiomeSource()
										       .getBiome(
												       BiomeCoords.fromBlock(placementPos.getX()),
												       BiomeCoords.fromBlock(placementPos.getY()),
												       BiomeCoords.fromBlock(placementPos.getZ()),
												       noiseConfig.getMultiNoiseSampler()
										       ),
										chunkGenerator.getSeaLevel()
								);
							}

							collector.addPiece(
									new RuinedPortalStructurePiece(
											context.structureTemplateManager(),
											placementPos,
											selectedSetup.placement(),
											properties,
											templateId,
											structureTemplate,
											blockRotation,
											blockMirror,
											templateCenter
									)
							);
						}
				)
		);
	}

	private RuinedPortalStructure.Setup pickWeightedSetup(ChunkRandom random) {
		float totalWeight = 0.0F;

		for (RuinedPortalStructure.Setup setup : setups) {
			totalWeight += setup.weight();
		}

		float roll = random.nextFloat();

		for (RuinedPortalStructure.Setup setup : setups) {
			roll -= setup.weight() / totalWeight;
			if (roll < 0.0F) {
				return setup;
			}
		}

		return null;
	}

	private static boolean shouldPlaceAirPocket(ChunkRandom random, float probability) {
		if (probability == 0.0F) {
			return false;
		}

		return probability == 1.0F || random.nextFloat() < probability;
	}

	private static boolean isColdAt(BlockPos pos, RegistryEntry<Biome> biome, int seaLevel) {
		return biome.value().isCold(pos, seaLevel);
	}

	/**
	 * Вычисляет Y-координату пола для размещения портала с учётом типа вертикального размещения.
	 * Сканирует угловые колонки чанка снизу вверх, ища позицию с достаточным количеством твёрдых блоков.
	 */
	private static int getFloorHeight(
			Random random,
			ChunkGenerator chunkGenerator,
			RuinedPortalStructurePiece.VerticalPlacement verticalPlacement,
			boolean airPocket,
			int height,
			int blockCountY,
			BlockBox box,
			HeightLimitView world,
			NoiseConfig noiseConfig
	) {
		int worldBottomPadded = world.getBottomY() + MIN_BLOCKS_ABOVE_WORLD_BOTTOM;
		int floorY;

		if (verticalPlacement == RuinedPortalStructurePiece.VerticalPlacement.IN_NETHER) {
			if (airPocket) {
				floorY = MathHelper.nextBetween(random, 32, 100);
			} else if (random.nextFloat() < 0.5F) {
				floorY = MathHelper.nextBetween(random, 27, 29);
			} else {
				floorY = MathHelper.nextBetween(random, 29, 100);
			}
		} else if (verticalPlacement == RuinedPortalStructurePiece.VerticalPlacement.IN_MOUNTAIN) {
			floorY = choosePlacementHeight(random, 70, height - blockCountY);
		} else if (verticalPlacement == RuinedPortalStructurePiece.VerticalPlacement.UNDERGROUND) {
			floorY = choosePlacementHeight(random, worldBottomPadded, height - blockCountY);
		} else if (verticalPlacement == RuinedPortalStructurePiece.VerticalPlacement.PARTLY_BURIED) {
			floorY = height - blockCountY + MathHelper.nextBetween(random, 2, 8);
		} else {
			floorY = height;
		}

		List<BlockPos> cornerPositions = ImmutableList.of(
				new BlockPos(box.getMinX(), 0, box.getMinZ()),
				new BlockPos(box.getMaxX(), 0, box.getMinZ()),
				new BlockPos(box.getMinX(), 0, box.getMaxZ()),
				new BlockPos(box.getMaxX(), 0, box.getMaxZ())
		);
		List<VerticalBlockSample> columnSamples = cornerPositions.stream()
				.map(pos -> chunkGenerator.getColumnSample(pos.getX(), pos.getZ(), world, noiseConfig))
				.collect(Collectors.toList());
		Heightmap.Type heightmapType = verticalPlacement == RuinedPortalStructurePiece.VerticalPlacement.ON_OCEAN_FLOOR
				? Heightmap.Type.OCEAN_FLOOR_WG
				: Heightmap.Type.WORLD_SURFACE_WG;

		int y;

		for (y = floorY; y > worldBottomPadded; y--) {
			int solidCount = 0;

			for (VerticalBlockSample columnSample : columnSamples) {
				BlockState blockState = columnSample.getState(y);
				if (heightmapType.getBlockPredicate().test(blockState)) {
					if (++solidCount == 3) {
						return y;
					}
				}
			}
		}

		return y;
	}

	private static int choosePlacementHeight(Random random, int min, int max) {
		return min < max ? MathHelper.nextBetween(random, min, max) : max;
	}

	@Override
	public StructureType<?> getType() {
		return StructureType.RUINED_PORTAL;
	}

	/**
	 * Конфигурация одного варианта размещения разрушенного портала.
	 * Содержит параметры внешнего вида и вес для взвешенного случайного выбора.
	 */
	public record Setup(
			RuinedPortalStructurePiece.VerticalPlacement placement,
			float airPocketProbability,
			float mossiness,
			boolean overgrown,
			boolean vines,
			boolean canBeCold,
			boolean replaceWithBlackstone,
			float weight
	) {

		public static final Codec<RuinedPortalStructure.Setup> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						                    RuinedPortalStructurePiece.VerticalPlacement.CODEC
								                    .fieldOf("placement")
								                    .forGetter(RuinedPortalStructure.Setup::placement),
						                    Codec
								                    .floatRange(0.0F, 1.0F)
								                    .fieldOf("air_pocket_probability")
								                    .forGetter(RuinedPortalStructure.Setup::airPocketProbability),
						                    Codec
								                    .floatRange(0.0F, 1.0F)
								                    .fieldOf("mossiness")
								                    .forGetter(RuinedPortalStructure.Setup::mossiness),
						                    Codec.BOOL.fieldOf("overgrown").forGetter(RuinedPortalStructure.Setup::overgrown),
						                    Codec.BOOL.fieldOf("vines").forGetter(RuinedPortalStructure.Setup::vines),
						                    Codec.BOOL.fieldOf("can_be_cold").forGetter(RuinedPortalStructure.Setup::canBeCold),
						                    Codec.BOOL
								                    .fieldOf("replace_with_blackstone")
								                    .forGetter(RuinedPortalStructure.Setup::replaceWithBlackstone),
						                    Codecs.POSITIVE_FLOAT.fieldOf("weight").forGetter(RuinedPortalStructure.Setup::weight)
				                    )
				                    .apply(instance, RuinedPortalStructure.Setup::new)
		);
	}
}
