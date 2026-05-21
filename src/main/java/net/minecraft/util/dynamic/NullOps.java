package net.minecraft.util.dynamic;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
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
 * {@code NullOps}.
 */
public class NullOps implements DynamicOps<Unit> {

	public static final NullOps INSTANCE = new NullOps();
	private static final MapLike<Unit> EMPTY_MAP = new MapLike<Unit>() {
		/**
		 * Get.
		 *
		 * @param unit unit
		 *
		 * @return @Nullable Unit — 
		 */
		public @Nullable Unit get(Unit unit) {
			return null;
		}

		/**
		 * Get.
		 *
		 * @param string string
		 *
		 * @return @Nullable Unit — 
		 */
		public @Nullable Unit get(String string) {
			return null;
		}

		/**
		 * Entries.
		 *
		 * @return Stream> — результат операции
		 */
		public Stream<Pair<Unit, Unit>> entries() {
			return Stream.empty();
		}
	};

	private NullOps() {
	}

	/**
	 * Конвертирует to.
	 *
	 * @param dynamicOps dynamic ops
	 * @param unit unit
	 *
	 * @return U — результат операции
	 */
	public <U> U convertTo(DynamicOps<U> dynamicOps, Unit unit) {
		return (U) dynamicOps.empty();
	}

	/**
	 * Empty.
	 *
	 * @return Unit — результат операции
	 */
	public Unit empty() {
		return Unit.INSTANCE;
	}

	/**
	 * Empty map.
	 *
	 * @return Unit — результат операции
	 */
	public Unit emptyMap() {
		return Unit.INSTANCE;
	}

	/**
	 * Empty list.
	 *
	 * @return Unit — результат операции
	 */
	public Unit emptyList() {
		return Unit.INSTANCE;
	}

	/**
	 * Создаёт numeric.
	 *
	 * @param number number
	 *
	 * @return Unit — результат операции
	 */
	public Unit createNumeric(Number number) {
		return Unit.INSTANCE;
	}

	/**
	 * Создаёт byte.
	 *
	 * @param b b
	 *
	 * @return Unit — результат операции
	 */
	public Unit createByte(byte b) {
		return Unit.INSTANCE;
	}

	/**
	 * Создаёт short.
	 *
	 * @param s s
	 *
	 * @return Unit — результат операции
	 */
	public Unit createShort(short s) {
		return Unit.INSTANCE;
	}

	/**
	 * Создаёт int.
	 *
	 * @param i i
	 *
	 * @return Unit — результат операции
	 */
	public Unit createInt(int i) {
		return Unit.INSTANCE;
	}

	/**
	 * Создаёт long.
	 *
	 * @param l l
	 *
	 * @return Unit — результат операции
	 */
	public Unit createLong(long l) {
		return Unit.INSTANCE;
	}

	/**
	 * Создаёт float.
	 *
	 * @param f f
	 *
	 * @return Unit — результат операции
	 */
	public Unit createFloat(float f) {
		return Unit.INSTANCE;
	}

	/**
	 * Создаёт double.
	 *
	 * @param d d
	 *
	 * @return Unit — результат операции
	 */
	public Unit createDouble(double d) {
		return Unit.INSTANCE;
	}

	/**
	 * Создаёт boolean.
	 *
	 * @param bl bl
	 *
	 * @return Unit — результат операции
	 */
	public Unit createBoolean(boolean bl) {
		return Unit.INSTANCE;
	}

	/**
	 * Создаёт string.
	 *
	 * @param string string
	 *
	 * @return Unit — результат операции
	 */
	public Unit createString(String string) {
		return Unit.INSTANCE;
	}

	public DataResult<Number> getNumberValue(Unit unit) {
		return DataResult.success(0);
	}

	public DataResult<Boolean> getBooleanValue(Unit unit) {
		return DataResult.success(false);
	}

	public DataResult<String> getStringValue(Unit unit) {
		return DataResult.success("");
	}

	/**
	 * Merge to list.
	 *
	 * @param unit unit
	 * @param unit2 unit2
	 *
	 * @return DataResult — результат операции
	 */
	public DataResult<Unit> mergeToList(Unit unit, Unit unit2) {
		return DataResult.success(Unit.INSTANCE);
	}

	/**
	 * Merge to list.
	 *
	 * @param unit unit
	 * @param list list
	 *
	 * @return DataResult — результат операции
	 */
	public DataResult<Unit> mergeToList(Unit unit, List<Unit> list) {
		return DataResult.success(Unit.INSTANCE);
	}

	/**
	 * Merge to map.
	 *
	 * @param unit unit
	 * @param unit2 unit2
	 * @param unit3 unit3
	 *
	 * @return DataResult — результат операции
	 */
	public DataResult<Unit> mergeToMap(Unit unit, Unit unit2, Unit unit3) {
		return DataResult.success(Unit.INSTANCE);
	}

	/**
	 * Merge to map.
	 *
	 * @param unit unit
	 * @param map map
	 *
	 * @return DataResult — результат операции
	 */
	public DataResult<Unit> mergeToMap(Unit unit, Map<Unit, Unit> map) {
		return DataResult.success(Unit.INSTANCE);
	}

