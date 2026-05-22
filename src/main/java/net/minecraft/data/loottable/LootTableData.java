package net.minecraft.data.loottable;

import com.google.common.collect.Maps;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemConvertible;
import net.minecraft.util.DyeColor;

import java.util.Map;

/**
 * Вспомогательные данные для генераторов таблиц лута.
 * Содержит общие справочные таблицы, используемые несколькими генераторами.
 */
public interface LootTableData {

	/**
	 * Маппинг цвета красителя на соответствующий блок шерсти.
	 * Используется генераторами лута овец для определения выпадающей шерсти по цвету.
	 */
	Map<DyeColor, ItemConvertible> WOOL_FROM_DYE_COLOR = Maps.newEnumMap(
		Map.ofEntries(
			Map.entry(DyeColor.WHITE, Blocks.WHITE_WOOL),
			Map.entry(DyeColor.ORANGE, Blocks.ORANGE_WOOL),
			Map.entry(DyeColor.MAGENTA, Blocks.MAGENTA_WOOL),
			Map.entry(DyeColor.LIGHT_BLUE, Blocks.LIGHT_BLUE_WOOL),
			Map.entry(DyeColor.YELLOW, Blocks.YELLOW_WOOL),
			Map.entry(DyeColor.LIME, Blocks.LIME_WOOL),
			Map.entry(DyeColor.PINK, Blocks.PINK_WOOL),
			Map.entry(DyeColor.GRAY, Blocks.GRAY_WOOL),
			Map.entry(DyeColor.LIGHT_GRAY, Blocks.LIGHT_GRAY_WOOL),
			Map.entry(DyeColor.CYAN, Blocks.CYAN_WOOL),
			Map.entry(DyeColor.PURPLE, Blocks.PURPLE_WOOL),
			Map.entry(DyeColor.BLUE, Blocks.BLUE_WOOL),
			Map.entry(DyeColor.BROWN, Blocks.BROWN_WOOL),
			Map.entry(DyeColor.GREEN, Blocks.GREEN_WOOL),
			Map.entry(DyeColor.RED, Blocks.RED_WOOL),
			Map.entry(DyeColor.BLACK, Blocks.BLACK_WOOL)
		)
	);
}
