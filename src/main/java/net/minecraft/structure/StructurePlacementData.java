package net.minecraft.structure;

import com.google.common.collect.Lists;
import net.minecraft.structure.processor.StructureProcessor;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Конфигурация размещения структурного шаблона в мире.
 * Задаёт зеркалирование, поворот, позицию, список процессоров блоков,
 * настройки жидкостей и прочие параметры, влияющие на то, как
 * {@link StructureTemplate} будет вставлен в чанк.
 */
public class StructurePlacementData {

	private BlockMirror mirror = BlockMirror.NONE;
	private BlockRotation rotation = BlockRotation.NONE;
	private BlockPos position = BlockPos.ORIGIN;
	private boolean ignoreEntities;
	private @Nullable BlockBox boundingBox;
	private StructureLiquidSettings liquidSettings = StructureLiquidSettings.APPLY_WATERLOGGING;
	private @Nullable Random random;
	private int boundingBoxMode;
	private final List<StructureProcessor> processors = Lists.newArrayList();
	private boolean updateNeighbors;
	private boolean initializeMobs;

	/**
	 * Создаёт полную независимую копию этого объекта конфигурации,
	 * включая список процессоров (shallow copy элементов).
	 */
	public StructurePlacementData copy() {
		StructurePlacementData copy = new StructurePlacementData();
		copy.mirror = mirror;
		copy.rotation = rotation;
		copy.position = position;
		copy.ignoreEntities = ignoreEntities;
		copy.boundingBox = boundingBox;
		copy.liquidSettings = liquidSettings;
		copy.random = random;
		copy.boundingBoxMode = boundingBoxMode;
		copy.processors.addAll(processors);
		copy.updateNeighbors = updateNeighbors;
		copy.initializeMobs = initializeMobs;
		return copy;
	}

	public StructurePlacementData setMirror(BlockMirror mirror) {
		this.mirror = mirror;
		return this;
	}

	public StructurePlacementData setRotation(BlockRotation rotation) {
		this.rotation = rotation;
		return this;
	}

	public StructurePlacementData setPosition(BlockPos position) {
		this.position = position;
		return this;
	}

	public StructurePlacementData setIgnoreEntities(boolean ignoreEntities) {
		this.ignoreEntities = ignoreEntities;
		return this;
	}

	public StructurePlacementData setBoundingBox(BlockBox boundingBox) {
		this.boundingBox = boundingBox;
		return this;
	}

	public StructurePlacementData setRandom(@Nullable Random random) {
		this.random = random;
		return this;
	}

	public StructurePlacementData setLiquidSettings(StructureLiquidSettings liquidSettings) {
		this.liquidSettings = liquidSettings;
		return this;
	}

	public StructurePlacementData setUpdateNeighbors(boolean updateNeighbors) {
		this.updateNeighbors = updateNeighbors;
		return this;
	}

	public StructurePlacementData clearProcessors() {
		processors.clear();
		return this;
	}

	public StructurePlacementData addProcessor(StructureProcessor processor) {
		processors.add(processor);
		return this;
	}

	public StructurePlacementData removeProcessor(StructureProcessor processor) {
		processors.remove(processor);
		return this;
	}

	public BlockMirror getMirror() {
		return mirror;
	}

	public BlockRotation getRotation() {
		return rotation;
	}

	public BlockPos getPosition() {
		return position;
	}

	/**
	 * Возвращает генератор случайных чисел для данного размещения.
	 * Если явный {@code random} не задан, создаёт детерминированный генератор
	 * на основе хэша позиции блока, либо на основе текущего времени, если позиция не указана.
	 *
	 * @param pos позиция блока для детерминированного сида, может быть {@code null}
	 * @return генератор случайных чисел
	 */
	public Random getRandom(@Nullable BlockPos pos) {
		if (random != null) {
			return random;
		}

		return pos == null
			? Random.create(Util.getMeasuringTimeMs())
			: Random.create(MathHelper.hashCode(pos));
	}

	public boolean shouldIgnoreEntities() {
		return ignoreEntities;
	}

	public @Nullable BlockBox getBoundingBox() {
		return boundingBox;
	}

	public boolean shouldUpdateNeighbors() {
		return updateNeighbors;
	}

	public List<StructureProcessor> getProcessors() {
		return Collections.unmodifiableList(processors);
	}

	public boolean shouldApplyWaterlogging() {
		return liquidSettings == StructureLiquidSettings.APPLY_WATERLOGGING;
	}

	/**
	 * Выбирает случайный список информации о блоках из нескольких палитр.
	 * Используется для вариативности внешнего вида структур с несколькими палитрами.
	 *
	 * @param infoLists список доступных палитр блоков
	 * @param pos позиция для детерминированного выбора палитры
	 * @return случайно выбранная палитра
	 * @throws IllegalStateException если список палитр пуст
	 */
	public StructureTemplate.PalettedBlockInfoList getRandomBlockInfos(
		List<StructureTemplate.PalettedBlockInfoList> infoLists,
		@Nullable BlockPos pos
	) {
		int count = infoLists.size();
		if (count == 0) {
			throw new IllegalStateException("No palettes");
		}

		return infoLists.get(getRandom(pos).nextInt(count));
	}

	public StructurePlacementData setInitializeMobs(boolean initializeMobs) {
		this.initializeMobs = initializeMobs;
		return this;
	}

	public boolean shouldInitializeMobs() {
		return initializeMobs;
	}
}
