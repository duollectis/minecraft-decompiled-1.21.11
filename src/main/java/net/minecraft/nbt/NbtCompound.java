package net.minecraft.nbt;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.*;
import net.minecraft.nbt.scanner.NbtScanner;
import net.minecraft.nbt.visitor.NbtElementVisitor;
import net.minecraft.nbt.visitor.StringNbtWriter;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

/**
 * NBT-тег, хранящий именованный набор дочерних тегов ({@code TAG_Compound}).
 * <p>
 * Является основным контейнером данных в формате NBT — аналог JSON-объекта.
 * Поддерживает типизированный доступ к полям через методы {@code get*(key)} и {@code put*(key, value)},
 * а также кодирование/декодирование через {@link Codec} и {@link MapCodec}.
 */
public final class NbtCompound implements NbtElement {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** Размер строки ключа в байтах: 28 байт заголовка + 2 байта на символ. */
	private static final int KEY_HEADER_BYTES = 28;
	private static final int BYTES_PER_KEY_CHAR = 2;
	/** Накладные расходы на хранение одной записи в HashMap. */
	private static final int ENTRY_OVERHEAD_BYTES = 36;
	private static final int SIZE = 48;

	public static final Codec<NbtCompound> CODEC = Codec.PASSTHROUGH
			.comapFlatMap(
					dynamic -> {
						NbtElement element = (NbtElement) dynamic.convert(NbtOps.INSTANCE).getValue();
						return element instanceof NbtCompound compound
							? DataResult.success(compound == dynamic.getValue() ? compound.copy() : compound)
							: DataResult.error(() -> "Not a compound tag: " + element);
					},
					nbt -> new Dynamic(NbtOps.INSTANCE, nbt.copy())
			);

	public static final NbtType<NbtCompound> TYPE = new NbtType.OfVariableSize<NbtCompound>() {
		@Override
		public NbtCompound read(DataInput input, NbtSizeTracker tracker) throws IOException {
			tracker.pushStack();

			try {
				return readCompound(input, tracker);
			}
			finally {
				tracker.popStack();
			}
		}

		private static NbtCompound readCompound(DataInput input, NbtSizeTracker tracker) throws IOException {
			tracker.add(SIZE);
			Map<String, NbtElement> map = Maps.newHashMap();

			byte typeId;
			while ((typeId = input.readByte()) != END_TYPE) {
				String key = readString(input, tracker);
				NbtElement element = NbtCompound.read(NbtTypes.byId(typeId), key, input, tracker);
				if (map.put(key, element) == null) {
					tracker.add(ENTRY_OVERHEAD_BYTES);
				}
			}

			return new NbtCompound(map);
		}

		@Override
		public NbtScanner.Result doAccept(DataInput input, NbtScanner visitor, NbtSizeTracker tracker)
		throws IOException {
			tracker.pushStack();

			try {
				return scanCompound(input, visitor, tracker);
			}
			finally {
				tracker.popStack();
			}
		}

		/**
		 * Сканирует компаунд без полной десериализации.
		 * Использует метки {@link NbtScanner.NestedResult} для управления обходом полей:
		 * BREAK прерывает обход и пропускает оставшиеся поля, SKIP пропускает текущее поле.
		 */
		private static NbtScanner.Result scanCompound(
			DataInput input,
			NbtScanner visitor,
			NbtSizeTracker tracker
		) throws IOException {
			tracker.add(SIZE);

			byte typeId;
			boolean broken = false;

			while ((typeId = input.readByte()) != END_TYPE) {
				NbtType<?> nbtType = NbtTypes.byId(typeId);

				switch (visitor.visitSubNbtType(nbtType)) {
					case HALT:
						return NbtScanner.Result.HALT;
					case BREAK:
						NbtString.skip(input);
						nbtType.skip(input, tracker);
						broken = true;
						break;
					case SKIP:
						NbtString.skip(input);
						nbtType.skip(input, tracker);
						continue;
					default:
						break;
				}

				if (broken) {
					break;
				}

				String key = readString(input, tracker);

				switch (visitor.startSubNbt(nbtType, key)) {
					case HALT:
						return NbtScanner.Result.HALT;
					case BREAK:
						nbtType.skip(input, tracker);
						broken = true;
						break;
					case SKIP:
						nbtType.skip(input, tracker);
						continue;
					default:
						break;
				}

				if (broken) {
					break;
				}

				tracker.add(ENTRY_OVERHEAD_BYTES);

				switch (nbtType.doAccept(input, visitor, tracker)) {
					case HALT:
						return NbtScanner.Result.HALT;
					case BREAK:
						broken = true;
						break;
					default:
						break;
				}

				if (broken) {
					break;
				}
			}

			if (broken) {
				while ((typeId = input.readByte()) != END_TYPE) {
					NbtString.skip(input);
					NbtTypes.byId(typeId).skip(input, tracker);
				}
			}

			return visitor.endNested();
		}

		private static String readString(DataInput input, NbtSizeTracker tracker) throws IOException {
			String key = input.readUTF();
			tracker.add(KEY_HEADER_BYTES);
			tracker.add(BYTES_PER_KEY_CHAR, key.length());
			return key;
		}

		@Override
		public void skip(DataInput input, NbtSizeTracker tracker) throws IOException {
			tracker.pushStack();

			try {
				byte typeId;
				while ((typeId = input.readByte()) != END_TYPE) {
					NbtString.skip(input);
					NbtTypes.byId(typeId).skip(input, tracker);
				}
			}
			finally {
				tracker.popStack();
			}
		}

		@Override
		public String getCrashReportName() {
			return "COMPOUND";
		}

		@Override
		public String getCommandFeedbackName() {
			return "TAG_Compound";
		}
	};

