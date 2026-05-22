package net.minecraft.client.render.chunk;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntPriorityQueue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.BitSet;
import java.util.EnumSet;
import java.util.Set;

@Environment(EnvType.CLIENT)
/**
 * {@code ChunkOcclusionDataBuilder}.
 */
public class ChunkOcclusionDataBuilder {

	private static final int COORD_BITS = 4;
	private static final int CHUNK_SIZE = 16;
	private static final int COORD_MASK = 15;
	private static final int TOTAL_BLOCKS = 4096;
	private static final int X_SHIFT = 0;
	private static final int Z_SHIFT = 4;
	private static final int Y_SHIFT = 8;
	private static final int STEP_X = (int) Math.pow(16.0, 0.0);
	private static final int STEP_Z = (int) Math.pow(16.0, 1.0);
	private static final int STEP_Y = (int) Math.pow(16.0, 2.0);
	private static final int INVALID_POS = -1;
	private static final Direction[] DIRECTIONS = Direction.values();
	private final BitSet closed = new BitSet(TOTAL_BLOCKS);
	private static final int[] EDGE_POINTS = Util.make(
			new int[1352], edgePoints -> {
				int i = 0;
				int j = COORD_MASK;
				int k = 0;

				for (int l = 0; l < CHUNK_SIZE; l++) {
					for (int m = 0; m < CHUNK_SIZE; m++) {
						for (int n = 0; n < CHUNK_SIZE; n++) {
							if (l == 0 || l == COORD_MASK || m == 0 || m == COORD_MASK || n == 0 || n == COORD_MASK) {
								edgePoints[k++] = pack(l, m, n);
							}
						}
					}
				}
			}
	);
	private int openCount = TOTAL_BLOCKS;

	/**
	 * Mark closed.
	 *
	 * @param pos pos
	 */
	public void markClosed(BlockPos pos) {
		this.closed.set(pack(pos), true);
		this.openCount--;
	}

	private static int pack(BlockPos pos) {
		return pack(pos.getX() & COORD_MASK, pos.getY() & COORD_MASK, pos.getZ() & COORD_MASK);
	}

	private static int pack(int x, int y, int z) {
		return x << 0 | y << 8 | z << 4;
	}

	/**
	 * Build.
	 *
	 * @return ChunkOcclusionData — результат операции
	 */
	public ChunkOcclusionData build() {
		ChunkOcclusionData chunkOcclusionData = new ChunkOcclusionData();
		if (TOTAL_BLOCKS - this.openCount < 256) {
			chunkOcclusionData.fill(true);
		}
		else if (this.openCount == 0) {
			chunkOcclusionData.fill(false);
		}
		else {
			for (int i : EDGE_POINTS) {
				if (!this.closed.get(i)) {
					chunkOcclusionData.addOpenEdgeFaces(this.getOpenFaces(i));
				}
			}
		}

		return chunkOcclusionData;
	}

	private Set<Direction> getOpenFaces(int pos) {
		Set<Direction> set = EnumSet.noneOf(Direction.class);
		IntPriorityQueue intPriorityQueue = new IntArrayFIFOQueue();
		intPriorityQueue.enqueue(pos);
		this.closed.set(pos, true);

		while (!intPriorityQueue.isEmpty()) {
			int i = intPriorityQueue.dequeueInt();
			this.addEdgeFaces(i, set);

			for (Direction direction : DIRECTIONS) {
				int j = this.offset(i, direction);
				if (j >= 0 && !this.closed.get(j)) {
					this.closed.set(j, true);
					intPriorityQueue.enqueue(j);
				}
			}
		}

		return set;
	}

	private void addEdgeFaces(int pos, Set<Direction> openFaces) {
		int i = pos >> 0 & COORD_MASK;
		if (i == 0) {
			openFaces.add(Direction.WEST);
		}
		else if (i == COORD_MASK) {
			openFaces.add(Direction.EAST);
		}

		int j = pos >> 8 & COORD_MASK;
		if (j == 0) {
			openFaces.add(Direction.DOWN);
		}
		else if (j == COORD_MASK) {
			openFaces.add(Direction.UP);
		}

		int k = pos >> 4 & COORD_MASK;
		if (k == 0) {
			openFaces.add(Direction.NORTH);
		}
		else if (k == COORD_MASK) {
			openFaces.add(Direction.SOUTH);
		}
	}

	private int offset(int pos, Direction direction) {
		switch (direction) {
			case DOWN:
				if ((pos >> 8 & COORD_MASK) == 0) {
					return -1;
				}

				return pos - STEP_Y;
			case UP:
				if ((pos >> 8 & COORD_MASK) == COORD_MASK) {
					return -1;
				}

				return pos + STEP_Y;
			case NORTH:
				if ((pos >> 4 & COORD_MASK) == 0) {
					return -1;
				}

				return pos - STEP_Z;
			case SOUTH:
				if ((pos >> 4 & COORD_MASK) == COORD_MASK) {
					return -1;
				}

				return pos + STEP_Z;
			case WEST:
				if ((pos >> 0 & COORD_MASK) == 0) {
					return -1;
				}

				return pos - STEP_X;
			case EAST:
				if ((pos >> 0 & COORD_MASK) == COORD_MASK) {
					return -1;
				}

				return pos + STEP_X;
			default:
				return -1;
		}
	}
}
