package net.minecraft.nbt;

import net.minecraft.nbt.scanner.NbtScanner;
import net.minecraft.nbt.visitor.NbtElementVisitor;
import net.minecraft.nbt.visitor.StringNbtWriter;
import net.minecraft.util.math.MathHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * NBT-элемент, хранящий значение типа {@code float}.
 *
 * <p>Нулевое значение кэшируется в {@link #ZERO}. Используйте фабричный метод
 * {@link #of(float)} вместо конструктора record.</p>
 */
public record NbtFloat(float value) implements AbstractNbtNumber {

	/** Размер тега в байтах: 4 байта данных + 8 байт заголовка объекта. */
	private static final int SIZE = 12;

	public static final NbtFloat ZERO = new NbtFloat(0.0F);

	public static final NbtType<NbtFloat> TYPE = new NbtType.OfFixedSize<>() {
		@Override
		public NbtFloat read(DataInput input, NbtSizeTracker tracker) throws IOException {
			return NbtFloat.of(readFloat(input, tracker));
		}

		@Override
		public NbtScanner.Result doAccept(DataInput input, NbtScanner visitor, NbtSizeTracker tracker)
		throws IOException {
			return visitor.visitFloat(readFloat(input, tracker));
		}

		private static float readFloat(DataInput input, NbtSizeTracker tracker) throws IOException {
			tracker.add(SIZE);
			return input.readFloat();
		}

		@Override
		public int getSizeInBytes() {
			return 4;
		}

		@Override
		public String getCrashReportName() {
			return "FLOAT";
		}

		@Override
		public String getCommandFeedbackName() {
			return "TAG_Float";
		}
	};

	@Deprecated(forRemoval = true)
	public NbtFloat(float value) {
		this.value = value;
	}

	/**
	 * Возвращает {@link #ZERO} для нулевого значения, иначе создаёт новый объект.
	 *
	 * @param value значение float
	 * @return {@link NbtFloat} для заданного значения
	 */
	public static NbtFloat of(float value) {
		return value == 0.0F ? ZERO : new NbtFloat(value);
	}

	@Override
	public void write(DataOutput output) throws IOException {
		output.writeFloat(value);
	}

	@Override
	public int getSizeInBytes() {
		return SIZE;
	}

	@Override
	public byte getType() {
		return FLOAT_TYPE;
	}

	@Override
	public NbtType<NbtFloat> getNbtType() {
		return TYPE;
	}

	@Override
	public NbtFloat copy() {
		return this;
	}

	@Override
	public void accept(NbtElementVisitor visitor) {
		visitor.visitFloat(this);
	}

	@Override
	public long longValue() {
		return (long) value;
	}

	/**
	 * Возвращает целую часть float через {@link MathHelper#floor} для корректной обработки
	 * отрицательных значений (в отличие от простого приведения типа).
	 */
	@Override
	public int intValue() {
		return MathHelper.floor(value);
	}

	@Override
	public short shortValue() {
		return (short) (MathHelper.floor(value) & 0xFFFF);
	}

	@Override
	public byte byteValue() {
		return (byte) (MathHelper.floor(value) & 0xFF);
	}

	@Override
	public double doubleValue() {
		return value;
	}

	@Override
	public float floatValue() {
		return value;
	}

	@Override
	public Number numberValue() {
		return value;
	}

	@Override
	public NbtScanner.Result doAccept(NbtScanner visitor) {
		return visitor.visitFloat(value);
	}

	@Override
	public String toString() {
		StringNbtWriter writer = new StringNbtWriter();
		writer.visitFloat(this);
		return writer.getString();
	}
}
