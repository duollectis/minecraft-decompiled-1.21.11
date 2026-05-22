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
 * NBT-тег, хранящий гетерогенный список элементов ({@code TAG_List}).
 * <p>
 * В отличие от {@link NbtByteArray}/{@link NbtIntArray}/{@link NbtLongArray},
 * этот список может содержать элементы любого типа, включая вложенные {@link NbtCompound}.
 * При сериализации все элементы приводятся к единому типу: если типы расходятся,
 * каждый элемент оборачивается в {@link NbtCompound} с ключом {@code ""}.
 */
public final class NbtList extends AbstractList<NbtElement> implements AbstractNbtList {

	private static final String HOMOGENIZED_ENTRY_KEY = "";
	private static final int SIZE = 36;
	private static final int BYTES_PER_ENTRY_HEADER = 4;

	public static final NbtType<NbtList> TYPE = new NbtType.OfVariableSize<NbtList>() {
		@Override
		public NbtList read(DataInput input, NbtSizeTracker tracker) throws IOException {
			tracker.pushStack();

			try {
				return readList(input, tracker);
			}
			finally {
				tracker.popStack();
			}
		}

		private static NbtList readList(DataInput input, NbtSizeTracker tracker) throws IOException {
			tracker.add(SIZE);
			byte typeId = input.readByte();
			int length = readListLength(input);

			if (typeId == END_TYPE && length > 0) {
				throw new InvalidNbtException("Missing type on ListTag");
			}

			tracker.add(BYTES_PER_ENTRY_HEADER, length);
			NbtType<?> nbtType = NbtTypes.byId(typeId);
			NbtList list = new NbtList(new ArrayList<>(length));

			for (int index = 0; index < length; index++) {
				list.unwrapAndAdd(nbtType.read(input, tracker));
			}

			return list;
		}

		@Override
		public NbtScanner.Result doAccept(DataInput input, NbtScanner visitor, NbtSizeTracker tracker)
		throws IOException {
			tracker.pushStack();

			try {
				return scanList(input, visitor, tracker);
			}
			finally {
				tracker.popStack();
			}
		}

		/**
		 * Сканирует список без полной десериализации — позволяет выборочно читать элементы.
		 * Использует метки {@link NbtScanner.Result} для управления обходом:
		 * HALT прерывает весь обход, BREAK пропускает оставшиеся элементы.
		 */
		private static NbtScanner.Result scanList(
			DataInput input,
			NbtScanner visitor,
			NbtSizeTracker tracker
		) throws IOException {
			tracker.add(SIZE);
			NbtType<?> nbtType = NbtTypes.byId(input.readByte());
			int length = readListLength(input);

			switch (visitor.visitListMeta(nbtType, length)) {
				case HALT:
					return NbtScanner.Result.HALT;
				case BREAK:
					nbtType.skip(input, length, tracker);
					return visitor.endNested();
				default:
					break;
			}

			tracker.add(BYTES_PER_ENTRY_HEADER, length);
			int index = 0;

			while (index < length) {
				switch (visitor.startListItem(nbtType, index)) {
					case HALT:
						return NbtScanner.Result.HALT;
					case BREAK:
						nbtType.skip(input, tracker);
						int remaining = length - 1 - index;
						if (remaining > 0) {
							nbtType.skip(input, remaining, tracker);
						}
						return visitor.endNested();
					case SKIP:
						nbtType.skip(input, tracker);
						index++;
						continue;
					default:
						break;
				}

				switch (nbtType.doAccept(input, visitor, tracker)) {
					case HALT:
						return NbtScanner.Result.HALT;
					case BREAK:
						int remaining = length - 1 - index;
						if (remaining > 0) {
							nbtType.skip(input, remaining, tracker);
						}
						return visitor.endNested();
					default:
						break;
				}

				index++;
			}

			return visitor.endNested();
		}

		private static int readListLength(DataInput input) throws IOException {
			int length = input.readInt();
			if (length < 0) {
				throw new InvalidNbtException("ListTag length cannot be negative: " + length);
			}
			return length;
		}

		@Override
		public void skip(DataInput input, NbtSizeTracker tracker) throws IOException {
			tracker.pushStack();

			try {
				NbtType<?> nbtType = NbtTypes.byId(input.readByte());
				int length = input.readInt();
				nbtType.skip(input, length, tracker);
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

	/**
	 * Разворачивает обёртку-компаунд, созданную при гомогенизации списка.
	 * Если {@link NbtCompound} содержит ровно один элемент с ключом {@code ""},
	 * возвращает этот элемент напрямую.
	 */
	private static NbtElement unwrap(NbtCompound nbt) {
		if (nbt.getSize() == 1) {
			NbtElement element = nbt.get(HOMOGENIZED_ENTRY_KEY);
			if (element != null) {
				return element;
			}
		}

		return nbt;
	}

	private static boolean isConvertedEntry(NbtCompound nbt) {
		return nbt.getSize() == 1 && nbt.contains(HOMOGENIZED_ENTRY_KEY);
	}

	/**
	 * Оборачивает элемент в {@link NbtCompound} с ключом {@code ""}, если список
	 * хранит тип {@code TAG_Compound} (id=10) и элемент не является уже обёрнутым.
	 * Это необходимо для гомогенизации гетерогенных списков при сериализации.
	 */
	private static NbtElement wrapIfNeeded(byte type, NbtElement element) {
		if (type != COMPOUND_TYPE) {
			return element;
		}

		return element instanceof NbtCompound compound && !isConvertedEntry(compound)
			? compound
			: convertToCompound(element);
	}

	private static NbtCompound convertToCompound(NbtElement nbt) {
		return new NbtCompound(Map.of(HOMOGENIZED_ENTRY_KEY, nbt));
	}

	@Override
	public void write(DataOutput output) throws IOException {
		byte typeId = getValueType();
		output.writeByte(typeId);
		output.writeInt(value.size());

		for (NbtElement element : value) {
			wrapIfNeeded(typeId, element).write(output);
		}
	}

	/**
	 * Определяет общий тип всех элементов списка для сериализации.
	 * Если все элементы одного типа — возвращает этот тип.
	 * Если типы расходятся — возвращает {@code TAG_Compound} (id=10) как универсальный контейнер.
	 */
	@VisibleForTesting
	byte getValueType() {
		byte detectedType = END_TYPE;

		for (NbtElement element : value) {
			byte elementType = element.getType();
			if (detectedType == END_TYPE) {
				detectedType = elementType;
			}
			else if (detectedType != elementType) {
				return COMPOUND_TYPE;
			}
		}

		return detectedType;
	}

	/**
	 * Добавляет элемент в список, предварительно разворачивая обёртку-компаунд
	 * если элемент является конвертированной записью (содержит только ключ {@code ""}).
	 *
	 * @param nbt элемент для добавления
	 */
	public void unwrapAndAdd(NbtElement nbt) {
		if (nbt instanceof NbtCompound compound) {
			add(unwrap(compound));
		}
		else {
			add(nbt);
		}
	}

	@Override
	public int getSizeInBytes() {
		int total = SIZE;
		total += BYTES_PER_ENTRY_HEADER * value.size();

		for (NbtElement element : value) {
			total += element.getSizeInBytes();
		}

		return total;
	}

	@Override
	public byte getType() {
		return LIST_TYPE;
	}

	@Override
	public NbtType<NbtList> getNbtType() {
		return TYPE;
	}

	@Override
	public String toString() {
		StringNbtWriter writer = new StringNbtWriter();
		writer.visitList(this);
		return writer.getString();
	}

	@Override
	public NbtElement get(int index) {
		return value.get(index);
	}

	@Override
	public NbtElement remove(int index) {
		return value.remove(index);
	}

	@Override
	public boolean isEmpty() {
		return value.isEmpty();
	}

	public Optional<NbtCompound> getCompound(int index) {
		return getNullable(index) instanceof NbtCompound compound
			? Optional.of(compound)
			: Optional.empty();
	}

	public NbtCompound getCompoundOrEmpty(int index) {
		return getCompound(index).orElseGet(NbtCompound::new);
	}

	public Optional<NbtList> getList(int index) {
		return getNullable(index) instanceof NbtList list
			? Optional.of(list)
			: Optional.empty();
	}

	public NbtList getListOrEmpty(int index) {
		return getList(index).orElseGet(NbtList::new);
	}

	public Optional<Short> getShort(int index) {
		return getOptional(index).flatMap(NbtElement::asShort);
	}

	public short getShort(int index, short fallback) {
		return getNullable(index) instanceof AbstractNbtNumber number
			? number.shortValue()
			: fallback;
	}

	public Optional<Integer> getInt(int index) {
		return getOptional(index).flatMap(NbtElement::asInt);
	}

	public int getInt(int index, int fallback) {
		return getNullable(index) instanceof AbstractNbtNumber number
			? number.intValue()
			: fallback;
	}

	public Optional<int[]> getIntArray(int index) {
		return getNullable(index) instanceof NbtIntArray array
			? Optional.of(array.getIntArray())
			: Optional.empty();
	}

	public Optional<long[]> getLongArray(int index) {
		return getNullable(index) instanceof NbtLongArray array
			? Optional.of(array.getLongArray())
			: Optional.empty();
	}

	public Optional<Double> getDouble(int index) {
		return getOptional(index).flatMap(NbtElement::asDouble);
	}

	public double getDouble(int index, double fallback) {
		return getNullable(index) instanceof AbstractNbtNumber number
			? number.doubleValue()
			: fallback;
	}

	public Optional<Float> getFloat(int index) {
		return getOptional(index).flatMap(NbtElement::asFloat);
	}

	public float getFloat(int index, float fallback) {
		return getNullable(index) instanceof AbstractNbtNumber number
			? number.floatValue()
			: fallback;
	}

	public Optional<String> getString(int index) {
		return getOptional(index).flatMap(NbtElement::asString);
	}

	public String getString(int index, String fallback) {
		return getNullable(index) instanceof NbtString(String str) ? str : fallback;
	}

	private @Nullable NbtElement getNullable(int index) {
		return index >= 0 && index < value.size() ? value.get(index) : null;
	}

	private Optional<NbtElement> getOptional(int index) {
		return Optional.ofNullable(getNullable(index));
	}

	@Override
	public int size() {
		return value.size();
	}

	@Override
	public NbtElement set(int index, NbtElement element) {
		return value.set(index, element);
	}

	@Override
	public void add(int index, NbtElement element) {
		value.add(index, element);
	}

	@Override
	public boolean setElement(int index, NbtElement element) {
		value.set(index, element);
		return true;
	}

	@Override
	public boolean addElement(int index, NbtElement element) {
		value.add(index, element);
		return true;
	}

	/**
	 * Создаёт глубокую копию списка, рекурсивно копируя каждый элемент.
	 *
	 * @return новый {@link NbtList} с независимыми копиями всех элементов
	 */
	public NbtList copy() {
		List<NbtElement> copy = new ArrayList<>(value.size());

		for (NbtElement element : value) {
			copy.add(element.copy());
		}

		return new NbtList(copy);
	}

	@Override
	public Optional<NbtList> asNbtList() {
		return Optional.of(this);
	}

	@Override
	public boolean equals(Object other) {
		return this == other
			? true
			: other instanceof NbtList list && Objects.equals(value, list.value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public Stream<NbtElement> stream() {
		return super.stream();
	}

	/**
	 * Возвращает поток только тех элементов, которые являются {@link NbtCompound}.
	 * Остальные элементы молча пропускаются.
	 *
	 * @return поток {@link NbtCompound}-элементов
	 */
	public Stream<NbtCompound> streamCompounds() {
		return stream().mapMulti((nbt, callback) -> {
			if (nbt instanceof NbtCompound compound) {
				callback.accept(compound);
			}
		});
	}

	@Override
	public void accept(NbtElementVisitor visitor) {
		visitor.visitList(this);
	}

	@Override
	public void clear() {
		value.clear();
	}

	@Override
	public NbtScanner.Result doAccept(NbtScanner visitor) {
		byte typeId = getValueType();

		switch (visitor.visitListMeta(NbtTypes.byId(typeId), value.size())) {
			case HALT:
				return NbtScanner.Result.HALT;
			case BREAK:
				return visitor.endNested();
			default:
				break;
		}

		int index = 0;

		while (index < value.size()) {
			NbtElement element = wrapIfNeeded(typeId, value.get(index));

			switch (visitor.startListItem(element.getNbtType(), index)) {
				case HALT:
					return NbtScanner.Result.HALT;
				case BREAK:
					return visitor.endNested();
				case SKIP:
					index++;
					continue;
				default:
					break;
			}

			switch (element.doAccept(visitor)) {
				case HALT:
					return NbtScanner.Result.HALT;
				case BREAK:
					return visitor.endNested();
				default:
					break;
			}

			index++;
		}

		return visitor.endNested();
	}
}
