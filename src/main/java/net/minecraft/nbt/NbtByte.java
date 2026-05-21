package net.minecraft.nbt;

import net.minecraft.nbt.scanner.NbtScanner;
import net.minecraft.nbt.visitor.NbtElementVisitor;
import net.minecraft.nbt.visitor.StringNbtWriter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * {@code NbtByte}.
 */
public record NbtByte(byte value) implements AbstractNbtNumber {

	private static final int SIZE = 9;
	public static final NbtType<NbtByte> TYPE = new NbtType.OfFixedSize<NbtByte>() {
		/**
		 * Read.
		 *
		 * @param dataInput data input
		 * @param nbtSizeTracker nbt size tracker
		 *
		 * @return NbtByte — результат операции
		 */
		public NbtByte read(DataInput dataInput, NbtSizeTracker nbtSizeTracker) throws IOException {
			return NbtByte.of(readByte(dataInput, nbtSizeTracker));
		}

		@Override
		public NbtScanner.Result doAccept(DataInput input, NbtScanner visitor, NbtSizeTracker tracker)
		throws IOException {
			return visitor.visitByte(readByte(input, tracker));
		}

		private static byte readByte(DataInput input, NbtSizeTracker tracker) throws IOException {
			tracker.add(9L);
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
	 * Of.
	 *
	 * @param value value
	 *
	 * @return NbtByte — результат операции
	 */
	public static NbtByte of(byte value) {
		return NbtByte.Cache.VALUES[128 + value];
	}

	/**
	 * Of.
	 *
	 * @param value value
	 *
	 * @return NbtByte — результат операции
	 */
	public static NbtByte of(boolean value) {
		return value ? ONE : ZERO;
	}

	@Override
	public void write(DataOutput output) throws IOException {
		output.writeByte(this.value);
	}

	@Override
	public int getSizeInBytes() {
		return 9;
	}

	@Override
	public byte getType() {
		return 1;
	}

	@Override
	public NbtType<NbtByte> getNbtType() {
		return TYPE;
	}

	/**
	 * Copy.
	 *
	 * @return NbtByte — результат операции
	 */
	public NbtByte copy() {
		return this;
	}

	@Override
	public void accept(NbtElementVisitor visitor) {
		visitor.visitByte(this);
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
		return this.value;
	}

	@Override
	public byte byteValue() {
		return this.value;
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
		return visitor.visitByte(this.value);
	}

	@Override
	public String toString() {
		StringNbtWriter stringNbtWriter = new StringNbtWriter();
		stringNbtWriter.visitByte(this);
		return stringNbtWriter.getString();
	}

	/**
	 * {@code Cache}.
	 */
	static class Cache {

		static final NbtByte[] VALUES = new NbtByte[256];

		private Cache() {
		}

		static {
			for (int i = 0; i < VALUES.length; i++) {
				VALUES[i] = new NbtByte((byte) (i - 128));
			}
		}
	}
}
