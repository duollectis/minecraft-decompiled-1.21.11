package net.minecraft.world.gen;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.structure.JigsawJunction;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * {@code StructureWeightSampler}.
 */
public class StructureWeightSampler implements DensityFunctionTypes.Beardifying {

	public static final int INDEX_OFFSET = 12;
	private static final int EDGE_LENGTH = 24;
	private static final float[] STRUCTURE_WEIGHT_TABLE = Util.make(
			new float[13824], array -> {
				for (int i = 0; i < 24; i++) {
					for (int j = 0; j < 24; j++) {
						for (int k = 0; k < 24; k++) {
							array[i * 24 * 24 + j * 24 + k] = (float) calculateStructureWeight(j - 12, k - 12, i - 12);
						}
					}
				}
			}
	);
	public static final StructureWeightSampler EMPTY = new StructureWeightSampler(List.of(), List.of(), null);
	private final List<StructureWeightSampler.Piece> pieces;
	private final List<JigsawJunction> junctions;
	private final @Nullable BlockBox boundingBox;

	public static StructureWeightSampler createStructureWeightSampler(
			StructureAccessor structureAccessor,
			ChunkPos chunkPos
	) {
		List<StructureStart> list = structureAccessor.getStructureStarts(
				chunkPos, structure -> structure.getTerrainAdaptation() != StructureTerrainAdaptation.NONE
		);
		if (list.isEmpty()) {
			return EMPTY;
		}
		else {
			int i = chunkPos.getStartX();
			int j = chunkPos.getStartZ();
			List<StructureWeightSampler.Piece> list2 = new ArrayList<>();
			List<JigsawJunction> list3 = new ArrayList<>();
			BlockBox blockBox = null;

			for (StructureStart structureStart : list) {
				StructureTerrainAdaptation
						structureTerrainAdaptation =
						structureStart.getStructure().getTerrainAdaptation();

				for (StructurePiece structurePiece : structureStart.getChildren()) {
					if (structurePiece.intersectsChunk(chunkPos, 12)) {
						if (structurePiece instanceof PoolStructurePiece poolStructurePiece) {
							StructurePool.Projection projection = poolStructurePiece.getPoolElement().getProjection();
							if (projection == StructurePool.Projection.RIGID) {
								list2.add(
										new StructureWeightSampler.Piece(
												poolStructurePiece.getBoundingBox(),
												structureTerrainAdaptation,
												poolStructurePiece.getGroundLevelDelta()
										)
								);
								blockBox = expandOrCreate(blockBox, structurePiece.getBoundingBox());
							}

							for (JigsawJunction jigsawJunction : poolStructurePiece.getJunctions()) {
								int k = jigsawJunction.getSourceX();
								int l = jigsawJunction.getSourceZ();
								if (k > i - 12 && l > j - 12 && k < i + 15 + 12 && l < j + 15 + 12) {
									list3.add(jigsawJunction);
									BlockBox
											blockBox2 =
											new BlockBox(new BlockPos(k, jigsawJunction.getSourceGroundY(), l));
									blockBox = expandOrCreate(blockBox, blockBox2);
								}
							}
						}
						else {
							list2.add(new StructureWeightSampler.Piece(
									structurePiece.getBoundingBox(),
									structureTerrainAdaptation,
									0
							));
							blockBox = expandOrCreate(blockBox, structurePiece.getBoundingBox());
						}
					}
				}
			}

			if (blockBox == null) {
				return EMPTY;
			}
			else {
				BlockBox blockBox3 = blockBox.expand(24);
				return new StructureWeightSampler(List.copyOf(list2), List.copyOf(list3), blockBox3);
			}
		}
	}

	private static BlockBox expandOrCreate(@Nullable BlockBox blockBox, BlockBox blockBox2) {
		return blockBox == null ? blockBox2 : BlockBox.createEncompassing(blockBox, blockBox2);
	}

