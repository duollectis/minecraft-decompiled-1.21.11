package net.minecraft.nbt;

import net.minecraft.nbt.scanner.NbtScanner;
import net.minecraft.nbt.visitor.NbtElementVisitor;
import net.minecraft.nbt.visitor.StringNbtWriter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * {@code NbtLong}.
 */
public record NbtLong(long value) implements AbstractNbtNumber {

	private static final int SIZE = 16;
	public static final NbtType<NbtLong> TYPE = new NbtType.OfFixedSize<NbtLong>() {
		/**
		 * Read.
		 *
		 * @param dataInput data input
		 * @param nbtSizeTracker nbt size tracker
		 *
		 * @return NbtLong — результат операции
		 */
		public NbtLong read(DataInput dataInput, NbtSizeTracker nbtSizeTracker) throws IOException {
			return NbtLong.of(readLong(dataInput, nbtSizeTracker));
		}

		@Override
		public NbtScanner.Result doAccept(DataInput input, NbtScanner visitor, NbtSizeTracker tracker)
		throws IOException {
			return visitor.visitLong(readLong(input, tracker));
		}

		private static long readLong(DataInput input, NbtSizeTracker tracker) throws IOException {
			tracker.add(16L);
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
	 * Of.
	 *
	 * @param value value
	 *
	 * @return NbtLong — результат операции
	 */
	public static NbtLong of(long value) {
		return value >= -128L && value <= 1024L ? NbtLong.Cache.VALUES[(int) value - -128] : new NbtLong(value);
	}

	@Override
	public void write(DataOutput output) throws IOException {
		output.writeLong(this.value);
	}

	@Override
	public int getSizeInBytes() {
		return 16;
	}

	@Override
	public byte getType() {
		return 4;
	}

	@Override
	public NbtType<NbtLong> getNbtType() {
		return TYPE;
	}

	/**
	 * Copy.
	 *
	 * @return NbtLong — результат операции
	 */
	public NbtLong copy() {
		return this;
	}

	@Override
	public void accept(NbtElementVisitor visitor) {
		visitor.visitLong(this);
	}

	@Override
	public long longValue() {
		return this.value;
	}

	@Override
	public int intValue() {
		return (int) (this.value & -1L);
	}

	@Override
	public short shortValue() {
		return (short) (this.value & 65535L);
	}

	@Override
	public byte byteValue() {
		return (byte) (this.value & 255L);
	}

	@Override
	public double doubleValue() {
		return this.value;
	}

	@Override
	public float floatValue() {
		return (float) this.value;
	}

	@Override
	public Number numberValue() {
		return this.value;
	}

	@Override
	public NbtScanner.Result doAccept(NbtScanner visitor) {
		return visitor.visitLong(this.value);
	}

	@Override
	public String toString() {
		StringNbtWriter stringNbtWriter = new StringNbtWriter();
		stringNbtWriter.visitLong(this);
		return stringNbtWriter.getString();
	}

	/**
	 * {@code Cache}.
	 */
	static class Cache {

		private static final int MAX = 1024;
		private static final int MIN = -128;
		static final NbtLong[] VALUES = new NbtLong[1153];

		private Cache() {
		}

		static {
			for (int i = 0; i < VALUES.length; i++) {
				VALUES[i] = new NbtLong(-128 + i);
			}
		}
	}
}
