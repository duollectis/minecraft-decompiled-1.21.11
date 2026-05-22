package net.minecraft.storage;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Streams;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult.Error;
import com.mojang.serialization.DataResult.Success;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.*;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.ErrorReporter;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

/**
 * Реализация {@link ReadView} поверх {@link NbtCompound}.
 * <p>
 * Читает поля из NBT-тега, декодируя их через кодеки DFU. При ошибках
 * декодирования или несоответствии типов сообщает об этом через
 * {@link ErrorReporter}, не бросая исключений. Поддерживает вложенные
 * объекты и списки с автоматическим созданием дочерних контекстов ошибок.
 */
public class NbtReadView implements ReadView {

	private final ErrorReporter reporter;
	private final ReadContext context;
	private final NbtCompound nbt;

	private NbtReadView(ErrorReporter reporter, ReadContext context, NbtCompound nbt) {
		this.reporter = reporter;
		this.context = context;
		this.nbt = nbt;
	}

	/**
	 * Создаёт корневое представление для чтения из {@link NbtCompound}.
	 *
	 * @param reporter   получатель ошибок декодирования
	 * @param registries реестры для разрешения ссылок
	 * @param nbt        исходный NBT-тег
	 * @return новый {@link ReadView}
	 */
	public static ReadView create(
			ErrorReporter reporter,
			RegistryWrapper.WrapperLookup registries,
			NbtCompound nbt
	) {
		return new NbtReadView(reporter, new ReadContext(registries, NbtOps.INSTANCE), nbt);
	}

	/**
	 * Создаёт корневое представление для чтения из списка {@link NbtCompound}.
	 *
	 * @param reporter   получатель ошибок декодирования
	 * @param registries реестры для разрешения ссылок
	 * @param elements   список NBT-тегов
	 * @return новый {@link ReadView.ListReadView}
	 */
	public static ReadView.ListReadView createList(
			ErrorReporter reporter,
			RegistryWrapper.WrapperLookup registries,
			List<NbtCompound> elements
	) {
		return new NbtListReadView(reporter, new ReadContext(registries, NbtOps.INSTANCE), elements);
	}

	@Override
	public <T> Optional<T> read(String key, Codec<T> codec) {
		NbtElement element = nbt.get(key);

		if (element == null) {
			return Optional.empty();
		}

		return switch (codec.parse(context.getOps(), element)) {
			case Success<T> success -> Optional.of(success.value());
			case Error<T> error -> {
				reporter.report(new DecodeError(key, element, error));
				yield error.partialValue();
			}
			default -> throw new MatchException(null, null);
		};
	}

	@Override
	public <T> Optional<T> read(MapCodec<T> mapCodec) {
		DynamicOps<NbtElement> ops = context.getOps();

		return switch (ops.getMap(nbt).flatMap(map -> mapCodec.decode(ops, map))) {
			case Success<T> success -> Optional.of(success.value());
			case Error<T> error -> {
				reporter.report(new DecodeMapError(error));
				yield error.partialValue();
			}
			default -> throw new MatchException(null, null);
		};
	}

	/**
	 * Извлекает NBT-элемент по ключу и проверяет соответствие ожидаемому типу.
	 * При несоответствии типа сообщает об ошибке и возвращает {@code null}.
	 */
	@SuppressWarnings("unchecked")
	private <T extends NbtElement> @Nullable T get(String key, NbtType<T> expectedType) {
		NbtElement element = nbt.get(key);

		if (element == null) {
			return null;
		}

		NbtType<?> actualType = element.getNbtType();

		if (actualType != expectedType) {
			reporter.report(new ExpectedTypeError(key, expectedType, actualType));
			return null;
		}

		return (T) element;
	}

	/**
	 * Извлекает числовой NBT-элемент по ключу.
	 * При отсутствии числового типа сообщает об ошибке и возвращает {@code null}.
	 */
	private @Nullable AbstractNbtNumber getNumber(String key) {
		NbtElement element = nbt.get(key);

		if (element == null) {
			return null;
		}

		if (element instanceof AbstractNbtNumber number) {
			return number;
		}

		reporter.report(new ExpectedNumberError(key, element.getNbtType()));
		return null;
	}

	@Override
	public Optional<ReadView> getOptionalReadView(String key) {
		NbtCompound compound = get(key, NbtCompound.TYPE);
		return compound != null
				? Optional.of(createChildReadView(key, compound))
				: Optional.empty();
	}

	@Override
	public ReadView getReadView(String key) {
		NbtCompound compound = get(key, NbtCompound.TYPE);
		return compound != null
				? createChildReadView(key, compound)
				: context.getEmptyReadView();
	}

