package net.minecraft.util.dynamic;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.ListBuilder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.RecordBuilder.AbstractUniversalBuilder;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * Реализация {@link DynamicOps}, которая вместо реальных данных вычисляет
 * хэш-коды структур. Используется для быстрого сравнения сложных объектов
 * без полной сериализации.
 *
 * <p>Все операции чтения (get*) возвращают ошибку — этот ops предназначен
 * исключительно для записи/хэширования.
 */
public class HashCodeOps implements DynamicOps<HashCode> {

	private static final byte TAG_EMPTY = 1;
	private static final byte TAG_MAP_START = 2;
	private static final byte TAG_MAP_END = 3;
	private static final byte TAG_LIST_START = 4;
	private static final byte TAG_LIST_END = 5;
	private static final byte TAG_BYTE = 6;
	private static final byte TAG_SHORT = 7;
	private static final byte TAG_INT = 8;
	private static final byte TAG_LONG = 9;
	private static final byte TAG_FLOAT = 10;
	private static final byte TAG_DOUBLE = 11;
	private static final byte TAG_STRING = 12;
	private static final byte TAG_BOOL = 13;
	private static final byte TAG_BYTE_LIST_START = 14;
	private static final byte TAG_BYTE_LIST_END = 15;
	private static final byte TAG_INT_LIST_START = 16;
	private static final byte TAG_INT_LIST_END = 17;
	private static final byte TAG_LONG_LIST_START = 18;
	private static final byte TAG_LONG_LIST_END = 19;

	private static final byte[] EMPTY_BYTES = {TAG_EMPTY};
	private static final byte[] FALSE_BYTES = {TAG_BOOL, 0};
	private static final byte[] TRUE_BYTES = {TAG_BOOL, 1};
	public static final byte[] emptyMapByteArray = {TAG_MAP_START, TAG_MAP_END};
	public static final byte[] emptyListByteArray = {TAG_LIST_START, TAG_LIST_END};

	private static final DataResult<Object> ERROR = DataResult.error(() -> "Unsupported operation");
	private static final Comparator<HashCode> HASH_CODE_COMPARATOR = Comparator.comparingLong(HashCode::padToLong);
	private static final Comparator<Entry<HashCode, HashCode>> ENTRY_COMPARATOR =
			Entry.<HashCode, HashCode>comparingByKey(HASH_CODE_COMPARATOR)
					.thenComparing(Entry.comparingByValue(HASH_CODE_COMPARATOR));
	private static final Comparator<Pair<HashCode, HashCode>> PAIR_COMPARATOR =
			Comparator.<Pair<HashCode, HashCode>, HashCode>comparing(Pair::getFirst, HASH_CODE_COMPARATOR)
					.thenComparing(Pair::getSecond, HASH_CODE_COMPARATOR);

	public static final HashCodeOps INSTANCE = new HashCodeOps(Hashing.crc32c());

	final HashFunction function;
	final HashCode empty;
	private final HashCode emptyMap;
	private final HashCode emptyList;
	private final HashCode hashTrue;
	private final HashCode hashFalse;

	public HashCodeOps(HashFunction function) {
		this.function = function;
		empty = function.hashBytes(EMPTY_BYTES);
		emptyMap = function.hashBytes(emptyMapByteArray);
		emptyList = function.hashBytes(emptyListByteArray);
		hashFalse = function.hashBytes(FALSE_BYTES);
		hashTrue = function.hashBytes(TRUE_BYTES);
	}

	@Override
	public HashCode empty() {
		return empty;
	}

	@Override
	public HashCode emptyMap() {
		return emptyMap;
	}

	@Override
	public HashCode emptyList() {
		return emptyList;
	}

	@Override
	public HashCode createNumeric(Number number) {
		return switch (number) {
			case Byte b -> createByte(b);
			case Short s -> createShort(s);
			case Integer i -> createInt(i);
			case Long l -> createLong(l);
			case Double d -> createDouble(d);
			case Float f -> createFloat(f);
			default -> createDouble(number.doubleValue());
		};
	}

	@Override
	public HashCode createByte(byte b) {
		return function.newHasher(2).putByte(TAG_BYTE).putByte(b).hash();
	}

	@Override
	public HashCode createShort(short s) {
		return function.newHasher(3).putByte(TAG_SHORT).putShort(s).hash();
	}

