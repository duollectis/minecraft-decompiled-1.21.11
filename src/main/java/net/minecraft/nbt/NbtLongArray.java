package net.minecraft.nbt;

import net.minecraft.nbt.scanner.NbtScanner;
import net.minecraft.nbt.visitor.NbtElementVisitor;
import net.minecraft.nbt.visitor.StringNbtWriter;
import org.apache.commons.lang3.ArrayUtils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

/**
 * NBT-тег, хранящий массив 64-битных целых чисел ({@code TAG_Long_Array}).
 * <p>
 * Реализует {@link AbstractNbtList}, что позволяет обращаться к элементам
 * через индекс и использовать стандартные операции добавления/удаления.
 * Каждый элемент при доступе через {@link #get(int)} оборачивается в {@link NbtLong}.
 */
public final class NbtLongArray implements AbstractNbtList {

	private static final int SIZE = 24;
	private static final int BYTES_PER_ELEMENT = 8;

	public static final NbtType<NbtLongArray> TYPE = new NbtType.OfVariableSize<NbtLongArray>() {
		@Override
		public NbtLongArray read(DataInput input, NbtSizeTracker tracker) throws IOException {
			return new NbtLongArray(readLongArray(input, tracker));
		}

		@Override
		public NbtScanner.Result doAccept(DataInput input, NbtScanner visitor, NbtSizeTracker tracker)
		throws IOException {
			return visitor.visitLongArray(readLongArray(input, tracker));
		}

		private static long[] readLongArray(DataInput input, NbtSizeTracker tracker) throws IOException {
			tracker.add(SIZE);
			int length = input.readInt();
			tracker.add(BYTES_PER_ELEMENT, length);
			long[] longs = new long[length];

			for (int index = 0; index < length; index++) {
				longs[index] = input.readLong();
			}

			return longs;
		}

		@Override
		public void skip(DataInput input, NbtSizeTracker tracker) throws IOException {
			input.skipBytes(input.readInt() * BYTES_PER_ELEMENT);
		}

		@Override
		public String getCrashReportName() {
			return "LONG[]";
		}

		@Override
		public String getCommandFeedbackName() {
			return "TAG_Long_Array";
		}
	};

	private long[] value;

	public NbtLongArray(long[] value) {
		this.value = value;
	}

	@Override
	public void write(DataOutput output) throws IOException {
		output.writeInt(value.length);

		for (long element : value) {
			output.writeLong(element);
		}
	}

	@Override
	public int getSizeInBytes() {
		return SIZE + BYTES_PER_ELEMENT * value.length;
	}

	@Override
	public byte getType() {
		return LONG_ARRAY_TYPE;
	}

	@Override
	public NbtType<NbtLongArray> getNbtType() {
		return TYPE;
	}

	@Override
	public String toString() {
		StringNbtWriter writer = new StringNbtWriter();
		writer.visitLongArray(this);
		return writer.getString();
	}

	/**
	 * Создаёт глубокую копию массива.
	 *
	 * @return новый {@link NbtLongArray} с независимой копией данных
	 */
	public NbtLongArray copy() {
		long[] copy = new long[value.length];
		System.arraycopy(value, 0, copy, 0, value.length);
		return new NbtLongArray(copy);
	}

	@Override
	public boolean equals(Object other) {
		return this == other
			? true
			: other instanceof NbtLongArray nbtLongArray && Arrays.equals(value, nbtLongArray.value);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(value);
	}

	@Override
	public void accept(NbtElementVisitor visitor) {
		visitor.visitLongArray(this);
	}

	public long[] getLongArray() {
		return value;
	}

	@Override
	public int size() {
		return value.length;
	}

	@Override
	public NbtLong get(int index) {
		return NbtLong.of(value[index]);
	}

	@Override
	public boolean setElement(int index, NbtElement element) {
		if (element instanceof AbstractNbtNumber number) {
			value[index] = number.longValue();
			return true;
		}

		return false;
	}

	@Override
	public boolean addElement(int index, NbtElement element) {
		if (element instanceof AbstractNbtNumber number) {
			value = ArrayUtils.add(value, index, number.longValue());
			return true;
		}

		return false;
	}

	/**
	 * Удаляет элемент по индексу и возвращает его как {@link NbtLong}.
	 *
	 * @param index индекс удаляемого элемента
	 * @return удалённое значение, обёрнутое в {@link NbtLong}
	 */
	public NbtLong remove(int index) {
		long removed = value[index];
		value = ArrayUtils.remove(value, index);
		return NbtLong.of(removed);
	}

	@Override
	public void clear() {
		value = new long[0];
	}

	@Override
	public Optional<long[]> asLongArray() {
		return Optional.of(value);
	}

	@Override
	public NbtScanner.Result doAccept(NbtScanner visitor) {
		return visitor.visitLongArray(value);
	}
}
