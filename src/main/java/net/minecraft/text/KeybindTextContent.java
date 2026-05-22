package net.minecraft.text;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Содержимое текстового компонента, отображающее локализованное название клавиши управления.
 *
 * <p>Перевод клавиши разрешается лениво через {@link KeybindTranslations#factory} при первом обращении.
 * Это позволяет обновлять отображение при смене раскладки без пересоздания компонента.</p>
 */
public class KeybindTextContent implements TextContent {

	public static final MapCodec<KeybindTextContent> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(Codec.STRING.fieldOf("keybind").forGetter(content -> content.key))
					.apply(instance, KeybindTextContent::new)
	);

	private final String key;
	private @Nullable Supplier<Text> translated;

	public KeybindTextContent(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}

	@Override
	public MapCodec<KeybindTextContent> getCodec() {
		return CODEC;
	}

	private Text getTranslated() {
		if (translated == null) {
			translated = KeybindTranslations.factory.apply(key);
		}

		return translated.get();
	}

	@Override
	public <T> Optional<T> visit(StringVisitable.Visitor<T> visitor) {
		return getTranslated().visit(visitor);
	}

	@Override
	public <T> Optional<T> visit(StringVisitable.StyledVisitor<T> visitor, Style style) {
		return getTranslated().visit(visitor, style);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		return o instanceof KeybindTextContent other && key.equals(other.key);
	}

	@Override
	public int hashCode() {
		return key.hashCode();
	}

	@Override
	public String toString() {
		return "keybind{" + key + "}";
	}
}
