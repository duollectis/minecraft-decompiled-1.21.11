package net.minecraft.client.color.block;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.jspecify.annotations.Nullable;

/**
 * Функциональный интерфейс для определения цвета блока в зависимости от его состояния,
 * окружающего биома и индекса тинта (tint layer). Используется системой {@link BlockColors}
 * для окрашивания блоков на стороне клиента (трава, листья, вода и т.д.).
 */
@Environment(EnvType.CLIENT)
public interface BlockColorProvider {

	/**
	 * Возвращает ARGB-цвет для указанного состояния блока и слоя тинта.
	 * <p>
	 * Если {@code world} или {@code pos} равны {@code null}, реализация обязана вернуть
	 * дефолтный цвет (без обращения к биому), так как этот метод вызывается и для
	 * рендера частиц, где контекст мира недоступен.
	 *
	 * @param state     состояние блока, для которого запрашивается цвет
	 * @param world     вид мира для чтения данных биома; может быть {@code null}
	 * @param pos       позиция блока в мире; может быть {@code null}
	 * @param tintIndex индекс слоя тинта из модели блока (обычно 0)
	 * @return ARGB-цвет в виде упакованного int
	 */
	int getColor(BlockState state, @Nullable BlockRenderView world, @Nullable BlockPos pos, int tintIndex);
}
