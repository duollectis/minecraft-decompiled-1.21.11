package net.minecraft.nbt;

import net.minecraft.nbt.scanner.NbtScanner;
import net.minecraft.nbt.visitor.NbtElementVisitor;
import net.minecraft.nbt.visitor.StringNbtWriter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * NBT-элемент, хранящий значение типа {@code int}.
 *
 * <p>Значения в диапазоне [{@link Cache#MIN}, {@link Cache#MAX}] кэшируются.
 * Используйте фабричный метод {@link #of(int)} вместо конструктора record.</p>
 */
public record NbtInt(int value) implements AbstractNbtNumber {

	/** Размер тега в байтах: 4 байта данных + 8 байт заголовка объекта. */
	private static final int SIZE = 12;

	public static final NbtType<NbtInt> TYPE = new NbtType.OfFixedSize<>() {
		@Override
		public NbtInt read(DataInput input, NbtSizeTracker tracker) throws IOException {
			return NbtInt.of(readInt(input, tracker));
		}

		@Override
		public NbtScanner.Result doAccept(DataInput input, NbtScanner visitor, NbtSizeTracker tracker)
		throws IOException {
			return visitor.visitInt(readInt(input, tracker));
		}

		private static int readInt(DataInput input, NbtSizeTracker tracker) throws IOException {
			tracker.add(SIZE);
			return input.readInt();
		}

		@Override
		public int getSizeInBytes() {
			return 4;
		}

		@Override
		public String getCrashReportName() {
			return "INT";
		}

		@Override
		public String getCommandFeedbackName() {
			return "TAG_Int";
		}
	};

	@Deprecated(forRemoval = true)
	public NbtInt(int value) {
		this.value = value;
	}

	/**
	 * Возвращает кэшированный экземпляр для значений в диапазоне кэша,
	 * иначе создаёт новый объект.
	 *
	 * @param value значение int
	 * @return {@link NbtInt} для заданного значения
	 */
	public static NbtInt of(int value) {
		return value >= Cache.MIN && value <= Cache.MAX
				? Cache.VALUES[value - Cache.MIN]
				: new NbtInt(value);
	}

	@Override
	public void write(DataOutput output) throws IOException {
		output.writeInt(value);
	}

	@Override
	public int getSizeInBytes() {
		return SIZE;
	}

	@Override
	public byte getType() {
		return INT_TYPE;
	}

	@Override
	public NbtType<NbtInt> getNbtType() {
		return TYPE;
	}

	@Override
	public NbtInt copy() {
		return this;
	}

	@Override
	public void accept(NbtElementVisitor visitor) {
		visitor.visitInt(this);
	}

	@Override
	public long longValue() {
		return value;
	}

	@Override
	public int intValue() {
		return value;
	}

	@Override
	public short shortValue() {
		return (short) (value & 0xFFFF);
	}

	@Override
	public byte byteValue() {
		return (byte) (value & 0xFF);
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
		return visitor.visitInt(value);
	}

	@Override
	public String toString() {
		StringNbtWriter writer = new StringNbtWriter();
		writer.visitInt(this);
		return writer.getString();
	}

	/** Кэш часто используемых значений int. */
	static class Cache {

		static final int MIN = -128;
		static final int MAX = 1024;
		static final NbtInt[] VALUES = new NbtInt[MAX - MIN + 1];

		private Cache() {
		}

		static {
			for (int index = 0; index < VALUES.length; index++) {
				VALUES[index] = new NbtInt(MIN + index);
			}
		}
	}
}
