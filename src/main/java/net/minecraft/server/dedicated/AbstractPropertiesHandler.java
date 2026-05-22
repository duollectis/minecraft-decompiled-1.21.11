package net.minecraft.server.dedicated;

import com.google.common.base.MoreObjects;
import com.mojang.logging.LogUtils;
import net.minecraft.registry.DynamicRegistryManager;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.*;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Базовый обработчик свойств выделенного сервера.
 * Предоставляет типизированный доступ к значениям из файла {@code server.properties}.
 *
 * @param <T> конкретный тип обработчика свойств
 */
public abstract class AbstractPropertiesHandler<T extends AbstractPropertiesHandler<T>> {

	private static final Logger LOGGER = LogUtils.getLogger();

	protected final Properties properties;

	public AbstractPropertiesHandler(Properties properties) {
		this.properties = properties;
	}

	/**
	 * Загружает свойства из файла. Сначала пробует UTF-8, при ошибке — ISO-8859-1.
	 *
	 * @param path путь к файлу свойств
	 * @return загруженные свойства или пустой объект при ошибке
	 */
	public static Properties loadProperties(Path path) {
		try {
			try {
				Properties var13;

				try (InputStream inputStream = Files.newInputStream(path)) {
					CharsetDecoder charsetDecoder = StandardCharsets.UTF_8
							.newDecoder()
							.onMalformedInput(CodingErrorAction.REPORT)
							.onUnmappableCharacter(CodingErrorAction.REPORT);
					Properties loaded = new Properties();
					loaded.load(new InputStreamReader(inputStream, charsetDecoder));
					var13 = loaded;
				}

				return var13;
			}
			catch (CharacterCodingException var9) {
				LOGGER.info("Failed to load properties as UTF-8 from file {}, trying ISO_8859_1", path);

				Properties var4;

				try (Reader reader = Files.newBufferedReader(path, StandardCharsets.ISO_8859_1)) {
					Properties loaded = new Properties();
					loaded.load(reader);
					var4 = loaded;
				}

				return var4;
			}
		}
		catch (IOException var10) {
			LOGGER.error("Failed to load properties from file: {}", path, var10);
			return new Properties();
		}
	}