	@Override
	public Optional<ReadView.ListReadView> getOptionalListReadView(String key) {
		NbtList list = get(key, NbtList.TYPE);
		return list != null
				? Optional.of(createChildListReadView(key, context, list))
				: Optional.empty();
	}

	@Override
	public ReadView.ListReadView getListReadView(String key) {
		NbtList list = get(key, NbtList.TYPE);
		return list != null
				? createChildListReadView(key, context, list)
				: context.getEmptyListReadView();
	}

	@Override
	public <T> Optional<ReadView.TypedListReadView<T>> getOptionalTypedListView(String key, Codec<T> typeCodec) {
		NbtList list = get(key, NbtList.TYPE);
		return list != null
				? Optional.of(createTypedListReadView(key, list, typeCodec))
				: Optional.empty();
	}

	@Override
	public <T> ReadView.TypedListReadView<T> getTypedListView(String key, Codec<T> typeCodec) {
		NbtList list = get(key, NbtList.TYPE);
		return list != null
				? createTypedListReadView(key, list, typeCodec)
				: context.getEmptyTypedListReadView();
	}

	@Override
	public boolean getBoolean(String key, boolean fallback) {
		AbstractNbtNumber number = getNumber(key);
		return number != null ? number.byteValue() != 0 : fallback;
	}

	@Override
	public byte getByte(String key, byte fallback) {
		AbstractNbtNumber number = getNumber(key);
		return number != null ? number.byteValue() : fallback;
	}

	@Override
	public int getShort(String key, short fallback) {
		AbstractNbtNumber number = getNumber(key);
		return number != null ? number.shortValue() : fallback;
	}

	@Override
	public Optional<Integer> getOptionalInt(String key) {
		AbstractNbtNumber number = getNumber(key);
		return number != null ? Optional.of(number.intValue()) : Optional.empty();
	}

	@Override
	public int getInt(String key, int fallback) {
		AbstractNbtNumber number = getNumber(key);
		return number != null ? number.intValue() : fallback;
	}

	@Override
	public long getLong(String key, long fallback) {
		AbstractNbtNumber number = getNumber(key);
		return number != null ? number.longValue() : fallback;
	}

	@Override
	public Optional<Long> getOptionalLong(String key) {
		AbstractNbtNumber number = getNumber(key);
		return number != null ? Optional.of(number.longValue()) : Optional.empty();
	}

	@Override
	public float getFloat(String key, float fallback) {
		AbstractNbtNumber number = getNumber(key);
		return number != null ? number.floatValue() : fallback;
	}

	@Override
	public double getDouble(String key, double fallback) {
		AbstractNbtNumber number = getNumber(key);
		return number != null ? number.doubleValue() : fallback;
	}

	@Override
	public Optional<String> getOptionalString(String key) {
		NbtString string = get(key, NbtString.TYPE);
		return string != null ? Optional.of(string.value()) : Optional.empty();
	}

	@Override
	public String getString(String key, String fallback) {
		NbtString string = get(key, NbtString.TYPE);
		return string != null ? string.value() : fallback;
	}

	@Override
	public Optional<int[]> getOptionalIntArray(String key) {
		NbtIntArray array = get(key, NbtIntArray.TYPE);
		return array != null ? Optional.of(array.getIntArray()) : Optional.empty();
	}

	@Override
	public RegistryWrapper.WrapperLookup getRegistries() {
		return context.getRegistries();
	}

	private ReadView createChildReadView(String key, NbtCompound compound) {
		if (compound.isEmpty()) {
			return context.getEmptyReadView();
		}

		return new NbtReadView(
				reporter.makeChild(new ErrorReporter.MapElementContext(key)),
				context,
				compound
		);
	}

	static ReadView createReadView(ErrorReporter reporter, ReadContext context, NbtCompound nbt) {
		return nbt.isEmpty()
				? context.getEmptyReadView()
				: new NbtReadView(reporter, context, nbt);
	}

	private ReadView.ListReadView createChildListReadView(String key, ReadContext ctx, NbtList list) {
		if (list.isEmpty()) {
			return ctx.getEmptyListReadView();
		}

		return new ChildListReadView(reporter, key, ctx, list);
	}

	private <T> ReadView.TypedListReadView<T> createTypedListReadView(
			String key,
			NbtList list,
			Codec<T> typeCodec
	) {
		if (list.isEmpty()) {
			return context.getEmptyTypedListReadView();
		}

		return new NbtTypedListReadView<>(reporter, key, context, typeCodec, list);
	}

	// -------------------------------------------------------------------------
	// Вложенные классы
	// -------------------------------------------------------------------------

