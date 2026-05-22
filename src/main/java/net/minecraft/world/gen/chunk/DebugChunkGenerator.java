package net.minecraft.world.gen.chunk;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.FixedBiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Отладочный генератор чанков. Отображает все возможные {@link BlockState} в виде сетки
 * на высоте {@link #BLOCK_STATE_Y}, разделённой барьерами на высоте {@link #BARRIER_Y}.
 * Используется исключительно для разработки и тестирования.
 */
public class DebugChunkGenerator extends ChunkGenerator {

	public static final MapCodec<DebugChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(RegistryOps.getEntryCodec(BiomeKeys.PLAINS))
					.apply(instance, instance.stable(DebugChunkGenerator::new))
	);

	private static final int BLOCK_UPDATE_FLAGS = 2;
	private static final List<BlockState> BLOCK_STATES = StreamSupport
			.stream(Registries.BLOCK.spliterator(), false)
			.flatMap(block -> block.getStateManager().getStates().stream())
			.collect(Collectors.toList());
	private static final int X_SIDE_LENGTH = MathHelper.ceil(MathHelper.sqrt(BLOCK_STATES.size()));
	private static final int Z_SIDE_LENGTH = MathHelper.ceil((float) BLOCK_STATES.size() / X_SIDE_LENGTH);

	protected static final BlockState AIR = Blocks.AIR.getDefaultState();
	protected static final BlockState BARRIER = Blocks.BARRIER.getDefaultState();

	public static final int BLOCK_STATE_Y = 70;
	public static final int BARRIER_Y = 60;

	public DebugChunkGenerator(RegistryEntry.Reference<Biome> biomeEntry) {
		super(new FixedBiomeSource(biomeEntry));
	}

	@Override
	protected MapCodec<? extends ChunkGenerator> getCodec() {
		return CODEC;
	}

	@Override
	public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {
	}

	@Override
	public void generateFeatures(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor) {
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		ChunkPos chunkPos = chunk.getPos();
		int chunkX = chunkPos.x;
		int chunkZ = chunkPos.z;

		for (int localX = 0; localX < 16; localX++) {
			for (int localZ = 0; localZ < 16; localZ++) {
				int worldX = ChunkSectionPos.getOffsetPos(chunkX, localX);
				int worldZ = ChunkSectionPos.getOffsetPos(chunkZ, localZ);
				world.setBlockState(mutable.set(worldX, BARRIER_Y, worldZ), BARRIER, BLOCK_UPDATE_FLAGS);
				world.setBlockState(mutable.set(worldX, BLOCK_STATE_Y, worldZ), getBlockState(worldX, worldZ), BLOCK_UPDATE_FLAGS);
			}
		}
	}

	@Override
	public CompletableFuture<Chunk> populateNoise(
			Blender blender,
			NoiseConfig noiseConfig,
			StructureAccessor structureAccessor,
			Chunk chunk
	) {
		return CompletableFuture.completedFuture(chunk);
	}

	@Override
	public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
		return 0;
	}

	@Override
	public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
		return new VerticalBlockSample(0, new BlockState[0]);
	}

	@Override
	public void appendDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
	}

	/**
	 * Возвращает {@link BlockState} для отображения в отладочной сетке по мировым координатам.
	 * Нечётные координаты (x % 2 != 0 && z % 2 != 0) содержат блоки; чётные — воздух (разделители).
	 */
	public static BlockState getBlockState(int x, int z) {
		if (x <= 0 || z <= 0 || x % 2 == 0 || z % 2 == 0) {
			return AIR;
		}

		int gridX = x / 2;
		int gridZ = z / 2;

		if (gridX > X_SIDE_LENGTH || gridZ > Z_SIDE_LENGTH) {
			return AIR;
		}

		int index = MathHelper.abs(gridX * X_SIDE_LENGTH + gridZ);
		return index < BLOCK_STATES.size() ? BLOCK_STATES.get(index) : AIR;
	}

	@Override
	public void carve(
			ChunkRegion chunkRegion,
			long seed,
			NoiseConfig noiseConfig,
			BiomeAccess biomeAccess,
			StructureAccessor structureAccessor,
			Chunk chunk
	) {
	}

	@Override
	public void populateEntities(ChunkRegion region) {
	}

	@Override
	public int getMinimumY() {
		return 0;
	}

	@Override
	public int getWorldHeight() {
		return 384;
	}

	@Override
	public int getSeaLevel() {
		return 63;
	}
}
