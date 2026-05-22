package net.minecraft.text;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Formatting;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Иммутабельный стиль текстового компонента Minecraft.
 * Хранит все визуальные атрибуты: цвет, жирность, курсив, подчёркивание,
 * зачёркивание, обфускацию, тень, шрифт, а также интерактивные события
 * (клик, наведение) и строку вставки.
 *
 * <p>Все методы {@code with*} возвращают новый экземпляр с изменённым атрибутом,
 * не модифицируя текущий. Если результат эквивалентен {@link #EMPTY}, возвращается
 * именно {@code EMPTY} для экономии памяти.
 */
public final class Style {

	public static final Style EMPTY = new Style(
		null, null, null, null, null, null, null, null, null, null, null
	);

	public static final int DEFAULT_COLOR = 0;

	final @Nullable TextColor color;
	final @Nullable Integer shadowColor;
	final @Nullable Boolean bold;
	final @Nullable Boolean italic;
	final @Nullable Boolean underlined;
	final @Nullable Boolean strikethrough;
	final @Nullable Boolean obfuscated;
	final @Nullable ClickEvent clickEvent;
	final @Nullable HoverEvent hoverEvent;
	final @Nullable String insertion;
	final @Nullable StyleSpriteSource font;

	private Style(
		@Nullable TextColor color,
		@Nullable Integer shadowColor,
		@Nullable Boolean bold,
		@Nullable Boolean italic,
		@Nullable Boolean underlined,
		@Nullable Boolean strikethrough,
		@Nullable Boolean obfuscated,
		@Nullable ClickEvent clickEvent,
		@Nullable HoverEvent hoverEvent,
		@Nullable String insertion,
		@Nullable StyleSpriteSource font
	) {
		this.color = color;
		this.shadowColor = shadowColor;
		this.bold = bold;
		this.italic = italic;
		this.underlined = underlined;
		this.strikethrough = strikethrough;
		this.obfuscated = obfuscated;
		this.clickEvent = clickEvent;
		this.hoverEvent = hoverEvent;
		this.insertion = insertion;
		this.font = font;
	}

	public @Nullable TextColor getColor() {
		return color;
	}

	public @Nullable Integer getShadowColor() {
		return shadowColor;
	}

	public boolean isBold() {
		return bold == Boolean.TRUE;
	}

	public boolean isItalic() {
		return italic == Boolean.TRUE;
	}

	public boolean isStrikethrough() {
		return strikethrough == Boolean.TRUE;
	}

	public boolean isUnderlined() {
		return underlined == Boolean.TRUE;
	}

	public boolean isObfuscated() {
		return obfuscated == Boolean.TRUE;
	}

	public boolean isEmpty() {
		return this == EMPTY;
	}

	public @Nullable ClickEvent getClickEvent() {
		return clickEvent;
	}

	public @Nullable HoverEvent getHoverEvent() {
		return hoverEvent;
	}

	public @Nullable String getInsertion() {
		return insertion;
	}

	public StyleSpriteSource getFont() {
		return font != null ? font : StyleSpriteSource.DEFAULT;
	}

	/**
	 * Вспомогательный метод: если старый атрибут был задан, а новый — null,
	 * и результирующий стиль пустой, возвращает {@link #EMPTY} вместо нового объекта.
	 */
	private static <T> Style with(Style newStyle, @Nullable T oldAttribute, @Nullable T newAttribute) {
		return oldAttribute != null && newAttribute == null && newStyle.equals(EMPTY) ? EMPTY : newStyle;
	}

	public Style withColor(@Nullable TextColor color) {
		return Objects.equals(this.color, color)
			? this
			: with(
				new Style(
					color, shadowColor, bold, italic, underlined,
					strikethrough, obfuscated, clickEvent, hoverEvent, insertion, font
				),
				this.color, color
			);
	}

	public Style withColor(@Nullable Formatting color) {
		return withColor(color != null ? TextColor.fromFormatting(color) : null);
	}

	public Style withColor(int rgbColor) {
		return withColor(TextColor.fromRgb(rgbColor));
	}

	public Style withShadowColor(int shadowColor) {
		return Objects.equals(this.shadowColor, shadowColor)
			? this
			: with(
				new Style(
					color, shadowColor, bold, italic, underlined,
					strikethrough, obfuscated, clickEvent, hoverEvent, insertion, font
				),
				this.shadowColor, shadowColor
			);
	}

	public Style withoutShadow() {
		return withShadowColor(0);
	}

	public Style withBold(@Nullable Boolean bold) {
		return Objects.equals(this.bold, bold)
			? this
			: with(
				new Style(
					color, shadowColor, bold, italic, underlined,
					strikethrough, obfuscated, clickEvent, hoverEvent, insertion, font
				),
				this.bold, bold
			);
	}

	public Style withItalic(@Nullable Boolean italic) {
		return Objects.equals(this.italic, italic)
			? this
			: with(
				new Style(
					color, shadowColor, bold, italic, underlined,
					strikethrough, obfuscated, clickEvent, hoverEvent, insertion, font
				),
				this.italic, italic
			);
	}

	public Style withUnderline(@Nullable Boolean underline) {
		return Objects.equals(underlined, underline)
			? this
			: with(
				new Style(
					color, shadowColor, bold, italic, underline,
					strikethrough, obfuscated, clickEvent, hoverEvent, insertion, font
				),
				underlined, underline
			);
	}

	public Style withStrikethrough(@Nullable Boolean strikethrough) {
		return Objects.equals(this.strikethrough, strikethrough)
			? this
			: with(
				new Style(
					color, shadowColor, bold, italic, underlined,
					strikethrough, obfuscated, clickEvent, hoverEvent, insertion, font
				),
				this.strikethrough, strikethrough
			);
	}

	public Style withObfuscated(@Nullable Boolean obfuscated) {
		return Objects.equals(this.obfuscated, obfuscated)
			? this
			: with(
				new Style(
					color, shadowColor, bold, italic, underlined,
					strikethrough, obfuscated, clickEvent, hoverEvent, insertion, font
				),
				this.obfuscated, obfuscated
			);
	}

	public Style withClickEvent(@Nullable ClickEvent clickEvent) {
		return Objects.equals(this.clickEvent, clickEvent)
			? this
			: with(
				new Style(
					color, shadowColor, bold, italic, underlined,
					strikethrough, obfuscated, clickEvent, hoverEvent, insertion, font
				),
				this.clickEvent, clickEvent
			);
	}

	public Style withHoverEvent(@Nullable HoverEvent hoverEvent) {
		return Objects.equals(this.hoverEvent, hoverEvent)
			? this
			: with(
				new Style(
					color, shadowColor, bold, italic, underlined,
					strikethrough, obfuscated, clickEvent, hoverEvent, insertion, font
				),
				this.hoverEvent, hoverEvent
			);
	}

	public Style withInsertion(@Nullable String insertion) {
		return Objects.equals(this.insertion, insertion)
			? this
			: with(
				new Style(
					color, shadowColor, bold, italic, underlined,
					strikethrough, obfuscated, clickEvent, hoverEvent, insertion, font
				),
				this.insertion, insertion
			);
	}

	public Style withFont(@Nullable StyleSpriteSource font) {
		return Objects.equals(this.font, font)
			? this
			: with(
				new Style(
					color, shadowColor, bold, italic, underlined,
					strikethrough, obfuscated, clickEvent, hoverEvent, insertion, font
				),
				this.font, font
			);
	}

	/**
	 * Применяет одно форматирование к стилю.
	 * {@link Formatting#RESET} сбрасывает стиль до {@link #EMPTY}.
	 * Цветовые форматирования устанавливают цвет, не затрагивая флаги.
	 */
	public Style withFormatting(Formatting formatting) {
		TextColor newColor = color;
		Boolean newBold = bold;
		Boolean newItalic = italic;
		Boolean newStrikethrough = strikethrough;
		Boolean newUnderlined = underlined;
		Boolean newObfuscated = obfuscated;

		switch (formatting) {
			case OBFUSCATED -> newObfuscated = true;
			case BOLD -> newBold = true;
			case STRIKETHROUGH -> newStrikethrough = true;
			case UNDERLINE -> newUnderlined = true;
			case ITALIC -> newItalic = true;
			case RESET -> { return EMPTY; }
			default -> newColor = TextColor.fromFormatting(formatting);
		}

		return new Style(
			newColor, shadowColor, newBold, newItalic, newUnderlined,
			newStrikethrough, newObfuscated, clickEvent, hoverEvent, insertion, font
		);
	}

	/**
	 * Применяет форматирование эксклюзивно: при установке цвета сбрасывает все флаги
	 * (жирность, курсив и т.д.), чтобы цвет был единственным активным атрибутом.
	 */
	public Style withExclusiveFormatting(Formatting formatting) {
		TextColor newColor = color;
		Boolean newBold = bold;
		Boolean newItalic = italic;
		Boolean newStrikethrough = strikethrough;
		Boolean newUnderlined = underlined;
		Boolean newObfuscated = obfuscated;

		switch (formatting) {
			case OBFUSCATED -> newObfuscated = true;
			case BOLD -> newBold = true;
			case STRIKETHROUGH -> newStrikethrough = true;
			case UNDERLINE -> newUnderlined = true;
			case ITALIC -> newItalic = true;
			case RESET -> { return EMPTY; }
			default -> {
				newObfuscated = false;
				newBold = false;
				newStrikethrough = false;
				newUnderlined = false;
				newItalic = false;
				newColor = TextColor.fromFormatting(formatting);
			}
		}

		return new Style(
			newColor, shadowColor, newBold, newItalic, newUnderlined,
			newStrikethrough, newObfuscated, clickEvent, hoverEvent, insertion, font
		);
	}

	/**
	 * Последовательно применяет несколько форматирований.
	 * При встрече {@link Formatting#RESET} немедленно возвращает {@link #EMPTY}.
	 */
	public Style withFormatting(Formatting... formattings) {
		TextColor newColor = color;
		Boolean newBold = bold;
		Boolean newItalic = italic;
		Boolean newStrikethrough = strikethrough;
		Boolean newUnderlined = underlined;
		Boolean newObfuscated = obfuscated;

		for (Formatting formatting : formattings) {
			switch (formatting) {
				case OBFUSCATED -> newObfuscated = true;
				case BOLD -> newBold = true;
				case STRIKETHROUGH -> newStrikethrough = true;
				case UNDERLINE -> newUnderlined = true;
				case ITALIC -> newItalic = true;
				case RESET -> { return EMPTY; }
				default -> newColor = TextColor.fromFormatting(formatting);
			}
		}

		return new Style(
			newColor, shadowColor, newBold, newItalic, newUnderlined,
			newStrikethrough, newObfuscated, clickEvent, hoverEvent, insertion, font
		);
	}

	/**
	 * Наследует атрибуты из родительского стиля для всех незаданных полей.
	 * Если текущий стиль — {@link #EMPTY}, возвращает родительский.
	 * Если родительский — {@link #EMPTY}, возвращает текущий.
	 */
	public Style withParent(Style parent) {
		if (this == EMPTY) {
			return parent;
		}

		return parent == EMPTY
			? this
			: new Style(
				color != null ? color : parent.color,
				shadowColor != null ? shadowColor : parent.shadowColor,
				bold != null ? bold : parent.bold,
				italic != null ? italic : parent.italic,
				underlined != null ? underlined : parent.underlined,
				strikethrough != null ? strikethrough : parent.strikethrough,
				obfuscated != null ? obfuscated : parent.obfuscated,
				clickEvent != null ? clickEvent : parent.clickEvent,
				hoverEvent != null ? hoverEvent : parent.hoverEvent,
				insertion != null ? insertion : parent.insertion,
				font != null ? font : parent.font
			);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder("{");

		class Writer {

			private boolean shouldAppendComma;

			private void appendComma() {
				if (shouldAppendComma) {
					builder.append(',');
				}

				shouldAppendComma = true;
			}

			void append(String key, @Nullable Boolean value) {
				if (value == null) {
					return;
				}

				appendComma();

				if (!value) {
					builder.append('!');
				}

				builder.append(key);
			}

			void append(String key, @Nullable Object value) {
				if (value == null) {
					return;
				}

				appendComma();
				builder.append(key);
				builder.append('=');
				builder.append(value);
			}
		}

		Writer writer = new Writer();
		writer.append("color", color);
		writer.append("shadowColor", shadowColor);
		writer.append("bold", bold);
		writer.append("italic", italic);
		writer.append("underlined", underlined);
		writer.append("strikethrough", strikethrough);
		writer.append("obfuscated", obfuscated);
		writer.append("clickEvent", clickEvent);
		writer.append("hoverEvent", hoverEvent);
		writer.append("insertion", insertion);
		writer.append("font", font);
		builder.append("}");

		return builder.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (!(o instanceof Style other)) {
			return false;
		}

		return bold == other.bold
			&& Objects.equals(color, other.color)
			&& Objects.equals(shadowColor, other.shadowColor)
			&& italic == other.italic
			&& obfuscated == other.obfuscated
			&& strikethrough == other.strikethrough
			&& underlined == other.underlined
			&& Objects.equals(clickEvent, other.clickEvent)
			&& Objects.equals(hoverEvent, other.hoverEvent)
			&& Objects.equals(insertion, other.insertion)
			&& Objects.equals(font, other.font);
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			color, shadowColor, bold, italic, underlined,
			strikethrough, obfuscated, clickEvent, hoverEvent, insertion
		);
	}

	/**
	 * Кодеки для сериализации {@link Style} через DFU и сетевые пакеты.
	 */
	public static class Codecs {

		public static final MapCodec<Style> MAP_CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
				TextColor.CODEC.optionalFieldOf("color")
					.forGetter(style -> Optional.ofNullable(style.color)),
				net.minecraft.util.dynamic.Codecs.ARGB.optionalFieldOf("shadow_color")
					.forGetter(style -> Optional.ofNullable(style.shadowColor)),
				Codec.BOOL.optionalFieldOf("bold")
					.forGetter(style -> Optional.ofNullable(style.bold)),
				Codec.BOOL.optionalFieldOf("italic")
					.forGetter(style -> Optional.ofNullable(style.italic)),
				Codec.BOOL.optionalFieldOf("underlined")
					.forGetter(style -> Optional.ofNullable(style.underlined)),
				Codec.BOOL.optionalFieldOf("strikethrough")
					.forGetter(style -> Optional.ofNullable(style.strikethrough)),
				Codec.BOOL.optionalFieldOf("obfuscated")
					.forGetter(style -> Optional.ofNullable(style.obfuscated)),
				ClickEvent.CODEC.optionalFieldOf("click_event")
					.forGetter(style -> Optional.ofNullable(style.clickEvent)),
				HoverEvent.CODEC.optionalFieldOf("hover_event")
					.forGetter(style -> Optional.ofNullable(style.hoverEvent)),
				Codec.STRING.optionalFieldOf("insertion")
					.forGetter(style -> Optional.ofNullable(style.insertion)),
				StyleSpriteSource.FONT_CODEC.optionalFieldOf("font")
					.forGetter(style -> Optional.ofNullable(style.font))
			).apply(instance, Style::of)
		);

		public static final Codec<Style> CODEC = MAP_CODEC.codec();
		public static final PacketCodec<RegistryByteBuf, Style> PACKET_CODEC =
			PacketCodecs.unlimitedRegistryCodec(CODEC);
	}

	private static Style of(
		Optional<TextColor> color,
		Optional<Integer> shadowColor,
		Optional<Boolean> bold,
		Optional<Boolean> italic,
		Optional<Boolean> underlined,
		Optional<Boolean> strikethrough,
		Optional<Boolean> obfuscated,
		Optional<ClickEvent> clickEvent,
		Optional<HoverEvent> hoverEvent,
		Optional<String> insertion,
		Optional<StyleSpriteSource> font
	) {
		Style style = new Style(
			color.orElse(null),
			shadowColor.orElse(null),
			bold.orElse(null),
			italic.orElse(null),
			underlined.orElse(null),
			strikethrough.orElse(null),
			obfuscated.orElse(null),
			clickEvent.orElse(null),
			hoverEvent.orElse(null),
			insertion.orElse(null),
			font.orElse(null)
		);
		return style.equals(EMPTY) ? EMPTY : style;
	}
}
