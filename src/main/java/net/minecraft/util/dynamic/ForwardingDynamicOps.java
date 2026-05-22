package net.minecraft.util.dynamic;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.ListBuilder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

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
 * Базовая реализация {@link DynamicOps}, делегирующая все операции
 * обёрнутому экземпляру. Подклассы переопределяют только нужные методы,
 * сохраняя прозрачность для остальных.
 */
public abstract class ForwardingDynamicOps<T> implements DynamicOps<T> {

	protected final DynamicOps<T> delegate;

	protected ForwardingDynamicOps(DynamicOps<T> delegate) {
		this.delegate = delegate;
	}

	@Override
	public T empty() {
		return delegate.empty();
	}

	@Override
	public T emptyMap() {
		return delegate.emptyMap();
	}

	@Override
	public T emptyList() {
		return delegate.emptyList();
	}

	@Override
	public <U> U convertTo(DynamicOps<U> outputOps, T input) {
		return Objects.equals(outputOps, delegate)
				? (U) input
				: delegate.convertTo(outputOps, input);
	}

	@Override
	public DataResult<Number> getNumberValue(T input) {
		return delegate.getNumberValue(input);
	}

	@Override
	public T createNumeric(Number number) {
		return delegate.createNumeric(number);
	}

	@Override
	public T createByte(byte b) {
		return delegate.createByte(b);
	}

	@Override
	public T createShort(short s) {
		return delegate.createShort(s);
	}

	@Override
	public T createInt(int i) {
		return delegate.createInt(i);
	}

	@Override
	public T createLong(long l) {
		return delegate.createLong(l);
	}

	@Override
	public T createFloat(float f) {
		return delegate.createFloat(f);
	}

	@Override
	public T createDouble(double d) {
		return delegate.createDouble(d);
	}

	@Override
	public DataResult<Boolean> getBooleanValue(T input) {
		return delegate.getBooleanValue(input);
	}

	@Override
	public T createBoolean(boolean value) {
		return delegate.createBoolean(value);
	}

	@Override
	public DataResult<String> getStringValue(T input) {
		return delegate.getStringValue(input);
	}

	@Override
	public T createString(String string) {
		return delegate.createString(string);
	}

	@Override
	public DataResult<T> mergeToList(T list, T value) {
		return delegate.mergeToList(list, value);
	}

	@Override
	public DataResult<T> mergeToList(T list, List<T> values) {
		return delegate.mergeToList(list, values);
	}

	@Override
	public DataResult<T> mergeToMap(T map, T key, T value) {
		return delegate.mergeToMap(map, key, value);
	}

	@Override
	public DataResult<T> mergeToMap(T map, MapLike<T> values) {
		return delegate.mergeToMap(map, values);
	}

	@Override
	public DataResult<T> mergeToMap(T map, Map<T, T> values) {
		return delegate.mergeToMap(map, values);
	}

	@Override
	public DataResult<T> mergeToPrimitive(T prefix, T value) {
		return delegate.mergeToPrimitive(prefix, value);
	}

	@Override
	public DataResult<Stream<Pair<T, T>>> getMapValues(T input) {
		return delegate.getMapValues(input);
	}

	@Override
	public DataResult<Consumer<BiConsumer<T, T>>> getMapEntries(T input) {
		return delegate.getMapEntries(input);
	}

	@Override
	public T createMap(Map<T, T> map) {
		return delegate.createMap(map);
	}

	@Override
	public T createMap(Stream<Pair<T, T>> map) {
		return delegate.createMap(map);
	}

	@Override
	public DataResult<MapLike<T>> getMap(T input) {
		return delegate.getMap(input);
	}

	@Override
	public DataResult<Stream<T>> getStream(T input) {
		return delegate.getStream(input);
	}

	@Override
	public DataResult<Consumer<Consumer<T>>> getList(T input) {
		return delegate.getList(input);
	}

	@Override
	public T createList(Stream<T> stream) {
		return delegate.createList(stream);
	}

	@Override
	public DataResult<ByteBuffer> getByteBuffer(T input) {
		return delegate.getByteBuffer(input);
	}

	@Override
	public T createByteList(ByteBuffer buf) {
		return delegate.createByteList(buf);
	}

	@Override
	public DataResult<IntStream> getIntStream(T input) {
		return delegate.getIntStream(input);
	}

	@Override
	public T createIntList(IntStream stream) {
		return delegate.createIntList(stream);
	}

