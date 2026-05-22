package net.minecraft.world.chunk.light;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.ChunkProvider;

/**
 * Провайдер блочного освещения.
 * <p>
 * Реализует алгоритм BFS-распространения блочного света (от факелов, лавы и т.д.).
 * Источником света является яркость ({@code luminance}) самого блока.
 */
public final class ChunkBlockLightProvider extends ChunkLightProvider<BlockLightStorage.Data, BlockLightStorage> {

	private final BlockPos.Mutable mutablePos = new BlockPos.Mutable();

	public ChunkBlockLightProvider(ChunkProvider chunkProvider) {
		this(chunkProvider, new BlockLightStorage(chunkProvider));
	}

	@VisibleForTesting
	public ChunkBlockLightProvider(ChunkProvider chunkProvider, BlockLightStorage blockLightStorage) {
		super(chunkProvider, blockLightStorage);
	}

	@Override
	protected void checkForLightUpdate(long blockPos) {
		long sectionPos = ChunkSectionPos.fromBlockPos(blockPos);

		if (!lightStorage.hasSection(sectionPos)) {
			return;
		}

		BlockState blockState = getStateForLighting(mutablePos.set(blockPos));
		int luminance = getLightSourceLuminance(blockPos, blockState);
		int storedLevel = lightStorage.get(blockPos);

		if (luminance < storedLevel) {
			lightStorage.set(blockPos, 0);
			queueLightDecrease(blockPos, ChunkLightProvider.PackedInfo.packWithAllDirectionsSet(storedLevel));
		} else {
			queueLightDecrease(blockPos, INITIAL_PACKED_INFO);
		}

		if (luminance > 0) {
			queueLightIncrease(
				blockPos,
				ChunkLightProvider.PackedInfo.packWithForce(luminance, isTrivialForLighting(blockState))
			);
		}
	}

	@Override
	protected void propagateLightIncrease(long blockPos, long packed, int lightLevel) {
		BlockState sourceState = null;

		for (Direction direction : DIRECTIONS) {
			if (!ChunkLightProvider.PackedInfo.isDirectionBitSet(packed, direction)) {
				continue;
			}

			long neighborPos = BlockPos.offset(blockPos, direction);

			if (!lightStorage.hasSection(ChunkSectionPos.fromBlockPos(neighborPos))) {
				continue;
			}

			int neighborLevel = lightStorage.get(neighborPos);
			int minRequired = lightLevel - 1;

			if (minRequired <= neighborLevel) {
				continue;
			}

			mutablePos.set(neighborPos);
			BlockState neighborState = getStateForLighting(mutablePos);
			int propagated = lightLevel - getOpacity(neighborState);

			if (propagated <= neighborLevel) {
				continue;
			}

			if (sourceState == null) {
				sourceState = ChunkLightProvider.PackedInfo.isTrivial(packed)
					? Blocks.AIR.getDefaultState()
					: getStateForLighting(mutablePos.set(blockPos));
			}

			if (shapesCoverFullCube(sourceState, neighborState, direction)) {
				continue;
			}

			lightStorage.set(neighborPos, propagated);

			if (propagated > 1) {
				queueLightIncrease(
					neighborPos,
					ChunkLightProvider.PackedInfo.packWithOneDirectionCleared(
						propagated,
						isTrivialForLighting(neighborState),
						direction.getOpposite()
					)
				);
			}
		}
	}

	@Override
	protected void propagateLightDecrease(long blockPos, long packed) {
		int packedLevel = ChunkLightProvider.PackedInfo.getLightLevel(packed);

		for (Direction direction : DIRECTIONS) {
			if (!ChunkLightProvider.PackedInfo.isDirectionBitSet(packed, direction)) {
				continue;
			}

			long neighborPos = BlockPos.offset(blockPos, direction);

			if (!lightStorage.hasSection(ChunkSectionPos.fromBlockPos(neighborPos))) {
				continue;
			}

			int neighborLevel = lightStorage.get(neighborPos);

			if (neighborLevel == 0) {
				continue;
			}

			if (neighborLevel <= packedLevel - 1) {
				BlockState neighborState = getStateForLighting(mutablePos.set(neighborPos));
				int luminance = getLightSourceLuminance(neighborPos, neighborState);
				lightStorage.set(neighborPos, 0);

				if (luminance < neighborLevel) {
					queueLightDecrease(
						neighborPos,
						ChunkLightProvider.PackedInfo.packWithOneDirectionCleared(
							neighborLevel,
							direction.getOpposite()
						)
					);
				}

				if (luminance > 0) {
					queueLightIncrease(
						neighborPos,
						ChunkLightProvider.PackedInfo.packWithForce(luminance, isTrivialForLighting(neighborState))
					);
				}
			} else {
				queueLightIncrease(
					neighborPos,
					ChunkLightProvider.PackedInfo.packWithRepropagate(neighborLevel, false, direction.getOpposite())
				);
			}
		}
	}

	private int getLightSourceLuminance(long blockPos, BlockState blockState) {
		int luminance = blockState.getLuminance();

		return luminance > 0 && lightStorage.isSectionInEnabledColumn(ChunkSectionPos.fromBlockPos(blockPos))
			? luminance
			: 0;
	}

	@Override
	public void propagateLight(ChunkPos chunkPos) {
		setColumnEnabled(chunkPos, true);
		LightSourceView chunk = chunkProvider.getChunk(chunkPos.x, chunkPos.z);

		if (chunk == null) {
			return;
		}

		chunk.forEachLightSource((blockPos, blockState) -> {
			int luminance = blockState.getLuminance();
			queueLightIncrease(
				blockPos.asLong(),
				ChunkLightProvider.PackedInfo.packWithForce(luminance, isTrivialForLighting(blockState))
			);
		});
	}
}
