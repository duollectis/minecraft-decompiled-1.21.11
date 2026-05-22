package net.minecraft.entity.ai.brain;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Цель взгляда — абстракция над точкой в мире, на которую сущность смотрит.
 * Может быть привязана к блоку ({@link BlockPosLookTarget}) или к другой сущности ({@link EntityLookTarget}).
 */
public interface LookTarget {

	Vec3d getPos();

	BlockPos getBlockPos();

	/**
	 * Проверяет, видит ли наблюдатель эту цель взгляда.
	 * Для блоков всегда возвращает {@code true}; для сущностей — проверяет кэш видимости.
	 *
	 * @param entity наблюдатель
	 */
	boolean isSeenBy(LivingEntity entity);
}
