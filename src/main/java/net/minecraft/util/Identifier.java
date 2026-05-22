package net.minecraft.util;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

import java.util.function.UnaryOperator;

/**
 * Уникальный идентификатор ресурса в формате {@code namespace:path}.
 * <p>
 * Пространство имён ({@code namespace}) может содержать символы {@code [a-z0-9_.-]}.
 * Путь ({@code path}) может содержать символы {@code [a-z0-9/_.-]}.
 * Если пространство имён не указано, используется {@value #DEFAULT_NAMESPACE}.
 * <p>
 * Примеры: {@code minecraft:stone}, {@code mymod:items/sword}.
 */
public final class Identifier implements Comparable<Identifier> {

	public static final Codec<Identifier> CODEC = Codec.STRING
		.comapFlatMap(Identifier::validate, Identifier::toString)
		.stable();
	public static final PacketCodec<ByteBuf, Identifier> PACKET_CODEC = PacketCodecs.STRING
		.xmap(Identifier::of, Identifier::toString);
	public static final SimpleCommandExceptionType COMMAND_EXCEPTION = new SimpleCommandExceptionType(
		Text.translatable("argument.id.invalid")
	);

	public static final char NAMESPACE_SEPARATOR = ':';
	public static final String DEFAULT_NAMESPACE = "minecraft";
	public static final String REALMS_NAMESPACE = "realms";

	private final String namespace;
	private final String path;

	private Identifier(String namespace, String path) {
		assert isNamespaceValid(namespace);
		assert isPathValid(path);

		this.namespace = namespace;
		this.path = path;
	}

	private static Identifier ofValidated(String namespace, String path) {
		return new Identifier(validateNamespace(namespace, path), validatePath(namespace, path));
	}

	/**
	 * Создаёт идентификатор из пространства имён и пути.
	 *
	 * @param namespace пространство имён
	 * @param path      путь
	 * @return новый идентификатор
	 * @throws InvalidIdentifierException если namespace или path содержат недопустимые символы
	 */
	public static Identifier of(String namespace, String path) {
		return ofValidated(namespace, path);
	}

	/**
	 * Создаёт идентификатор из строки вида {@code namespace:path} или {@code path}.
	 * Если разделитель {@code :} отсутствует, используется пространство имён {@value #DEFAULT_NAMESPACE}.
	 *
	 * @param id строка идентификатора
	 * @return новый идентификатор
	 * @throws InvalidIdentifierException если строка содержит недопустимые символы
	 */
	public static Identifier of(String id) {
		return splitOn(id, NAMESPACE_SEPARATOR);
	}

	/**
	 * Создаёт идентификатор в пространстве имён {@value #DEFAULT_NAMESPACE}.
	 *
	 * @param path путь ресурса
	 * @return новый идентификатор в пространстве имён minecraft
	 * @throws InvalidIdentifierException если path содержит недопустимые символы
	 */
	public static Identifier ofVanilla(String path) {
		return new Identifier(DEFAULT_NAMESPACE, validatePath(DEFAULT_NAMESPACE, path));
	}

	/**
	 * Пытается создать идентификатор из строки, возвращая {@code null} при ошибке.
	 *
	 * @param id строка идентификатора
	 * @return идентификатор или {@code null} если строка невалидна
	 */
	public static @Nullable Identifier tryParse(String id) {
		return trySplitOn(id, NAMESPACE_SEPARATOR);
	}

	/**
	 * Пытается создать идентификатор из пространства имён и пути, возвращая {@code null} при ошибке.
	 *
	 * @param namespace пространство имён
	 * @param path      путь
	 * @return идентификатор или {@code null} если аргументы невалидны
	 */
	public static @Nullable Identifier tryParse(String namespace, String path) {
		return isNamespaceValid(namespace) && isPathValid(path) ? new Identifier(namespace, path) : null;
	}

