package net.minecraft.nbt;

import net.minecraft.nbt.scanner.NbtScanner;
import net.minecraft.nbt.visitor.NbtElementVisitor;
import net.minecraft.nbt.visitor.StringNbtWriter;
import net.minecraft.util.math.MathHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * NBT-элемент, хранящий значение типа {@code double}.
 *
 * <p>Нулевое значение кэшируется в {@link #ZERO}. Используйте фабричный метод
 * {@link #of(double)} вместо конструктора record.</p>
 */
public record NbtDouble(double value) implements AbstractNbtNumber {

	/** Размер тега в байтах: 8 байт данных + 8 байт заголовка объекта. */
	private static final int SIZE = 16;

	public static final NbtDouble ZERO = new NbtDouble(0.0);

	public static final NbtType<NbtDouble> TYPE = new NbtType.OfFixedSize<>() {
		@Override
		public NbtDouble read(DataInput input, NbtSizeTracker tracker) throws IOException {
			return NbtDouble.of(readDouble(input, tracker));
		}

		@Override
		public NbtScanner.Result doAccept(DataInput input, NbtScanner visitor, NbtSizeTracker tracker)
		throws IOException {
			return visitor.visitDouble(readDouble(input, tracker));
		}

		private static double readDouble(DataInput input, NbtSizeTracker tracker) throws IOException {
			tracker.add(SIZE);
			return input.readDouble();
		}

		@Override
		public int getSizeInBytes() {
			return 8;
		}

		@Override
		public String getCrashReportName() {
			return "DOUBLE";
		}

		@Override
		public String getCommandFeedbackName() {
			return "TAG_Double";
		}
	};

	@Deprecated(forRemoval = true)
	public NbtDouble(double value) {
		this.value = value;
	}

	/**
	 * Возвращает {@link #ZERO} для нулевого значения, иначе создаёт новый объект.
	 *
	 * @param value значение double
	 * @return {@link NbtDouble} для заданного значения
	 */
	public static NbtDouble of(double value) {
		return value == 0.0 ? ZERO : new NbtDouble(value);
	}

	@Override
	public void write(DataOutput output) throws IOException {
		output.writeDouble(value);
	}

	@Override
	public int getSizeInBytes() {
		return SIZE;
	}

	@Override
	public byte getType() {
		return DOUBLE_TYPE;
	}

	@Override
	public NbtType<NbtDouble> getNbtType() {
		return TYPE;
	}

	@Override
	public NbtDouble copy() {
		return this;
	}

	@Override
	public void accept(NbtElementVisitor visitor) {
		visitor.visitDouble(this);
	}

	/**
	 * Конвертирует double в long через {@link Math#floor} для корректной обработки
	 * отрицательных значений (в отличие от простого приведения типа).
	 */
	@Override
	public long longValue() {
		return (long) Math.floor(value);
	}

	/**
	 * Возвращает целую часть double через {@link MathHelper#floor} для корректной обработки
	 * отрицательных значений.
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
		return (float) value;
	}

	@Override
	public Number numberValue() {
		return value;
	}

	@Override
	public NbtScanner.Result doAccept(NbtScanner visitor) {
		return visitor.visitDouble(value);
	}

	@Override
	public String toString() {
		StringNbtWriter writer = new StringNbtWriter();
		writer.visitDouble(this);
		return writer.getString();
	}
}
