package net.minecraft.nbt;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.nbt.scanner.NbtScanner;
import net.minecraft.nbt.visitor.NbtElementVisitor;
import net.minecraft.nbt.visitor.StringNbtWriter;
import org.jspecify.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

/**
 * {@code NbtList}.
 */
public final class NbtList extends AbstractList<NbtElement> implements AbstractNbtList {

	private static final String HOMOGENIZED_ENTRY_KEY = "";
	private static final int SIZE = 36;
	public static final NbtType<NbtList> TYPE = new NbtType.OfVariableSize<NbtList>() {
		/**
		 * Read.
		 *
		 * @param dataInput data input
		 * @param nbtSizeTracker nbt size tracker
		 *
		 * @return NbtList — результат операции
		 */
		public NbtList read(DataInput dataInput, NbtSizeTracker nbtSizeTracker) throws IOException {
			nbtSizeTracker.pushStack();

			NbtList var3;
			try {
				var3 = readList(dataInput, nbtSizeTracker);
			}
			finally {
				nbtSizeTracker.popStack();
			}

			return var3;
		}

		private static NbtList readList(DataInput input, NbtSizeTracker tracker) throws IOException {
			tracker.add(36L);
			byte b = input.readByte();
			int i = readListLength(input);
			if (b == 0 && i > 0) {
				throw new InvalidNbtException("Missing type on ListTag");
			}
			else {
				tracker.add(4L, i);
				NbtType<?> nbtType = NbtTypes.byId(b);
				NbtList nbtList = new NbtList(new ArrayList<>(i));

				for (int j = 0; j < i; j++) {
					nbtList.unwrapAndAdd(nbtType.read(input, tracker));
				}

				return nbtList;
			}
		}

		@Override
		public NbtScanner.Result doAccept(DataInput input, NbtScanner visitor, NbtSizeTracker tracker)
		throws IOException {
			tracker.pushStack();

			NbtScanner.Result var4;
			try {
				var4 = scanList(input, visitor, tracker);
			}
			finally {
				tracker.popStack();
			}

			return var4;
		}

		private static NbtScanner.Result scanList(DataInput input, NbtScanner visitor, NbtSizeTracker tracker)
		throws IOException {
			tracker.add(36L);
			NbtType<?> nbtType = NbtTypes.byId(input.readByte());
			int i = readListLength(input);
			switch (visitor.visitListMeta(nbtType, i)) {
				case HALT:
					return NbtScanner.Result.HALT;
				case BREAK:
					nbtType.skip(input, i, tracker);
					return visitor.endNested();
				default:
					tracker.add(4L, i);
					int j = 0;

					while (true) {
						label41:
						{
							if (j < i) {
								switch (visitor.startListItem(nbtType, j)) {
									case HALT:
										return NbtScanner.Result.HALT;
									case BREAK:
										nbtType.skip(input, tracker);
										break;
									case SKIP:
										nbtType.skip(input, tracker);
										break label41;
									default:
										switch (nbtType.doAccept(input, visitor, tracker)) {
											case HALT:
												return NbtScanner.Result.HALT;
											case BREAK:
												break;
											default:
												break label41;
										}
								}
							}

							int k = i - 1 - j;
							if (k > 0) {
								nbtType.skip(input, k, tracker);
							}

							return visitor.endNested();
						}

						j++;
					}
			}
		}

		private static int readListLength(DataInput input) throws IOException {
			int i = input.readInt();
			if (i < 0) {
				throw new InvalidNbtException("ListTag length cannot be negative: " + i);
			}
			else {
				return i;
			}
		}

		@Override
		public void skip(DataInput input, NbtSizeTracker tracker) throws IOException {
			tracker.pushStack();

			try {
				NbtType<?> nbtType = NbtTypes.byId(input.readByte());
				int i = input.readInt();
				nbtType.skip(input, i, tracker);
			}
			finally {
				tracker.popStack();
			}
		}

		@Override
		public String getCrashReportName() {
			return "LIST";
		}

		@Override
		public String getCommandFeedbackName() {
			return "TAG_List";
		}
	};
	private final List<NbtElement> value;

	public NbtList() {
		this(new ArrayList<>());
	}

	NbtList(List<NbtElement> value) {
		this.value = value;
	}

