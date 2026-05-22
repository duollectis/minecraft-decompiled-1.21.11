package net.minecraft.text;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/**
 * Содержимое текстового компонента, представляющее простую строку без форматирования.
 *
 * <p>Является базовым типом содержимого для большинства текстовых компонентов.
 * Константа {@link #EMPTY} используется как синглтон для пустого текста,
 * а {@link Literal} — для непустых строк.</p>
 */
public interface PlainTextContent extends TextContent {

	/**
	 * Codec для сериализации/десериализации через поле {@code "text"}.
	 * Делегирует в {@link #of} для автоматического выбора между {@link #EMPTY} и {@link Literal}.
	 */
	MapCodec<PlainTextContent> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(Codec.STRING.fieldOf("text").forGetter(PlainTextContent::string))
					.apply(instance, PlainTextContent::of)
	);

	/** Синглтон пустого текстового содержимого. */
	PlainTextContent EMPTY = new PlainTextContent() {
		@Override
		public String toString() {
			return "empty";
		}

		@Override
		public String string() {
			return "";
		}
	};

	/**
	 * Создаёт {@link PlainTextContent} для заданной строки.
	 * Возвращает {@link #EMPTY} для пустой строки, иначе — новый {@link Literal}.
	 *
	 * @param string исходная строка
	 * @return соответствующая реализация {@link PlainTextContent}
	 */
	static PlainTextContent of(String string) {
		return string.isEmpty() ? EMPTY : new PlainTextContent.Literal(string);
	}

	/** @return строковое содержимое этого компонента */
	String string();

	@Override
	default MapCodec<PlainTextContent> getCodec() {
		return CODEC;
	}

	/**
	 * Неизменяемый текстовый компонент с конкретным строковым значением.
	 */
	record Literal(String string) implements PlainTextContent {

		@Override
		public <T> Optional<T> visit(StringVisitable.Visitor<T> visitor) {
			return visitor.accept(string);
		}

		@Override
		public <T> Optional<T> visit(StringVisitable.StyledVisitor<T> visitor, Style style) {
			return visitor.accept(style, string);
		}

		@Override
		public String toString() {
			return "literal{" + string + "}";
		}
	}
}
