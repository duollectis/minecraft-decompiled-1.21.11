package net.minecraft.text;

import java.util.Locale;

/**
 * Исключение, выбрасываемое при ошибке разбора или обращения к аргументам переводимого текста.
 *
 * <p>Используется в {@link TranslatableTextContent} для трёх сценариев:
 * синтаксической ошибки в строке перевода, выхода за пределы массива аргументов
 * и исключения, возникшего при обработке аргумента.</p>
 */
public class TranslationException extends IllegalArgumentException {

	/**
	 * Создаёт исключение с описанием ошибки разбора строки перевода.
	 *
	 * @param text содержимое переводимого компонента
	 * @param message описание ошибки
	 */
	public TranslationException(TranslatableTextContent text, String message) {
		super(String.format(Locale.ROOT, "Error parsing: %s: %s", text, message));
	}

	/**
	 * Создаёт исключение при обращении к несуществующему индексу аргумента.
	 *
	 * @param text содержимое переводимого компонента
	 * @param index запрошенный индекс аргумента
	 */
	public TranslationException(TranslatableTextContent text, int index) {
		super(String.format(Locale.ROOT, "Invalid index %d requested for %s", index, text));
	}

	/**
	 * Создаёт исключение с причиной — исключением, возникшим при обработке аргумента.
	 *
	 * @param text содержимое переводимого компонента
	 * @param cause исходное исключение
	 */
	public TranslationException(TranslatableTextContent text, Throwable cause) {
		super(String.format(Locale.ROOT, "Error while parsing: %s", text), cause);
	}
}