	private final Map<String, NbtElement> entries;

	NbtCompound(Map<String, NbtElement> entries) {
		this.entries = entries;
	}

	public NbtCompound() {
		this(new HashMap<>());
	}

	@Override
	public void write(DataOutput output) throws IOException {
		for (Entry<String, NbtElement> entry : entries.entrySet()) {
			write(entry.getKey(), entry.getValue(), output);
		}

		output.writeByte(END_TYPE);
	}

	@Override
	public int getSizeInBytes() {
		int total = SIZE;

		for (Entry<String, NbtElement> entry : entries.entrySet()) {
			total += KEY_HEADER_BYTES + BYTES_PER_KEY_CHAR * entry.getKey().length();
			total += ENTRY_OVERHEAD_BYTES;
			total += entry.getValue().getSizeInBytes();
		}

		return total;
	}

	public Set<String> getKeys() {
		return entries.keySet();
	}

	public Set<Entry<String, NbtElement>> entrySet() {
		return entries.entrySet();
	}

	public Collection<NbtElement> values() {
		return entries.values();
	}

	public void forEach(BiConsumer<String, NbtElement> entryConsumer) {
		entries.forEach(entryConsumer);
	}

	@Override
	public byte getType() {
		return COMPOUND_TYPE;
	}

	@Override
	public NbtType<NbtCompound> getNbtType() {
		return TYPE;
	}

	public int getSize() {
		return entries.size();
	}

	public @Nullable NbtElement put(String key, NbtElement element) {
		return entries.put(key, element);
	}

	public void putByte(String key, byte value) {
		entries.put(key, NbtByte.of(value));
	}

	public void putShort(String key, short value) {
		entries.put(key, NbtShort.of(value));
	}

	public void putInt(String key, int value) {
		entries.put(key, NbtInt.of(value));
	}

	public void putLong(String key, long value) {
		entries.put(key, NbtLong.of(value));
	}

	public void putFloat(String key, float value) {
		entries.put(key, NbtFloat.of(value));
	}

	public void putDouble(String key, double value) {
		entries.put(key, NbtDouble.of(value));
	}

	public void putString(String key, String value) {
		entries.put(key, NbtString.of(value));
	}

	public void putByteArray(String key, byte[] value) {
		entries.put(key, new NbtByteArray(value));
	}

	public void putIntArray(String key, int[] value) {
		entries.put(key, new NbtIntArray(value));
	}

	public void putLongArray(String key, long[] value) {
		entries.put(key, new NbtLongArray(value));
	}

