package net.minecraft.nbt;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.RecordBuilder.AbstractStringBuilder;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * Реализация {@link DynamicOps} для NBT-формата.
 * <p>
 * Позволяет использовать NBT как целевой формат для кодеков DFU ({@code Codec}, {@code MapCodec}).
 * Синглтон — доступен через {@link #INSTANCE}.
 */
public class NbtOps implements DynamicOps<NbtElement> {

	public static final NbtOps INSTANCE = new NbtOps();

	private NbtOps() {
	}

	@Override
	public NbtElement empty() {
		return NbtEnd.INSTANCE;
	}

	@Override
	public NbtElement emptyList() {
		return new NbtList();
	}

	@Override
	public NbtElement emptyMap() {
		return new NbtCompound();
	}

	/**
	 * Конвертирует NBT-элемент в целевой формат {@code U}, используя соответствующий метод {@link DynamicOps}.
	 * Поддерживает все 13 типов NBT через exhaustive pattern matching.
	 */
	@Override
	public <U> U convertTo(DynamicOps<U> dynamicOps, NbtElement element) {
		return (U) (switch (element) {
			case NbtEnd ignored -> (Object) dynamicOps.empty();
			case NbtByte(byte value) -> (Object) dynamicOps.createByte(value);
			case NbtShort(short value) -> (Object) dynamicOps.createShort(value);
			case NbtInt(int value) -> (Object) dynamicOps.createInt(value);
			case NbtLong(long value) -> (Object) dynamicOps.createLong(value);
			case NbtFloat(float value) -> (Object) dynamicOps.createFloat(value);
			case NbtDouble(double value) -> (Object) dynamicOps.createDouble(value);
			case NbtByteArray array -> (Object) dynamicOps.createByteList(ByteBuffer.wrap(array.getByteArray()));
			case NbtString(String value) -> (Object) dynamicOps.createString(value);
			case NbtList list -> (Object) convertList(dynamicOps, list);
			case NbtCompound compound -> (Object) convertMap(dynamicOps, compound);
			case NbtIntArray array -> (Object) dynamicOps.createIntList(Arrays.stream(array.getIntArray()));
			case NbtLongArray array -> (Object) dynamicOps.createLongList(Arrays.stream(array.getLongArray()));
			default -> throw new MatchException(null, null);
		});
	}

	@Override
	public DataResult<Number> getNumberValue(NbtElement element) {
		return element
			.asNumber()
			.<DataResult<Number>>map(DataResult::success)
			.orElseGet(() -> DataResult.error(() -> "Not a number"));
	}

	@Override
	public NbtElement createNumeric(Number number) {
		return NbtDouble.of(number.doubleValue());
	}

	@Override
	public NbtElement createByte(byte value) {
		return NbtByte.of(value);
	}

	@Override
	public NbtElement createShort(short value) {
		return NbtShort.of(value);
	}

	@Override
	public NbtElement createInt(int value) {
		return NbtInt.of(value);
	}

	@Override
	public NbtElement createLong(long value) {
		return NbtLong.of(value);
	}

	@Override
	public NbtElement createFloat(float value) {
		return NbtFloat.of(value);
	}

	@Override
	public NbtElement createDouble(double value) {
		return NbtDouble.of(value);
	}

	@Override
	public NbtElement createBoolean(boolean value) {
		return NbtByte.of(value);
	}

	@Override
	public DataResult<String> getStringValue(NbtElement element) {
		return element instanceof NbtString(String value)
			? DataResult.success(value)
			: DataResult.error(() -> "Not a string");
	}

	@Override
	public NbtElement createString(String value) {
		return NbtString.of(value);
	}

	@Override
	public DataResult<NbtElement> mergeToList(NbtElement list, NbtElement element) {
		return createMerger(list)
			.map(merger -> DataResult.success(merger.merge(element).getResult()))
			.orElseGet(() -> DataResult.error(
				() -> "mergeToList called with not a list: " + list,
				list
			));
	}

	@Override
	public DataResult<NbtElement> mergeToList(NbtElement list, List<NbtElement> elements) {
		return createMerger(list)
			.map(merger -> DataResult.success(merger.merge(elements).getResult()))
			.orElseGet(() -> DataResult.error(
				() -> "mergeToList called with not a list: " + list,
				list
			));
	}

	/**
	 * Добавляет пару ключ-значение в NBT-компаунд.
	 * Ключ должен быть {@link NbtString}, иначе возвращается ошибка.
	 */
	@Override
	public DataResult<NbtElement> mergeToMap(NbtElement map, NbtElement key, NbtElement value) {
		if (!(map instanceof NbtCompound) && !(map instanceof NbtEnd)) {
			return DataResult.error(() -> "mergeToMap called with not a map: " + map, map);
		}

		if (!(key instanceof NbtString(String keyStr))) {
			return DataResult.error(() -> "key is not a string: " + key, map);
		}

		NbtCompound result = map instanceof NbtCompound compound ? compound.shallowCopy() : new NbtCompound();
		result.put(keyStr, value);
		return DataResult.success(result);
	}

	@Override
	public DataResult<NbtElement> mergeToMap(NbtElement map, MapLike<NbtElement> entries) {
		if (!(map instanceof NbtCompound) && !(map instanceof NbtEnd)) {
			return DataResult.error(() -> "mergeToMap called with not a map: " + map, map);
		}

		Iterator<Pair<NbtElement, NbtElement>> iterator = entries.entries().iterator();
		if (!iterator.hasNext()) {
			return map == empty() ? DataResult.success(emptyMap()) : DataResult.success(map);
		}

		NbtCompound result = map instanceof NbtCompound compound ? compound.shallowCopy() : new NbtCompound();
		List<NbtElement> invalidKeys = new ArrayList<>();

		iterator.forEachRemaining(pair -> {
			NbtElement entryKey = (NbtElement) pair.getFirst();
			if (entryKey instanceof NbtString(String keyStr)) {
				result.put(keyStr, (NbtElement) pair.getSecond());
			}
			else {
				invalidKeys.add(entryKey);
			}
		});

		return invalidKeys.isEmpty()
			? DataResult.success(result)
			: DataResult.error(() -> "some keys are not strings: " + invalidKeys, result);
	}

	@Override
	public DataResult<NbtElement> mergeToMap(NbtElement map, Map<NbtElement, NbtElement> entries) {
		if (!(map instanceof NbtCompound) && !(map instanceof NbtEnd)) {
			return DataResult.error(() -> "mergeToMap called with not a map: " + map, map);
		}

		if (entries.isEmpty()) {
			return map == empty() ? DataResult.success(emptyMap()) : DataResult.success(map);
		}

		NbtCompound result = map instanceof NbtCompound compound ? compound.shallowCopy() : new NbtCompound();
		List<NbtElement> invalidKeys = new ArrayList<>();

		for (Entry<NbtElement, NbtElement> entry : entries.entrySet()) {
			NbtElement entryKey = entry.getKey();
			if (entryKey instanceof NbtString(String keyStr)) {
				result.put(keyStr, entry.getValue());
			}
			else {
				invalidKeys.add(entryKey);
			}
		}

		return invalidKeys.isEmpty()
			? DataResult.success(result)
			: DataResult.error(() -> "some keys are not strings: " + invalidKeys, result);
	}

	@Override
	public DataResult<Stream<Pair<NbtElement, NbtElement>>> getMapValues(NbtElement element) {
		return element instanceof NbtCompound compound
			? DataResult.success(
				compound.entrySet()
					.stream()
					.map(entry -> Pair.of(createString(entry.getKey()), entry.getValue()))
			)
			: DataResult.error(() -> "Not a map: " + element);
	}

	@Override
	public DataResult<Consumer<BiConsumer<NbtElement, NbtElement>>> getMapEntries(NbtElement element) {
		return element instanceof NbtCompound compound
			? DataResult.success((Consumer<BiConsumer<NbtElement, NbtElement>>) biConsumer -> {
				for (Entry<String, NbtElement> entry : compound.entrySet()) {
					biConsumer.accept(createString(entry.getKey()), entry.getValue());
				}
			})
			: DataResult.error(() -> "Not a map: " + element);
	}

	@Override
	public DataResult<MapLike<NbtElement>> getMap(NbtElement element) {
		return element instanceof NbtCompound compound
			? DataResult.success(new MapLike<NbtElement>() {
				public @Nullable NbtElement get(NbtElement key) {
					if (key instanceof NbtString(String keyStr)) {
						return compound.get(keyStr);
					}

					throw new UnsupportedOperationException(
						"Cannot get map entry with non-string key: " + key
					);
				}

				public @Nullable NbtElement get(String key) {
					return compound.get(key);
				}

				public Stream<Pair<NbtElement, NbtElement>> entries() {
					return compound.entrySet()
						.stream()
						.map(entry -> Pair.of(NbtOps.this.createString(entry.getKey()), entry.getValue()));
				}

				@Override
				public String toString() {
					return "MapLike[" + compound + "]";
				}
			})
			: DataResult.error(() -> "Not a map: " + element);
	}

	@Override
	public NbtElement createMap(Stream<Pair<NbtElement, NbtElement>> stream) {
		NbtCompound compound = new NbtCompound();
		stream.forEach(entry -> {
			NbtElement key = (NbtElement) entry.getFirst();
			NbtElement value = (NbtElement) entry.getSecond();
			if (key instanceof NbtString(String keyStr)) {
				compound.put(keyStr, value);
			}
			else {
				throw new UnsupportedOperationException("Cannot create map with non-string key: " + key);
			}
		});
		return compound;
	}

	@Override
	public DataResult<Stream<NbtElement>> getStream(NbtElement element) {
		return element instanceof AbstractNbtList list
			? DataResult.success(list.stream())
			: DataResult.error(() -> "Not a list");
	}

	@Override
	public DataResult<Consumer<Consumer<NbtElement>>> getList(NbtElement element) {
		return element instanceof AbstractNbtList list
			? DataResult.success(list::forEach)
			: DataResult.error(() -> "Not a list: " + element);
	}

	@Override
	public DataResult<ByteBuffer> getByteBuffer(NbtElement element) {
		return element instanceof NbtByteArray array
			? DataResult.success(ByteBuffer.wrap(array.getByteArray()))
			: DynamicOps.super.getByteBuffer(element);
	}

	@Override
	public NbtElement createByteList(ByteBuffer byteBuffer) {
		ByteBuffer duplicate = byteBuffer.duplicate().clear();
		byte[] bytes = new byte[byteBuffer.capacity()];
		duplicate.get(0, bytes, 0, bytes.length);
		return new NbtByteArray(bytes);
	}

	@Override
	public DataResult<IntStream> getIntStream(NbtElement element) {
		return element instanceof NbtIntArray array
			? DataResult.success(Arrays.stream(array.getIntArray()))
			: DynamicOps.super.getIntStream(element);
	}

	@Override
	public NbtElement createIntList(IntStream intStream) {
		return new NbtIntArray(intStream.toArray());
	}

	@Override
	public DataResult<LongStream> getLongStream(NbtElement element) {
		return element instanceof NbtLongArray array
			? DataResult.success(Arrays.stream(array.getLongArray()))
			: DynamicOps.super.getLongStream(element);
	}

	@Override
	public NbtElement createLongList(LongStream longStream) {
		return new NbtLongArray(longStream.toArray());
	}

	@Override
	public NbtElement createList(Stream<NbtElement> stream) {
		return new NbtList(stream.collect(Util.toArrayList()));
	}

	@Override
	public NbtElement remove(NbtElement element, String key) {
		if (element instanceof NbtCompound compound) {
			NbtCompound copy = compound.shallowCopy();
			copy.remove(key);
			return copy;
		}

		return element;
	}

	@Override
	public String toString() {
		return "NBT";
	}

	@Override
	public RecordBuilder<NbtElement> mapBuilder() {
		return new NbtOps.MapBuilder();
	}

	private static Optional<NbtOps.Merger> createMerger(NbtElement nbt) {
		if (nbt instanceof NbtEnd) {
			return Optional.of(new NbtOps.CompoundListMerger());
		}

		if (nbt instanceof AbstractNbtList list) {
			if (list.isEmpty()) {
				return Optional.of(new NbtOps.CompoundListMerger());
			}

			return switch (list) {
				case NbtList nbtList -> Optional.of(new NbtOps.CompoundListMerger(nbtList));
				case NbtByteArray array -> Optional.of(new NbtOps.ByteArrayMerger(array.getByteArray()));
				case NbtIntArray array -> Optional.of(new NbtOps.IntArrayMerger(array.getIntArray()));
				case NbtLongArray array -> Optional.of(new NbtOps.LongArrayMerger(array.getLongArray()));
				default -> throw new MatchException(null, null);
			};
		}

		return Optional.empty();
	}

	/**
	 * Слияние элементов в {@link NbtByteArray}.
	 * При добавлении не-байтового элемента автоматически деградирует до {@link CompoundListMerger}.
	 */
	static class ByteArrayMerger implements NbtOps.Merger {

		private final ByteArrayList list = new ByteArrayList();

		public ByteArrayMerger(byte[] values) {
			list.addElements(0, values);
		}

		@Override
		public NbtOps.Merger merge(NbtElement nbt) {
			if (nbt instanceof NbtByte nbtByte) {
				list.add(nbtByte.byteValue());
				return this;
			}

			return new NbtOps.CompoundListMerger(list).merge(nbt);
		}

		@Override
		public NbtElement getResult() {
			return new NbtByteArray(list.toByteArray());
		}
	}

	/**
	 * Слияние элементов в {@link NbtList}.
	 * Используется как универсальный fallback при гетерогенных типах.
	 */
	static class CompoundListMerger implements NbtOps.Merger {

		private final NbtList list = new NbtList();

		CompoundListMerger() {
		}

		CompoundListMerger(NbtList nbtList) {
			list.addAll(nbtList);
		}

		public CompoundListMerger(IntArrayList source) {
			source.forEach(value -> list.add(NbtInt.of(value)));
		}

		public CompoundListMerger(ByteArrayList source) {
			source.forEach(value -> list.add(NbtByte.of(value)));
		}

		public CompoundListMerger(LongArrayList source) {
			source.forEach(value -> list.add(NbtLong.of(value)));
		}

		@Override
		public NbtOps.Merger merge(NbtElement nbt) {
			list.add(nbt);
			return this;
		}

		@Override
		public NbtElement getResult() {
			return list;
		}
	}

	/**
	 * Слияние элементов в {@link NbtIntArray}.
	 * При добавлении не-int элемента деградирует до {@link CompoundListMerger}.
	 */
	static class IntArrayMerger implements NbtOps.Merger {

		private final IntArrayList list = new IntArrayList();

		public IntArrayMerger(int[] values) {
			list.addElements(0, values);
		}

		@Override
		public NbtOps.Merger merge(NbtElement nbt) {
			if (nbt instanceof NbtInt nbtInt) {
				list.add(nbtInt.intValue());
				return this;
			}

			return new NbtOps.CompoundListMerger(list).merge(nbt);
		}

		@Override
		public NbtElement getResult() {
			return new NbtIntArray(list.toIntArray());
		}
	}

	/**
	 * Слияние элементов в {@link NbtLongArray}.
	 * При добавлении не-long элемента деградирует до {@link CompoundListMerger}.
	 */
	static class LongArrayMerger implements NbtOps.Merger {

		private final LongArrayList list = new LongArrayList();

		public LongArrayMerger(long[] values) {
			list.addElements(0, values);
		}

		@Override
		public NbtOps.Merger merge(NbtElement nbt) {
			if (nbt instanceof NbtLong nbtLong) {
				list.add(nbtLong.longValue());
				return this;
			}

			return new NbtOps.CompoundListMerger(list).merge(nbt);
		}

		@Override
		public NbtElement getResult() {
			return new NbtLongArray(list.toLongArray());
		}
	}

	/**
	 * Построитель NBT-компаунда для использования с DFU {@link RecordBuilder}.
	 */
	class MapBuilder extends AbstractStringBuilder<NbtElement, NbtCompound> {

		protected MapBuilder() {
			super(NbtOps.this);
		}

		@Override
		protected NbtCompound initBuilder() {
			return new NbtCompound();
		}

		@Override
		protected NbtCompound append(String key, NbtElement value, NbtCompound compound) {
			compound.put(key, value);
			return compound;
		}

		/**
		 * Финализирует построение: если {@code prefix} является компаундом,
		 * сливает накопленные поля в его копию. Если {@code prefix} — {@code NbtEnd} или {@code null},
		 * возвращает накопленный компаунд как есть.
		 */
		@Override
		protected DataResult<NbtElement> build(NbtCompound accumulated, NbtElement prefix) {
			if (prefix == null || prefix == NbtEnd.INSTANCE) {
				return DataResult.success(accumulated);
			}

			if (!(prefix instanceof NbtCompound prefixCompound)) {
				return DataResult.error(
					() -> "mergeToMap called with not a map: " + prefix,
					prefix
				);
			}

			NbtCompound result = prefixCompound.shallowCopy();

			for (Entry<String, NbtElement> entry : accumulated.entrySet()) {
				result.put(entry.getKey(), entry.getValue());
			}

			return DataResult.success(result);
		}
	}

	/**
	 * Стратегия слияния NBT-элементов в коллекцию.
	 * Реализации выбирают оптимальный тип контейнера и деградируют при необходимости.
	 */
	interface Merger {

		NbtOps.Merger merge(NbtElement nbt);

		default NbtOps.Merger merge(Iterable<NbtElement> nbts) {
			NbtOps.Merger merger = this;

			for (NbtElement element : nbts) {
				merger = merger.merge(element);
			}

			return merger;
		}

		default NbtOps.Merger merge(Stream<NbtElement> nbts) {
			return merge(nbts::iterator);
		}

		NbtElement getResult();
	}
}
