package net.minecraft.nbt;

import net.minecraft.nbt.scanner.NbtScanner;
import net.minecraft.nbt.visitor.NbtElementVisitor;
import net.minecraft.nbt.visitor.StringNbtWriter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * NBT-элемент, хранящий значение типа {@code byte}.
 *
 * <p>Все 256 возможных значений кэшируются в {@link Cache}, поэтому фабричный метод
 * {@link #of(byte)} никогда не создаёт новых объектов. Конструктор record помечен
 * {@link Deprecated} — используйте {@link #of(byte)} или {@link #of(boolean)}.</p>
 */
public record NbtByte(byte value) implements AbstractNbtNumber {

	/** Размер тега в байтах: 1 байт данных + 8 байт заголовка объекта. */
	private static final int SIZE = 9;

	public static final NbtType<NbtByte> TYPE = new NbtType.OfFixedSize<>() {
		@Override
		public NbtByte read(DataInput input, NbtSizeTracker tracker) throws IOException {
			return NbtByte.of(readByte(input, tracker));
		}

		@Override
		public NbtScanner.Result doAccept(DataInput input, NbtScanner visitor, NbtSizeTracker tracker)
		throws IOException {
			return visitor.visitByte(readByte(input, tracker));
		}

		private static byte readByte(DataInput input, NbtSizeTracker tracker) throws IOException {
			tracker.add(SIZE);
			return input.readByte();
		}

		@Override
		public int getSizeInBytes() {
			return 1;
		}

		@Override
		public String getCrashReportName() {
			return "BYTE";
		}

		@Override
		public String getCommandFeedbackName() {
			return "TAG_Byte";
		}
	};

	public static final NbtByte ZERO = of((byte) 0);
	public static final NbtByte ONE = of((byte) 1);

	@Deprecated(forRemoval = true)
	public NbtByte(byte value) {
		this.value = value;
	}

	/**
	 * Возвращает кэшированный экземпляр для заданного байтового значения.
	 *
	 * @param value байтовое значение
	 * @return кэшированный {@link NbtByte}
	 */
	public static NbtByte of(byte value) {
		return Cache.VALUES[128 + value];
	}

	/**
	 * Конвертирует булево значение в {@link NbtByte}: {@code true} → {@link #ONE}, {@code false} → {@link #ZERO}.
	 *
	 * @param value булево значение
	 * @return {@link #ONE} или {@link #ZERO}
	 */
	public static NbtByte of(boolean value) {
		return value ? ONE : ZERO;
	}

	@Override
	public void write(DataOutput output) throws IOException {
		output.writeByte(value);
	}

	@Override
	public int getSizeInBytes() {
		return SIZE;
	}

	@Override
	public byte getType() {
		return BYTE_TYPE;
	}

	@Override
	public NbtType<NbtByte> getNbtType() {
		return TYPE;
	}

	@Override
	public NbtByte copy() {
		return this;
	}

	@Override
	public void accept(NbtElementVisitor visitor) {
		visitor.visitByte(this);
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
		return value;
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
		return visitor.visitByte(value);
	}

	@Override
	public String toString() {
		StringNbtWriter writer = new StringNbtWriter();
		writer.visitByte(this);
		return writer.getString();
	}

	/**
	 * Кэш всех 256 возможных байтовых значений.
	 * Индекс {@code i} соответствует значению {@code i - 128}.
	 */
	static class Cache {

		private static final int CACHE_SIZE = 256;
		private static final int OFFSET = 128;

		static final NbtByte[] VALUES = new NbtByte[CACHE_SIZE];

		private Cache() {
		}

		static {
			for (int index = 0; index < VALUES.length; index++) {
				VALUES[index] = new NbtByte((byte) (index - OFFSET));
			}
		}
	}
}
