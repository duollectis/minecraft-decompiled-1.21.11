package net.minecraft.util.packrat;

/**
 * {@code Symbol}.
 */
public record Symbol<T>(String name) {

	@Override
	public String toString() {
		return "<" + this.name + ">";
	}

	/**
	 * Of.
	 *
	 * @param name name
	 *
	 * @return Symbol — результат операции
	 */
	public static <T> Symbol<T> of(String name) {
		return new Symbol<>(name);
	}
}