	@Override
	public HashCode createInt(int i) {
		return function.newHasher(5).putByte(TAG_INT).putInt(i).hash();
	}

	@Override
	public HashCode createLong(long l) {
		return function.newHasher(9).putByte(TAG_LONG).putLong(l).hash();
	}

	@Override
	public HashCode createFloat(float f) {
		return function.newHasher(5).putByte(TAG_FLOAT).putFloat(f).hash();
	}

	@Override
	public HashCode createDouble(double d) {
		return function.newHasher(9).putByte(TAG_DOUBLE).putDouble(d).hash();
	}

	@Override
	public HashCode createString(String string) {
		return function.newHasher()
				.putByte(TAG_STRING)
				.putInt(string.length())
				.putUnencodedChars(string)
				.hash();
	}

	@Override
	public HashCode createBoolean(boolean value) {
		return value ? hashTrue : hashFalse;
	}

	@Override
	public HashCode createMap(Stream<Pair<HashCode, HashCode>> stream) {
		return hash(function.newHasher(), stream).hash();
	}

	@Override
	public HashCode createMap(Map<HashCode, HashCode> map) {
		return hash(function.newHasher(), map).hash();
	}

	@Override
	public HashCode createList(Stream<HashCode> stream) {
		Hasher hasher = function.newHasher();
		hasher.putByte(TAG_LIST_START);
		stream.forEach(hashCode -> hasher.putBytes(hashCode.asBytes()));
		hasher.putByte(TAG_LIST_END);
		return hasher.hash();
	}

	@Override
	public HashCode createByteList(ByteBuffer byteBuffer) {
		Hasher hasher = function.newHasher();
		hasher.putByte(TAG_BYTE_LIST_START);
		hasher.putBytes(byteBuffer);
		hasher.putByte(TAG_BYTE_LIST_END);
		return hasher.hash();
	}

	@Override
	public HashCode createIntList(IntStream intStream) {
		Hasher hasher = function.newHasher();
		hasher.putByte(TAG_INT_LIST_START);
		intStream.forEach(hasher::putInt);
		hasher.putByte(TAG_INT_LIST_END);
		return hasher.hash();
	}

	@Override
	public HashCode createLongList(LongStream longStream) {
		Hasher hasher = function.newHasher();
		hasher.putByte(TAG_LONG_LIST_START);
		longStream.forEach(hasher::putLong);
		hasher.putByte(TAG_LONG_LIST_END);
		return hasher.hash();
	}

	@Override
	public HashCode remove(HashCode hashCode, String key) {
		return hashCode;
	}

	@Override
	public RecordBuilder<HashCode> mapBuilder() {
		return new Builder();
	}

	@Override
	public ListBuilder<HashCode> listBuilder() {
		return new HashListBuilder();
	}

	@Override
	public <U> U convertTo(DynamicOps<U> dynamicOps, HashCode hashCode) {
		throw new UnsupportedOperationException("Can't convert from this type");
	}

	@Override
	public DataResult<Number> getNumberValue(HashCode hashCode) {
		return error();
	}

	@Override
	public Number getNumberValue(HashCode hashCode, Number fallback) {
		return fallback;
	}

	@Override
	public DataResult<Boolean> getBooleanValue(HashCode hashCode) {
		return error();
	}

	@Override
	public DataResult<String> getStringValue(HashCode hashCode) {
		return error();
	}

	@Override
	public DataResult<HashCode> get(HashCode hashCode, String key) {
		return error();
	}

	@Override
	public DataResult<HashCode> getGeneric(HashCode hashCode, HashCode key) {
		return error();
	}

	@Override
	public HashCode set(HashCode hashCode, String key, HashCode value) {
		return hashCode;
	}

	@Override
	public HashCode update(HashCode hashCode, String key, Function<HashCode, HashCode> updater) {
		return hashCode;
	}

	@Override
	public HashCode updateGeneric(HashCode hashCode, HashCode key, Function<HashCode, HashCode> updater) {
		return hashCode;
	}

	@Override
	public DataResult<HashCode> mergeToList(HashCode hashCode, HashCode value) {
		return isEmpty(hashCode) ? DataResult.success(createList(Stream.of(value))) : error();
	}

	@Override
	public DataResult<HashCode> mergeToList(HashCode hashCode, List<HashCode> list) {
		return isEmpty(hashCode) ? DataResult.success(createList(list.stream())) : error();
	}

