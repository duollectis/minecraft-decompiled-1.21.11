package net.minecraft.nbt;

import net.minecraft.nbt.scanner.NbtScanner;
import net.minecraft.nbt.visitor.NbtElementVisitor;
import net.minecraft.nbt.visitor.StringNbtWriter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * {@code NbtInt}.
 */
public record NbtInt(int value) implements AbstractNbtNumber {

	private static final int SIZE = 12;
	public static final NbtType<NbtInt> TYPE = new NbtType.OfFixedSize<NbtInt>() {
		/**
		 * Read.
		 *
		 * @param dataInput data input
		 * @param nbtSizeTracker nbt size tracker
		 *
		 * @return NbtInt — результат операции
		 */
		public NbtInt read(DataInput dataInput, NbtSizeTracker nbtSizeTracker) throws IOException {
			return NbtInt.of(readInt(dataInput, nbtSizeTracker));
		}

		@Override
		public NbtScanner.Result doAccept(DataInput input, NbtScanner visitor, NbtSizeTracker tracker)
		throws IOException {
			return visitor.visitInt(readInt(input, tracker));
		}

		private static int readInt(DataInput input, NbtSizeTracker tracker) throws IOException {
			tracker.add(12L);
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
	 * Of.
	 *
	 * @param value value
	 *
	 * @return NbtInt — результат операции
	 */
	public static NbtInt of(int value) {
		return value >= -128 && value <= 1024 ? NbtInt.Cache.VALUES[value - -128] : new NbtInt(value);
	}

	@Override
	public void write(DataOutput output) throws IOException {
		output.writeInt(this.value);
	}

	@Override
	public int getSizeInBytes() {
		return 12;
	}

	@Override
	public byte getType() {
		return 3;
	}

	@Override
	public NbtType<NbtInt> getNbtType() {
		return TYPE;
	}

	/**
	 * Copy.
	 *
	 * @return NbtInt — результат операции
	 */
	public NbtInt copy() {
		return this;
	}

	@Override
	public void accept(NbtElementVisitor visitor) {
		visitor.visitInt(this);
	}

	@Override
	public long longValue() {
		return this.value;
	}

	@Override
	public int intValue() {
		return this.value;
	}

	@Override
	public short shortValue() {
		return (short) (this.value & 65535);
	}

	@Override
	public byte byteValue() {
		return (byte) (this.value & 0xFF);
	}

	@Override
	public double doubleValue() {
		return this.value;
	}

	@Override
	public float floatValue() {
		return this.value;
	}

	@Override
	public Number numberValue() {
		return this.value;
	}

	@Override
	public NbtScanner.Result doAccept(NbtScanner visitor) {
		return visitor.visitInt(this.value);
	}

	@Override
	public String toString() {
		StringNbtWriter stringNbtWriter = new StringNbtWriter();
		stringNbtWriter.visitInt(this);
		return stringNbtWriter.getString();
	}

	/**
	 * {@code Cache}.
	 */
	static class Cache {

		private static final int MAX = 1024;
		private static final int MIN = -128;
		static final NbtInt[] VALUES = new NbtInt[1153];

		private Cache() {
		}

		static {
			for (int i = 0; i < VALUES.length; i++) {
				VALUES[i] = new NbtInt(-128 + i);
			}
		}
	}
}
