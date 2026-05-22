package net.minecraft.entity;

import net.minecraft.block.entity.ViewerCountManager;
import net.minecraft.util.math.BlockPos;

/**
 * Интерфейс для сущностей, способных взаимодействовать с контейнерами (сундуки, бочки и т.д.).
 * Реализующая сущность обязана быть {@link LivingEntity}.
 */
public interface ContainerUser {

	boolean isViewingContainerAt(ViewerCountManager viewerCountManager, BlockPos pos);

	double getContainerInteractionRange();

	/**
	 * Приводит текущую сущность к {@link LivingEntity}.
	 * Выбрасывает исключение, если реализующий класс не является {@code LivingEntity},
	 * что является нарушением контракта интерфейса.
	 *
	 * @return эта сущность как {@link LivingEntity}
	 * @throws IllegalStateException если реализующий класс не является {@link LivingEntity}
	 */
	default LivingEntity asLivingEntity() {
		if (this instanceof LivingEntity livingEntity) {
			return livingEntity;
		}

		throw new IllegalStateException("A container user must be a LivingEntity");
	}
}
