package net.minecraft.util.collection;

import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.longs.Long2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import net.minecraft.util.math.MathHelper;

import java.util.NoSuchElementException;

/**
 * Хэш-множество позиций блоков с сохранением порядка вставки.
 * Использует компактное хранилище: несколько позиций блоков упаковываются
 * в одну запись карты через битовые маски, что снижает накладные расходы памяти.
 */
public class LinkedBlockPosHashSet extends LongLinkedOpenHashSet {

	private final Storage buffer;

	public LinkedBlockPosHashSet(int expectedSize, float loadFactor) {
		super(expectedSize, loadFactor);
		buffer = new Storage(expectedSize / 64, loadFactor);
	}

	public boolean add(long posLong) {
		return buffer.add(posLong);
	}

	public boolean rem(long posLong) {
		return buffer.rem(posLong);
	}

	@Override
	public long removeFirstLong() {
		return buffer.removeFirstLong();
	}

	@Override
	public int size() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isEmpty() {
		return buffer.isEmpty();
	}

	/**
	 * Компактное хранилище позиций блоков на основе {@link Long2LongLinkedOpenHashMap}.
	 * Группирует позиции блоков по ключу (старшие биты) и хранит набор смещений
	 * внутри группы в виде битовой маски (64 бита = до 64 позиций на ключ).
	 *
	 * <p>Биты позиции распределены следующим образом:
	 * <ul>
	 *   <li>биты Y: позиции 0..FIELD_SPACING-1</li>
	 *   <li>биты X: позиции X_BIT_OFFSET..X_BIT_OFFSET+HORIZONTAL_COLUMN_BIT_SEPARATION-1</li>
	 *   <li>биты Z: позиции Z_BIT_OFFSET..Z_BIT_OFFSET+HORIZONTAL_COLUMN_BIT_SEPARATION-1</li>
	 * </ul>
	 */
	protected static class Storage extends Long2LongLinkedOpenHashMap {

		private static final int STARTING_OFFSET = MathHelper.floorLog2(60000000);
		private static final int HORIZONTAL_COLUMN_BIT_SEPARATION = MathHelper.floorLog2(60000000);
		private static final int FIELD_SPACING = 64 - STARTING_OFFSET - HORIZONTAL_COLUMN_BIT_SEPARATION;
		private static final int Y_BIT_OFFSET = 0;
		private static final int X_BIT_OFFSET = FIELD_SPACING;
		private static final int Z_BIT_OFFSET = FIELD_SPACING + HORIZONTAL_COLUMN_BIT_SEPARATION;
		private static final long MAX_POSITION = 3L << Z_BIT_OFFSET | 3L | 3L << X_BIT_OFFSET;

		private int lastWrittenIndex = -1;
		private long lastWrittenKey;
		private final int expectedSize;

		public Storage(int expectedSize, float loadFactor) {
			super(expectedSize, loadFactor);
			this.expectedSize = expectedSize;
		}

		static long getKey(long posLong) {
			return posLong & ~MAX_POSITION;
		}

		static int getBlockOffset(long posLong) {
			int zBits = (int) (posLong >>> Z_BIT_OFFSET & 3L);
			int yBits = (int) (posLong >>> Y_BIT_OFFSET & 3L);
			int xBits = (int) (posLong >>> X_BIT_OFFSET & 3L);
			return zBits << 4 | xBits << 2 | yBits;
		}

		static long getBlockPosLong(long key, int offset) {
			key |= (long) (offset >>> 4 & 3) << Z_BIT_OFFSET;
			key |= (long) (offset >>> 2 & 3) << X_BIT_OFFSET;
			return key | (long) (offset & 3) << Y_BIT_OFFSET;
		}

		public boolean add(long posLong) {
			long groupKey = getKey(posLong);
			int blockOffset = getBlockOffset(posLong);
			long bitMask = 1L << blockOffset;

			if (groupKey == 0L) {
				if (containsNullKey) {
					return setBits(n, bitMask);
				}

				containsNullKey = true;
				return insertNewGroup(n, groupKey, bitMask);
			}

			if (lastWrittenIndex != -1 && groupKey == lastWrittenKey) {
				return setBits(lastWrittenIndex, bitMask);
			}

			long[] keys = key;
			int slot = (int) HashCommon.mix(groupKey) & mask;

			for (long existing = keys[slot]; existing != 0L; existing = keys[slot]) {
				if (existing == groupKey) {
					lastWrittenIndex = slot;
					lastWrittenKey = groupKey;
					return setBits(slot, bitMask);
				}

				slot = slot + 1 & mask;
			}

			return insertNewGroup(slot, groupKey, bitMask);
		}

		private boolean insertNewGroup(int slot, long groupKey, long bitMask) {
			key[slot] = groupKey;
			value[slot] = bitMask;

			if (size == 0) {
				first = last = slot;
				link[slot] = -1L;
			} else {
				link[last] = link[last] ^ (link[last] ^ slot & 4294967295L) & 4294967295L;
				link[slot] = (last & 4294967295L) << 32 | 4294967295L;
				last = slot;
			}

			if (size++ >= maxFill) {
				rehash(HashCommon.arraySize(size + 1, f));
			}

			return false;
		}

		private boolean setBits(int index, long mask) {
			boolean alreadySet = (value[index] & mask) != 0L;
			value[index] = value[index] | mask;
			return alreadySet;
		}

		public boolean rem(long posLong) {
			long groupKey = getKey(posLong);
			int blockOffset = getBlockOffset(posLong);
			long bitMask = 1L << blockOffset;

			if (groupKey == 0L) {
				return containsNullKey ? unsetBits(bitMask) : false;
			}

			if (lastWrittenIndex != -1 && groupKey == lastWrittenKey) {
				return unsetBitsAt(lastWrittenIndex, bitMask);
			}

			long[] keys = key;
			int slot = (int) HashCommon.mix(groupKey) & mask;

			for (long existing = keys[slot]; existing != 0L; existing = keys[slot]) {
				if (groupKey == existing) {
					lastWrittenIndex = slot;
					lastWrittenKey = groupKey;
					return unsetBitsAt(slot, bitMask);
				}

				slot = slot + 1 & mask;
			}

			return false;
		}

		private boolean unsetBits(long mask) {
			if ((value[n] & mask) == 0L) {
				return false;
			}

			value[n] = value[n] & ~mask;

			if (value[n] != 0L) {
				return true;
			}

			containsNullKey = false;
			size--;
			fixPointers(n);

			if (size < maxFill / 4 && n > 16) {
				rehash(n / 2);
			}

			return true;
		}

		private boolean unsetBitsAt(int index, long mask) {
			if ((value[index] & mask) == 0L) {
				return false;
			}

			value[index] = value[index] & ~mask;

			if (value[index] != 0L) {
				return true;
			}

			lastWrittenIndex = -1;
			size--;
			fixPointers(index);
			shiftKeys(index);

			if (size < maxFill / 4 && n > 16) {
				rehash(n / 2);
			}

			return true;
		}

		@Override
		public long removeFirstLong() {
			if (size == 0) {
				throw new NoSuchElementException();
			}

			int firstSlot = first;
			long groupKey = key[firstSlot];
			int bitIndex = Long.numberOfTrailingZeros(value[firstSlot]);
			value[firstSlot] = value[firstSlot] & ~(1L << bitIndex);

			if (value[firstSlot] == 0L) {
				super.removeFirstLong();
				lastWrittenIndex = -1;
			}

			return getBlockPosLong(groupKey, bitIndex);
		}

		@Override
		protected void rehash(int newN) {
			if (newN > expectedSize) {
				super.rehash(newN);
			}
		}
	}
}
