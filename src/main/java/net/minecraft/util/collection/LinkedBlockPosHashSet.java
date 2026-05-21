package net.minecraft.util.collection;

import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.longs.Long2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import net.minecraft.util.math.MathHelper;

import java.util.NoSuchElementException;

/**
 * {@code LinkedBlockPosHashSet}.
 */
public class LinkedBlockPosHashSet extends LongLinkedOpenHashSet {

	private final LinkedBlockPosHashSet.Storage buffer;

	public LinkedBlockPosHashSet(int expectedSize, float loadFactor) {
		super(expectedSize, loadFactor);
		this.buffer = new LinkedBlockPosHashSet.Storage(expectedSize / 64, loadFactor);
	}

	/**
	 * Add.
	 *
	 * @param posLong pos long
	 *
	 * @return boolean — результат операции
	 */
	public boolean add(long posLong) {
		return this.buffer.add(posLong);
	}

	/**
	 * Rem.
	 *
	 * @param posLong pos long
	 *
	 * @return boolean — результат операции
	 */
	public boolean rem(long posLong) {
		return this.buffer.rem(posLong);
	}

	/**
	 * Удаляет first long.
	 *
	 * @return long — результат операции
	 */
	public long removeFirstLong() {
		return this.buffer.removeFirstLong();
	}

	/**
	 * Size.
	 *
	 * @return int — результат операции
	 */
	public int size() {
		throw new UnsupportedOperationException();
	}

	public boolean isEmpty() {
		return this.buffer.isEmpty();
	}

	/**
	 * {@code Storage}.
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
			int i = (int) (posLong >>> Z_BIT_OFFSET & 3L);
			int j = (int) (posLong >>> 0 & 3L);
			int k = (int) (posLong >>> X_BIT_OFFSET & 3L);
			return i << 4 | k << 2 | j;
		}

		static long getBlockPosLong(long key, int valueLength) {
			key |= (long) (valueLength >>> 4 & 3) << Z_BIT_OFFSET;
			key |= (long) (valueLength >>> 2 & 3) << X_BIT_OFFSET;
			return key | (long) (valueLength >>> 0 & 3) << 0;
		}

		/**
		 * Add.
		 *
		 * @param posLong pos long
		 *
		 * @return boolean — результат операции
		 */
		public boolean add(long posLong) {
			long l = getKey(posLong);
			int i = getBlockOffset(posLong);
			long m = 1L << i;
			int j;
			if (l == 0L) {
				if (this.containsNullKey) {
					return this.setBits(this.n, m);
				}

				this.containsNullKey = true;
				j = this.n;
			}
			else {
				if (this.lastWrittenIndex != -1 && l == this.lastWrittenKey) {
					return this.setBits(this.lastWrittenIndex, m);
				}

				long[] ls = this.key;
				j = (int) HashCommon.mix(l) & this.mask;

				for (long n = ls[j]; n != 0L; n = ls[j]) {
					if (n == l) {
						this.lastWrittenIndex = j;
						this.lastWrittenKey = l;
						return this.setBits(j, m);
					}

					j = j + 1 & this.mask;
				}
			}

			this.key[j] = l;
			this.value[j] = m;
			if (this.size == 0) {
				this.first = this.last = j;
				this.link[j] = -1L;
			}
			else {
				this.link[this.last] = this.link[this.last] ^ (this.link[this.last] ^ j & 4294967295L) & 4294967295L;
				this.link[j] = (this.last & 4294967295L) << 32 | 4294967295L;
				this.last = j;
			}

			if (this.size++ >= this.maxFill) {
				this.rehash(HashCommon.arraySize(this.size + 1, this.f));
			}

			return false;
		}

		private boolean setBits(int index, long mask) {
			boolean bl = (this.value[index] & mask) != 0L;
			this.value[index] = this.value[index] | mask;
			return bl;
		}

		/**
		 * Rem.
		 *
		 * @param posLong pos long
		 *
		 * @return boolean — результат операции
		 */
		public boolean rem(long posLong) {
			long l = getKey(posLong);
			int i = getBlockOffset(posLong);
			long m = 1L << i;
			if (l == 0L) {
				return this.containsNullKey ? this.unsetBits(m) : false;
			}
			else if (this.lastWrittenIndex != -1 && l == this.lastWrittenKey) {
				return this.unsetBitsAt(this.lastWrittenIndex, m);
			}
			else {
				long[] ls = this.key;
				int j = (int) HashCommon.mix(l) & this.mask;

				for (long n = ls[j]; n != 0L; n = ls[j]) {
					if (l == n) {
						this.lastWrittenIndex = j;
						this.lastWrittenKey = l;
						return this.unsetBitsAt(j, m);
					}

					j = j + 1 & this.mask;
				}

				return false;
			}
		}

		private boolean unsetBits(long mask) {
			if ((this.value[this.n] & mask) == 0L) {
				return false;
			}
			else {
				this.value[this.n] = this.value[this.n] & ~mask;
				if (this.value[this.n] != 0L) {
					return true;
				}
				else {
					this.containsNullKey = false;
					this.size--;
					this.fixPointers(this.n);
					if (this.size < this.maxFill / 4 && this.n > 16) {
						this.rehash(this.n / 2);
					}

					return true;
				}
			}
		}

		private boolean unsetBitsAt(int index, long mask) {
			if ((this.value[index] & mask) == 0L) {
				return false;
			}
			else {
				this.value[index] = this.value[index] & ~mask;
				if (this.value[index] != 0L) {
					return true;
				}
				else {
					this.lastWrittenIndex = -1;
					this.size--;
					this.fixPointers(index);
					this.shiftKeys(index);
					if (this.size < this.maxFill / 4 && this.n > 16) {
						this.rehash(this.n / 2);
					}

					return true;
				}
			}
		}

		/**
		 * Удаляет first long.
		 *
		 * @return long — результат операции
		 */
		public long removeFirstLong() {
			if (this.size == 0) {
				throw new NoSuchElementException();
			}
			else {
				int i = this.first;
				long l = this.key[i];
				int j = Long.numberOfTrailingZeros(this.value[i]);
				this.value[i] = this.value[i] & ~(1L << j);
				if (this.value[i] == 0L) {
					this.removeFirstLong();
					this.lastWrittenIndex = -1;
				}

				return getBlockPosLong(l, j);
			}
		}

		/**
		 * Rehash.
		 *
		 * @param newN new n
		 */
		protected void rehash(int newN) {
			if (newN > this.expectedSize) {
				super.rehash(newN);
			}
		}
	}
}