	/**
	 * Разбивает строку по указанному разделителю и создаёт идентификатор.
	 * Если разделитель не найден или стоит в начале, используется {@value #DEFAULT_NAMESPACE}.
	 *
	 * @param id        строка идентификатора
	 * @param delimiter символ-разделитель
	 * @return новый идентификатор
	 * @throws InvalidIdentifierException если строка содержит недопустимые символы
	 */
	public static Identifier splitOn(String id, char delimiter) {
		int separatorIndex = id.indexOf(delimiter);

		if (separatorIndex < 0) {
			return ofVanilla(id);
		}

		String path = id.substring(separatorIndex + 1);

		if (separatorIndex == 0) {
			return ofVanilla(path);
		}

		return ofValidated(id.substring(0, separatorIndex), path);
	}

	/**
	 * Пытается разбить строку по разделителю и создать идентификатор, возвращая {@code null} при ошибке.
	 *
	 * @param id        строка идентификатора
	 * @param delimiter символ-разделитель
	 * @return идентификатор или {@code null} если строка невалидна
	 */
	public static @Nullable Identifier trySplitOn(String id, char delimiter) {
		int separatorIndex = id.indexOf(delimiter);

		if (separatorIndex < 0) {
			return isPathValid(id) ? new Identifier(DEFAULT_NAMESPACE, id) : null;
		}

		String path = id.substring(separatorIndex + 1);

		if (!isPathValid(path)) {
			return null;
		}

		if (separatorIndex == 0) {
			return new Identifier(DEFAULT_NAMESPACE, path);
		}

		String namespace = id.substring(0, separatorIndex);
		return isNamespaceValid(namespace) ? new Identifier(namespace, path) : null;
	}

	/**
	 * Валидирует строку и возвращает {@link DataResult} с идентификатором или ошибкой.
	 * Используется в кодеках сериализации.
	 *
	 * @param id строка идентификатора
	 * @return успешный результат или ошибка с описанием
	 */
	public static DataResult<Identifier> validate(String id) {
		try {
			return DataResult.success(of(id));
		} catch (InvalidIdentifierException exception) {
			return DataResult.error(() -> "Not a valid resource location: " + id + " " + exception.getMessage());
		}
	}

	public String getPath() {
		return path;
	}

	public String getNamespace() {
		return namespace;
	}

	/**
	 * Создаёт новый идентификатор с тем же пространством имён, но другим путём.
	 *
	 * @param newPath новый путь
	 * @return новый идентификатор
	 */
	public Identifier withPath(String newPath) {
		return new Identifier(namespace, validatePath(namespace, newPath));
	}

	/**
	 * Создаёт новый идентификатор с тем же пространством имён, применяя функцию к пути.
	 *
	 * @param pathFunction функция преобразования пути
	 * @return новый идентификатор
	 */
	public Identifier withPath(UnaryOperator<String> pathFunction) {
		return withPath(pathFunction.apply(path));
	}

	/** @return новый идентификатор с добавленным префиксом к пути */
	public Identifier withPrefixedPath(String prefix) {
		return withPath(prefix + path);
	}

	/** @return новый идентификатор с добавленным суффиксом к пути */
	public Identifier withSuffixedPath(String suffix) {
		return withPath(path + suffix);
	}

	@Override
	public String toString() {
		return namespace + ":" + path;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}

