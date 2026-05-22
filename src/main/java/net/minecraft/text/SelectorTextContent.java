package net.minecraft.text;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Содержимое текстового компонента, разрешающее сущности через селектор и отображающее их имена.
 *
 * <p>При рендеринге выполняет {@link ParsedSelector#selector()} против источника команды,
 * собирает отображаемые имена всех найденных сущностей и объединяет их через {@code separator}.</p>
 */
public record SelectorTextContent(ParsedSelector selector, Optional<Text> separator) implements TextContent {

	public static final MapCodec<SelectorTextContent> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					ParsedSelector.CODEC.fieldOf("selector").forGetter(SelectorTextContent::selector),
					TextCodecs.CODEC.optionalFieldOf("separator").forGetter(SelectorTextContent::separator)
			)
			.apply(instance, SelectorTextContent::new)
	);

	@Override
	public MapCodec<SelectorTextContent> getCodec() {
		return CODEC;
	}

	@Override
	public MutableText parse(@Nullable ServerCommandSource source, @Nullable Entity sender, int depth)
	throws CommandSyntaxException {
		if (source == null) {
			return Text.empty();
		}

		Optional<? extends Text> resolvedSeparator = Texts.parse(source, separator, sender, depth);
		return Texts.join(selector.selector().getEntities(source), resolvedSeparator, Entity::getDisplayName);
	}

	@Override
	public <T> Optional<T> visit(StringVisitable.StyledVisitor<T> visitor, Style style) {
		return visitor.accept(style, selector.raw());
	}

	@Override
	public <T> Optional<T> visit(StringVisitable.Visitor<T> visitor) {
		return visitor.accept(selector.raw());
	}

	@Override
	public String toString() {
		return "pattern{" + selector + "}";
	}
}
