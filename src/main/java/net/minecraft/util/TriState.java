package net.minecraft.util;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;

import java.util.function.Function;

/**
 * Трёхзначное логическое состояние: {@link #TRUE}, {@link #FALSE} или {@link #DEFAULT}.
 * Сериализуется как булево значение ({@code true}/{@code false}) или строка {@code "default"}.
 */
public enum TriState implements StringIdentifiable {
	TRUE("true"),
	FALSE("false"),
	DEFAULT("default");

	public static final Codec<TriState> CODEC = Codec
			.either(Codec.BOOL, StringIdentifiable.createCodec(TriState::values))
			.xmap(
					either -> either.map(TriState::ofBoolean, Function.identity()),
					triState -> switch (triState) {
						case TRUE -> Either.left(true);
						case FALSE -> Either.left(false);
						case DEFAULT -> Either.right(triState);
					}
			);

	private final String name;

	TriState(String name) {
		this.name = name;
	}

	public static TriState ofBoolean(boolean value) {
		return value ? TRUE : FALSE;
	}

	/**
	 * Возвращает булево значение состояния, используя {@code fallback} для {@link #DEFAULT}.
	 *
	 * @param fallback значение, возвращаемое при состоянии {@link #DEFAULT}
	 * @return булево значение
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
		return name;
	}
}
