package net.minecraft.world.gen.structure;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructureLiquidSettings;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePoolBasedGenerator;
import net.minecraft.structure.pool.alias.StructurePoolAliasBinding;
import net.minecraft.structure.pool.alias.StructurePoolAliasLookup;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.heightprovider.HeightProvider;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * {@code JigsawStructure}.
 */
public final class JigsawStructure extends Structure {

	public static final DimensionPadding DEFAULT_DIMENSION_PADDING = DimensionPadding.NONE;
	public static final StructureLiquidSettings DEFAULT_LIQUID_SETTINGS = StructureLiquidSettings.APPLY_WATERLOGGING;
	public static final int MAX_SIZE = 128;
	public static final int DEFAULT_MAX_DISTANCE_FROM_CENTER = 0;
	public static final int MAX_GENERATION_DEPTH = 20;
	public static final MapCodec<JigsawStructure> CODEC = RecordCodecBuilder.<JigsawStructure>mapCodec(
			                                                                        instance -> instance.group(
					                                                                                            configCodecBuilder(instance),
					                                                                                            StructurePool.REGISTRY_CODEC.fieldOf("start_pool").forGetter(structure -> structure.startPool),
					                                                                                            Identifier.CODEC
							                                                                                            .optionalFieldOf("start_jigsaw_name")
							                                                                                            .forGetter(structure -> structure.startJigsawName),
					                                                                                            Codec.intRange(0, 20).fieldOf("size").forGetter(structure -> structure.size),
					                                                                                            HeightProvider.CODEC.fieldOf("start_height").forGetter(structure -> structure.startHeight),
					                                                                                            Codec.BOOL.fieldOf("use_expansion_hack").forGetter(structure -> structure.useExpansionHack),
					                                                                                            Heightmap.Type.CODEC
							                                                                                            .optionalFieldOf("project_start_to_heightmap")
							                                                                                            .forGetter(structure -> structure.projectStartToHeightmap),
					                                                                                            JigsawStructure.MaxDistanceFromCenter.CODEC
							                                                                                            .fieldOf("max_distance_from_center")
							                                                                                            .forGetter(structure -> structure.maxDistanceFromCenter),
					                                                                                            Codec
							                                                                                            .list(StructurePoolAliasBinding.CODEC)
							                                                                                            .optionalFieldOf("pool_aliases", List.of())
							                                                                                            .forGetter(structure -> structure.poolAliasBindings),
					                                                                                            DimensionPadding.CODEC
							                                                                                            .optionalFieldOf("dimension_padding", DEFAULT_DIMENSION_PADDING)
							                                                                                            .forGetter(structure -> structure.dimensionPadding),
					                                                                                            StructureLiquidSettings.codec
							                                                                                            .optionalFieldOf("liquid_settings", DEFAULT_LIQUID_SETTINGS)
							                                                                                            .forGetter(jigsawStructure -> jigsawStructure.liquidSettings)
			                                                                                            )
			                                                                                            .apply(instance, JigsawStructure::new)
	                                                                        )
	                                                                        .validate(JigsawStructure::validate);
	private final RegistryEntry<StructurePool> startPool;
	private final Optional<Identifier> startJigsawName;
	private final int size;
	private final HeightProvider startHeight;
	private final boolean useExpansionHack;
	private final Optional<Heightmap.Type> projectStartToHeightmap;
	private final JigsawStructure.MaxDistanceFromCenter maxDistanceFromCenter;
	private final List<StructurePoolAliasBinding> poolAliasBindings;
	private final DimensionPadding dimensionPadding;
	private final StructureLiquidSettings liquidSettings;

	private static DataResult<JigsawStructure> validate(JigsawStructure structure) {
		int i = switch (structure.getTerrainAdaptation()) {
			case NONE -> 0;
			case BURY, BEARD_THIN, BEARD_BOX, ENCAPSULATE -> 12;
		};
		return structure.maxDistanceFromCenter.horizontal() + i > 128
		       ? DataResult.error(() -> "Horizontal structure size including terrain adaptation must not exceed 128")
		       : DataResult.success(structure);
	}

