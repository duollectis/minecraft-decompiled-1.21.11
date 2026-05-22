package net.minecraft.world.chunk;

import com.google.common.base.Suppliers;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.CollisionView;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.border.WorldBorder;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Кеш чанков для операций коллизий и чтения блоков в ограниченной области мира.
 * Предварительно загружает все чанки в диапазоне [minPos, maxPos] и предоставляет
 * быстрый доступ без обращения к менеджеру чанков на каждый запрос.
 */
public class ChunkCache implements CollisionView {

	protected final int minX;
	protected final int minZ;
	protected final Chunk[][] chunks;
	protected boolean empty;
	protected final World world;
	private final Supplier<RegistryEntry<Biome>> plainsEntryGetter;

	public ChunkCache(World world, BlockPos minPos, BlockPos maxPos) {
		this.world = world;
		plainsEntryGetter = Suppliers.memoize(() -> world
				.getRegistryManager()
				.getOrThrow(RegistryKeys.BIOME)
				.getOrThrow(BiomeKeys.PLAINS));

		minX = ChunkSectionPos.getSectionCoord(minPos.getX());
		minZ = ChunkSectionPos.getSectionCoord(minPos.getZ());
		int maxChunkX = ChunkSectionPos.getSectionCoord(maxPos.getX());
		int maxChunkZ = ChunkSectionPos.getSectionCoord(maxPos.getZ());
		chunks = new Chunk[maxChunkX - minX + 1][maxChunkZ - minZ + 1];

		ChunkManager chunkManager = world.getChunkManager();
		empty = true;

		for (int cx = minX; cx <= maxChunkX; cx++) {
			for (int cz = minZ; cz <= maxChunkZ; cz++) {
				chunks[cx - minX][cz - minZ] = chunkManager.getWorldChunk(cx, cz);
			}
		}

		for (int cx = minX; cx <= maxChunkX; cx++) {
			for (int cz = minZ; cz <= maxChunkZ; cz++) {
				Chunk chunk = chunks[cx - minX][cz - minZ];
				if (chunk != null && !chunk.areSectionsEmptyBetween(minPos.getY(), maxPos.getY())) {
					empty = false;
					return;
				}
			}
		}
	}

	private Chunk getChunk(BlockPos pos) {
		return getChunk(ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ()));
	}

	private Chunk getChunk(int chunkX, int chunkZ) {
		int relX = chunkX - minX;
		int relZ = chunkZ - minZ;

		if (relX >= 0 && relX < chunks.length && relZ >= 0 && relZ < chunks[relX].length) {
			Chunk chunk = chunks[relX][relZ];
			return chunk != null ? chunk : new EmptyChunk(world, new ChunkPos(chunkX, chunkZ), plainsEntryGetter.get());
		}

		return new EmptyChunk(world, new ChunkPos(chunkX, chunkZ), plainsEntryGetter.get());
	}

	@Override
	public WorldBorder getWorldBorder() {
		return world.getWorldBorder();
	}

	@Override
	public BlockView getChunkAsView(int chunkX, int chunkZ) {
		return getChunk(chunkX, chunkZ);
	}

	@Override
	public List<VoxelShape> getEntityCollisions(@Nullable Entity entity, Box box) {
		return List.of();
	}

	@Override
	public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
		return getChunk(pos).getBlockEntity(pos);
	}

	@Override
	public BlockState getBlockState(BlockPos pos) {
		if (isOutOfHeightLimit(pos)) {
			return Blocks.AIR.getDefaultState();
		}

		return getChunk(pos).getBlockState(pos);
	}

	@Override
	public FluidState getFluidState(BlockPos pos) {
		if (isOutOfHeightLimit(pos)) {
			return Fluids.EMPTY.getDefaultState();
		}

		return getChunk(pos).getFluidState(pos);
	}

	@Override
	public int getBottomY() {
		return world.getBottomY();
	}

	@Override
	public int getHeight() {
		return world.getHeight();
	}
}
