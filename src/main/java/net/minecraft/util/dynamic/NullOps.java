package net.minecraft.util.dynamic;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.ListBuilder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.RecordBuilder.AbstractUniversalBuilder;
import net.minecraft.util.Unit;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * Реализация {@link DynamicOps} для типа {@link Unit}, которая игнорирует
 * все записываемые данные и возвращает пустые/нулевые значения при чтении.
 * Используется для валидации структуры кодека без реальной сериализации.
 */
public class NullOps implements DynamicOps<Unit> {

	public static final NullOps INSTANCE = new NullOps();

	private static final MapLike<Unit> EMPTY_MAP = new MapLike<>() {
		@Override
		public @Nullable Unit get(Unit unit) {
			return null;
		}

		@Override
		public @Nullable Unit get(String key) {
			return null;
		}

		@Override
		public Stream<Pair<Unit, Unit>> entries() {
			return Stream.empty();
		}
	};

	private NullOps() {
	}

	@Override
	public <U> U convertTo(DynamicOps<U> dynamicOps, Unit unit) {
		return dynamicOps.empty();
	}

	@Override
	public Unit empty() {
		return Unit.INSTANCE;
	}

	@Override
	public Unit emptyMap() {
		return Unit.INSTANCE;
	}

	@Override
	public Unit emptyList() {
		return Unit.INSTANCE;
	}

	@Override
	public Unit createNumeric(Number number) {
		return Unit.INSTANCE;
	}

	@Override
	public Unit createByte(byte b) {
		return Unit.INSTANCE;
	}

	@Override
	public Unit createShort(short s) {
		return Unit.INSTANCE;
	}

	@Override
	public Unit createInt(int i) {
		return Unit.INSTANCE;
	}

	@Override
	public Unit createLong(long l) {
		return Unit.INSTANCE;
	}

	@Override
	public Unit createFloat(float f) {
		return Unit.INSTANCE;
	}

	@Override
	public Unit createDouble(double d) {
		return Unit.INSTANCE;
	}

	@Override
	public Unit createBoolean(boolean value) {
		return Unit.INSTANCE;
	}

	@Override
	public Unit createString(String string) {
		return Unit.INSTANCE;
	}

	@Override
	public DataResult<Number> getNumberValue(Unit unit) {
		return DataResult.success(0);
	}

	@Override
	public DataResult<Boolean> getBooleanValue(Unit unit) {
		return DataResult.success(false);
	}

	@Override
	public DataResult<String> getStringValue(Unit unit) {
		return DataResult.success("");
	}

	@Override
	public DataResult<Unit> mergeToList(Unit unit, Unit value) {
		return DataResult.success(Unit.INSTANCE);
	}

	@Override
	public DataResult<Unit> mergeToList(Unit unit, List<Unit> list) {
		return DataResult.success(Unit.INSTANCE);
	}

	@Override
	public DataResult<Unit> mergeToMap(Unit unit, Unit key, Unit value) {
		return DataResult.success(Unit.INSTANCE);
	}

	@Override
	public DataResult<Unit> mergeToMap(Unit unit, Map<Unit, Unit> map) {
		return DataResult.success(Unit.INSTANCE);
	}

	@Override
	public DataResult<Unit> mergeToMap(Unit unit, MapLike<Unit> mapLike) {
		return DataResult.success(Unit.INSTANCE);
	}

	@Override
	public DataResult<Stream<Pair<Unit, Unit>>> getMapValues(Unit unit) {
		return DataResult.success(Stream.empty());
	}

	@Override
	public DataResult<Consumer<BiConsumer<Unit, Unit>>> getMapEntries(Unit unit) {
		return DataResult.success(biConsumer -> {});
	}

	@Override
	public DataResult<MapLike<Unit>> getMap(Unit unit) {
		return DataResult.success(EMPTY_MAP);
	}

	@Override
	public DataResult<Stream<Unit>> getStream(Unit unit) {
		return DataResult.success(Stream.empty());
	}

	@Override
	public DataResult<Consumer<Consumer<Unit>>> getList(Unit unit) {
		return DataResult.success(consumer -> {});
	}

	@Override
	public DataResult<ByteBuffer> getByteBuffer(Unit unit) {
		return DataResult.success(ByteBuffer.wrap(new byte[0]));
	}

	@Override
	public DataResult<IntStream> getIntStream(Unit unit) {
		return DataResult.success(IntStream.empty());
	}

	@Override
	public DataResult<LongStream> getLongStream(Unit unit) {
		return DataResult.success(LongStream.empty());
	}

	@Override
	public Unit createMap(Stream<Pair<Unit, Unit>> stream) {
		return Unit.INSTANCE;
	}

	@Override
	public Unit createMap(Map<Unit, Unit> map) {
		return Unit.INSTANCE;
	}

	@Override
	public Unit createList(Stream<Unit> stream) {
		return Unit.INSTANCE;
	}

	@Override
	public Unit createByteList(ByteBuffer byteBuffer) {
		return Unit.INSTANCE;
	}

	@Override
	public Unit createIntList(IntStream intStream) {
		return Unit.INSTANCE;
	}

	@Override
	public Unit createLongList(LongStream longStream) {
		return Unit.INSTANCE;
	}

	@Override
	public Unit remove(Unit unit, String key) {
		return unit;
	}

	@Override
	public RecordBuilder<Unit> mapBuilder() {
		return new NullMapBuilder(this);
	}

	@Override
	public ListBuilder<Unit> listBuilder() {
		return new NullListBuilder(this);
	}

	@Override
	public String toString() {
		return "Null";
	}

	static final class NullListBuilder extends AbstractListBuilder<Unit, Unit> {

		public NullListBuilder(DynamicOps<Unit> dynamicOps) {
			super(dynamicOps);
		}

		@Override
		protected Unit initBuilder() {
			return Unit.INSTANCE;
		}

		@Override
		protected Unit add(Unit accumulator, Unit value) {
			return accumulator;
		}

		@Override
		protected DataResult<Unit> build(Unit accumulator, Unit prefix) {
			return DataResult.success(accumulator);
		}
	}

	static final class NullMapBuilder extends AbstractUniversalBuilder<Unit, Unit> {

		public NullMapBuilder(DynamicOps<Unit> ops) {
			super(ops);
		}

		@Override
		protected Unit initBuilder() {
			return Unit.INSTANCE;
		}

		@Override
		protected Unit append(Unit key, Unit value, Unit accumulator) {
			return accumulator;
		}

		@Override
		protected DataResult<Unit> build(Unit accumulator, Unit prefix) {
			return DataResult.success(prefix);
		}
	}
}
