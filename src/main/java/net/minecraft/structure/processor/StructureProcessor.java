package net.minecraft.structure.processor;

import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.WorldView;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Базовый класс процессора структур. Процессор применяется к каждому блоку шаблона
 * во время его размещения в мире, позволяя модифицировать, заменять или удалять блоки.
 * <p>
 * Метод {@link #process} вызывается для каждого блока индивидуально,
 * а {@link #reprocess} — для всего списка блоков сразу (используется в {@link CappedStructureProcessor}).
 */
public abstract class StructureProcessor {

	/**
	 * Обрабатывает один блок шаблона. Возвращает {@code null}, чтобы пропустить размещение блока.
	 *
	 * @param world             мир, в который размещается структура
	 * @param pos               позиция начала структуры
	 * @param pivot             опорная точка для трансформаций
	 * @param originalBlockInfo исходная информация о блоке из шаблона (до трансформаций)
	 * @param currentBlockInfo  текущая информация о блоке (после трансформаций предыдущих процессоров)
	 * @param data              данные о размещении структуры
	 * @return модифицированная информация о блоке, или {@code null} для пропуска блока
	 */
	public StructureTemplate.@Nullable StructureBlockInfo process(
		WorldView world,
		BlockPos pos,
		BlockPos pivot,
		StructureTemplate.StructureBlockInfo originalBlockInfo,
		StructureTemplate.StructureBlockInfo currentBlockInfo,
		StructurePlacementData data
	) {
		return currentBlockInfo;
	}

	protected abstract StructureProcessorType<?> getType();

	/**
	 * Обрабатывает весь список блоков шаблона за один проход.
	 * Используется процессорами, которым необходим глобальный контекст всех блоков
	 * (например, {@link CappedStructureProcessor} для ограничения числа замен).
	 */
	public List<StructureTemplate.StructureBlockInfo> reprocess(
		ServerWorldAccess world,
		BlockPos pos,
		BlockPos pivot,
		List<StructureTemplate.StructureBlockInfo> originalBlockInfos,
		List<StructureTemplate.StructureBlockInfo> currentBlockInfos,
		StructurePlacementData data
	) {
		return currentBlockInfos;
	}
}
