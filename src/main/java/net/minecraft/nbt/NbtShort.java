package net.minecraft.nbt;

import net.minecraft.nbt.scanner.NbtScanner;
import net.minecraft.nbt.visitor.NbtElementVisitor;
import net.minecraft.nbt.visitor.StringNbtWriter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * NBT-элемент, хранящий значение типа {@code short}.
 *
 * <p>Значения в диапазоне [{@link Cache#MIN}, {@link Cache#MAX}] кэшируются.
 * Используйте фабричный метод {@link #of(short)} вместо конструктора record.</p>
 */
public record NbtShort(short value) implements AbstractNbtNumber {

	/** Размер тега в байтах: 2 байта данных + 8 байт заголовка объекта. */
	private static final int SIZE = 10;

	public static final NbtType<NbtShort> TYPE = new NbtType.OfFixedSize<>() {
		@Override
		public NbtShort read(DataInput input, NbtSizeTracker tracker) throws IOException {
			return NbtShort.of(readShort(input, tracker));
		}

		@Override
		public NbtScanner.Result doAccept(DataInput input, NbtScanner visitor, NbtSizeTracker tracker)
		throws IOException {
			return visitor.visitShort(readShort(input, tracker));
		}

		private static short readShort(DataInput input, NbtSizeTracker tracker) throws IOException {
			tracker.add(SIZE);
			return input.readShort();
		}

		@Override
		public int getSizeInBytes() {
			return 2;
		}

		@Override
		public String getCrashReportName() {
			return "SHORT";
		}

		@Override
		public String getCommandFeedbackName() {
			return "TAG_Short";
		}
	};

	@Deprecated(forRemoval = true)
	public NbtShort(short value) {
		this.value = value;
	}

	/**
	 * Возвращает кэшированный экземпляр для значений в диапазоне кэша,
	 * иначе создаёт новый объект.
	 *
	 * @param value значение short
	 * @return {@link NbtShort} для заданного значения
	 */
	public static NbtShort of(short value) {
		return value >= Cache.MIN && value <= Cache.MAX
				? Cache.VALUES[value - Cache.MIN]
				: new NbtShort(value);
	}

	@Override
	public void write(DataOutput output) throws IOException {
		output.writeShort(value);
	}

	@Override
	public int getSizeInBytes() {
		return SIZE;
	}

	@Override
	public byte getType() {
		return SHORT_TYPE;
	}

	@Override
	public NbtType<NbtShort> getNbtType() {
		return TYPE;
	}

	@Override
	public NbtShort copy() {
		return this;
	}

	@Override
	public void accept(NbtElementVisitor visitor) {
		visitor.visitShort(this);
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
		return value;
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
		return visitor.visitShort(value);
	}

	@Override
	public String toString() {
		StringNbtWriter writer = new StringNbtWriter();
		writer.visitShort(this);
		return writer.getString();
	}

	/** Кэш часто используемых значений short. */
	static class Cache {

		static final int MIN = -128;
		static final int MAX = 1024;
		static final NbtShort[] VALUES = new NbtShort[MAX - MIN + 1];

		private Cache() {
		}

		static {
			for (int index = 0; index < VALUES.length; index++) {
				VALUES[index] = new NbtShort((short) (MIN + index));
			}
		}
	}
}