	@VisibleForTesting
	public StructureWeightSampler(
			List<StructureWeightSampler.Piece> list,
			List<JigsawJunction> list2,
			@Nullable BlockBox blockBox
	) {
		this.pieces = list;
		this.junctions = list2;
		this.boundingBox = blockBox;
	}

	@Override
	public void fill(double[] densities, DensityFunction.EachApplier applier) {
		if (this.boundingBox == null) {
			Arrays.fill(densities, 0.0);
		}
		else {
			DensityFunctionTypes.Beardifying.super.fill(densities, applier);
		}
	}

	@Override
	public double sample(DensityFunction.NoisePos pos) {
		if (this.boundingBox == null) {
			return 0.0;
		}
		else {
			int i = pos.blockX();
			int j = pos.blockY();
			int k = pos.blockZ();
			if (!this.boundingBox.contains(i, j, k)) {
				return 0.0;
			}
			else {
				double d = 0.0;

				for (StructureWeightSampler.Piece piece : this.pieces) {
					BlockBox blockBox = piece.box();
					int l = piece.groundLevelDelta();
					int m = Math.max(0, Math.max(blockBox.getMinX() - i, i - blockBox.getMaxX()));
					int n = Math.max(0, Math.max(blockBox.getMinZ() - k, k - blockBox.getMaxZ()));
					int o = blockBox.getMinY() + l;
					int p = j - o;

					int q = switch (piece.terrainAdjustment()) {
						case NONE -> 0;
						case BURY, BEARD_THIN -> p;
						case BEARD_BOX -> Math.max(0, Math.max(o - j, j - blockBox.getMaxY()));
						case ENCAPSULATE -> Math.max(0, Math.max(blockBox.getMinY() - j, j - blockBox.getMaxY()));
					};

					d += switch (piece.terrainAdjustment()) {
						case NONE -> 0.0;
						case BURY -> getMagnitudeWeight(m, q / 2.0, n);
						case BEARD_THIN, BEARD_BOX -> getStructureWeight(m, q, n, p) * 0.8;
						case ENCAPSULATE -> getMagnitudeWeight(m / 2.0, q / 2.0, n / 2.0) * 0.8;
					};
				}

				for (JigsawJunction jigsawJunction : this.junctions) {
					int r = i - jigsawJunction.getSourceX();
					int l = j - jigsawJunction.getSourceGroundY();
					int m = k - jigsawJunction.getSourceZ();
					d += getStructureWeight(r, l, m, l) * 0.4;
				}

				return d;
			}
		}
	}

	@Override
	public double minValue() {
		return Double.NEGATIVE_INFINITY;
	}

	@Override
	public double maxValue() {
		return Double.POSITIVE_INFINITY;
	}

	private static double getMagnitudeWeight(double x, double y, double z) {
		double d = MathHelper.magnitude(x, y, z);
		return MathHelper.clampedMap(d, 0.0, 6.0, 1.0, 0.0);
	}

	private static double getStructureWeight(int x, int y, int z, int yy) {
		int i = x + 12;
		int j = y + 12;
		int k = z + 12;
		if (indexInBounds(i) && indexInBounds(j) && indexInBounds(k)) {
			double d = yy + 0.5;
			double e = MathHelper.squaredMagnitude(x, d, z);
			double f = -d * MathHelper.fastInverseSqrt(e / 2.0) / 2.0;
			return f * STRUCTURE_WEIGHT_TABLE[k * 24 * 24 + i * 24 + j];
		}
		else {
			return 0.0;
		}
	}

	private static boolean indexInBounds(int i) {
		return i >= 0 && i < 24;
	}

	private static double calculateStructureWeight(int x, int y, int z) {
		return structureWeight(x, y + 0.5, z);
	}

	private static double structureWeight(int x, double y, int z) {
		double d = MathHelper.squaredMagnitude(x, y, z);
		return Math.pow(Math.E, -d / 16.0);
	}

	@VisibleForTesting
	/**
	 * {@code Piece}.
	 */
	public record Piece(BlockBox box, StructureTerrainAdaptation terrainAdjustment, int groundLevelDelta) {
	}
}
