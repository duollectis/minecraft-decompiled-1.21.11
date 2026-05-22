package net.minecraft.world;

import com.google.common.collect.AbstractIterator;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.util.CuboidBlockIterator;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.jspecify.annotations.Nullable;

import java.util.function.BiFunction;

/**
 * Итератор столкновений блоков в заданном AABB-объёме.
 * Перебирает все блоки в кубоиде и возвращает их формы столкновений,
 * пересекающиеся с исходным боксом.
 *
 * @param <T> тип результата, возвращаемого функцией-маппером
 */
public class BlockCollisionSpliterator<T> extends AbstractIterator<T> {

	/** Погрешность расширения границ для надёжного захвата граничных блоков. */
	private static final double BOUNDARY_EPSILON = 1.0E-7;

	private final Box box;
	private final ShapeContext context;
	private final CuboidBlockIterator blockIterator;
	private final BlockPos.Mutable pos;
	private final VoxelShape boxShape;
	private final CollisionView world;
	private final boolean forEntity;
	private @Nullable BlockView chunk;
	private long chunkPos;
	private final BiFunction<BlockPos.Mutable, VoxelShape, T> resultFunction;

	public BlockCollisionSpliterator(
		CollisionView world,
		@Nullable Entity entity,
		Box box,
		boolean forEntity,
		BiFunction<BlockPos.Mutable, VoxelShape, T> resultFunction
	) {
		this(
			world,
			entity == null ? ShapeContext.absent() : ShapeContext.of(entity),
			box,
			forEntity,
			resultFunction
		);
	}

	public BlockCollisionSpliterator(
		CollisionView world,
		ShapeContext context,
		Box box,
		boolean forEntity,
		BiFunction<BlockPos.Mutable, VoxelShape, T> resultFunction
	) {
		this.context = context;
		this.pos = new BlockPos.Mutable();
		this.boxShape = VoxelShapes.cuboid(box);
		this.world = world;
		this.box = box;
		this.forEntity = forEntity;
		this.resultFunction = resultFunction;

		int minX = MathHelper.floor(box.minX - BOUNDARY_EPSILON) - 1;
		int maxX = MathHelper.floor(box.maxX + BOUNDARY_EPSILON) + 1;
		int minY = MathHelper.floor(box.minY - BOUNDARY_EPSILON) - 1;
		int maxY = MathHelper.floor(box.maxY + BOUNDARY_EPSILON) + 1;
		int minZ = MathHelper.floor(box.minZ - BOUNDARY_EPSILON) - 1;
		int maxZ = MathHelper.floor(box.maxZ + BOUNDARY_EPSILON) + 1;

		this.blockIterator = new CuboidBlockIterator(minX, minY, minZ, maxX, maxY, maxZ);
	}

	private @Nullable BlockView getChunk(int x, int z) {
		int chunkX = ChunkSectionPos.getSectionCoord(x);
		int chunkZ = ChunkSectionPos.getSectionCoord(z);
		long packedPos = ChunkPos.toLong(chunkX, chunkZ);

		if (this.chunk != null && this.chunkPos == packedPos) {
			return this.chunk;
		}

		BlockView blockView = this.world.getChunkAsView(chunkX, chunkZ);
		this.chunk = blockView;
		this.chunkPos = packedPos;

		return blockView;
	}

	/**
	 * Вычисляет следующий элемент итерации — форму столкновения блока,
	 * пересекающуюся с исходным боксом.
	 * Пропускает блоки на рёбрах куба (edgeCount == 3), а также
	 * применяет оптимизации для граничных случаев (edgeCount 1 и 2).
	 */
	@Override
	protected T computeNext() {
		while (this.blockIterator.step()) {
			int x = this.blockIterator.getX();
			int y = this.blockIterator.getY();
			int z = this.blockIterator.getZ();
			int edgeCount = this.blockIterator.getEdgeCoordinatesCount();

			// Блоки в углах куба (3 граничных координаты) пропускаем — они не могут пересекаться
			if (edgeCount == 3) {
				continue;
			}

			BlockView blockView = this.getChunk(x, z);
			if (blockView == null) {
				continue;
			}

			this.pos.set(x, y, z);
			BlockState blockState = blockView.getBlockState(this.pos);

			boolean passesEntityCheck = !this.forEntity || blockState.shouldSuffocate(blockView, this.pos);
			boolean passesEdgeCheck1 = edgeCount != 1 || blockState.exceedsCube();
			boolean passesEdgeCheck2 = edgeCount != 2 || blockState.isOf(Blocks.MOVING_PISTON);

			if (passesEntityCheck && passesEdgeCheck1 && passesEdgeCheck2) {
				VoxelShape shape = this.context.getCollisionShape(blockState, this.world, this.pos);

				if (shape == VoxelShapes.fullCube()) {
					if (this.box.intersects(x, y, z, x + 1.0, y + 1.0, z + 1.0)) {
						return this.resultFunction.apply(this.pos, shape.offset(this.pos));
					}
				} else {
					VoxelShape offsetShape = shape.offset(this.pos);
					if (!offsetShape.isEmpty()
						&& VoxelShapes.matchesAnywhere(offsetShape, this.boxShape, BooleanBiFunction.AND)
					) {
						return this.resultFunction.apply(this.pos, offsetShape);
					}
				}
			}
		}

		return (T) this.endOfData();
	}
}
