package net.minecraft.structure;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.WorldAccess;

/**
 * Базовый класс для структурных фрагментов, которые могут вертикально смещаться
 * при генерации, подстраиваясь под рельеф местности.
 * Хранит размеры фрагмента и кэшированную высоту {@code hPos}, вычисляемую
 * один раз при первом вызове методов выравнивания.
 */
public abstract class ShiftableStructurePiece extends StructurePiece {

	private static final int UNSET_HEIGHT = -1;

	protected final int width;
	protected final int height;
	protected final int depth;
	protected int hPos = UNSET_HEIGHT;

	protected ShiftableStructurePiece(
		StructurePieceType type,
		int x,
		int y,
		int z,
		int width,
		int height,
		int depth,
		Direction orientation
	) {
		super(type, 0, StructurePiece.createBox(x, y, z, orientation, width, height, depth));
		this.width = width;
		this.height = height;
		this.depth = depth;
		setOrientation(orientation);
	}

	protected ShiftableStructurePiece(StructurePieceType type, NbtCompound nbt) {
		super(type, nbt);
		width = nbt.getInt("Width", 0);
		height = nbt.getInt("Height", 0);
		depth = nbt.getInt("Depth", 0);
		hPos = nbt.getInt("HPos", 0);
	}

	@Override
	protected void writeNbt(StructureContext context, NbtCompound nbt) {
		nbt.putInt("Width", width);
		nbt.putInt("Height", height);
		nbt.putInt("Depth", depth);
		nbt.putInt("HPos", hPos);
	}

	/**
	 * Смещает фрагмент по вертикали так, чтобы его нижняя грань совпадала
	 * со средней высотой поверхности в пределах ограничивающего прямоугольника.
	 * Вычисление выполняется только один раз — результат кэшируется в {@code hPos}.
	 *
	 * @param world мир, из которого читается карта высот
	 * @param boundingBox ограничивающий прямоугольник чанка для фильтрации точек
	 * @param deltaY дополнительное вертикальное смещение относительно средней высоты
	 * @return {@code true}, если выравнивание выполнено успешно
	 */
	protected boolean adjustToAverageHeight(WorldAccess world, BlockBox boundingBox, int deltaY) {
		if (hPos >= 0) {
			return true;
		}

		int heightSum = 0;
		int sampleCount = 0;
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (int z = this.boundingBox.getMinZ(); z <= this.boundingBox.getMaxZ(); z++) {
			for (int x = this.boundingBox.getMinX(); x <= this.boundingBox.getMaxX(); x++) {
				mutable.set(x, 64, z);
				if (boundingBox.contains(mutable)) {
					heightSum += world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, mutable).getY();
					sampleCount++;
				}
			}
		}

		if (sampleCount == 0) {
			return false;
		}

		hPos = heightSum / sampleCount;
		this.boundingBox.move(0, hPos - this.boundingBox.getMinY() + deltaY, 0);
		return true;
	}

	/**
	 * Смещает фрагмент по вертикали так, чтобы его нижняя грань совпадала
	 * с минимальной высотой поверхности в пределах ограничивающего прямоугольника.
	 * Вычисление выполняется только один раз — результат кэшируется в {@code hPos}.
	 *
	 * @param world мир, из которого читается карта высот
	 * @param yOffset дополнительное вертикальное смещение относительно минимальной высоты
	 * @return {@code true}, если выравнивание выполнено успешно
	 */
	protected boolean adjustToMinHeight(WorldAccess world, int yOffset) {
		if (hPos >= 0) {
			return true;
		}

		int minHeight = world.getTopYInclusive() + 1;
		boolean hasSamples = false;
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (int z = boundingBox.getMinZ(); z <= boundingBox.getMaxZ(); z++) {
			for (int x = boundingBox.getMinX(); x <= boundingBox.getMaxX(); x++) {
				mutable.set(x, 0, z);
				minHeight = Math.min(
					minHeight,
					world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, mutable).getY()
				);
				hasSamples = true;
			}
		}

		if (!hasSamples) {
			return false;
		}

		hPos = minHeight;
		boundingBox.move(0, hPos - boundingBox.getMinY() + yOffset, 0);
		return true;
	}
}
