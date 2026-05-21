package net.minecraft.text;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.text.object.TextObjectContentTypes;
import net.minecraft.text.object.TextObjectContents;

import java.util.Optional;

/**
 * {@code ObjectTextContent}.
 */
public record ObjectTextContent(TextObjectContents contents) implements TextContent {

	private static final String REPLACEMENT = Character.toString('￼');
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
		return visitor.accept(this.contents.asText());
	}

	@Override
	public <T> Optional<T> visit(StringVisitable.StyledVisitor<T> visitor, Style style) {
		return visitor.accept(style.withFont(this.contents.spriteSource()), REPLACEMENT);
	}
}