	/**
	 * Сохраняет текущие свойства в файл в кодировке UTF-8.
	 *
	 * @param path путь к файлу для сохранения
	 */
	public void saveProperties(Path path) {
		try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			this.properties.store(writer, "Minecraft server properties");
		}
		catch (IOException var7) {
			LOGGER.error("Failed to store properties to file: {}", path);
		}
	}

	/**
	 * Оборачивает числовой парсер, возвращая {@code null} при {@link NumberFormatException}.
	 */
	private static <V extends Number> Function<String, @Nullable V> wrapNumberParser(Function<String, V> parser) {
		return string -> {
			try {
				return parser.apply(string);
			}
			catch (NumberFormatException var3) {
				return null;
			}
		};
	}

	/**
	 * Создаёт комбинированный парсер: сначала пробует разобрать как int, затем как строку.
	 *
	 * @param intParser      парсер по целочисленному значению
	 * @param fallbackParser запасной строковый парсер
	 * @return комбинированный парсер
	 */
	protected static <V> Function<String, @Nullable V> combineParser(
			IntFunction<@Nullable V> intParser,
			Function<String, @Nullable V> fallbackParser
	) {
		return string -> {
			try {
				return intParser.apply(Integer.parseInt(string));
			}
			catch (NumberFormatException var4) {
				return fallbackParser.apply(string);
			}
		};
	}

	private @Nullable String getStringValue(String key) {
		return (String) this.properties.get(key);
	}

	/**
	 * Читает устаревшее свойство, удаляя его из хранилища после чтения.
	 *
	 * @param key         ключ свойства
	 * @param stringifier функция преобразования строки в значение
	 * @return значение или {@code null} если свойство отсутствует
	 */
	protected <V> @Nullable V getDeprecated(String key, Function<String, V> stringifier) {
		String string = this.getStringValue(key);

		if (string == null) {
			return null;
		}

		this.properties.remove(key);
		return stringifier.apply(string);
	}

	/**
	 * Читает свойство с парсингом и записывает нормализованное значение обратно.
	 *
	 * @param key         ключ свойства
	 * @param parser      парсер строки в значение
	 * @param stringifier сериализатор значения в строку
	 * @param fallback    значение по умолчанию
	 * @return разобранное или дефолтное значение
	 */
	protected <V> V get(String key, Function<String, @Nullable V> parser, Function<V, String> stringifier, V fallback) {
		String string = this.getStringValue(key);
		V object = (V) MoreObjects.firstNonNull(string != null ? parser.apply(string) : null, fallback);
		this.properties.put(key, stringifier.apply(object));
		return object;
	}

	/**
	 * Создаёт аксессор для свойства с возможностью создания нового обработчика с изменённым значением.
	 *
	 * @param key         ключ свойства
	 * @param parser      парсер строки в значение
	 * @param stringifier сериализатор значения в строку
	 * @param fallback    значение по умолчанию
	 * @return аксессор свойства
	 */
	protected <V> AbstractPropertiesHandler<T>.PropertyAccessor<V> accessor(
			String key,
			Function<String, @Nullable V> parser,
			Function<V, String> stringifier,
			V fallback
	) {
		String string = this.getStringValue(key);
		V object = (V) MoreObjects.firstNonNull(string != null ? parser.apply(string) : null, fallback);
		this.properties.put(key, stringifier.apply(object));
		return new AbstractPropertiesHandler<T>.PropertyAccessor<>(key, object, stringifier);
	}

	/**
	 * Читает свойство с парсингом и дополнительным преобразованием разобранного значения.
	 */
	protected <V> V get(
			String key,
			Function<String, @Nullable V> parser,
			UnaryOperator<V> parsedTransformer,
			Function<V, String> stringifier,
			V fallback
	) {
		return this.get(
				key, value -> {
					V object = parser.apply(value);
					return object != null ? parsedTransformer.apply(object) : null;
				}, stringifier, fallback
		);
	}

	/**
	 * Читает свойство, используя {@link Objects#toString} для сериализации.
	 */
	protected <V> V get(String key, Function<String, V> parser, V fallback) {
		return this.get(key, parser, Objects::toString, fallback);
	}

	/**
	 * Создаёт аксессор свойства, используя {@link Objects#toString} для сериализации.
	 */
	protected <V> AbstractPropertiesHandler<T>.PropertyAccessor<V> accessor(
			String key,
			Function<String, V> parser,
			V fallback
	) {
		return this.accessor(key, parser, Objects::toString, fallback);
	}

	/**
	 * Читает строковое свойство.
	 *
	 * @param key      ключ свойства
	 * @param fallback значение по умолчанию
	 * @return строковое значение
	 */
	protected String getString(String key, String fallback) {
		return this.get(key, Function.identity(), Function.identity(), fallback);
	}

	/**
	 * Читает устаревшее строковое свойство, удаляя его после чтения.
	 */
	protected @Nullable String getDeprecatedString(String key) {
		return this.getDeprecated(key, Function.identity());
	}

	/**
	 * Читает целочисленное свойство.
	 *
	 * @param key      ключ свойства
	 * @param fallback значение по умолчанию
	 * @return целочисленное значение
	 */
	protected int getInt(String key, int fallback) {
		return this.get(key, wrapNumberParser(Integer::parseInt), fallback);
	}

	/**
	 * Создаёт аксессор для целочисленного свойства.
	 */
	protected AbstractPropertiesHandler<T>.PropertyAccessor<Integer> intAccessor(String key, int fallback) {
		return this.accessor(key, wrapNumberParser(Integer::parseInt), fallback);
	}

	/**
	 * Создаёт аксессор для строкового свойства.
	 */
	protected AbstractPropertiesHandler<T>.PropertyAccessor<String> stringAccessor(String key, String fallback) {
		return this.accessor(key, String::new, fallback);
	}

	/**
	 * Читает целочисленное свойство с дополнительным преобразованием значения.
	 */
	protected int transformedParseInt(String key, UnaryOperator<Integer> transformer, int fallback) {
		return this.get(key, wrapNumberParser(Integer::parseInt), transformer, Objects::toString, fallback);
	}

	/**
	 * Читает свойство типа {@code long}.
	 */
	protected long parseLong(String key, long fallback) {
		return this.get(key, wrapNumberParser(Long::parseLong), fallback);
	}

	/**
	 * Читает булево свойство.
	 */
	protected boolean parseBoolean(String key, boolean fallback) {
		return this.get(key, Boolean::valueOf, fallback);
	}

	/**
	 * Создаёт аксессор для булевого свойства.
	 */
	protected AbstractPropertiesHandler<T>.PropertyAccessor<Boolean> booleanAccessor(String key, boolean fallback) {
		return this.accessor(key, Boolean::valueOf, fallback);
	}

	/**
	 * Читает устаревшее булево свойство, удаляя его после чтения.
	 */
	protected @Nullable Boolean getDeprecatedBoolean(String key) {
		return this.getDeprecated(key, Boolean::valueOf);
	}

	/**
	 * Создаёт копию текущих свойств.
	 *
	 * @return новый объект {@link Properties} с теми же значениями
	 */
	protected Properties copyProperties() {
		Properties copy = new Properties();
		copy.putAll(this.properties);
		return copy;
	}

	/**
	 * Создаёт новый экземпляр обработчика с указанными свойствами.
	 *
	 * @param registryManager менеджер реестров
	 * @param properties      свойства для нового экземпляра
	 * @return новый обработчик свойств
	 */
	protected abstract T create(DynamicRegistryManager registryManager, Properties properties);

	/**
	 * Аксессор для отдельного свойства, позволяющий читать значение и создавать
	 * новый обработчик с изменённым значением этого свойства.
	 *
	 * @param <V> тип значения свойства
	 */
	public class PropertyAccessor<V> implements Supplier<V> {

		private final String key;
		private final V value;
		private final Function<V, String> stringifier;

		PropertyAccessor(final String key, final V value, final Function<V, String> stringifier) {
			this.key = key;
			this.value = value;
			this.stringifier = stringifier;
		}

		@Override
		public V get() {
			return this.value;
		}

		/**
		 * Создаёт новый обработчик свойств с изменённым значением этого свойства.
		 *
		 * @param registryManager менеджер реестров
		 * @param value           новое значение свойства
		 * @return новый обработчик с обновлённым свойством
		 */
		public T set(DynamicRegistryManager registryManager, V value) {
			Properties updated = AbstractPropertiesHandler.this.copyProperties();
			updated.put(this.key, this.stringifier.apply(value));
			return AbstractPropertiesHandler.this.create(registryManager, updated);
		}
	}
}