	@Override
	public DataResult<HashCode> mergeToMap(HashCode hashCode, HashCode key, HashCode value) {
		return isEmpty(hashCode) ? DataResult.success(createMap(Map.of(key, value))) : error();
	}

	@Override
	public DataResult<HashCode> mergeToMap(HashCode hashCode, Map<HashCode, HashCode> map) {
		return isEmpty(hashCode) ? DataResult.success(createMap(map)) : error();
	}

	@Override
	public DataResult<HashCode> mergeToMap(HashCode hashCode, MapLike<HashCode> mapLike) {
		return isEmpty(hashCode) ? DataResult.success(createMap(mapLike.entries())) : error();
	}

	@Override
	public DataResult<Stream<Pair<HashCode, HashCode>>> getMapValues(HashCode hashCode) {
		return error();
	}

	@Override
	public DataResult<Consumer<BiConsumer<HashCode, HashCode>>> getMapEntries(HashCode hashCode) {
		return error();
	}

	@Override
	public DataResult<Stream<HashCode>> getStream(HashCode hashCode) {
		return error();
	}

	@Override
	public DataResult<Consumer<Consumer<HashCode>>> getList(HashCode hashCode) {
		return error();
	}

	@Override
	public DataResult<MapLike<HashCode>> getMap(HashCode hashCode) {
		return error();
	}

	@Override
	public DataResult<ByteBuffer> getByteBuffer(HashCode hashCode) {
		return error();
	}

	@Override
	public DataResult<IntStream> getIntStream(HashCode hashCode) {
		return error();
	}

	@Override
	public DataResult<LongStream> getLongStream(HashCode hashCode) {
		return error();
	}

	@Override
	public String toString() {
		return "Hash " + function;
	}

	boolean isEmpty(HashCode hashCode) {
		return hashCode.equals(empty);
	}

	private static Hasher hash(Hasher hasher, Map<HashCode, HashCode> map) {
		hasher.putByte(TAG_MAP_START);
		map.entrySet()
				.stream()
				.sorted(ENTRY_COMPARATOR)
				.forEach(entry -> hasher.putBytes(entry.getKey().asBytes()).putBytes(entry.getValue().asBytes()));
		hasher.putByte(TAG_MAP_END);
		return hasher;
	}

	static Hasher hash(Hasher hasher, Stream<Pair<HashCode, HashCode>> pairs) {
		hasher.putByte(TAG_MAP_START);
		pairs.sorted(PAIR_COMPARATOR)
				.forEach(pair -> hasher
						.putBytes(pair.getFirst().asBytes())
						.putBytes(pair.getSecond().asBytes()));
		hasher.putByte(TAG_MAP_END);
		return hasher;
	}

	@SuppressWarnings("unchecked")
	private static <T> DataResult<T> error() {
		return (DataResult<T>) ERROR;
	}

	final class Builder extends AbstractUniversalBuilder<HashCode, List<Pair<HashCode, HashCode>>> {

		public Builder() {
			super(HashCodeOps.this);
		}

		@Override
		protected List<Pair<HashCode, HashCode>> initBuilder() {
			return new ArrayList<>();
		}

		@Override
		protected List<Pair<HashCode, HashCode>> append(
				HashCode key,
				HashCode value,
				List<Pair<HashCode, HashCode>> list
		) {
			list.add(Pair.of(key, value));
			return list;
		}

		@Override
		protected DataResult<HashCode> build(List<Pair<HashCode, HashCode>> list, HashCode prefix) {
			assert HashCodeOps.this.isEmpty(prefix);

			return DataResult.success(hash(function.newHasher(), list.stream()).hash());
		}
	}

	class HashListBuilder extends AbstractListBuilder<HashCode, Hasher> {

		public HashListBuilder() {
			super(HashCodeOps.this);
		}

		@Override
		protected Hasher initBuilder() {
			return function.newHasher().putByte(TAG_LIST_START);
		}

		@Override
		protected Hasher add(Hasher hasher, HashCode hashCode) {
			return hasher.putBytes(hashCode.asBytes());
		}

		@Override
		protected DataResult<HashCode> build(Hasher hasher, HashCode prefix) {
			assert prefix.equals(empty);

			hasher.putByte(TAG_LIST_END);
			return DataResult.success(hasher.hash());
		}
	}
}
