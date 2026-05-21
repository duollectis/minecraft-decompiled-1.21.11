package net.minecraft.util;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;

import java.util.function.Function;

/**
 * {@code TriState}.
 */
public enum TriState implements StringIdentifiable {
	TRUE("true"),
	FALSE("false"),
	DEFAULT("default");

	public static final Codec<TriState>
			CODEC =
			Codec.either(Codec.BOOL, StringIdentifiable.createCodec(TriState::values))
			     .xmap(
					     either -> (TriState) either.map(TriState::ofBoolean, Function.identity()), triState -> {
						     return switch (triState) {
							     case TRUE -> Either.left(true);
							     case FALSE -> Either.left(false);
							     case DEFAULT -> Either.right(triState);
						     };
					     }
			     );
	private final String name;

	private TriState(final String name) {
		this.name = name;
	}

	/**
	 * Of boolean.
	 *
	 * @param value value
	 *
	 * @return TriState — результат операции
	 */
	public static TriState ofBoolean(boolean value) {
		return value ? TRUE : FALSE;
	}

	/**
	 * As boolean.
	 *
	 * @param fallback fallback
	 *
	 * @return boolean — результат операции
	 */
	public boolean asBoolean(boolean fallback) {
		return switch (this) {
			case TRUE -> true;
			case FALSE -> false;
			default -> fallback;
		};
	}

	@Override
	public String asString() {
		return this.name;
	}
}
