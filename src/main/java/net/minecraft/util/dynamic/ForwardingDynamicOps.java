package net.minecraft.util.dynamic;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * {@code ForwardingDynamicOps}.
 */
public abstract class ForwardingDynamicOps<T> implements DynamicOps<T> {

	protected final DynamicOps<T> delegate;

	protected ForwardingDynamicOps(DynamicOps<T> delegate) {
		this.delegate = delegate;
	}

	/**
	 * Empty.
	 *
	 * @return T — результат операции
	 */
	public T empty() {
		return (T) this.delegate.empty();
	}

	/**
	 * Empty map.
	 *
	 * @return T — результат операции
	 */
	public T emptyMap() {
		return (T) this.delegate.emptyMap();
	}

	/**
	 * Empty list.
	 *
	 * @return T — результат операции
	 */
	public T emptyList() {
		return (T) this.delegate.emptyList();
	}

	/**
	 * Конвертирует to.
	 *
	 * @param outputOps output ops
	 * @param input input
	 *
	 * @return U — результат операции
	 */
	public <U> U convertTo(DynamicOps<U> outputOps, T input) {
		return (U) (Objects.equals(outputOps, this.delegate) ? input : this.delegate.convertTo(outputOps, input));
	}

	public DataResult<Number> getNumberValue(T input) {
		return this.delegate.getNumberValue(input);
	}

	/**
	 * Создаёт numeric.
	 *
	 * @param number number
	 *
	 * @return T — результат операции
	 */
	public T createNumeric(Number number) {
		return (T) this.delegate.createNumeric(number);
	}

	/**
	 * Создаёт byte.
	 *
	 * @param b b
	 *
	 * @return T — результат операции
	 */
	public T createByte(byte b) {
		return (T) this.delegate.createByte(b);
	}

	/**
	 * Создаёт short.
	 *
	 * @param s s
	 *
	 * @return T — результат операции
	 */
	public T createShort(short s) {
		return (T) this.delegate.createShort(s);
	}

	/**
	 * Создаёт int.
	 *
	 * @param i i
	 *
	 * @return T — результат операции
	 */
	public T createInt(int i) {
		return (T) this.delegate.createInt(i);
	}

	/**
	 * Создаёт long.
	 *
	 * @param l l
	 *
	 * @return T — результат операции
	 */
	public T createLong(long l) {
		return (T) this.delegate.createLong(l);
	}

	/**
	 * Создаёт float.
	 *
	 * @param f f
	 *
	 * @return T — результат операции
	 */
	public T createFloat(float f) {
		return (T) this.delegate.createFloat(f);
	}

	/**
	 * Создаёт double.
	 *
	 * @param d d
	 *
	 * @return T — результат операции
	 */
	public T createDouble(double d) {
		return (T) this.delegate.createDouble(d);
	}

	public DataResult<Boolean> getBooleanValue(T input) {
		return this.delegate.getBooleanValue(input);
	}

	/**
	 * Создаёт boolean.
	 *
	 * @param bl bl
	 *
	 * @return T — результат операции
	 */
	public T createBoolean(boolean bl) {
		return (T) this.delegate.createBoolean(bl);
	}

	public DataResult<String> getStringValue(T input) {
		return this.delegate.getStringValue(input);
	}

	/**
	 * Создаёт string.
	 *
	 * @param string string
	 *
	 * @return T — результат операции
	 */
	public T createString(String string) {
		return (T) this.delegate.createString(string);
	}

	/**
	 * Merge to list.
	 *
	 * @param list list
	 * @param value value
	 *
	 * @return DataResult — результат операции
	 */
	public DataResult<T> mergeToList(T list, T value) {
		return this.delegate.mergeToList(list, value);
	}

	/**
	 * Merge to list.
	 *
	 * @param list list
	 * @param values values
	 *
	 * @return DataResult — результат операции
	 */
	public DataResult<T> mergeToList(T list, List<T> values) {
		return this.delegate.mergeToList(list, values);
	}

	/**
	 * Merge to map.
	 *
	 * @param map map
	 * @param key key
	 * @param value value
	 *
	 * @return DataResult — результат операции
	 */
	public DataResult<T> mergeToMap(T map, T key, T value) {
		return this.delegate.mergeToMap(map, key, value);
	}

	/**
	 * Merge to map.
	 *
	 * @param map map
	 * @param values values
	 *
	 * @return DataResult — результат операции
	 */
	public DataResult<T> mergeToMap(T map, MapLike<T> values) {
		return this.delegate.mergeToMap(map, values);
	}

	/**
	 * Merge to map.
	 *
	 * @param map map
	 * @param values values
	 *
	 * @return DataResult — результат операции
	 */
	public DataResult<T> mergeToMap(T map, Map<T, T> values) {
		return this.delegate.mergeToMap(map, values);
	}

