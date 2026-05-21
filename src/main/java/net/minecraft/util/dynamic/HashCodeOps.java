package net.minecraft.util.dynamic;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
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
 * {@code HashCodeOps}.
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
	private static final byte[] emptyByteArray = new byte[]{1};
	private static final byte[] falseByteArray = new byte[]{13, 0};
	private static final byte[] trueByteArray = new byte[]{13, 1};
	public static final byte[] emptyMapByteArray = new byte[]{2, 3};
	public static final byte[] emptyListByteArray = new byte[]{4, 5};
	private static final DataResult<Object> ERROR = DataResult.error(() -> "Unsupported operation");
	private static final Comparator<HashCode> HASH_CODE_COMPARATOR = Comparator.comparingLong(HashCode::padToLong);
	private static final Comparator<Entry<HashCode, HashCode>>
			ENTRY_COMPARATOR =
			Entry.<HashCode, HashCode>comparingByKey(HASH_CODE_COMPARATOR)
			     .thenComparing(Entry.comparingByValue(HASH_CODE_COMPARATOR));
	private static final Comparator<Pair<HashCode, HashCode>>
			PAIR_COMPARATOR =
			Comparator.<Pair<HashCode, HashCode>, HashCode>comparing(
					          Pair::getFirst, HASH_CODE_COMPARATOR
			          )
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
		this.empty = function.hashBytes(emptyByteArray);
		this.emptyMap = function.hashBytes(emptyMapByteArray);
		this.emptyList = function.hashBytes(emptyListByteArray);
		this.hashFalse = function.hashBytes(falseByteArray);
		this.hashTrue = function.hashBytes(trueByteArray);
	}

	/**
	 * Empty.
	 *
	 * @return HashCode — результат операции
	 */
	public HashCode empty() {
		return this.empty;
	}

	/**
	 * Empty map.
	 *
	 * @return HashCode — результат операции
	 */
	public HashCode emptyMap() {
		return this.emptyMap;
	}

	/**
	 * Empty list.
	 *
	 * @return HashCode — результат операции
	 */
	public HashCode emptyList() {
		return this.emptyList;
	}

	/**
	 * Создаёт numeric.
	 *
	 * @param number number
	 *
	 * @return HashCode — результат операции
	 */
	public HashCode createNumeric(Number number) {
		return switch (number) {
			case Byte byte_ -> this.createByte(byte_);
			case Short short_ -> this.createShort(short_);
			case Integer integer -> this.createInt(integer);
			case Long long_ -> this.createLong(long_);
			case Double double_ -> this.createDouble(double_);
			case Float float_ -> this.createFloat(float_);
			default -> this.createDouble(number.doubleValue());
		};
	}

	/**
	 * Создаёт byte.
	 *
	 * @param b b
	 *
	 * @return HashCode — результат операции
	 */
	public HashCode createByte(byte b) {
		return this.function.newHasher(2).putByte((byte) 6).putByte(b).hash();
	}

	/**
	 * Создаёт short.
	 *
	 * @param s s
	 *
	 * @return HashCode — результат операции
	 */
	public HashCode createShort(short s) {
		return this.function.newHasher(3).putByte((byte) 7).putShort(s).hash();
	}

	/**
	 * Создаёт int.
	 *
	 * @param i i
	 *
	 * @return HashCode — результат операции
	 */
	public HashCode createInt(int i) {
		return this.function.newHasher(5).putByte((byte) 8).putInt(i).hash();
	}

	/**
	 * Создаёт long.
	 *
	 * @param l l
	 *
	 * @return HashCode — результат операции
	 */
	public HashCode createLong(long l) {
		return this.function.newHasher(9).putByte((byte) 9).putLong(l).hash();
	}

	/**
	 * Создаёт float.
	 *
	 * @param f f
	 *
	 * @return HashCode — результат операции
	 */
	public HashCode createFloat(float f) {
		return this.function.newHasher(5).putByte((byte) 10).putFloat(f).hash();
	}

	/**
	 * Создаёт double.
	 *
	 * @param d d
	 *
	 * @return HashCode — результат операции
	 */
	public HashCode createDouble(double d) {
		return this.function.newHasher(9).putByte((byte) 11).putDouble(d).hash();
	}

	/**
	 * Создаёт string.
	 *
	 * @param string string
	 *
	 * @return HashCode — результат операции
	 */
	public HashCode createString(String string) {
		return this.function.newHasher().putByte((byte) 12).putInt(string.length()).putUnencodedChars(string).hash();
	}

	/**
	 * Создаёт boolean.
	 *
	 * @param bl bl
	 *
	 * @return HashCode — результат операции
	 */
	public HashCode createBoolean(boolean bl) {
		return bl ? this.hashTrue : this.hashFalse;
	}

	private static Hasher hash(Hasher hasher, Map<HashCode, HashCode> map) {
		hasher.putByte((byte) 2);
		map
				.entrySet()
				.stream()
				.sorted(ENTRY_COMPARATOR)
				.forEach(entry -> hasher.putBytes(entry.getKey().asBytes()).putBytes(entry.getValue().asBytes()));
		hasher.putByte((byte) 3);
		return hasher;
	}

	static Hasher hash(Hasher hasher, Stream<Pair<HashCode, HashCode>> pairs) {
		hasher.putByte((byte) 2);
		pairs
				.sorted(PAIR_COMPARATOR)
				.forEach(pair -> hasher
						.putBytes(((HashCode) pair.getFirst()).asBytes())
						.putBytes(((HashCode) pair.getSecond()).asBytes()));
		hasher.putByte((byte) 3);
		return hasher;
	}

	/**
	 * Создаёт map.
	 *
	 * @param stream stream
	 *
	 * @return HashCode — результат операции
	 */
	public HashCode createMap(Stream<Pair<HashCode, HashCode>> stream) {
		return hash(this.function.newHasher(), stream).hash();
	}

	/**
	 * Создаёт map.
	 *
	 * @param map map
	 *
	 * @return HashCode — результат операции
	 */
	public HashCode createMap(Map<HashCode, HashCode> map) {
		return hash(this.function.newHasher(), map).hash();
	}

	/**
	 * Создаёт list.
	 *
	 * @param stream stream
	 *
	 * @return HashCode — результат операции
	 */
	public HashCode createList(Stream<HashCode> stream) {
		Hasher hasher = this.function.newHasher();
		hasher.putByte((byte) 4);
		stream.forEach(hashCode -> hasher.putBytes(hashCode.asBytes()));
		hasher.putByte((byte) 5);
		return hasher.hash();
	}

	/**
	 * Создаёт byte list.
	 *
	 * @param byteBuffer byte buffer
	 *
	 * @return HashCode — результат операции
	 */
	public HashCode createByteList(ByteBuffer byteBuffer) {
		Hasher hasher = this.function.newHasher();
		hasher.putByte((byte) 14);
		hasher.putBytes(byteBuffer);
		hasher.putByte((byte) 15);
		return hasher.hash();
	}

	/**
	 * Создаёт int list.
	 *
	 * @param intStream int stream
	 *
	 * @return HashCode — результат операции
	 */
	public HashCode createIntList(IntStream intStream) {
		Hasher hasher = this.function.newHasher();
		hasher.putByte((byte) 16);
		intStream.forEach(hasher::putInt);
		hasher.putByte((byte) 17);
		return hasher.hash();
	}

	/**
	 * Создаёт long list.
	 *
	 * @param longStream long stream
	 *
	 * @return HashCode — результат операции
	 */
	public HashCode createLongList(LongStream longStream) {
		Hasher hasher = this.function.newHasher();
		hasher.putByte((byte) 18);
		longStream.forEach(hasher::putLong);
		hasher.putByte((byte) 19);
		return hasher.hash();
	}

	/**
	 * Remove.
	 *
	 * @param hashCode hash code
	 * @param string string
	 *
	 * @return HashCode — результат операции
	 */
	public HashCode remove(HashCode hashCode, String string) {
		return hashCode;
	}

	/**
	 * Map builder.
	 *
	 * @return RecordBuilder — результат операции
	 */
	public RecordBuilder<HashCode> mapBuilder() {
		return new HashCodeOps.Builder();
	}

	public com.mojang.serialization.ListBuilder<HashCode> listBuilder() {
		return new HashCodeOps.ListBuilder();
	}

	@Override
	public String toString() {
		return "Hash " + this.function;
	}

	/**
	 * Конвертирует to.
	 *
	 * @param dynamicOps dynamic ops
	 * @param hashCode hash code
	 *
	 * @return U — результат операции
	 */
	public <U> U convertTo(DynamicOps<U> dynamicOps, HashCode hashCode) {
		throw new UnsupportedOperationException("Can't convert from this type");
	}

	public Number getNumberValue(HashCode hashCode, Number number) {
		return number;
	}

	/**
	 * Set.
	 *
	 * @param hashCode hash code
	 * @param string string
	 * @param hashCode2 hash code2
	 *
	 * @return HashCode — результат операции
	 */
	public HashCode set(HashCode hashCode, String string, HashCode hashCode2) {
		return hashCode;
	}

	/**
	 * Update.
	 *
	 * @param hashCode hash code
	 * @param string string
	 * @param function function
	 *
	 * @return HashCode — результат операции
	 */
	public HashCode update(HashCode hashCode, String string, Function<HashCode, HashCode> function) {
		return hashCode;
	}

	/**
	 * Обновляет generic.
	 *
	 * @param hashCode hash code
	 * @param hashCode2 hash code2
	 * @param function function
	 *
	 * @return HashCode — результат операции
	 */
	public HashCode updateGeneric(HashCode hashCode, HashCode hashCode2, Function<HashCode, HashCode> function) {
		return hashCode;
	}

	private static <T> DataResult<T> error() {
		return (DataResult<T>) ERROR;
	}

	/**
	 * Get.
	 *
	 * @param hashCode hash code
	 * @param string string
	 *
	 * @return DataResult — 
	 */
	public DataResult<HashCode> get(HashCode hashCode, String string) {
		return error();
	}

	public DataResult<HashCode> getGeneric(HashCode hashCode, HashCode hashCode2) {
		return error();
	}

	public DataResult<Number> getNumberValue(HashCode hashCode) {
		return error();
	}

	public DataResult<Boolean> getBooleanValue(HashCode hashCode) {
		return error();
	}

	public DataResult<String> getStringValue(HashCode hashCode) {
		return error();
	}

	boolean isEmpty(HashCode hashCode) {
		return hashCode.equals(this.empty);
	}

	/**
	 * Merge to list.
	 *
	 * @param hashCode hash code
	 * @param hashCode2 hash code2
	 *
	 * @return DataResult — результат операции
	 */
	public DataResult<HashCode> mergeToList(HashCode hashCode, HashCode hashCode2) {
		return this.isEmpty(hashCode) ? DataResult.success(this.createList(Stream.of(hashCode2))) : error();
	}

	/**
	 * Merge to list.
	 *
	 * @param hashCode hash code
	 * @param list list
	 *
	 * @return DataResult — результат операции
	 */
	public DataResult<HashCode> mergeToList(HashCode hashCode, List<HashCode> list) {
		return this.isEmpty(hashCode) ? DataResult.success(this.createList(list.stream())) : error();
	}

	/**
	 * Merge to map.
	 *
	 * @param hashCode hash code
	 * @param hashCode2 hash code2
	 * @param hashCode3 hash code3
	 *
	 * @return DataResult — результат операции
	 */
	public DataResult<HashCode> mergeToMap(HashCode hashCode, HashCode hashCode2, HashCode hashCode3) {
		return this.isEmpty(hashCode) ? DataResult.success(this.createMap(Map.of(hashCode2, hashCode3))) : error();
	}

	/**
	 * Merge to map.
	 *
	 * @param hashCode hash code
	 * @param map map
	 *
	 * @return DataResult — результат операции
	 */
	public DataResult<HashCode> mergeToMap(HashCode hashCode, Map<HashCode, HashCode> map) {
		return this.isEmpty(hashCode) ? DataResult.success(this.createMap(map)) : error();
	}

	/**
	 * Merge to map.
	 *
	 * @param hashCode hash code
	 * @param mapLike map like
	 *
	 * @return DataResult — результат операции
	 */
	public DataResult<HashCode> mergeToMap(HashCode hashCode, MapLike<HashCode> mapLike) {
		return this.isEmpty(hashCode) ? DataResult.success(this.createMap(mapLike.entries())) : error();
	}

	public DataResult<Stream<Pair<HashCode, HashCode>>> getMapValues(HashCode hashCode) {
		return error();
	}

	public DataResult<Consumer<BiConsumer<HashCode, HashCode>>> getMapEntries(HashCode hashCode) {
		return error();
	}

	public DataResult<Stream<HashCode>> getStream(HashCode hashCode) {
		return error();
	}

	public DataResult<Consumer<Consumer<HashCode>>> getList(HashCode hashCode) {
		return error();
	}

	public DataResult<MapLike<HashCode>> getMap(HashCode hashCode) {
		return error();
	}

	public DataResult<ByteBuffer> getByteBuffer(HashCode hashCode) {
		return error();
	}

	public DataResult<IntStream> getIntStream(HashCode hashCode) {
		return error();
	}

	public DataResult<LongStream> getLongStream(HashCode hashCode) {
		return error();
	}

	/**
	 * {@code Builder}.
	 */
	final class Builder extends AbstractUniversalBuilder<HashCode, List<Pair<HashCode, HashCode>>> {

		public Builder() {
			super(HashCodeOps.this);
		}

		/**
		 * Инициализирует builder.
		 *
		 * @return List> — результат операции
		 */
		protected List<Pair<HashCode, HashCode>> initBuilder() {
			return new ArrayList<>();
		}

		protected List<Pair<HashCode, HashCode>> append(
				HashCode hashCode,
				HashCode hashCode2,
				List<Pair<HashCode, HashCode>> list
		) {
			list.add(Pair.of(hashCode, hashCode2));
			return list;
		}

		/**
		 * Build.
		 *
		 * @param list list
		 * @param hashCode hash code
		 *
		 * @return DataResult — результат операции
		 */
		protected DataResult<HashCode> build(List<Pair<HashCode, HashCode>> list, HashCode hashCode) {
			assert HashCodeOps.this.isEmpty(hashCode);

			return DataResult.success(HashCodeOps.hash(HashCodeOps.this.function.newHasher(), list.stream()).hash());
		}
	}

	/**
	 * {@code ListBuilder}.
	 */
	class ListBuilder extends AbstractListBuilder<HashCode, Hasher> {

		public ListBuilder() {
			super(HashCodeOps.this);
		}

		/**
		 * Инициализирует builder.
		 *
		 * @return Hasher — результат операции
		 */
		protected Hasher initBuilder() {
			return HashCodeOps.this.function.newHasher().putByte((byte) 4);
		}

		/**
		 * Add.
		 *
		 * @param hasher hasher
		 * @param hashCode hash code
		 *
		 * @return Hasher — результат операции
		 */
		protected Hasher add(Hasher hasher, HashCode hashCode) {
			return hasher.putBytes(hashCode.asBytes());
		}

		/**
		 * Build.
		 *
		 * @param hasher hasher
		 * @param hashCode hash code
		 *
		 * @return DataResult — результат операции
		 */
		protected DataResult<HashCode> build(Hasher hasher, HashCode hashCode) {
			assert hashCode.equals(HashCodeOps.this.empty);

			hasher.putByte((byte) 5);
			return DataResult.success(hasher.hash());
		}
	}
}