	/**
	 * Представление списка вложенных NBT-объектов, привязанное к именованному полю.
	 * Используется при чтении {@link NbtList} из поля {@link NbtCompound}.
	 */
	static class ChildListReadView implements ReadView.ListReadView {

		private final ErrorReporter reporter;
		private final String name;
		final ReadContext context;
		private final NbtList list;

		ChildListReadView(ErrorReporter reporter, String name, ReadContext context, NbtList list) {
			this.reporter = reporter;
			this.name = name;
			this.context = context;
			this.list = list;
		}

		@Override
		public boolean isEmpty() {
			return list.isEmpty();
		}

		ErrorReporter createErrorReporter(int index) {
			return reporter.makeChild(new ErrorReporter.NamedListElementContext(name, index));
		}

		void reportExpectedTypeAtIndexError(int index, NbtElement element) {
			reporter.report(new ExpectedTypeAtIndexError(
					name,
					index,
					NbtCompound.TYPE,
					element.getNbtType()
			));
		}

		@Override
		public Stream<ReadView> stream() {
			return Streams.mapWithIndex(
					list.stream(),
					(element, rawIndex) -> {
						int index = (int) rawIndex;

						if (element instanceof NbtCompound compound) {
							return NbtReadView.createReadView(createErrorReporter(index), context, compound);
						}

						reportExpectedTypeAtIndexError(index, element);
						return null;
					}
			).filter(Objects::nonNull);
		}

		@Override
		public Iterator<ReadView> iterator() {
			Iterator<NbtElement> elementIterator = list.iterator();

			return new AbstractIterator<>() {
				private int index;

				@Override
				protected @Nullable ReadView computeNext() {
					while (elementIterator.hasNext()) {
						NbtElement element = elementIterator.next();
						int currentIndex = index++;

						if (element instanceof NbtCompound compound) {
							return NbtReadView.createReadView(
									createErrorReporter(currentIndex),
									context,
									compound
							);
						}

						reportExpectedTypeAtIndexError(currentIndex, element);
					}

					return endOfData();
				}
			};
		}
	}

	/**
	 * Представление списка {@link NbtCompound}, созданного напрямую из {@link java.util.List}.
	 * Используется при создании через {@link NbtReadView#createList}.
	 */
	static class NbtListReadView implements ReadView.ListReadView {

		private final ErrorReporter reporter;
		private final ReadContext context;
		private final List<NbtCompound> nbts;

		NbtListReadView(ErrorReporter reporter, ReadContext context, List<NbtCompound> nbts) {
			this.reporter = reporter;
			this.context = context;
			this.nbts = nbts;
		}

		ReadView createReadView(int index, NbtCompound nbt) {
			return NbtReadView.createReadView(
					reporter.makeChild(new ErrorReporter.ListElementContext(index)),
					context,
					nbt
			);
		}

		@Override
		public boolean isEmpty() {
			return nbts.isEmpty();
		}

		@Override
		public Stream<ReadView> stream() {
			return Streams.mapWithIndex(
					nbts.stream(),
					(nbt, rawIndex) -> createReadView((int) rawIndex, nbt)
			);
		}

		@Override
		public Iterator<ReadView> iterator() {
			ListIterator<NbtCompound> listIterator = nbts.listIterator();

			return new AbstractIterator<>() {
				@Override
				protected @Nullable ReadView computeNext() {
					if (listIterator.hasNext()) {
						int index = listIterator.nextIndex();
						NbtCompound compound = listIterator.next();
						return NbtListReadView.this.createReadView(index, compound);
					}

					return endOfData();
				}
			};
		}
	}

	/**
	 * Типизированное представление {@link NbtList}, элементы которого декодируются через кодек.
	 *
	 * @param <T> тип декодированных элементов
	 */
	static class NbtTypedListReadView<T> implements ReadView.TypedListReadView<T> {

		private final ErrorReporter reporter;
		private final String name;
		final ReadContext context;
		final Codec<T> typeCodec;
		private final NbtList list;

		NbtTypedListReadView(
				ErrorReporter reporter,
				String name,
				ReadContext context,
				Codec<T> typeCodec,
				NbtList list
		) {
			this.reporter = reporter;
			this.name = name;
			this.context = context;
			this.typeCodec = typeCodec;
			this.list = list;
		}

		@Override
		public boolean isEmpty() {
			return list.isEmpty();
		}

		void reportDecodeAtIndexError(int index, NbtElement element, Error<?> error) {
			reporter.report(new DecodeAtIndexError(name, index, element, error));
		}

