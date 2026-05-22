package net.minecraft.util.packrat;

/**
 * Именованный символ грамматики — уникальный ключ для хранения результата разбора
 * в {@link ParseResults}. Идентичность символов определяется по ссылке (identity),
 * поэтому два символа с одинаковым именем — разные ключи.
 */
public record Symbol<T>(String name) {

	@Override
	public String toString() {
		return "<" + name + ">";
	}

	public static <T> Symbol<T> of(String name) {
		return new Symbol<>(name);
	}
}
