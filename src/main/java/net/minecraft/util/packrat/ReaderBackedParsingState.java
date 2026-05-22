package net.minecraft.util.packrat;

import com.mojang.brigadier.StringReader;

/**
 * Конкретная реализация {@link ParsingStateImpl} для разбора строк через {@link StringReader}.
 * Делегирует управление курсором напрямую ридеру, синхронизируя позицию парсера
 * с позицией в строке ввода.
 */
public class ReaderBackedParsingState extends ParsingStateImpl<StringReader> {

	private final StringReader reader;

	public ReaderBackedParsingState(ParseErrorList<StringReader> errors, StringReader reader) {
		super(errors);
		this.reader = reader;
	}

	@Override
	public StringReader getReader() {
		return reader;
	}

	@Override
	public int getCursor() {
		return reader.getCursor();
	}

	@Override
	public void setCursor(int cursor) {
		reader.setCursor(cursor);
	}
}