	public JigsawStructure(
			Structure.Config config,
			RegistryEntry<StructurePool> startPool,
			Optional<Identifier> startJigsawName,
			int size,
			HeightProvider startHeight,
			boolean useExpansionHack,
			Optional<Heightmap.Type> projectStartToHeightmap,
			JigsawStructure.MaxDistanceFromCenter maxDistanceFromCenter,
			List<StructurePoolAliasBinding> poolAliasBindings,
			DimensionPadding dimensionPadding,
			StructureLiquidSettings liquidSettings
	) {
		super(config);
		this.startPool = startPool;
		this.startJigsawName = startJigsawName;
		this.size = size;
		this.startHeight = startHeight;
		this.useExpansionHack = useExpansionHack;
		this.projectStartToHeightmap = projectStartToHeightmap;
		this.maxDistanceFromCenter = maxDistanceFromCenter;
		this.poolAliasBindings = poolAliasBindings;
		this.dimensionPadding = dimensionPadding;
		this.liquidSettings = liquidSettings;
	}

	public JigsawStructure(
			Structure.Config config,
			RegistryEntry<StructurePool> startPool,
			int size,
			HeightProvider startHeight,
			boolean useExpansionHack,
			Heightmap.Type projectStartToHeightmap
	) {
		this(
				config,
				startPool,
				Optional.empty(),
				size,
				startHeight,
				useExpansionHack,
				Optional.of(projectStartToHeightmap),
				new JigsawStructure.MaxDistanceFromCenter(80),
				List.of(),
				DEFAULT_DIMENSION_PADDING,
				DEFAULT_LIQUID_SETTINGS
		);
	}

	public JigsawStructure(
			Structure.Config config,
			RegistryEntry<StructurePool> startPool,
			int size,
			HeightProvider startHeight,
			boolean useExpansionHack
	) {
		this(
				config,
				startPool,
				Optional.empty(),
				size,
				startHeight,
				useExpansionHack,
				Optional.empty(),
				new JigsawStructure.MaxDistanceFromCenter(80),
				List.of(),
				DEFAULT_DIMENSION_PADDING,
				DEFAULT_LIQUID_SETTINGS
		);
	}

	@Override
	public Optional<Structure.StructurePosition> getStructurePosition(Structure.Context context) {
		ChunkPos chunkPos = context.chunkPos();
		int i = this.startHeight.get(context.random(), new HeightContext(context.chunkGenerator(), context.world()));
		BlockPos blockPos = new BlockPos(chunkPos.getStartX(), i, chunkPos.getStartZ());
		return StructurePoolBasedGenerator.generate(
				context,
				this.startPool,
				this.startJigsawName,
				this.size,
				blockPos,
				this.useExpansionHack,
				this.projectStartToHeightmap,
				this.maxDistanceFromCenter,
				StructurePoolAliasLookup.create(this.poolAliasBindings, blockPos, context.seed()),
				this.dimensionPadding,
				this.liquidSettings
		);
	}

	@Override
	public StructureType<?> getType() {
		return StructureType.JIGSAW;
	}

	@VisibleForTesting
	public RegistryEntry<StructurePool> getStartPool() {
		return this.startPool;
	}

	@VisibleForTesting
	public List<StructurePoolAliasBinding> getPoolAliasBindings() {
		return this.poolAliasBindings;
	}

	/**
	 * {@code MaxDistanceFromCenter}.
	 */
	public record MaxDistanceFromCenter(int horizontal, int vertical) {

		private static final Codec<Integer> DISTANCE_CODEC = Codec.intRange(1, 128);
		private static final Codec<JigsawStructure.MaxDistanceFromCenter> EXPANDED_CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						                    DISTANCE_CODEC
								                    .fieldOf("horizontal")
								                    .forGetter(JigsawStructure.MaxDistanceFromCenter::horizontal),
						                    Codecs.rangedInt(1, DimensionType.MAX_HEIGHT)
						                          .optionalFieldOf("vertical", DimensionType.MAX_HEIGHT)
						                          .forGetter(JigsawStructure.MaxDistanceFromCenter::vertical)
				                    )
				                    .apply(instance, JigsawStructure.MaxDistanceFromCenter::new)
		);
		public static final Codec<JigsawStructure.MaxDistanceFromCenter>
				CODEC =
				Codec.either(EXPANDED_CODEC, DISTANCE_CODEC)
				     .xmap(
						     either -> (JigsawStructure.MaxDistanceFromCenter) either.map(
								     Function.identity(),
								     JigsawStructure.MaxDistanceFromCenter::new
						     ),
						     distance -> distance.horizontal == distance.vertical ? Either.right(distance.horizontal)
						                                                          : Either.left(distance)
				     );

		public MaxDistanceFromCenter(int distance) {
			this(distance, distance);
		}
	}
}