	public void putBoolean(String key, boolean value) {
		entries.put(key, NbtByte.of(value));
	}

	public @Nullable NbtElement get(String key) {
		return entries.get(key);
	}

	public boolean contains(String key) {
		return entries.containsKey(key);
	}

	private Optional<NbtElement> getOptional(String key) {
		return Optional.ofNullable(entries.get(key));
	}

	public Optional<Byte> getByte(String key) {
		return getOptional(key).flatMap(NbtElement::asByte);
	}

	public byte getByte(String key, byte fallback) {
		return entries.get(key) instanceof AbstractNbtNumber number ? number.byteValue() : fallback;
	}

	public Optional<Short> getShort(String key) {
		return getOptional(key).flatMap(NbtElement::asShort);
	}

	public short getShort(String key, short fallback) {
		return entries.get(key) instanceof AbstractNbtNumber number ? number.shortValue() : fallback;
	}

	public Optional<Integer> getInt(String key) {
		return getOptional(key).flatMap(NbtElement::asInt);
	}

	public int getInt(String key, int fallback) {
		return entries.get(key) instanceof AbstractNbtNumber number ? number.intValue() : fallback;
	}

	public Optional<Long> getLong(String key) {
		return getOptional(key).flatMap(NbtElement::asLong);
	}

	public long getLong(String key, long fallback) {
		return entries.get(key) instanceof AbstractNbtNumber number ? number.longValue() : fallback;
	}

	public Optional<Float> getFloat(String key) {
		return getOptional(key).flatMap(NbtElement::asFloat);
	}

	public float getFloat(String key, float fallback) {
		return entries.get(key) instanceof AbstractNbtNumber number ? number.floatValue() : fallback;
	}

	public Optional<Double> getDouble(String key) {
		return getOptional(key).flatMap(NbtElement::asDouble);
	}

	public double getDouble(String key, double fallback) {
		return entries.get(key) instanceof AbstractNbtNumber number ? number.doubleValue() : fallback;
	}

	public Optional<String> getString(String key) {
		return getOptional(key).flatMap(NbtElement::asString);
	}

	public String getString(String key, String fallback) {
		return entries.get(key) instanceof NbtString(String str) ? str : fallback;
	}

	public Optional<byte[]> getByteArray(String key) {
		return entries.get(key) instanceof NbtByteArray array
			? Optional.of(array.getByteArray())
			: Optional.empty();
	}

	public Optional<int[]> getIntArray(String key) {
		return entries.get(key) instanceof NbtIntArray array
			? Optional.of(array.getIntArray())
			: Optional.empty();
	}

	public Optional<long[]> getLongArray(String key) {
		return entries.get(key) instanceof NbtLongArray array
			? Optional.of(array.getLongArray())
			: Optional.empty();
	}

	public Optional<NbtCompound> getCompound(String key) {
		return entries.get(key) instanceof NbtCompound compound
			? Optional.of(compound)
			: Optional.empty();
	}

	public NbtCompound getCompoundOrEmpty(String key) {
		return getCompound(key).orElseGet(NbtCompound::new);
	}

	public Optional<NbtList> getList(String key) {
		return entries.get(key) instanceof NbtList list ? Optional.of(list) : Optional.empty();
	}

	public NbtList getListOrEmpty(String key) {
		return getList(key).orElseGet(NbtList::new);
	}

	public Optional<Boolean> getBoolean(String key) {
		return getOptional(key).flatMap(NbtElement::asBoolean);
	}

	public boolean getBoolean(String key, boolean fallback) {
		return getByte(key, NbtByte.of(fallback).value()) != 0;
	}

	public @Nullable NbtElement remove(String key) {
		return entries.remove(key);
	}

	@Override
	public String toString() {
		StringNbtWriter writer = new StringNbtWriter();
		writer.visitCompound(this);
		return writer.getString();
	}

	public boolean isEmpty() {
		return entries.isEmpty();
	}

	protected NbtCompound shallowCopy() {
		return new NbtCompound(new HashMap<>(entries));
	}