	private static NbtElement unwrap(NbtCompound nbt) {
		if (nbt.getSize() == 1) {
			NbtElement nbtElement = nbt.get("");
			if (nbtElement != null) {
				return nbtElement;
			}
		}

		return nbt;
	}

	private static boolean isConvertedEntry(NbtCompound nbt) {
		return nbt.getSize() == 1 && nbt.contains("");
	}

	private static NbtElement wrapIfNeeded(byte type, NbtElement value) {
		if (type != 10) {
			return value;
		}
		else {
			return value instanceof NbtCompound nbtCompound && !isConvertedEntry(nbtCompound) ? nbtCompound
			                                                                                  : convertToCompound(value);
		}
	}

	private static NbtCompound convertToCompound(NbtElement nbt) {
		return new NbtCompound(Map.of("", nbt));
	}

	@Override
	public void write(DataOutput output) throws IOException {
		byte b = this.getValueType();
		output.writeByte(b);
		output.writeInt(this.value.size());

		for (NbtElement nbtElement : this.value) {
			wrapIfNeeded(b, nbtElement).write(output);
		}
	}

	@VisibleForTesting
	byte getValueType() {
		byte b = 0;

		for (NbtElement nbtElement : this.value) {
			byte c = nbtElement.getType();
			if (b == 0) {
				b = c;
			}
			else if (b != c) {
				return 10;
			}
		}

		return b;
	}

	/**
	 * Unwrap and add.
	 *
	 * @param nbt nbt
	 */
	public void unwrapAndAdd(NbtElement nbt) {
		if (nbt instanceof NbtCompound nbtCompound) {
			this.add(unwrap(nbtCompound));
		}
		else {
			this.add(nbt);
		}
	}

	@Override
	public int getSizeInBytes() {
		int i = 36;
		i += 4 * this.value.size();

		for (NbtElement nbtElement : this.value) {
			i += nbtElement.getSizeInBytes();
		}

		return i;
	}

	@Override
	public byte getType() {
		return 9;
	}

	@Override
	public NbtType<NbtList> getNbtType() {
		return TYPE;
	}

	@Override
	public String toString() {
		StringNbtWriter stringNbtWriter = new StringNbtWriter();
		stringNbtWriter.visitList(this);
		return stringNbtWriter.getString();
	}

	@Override
	public NbtElement get(int index) {
		return this.value.get(index);
	}

	@Override
	public NbtElement remove(int i) {
		return this.value.remove(i);
	}

	@Override
	public boolean isEmpty() {
		return this.value.isEmpty();
	}

	public Optional<NbtCompound> getCompound(int index) {
		return this.getNullable(index) instanceof NbtCompound nbtCompound ? Optional.of(nbtCompound) : Optional.empty();
	}

	public NbtCompound getCompoundOrEmpty(int index) {
		return this.getCompound(index).orElseGet(NbtCompound::new);
	}

	public Optional<NbtList> getList(int index) {
		return this.getNullable(index) instanceof NbtList nbtList ? Optional.of(nbtList) : Optional.empty();
	}

	public NbtList getListOrEmpty(int index) {
		return this.getList(index).orElseGet(NbtList::new);
	}

	public Optional<Short> getShort(int index) {
		return this.getOptional(index).flatMap(NbtElement::asShort);
	}

	public short getShort(int index, short fallback) {
		return this.getNullable(index) instanceof AbstractNbtNumber abstractNbtNumber ? abstractNbtNumber.shortValue()
		                                                                              : fallback;
	}

	public Optional<Integer> getInt(int index) {
		return this.getOptional(index).flatMap(NbtElement::asInt);
	}

	public int getInt(int index, int fallback) {
		return this.getNullable(index) instanceof AbstractNbtNumber abstractNbtNumber ? abstractNbtNumber.intValue()
		                                                                              : fallback;
	}

	public Optional<int[]> getIntArray(int index) {
		return this.getNullable(index) instanceof NbtIntArray nbtIntArray ? Optional.of(nbtIntArray.getIntArray())
		                                                                  : Optional.empty();
	}

	public Optional<long[]> getLongArray(int index) {
		return this.getNullable(index) instanceof NbtLongArray nbtLongArray ? Optional.of(nbtLongArray.getLongArray())
		                                                                    : Optional.empty();
	}