	/**
	 * Merge to map.
	 *
	 * @param unit unit
	 * @param mapLike map like
	 *
	 * @return DataResult — результат операции
	 */
	public DataResult<Unit> mergeToMap(Unit unit, MapLike<Unit> mapLike) {
		return DataResult.success(Unit.INSTANCE);
	}

	public DataResult<Stream<Pair<Unit, Unit>>> getMapValues(Unit unit) {
		return DataResult.success(Stream.empty());
	}

	public DataResult<Consumer<BiConsumer<Unit, Unit>>> getMapEntries(Unit unit) {
		return DataResult.success(biConsumer -> {});
	}

	public DataResult<MapLike<Unit>> getMap(Unit unit) {
		return DataResult.success(EMPTY_MAP);
	}

	public DataResult<Stream<Unit>> getStream(Unit unit) {
		return DataResult.success(Stream.empty());
	}

	public DataResult<Consumer<Consumer<Unit>>> getList(Unit unit) {
		return DataResult.success(consumer -> {});
	}

	public DataResult<ByteBuffer> getByteBuffer(Unit unit) {
		return DataResult.success(ByteBuffer.wrap(new byte[0]));
	}

	public DataResult<IntStream> getIntStream(Unit unit) {
		return DataResult.success(IntStream.empty());
	}

	public DataResult<LongStream> getLongStream(Unit unit) {
		return DataResult.success(LongStream.empty());
	}

	/**
	 * Создаёт map.
	 *
	 * @param stream stream
	 *
	 * @return Unit — результат операции
	 */
	public Unit createMap(Stream<Pair<Unit, Unit>> stream) {
		return Unit.INSTANCE;
	}

	/**
	 * Создаёт map.
	 *
	 * @param map map
	 *
	 * @return Unit — результат операции
	 */
	public Unit createMap(Map<Unit, Unit> map) {
		return Unit.INSTANCE;
	}

	/**
	 * Создаёт list.
	 *
	 * @param stream stream
	 *
	 * @return Unit — результат операции
	 */
	public Unit createList(Stream<Unit> stream) {
		return Unit.INSTANCE;
	}

	/**
	 * Создаёт byte list.
	 *
	 * @param byteBuffer byte buffer
	 *
	 * @return Unit — результат операции
	 */
	public Unit createByteList(ByteBuffer byteBuffer) {
		return Unit.INSTANCE;
	}

	/**
	 * Создаёт int list.
	 *
	 * @param intStream int stream
	 *
	 * @return Unit — результат операции
	 */
	public Unit createIntList(IntStream intStream) {
		return Unit.INSTANCE;
	}

	/**
	 * Создаёт long list.
	 *
	 * @param longStream long stream
	 *
	 * @return Unit — результат операции
	 */
	public Unit createLongList(LongStream longStream) {
		return Unit.INSTANCE;
	}

	/**
	 * Remove.
	 *
	 * @param unit unit
	 * @param string string
	 *
	 * @return Unit — результат операции
	 */
	public Unit remove(Unit unit, String string) {
		return unit;
	}

	/**
	 * Map builder.
	 *
	 * @return RecordBuilder — результат операции
	 */
	public RecordBuilder<Unit> mapBuilder() {
		return new NullOps.NullMapBuilder(this);
	}

	/**
	 * List builder.
	 *
	 * @return ListBuilder — результат операции
	 */
	public ListBuilder<Unit> listBuilder() {
		return new NullOps.NullListBuilder(this);
	}

	@Override
	public String toString() {
		return "Null";
	}

	/**
	 * {@code NullListBuilder}.
	 */
	static final class NullListBuilder extends AbstractListBuilder<Unit, Unit> {

		public NullListBuilder(DynamicOps<Unit> dynamicOps) {
			super(dynamicOps);
		}

		/**
		 * Инициализирует builder.
		 *
		 * @return Unit — результат операции
		 */
		protected Unit initBuilder() {
			return Unit.INSTANCE;
		}

		/**
		 * Add.
		 *
		 * @param unit unit
		 * @param unit2 unit2
		 *
		 * @return Unit — результат операции
		 */
		protected Unit add(Unit unit, Unit unit2) {
			return unit;
		}

		/**
		 * Build.
		 *
		 * @param unit unit
		 * @param unit2 unit2
		 *
		 * @return DataResult — результат операции
		 */
		protected DataResult<Unit> build(Unit unit, Unit unit2) {
			return DataResult.success(unit);
		}
	}

	/**
	 * {@code NullMapBuilder}.
	 */
	static final class NullMapBuilder extends AbstractUniversalBuilder<Unit, Unit> {

		public NullMapBuilder(DynamicOps<Unit> ops) {
			super(ops);
		}

		/**
		 * Инициализирует builder.
		 *
		 * @return Unit — результат операции
		 */
		protected Unit initBuilder() {
			return Unit.INSTANCE;
		}

		/**
		 * Append.
		 *
		 * @param unit unit
		 * @param unit2 unit2
		 * @param unit3 unit3
		 *
		 * @return Unit — результат операции
		 */
		protected Unit append(Unit unit, Unit unit2, Unit unit3) {
			return unit3;
		}

		/**
		 * Build.
		 *
		 * @param unit unit
		 * @param unit2 unit2
		 *
		 * @return DataResult — результат операции
		 */
		protected DataResult<Unit> build(Unit unit, Unit unit2) {
			return DataResult.success(unit2);
		}
	}
}