	@Override
	public DataResult<LongStream> getLongStream(T input) {
		return delegate.getLongStream(input);
	}

	@Override
	public T createLongList(LongStream stream) {
		return delegate.createLongList(stream);
	}

	@Override
	public T remove(T input, String key) {
		return delegate.remove(input, key);
	}

	@Override
	public boolean compressMaps() {
		return delegate.compressMaps();
	}

	@Override
	public ListBuilder<T> listBuilder() {
		return new ForwardingListBuilder(delegate.listBuilder());
	}

	@Override
	public RecordBuilder<T> mapBuilder() {
		return new ForwardingRecordBuilder(delegate.mapBuilder());
	}

	/**
	 * Делегирующий {@link ListBuilder}, перенаправляющий все вызовы
	 * во внутренний builder, но возвращающий {@code this} для корректного chaining.
	 */
	protected class ForwardingListBuilder implements ListBuilder<T> {

		private final ListBuilder<T> delegate;

		protected ForwardingListBuilder(ListBuilder<T> delegate) {
			this.delegate = delegate;
		}

		@Override
		public DynamicOps<T> ops() {
			return ForwardingDynamicOps.this;
		}

		@Override
		public DataResult<T> build(T prefix) {
			return delegate.build(prefix);
		}

		@Override
		public DataResult<T> build(DataResult<T> prefix) {
			return delegate.build(prefix);
		}

		@Override
		public ListBuilder<T> add(T value) {
			delegate.add(value);
			return this;
		}

		@Override
		public ListBuilder<T> add(DataResult<T> value) {
			delegate.add(value);
			return this;
		}

		@Override
		public <E> ListBuilder<T> add(E value, Encoder<E> encoder) {
			delegate.add(encoder.encodeStart(ops(), value));
			return this;
		}

		@Override
		public <E> ListBuilder<T> addAll(Iterable<E> values, Encoder<E> encoder) {
			values.forEach(value -> delegate.add(encoder.encode(value, ops(), ops().empty())));
			return this;
		}

		@Override
		public ListBuilder<T> withErrorsFrom(DataResult<?> result) {
			delegate.withErrorsFrom(result);
			return this;
		}

		@Override
		public ListBuilder<T> mapError(UnaryOperator<String> onError) {
			delegate.mapError(onError);
			return this;
		}
	}

	/**
	 * Делегирующий {@link RecordBuilder}, перенаправляющий все вызовы
	 * во внутренний builder, но возвращающий {@code this} для корректного chaining.
	 */
	protected class ForwardingRecordBuilder implements RecordBuilder<T> {

		private final RecordBuilder<T> delegate;

		protected ForwardingRecordBuilder(RecordBuilder<T> delegate) {
			this.delegate = delegate;
		}

		@Override
		public DynamicOps<T> ops() {
			return ForwardingDynamicOps.this;
		}

		@Override
		public RecordBuilder<T> add(T key, T value) {
			delegate.add(key, value);
			return this;
		}

		@Override
		public RecordBuilder<T> add(T key, DataResult<T> value) {
			delegate.add(key, value);
			return this;
		}

		@Override
		public RecordBuilder<T> add(DataResult<T> key, DataResult<T> value) {
			delegate.add(key, value);
			return this;
		}

		@Override
		public RecordBuilder<T> add(String key, T value) {
			delegate.add(key, value);
			return this;
		}

		@Override
		public RecordBuilder<T> add(String key, DataResult<T> value) {
			delegate.add(key, value);
			return this;
		}

		@Override
		public <E> RecordBuilder<T> add(String key, E value, Encoder<E> encoder) {
			return delegate.add(key, encoder.encodeStart(ops(), value));
		}

		@Override
		public RecordBuilder<T> withErrorsFrom(DataResult<?> result) {
			delegate.withErrorsFrom(result);
			return this;
		}

		@Override
		public RecordBuilder<T> setLifecycle(Lifecycle lifecycle) {
			delegate.setLifecycle(lifecycle);
			return this;
		}

		@Override
		public RecordBuilder<T> mapError(UnaryOperator<String> onError) {
			delegate.mapError(onError);
			return this;
		}

		@Override
		public DataResult<T> build(T prefix) {
			return delegate.build(prefix);
		}

		@Override
		public DataResult<T> build(DataResult<T> prefix) {
			return delegate.build(prefix);
		}
	}
}
