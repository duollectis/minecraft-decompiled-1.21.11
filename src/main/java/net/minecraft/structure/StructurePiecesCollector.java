package net.minecraft.structure;

import com.google.common.collect.Lists;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.random.Random;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Коллектор кусков структуры с поддержкой вертикального смещения.
 * Реализует {@link StructurePiecesHolder} и накапливает куски для последующей
 * конвертации в иммутабельный {@link StructurePiecesList}.
 */
public class StructurePiecesCollector implements StructurePiecesHolder {

	private final List<StructurePiece> pieces = Lists.newArrayList();

	@Override
	public void addPiece(StructurePiece piece) {
		pieces.add(piece);
	}

	@Override
	public @Nullable StructurePiece getIntersecting(BlockBox box) {
		return StructurePiece.firstIntersecting(pieces, box);
	}

	/**
	 * Смещает все куски по оси Y на заданное значение.
	 *
	 * @deprecated используйте {@link #shiftInto(int, int, Random, int)} или {@link #shiftInto(Random, int, int)}
	 */
	@Deprecated
	public void shift(int y) {
		for (StructurePiece piece : pieces) {
			piece.translate(0, y, 0);
		}
	}

	/**
	 * Смещает куски так, чтобы верхняя граница оказалась в диапазоне [{@code bottomY}, {@code topY - topPenalty}].
	 *
	 * @return величина смещения по Y
	 * @deprecated используйте {@link #shiftInto(Random, int, int)}
	 */
	@Deprecated
	public int shiftInto(int topY, int bottomY, Random random, int topPenalty) {
		int maxY = topY - topPenalty;
		BlockBox box = getBoundingBox();
		int targetY = box.getBlockCountY() + bottomY + 1;
		if (targetY < maxY) {
			targetY += random.nextInt(maxY - targetY);
		}

		int delta = targetY - box.getMaxY();
		shift(delta);
		return delta;
	}

	/**
	 * Смещает куски так, чтобы нижняя граница оказалась в диапазоне [{@code baseY}, {@code topY}].
	 *
	 * @deprecated
	 */
	@Deprecated
	public void shiftInto(Random random, int baseY, int topY) {
		BlockBox box = getBoundingBox();
		int range = topY - baseY + 1 - box.getBlockCountY();
		int targetY = range > 1 ? baseY + random.nextInt(range) : baseY;
		shift(targetY - box.getMinY());
	}

	public StructurePiecesList toList() {
		return new StructurePiecesList(pieces);
	}

	public void clear() {
		pieces.clear();
	}

	public boolean isEmpty() {
		return pieces.isEmpty();
	}

	public BlockBox getBoundingBox() {
		return StructurePiece.boundingBox(pieces.stream());
	}
}
