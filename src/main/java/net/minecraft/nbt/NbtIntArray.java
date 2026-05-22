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
 * NBT-тег, хранящий массив 32-битных целых чисел ({@code TAG_Int_Array}).
 * <p>
 * Реализует {@link AbstractNbtList}, что позволяет обращаться к элементам
 * через индекс и использовать стандартные операции добавления/удаления.
 * Каждый элемент при доступе через {@link #get(int)} оборачивается в {@link NbtInt}.
 */
public final class NbtIntArray implements AbstractNbtList {

	private static final int SIZE = 24;
	private static final int BYTES_PER_ELEMENT = 4;

	public static final NbtType<NbtIntArray> TYPE = new NbtType.OfVariableSize<NbtIntArray>() {
		@Override
		public NbtIntArray read(DataInput input, NbtSizeTracker tracker) throws IOException {
			return new NbtIntArray(readIntArray(input, tracker));
		}

		@Override
		public NbtScanner.Result doAccept(DataInput input, NbtScanner visitor, NbtSizeTracker tracker)
		throws IOException {
			return visitor.visitIntArray(readIntArray(input, tracker));
		}

		private static int[] readIntArray(DataInput input, NbtSizeTracker tracker) throws IOException {
			tracker.add(SIZE);
			int length = input.readInt();
			tracker.add(BYTES_PER_ELEMENT, length);
			int[] ints = new int[length];

			for (int index = 0; index < length; index++) {
				ints[index] = input.readInt();
			}

			return ints;
		}

		@Override
		public void skip(DataInput input, NbtSizeTracker tracker) throws IOException {
			input.skipBytes(input.readInt() * BYTES_PER_ELEMENT);
		}

		@Override
		public String getCrashReportName() {
			return "INT[]";
		}

		@Override
		public String getCommandFeedbackName() {
			return "TAG_Int_Array";
		}
	};

	private int[] value;

	public NbtIntArray(int[] value) {
		this.value = value;
	}

	@Override
	public void write(DataOutput output) throws IOException {
		output.writeInt(value.length);

		for (int element : value) {
			output.writeInt(element);
		}
	}

	@Override
	public int getSizeInBytes() {
		return SIZE + BYTES_PER_ELEMENT * value.length;
	}

	@Override
	public byte getType() {
		return INT_ARRAY_TYPE;
	}

	@Override
	public NbtType<NbtIntArray> getNbtType() {
		return TYPE;
	}

	@Override
	public String toString() {
		StringNbtWriter writer = new StringNbtWriter();
		writer.visitIntArray(this);
		return writer.getString();
	}

	/**
	 * Создаёт глубокую копию массива.
	 *
	 * @return новый {@link NbtIntArray} с независимой копией данных
	 */
	public NbtIntArray copy() {
		int[] copy = new int[value.length];
		System.arraycopy(value, 0, copy, 0, value.length);
		return new NbtIntArray(copy);
	}

	@Override
	public boolean equals(Object other) {
		return this == other
			? true
			: other instanceof NbtIntArray nbtIntArray && Arrays.equals(value, nbtIntArray.value);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(value);
	}

	public int[] getIntArray() {
		return value;
	}

	@Override
	public void accept(NbtElementVisitor visitor) {
		visitor.visitIntArray(this);
	}

	@Override
	public int size() {
		return value.length;
	}

	@Override
	public NbtInt get(int index) {
		return NbtInt.of(value[index]);
	}

	@Override
	public boolean setElement(int index, NbtElement element) {
		if (element instanceof AbstractNbtNumber number) {
			value[index] = number.intValue();
			return true;
		}

		return false;
	}

	@Override
	public boolean addElement(int index, NbtElement element) {
		if (element instanceof AbstractNbtNumber number) {
			value = ArrayUtils.add(value, index, number.intValue());
			return true;
		}

		return false;
	}

	/**
	 * Удаляет элемент по индексу и возвращает его как {@link NbtInt}.
	 *
	 * @param index индекс удаляемого элемента
	 * @return удалённое значение, обёрнутое в {@link NbtInt}
	 */
	public NbtInt remove(int index) {
		int removed = value[index];
		value = ArrayUtils.remove(value, index);
		return NbtInt.of(removed);
	}

	@Override
	public void clear() {
		value = new int[0];
	}

	@Override
	public Optional<int[]> asIntArray() {
		return Optional.of(value);
	}

	@Override
	public NbtScanner.Result doAccept(NbtScanner visitor) {
		return visitor.visitIntArray(value);
	}
}
