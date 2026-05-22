package net.minecraft.nbt;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.text.Text;
import net.minecraft.util.packrat.PackratParser;

/**
 * Читает NBT-данные из строки в формате SNBT (Stringified NBT).
 * <p>
 * Поддерживает два режима работы:
 * <ul>
 *   <li>{@link #read(String)} — читает всю строку целиком, выбрасывает исключение при наличии хвостовых символов.</li>
 *   <li>{@link #readAsArgument(StringReader)} — читает NBT как часть более длинной строки (для команд).</li>
 * </ul>
 * Использует {@link SnbtParsing#createParser(DynamicOps)} для построения packrat-парсера,
 * что обеспечивает поддержку всех типов NBT, включая числа с суффиксами, массивы и вложенные структуры.
 *
 * @param <T> целевой тип данных, определяемый переданным {@link DynamicOps}
 */
public class StringNbtReader<T> {

	public static final SimpleCommandExceptionType TRAILING = new SimpleCommandExceptionType(
		Text.translatable("argument.nbt.trailing")
	);
	public static final SimpleCommandExceptionType EXPECTED_COMPOUND = new SimpleCommandExceptionType(
		Text.translatable("argument.nbt.expected.compound")
	);
	public static final char COMMA = ',';
	public static final char COLON = ':';

	private static final StringNbtReader<NbtElement> DEFAULT_READER = fromOps(NbtOps.INSTANCE);

	/**
	 * Кодек для десериализации {@link NbtCompound} из SNBT-строки.
	 * При сериализации использует {@link NbtCompound#toString()}.
	 */
	public static final Codec<NbtCompound> STRINGIFIED_CODEC = Codec.STRING
		.comapFlatMap(
			snbt -> {
				try {
					NbtElement parsed = DEFAULT_READER.read(snbt);
					return parsed instanceof NbtCompound compound
					       ? DataResult.success(compound, Lifecycle.stable())
					       : DataResult.error(() -> "Expected compound tag, got " + parsed);
				}
				catch (CommandSyntaxException exception) {
					return DataResult.error(exception::getMessage);
				}
			},
			NbtCompound::toString
		);

	public static final Codec<NbtCompound> NBT_COMPOUND_CODEC = Codec.withAlternative(
		STRINGIFIED_CODEC,
		NbtCompound.CODEC
	);

	private final DynamicOps<T> ops;
	private final PackratParser<T> parser;

	private StringNbtReader(DynamicOps<T> ops, PackratParser<T> parser) {
		this.ops = ops;
		this.parser = parser;
	}

	public DynamicOps<T> getOps() {
		return ops;
	}

	/**
	 * Создаёт {@link StringNbtReader} для заданного {@link DynamicOps}.
	 * Каждый вызов создаёт новый экземпляр с собственным парсером.
	 *
	 * @param ops операции сериализации целевого типа
	 * @param <T> целевой тип данных
	 * @return новый ридер для указанного типа
	 */
	public static <T> StringNbtReader<T> fromOps(DynamicOps<T> ops) {
		return new StringNbtReader<>(ops, SnbtParsing.createParser(ops));
	}

	/**
	 * Читает SNBT-строку и возвращает {@link NbtCompound}.
	 * Выбрасывает исключение, если результат не является compound-тегом.
	 *
	 * @param snbt строка в формате SNBT
	 * @return распарсенный {@link NbtCompound}
	 * @throws CommandSyntaxException если строка не является валидным compound-тегом
	 */
	public static NbtCompound readCompound(String snbt) throws CommandSyntaxException {
		StringReader reader = new StringReader(snbt);
		return expectCompound(reader, DEFAULT_READER.read(reader));
	}

	/**
	 * Читает всю строку как NBT-элемент.
	 * Выбрасывает исключение, если после элемента остались непрочитанные символы.
	 *
	 * @param snbt строка в формате SNBT
	 * @return распарсенный элемент
	 * @throws CommandSyntaxException при синтаксической ошибке или хвостовых символах
	 */
	public T read(String snbt) throws CommandSyntaxException {
		return read(new StringReader(snbt));
	}

	/**
	 * Читает NBT-элемент из {@link StringReader}, потребляя всю оставшуюся строку.
	 * Выбрасывает исключение, если после элемента остались непрочитанные символы.
	 *
	 * @param reader источник символов
	 * @return распарсенный элемент
	 * @throws CommandSyntaxException при синтаксической ошибке или хвостовых символах
	 */
	public T read(StringReader reader) throws CommandSyntaxException {
		T parsed = parser.parse(reader);
		reader.skipWhitespace();

		if (reader.canRead()) {
			throw TRAILING.createWithContext(reader);
		}

		return parsed;
	}

	/**
	 * Читает NBT-элемент из {@link StringReader} как аргумент команды.
	 * В отличие от {@link #read(StringReader)}, не требует, чтобы строка была прочитана до конца.
	 *
	 * @param reader источник символов
	 * @return распарсенный элемент
	 * @throws CommandSyntaxException при синтаксической ошибке
	 */
	public T readAsArgument(StringReader reader) throws CommandSyntaxException {
		return parser.parse(reader);
	}

	/**
	 * Читает {@link NbtCompound} из {@link StringReader} как аргумент команды.
	 * Выбрасывает исключение, если результат не является compound-тегом.
	 *
	 * @param reader источник символов
	 * @return распарсенный {@link NbtCompound}
	 * @throws CommandSyntaxException если результат не является compound-тегом
	 */
	public static NbtCompound readCompoundAsArgument(StringReader reader) throws CommandSyntaxException {
		NbtElement parsed = DEFAULT_READER.readAsArgument(reader);
		return expectCompound(reader, parsed);
	}

	private static NbtCompound expectCompound(StringReader reader, NbtElement element)
	throws CommandSyntaxException {
		if (element instanceof NbtCompound compound) {
			return compound;
		}

		throw EXPECTED_COMPOUND.createWithContext(reader);
	}
}
