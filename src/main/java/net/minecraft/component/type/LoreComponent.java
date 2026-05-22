package net.minecraft.component.type;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipAppender;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

import java.util.List;
import java.util.function.Consumer;

/**
	 * Компонент описания предмета (лора). Хранит список строк текста, отображаемых
	 * в тултипе предмета курсивным тёмно-фиолетовым шрифтом.
	 */
public record LoreComponent(List<Text> lines, List<Text> styledLines) implements TooltipAppender {

	public static final LoreComponent DEFAULT = new LoreComponent(List.of());
	public static final int MAX_LORES = 256;
	private static final Style STYLE = Style.EMPTY.withColor(Formatting.DARK_PURPLE).withItalic(true);
	public static final Codec<LoreComponent>
			CODEC =
			TextCodecs.CODEC.sizeLimitedListOf(MAX_LORES).xmap(LoreComponent::new, LoreComponent::lines);
	public static final PacketCodec<RegistryByteBuf, LoreComponent> PACKET_CODEC = TextCodecs.REGISTRY_PACKET_CODEC
			.collect(PacketCodecs.toList(MAX_LORES))
			.xmap(LoreComponent::new, LoreComponent::lines);

	public LoreComponent(List<Text> lines) {
		this(lines, Lists.transform(lines, style -> Texts.withStyle(style, STYLE)));
	}

	public LoreComponent(List<Text> lines, List<Text> styledLines) {
		if (lines.size() > MAX_LORES) {
			throw new IllegalArgumentException("Got " + lines.size() + " lines, but maximum is " + MAX_LORES);
		}

		this.lines = lines;
		this.styledLines = styledLines;
	}

	/**
		 * Возвращает новый компонент с добавленной строкой лора в конец списка.
		 *
		 * @param line добавляемая строка текста
		 * @return новый {@code LoreComponent} с расширенным списком строк
		 */
	public LoreComponent with(Text line) {
		return new LoreComponent(Util.withAppended(lines, line));
	}

	@Override
	public void appendTooltip(
			Item.TooltipContext context,
			Consumer<Text> textConsumer,
			TooltipType type,
			ComponentsAccess components
	) {
		this.styledLines.forEach(textConsumer);
	}
}
