package net.minecraft.world.gen.carver;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.BitSet;
import java.util.stream.Stream;

/**
 * Битовая маска, отслеживающая, какие позиции в чанке уже были обработаны карвером.
 * Индексирование: (offsetX & 15) | ((offsetZ & 15) << 4) | ((y - bottomY) << 8).
 */
public class CarvingMask {

	private final int bottomY;
	private final BitSet mask;
	private CarvingMask.MaskPredicate maskPredicate = (offsetX, y, offsetZ) -> false;

	public CarvingMask(int height, int bottomY) {
		this.bottomY = bottomY;
		this.mask = new BitSet(256 * height);
	}

	public CarvingMask(long[] mask, int bottomY) {
		this.bottomY = bottomY;
		this.mask = BitSet.valueOf(mask);
	}

	public void setMaskPredicate(CarvingMask.MaskPredicate maskPredicate) {
		this.maskPredicate = maskPredicate;
	}

	public void set(int offsetX, int y, int offsetZ) {
		mask.set(getIndex(offsetX, y, offsetZ));
	}

	public boolean get(int offsetX, int y, int offsetZ) {
		return maskPredicate.test(offsetX, y, offsetZ) || mask.get(getIndex(offsetX, y, offsetZ));
	}

	/**
	 * Возвращает поток всех помеченных позиций в виде {@link BlockPos} относительно чанка.
	 */
	public Stream<BlockPos> streamBlockPos(ChunkPos chunkPos) {
		return mask.stream().mapToObj(index -> {
			int localX = index & 15;
			int localZ = index >> 4 & 15;
			int relY = index >> 8;
			return chunkPos.getBlockPos(localX, relY + bottomY, localZ);
		});
	}

	public long[] getMask() {
		return mask.toLongArray();
	}

	private int getIndex(int offsetX, int y, int offsetZ) {
		return offsetX & 15 | (offsetZ & 15) << 4 | y - bottomY << 8;
	}

	/**
	 * Дополнительный предикат, позволяющий динамически помечать позиции как уже обработанные.
	 */
	public interface MaskPredicate {

		boolean test(int offsetX, int y, int offsetZ);
	}
}
