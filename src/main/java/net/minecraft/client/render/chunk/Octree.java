package net.minecraft.client.render.chunk;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Frustum;
import net.minecraft.util.math.*;
import org.jspecify.annotations.Nullable;

/**
 * Пространственная структура данных (октодерево) для иерархической организации
 * скомпилированных секций чанков ({@link ChunkBuilder.BuiltChunk}).
 * Позволяет эффективно обходить видимые секции с учётом фрустума камеры,
 * сортируя дочерние узлы по расстоянию от центра мира в порядке, определяемом {@link AxisOrder}.
 */
@Environment(EnvType.CLIENT)
public class Octree {

	private final Octree.Branch root;
	final BlockPos centerPos;

	public Octree(ChunkSectionPos sectionPos, int viewDistance, int sizeY, int bottomY) {
		int diameter = viewDistance * 2 + 1;
		int treeSize = MathHelper.smallestEncompassingPowerOfTwo(diameter);
		int radiusBlocks = viewDistance * 16;
		BlockPos minPos = sectionPos.getMinPos();
		centerPos = sectionPos.getCenterPos();
		int minX = minPos.getX() - radiusBlocks;
		int maxX = minX + treeSize * 16 - 1;
		int minY = treeSize >= sizeY ? bottomY : minPos.getY() - radiusBlocks;
		int maxY = minY + treeSize * 16 - 1;
		int minZ = minPos.getZ() - radiusBlocks;
		int maxZ = minZ + treeSize * 16 - 1;
		root = new Octree.Branch(new BlockBox(minX, minY, minZ, maxX, maxY, maxZ));
	}

	public boolean add(ChunkBuilder.BuiltChunk chunk) {
		return root.add(chunk);
	}

	/**
	 * Обходит все видимые узлы дерева в порядке от ближних к дальним относительно камеры.
	 *
	 * @param visitor колбэк, вызываемый для каждого видимого узла
	 * @param frustum фрустум камеры для отсечения невидимых узлов
	 * @param margin дополнительный отступ в блоках при проверке попадания центра в узел
	 */
	public void visit(Octree.Visitor visitor, Frustum frustum, int margin) {
		root.visit(visitor, false, frustum, 0, margin, true);
	}

	boolean isCenterWithin(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int margin) {
		int cx = centerPos.getX();
		int cy = centerPos.getY();
		int cz = centerPos.getZ();
		return cx > minX - margin && cx < maxX + margin
				&& cy > minY - margin && cy < maxY + margin
				&& cz > minZ - margin && cz < maxZ + margin;
	}

	/**
	 * Определяет порядок обхода дочерних узлов октодерева по трём осям.
	 * Значения {@code x}, {@code y}, {@code z} — битовые веса для вычисления индекса дочернего узла,
	 * обеспечивающие обход от ближней к дальней стороне относительно камеры.
	 */
	@Environment(EnvType.CLIENT)
	enum AxisOrder {
		XYZ(4, 2, 1),
		XZY(4, 1, 2),
		YXZ(2, 4, 1),
		YZX(1, 4, 2),
		ZXY(2, 1, 4),
		ZYX(1, 2, 4);

		final int x;
		final int y;
		final int z;

		AxisOrder(final int x, final int y, final int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		public static Octree.AxisOrder fromPos(int x, int y, int z) {
			if (x > y && x > z) {
				return y > z ? XYZ : XZY;
			}
			else if (y > x && y > z) {
				return x > z ? YXZ : YZX;
			}
			else {
				return x > y ? ZXY : ZYX;
			}
		}
	}

	/**
	 * Внутренний узел октодерева, содержащий до 8 дочерних узлов.
	 * При добавлении чанка определяет нужный квадрант по координатам секции
	 * и рекурсивно делегирует вставку. Порядок обхода дочерних узлов определяется
	 * {@link AxisOrder} — от ближних к дальним относительно центра мира.
	 */
	@Environment(EnvType.CLIENT)
	class Branch implements Octree.Node {

		private final Octree.@Nullable Node[] children = new Octree.Node[8];
		private final BlockBox box;
		private final int centerX;
		private final int centerY;
		private final int centerZ;
		private final Octree.AxisOrder axisOrder;
		private final boolean easternSide;
		private final boolean topSide;
		private final boolean southernSide;

		public Branch(final BlockBox box) {
			this.box = box;
			centerX = box.getMinX() + box.getBlockCountX() / 2;
			centerY = box.getMinY() + box.getBlockCountY() / 2;
			centerZ = box.getMinZ() + box.getBlockCountZ() / 2;
			int dx = centerPos.getX() - centerX;
			int dy = centerPos.getY() - centerY;
			int dz = centerPos.getZ() - centerZ;
			axisOrder = Octree.AxisOrder.fromPos(Math.abs(dx), Math.abs(dy), Math.abs(dz));
			easternSide = dx < 0;
			topSide = dy < 0;
			southernSide = dz < 0;
		}

