package net.minecraft.util.context;

import net.minecraft.util.Identifier;

/**
 * {@code ContextParameter}.
 */
public class ContextParameter<T> {

	private final Identifier id;

	public ContextParameter(Identifier id) {
		this.id = id;
	}

	/**
	 * Of.
	 *
	 * @param id id
	 *
	 * @return ContextParameter — результат операции
	 */
	public static <T> ContextParameter<T> of(String id) {
		return new ContextParameter<>(Identifier.ofVanilla(id));
	}

	public Identifier getId() {
		return this.id;
	}

	@Override
	public String toString() {
		return "<parameter " + this.id + ">";
	}
}
