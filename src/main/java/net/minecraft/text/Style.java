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
 * {@code Style}.
 */
public final class Style {

	public static final Style EMPTY = new Style(null, null, null, null, null, null, null, null, null, null, null);
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
		return this.color;
	}

	public @Nullable Integer getShadowColor() {
		return this.shadowColor;
	}

	public boolean isBold() {
		return this.bold == Boolean.TRUE;
	}

	public boolean isItalic() {
		return this.italic == Boolean.TRUE;
	}

	public boolean isStrikethrough() {
		return this.strikethrough == Boolean.TRUE;
	}

	public boolean isUnderlined() {
		return this.underlined == Boolean.TRUE;
	}

	public boolean isObfuscated() {
		return this.obfuscated == Boolean.TRUE;
	}

	public boolean isEmpty() {
		return this == EMPTY;
	}

	public @Nullable ClickEvent getClickEvent() {
		return this.clickEvent;
	}

	public @Nullable HoverEvent getHoverEvent() {
		return this.hoverEvent;
	}

	public @Nullable String getInsertion() {
		return this.insertion;
	}

	public StyleSpriteSource getFont() {
		return (StyleSpriteSource) (this.font != null ? this.font : StyleSpriteSource.DEFAULT);
	}

	private static <T> Style with(Style newStyle, @Nullable T oldAttribute, @Nullable T newAttribute) {
		return oldAttribute != null && newAttribute == null && newStyle.equals(EMPTY) ? EMPTY : newStyle;
	}

	/**
	 * With color.
	 *
	 * @param color color
	 *
	 * @return Style — результат операции
	 */
	public Style withColor(@Nullable TextColor color) {
		return Objects.equals(this.color, color)
		       ? this
		       : with(
				       new Style(
						       color,
						       this.shadowColor,
						       this.bold,
						       this.italic,
						       this.underlined,
						       this.strikethrough,
						       this.obfuscated,
						       this.clickEvent,
						       this.hoverEvent,
						       this.insertion,
						       this.font
				       ),
				       this.color,
				       color
		       );
	}

	/**
	 * With color.
	 *
	 * @param color color
	 *
	 * @return Style — результат операции
	 */
	public Style withColor(@Nullable Formatting color) {
		return this.withColor(color != null ? TextColor.fromFormatting(color) : null);
	}

	/**
	 * With color.
	 *
	 * @param rgbColor rgb color
	 *
	 * @return Style — результат операции
	 */
	public Style withColor(int rgbColor) {
		return this.withColor(TextColor.fromRgb(rgbColor));
	}

	/**
	 * With shadow color.
	 *
	 * @param shadowColor shadow color
	 *
	 * @return Style — результат операции
	 */
	public Style withShadowColor(int shadowColor) {
		return Objects.equals(this.shadowColor, shadowColor)
		       ? this
		       : with(
				       new Style(
						       this.color,
						       shadowColor,
						       this.bold,
						       this.italic,
						       this.underlined,
						       this.strikethrough,
						       this.obfuscated,
						       this.clickEvent,
						       this.hoverEvent,
						       this.insertion,
						       this.font
				       ),
				       this.shadowColor,
				       shadowColor
		       );
	}

	/**
	 * Without shadow.
	 *
	 * @return Style — результат операции
	 */
	public Style withoutShadow() {
		return this.withShadowColor(0);
	}

	/**
	 * With bold.
	 *
	 * @param bold bold
	 *
	 * @return Style — результат операции
	 */
	public Style withBold(@Nullable Boolean bold) {
		return Objects.equals(this.bold, bold)
		       ? this
		       : with(
				       new Style(
						       this.color,
						       this.shadowColor,
						       bold,
						       this.italic,
						       this.underlined,
						       this.strikethrough,
						       this.obfuscated,
						       this.clickEvent,
						       this.hoverEvent,
						       this.insertion,
						       this.font
				       ),
				       this.bold,
				       bold
		       );
	}

	/**
	 * With italic.
	 *
	 * @param italic italic
	 *
	 * @return Style — результат операции
	 */
	public Style withItalic(@Nullable Boolean italic) {
		return Objects.equals(this.italic, italic)
		       ? this
		       : with(
				       new Style(
						       this.color,
						       this.shadowColor,
						       this.bold,
						       italic,
						       this.underlined,
						       this.strikethrough,
						       this.obfuscated,
						       this.clickEvent,
						       this.hoverEvent,
						       this.insertion,
						       this.font
				       ),
				       this.italic,
				       italic
		       );
	}

	/**
	 * With underline.
	 *
	 * @param underline underline
	 *
	 * @return Style — результат операции
	 */
	public Style withUnderline(@Nullable Boolean underline) {
		return Objects.equals(this.underlined, underline)
		       ? this
		       : with(
				       new Style(
						       this.color,
						       this.shadowColor,
						       this.bold,
						       this.italic,
						       underline,
						       this.strikethrough,
						       this.obfuscated,
						       this.clickEvent,
						       this.hoverEvent,
						       this.insertion,
						       this.font
				       ),
				       this.underlined,
				       underline
		       );
	}

	/**
	 * With strikethrough.
	 *
	 * @param strikethrough strikethrough
	 *
	 * @return Style — результат операции
	 */
	public Style withStrikethrough(@Nullable Boolean strikethrough) {
		return Objects.equals(this.strikethrough, strikethrough)
		       ? this
		       : with(
				       new Style(
						       this.color,
						       this.shadowColor,
						       this.bold,
						       this.italic,
						       this.underlined,
						       strikethrough,
						       this.obfuscated,
						       this.clickEvent,
						       this.hoverEvent,
						       this.insertion,
						       this.font
				       ),
				       this.strikethrough,
				       strikethrough
		       );
	}

	/**
	 * With obfuscated.
	 *
	 * @param obfuscated obfuscated
	 *
	 * @return Style — результат операции
	 */
	public Style withObfuscated(@Nullable Boolean obfuscated) {
		return Objects.equals(this.obfuscated, obfuscated)
		       ? this
		       : with(
				       new Style(
						       this.color,
						       this.shadowColor,
						       this.bold,
						       this.italic,
						       this.underlined,
						       this.strikethrough,
						       obfuscated,
						       this.clickEvent,
						       this.hoverEvent,
						       this.insertion,
						       this.font
				       ),
				       this.obfuscated,
				       obfuscated
		       );
	}

	/**
	 * With click event.
	 *
	 * @param clickEvent click event
	 *
	 * @return Style — результат операции
	 */
	public Style withClickEvent(@Nullable ClickEvent clickEvent) {
		return Objects.equals(this.clickEvent, clickEvent)
		       ? this
		       : with(
				       new Style(
						       this.color,
						       this.shadowColor,
						       this.bold,
						       this.italic,
						       this.underlined,
						       this.strikethrough,
						       this.obfuscated,
						       clickEvent,
						       this.hoverEvent,
						       this.insertion,
						       this.font
				       ),
				       this.clickEvent,
				       clickEvent
		       );
	}

	/**
	 * With hover event.
	 *
	 * @param hoverEvent hover event
	 *
	 * @return Style — результат операции
	 */
	public Style withHoverEvent(@Nullable HoverEvent hoverEvent) {
		return Objects.equals(this.hoverEvent, hoverEvent)
		       ? this
		       : with(
				       new Style(
						       this.color,
						       this.shadowColor,
						       this.bold,
						       this.italic,
						       this.underlined,
						       this.strikethrough,
						       this.obfuscated,
						       this.clickEvent,
						       hoverEvent,
						       this.insertion,
						       this.font
				       ),
				       this.hoverEvent,
				       hoverEvent
		       );
	}

	/**
	 * With insertion.
	 *
	 * @param insertion insertion
	 *
	 * @return Style — результат операции
	 */
	public Style withInsertion(@Nullable String insertion) {
		return Objects.equals(this.insertion, insertion)
		       ? this
		       : with(
				       new Style(
						       this.color,
						       this.shadowColor,
						       this.bold,
						       this.italic,
						       this.underlined,
						       this.strikethrough,
						       this.obfuscated,
						       this.clickEvent,
						       this.hoverEvent,
						       insertion,
						       this.font
				       ),
				       this.insertion,
				       insertion
		       );
	}

	/**
	 * With font.
	 *
	 * @param font font
	 *
	 * @return Style — результат операции
	 */
	public Style withFont(@Nullable StyleSpriteSource font) {
		return Objects.equals(this.font, font)
		       ? this
		       : with(
				       new Style(
						       this.color,
						       this.shadowColor,
						       this.bold,
						       this.italic,
						       this.underlined,
						       this.strikethrough,
						       this.obfuscated,
						       this.clickEvent,
						       this.hoverEvent,
						       this.insertion,
						       font
				       ),
				       this.font,
				       font
		       );
	}

	/**
	 * With formatting.
	 *
	 * @param formatting formatting
	 *
	 * @return Style — результат операции
	 */
	public Style withFormatting(Formatting formatting) {
		TextColor textColor = this.color;
		Boolean boolean_ = this.bold;
		Boolean boolean2 = this.italic;
		Boolean boolean3 = this.strikethrough;
		Boolean boolean4 = this.underlined;
		Boolean boolean5 = this.obfuscated;
		switch (formatting) {
			case OBFUSCATED:
				boolean5 = true;
				break;
			case BOLD:
				boolean_ = true;
				break;
			case STRIKETHROUGH:
				boolean3 = true;
				break;
			case UNDERLINE:
				boolean4 = true;
				break;
			case ITALIC:
				boolean2 = true;
				break;
			case RESET:
				return EMPTY;
			default:
				textColor = TextColor.fromFormatting(formatting);
		}

		return new Style(
				textColor,
				this.shadowColor,
				boolean_,
				boolean2,
				boolean4,
				boolean3,
				boolean5,
				this.clickEvent,
				this.hoverEvent,
				this.insertion,
				this.font
		);
	}

	/**
	 * With exclusive formatting.
	 *
	 * @param formatting formatting
	 *
	 * @return Style — результат операции
	 */
	public Style withExclusiveFormatting(Formatting formatting) {
		TextColor textColor = this.color;
		Boolean boolean_ = this.bold;
		Boolean boolean2 = this.italic;
		Boolean boolean3 = this.strikethrough;
		Boolean boolean4 = this.underlined;
		Boolean boolean5 = this.obfuscated;
		switch (formatting) {
			case OBFUSCATED:
				boolean5 = true;
				break;
			case BOLD:
				boolean_ = true;
				break;
			case STRIKETHROUGH:
				boolean3 = true;
				break;
			case UNDERLINE:
				boolean4 = true;
				break;
			case ITALIC:
				boolean2 = true;
				break;
			case RESET:
				return EMPTY;
			default:
				boolean5 = false;
				boolean_ = false;
				boolean3 = false;
				boolean4 = false;
				boolean2 = false;
				textColor = TextColor.fromFormatting(formatting);
		}

		return new Style(
				textColor,
				this.shadowColor,
				boolean_,
				boolean2,
				boolean4,
				boolean3,
				boolean5,
				this.clickEvent,
				this.hoverEvent,
				this.insertion,
				this.font
		);
	}

	/**
	 * With formatting.
	 *
	 * @param formattings formattings
	 *
	 * @return Style — результат операции
	 */
	public Style withFormatting(Formatting... formattings) {
		TextColor textColor = this.color;
		Boolean boolean_ = this.bold;
		Boolean boolean2 = this.italic;
		Boolean boolean3 = this.strikethrough;
		Boolean boolean4 = this.underlined;
		Boolean boolean5 = this.obfuscated;

		for (Formatting formatting : formattings) {
			switch (formatting) {
				case OBFUSCATED:
					boolean5 = true;
					break;
				case BOLD:
					boolean_ = true;
					break;
				case STRIKETHROUGH:
					boolean3 = true;
					break;
				case UNDERLINE:
					boolean4 = true;
					break;
				case ITALIC:
					boolean2 = true;
					break;
				case RESET:
					return EMPTY;
				default:
					textColor = TextColor.fromFormatting(formatting);
			}
		}

		return new Style(
				textColor,
				this.shadowColor,
				boolean_,
				boolean2,
				boolean4,
				boolean3,
				boolean5,
				this.clickEvent,
				this.hoverEvent,
				this.insertion,
				this.font
		);
	}

	/**
	 * With parent.
	 *
	 * @param parent parent
	 *
	 * @return Style — результат операции
	 */
	public Style withParent(Style parent) {
		if (this == EMPTY) {
			return parent;
		}
		else {
			return parent == EMPTY
			       ? this
			       : new Style(
					       this.color != null ? this.color : parent.color,
					       this.shadowColor != null ? this.shadowColor : parent.shadowColor,
					       this.bold != null ? this.bold : parent.bold,
					       this.italic != null ? this.italic : parent.italic,
					       this.underlined != null ? this.underlined : parent.underlined,
					       this.strikethrough != null ? this.strikethrough : parent.strikethrough,
					       this.obfuscated != null ? this.obfuscated : parent.obfuscated,
					       this.clickEvent != null ? this.clickEvent : parent.clickEvent,
					       this.hoverEvent != null ? this.hoverEvent : parent.hoverEvent,
					       this.insertion != null ? this.insertion : parent.insertion,
					       this.font != null ? this.font : parent.font
			       );
		}
	}

	@Override
	public String toString() {
		final StringBuilder stringBuilder = new StringBuilder("{");

		/**
		 * {@code Writer}.
		 */
		class Writer {

			private boolean shouldAppendComma;

			private void appendComma() {
				if (this.shouldAppendComma) {
					stringBuilder.append(',');
				}

				this.shouldAppendComma = true;
			}

			void append(String key, @Nullable Boolean value) {
				if (value != null) {
					this.appendComma();
					if (!value) {
						stringBuilder.append('!');
					}

					stringBuilder.append(key);
				}
			}

			void append(String key, @Nullable Object value) {
				if (value != null) {
					this.appendComma();
					stringBuilder.append(key);
					stringBuilder.append('=');
					stringBuilder.append(value);
				}
			}
		}

		Writer writer = new Writer();
		writer.append("color", this.color);
		writer.append("shadowColor", this.shadowColor);
		writer.append("bold", this.bold);
		writer.append("italic", this.italic);
		writer.append("underlined", this.underlined);
		writer.append("strikethrough", this.strikethrough);
		writer.append("obfuscated", this.obfuscated);
		writer.append("clickEvent", this.clickEvent);
		writer.append("hoverEvent", this.hoverEvent);
		writer.append("insertion", this.insertion);
		writer.append("font", this.font);
		stringBuilder.append("}");
		return stringBuilder.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		else {
			return !(o instanceof Style style)
			       ? false
			       : this.bold == style.bold
			         && Objects.equals(this.getColor(), style.getColor())
			         && Objects.equals(this.getShadowColor(), style.getShadowColor())
			         && this.italic == style.italic
			         && this.obfuscated == style.obfuscated
			         && this.strikethrough == style.strikethrough
			         && this.underlined == style.underlined
			         && Objects.equals(this.clickEvent, style.clickEvent)
			         && Objects.equals(this.hoverEvent, style.hoverEvent)
			         && Objects.equals(this.insertion, style.insertion)
			         && Objects.equals(this.font, style.font);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(
				this.color,
				this.shadowColor,
				this.bold,
				this.italic,
				this.underlined,
				this.strikethrough,
				this.obfuscated,
				this.clickEvent,
				this.hoverEvent,
				this.insertion
		);
	}

	/**
	 * {@code Codecs}.
	 */
	public static class Codecs {

		public static final MapCodec<Style> MAP_CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						                    TextColor.CODEC.optionalFieldOf("color").forGetter(style -> Optional.ofNullable(style.color)),
						                    net.minecraft.util.dynamic.Codecs.ARGB
								                    .optionalFieldOf("shadow_color")
								                    .forGetter(style -> Optional.ofNullable(style.shadowColor)),
						                    Codec.BOOL.optionalFieldOf("bold").forGetter(style -> Optional.ofNullable(style.bold)),
						                    Codec.BOOL.optionalFieldOf("italic").forGetter(style -> Optional.ofNullable(style.italic)),
						                    Codec.BOOL
								                    .optionalFieldOf("underlined")
								                    .forGetter(style -> Optional.ofNullable(style.underlined)),
						                    Codec.BOOL
								                    .optionalFieldOf("strikethrough")
								                    .forGetter(style -> Optional.ofNullable(style.strikethrough)),
						                    Codec.BOOL
								                    .optionalFieldOf("obfuscated")
								                    .forGetter(style -> Optional.ofNullable(style.obfuscated)),
						                    ClickEvent.CODEC
								                    .optionalFieldOf("click_event")
								                    .forGetter(style -> Optional.ofNullable(style.clickEvent)),
						                    HoverEvent.CODEC
								                    .optionalFieldOf("hover_event")
								                    .forGetter(style -> Optional.ofNullable(style.hoverEvent)),
						                    Codec.STRING
								                    .optionalFieldOf("insertion")
								                    .forGetter(style -> Optional.ofNullable(style.insertion)),
						                    StyleSpriteSource.FONT_CODEC
								                    .optionalFieldOf("font")
								                    .forGetter(style -> Optional.ofNullable(style.font))
				                    )
				                    .apply(instance, Style::of)
		);
		public static final Codec<Style> CODEC = MAP_CODEC.codec();
		public static final PacketCodec<RegistryByteBuf, Style>
				PACKET_CODEC =
				PacketCodecs.unlimitedRegistryCodec(CODEC);
	}
}