	public Optional<Double> getDouble(int index) {
		return this.getOptional(index).flatMap(NbtElement::asDouble);
	}

	public double getDouble(int index, double fallback) {
		return this.getNullable(index) instanceof AbstractNbtNumber abstractNbtNumber ? abstractNbtNumber.doubleValue()
		                                                                              : fallback;
	}

	public Optional<Float> getFloat(int index) {
		return this.getOptional(index).flatMap(NbtElement::asFloat);
	}

	public float getFloat(int index, float fallback) {
		return this.getNullable(index) instanceof AbstractNbtNumber abstractNbtNumber ? abstractNbtNumber.floatValue()
		                                                                              : fallback;
	}

	public Optional<String> getString(int index) {
		return this.getOptional(index).flatMap(NbtElement::asString);
	}

	public String getString(int index, String fallback) {
		return this.getNullable(index) instanceof NbtString(String var8) ? var8 : fallback;
	}

	private @Nullable NbtElement getNullable(int index) {
		return index >= 0 && index < this.value.size() ? this.value.get(index) : null;
	}

	private Optional<NbtElement> getOptional(int index) {
		return Optional.ofNullable(this.getNullable(index));
	}

	@Override
	public int size() {
		return this.value.size();
	}

	/**
	 * Set.
	 *
	 * @param i i
	 * @param nbtElement nbt element
	 *
	 * @return NbtElement — результат операции
	 */
	public NbtElement set(int i, NbtElement nbtElement) {
		return this.value.set(i, nbtElement);
	}

	/**
	 * Add.
	 *
	 * @param i i
	 * @param nbtElement nbt element
	 */
	public void add(int i, NbtElement nbtElement) {
		this.value.add(i, nbtElement);
	}

	@Override
	public boolean setElement(int index, NbtElement element) {
		this.value.set(index, element);
		return true;
	}

	@Override
	public boolean addElement(int index, NbtElement element) {
		this.value.add(index, element);
		return true;
	}

	/**
	 * Copy.
	 *
	 * @return NbtList — результат операции
	 */
	public NbtList copy() {
		List<NbtElement> list = new ArrayList<>(this.value.size());

		for (NbtElement nbtElement : this.value) {
			list.add(nbtElement.copy());
		}

		return new NbtList(list);
	}

	@Override
	public Optional<NbtList> asNbtList() {
		return Optional.of(this);
	}

	@Override
	public boolean equals(Object o) {
		return this == o ? true : o instanceof NbtList && Objects.equals(this.value, ((NbtList) o).value);
	}

	@Override
	public int hashCode() {
		return this.value.hashCode();
	}

	@Override
	public Stream<NbtElement> stream() {
		return super.stream();
	}

	/**
	 * Stream compounds.
	 *
	 * @return Stream — результат операции
	 */
	public Stream<NbtCompound> streamCompounds() {
		return this.stream().mapMulti((nbt, callback) -> {
			if (nbt instanceof NbtCompound nbtCompound) {
				callback.accept(nbtCompound);
			}
		});
	}

	@Override
	public void accept(NbtElementVisitor visitor) {
		visitor.visitList(this);
	}

	@Override
	public void clear() {
		this.value.clear();
	}

	@Override
	public NbtScanner.Result doAccept(NbtScanner visitor) {
		byte b = this.getValueType();
		switch (visitor.visitListMeta(NbtTypes.byId(b), this.value.size())) {
			case HALT:
				return NbtScanner.Result.HALT;
			case BREAK:
				return visitor.endNested();
			default:
				int i = 0;

				while (i < this.value.size()) {
					NbtElement nbtElement = wrapIfNeeded(b, this.value.get(i));
					switch (visitor.startListItem(nbtElement.getNbtType(), i)) {
						case HALT:
							return NbtScanner.Result.HALT;
						case BREAK:
							return visitor.endNested();
						default:
							switch (nbtElement.doAccept(visitor)) {
								case HALT:
									return NbtScanner.Result.HALT;
								case BREAK:
									return visitor.endNested();
							}
						case SKIP:
							i++;
					}
				}

				return visitor.endNested();
		}
	}
}