		@Override
		@SuppressWarnings("unchecked")
		public Stream<T> stream() {
			return (Stream<T>) Streams.mapWithIndex(
					list.stream(),
					(element, rawIndex) -> switch (typeCodec.parse(context.getOps(), element)) {
						case Success<T> success -> (Object) success.value();
						case Error<T> error -> {
							reportDecodeAtIndexError((int) rawIndex, element, error);
							yield error.partialValue().orElse(null);
						}
						default -> throw new MatchException(null, null);
					}
			).filter(Objects::nonNull);
		}

		@Override
		@SuppressWarnings("unchecked")
		public Iterator<T> iterator() {
			ListIterator<NbtElement> listIterator = list.listIterator();

			return new AbstractIterator<>() {
				@Override
				protected @Nullable T computeNext() {
					while (listIterator.hasNext()) {
						int index = listIterator.nextIndex();
						NbtElement element = listIterator.next();

						switch (typeCodec.parse(context.getOps(), element)) {
							case Success<T> success:
								return (T) success.value();
							case Error<T> error:
								reportDecodeAtIndexError(index, element, error);
								Optional<T> partial = error.partialValue();

								if (partial.isPresent()) {
									return partial.get();
								}

								break;
							default:
								throw new MatchException(null, null);
						}
					}

					return (T) endOfData();
				}
			};
		}
	}

	// -------------------------------------------------------------------------
	// Записи об ошибках
	// -------------------------------------------------------------------------

	/**
	 * Ошибка декодирования элемента списка по индексу.
	 *
	 * @param name    имя поля-списка
	 * @param index   индекс элемента в списке
	 * @param element NBT-элемент, который не удалось декодировать
	 * @param error   результат ошибки от DFU
	 */
	public record DecodeAtIndexError(
			String name,
			int index,
			NbtElement element,
			Error<?> error
	) implements ErrorReporter.Error {

		@Override
		public String getMessage() {
			return "Failed to decode value '"
					+ element
					+ "' from field '"
					+ name
					+ "' at index "
					+ index
					+ "': "
					+ error.message();
		}
	}

	/**
	 * Ошибка декодирования значения поля через {@link Codec}.
	 *
	 * @param name    имя поля
	 * @param element NBT-элемент, который не удалось декодировать
	 * @param error   результат ошибки от DFU
	 */
	public record DecodeError(
			String name,
			NbtElement element,
			Error<?> error
	) implements ErrorReporter.Error {

		@Override
		public String getMessage() {
			return "Failed to decode value '"
					+ element
					+ "' from field '"
					+ name
					+ "': "
					+ error.message();
		}
	}

	/**
	 * Ошибка декодирования через {@link MapCodec} из плоской карты полей.
	 *
	 * @param error результат ошибки от DFU
	 */
	public record DecodeMapError(Error<?> error) implements ErrorReporter.Error {

		@Override
		public String getMessage() {
			return "Failed to decode from map: " + error.message();
		}
	}

	/**
	 * Ошибка: поле содержит не числовой NBT-тип, хотя ожидалось число.
	 *
	 * @param name   имя поля
	 * @param actual фактический NBT-тип поля
	 */
	public record ExpectedNumberError(String name, NbtType<?> actual) implements ErrorReporter.Error {

		@Override
		public String getMessage() {
			return "Expected field '"
					+ name
					+ "' to contain number, but got "
					+ actual.getCrashReportName();
		}
	}

	/**
	 * Ошибка: элемент списка по индексу имеет неожиданный NBT-тип.
	 *
	 * @param name     имя поля-списка
	 * @param index    индекс элемента
	 * @param expected ожидаемый NBT-тип
	 * @param actual   фактический NBT-тип
	 */
	public record ExpectedTypeAtIndexError(
			String name,
			int index,
			NbtType<?> expected,
			NbtType<?> actual
	) implements ErrorReporter.Error {

		@Override
		public String getMessage() {
			return "Expected list '"
					+ name
					+ "' to contain at index "
					+ index
					+ " value of type "
					+ expected.getCrashReportName()
					+ ", but got "
					+ actual.getCrashReportName();
		}
	}

	/**
	 * Ошибка: поле содержит NBT-элемент неожиданного типа.
	 *
	 * @param name     имя поля
	 * @param expected ожидаемый NBT-тип
	 * @param actual   фактический NBT-тип
	 */
	public record ExpectedTypeError(
			String name,
			NbtType<?> expected,
			NbtType<?> actual
	) implements ErrorReporter.Error {

		@Override
		public String getMessage() {
			return "Expected field '"
					+ name
					+ "' to contain value of type "
					+ expected.getCrashReportName()
					+ ", but got "
					+ actual.getCrashReportName();
		}
	}
}