	/**
	 * Merge to primitive.
	 *
	 * @param prefix prefix
	 * @param value value
	 *
	 * @return DataResult — результат операции
	 */
	public DataResult<T> mergeToPrimitive(T prefix, T value) {
		return this.delegate.mergeToPrimitive(prefix, value);
	}

	public DataResult<Stream<Pair<T, T>>> getMapValues(T input) {
		return this.delegate.getMapValues(input);
	}

	public DataResult<Consumer<BiConsumer<T, T>>> getMapEntries(T input) {
		return this.delegate.getMapEntries(input);
	}

	/**
	 * Создаёт map.
	 *
	 * @param map map
	 *
	 * @return T — результат операции
	 */
	public T createMap(Map<T, T> map) {
		return (T) this.delegate.createMap(map);
	}

	/**
	 * Создаёт map.
	 *
	 * @param map map
	 *
	 * @return T — результат операции
	 */
	public T createMap(Stream<Pair<T, T>> map) {
		return (T) this.delegate.createMap(map);
	}

	public DataResult<MapLike<T>> getMap(T input) {
		return this.delegate.getMap(input);
	}

	public DataResult<Stream<T>> getStream(T input) {
		return this.delegate.getStream(input);
	}

	public DataResult<Consumer<Consumer<T>>> getList(T input) {
		return this.delegate.getList(input);
	}

	/**
	 * Создаёт list.
	 *
	 * @param stream stream
	 *
	 * @return T — результат операции
	 */
	public T createList(Stream<T> stream) {
		return (T) this.delegate.createList(stream);
	}

	public DataResult<ByteBuffer> getByteBuffer(T input) {
		return this.delegate.getByteBuffer(input);
	}

	/**
	 * Создаёт byte list.
	 *
	 * @param buf buf
	 *
	 * @return T — результат операции
	 */
	public T createByteList(ByteBuffer buf) {
		return (T) this.delegate.createByteList(buf);
	}

	public DataResult<IntStream> getIntStream(T input) {
		return this.delegate.getIntStream(input);
	}

	/**
	 * Создаёт int list.
	 *
	 * @param stream stream
	 *
	 * @return T — результат операции
	 */
	public T createIntList(IntStream stream) {
		return (T) this.delegate.createIntList(stream);
	}

	public DataResult<LongStream> getLongStream(T input) {
		return this.delegate.getLongStream(input);
	}

	/**
	 * Создаёт long list.
	 *
	 * @param stream stream
	 *
	 * @return T — результат операции
	 */
	public T createLongList(LongStream stream) {
		return (T) this.delegate.createLongList(stream);
	}

	/**
	 * Remove.
	 *
	 * @param input input
	 * @param key key
	 *
	 * @return T — результат операции
	 */
	public T remove(T input, String key) {
		return (T) this.delegate.remove(input, key);
	}

	/**
	 * Compress maps.
	 *
	 * @return boolean — результат операции
	 */
	public boolean compressMaps() {
		return this.delegate.compressMaps();
	}

	/**
	 * List builder.
	 *
	 * @return ListBuilder — результат операции
	 */
	public ListBuilder<T> listBuilder() {
		return new ForwardingDynamicOps.ForwardingListBuilder(this.delegate.listBuilder());
	}

	/**
	 * Map builder.
	 *
	 * @return RecordBuilder — результат операции
	 */
	public RecordBuilder<T> mapBuilder() {
		return new ForwardingDynamicOps.ForwardingRecordBuilder(this.delegate.mapBuilder());
	}

	/**
	 * {@code ForwardingListBuilder}.
	 */
	protected class ForwardingListBuilder implements ListBuilder<T> {

		private final ListBuilder<T> delegate;

		protected ForwardingListBuilder(final ListBuilder<T> delegate) {
			this.delegate = delegate;
		}

		/**
		 * Ops.
		 *
		 * @return DynamicOps — результат операции
		 */
		public DynamicOps<T> ops() {
			return ForwardingDynamicOps.this;
		}

		/**
		 * Build.
		 *
		 * @param prefix prefix
		 *
		 * @return DataResult — результат операции
		 */
		public DataResult<T> build(T prefix) {
			return this.delegate.build(prefix);
		}

		/**
		 * Add.
		 *
		 * @param value value
		 *
		 * @return ListBuilder — результат операции
		 */
		public ListBuilder<T> add(T value) {
			this.delegate.add(value);
			return this;
		}

		/**
		 * Add.
		 *
		 * @param value value
		 *
		 * @return ListBuilder — результат операции
		 */
		public ListBuilder<T> add(DataResult<T> value) {
			this.delegate.add(value);
			return this;
		}

