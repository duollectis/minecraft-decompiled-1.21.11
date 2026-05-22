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
 * NBT-тег, хранящий массив байт ({@code TAG_Byte_Array}).
 * <p>
 * Реализует {@link AbstractNbtList}, что позволяет обращаться к элементам
 * через индекс и использовать стандартные операции добавления/удаления.
 * Каждый элемент при доступе через {@link #get(int)} оборачивается в {@link NbtByte}.
 */
public final class NbtByteArray implements AbstractNbtList {

	private static final int SIZE = 24;
	private static final int BYTES_PER_ELEMENT = 1;

	public static final NbtType<NbtByteArray> TYPE = new NbtType.OfVariableSize<NbtByteArray>() {
		@Override
		public NbtByteArray read(DataInput input, NbtSizeTracker tracker) throws IOException {
			return new NbtByteArray(readByteArray(input, tracker));
		}

		@Override
		public NbtScanner.Result doAccept(DataInput input, NbtScanner visitor, NbtSizeTracker tracker)
		throws IOException {
			return visitor.visitByteArray(readByteArray(input, tracker));
		}

		private static byte[] readByteArray(DataInput input, NbtSizeTracker tracker) throws IOException {
			tracker.add(SIZE);
			int length = input.readInt();
			tracker.add(BYTES_PER_ELEMENT, length);
			byte[] bytes = new byte[length];
			input.readFully(bytes);
			return bytes;
		}

		@Override
		public void skip(DataInput input, NbtSizeTracker tracker) throws IOException {
			input.skipBytes(input.readInt() * BYTES_PER_ELEMENT);
		}

		@Override
		public String getCrashReportName() {
			return "BYTE[]";
		}

		@Override
		public String getCommandFeedbackName() {
			return "TAG_Byte_Array";
		}
	};

	private byte[] value;

	public NbtByteArray(byte[] value) {
		this.value = value;
	}

	@Override
	public void write(DataOutput output) throws IOException {
		output.writeInt(value.length);
		output.write(value);
	}

	@Override
	public int getSizeInBytes() {
		return SIZE + BYTES_PER_ELEMENT * value.length;
	}

	@Override
	public byte getType() {
		return BYTE_ARRAY_TYPE;
	}

	@Override
	public NbtType<NbtByteArray> getNbtType() {
		return TYPE;
	}

	@Override
	public String toString() {
		StringNbtWriter writer = new StringNbtWriter();
		writer.visitByteArray(this);
		return writer.getString();
	}

	@Override
	public NbtElement copy() {
		byte[] copy = new byte[value.length];
		System.arraycopy(value, 0, copy, 0, value.length);
		return new NbtByteArray(copy);
	}

	@Override
	public boolean equals(Object other) {
		return this == other
			? true
			: other instanceof NbtByteArray nbtByteArray && Arrays.equals(value, nbtByteArray.value);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(value);
	}

	@Override
	public void accept(NbtElementVisitor visitor) {
		visitor.visitByteArray(this);
	}

	public byte[] getByteArray() {
		return value;
	}

	@Override
	public int size() {
		return value.length;
	}

	@Override
	public NbtByte get(int index) {
		return NbtByte.of(value[index]);
	}

	@Override
	public boolean setElement(int index, NbtElement element) {
		if (element instanceof AbstractNbtNumber number) {
			value[index] = number.byteValue();
			return true;
		}

		return false;
	}

	@Override
	public boolean addElement(int index, NbtElement element) {
		if (element instanceof AbstractNbtNumber number) {
			value = ArrayUtils.add(value, index, number.byteValue());
			return true;
		}

		return false;
	}

	/**
	 * Удаляет элемент по индексу и возвращает его как {@link NbtByte}.
	 *
	 * @param index индекс удаляемого элемента
	 * @return удалённый байт, обёрнутый в {@link NbtByte}
	 */
	public NbtByte remove(int index) {
		byte removed = value[index];
		value = ArrayUtils.remove(value, index);
		return NbtByte.of(removed);
	}

	@Override
	public void clear() {
		value = new byte[0];
	}

	@Override
	public Optional<byte[]> asByteArray() {
		return Optional.of(value);
	}

	@Override
	public NbtScanner.Result doAccept(NbtScanner visitor) {
		return visitor.visitByteArray(value);
	}
}