		public boolean add(ChunkBuilder.BuiltChunk chunk) {
			long sectionPos = chunk.getSectionPos();
			boolean isWestern = ChunkSectionPos.getBlockCoord(ChunkSectionPos.unpackX(sectionPos)) - centerX < 0;
			boolean isBottom = ChunkSectionPos.getBlockCoord(ChunkSectionPos.unpackY(sectionPos)) - centerY < 0;
			boolean isNorthern = ChunkSectionPos.getBlockCoord(ChunkSectionPos.unpackZ(sectionPos)) - centerZ < 0;
			boolean sameX = isWestern != easternSide;
			boolean sameY = isBottom != topSide;
			boolean sameZ = isNorthern != southernSide;
			int childIndex = getIndex(axisOrder, sameX, sameY, sameZ);

			if (areChildrenLeaves()) {
				boolean existed = children[childIndex] != null;
				children[childIndex] = Octree.this.new Leaf(chunk);
				return !existed;
			}
			else if (children[childIndex] != null) {
				return ((Octree.Branch) children[childIndex]).add(chunk);
			}
			else {
				BlockBox childBox = getChildBox(isWestern, isBottom, isNorthern);
				Octree.Branch branch = Octree.this.new Branch(childBox);
				children[childIndex] = branch;
				return branch.add(chunk);
			}
		}

		private static int getIndex(
				Octree.AxisOrder axisOrder,
				boolean sameRelativeSideX,
				boolean sameRelativeSideY,
				boolean sameRelativeSideZ
		) {
			int index = 0;

			if (sameRelativeSideX) {
				index += axisOrder.x;
			}

			if (sameRelativeSideY) {
				index += axisOrder.y;
			}

			if (sameRelativeSideZ) {
				index += axisOrder.z;
			}

			return index;
		}

		private boolean areChildrenLeaves() {
			return box.getBlockCountX() == 32;
		}

		private BlockBox getChildBox(boolean western, boolean bottom, boolean northern) {
			int minX = western ? box.getMinX() : centerX;
			int maxX = western ? centerX - 1 : box.getMaxX();
			int minY = bottom ? box.getMinY() : centerY;
			int maxY = bottom ? centerY - 1 : box.getMaxY();
			int minZ = northern ? box.getMinZ() : centerZ;
			int maxZ = northern ? centerZ - 1 : box.getMaxZ();
			return new BlockBox(minX, minY, minZ, maxX, maxY, maxZ);
		}

		@Override
		public void visit(
				Octree.Visitor visitor,
				boolean skipVisibilityCheck,
				Frustum frustum,
				int depth,
				int margin,
				boolean nearCenter
		) {
			boolean visible = skipVisibilityCheck;

			if (!skipVisibilityCheck) {
				int intersection = frustum.intersectAab(box);
				skipVisibilityCheck = intersection == -2;
				visible = intersection == -2 || intersection == -1;
			}

			if (!visible) {
				return;
			}

			nearCenter = nearCenter && isCenterWithin(
					box.getMinX(), box.getMinY(), box.getMinZ(),
					box.getMaxX(), box.getMaxY(), box.getMaxZ(),
					margin
			);
			visitor.visit(this, skipVisibilityCheck, depth, nearCenter);

			for (Octree.Node node : children) {
				if (node != null) {
					node.visit(visitor, skipVisibilityCheck, frustum, depth + 1, margin, nearCenter);
				}
			}
		}

		@Override
		public ChunkBuilder.@Nullable BuiltChunk getBuiltChunk() {
			return null;
		}

		@Override
		public Box getBoundingBox() {
			return new Box(
					box.getMinX(),
					box.getMinY(),
					box.getMinZ(),
					box.getMaxX() + 1,
					box.getMaxY() + 1,
					box.getMaxZ() + 1
			);
		}
	}

	/** Листовой узел октодерева, хранящий ссылку на одну скомпилированную секцию чанка. */
	@Environment(EnvType.CLIENT)
	final class Leaf implements Octree.Node {

		private final ChunkBuilder.BuiltChunk chunk;

		Leaf(final ChunkBuilder.BuiltChunk chunk) {
			this.chunk = chunk;
		}

		@Override
		public void visit(
				Octree.Visitor visitor,
				boolean skipVisibilityCheck,
				Frustum frustum,
				int depth,
				int margin,
				boolean nearCenter
		) {
			Box box = chunk.getBoundingBox();

			if (!skipVisibilityCheck && !frustum.isVisible(box)) {
				return;
			}

			nearCenter = nearCenter && isCenterWithin(
					box.minX, box.minY, box.minZ,
					box.maxX, box.maxY, box.maxZ,
					margin
			);
			visitor.visit(this, skipVisibilityCheck, depth, nearCenter);
		}

		@Override
		public ChunkBuilder.BuiltChunk getBuiltChunk() {
			return chunk;
		}

		@Override
		public Box getBoundingBox() {
			return chunk.getBoundingBox();
		}
	}

	/** Узел октодерева — либо {@link Branch} (внутренний), либо {@link Leaf} (листовой). */
	@Environment(EnvType.CLIENT)
	public interface Node {

		void visit(
				Octree.Visitor visitor,
				boolean skipVisibilityCheck,
				Frustum frustum,
				int depth,
				int margin,
				boolean nearCenter
		);

		ChunkBuilder.@Nullable BuiltChunk getBuiltChunk();

		Box getBoundingBox();
	}

	/** Колбэк для обхода узлов октодерева методом {@link Octree#visit}. */
	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	public interface Visitor {

		void visit(Octree.Node node, boolean skipVisibilityCheck, int depth, boolean nearCenter);
	}
}
