package net.minecraft.text;

import com.google.common.collect.Lists;
import com.mojang.brigadier.Message;
import com.mojang.datafixers.util.Either;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.object.TextObjectContents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.*;

/**
 * {@code Text}.
 */
public interface Text extends Message, StringVisitable {

	Style getStyle();

	TextContent getContent();

	@Override
	default String getString() {
		return StringVisitable.super.getString();
	}

	default String asTruncatedString(int length) {
		StringBuilder stringBuilder = new StringBuilder();
		this.visit(string -> {
			int j = length - stringBuilder.length();
			if (j <= 0) {
				return TERMINATE_VISIT;
			}
			else {
				stringBuilder.append(string.length() <= j ? string : string.substring(0, j));
				return Optional.empty();
			}
		});
		return stringBuilder.toString();
	}

	List<Text> getSiblings();

	default @Nullable String getLiteralString() {
		return this.getContent() instanceof PlainTextContent plainTextContent && this.getSiblings().isEmpty() && this
				.getStyle()
				.isEmpty()
		       ? plainTextContent.string()
		       : null;
	}

	default MutableText copyContentOnly() {
		return MutableText.of(this.getContent());
	}

	default MutableText copy() {
		return new MutableText(this.getContent(), new ArrayList<>(this.getSiblings()), this.getStyle());
	}

	OrderedText asOrderedText();

	@Override
	default <T> Optional<T> visit(StringVisitable.StyledVisitor<T> styledVisitor, Style style) {
		Style style2 = this.getStyle().withParent(style);
		Optional<T> optional = this.getContent().visit(styledVisitor, style2);
		if (optional.isPresent()) {
			return optional;
		}
		else {
			for (Text text : this.getSiblings()) {
				Optional<T> optional2 = text.visit(styledVisitor, style2);
				if (optional2.isPresent()) {
					return optional2;
				}
			}

			return Optional.empty();
		}
	}

	@Override
	default <T> Optional<T> visit(StringVisitable.Visitor<T> visitor) {
		Optional<T> optional = this.getContent().visit(visitor);
		if (optional.isPresent()) {
			return optional;
		}
		else {
			for (Text text : this.getSiblings()) {
				Optional<T> optional2 = text.visit(visitor);
				if (optional2.isPresent()) {
					return optional2;
				}
			}

			return Optional.empty();
		}
	}

	default List<Text> withoutStyle() {
		return this.getWithStyle(Style.EMPTY);
	}

	default List<Text> getWithStyle(Style style) {
		List<Text> list = Lists.newArrayList();
		this.visit(
				(styleOverride, text) -> {
					if (!text.isEmpty()) {
						list.add(literal(text).fillStyle(styleOverride));
					}

					return Optional.empty();
				}, style
		);
		return list;
	}

	default boolean contains(Text text) {
		if (this.equals(text)) {
			return true;
		}
		else {
			List<Text> list = this.withoutStyle();
			List<Text> list2 = text.getWithStyle(this.getStyle());
			return Collections.indexOfSubList(list, list2) != -1;
		}
	}

	static Text of(@Nullable String string) {
		return (Text) (string != null ? literal(string) : ScreenTexts.EMPTY);
	}

	static MutableText literal(String string) {
		return MutableText.of(PlainTextContent.of(string));
	}

	static MutableText translatable(String key) {
		return MutableText.of(new TranslatableTextContent(key, null, TranslatableTextContent.EMPTY_ARGUMENTS));
	}

	static MutableText translatable(String key, Object... args) {
		return MutableText.of(new TranslatableTextContent(key, null, args));
	}

	static MutableText stringifiedTranslatable(String key, Object... args) {
		for (int i = 0; i < args.length; i++) {
			Object object = args[i];
			if (!TranslatableTextContent.isPrimitive(object) && !(object instanceof Text)) {
				args[i] = String.valueOf(object);
			}
		}

		return translatable(key, args);
	}

	static MutableText translatableWithFallback(String key, @Nullable String fallback) {
		return MutableText.of(new TranslatableTextContent(key, fallback, TranslatableTextContent.EMPTY_ARGUMENTS));
	}

	static MutableText translatableWithFallback(String key, @Nullable String fallback, Object... args) {
		return MutableText.of(new TranslatableTextContent(key, fallback, args));
	}

	static MutableText empty() {
		return MutableText.of(PlainTextContent.EMPTY);
	}

	static MutableText keybind(String string) {
		return MutableText.of(new KeybindTextContent(string));
	}

	static MutableText nbt(String rawPath, boolean interpret, Optional<Text> separator, NbtDataSource dataSource) {
		return MutableText.of(new NbtTextContent(rawPath, interpret, separator, dataSource));
	}

	static MutableText score(ParsedSelector selector, String objective) {
		return MutableText.of(new ScoreTextContent(Either.left(selector), objective));
	}

	static MutableText score(String name, String objective) {
		return MutableText.of(new ScoreTextContent(Either.right(name), objective));
	}

	static MutableText selector(ParsedSelector selector, Optional<Text> separator) {
		return MutableText.of(new SelectorTextContent(selector, separator));
	}

	static MutableText object(TextObjectContents object) {
		return MutableText.of(new ObjectTextContent(object));
	}

	static Text of(Date date) {
		return literal(date.toString());
	}

	static Text of(Message message) {
		return (Text) (message instanceof Text text ? text : literal(message.getString()));
	}

	static Text of(UUID uuid) {
		return literal(uuid.toString());
	}

	static Text of(Identifier id) {
		return literal(id.toString());
	}

	static Text of(ChunkPos pos) {
		return literal(pos.toString());
	}

	static Text of(URI uri) {
		return literal(uri.toString());
	}
}
