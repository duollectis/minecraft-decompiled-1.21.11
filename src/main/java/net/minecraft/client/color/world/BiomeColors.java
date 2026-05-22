package net.minecraft.client.color.world;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.ColorResolver;

/**
 * Утилитарный класс для получения биомных цветов в точке мира на стороне клиента.
 * <p>
 * Каждый метод делегирует вызов в {@link BlockRenderView#getColor(BlockPos, ColorResolver)},
 * который усредняет цвет по соседним биомам в радиусе сглаживания (blend radius).
 * Публичные {@link ColorResolver}-константы используются также в системе частиц
 * и других местах, где нужен прямой доступ к резолверу без вызова метода.
 */
@Environment(EnvType.CLIENT)
public class BiomeColors {

	/** Резолвер цвета травы: учитывает температуру и влажность биома. */
	public static final ColorResolver GRASS_COLOR = Biome::getGrassColorAt;

	/** Резолвер цвета листвы: возвращает фиксированный цвет листвы биома. */
	public static final ColorResolver FOLIAGE_COLOR = (biome, x, z) -> biome.getFoliageColor();

	/** Резолвер цвета сухой листвы (опавшие листья): возвращает цвет сухой листвы биома. */
	public static final ColorResolver DRY_FOLIAGE_COLOR = (biome, x, z) -> biome.getDryFoliageColor();

	/** Резолвер цвета воды: возвращает цвет воды биома. */
	public static final ColorResolver WATER_COLOR = (biome, x, z) -> biome.getWaterColor();

	/**
	 * Возвращает усреднённый цвет травы в указанной позиции с учётом соседних биомов.
	 *
	 * @param world вид мира для чтения данных биома
	 * @param pos   позиция блока
	 * @return ARGB-цвет травы
	 */
	public static int getGrassColor(BlockRenderView world, BlockPos pos) {
		return world.getColor(pos, GRASS_COLOR);
	}

	/**
	 * Возвращает усреднённый цвет листвы в указанной позиции с учётом соседних биомов.
	 *
	 * @param world вид мира для чтения данных биома
	 * @param pos   позиция блока
	 * @return ARGB-цвет листвы
	 */
	public static int getFoliageColor(BlockRenderView world, BlockPos pos) {
		return world.getColor(pos, FOLIAGE_COLOR);
	}

	/**
	 * Возвращает усреднённый цвет сухой листвы в указанной позиции с учётом соседних биомов.
	 *
	 * @param world вид мира для чтения данных биома
	 * @param pos   позиция блока
	 * @return ARGB-цвет сухой листвы
	 */
	public static int getDryFoliageColor(BlockRenderView world, BlockPos pos) {
		return world.getColor(pos, DRY_FOLIAGE_COLOR);
	}

	/**
	 * Возвращает усреднённый цвет воды в указанной позиции с учётом соседних биомов.
	 *
	 * @param world вид мира для чтения данных биома
	 * @param pos   позиция блока
	 * @return ARGB-цвет воды
	 */
	public static int getWaterColor(BlockRenderView world, BlockPos pos) {
		return world.getColor(pos, WATER_COLOR);
	}
}