	/**
	 * Создаёт глубокую копию компаунда, рекурсивно копируя все дочерние элементы.
	 *
	 * @return новый {@link NbtCompound} с независимыми копиями всех полей
	 */
	public NbtCompound copy() {
		HashMap<String, NbtElement> copy = new HashMap<>();
		entries.forEach((key, value) -> copy.put(key, value.copy()));
		return new NbtCompound(copy);
	}

	@Override
	public Optional<NbtCompound> asCompound() {
		return Optional.of(this);
	}

	@Override
	public boolean equals(Object other) {
		return this == other
			? true
			: other instanceof NbtCompound compound && Objects.equals(entries, compound.entries);
	}

	@Override
	public int hashCode() {
		return entries.hashCode();
	}

	private static void write(String key, NbtElement element, DataOutput output) throws IOException {
		output.writeByte(element.getType());
		if (element.getType() != END_TYPE) {
			output.writeUTF(key);
			element.write(output);
		}
	}

	/**
	 * Читает дочерний элемент из потока, оборачивая {@link IOException} в {@link NbtCrashException}
	 * с подробным отчётом о сбое для диагностики повреждённых данных.
	 */
	static NbtElement read(NbtType<?> reader, String key, DataInput input, NbtSizeTracker tracker) {
		try {
			return reader.read(input, tracker);
		}
		catch (IOException exception) {
			CrashReport crashReport = CrashReport.create(exception, "Loading NBT data");
			CrashReportSection section = crashReport.addElement("NBT Tag");
			section.add("Tag name", key);
			section.add("Tag type", reader.getCrashReportName());
			throw new NbtCrashException(crashReport);
		}
	}

	/**
	 * Рекурсивно копирует поля из {@code source} в этот компаунд.
	 * Если оба компаунда содержат одноимённый вложенный {@link NbtCompound},
	 * выполняется рекурсивное слияние вместо замены.
	 *
	 * @param source источник данных для копирования
	 * @return {@code this} для цепочки вызовов
	 */
	public NbtCompound copyFrom(NbtCompound source) {
		for (Entry<String, NbtElement> entry : source.entries.entrySet()) {
			String key = entry.getKey();
			NbtElement sourceElement = entry.getValue();

			if (sourceElement instanceof NbtCompound sourceCompound
					&& entries.get(key) instanceof NbtCompound existingCompound) {
				existingCompound.copyFrom(sourceCompound);
			}
			else {
				put(key, sourceElement.copy());
			}
		}

		return this;
	}

	@Override
	public void accept(NbtElementVisitor visitor) {
		visitor.visitCompound(this);
	}

