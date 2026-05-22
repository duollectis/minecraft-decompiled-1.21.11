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
 * Сэмплер весов структур для адаптации рельефа под размещённые постройки.
 * Вычисляет плотностную поправку («бороду») вокруг кусков структур и джигсо-соединений,
 * чтобы ландшафт плавно переходил в основание структуры.
 */
public class StructureWeightSampler implements DensityFunctionTypes.Beardifying {

	public static final int INDEX_OFFSET = 12;
	private static final int EDGE_LENGTH = 24;
	private static final double MAGNITUDE_WEIGHT_MAX_DIST = 6.0;
	private static final double JUNCTION_WEIGHT_FACTOR = 0.4;
	private static final double PIECE_WEIGHT_FACTOR = 0.8;

	/** Предвычисленная таблица весов структуры размером 24×24×24. */
	private static final float[] STRUCTURE_WEIGHT_TABLE = Util.make(
		new float[EDGE_LENGTH * EDGE_LENGTH * EDGE_LENGTH],
		array -> {
			for (int x = 0; x < EDGE_LENGTH; x++) {
				for (int y = 0; y < EDGE_LENGTH; y++) {
					for (int z = 0; z < EDGE_LENGTH; z++) {
						array[x * EDGE_LENGTH * EDGE_LENGTH + y * EDGE_LENGTH + z] =
							(float) calculateStructureWeight(y - INDEX_OFFSET, z - INDEX_OFFSET, x - INDEX_OFFSET);
					}
				}
			}
		}
	);

	public static final StructureWeightSampler EMPTY = new StructureWeightSampler(List.of(), List.of(), null);

	private final List<Piece> pieces;
	private final List<JigsawJunction> junctions;
	private final @Nullable BlockBox boundingBox;

	/**
	 * Создаёт сэмплер весов для всех структур в чанке, требующих адаптации рельефа.
	 * Собирает куски структур и джигсо-соединения, попадающие в расширенный диапазон чанка.
	 */
	public static StructureWeightSampler createStructureWeightSampler(
		StructureAccessor structureAccessor,
		ChunkPos chunkPos
	) {
		List<StructureStart> starts = structureAccessor.getStructureStarts(
			chunkPos,
			structure -> structure.getTerrainAdaptation() != StructureTerrainAdaptation.NONE
		);

		if (starts.isEmpty()) {
			return EMPTY;
		}

		int chunkStartX = chunkPos.getStartX();
		int chunkStartZ = chunkPos.getStartZ();
		List<Piece> pieces = new ArrayList<>();
		List<JigsawJunction> junctions = new ArrayList<>();
		BlockBox combinedBox = null;

		for (StructureStart start : starts) {
			StructureTerrainAdaptation adaptation = start.getStructure().getTerrainAdaptation();

			for (StructurePiece piece : start.getChildren()) {
				if (!piece.intersectsChunk(chunkPos, INDEX_OFFSET)) {
					continue;
				}

				if (piece instanceof PoolStructurePiece poolPiece) {
					StructurePool.Projection projection = poolPiece.getPoolElement().getProjection();

					if (projection == StructurePool.Projection.RIGID) {
						pieces.add(new Piece(poolPiece.getBoundingBox(), adaptation, poolPiece.getGroundLevelDelta()));
						combinedBox = expandOrCreate(combinedBox, piece.getBoundingBox());
					}

					for (JigsawJunction junction : poolPiece.getJunctions()) {
						int sourceX = junction.getSourceX();
						int sourceZ = junction.getSourceZ();
						boolean inRangeX = sourceX > chunkStartX - INDEX_OFFSET
							&& sourceX < chunkStartX + 15 + INDEX_OFFSET;
						boolean inRangeZ = sourceZ > chunkStartZ - INDEX_OFFSET
							&& sourceZ < chunkStartZ + 15 + INDEX_OFFSET;

						if (inRangeX && inRangeZ) {
							junctions.add(junction);
							BlockBox junctionBox = new BlockBox(
								new BlockPos(sourceX, junction.getSourceGroundY(), sourceZ)
							);
							combinedBox = expandOrCreate(combinedBox, junctionBox);
						}
					}
				} else {
					pieces.add(new Piece(piece.getBoundingBox(), adaptation, 0));
					combinedBox = expandOrCreate(combinedBox, piece.getBoundingBox());
				}
			}
		}

		if (combinedBox == null) {
			return EMPTY;
		}

		return new StructureWeightSampler(List.copyOf(pieces), List.copyOf(junctions), combinedBox.expand(EDGE_LENGTH));
	}

	private static BlockBox expandOrCreate(@Nullable BlockBox existing, BlockBox addition) {
		return existing == null ? addition : BlockBox.createEncompassing(existing, addition);
	}