		/**
		 * Add.
		 *
		 * @param value value
		 * @param encoder encoder
		 *
		 * @return ListBuilder — результат операции
		 */
		public <E> ListBuilder<T> add(E value, Encoder<E> encoder) {
			this.delegate.add(encoder.encodeStart(this.ops(), value));
			return this;
		}

		/**
		 * Добавляет all.
		 *
		 * @param values values
		 * @param encoder encoder
		 *
		 * @return ListBuilder — результат операции
		 */
		public <E> ListBuilder<T> addAll(Iterable<E> values, Encoder<E> encoder) {
			values.forEach(value -> this.delegate.add(encoder.encode(value, this.ops(), this.ops().empty())));
			return this;
		}

		/**
		 * With errors from.
		 *
		 * @param result result
		 *
		 * @return ListBuilder — результат операции
		 */
		public ListBuilder<T> withErrorsFrom(DataResult<?> result) {
			this.delegate.withErrorsFrom(result);
			return this;
		}

		/**
		 * Map error.
		 *
		 * @param onError on error
		 *
		 * @return ListBuilder — результат операции
		 */
		public ListBuilder<T> mapError(UnaryOperator<String> onError) {
			this.delegate.mapError(onError);
			return this;
		}

		/**
		 * Build.
		 *
		 * @param prefix prefix
		 *
		 * @return DataResult — результат операции
		 */
		public DataResult<T> build(DataResult<T> prefix) {
			return this.delegate.build(prefix);
		}
	}

	/**
	 * {@code ForwardingRecordBuilder}.
	 */
	protected class ForwardingRecordBuilder implements RecordBuilder<T> {

		private final RecordBuilder<T> delegate;

		protected ForwardingRecordBuilder(final RecordBuilder<T> delegate) {
			this.delegate = delegate;
		}

		/**
		 * Ops.
		 *
		 * @return DynamicOps — результат операции
		 */
		public DynamicOps<T> ops() {
			return ForwardingDynamicOps.this;
		}

		/**
		 * Add.
		 *
		 * @param key key
		 * @param value value
		 *
		 * @return RecordBuilder — результат операции
		 */
		public RecordBuilder<T> add(T key, T value) {
			this.delegate.add(key, value);
			return this;
		}

		/**
		 * Add.
		 *
		 * @param key key
		 * @param value value
		 *
		 * @return RecordBuilder — результат операции
		 */
		public RecordBuilder<T> add(T key, DataResult<T> value) {
			this.delegate.add(key, value);
			return this;
		}

		/**
		 * Add.
		 *
		 * @param key key
		 * @param value value
		 *
		 * @return RecordBuilder — результат операции
		 */
		public RecordBuilder<T> add(DataResult<T> key, DataResult<T> value) {
			this.delegate.add(key, value);
			return this;
		}

		/**
		 * Add.
		 *
		 * @param key key
		 * @param value value
		 *
		 * @return RecordBuilder — результат операции
		 */
		public RecordBuilder<T> add(String key, T value) {
			this.delegate.add(key, value);
			return this;
		}

		/**
		 * Add.
		 *
		 * @param key key
		 * @param value value
		 *
		 * @return RecordBuilder — результат операции
		 */
		public RecordBuilder<T> add(String key, DataResult<T> value) {
			this.delegate.add(key, value);
			return this;
		}

		/**
		 * Add.
		 *
		 * @param key key
		 * @param value value
		 * @param encoder encoder
		 *
		 * @return RecordBuilder — результат операции
		 */
		public <E> RecordBuilder<T> add(String key, E value, Encoder<E> encoder) {
			return this.delegate.add(key, encoder.encodeStart(this.ops(), value));
		}

		/**
		 * With errors from.
		 *
		 * @param result result
		 *
		 * @return RecordBuilder — результат операции
		 */
		public RecordBuilder<T> withErrorsFrom(DataResult<?> result) {
			this.delegate.withErrorsFrom(result);
			return this;
		}

		public RecordBuilder<T> setLifecycle(Lifecycle lifecycle) {
			this.delegate.setLifecycle(lifecycle);
			return this;
		}

		/**
		 * Map error.
		 *
		 * @param onError on error
		 *
		 * @return RecordBuilder — результат операции
		 */
		public RecordBuilder<T> mapError(UnaryOperator<String> onError) {
			this.delegate.mapError(onError);
			return this;
		}

		/**
		 * Build.
		 *
		 * @param prefix prefix
		 *
		 * @return DataResult — результат операции
		 */
		public DataResult<T> build(T prefix) {
			return this.delegate.build(prefix);
		}

		/**
		 * Build.
		 *
		 * @param prefix prefix
		 *
		 * @return DataResult — результат операции
		 */
		public DataResult<T> build(DataResult<T> prefix) {
			return this.delegate.build(prefix);
		}
	}
}
