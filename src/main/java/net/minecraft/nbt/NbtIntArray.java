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
 * {@code NbtIntArray}.
 */
public final class NbtIntArray implements AbstractNbtList {

	private static final int SIZE = 24;
	public static final NbtType<NbtIntArray> TYPE = new NbtType.OfVariableSize<NbtIntArray>() {
		public NbtIntArray read(DataInput dataInput, NbtSizeTracker nbtSizeTracker) throws IOException {
			return new NbtIntArray(readIntArray(dataInput, nbtSizeTracker));
		}

		@Override
		public NbtScanner.Result doAccept(DataInput input, NbtScanner visitor, NbtSizeTracker tracker)
		throws IOException {
			return visitor.visitIntArray(readIntArray(input, tracker));
		}

		private static int[] readIntArray(DataInput input, NbtSizeTracker tracker) throws IOException {
			tracker.add(24L);
			int i = input.readInt();
			tracker.add(4L, i);
			int[] is = new int[i];

			for (int j = 0; j < i; j++) {
				is[j] = input.readInt();
			}

			return is;
		}

		@Override
		public void skip(DataInput input, NbtSizeTracker tracker) throws IOException {
			input.skipBytes(input.readInt() * 4);
		}

		@Override
		public String getCrashReportName() {
			return "INT[]";
		}

		@Override
		public String getCommandFeedbackName() {
			return "TAG_Int_Array";
		}
	};
	private int[] value;

	public NbtIntArray(int[] value) {
		this.value = value;
	}

	@Override
	public void write(DataOutput output) throws IOException {
		output.writeInt(this.value.length);

		for (int i : this.value) {
			output.writeInt(i);
		}
	}

	@Override
	public int getSizeInBytes() {
		return 24 + 4 * this.value.length;
	}

	@Override
	public byte getType() {
		return 11;
	}

	@Override
	public NbtType<NbtIntArray> getNbtType() {
		return TYPE;
	}

	@Override
	public String toString() {
		StringNbtWriter stringNbtWriter = new StringNbtWriter();
		stringNbtWriter.visitIntArray(this);
		return stringNbtWriter.getString();
	}

	public NbtIntArray copy() {
		int[] is = new int[this.value.length];
		System.arraycopy(this.value, 0, is, 0, this.value.length);
		return new NbtIntArray(is);
	}

	@Override
	public boolean equals(Object o) {
		return this == o ? true : o instanceof NbtIntArray && Arrays.equals(this.value, ((NbtIntArray) o).value);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(this.value);
	}

	public int[] getIntArray() {
		return this.value;
	}

	@Override
	public void accept(NbtElementVisitor visitor) {
		visitor.visitIntArray(this);
	}

	@Override
	public int size() {
		return this.value.length;
	}

	public NbtInt get(int i) {
		return NbtInt.of(this.value[i]);
	}

	@Override
	public boolean setElement(int index, NbtElement element) {
		if (element instanceof AbstractNbtNumber abstractNbtNumber) {
			this.value[index] = abstractNbtNumber.intValue();
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public boolean addElement(int index, NbtElement element) {
		if (element instanceof AbstractNbtNumber abstractNbtNumber) {
			this.value = ArrayUtils.add(this.value, index, abstractNbtNumber.intValue());
			return true;
		}
		else {
			return false;
		}
	}

	public NbtInt remove(int i) {
		int j = this.value[i];
		this.value = ArrayUtils.remove(this.value, i);
		return NbtInt.of(j);
	}

	@Override
	public void clear() {
		this.value = new int[0];
	}

	@Override
	public Optional<int[]> asIntArray() {
		return Optional.of(this.value);
	}

	@Override
	public NbtScanner.Result doAccept(NbtScanner visitor) {
		return visitor.visitIntArray(this.value);
	}
}