	@VisibleForTesting
	public StructureWeightSampler(List<Piece> pieces, List<JigsawJunction> junctions, @Nullable BlockBox boundingBox) {
		this.pieces = pieces;
		this.junctions = junctions;
		this.boundingBox = boundingBox;
	}

	@Override
	public void fill(double[] densities, DensityFunction.EachApplier applier) {
		if (boundingBox == null) {
			Arrays.fill(densities, 0.0);
		} else {
			DensityFunctionTypes.Beardifying.super.fill(densities, applier);
		}
	}

	@Override
	public double sample(DensityFunction.NoisePos pos) {
		if (boundingBox == null) {
			return 0.0;
		}

		int blockX = pos.blockX();
		int blockY = pos.blockY();
		int blockZ = pos.blockZ();

		if (!boundingBox.contains(blockX, blockY, blockZ)) {
			return 0.0;
		}

		double density = 0.0;

		for (Piece piece : pieces) {
			BlockBox box = piece.box();
			int groundLevelDelta = piece.groundLevelDelta();
			int distX = Math.max(0, Math.max(box.getMinX() - blockX, blockX - box.getMaxX()));
			int distZ = Math.max(0, Math.max(box.getMinZ() - blockZ, blockZ - box.getMaxZ()));
			int groundY = box.getMinY() + groundLevelDelta;
			int relY = blockY - groundY;

			int verticalDist = switch (piece.terrainAdjustment()) {
				case NONE -> 0;
				case BURY, BEARD_THIN -> relY;
				case BEARD_BOX -> Math.max(0, Math.max(groundY - blockY, blockY - box.getMaxY()));
				case ENCAPSULATE -> Math.max(0, Math.max(box.getMinY() - blockY, blockY - box.getMaxY()));
			};

			density += switch (piece.terrainAdjustment()) {
				case NONE -> 0.0;
				case BURY -> getMagnitudeWeight(distX, verticalDist / 2.0, distZ);
				case BEARD_THIN, BEARD_BOX -> getStructureWeight(distX, verticalDist, distZ, relY) * PIECE_WEIGHT_FACTOR;
				case ENCAPSULATE -> getMagnitudeWeight(distX / 2.0, verticalDist / 2.0, distZ / 2.0) * PIECE_WEIGHT_FACTOR;
			};
		}

		for (JigsawJunction junction : junctions) {
			int relX = blockX - junction.getSourceX();
			int relY = blockY - junction.getSourceGroundY();
			int relZ = blockZ - junction.getSourceZ();
			density += getStructureWeight(relX, relY, relZ, relY) * JUNCTION_WEIGHT_FACTOR;
		}

		return density;
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
		double magnitude = MathHelper.magnitude(x, y, z);
		return MathHelper.clampedMap(magnitude, 0.0, MAGNITUDE_WEIGHT_MAX_DIST, 1.0, 0.0);
	}

	/**
	 * Вычисляет вес структуры по таблице с учётом направленного затухания по оси Y.
	 * Использует предвычисленную таблицу {@link #STRUCTURE_WEIGHT_TABLE} для производительности.
	 */
	private static double getStructureWeight(int x, int y, int z, int rawY) {
		int tableX = x + INDEX_OFFSET;
		int tableY = y + INDEX_OFFSET;
		int tableZ = z + INDEX_OFFSET;

		if (!indexInBounds(tableX) || !indexInBounds(tableY) || !indexInBounds(tableZ)) {
			return 0.0;
		}

		double centeredY = rawY + 0.5;
		double squaredMag = MathHelper.squaredMagnitude(x, centeredY, z);
		double directionalFactor = -centeredY * MathHelper.fastInverseSqrt(squaredMag / 2.0) / 2.0;
		return directionalFactor * STRUCTURE_WEIGHT_TABLE[tableZ * EDGE_LENGTH * EDGE_LENGTH + tableX * EDGE_LENGTH + tableY];
	}

	private static boolean indexInBounds(int index) {
		return index >= 0 && index < EDGE_LENGTH;
	}

	private static double calculateStructureWeight(int x, int y, int z) {
		return structureWeight(x, y + 0.5, z);
	}

	private static double structureWeight(int x, double y, int z) {
		double squaredMag = MathHelper.squaredMagnitude(x, y, z);
		return Math.pow(Math.E, -squaredMag / 16.0);
	}

	/**
	 * Кусок структуры с информацией об адаптации рельефа.
	 */
	@VisibleForTesting
	public record Piece(BlockBox box, StructureTerrainAdaptation terrainAdjustment, int groundLevelDelta) {
	}
}
