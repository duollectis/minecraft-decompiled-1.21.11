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
 * {@code ChunkBlockLightProvider}.
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
		long l = ChunkSectionPos.fromBlockPos(blockPos);
		if (this.lightStorage.hasSection(l)) {
			BlockState blockState = this.getStateForLighting(this.mutablePos.set(blockPos));
			int i = this.getLightSourceLuminance(blockPos, blockState);
			int j = this.lightStorage.get(blockPos);
			if (i < j) {
				this.lightStorage.set(blockPos, 0);
				this.queueLightDecrease(blockPos, ChunkLightProvider.PackedInfo.packWithAllDirectionsSet(j));
			}
			else {
				this.queueLightDecrease(blockPos, INITIAL_PACKED_INFO);
			}

			if (i > 0) {
				this.queueLightIncrease(
						blockPos,
						ChunkLightProvider.PackedInfo.packWithForce(i, isTrivialForLighting(blockState))
				);
			}
		}
	}

	@Override
	protected void propagateLightIncrease(long blockPos, long packed, int lightLevel) {
		BlockState blockState = null;

		for (Direction direction : DIRECTIONS) {
			if (ChunkLightProvider.PackedInfo.isDirectionBitSet(packed, direction)) {
				long l = BlockPos.offset(blockPos, direction);
				if (this.lightStorage.hasSection(ChunkSectionPos.fromBlockPos(l))) {
					int i = this.lightStorage.get(l);
					int j = lightLevel - 1;
					if (j > i) {
						this.mutablePos.set(l);
						BlockState blockState2 = this.getStateForLighting(this.mutablePos);
						int k = lightLevel - this.getOpacity(blockState2);
						if (k > i) {
							if (blockState == null) {
								blockState = ChunkLightProvider.PackedInfo.isTrivial(packed)
								             ? Blocks.AIR.getDefaultState()
								             : this.getStateForLighting(this.mutablePos.set(blockPos));
							}

							if (!this.shapesCoverFullCube(blockState, blockState2, direction)) {
								this.lightStorage.set(l, k);
								if (k > 1) {
									this.queueLightIncrease(
											l,
											ChunkLightProvider.PackedInfo.packWithOneDirectionCleared(
													k,
													isTrivialForLighting(blockState2),
													direction.getOpposite()
											)
									);
								}
							}
						}
					}
				}
			}
		}
	}

	@Override
	protected void propagateLightDecrease(long blockPos, long packed) {
		int i = ChunkLightProvider.PackedInfo.getLightLevel(packed);

		for (Direction direction : DIRECTIONS) {
			if (ChunkLightProvider.PackedInfo.isDirectionBitSet(packed, direction)) {
				long l = BlockPos.offset(blockPos, direction);
				if (this.lightStorage.hasSection(ChunkSectionPos.fromBlockPos(l))) {
					int j = this.lightStorage.get(l);
					if (j != 0) {
						if (j <= i - 1) {
							BlockState blockState = this.getStateForLighting(this.mutablePos.set(l));
							int k = this.getLightSourceLuminance(l, blockState);
							this.lightStorage.set(l, 0);
							if (k < j) {
								this.queueLightDecrease(
										l,
										ChunkLightProvider.PackedInfo.packWithOneDirectionCleared(
												j,
												direction.getOpposite()
										)
								);
							}

							if (k > 0) {
								this.queueLightIncrease(
										l,
										ChunkLightProvider.PackedInfo.packWithForce(k, isTrivialForLighting(blockState))
								);
							}
						}
						else {
							this.queueLightIncrease(
									l,
									ChunkLightProvider.PackedInfo.packWithRepropagate(j, false, direction.getOpposite())
							);
						}
					}
				}
			}
		}
	}

	private int getLightSourceLuminance(long blockPos, BlockState blockState) {
		int i = blockState.getLuminance();
		return i > 0 && this.lightStorage.isSectionInEnabledColumn(ChunkSectionPos.fromBlockPos(blockPos)) ? i : 0;
	}

	@Override
	public void propagateLight(ChunkPos chunkPos) {
		this.setColumnEnabled(chunkPos, true);
		LightSourceView lightSourceView = this.chunkProvider.getChunk(chunkPos.x, chunkPos.z);
		if (lightSourceView != null) {
			lightSourceView.forEachLightSource((blockPos, blockState) -> {
				int i = blockState.getLuminance();
				this.queueLightIncrease(
						blockPos.asLong(),
						ChunkLightProvider.PackedInfo.packWithForce(i, isTrivialForLighting(blockState))
				);
			});
		}
	}
}