	@Override
	public NbtScanner.Result doAccept(NbtScanner visitor) {
		for (Entry<String, NbtElement> entry : entries.entrySet()) {
			NbtElement element = entry.getValue();
			NbtType<?> nbtType = element.getNbtType();

			switch (visitor.visitSubNbtType(nbtType)) {
				case HALT:
					return NbtScanner.Result.HALT;
				case BREAK:
					return visitor.endNested();
				case SKIP:
					continue;
				default:
					break;
			}

			switch (visitor.startSubNbt(nbtType, entry.getKey())) {
				case HALT:
					return NbtScanner.Result.HALT;
				case BREAK:
					return visitor.endNested();
				case SKIP:
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
		}

		return visitor.endNested();
	}

	/**
	 * Кодирует значение через {@link Codec} и сохраняет результат по ключу.
	 *
	 * @param key   ключ записи
	 * @param codec кодек для сериализации
	 * @param value значение для сохранения
	 */
	public <T> void put(String key, Codec<T> codec, T value) {
		put(key, codec, NbtOps.INSTANCE, value);
	}

	/**
	 * Кодирует значение через {@link Codec} и сохраняет результат по ключу,
	 * используя указанный {@link DynamicOps}.
	 *
	 * @param key   ключ записи
	 * @param codec кодек для сериализации
	 * @param ops   операции динамической сериализации
	 * @param value значение для сохранения
	 */
	public <T> void put(String key, Codec<T> codec, DynamicOps<NbtElement> ops, T value) {
		put(key, (NbtElement) codec.encodeStart(ops, value).getOrThrow());
	}

	/**
	 * Кодирует значение через {@link Codec} и сохраняет результат по ключу,
	 * если значение не {@code null}.
	 *
	 * @param key   ключ записи
	 * @param codec кодек для сериализации
	 * @param value значение для сохранения, или {@code null} для пропуска
	 */
	public <T> void putNullable(String key, Codec<T> codec, @Nullable T value) {
		if (value != null) {
			put(key, codec, value);
		}
	}

	/**
	 * Кодирует значение через {@link Codec} и сохраняет результат по ключу,
	 * если значение не {@code null}, используя указанный {@link DynamicOps}.
	 *
	 * @param key   ключ записи
	 * @param codec кодек для сериализации
	 * @param ops   операции динамической сериализации
	 * @param value значение для сохранения, или {@code null} для пропуска
	 */
	public <T> void putNullable(String key, Codec<T> codec, DynamicOps<NbtElement> ops, @Nullable T value) {
		if (value != null) {
			put(key, codec, ops, value);
		}
	}

	/**
	 * Кодирует объект через {@link MapCodec} и сливает результат в этот компаунд.
	 *
	 * @param codec кодек для сериализации
	 * @param value объект для кодирования
	 */
	public <T> void copyFromCodec(MapCodec<T> codec, T value) {
		copyFromCodec(codec, NbtOps.INSTANCE, value);
	}

	/**
	 * Кодирует объект через {@link MapCodec} и сливает результат в этот компаунд,
	 * используя указанный {@link DynamicOps}.
	 *
	 * @param codec кодек для сериализации
	 * @param ops   операции динамической сериализации
	 * @param value объект для кодирования
	 */
	public <T> void copyFromCodec(MapCodec<T> codec, DynamicOps<NbtElement> ops, T value) {
		copyFrom((NbtCompound) codec.encoder().encodeStart(ops, value).getOrThrow());
	}

	/**
	 * Читает и декодирует значение по ключу через {@link Codec}.
	 * При ошибке декодирования логирует её и возвращает {@link Optional#empty()}.
	 *
	 * @param key   ключ записи
	 * @param codec кодек для десериализации
	 * @return декодированное значение или {@link Optional#empty()} при отсутствии/ошибке
	 */
	public <T> Optional<T> get(String key, Codec<T> codec) {
		return get(key, codec, NbtOps.INSTANCE);
	}

	/**
	 * Читает и декодирует значение по ключу через {@link Codec},
	 * используя указанный {@link DynamicOps}.
	 * При ошибке декодирования логирует её и возвращает {@link Optional#empty()}.
	 *
	 * @param key   ключ записи
	 * @param codec кодек для десериализации
	 * @param ops   операции динамической сериализации
	 * @return декодированное значение или {@link Optional#empty()} при отсутствии/ошибке
	 */
	public <T> Optional<T> get(String key, Codec<T> codec, DynamicOps<NbtElement> ops) {
		NbtElement element = get(key);
		return element == null
			? Optional.empty()
			: codec
				.parse(ops, element)
				.resultOrPartial(error -> LOGGER.error(
					"Failed to read field ({}={}): {}",
					new Object[]{key, element, error}
				));
	}

	/**
	 * Декодирует весь компаунд через {@link MapCodec}.
	 * При ошибке декодирования логирует её и возвращает {@link Optional#empty()}.
	 *
	 * @param codec кодек для десериализации
	 * @return декодированное значение или {@link Optional#empty()} при ошибке
	 */
	public <T> Optional<T> decode(MapCodec<T> codec) {
		return decode(codec, NbtOps.INSTANCE);
	}

	/**
	 * Декодирует весь компаунд через {@link MapCodec},
	 * используя указанный {@link DynamicOps}.
	 * При ошибке декодирования логирует её и возвращает {@link Optional#empty()}.
	 *
	 * @param codec кодек для десериализации
	 * @param ops   операции динамической сериализации
	 * @return декодированное значение или {@link Optional#empty()} при ошибке
	 */
	public <T> Optional<T> decode(MapCodec<T> codec, DynamicOps<NbtElement> ops) {
		return codec
				.decode(ops, (MapLike) ops.getMap(this).getOrThrow())
				.resultOrPartial(error -> LOGGER.error("Failed to read value ({}): {}", this, error));
	}
}
