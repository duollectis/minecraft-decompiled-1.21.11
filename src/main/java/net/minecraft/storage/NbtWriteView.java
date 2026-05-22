package net.minecraft.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult.Error;
import com.mojang.serialization.DataResult.Success;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.ErrorReporter;
import org.jspecify.annotations.Nullable;

/**
 * Реализация {@link WriteView} поверх {@link NbtCompound}.
 * <p>
 * Записывает поля в NBT-тег, кодируя значения через кодеки DFU. При ошибках
 * кодирования сообщает об этом через {@link ErrorReporter}, не бросая исключений.
 * Поддерживает вложенные объекты и списки с автоматическим созданием дочерних
 * контекстов ошибок.
 */
public class NbtWriteView implements WriteView {

	private final ErrorReporter reporter;
	private final DynamicOps<NbtElement> ops;
	private final NbtCompound nbt;

	NbtWriteView(ErrorReporter reporter, DynamicOps<NbtElement> ops, NbtCompound nbt) {
		this.reporter = reporter;
		this.ops = ops;
		this.nbt = nbt;
	}

	/**
	 * Создаёт корневое представление для записи с поддержкой реестров.
	 * Операции оборачиваются через {@link RegistryWrapper.WrapperLookup#getOps},
	 * чтобы корректно кодировать ссылки на реестровые объекты.
	 *
	 * @param reporter   получатель ошибок кодирования
	 * @param registries реестры для разрешения ссылок
	 * @return новый {@link NbtWriteView}
	 */
	public static NbtWriteView create(ErrorReporter reporter, RegistryWrapper.WrapperLookup registries) {
		return new NbtWriteView(reporter, registries.getOps(NbtOps.INSTANCE), new NbtCompound());
	}

	/**
	 * Создаёт корневое представление для записи без реестров (чистый NBT).
	 *
	 * @param reporter получатель ошибок кодирования
	 * @return новый {@link NbtWriteView}
	 */
	public static NbtWriteView create(ErrorReporter reporter) {
		return new NbtWriteView(reporter, NbtOps.INSTANCE, new NbtCompound());
	}

	@Override
	public <T> void put(String key, Codec<T> codec, T value) {
		switch (codec.encodeStart(ops, value)) {
			case Success<NbtElement> success:
				nbt.put(key, success.value());
				break;
			case Error<NbtElement> error:
				reporter.report(new EncodeFieldError(key, value, error));
				error.partialValue().ifPresent(partial -> nbt.put(key, partial));
				break;
			default:
				throw new MatchException(null, null);
		}
	}

	@Override
	public <T> void putNullable(String key, Codec<T> codec, @Nullable T value) {
		if (value != null) {
			put(key, codec, value);
		}
	}

	@Override
	public <T> void put(MapCodec<T> codec, T value) {
		switch (codec.encoder().encodeStart(ops, value)) {
			case Success<NbtElement> success:
				nbt.copyFrom((NbtCompound) success.value());
				break;
			case Error<NbtElement> error:
				reporter.report(new MergeError(value, error));
				error.partialValue().ifPresent(partial -> nbt.copyFrom((NbtCompound) partial));
				break;
			default:
				throw new MatchException(null, null);
		}
	}

	@Override
	public void putBoolean(String key, boolean value) {
		nbt.putBoolean(key, value);
	}

	@Override
	public void putByte(String key, byte value) {
		nbt.putByte(key, value);
	}

	@Override
	public void putShort(String key, short value) {
		nbt.putShort(key, value);
	}

	@Override
	public void putInt(String key, int value) {
		nbt.putInt(key, value);
	}

	@Override
	public void putLong(String key, long value) {
		nbt.putLong(key, value);
	}

	@Override
	public void putFloat(String key, float value) {
		nbt.putFloat(key, value);
	}

	@Override
	public void putDouble(String key, double value) {
		nbt.putDouble(key, value);
	}

	@Override
	public void putString(String key, String value) {
		nbt.putString(key, value);
	}

	@Override
	public void putIntArray(String key, int[] value) {
		nbt.putIntArray(key, value);
	}

	private ErrorReporter makeChildReporter(String key) {
		return reporter.makeChild(new ErrorReporter.MapElementContext(key));
	}

	@Override
	public WriteView get(String key) {
		NbtCompound compound = new NbtCompound();
		nbt.put(key, compound);
		return new NbtWriteView(makeChildReporter(key), ops, compound);
	}

