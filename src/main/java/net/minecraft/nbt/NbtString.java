package net.minecraft.nbt;

import net.minecraft.nbt.scanner.NbtScanner;
import net.minecraft.nbt.visitor.NbtElementVisitor;
import net.minecraft.nbt.visitor.StringNbtWriter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Optional;

/**
 * NBT-элемент, хранящий строковое значение в кодировке UTF-8.
 *
 * <p>Пустая строка кэшируется в {@code EMPTY}. Используйте фабричный метод
 * {@link #of(String)} вместо конструктора record.</p>
 */
public record NbtString(String value) implements NbtPrimitive {

	/**
	 * Базовый размер тега в байтах: 36 байт заголовка объекта + 2 байта на каждый символ.
	 * Итоговый размер: {@code SIZE + 2 * value.length()}.
	 */
	private static final int SIZE = 36;

	public static final NbtType<NbtString> TYPE = new NbtType.OfVariableSize<>() {
		@Override
		public NbtString read(DataInput input, NbtSizeTracker tracker) throws IOException {
			return NbtString.of(readString(input, tracker));
		}

		@Override
		public NbtScanner.Result doAccept(DataInput input, NbtScanner visitor, NbtSizeTracker tracker)
		throws IOException {
			return visitor.visitString(readString(input, tracker));
		}

		private static String readString(DataInput input, NbtSizeTracker tracker) throws IOException {
			tracker.add(SIZE);
			String string = input.readUTF();
			tracker.add(2L, string.length());
			return string;
		}

		@Override
		public void skip(DataInput input, NbtSizeTracker tracker) throws IOException {
			NbtString.skip(input);
		}

		@Override
		public String getCrashReportName() {
			return "STRING";
		}

		@Override
		public String getCommandFeedbackName() {
			return "TAG_String";
		}
	};

	private static final NbtString EMPTY = new NbtString("");

	@Deprecated(forRemoval = true)
	public NbtString(String value) {
		this.value = value;
	}

	/**
	 * Пропускает строку в потоке без её чтения (читает длину и пропускает байты).
	 *
	 * @param input поток ввода
	 * @throws IOException при ошибке чтения
	 */
	public static void skip(DataInput input) throws IOException {
		input.skipBytes(input.readUnsignedShort());
	}

	/**
	 * Возвращает кэшированный {@code EMPTY} для пустой строки, иначе создаёт новый объект.
	 *
	 * @param value строковое значение
	 * @return {@link NbtString} для заданного значения
	 */
	public static NbtString of(String value) {
		return value.isEmpty() ? EMPTY : new NbtString(value);
	}

	@Override
	public void write(DataOutput output) throws IOException {
		output.writeUTF(value);
	}

	@Override
	public int getSizeInBytes() {
		return SIZE + 2 * value.length();
	}

	@Override
	public byte getType() {
		return STRING_TYPE;
	}

	@Override
	public NbtType<NbtString> getNbtType() {
		return TYPE;
	}

	@Override
	public String toString() {
		StringNbtWriter writer = new StringNbtWriter();
		writer.visitString(this);
		return writer.getString();
	}

	@Override
	public NbtString copy() {
		return this;
	}

	@Override
	public Optional<String> asString() {
		return Optional.of(value);
	}

	@Override
	public void accept(NbtElementVisitor visitor) {
		visitor.visitString(this);
	}

	/**
	 * Экранирует строку и оборачивает её в кавычки для SNBT-формата.
	 * Автоматически выбирает тип кавычек (одинарные или двойные) в зависимости от содержимого.
	 *
	 * @param value исходная строка
	 * @return экранированная строка в кавычках
	 */
	public static String escape(String value) {
		StringBuilder builder = new StringBuilder();
		appendEscaped(value, builder);
		return builder.toString();
	}

	/**
	 * Добавляет экранированную строку в кавычках в переданный {@link StringBuilder}.
	 * Алгоритм выбора кавычек: если встречается {@code "}, используются {@code '}, и наоборот.
	 * Если встречаются оба типа, используются {@code "} с экранированием.
	 *
	 * @param value   исходная строка
	 * @param builder целевой строитель
	 */
	public static void appendEscaped(String value, StringBuilder builder) {
		int quotePosition = builder.length();
		builder.append(' ');
		char selectedQuote = 0;

		for (int charIndex = 0; charIndex < value.length(); charIndex++) {
			char ch = value.charAt(charIndex);

			if (ch == '\\') {
				builder.append("\\\\");
			} else if (ch != '"' && ch != '\'') {
				String escaped = SnbtParsing.escapeSpecialChar(ch);

				if (escaped != null) {
					builder.append('\\');
					builder.append(escaped);
				} else {
					builder.append(ch);
				}
			} else {
				if (selectedQuote == 0) {
					// Выбираем противоположный тип кавычек, чтобы не экранировать текущий символ
					selectedQuote = ch == '"' ? '\'' : '"';
				}

				if (selectedQuote == ch) {
					builder.append('\\');
				}

				builder.append(ch);
			}
		}

		if (selectedQuote == 0) {
			selectedQuote = '"';
		}

		builder.setCharAt(quotePosition, selectedQuote);
		builder.append(selectedQuote);
	}

	/**
	 * Экранирует строку без добавления обрамляющих кавычек.
	 *
	 * @param value исходная строка
	 * @return экранированная строка без кавычек
	 */
	public static String escapeUnquoted(String value) {
		StringBuilder builder = new StringBuilder();
		appendEscapedWithoutQuoting(value, builder);
		return builder.toString();
	}

	/**
	 * Добавляет экранированную строку без кавычек в переданный {@link StringBuilder}.
	 *
	 * @param value   исходная строка
	 * @param builder целевой строитель
	 */
	public static void appendEscapedWithoutQuoting(String value, StringBuilder builder) {
		for (int charIndex = 0; charIndex < value.length(); charIndex++) {
			char ch = value.charAt(charIndex);

			switch (ch) {
				case '"', '\'', '\\' -> {
					builder.append('\\');
					builder.append(ch);
				}
				default -> {
					String escaped = SnbtParsing.escapeSpecialChar(ch);

					if (escaped != null) {
						builder.append('\\');
						builder.append(escaped);
					} else {
						builder.append(ch);
					}
				}
			}
		}
	}

	@Override
	public NbtScanner.Result doAccept(NbtScanner visitor) {
		return visitor.visitString(value);
	}
}
