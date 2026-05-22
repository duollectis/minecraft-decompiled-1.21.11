package net.minecraft.component.type;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipAppender;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.ColorHelper;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
	 * Компонент цвета окрашенного предмета (например, кожаной брони).
	 * Хранит RGB-цвет и предоставляет логику смешивания красителей.
	 */
public record DyedColorComponent(int rgb) implements TooltipAppender {

	public static final Codec<DyedColorComponent>
			CODEC =
			Codecs.RGB.xmap(DyedColorComponent::new, DyedColorComponent::rgb);
	public static final PacketCodec<ByteBuf, DyedColorComponent> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.INTEGER, DyedColorComponent::rgb, DyedColorComponent::new
	);
	// 0xA06050 — стандартный цвет кожаной брони без окраски
	public static final int DEFAULT_COLOR = 0xFFA06050;

	public static int getColor(ItemStack stack, int defaultColor) {
		DyedColorComponent component = stack.get(DataComponentTypes.DYED_COLOR);
		return component != null ? ColorHelper.fullAlpha(component.rgb()) : defaultColor;
	}

	/**
		 * Смешивает текущий цвет предмета с цветами переданных красителей по алгоритму взвешенного среднего.
		 * Яркость итогового цвета масштабируется относительно максимальной компоненты для сохранения насыщенности.
		 *
		 * @param stack стек предмета, который нужно окрасить
		 * @param dyes  список красителей для смешивания
		 * @return новый стек с применённым цветом, или {@link ItemStack#EMPTY} если предмет не красится
		 */
	public static ItemStack setColor(ItemStack stack, List<DyeItem> dyes) {
		if (!stack.isIn(ItemTags.DYEABLE)) {
			return ItemStack.EMPTY;
		}

		ItemStack result = stack.copyWithCount(1);
		int totalRed = 0;
		int totalGreen = 0;
		int totalBlue = 0;
		int totalMax = 0;
		int count = 0;

		DyedColorComponent existing = result.get(DataComponentTypes.DYED_COLOR);
		if (existing != null) {
			int red = ColorHelper.getRed(existing.rgb());
			int green = ColorHelper.getGreen(existing.rgb());
			int blue = ColorHelper.getBlue(existing.rgb());
			totalMax += Math.max(red, Math.max(green, blue));
			totalRed += red;
			totalGreen += green;
			totalBlue += blue;
			count++;
		}

		for (DyeItem dyeItem : dyes) {
			int dyeColor = dyeItem.getColor().getEntityColor();
			int dyeRed = ColorHelper.getRed(dyeColor);
			int dyeGreen = ColorHelper.getGreen(dyeColor);
			int dyeBlue = ColorHelper.getBlue(dyeColor);
			totalMax += Math.max(dyeRed, Math.max(dyeGreen, dyeBlue));
			totalRed += dyeRed;
			totalGreen += dyeGreen;
			totalBlue += dyeBlue;
			count++;
		}

		int avgRed = totalRed / count;
		int avgGreen = totalGreen / count;
		int avgBlue = totalBlue / count;
		float maxAvg = (float) totalMax / count;
		float maxComponent = Math.max(avgRed, Math.max(avgGreen, avgBlue));
		int finalRed = (int) (avgRed * maxAvg / maxComponent);
		int finalGreen = (int) (avgGreen * maxAvg / maxComponent);
		int finalBlue = (int) (avgBlue * maxAvg / maxComponent);
		int finalColor = ColorHelper.getArgb(0, finalRed, finalGreen, finalBlue);
		result.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(finalColor));
		return result;
	}

	@Override
	public void appendTooltip(
			Item.TooltipContext context,
			Consumer<Text> textConsumer,
			TooltipType type,
			ComponentsAccess components
	) {
		if (type.isAdvanced()) {
			textConsumer.accept(Text
					.translatable("item.color", String.format(Locale.ROOT, "#%06X", rgb))
					.formatted(Formatting.GRAY));
		}
		else {
			textConsumer.accept(Text.translatable("item.dyed").formatted(Formatting.GRAY, Formatting.ITALIC));
		}
	}
}