		return other instanceof Identifier identifier
			&& namespace.equals(identifier.namespace)
			&& path.equals(identifier.path);
	}

	@Override
	public int hashCode() {
		return 31 * namespace.hashCode() + path.hashCode();
	}

	@Override
	public int compareTo(Identifier other) {
		int pathComparison = path.compareTo(other.path);
		return pathComparison != 0 ? pathComparison : namespace.compareTo(other.namespace);
	}

	/** @return строка вида {@code namespace_path} (все {@code /} и {@code :} заменены на {@code _}) */
	public String toUnderscoreSeparatedString() {
		return toString().replace('/', '_').replace(':', '_');
	}

	/** @return ключ локализации вида {@code namespace.path} */
	public String toTranslationKey() {
		return namespace + "." + path;
	}

	/**
	 * @return сокращённый ключ локализации: только {@code path} для {@value #DEFAULT_NAMESPACE},
	 *         иначе полный {@code namespace.path}
	 */
	public String toShortTranslationKey() {
		return namespace.equals(DEFAULT_NAMESPACE) ? path : toTranslationKey();
	}

	/**
	 * @return сокращённая строка: только {@code path} для {@value #DEFAULT_NAMESPACE},
	 *         иначе полный {@code namespace:path}
	 */
	public String toShortString() {
		return namespace.equals(DEFAULT_NAMESPACE) ? path : toString();
	}

	/** @return ключ локализации с префиксом вида {@code prefix.namespace.path} */
	public String toTranslationKey(String prefix) {
		return prefix + "." + toTranslationKey();
	}

	/** @return ключ локализации с префиксом и суффиксом вида {@code prefix.namespace.path.suffix} */
	public String toTranslationKey(String prefix, String suffix) {
		return prefix + "." + toTranslationKey() + "." + suffix;
	}

	private static String readString(StringReader reader) {
		int start = reader.getCursor();

		while (reader.canRead() && isCharValid(reader.peek())) {
			reader.skip();
		}

		return reader.getString().substring(start, reader.getCursor());
	}

	/**
	 * Читает идентификатор из командного ввода.
	 *
	 * @param reader читатель командной строки
	 * @return прочитанный идентификатор
	 * @throws CommandSyntaxException если идентификатор невалиден
	 */
	public static Identifier fromCommandInput(StringReader reader) throws CommandSyntaxException {
		int cursor = reader.getCursor();
		String raw = readString(reader);

		try {
			return of(raw);
		} catch (InvalidIdentifierException exception) {
			reader.setCursor(cursor);
			throw COMMAND_EXCEPTION.createWithContext(reader);
		}
	}

	/**
	 * Читает непустой идентификатор из командного ввода.
	 *
	 * @param reader читатель командной строки
	 * @return прочитанный идентификатор
	 * @throws CommandSyntaxException если идентификатор пустой или невалиден
	 */
	public static Identifier fromCommandInputNonEmpty(StringReader reader) throws CommandSyntaxException {
		int cursor = reader.getCursor();
		String raw = readString(reader);

		if (raw.isEmpty()) {
			throw COMMAND_EXCEPTION.createWithContext(reader);
		}

		try {
			return of(raw);
		} catch (InvalidIdentifierException exception) {
			reader.setCursor(cursor);
			throw COMMAND_EXCEPTION.createWithContext(reader);
		}
	}

	public static boolean isCharValid(char character) {
		return character >= '0' && character <= '9'
			|| character >= 'a' && character <= 'z'
			|| character == '_'
			|| character == ':'
			|| character == '/'
			|| character == '.'
			|| character == '-';
	}

	public static boolean isPathValid(String path) {
		for (int i = 0; i < path.length(); i++) {
			if (!isPathCharacterValid(path.charAt(i))) {
				return false;
			}
		}

		return true;
	}

	public static boolean isNamespaceValid(String namespace) {
		for (int i = 0; i < namespace.length(); i++) {
			if (!isNamespaceCharacterValid(namespace.charAt(i))) {
				return false;
			}
		}

		return true;
	}

	private static String validateNamespace(String namespace, String path) {
		if (!isNamespaceValid(namespace)) {
			throw new InvalidIdentifierException(
				"Non [a-z0-9_.-] character in namespace of location: " + namespace + ":" + path
			);
		}

		return namespace;
	}

	public static boolean isPathCharacterValid(char character) {
		return character == '_'
			|| character == '-'
			|| character >= 'a' && character <= 'z'
			|| character >= '0' && character <= '9'
			|| character == '/'
			|| character == '.';
	}

	private static boolean isNamespaceCharacterValid(char character) {
		return character == '_'
			|| character == '-'
			|| character >= 'a' && character <= 'z'
			|| character >= '0' && character <= '9'
			|| character == '.';
	}

	private static String validatePath(String namespace, String path) {
		if (!isPathValid(path)) {
			throw new InvalidIdentifierException(
				"Non [a-z0-9/._-] character in path of location: " + namespace + ":" + path
			);
		}

		return path;
	}
}
