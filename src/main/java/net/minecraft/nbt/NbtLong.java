package net.minecraft.nbt;

import net.minecraft.nbt.scanner.NbtScanner;
import net.minecraft.nbt.visitor.NbtElementVisitor;
import net.minecraft.nbt.visitor.StringNbtWriter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * NBT-элемент, хранящий значение типа {@code long}.
 *
 * <p>Значения в диапазоне [{@link Cache#MIN}, {@link Cache#MAX}] кэшируются.
 * Используйте фабричный метод {@link #of(long)} вместо конструктора record.</p>
 */
public record NbtLong(long value) implements AbstractNbtNumber {

	/** Размер тега в байтах: 8 байт данных + 8 байт заголовка объекта. */
	private static final int SIZE = 16;

	public static final NbtType<NbtLong> TYPE = new NbtType.OfFixedSize<>() {
		@Override
		public NbtLong read(DataInput input, NbtSizeTracker tracker) throws IOException {
			return NbtLong.of(readLong(input, tracker));
		}

		@Override
		public NbtScanner.Result doAccept(DataInput input, NbtScanner visitor, NbtSizeTracker tracker)
		throws IOException {
			return visitor.visitLong(readLong(input, tracker));
		}

		private static long readLong(DataInput input, NbtSizeTracker tracker) throws IOException {
			tracker.add(SIZE);
			return input.readLong();
		}

		@Override
		public int getSizeInBytes() {
			return 8;
		}

		@Override
		public String getCrashReportName() {
			return "LONG";
		}

		@Override
		public String getCommandFeedbackName() {
			return "TAG_Long";
		}
	};

	@Deprecated(forRemoval = true)
	public NbtLong(long value) {
		this.value = value;
	}

	/**
	 * Возвращает кэшированный экземпляр для значений в диапазоне кэша,
	 * иначе создаёт новый объект.
	 *
	 * @param value значение long
	 * @return {@link NbtLong} для заданного значения
	 */
	public static NbtLong of(long value) {
		return value >= Cache.MIN && value <= Cache.MAX
				? Cache.VALUES[(int) value - Cache.MIN]
				: new NbtLong(value);
	}

	@Override
	public void write(DataOutput output) throws IOException {
		output.writeLong(value);
	}

	@Override
	public int getSizeInBytes() {
		return SIZE;
	}

	@Override
	public byte getType() {
		return LONG_TYPE;
	}

	@Override
	public NbtType<NbtLong> getNbtType() {
		return TYPE;
	}

	@Override
	public NbtLong copy() {
		return this;
	}

	@Override
	public void accept(NbtElementVisitor visitor) {
		visitor.visitLong(this);
	}

	@Override
	public long longValue() {
		return value;
	}

	/**
	 * Усекает long до int, сохраняя все 32 младших бита (без знакового расширения).
	 * Маска {@code -1L} (все биты единицы) гарантирует корректное усечение.
	 */
	@Override
	public int intValue() {
		return (int) (value & -1L);
	}

	@Override
	public short shortValue() {
		return (short) (value & 0xFFFFL);
	}

	@Override
	public byte byteValue() {
		return (byte) (value & 0xFFL);
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
		return visitor.visitLong(value);
	}

	@Override
	public String toString() {
		StringNbtWriter writer = new StringNbtWriter();
		writer.visitLong(this);
		return writer.getString();
	}

	/** Кэш часто используемых значений long. */
	static class Cache {

		static final int MIN = -128;
		static final int MAX = 1024;
		static final NbtLong[] VALUES = new NbtLong[MAX - MIN + 1];

		private Cache() {
		}

		static {
			for (int index = 0; index < VALUES.length; index++) {
				VALUES[index] = new NbtLong(MIN + index);
			}
		}
	}
}
