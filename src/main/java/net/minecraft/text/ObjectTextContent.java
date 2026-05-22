package net.minecraft.text;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.text.object.TextObjectContentTypes;
import net.minecraft.text.object.TextObjectContents;

import java.util.Optional;

/**
 * Содержимое текстового компонента, представляющее встроенный объект (спрайт, игрок и т.д.).
 *
 * <p>При текстовом обходе (visit) возвращает строку {@link #OBJECT_REPLACEMENT_CHARACTER} —
 * символ U+FFFC (Object Replacement Character), который рендерер заменяет на реальный спрайт.
 * При строковом обходе возвращает человекочитаемое представление через {@link TextObjectContents#asText()}.</p>
 */
public record ObjectTextContent(TextObjectContents contents) implements TextContent {

	/** Unicode Object Replacement Character (U+FFFC), используется как placeholder при рендеринге спрайтов. */
	private static final String OBJECT_REPLACEMENT_CHARACTER = "\uFFFC";

	public static final MapCodec<ObjectTextContent> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(TextObjectContentTypes.CODEC.forGetter(ObjectTextContent::contents))
					.apply(instance, ObjectTextContent::new)
	);

	@Override
	public MapCodec<ObjectTextContent> getCodec() {
		return CODEC;
	}

	@Override
	public <T> Optional<T> visit(StringVisitable.Visitor<T> visitor) {
		return visitor.accept(contents.asText());
	}

	@Override
	public <T> Optional<T> visit(StringVisitable.StyledVisitor<T> visitor, Style style) {
		return visitor.accept(style.withFont(contents.spriteSource()), OBJECT_REPLACEMENT_CHARACTER);
	}
}