	@Override
	public WriteView.ListView getList(String key) {
		NbtList list = new NbtList();
		nbt.put(key, list);
		return new NbtListView(key, reporter, ops, list);
	}

	@Override
	public <T> WriteView.ListAppender<T> getListAppender(String key, Codec<T> codec) {
		NbtList list = new NbtList();
		nbt.put(key, list);
		return new NbtListAppender<>(reporter, key, ops, codec, list);
	}

	@Override
	public void remove(String key) {
		nbt.remove(key);
	}

	@Override
	public boolean isEmpty() {
		return nbt.isEmpty();
	}

	public NbtCompound getNbt() {
		return nbt;
	}

	// -------------------------------------------------------------------------
	// Вложенные классы
	// -------------------------------------------------------------------------

	/**
	 * Аппендер для добавления типизированных элементов в {@link NbtList}.
	 * Каждый элемент кодируется через заданный кодек перед добавлением.
	 *
	 * @param <T> тип элементов
	 */
	static class NbtListAppender<T> implements WriteView.ListAppender<T> {

		private final ErrorReporter reporter;
		private final String key;
		private final DynamicOps<NbtElement> ops;
		private final Codec<T> codec;
		private final NbtList list;

		NbtListAppender(
				ErrorReporter reporter,
				String key,
				DynamicOps<NbtElement> ops,
				Codec<T> codec,
				NbtList list
		) {
			this.reporter = reporter;
			this.key = key;
			this.ops = ops;
			this.codec = codec;
			this.list = list;
		}

		@Override
		public void add(T value) {
			switch (codec.encodeStart(ops, value)) {
				case Success<NbtElement> success:
					list.add(success.value());
					break;
				case Error<NbtElement> error:
					reporter.report(new AppendToListError(key, value, error));
					error.partialValue().ifPresent(list::add);
					break;
				default:
					throw new MatchException(null, null);
			}
		}

		@Override
		public boolean isEmpty() {
			return list.isEmpty();
		}
	}

	/**
	 * Представление {@link NbtList} для последовательного добавления вложенных объектов.
	 * Каждый вызов {@link #add()} создаёт новый {@link NbtCompound} и возвращает его представление.
	 */
	static class NbtListView implements WriteView.ListView {

		private final String key;
		private final ErrorReporter reporter;
		private final DynamicOps<NbtElement> ops;
		private final NbtList list;

		NbtListView(String key, ErrorReporter reporter, DynamicOps<NbtElement> ops, NbtList list) {
			this.key = key;
			this.reporter = reporter;
			this.ops = ops;
			this.list = list;
		}

		@Override
		public WriteView add() {
			int index = list.size();
			NbtCompound compound = new NbtCompound();
			list.add(compound);
			return new NbtWriteView(
					reporter.makeChild(new ErrorReporter.NamedListElementContext(key, index)),
					ops,
					compound
			);
		}

		@Override
		public void removeLast() {
			list.removeLast();
		}

		@Override
		public boolean isEmpty() {
			return list.isEmpty();
		}
	}

	// -------------------------------------------------------------------------
	// Записи об ошибках
	// -------------------------------------------------------------------------

	/**
	 * Ошибка добавления элемента в список через кодек.
	 *
	 * @param name  имя поля-списка
	 * @param value значение, которое не удалось закодировать
	 * @param error результат ошибки от DFU
	 */
	public record AppendToListError(String name, Object value, Error<?> error) implements ErrorReporter.Error {

		@Override
		public String getMessage() {
			return "Failed to append value '"
					+ value
					+ "' to list '"
					+ name
					+ "': "
					+ error.message();
		}
	}

	/**
	 * Ошибка кодирования значения поля через {@link Codec}.
	 *
	 * @param name  имя поля
	 * @param value значение, которое не удалось закодировать
	 * @param error результат ошибки от DFU
	 */
	public record EncodeFieldError(String name, Object value, Error<?> error) implements ErrorReporter.Error {

		@Override
		public String getMessage() {
			return "Failed to encode value '"
					+ value
					+ "' to field '"
					+ name
					+ "': "
					+ error.message();
		}
	}

	/**
	 * Ошибка слияния значения в объект через {@link MapCodec}.
	 *
	 * @param value значение, которое не удалось закодировать
	 * @param error результат ошибки от DFU
	 */
	public record MergeError(Object value, Error<?> error) implements ErrorReporter.Error {

		@Override
		public String getMessage() {
			return "Failed to merge value '"
					+ value
					+ "' to an object: "
					+ error.message();
		}
	}
}
