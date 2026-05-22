package net.minecraft.world.waypoint;

import net.minecraft.entity.Entity;

/**
 * Предоставляет интерполированный прогресс текущего тика для сущности.
 * <p>
 * Используется при рендеринге вейпоинтов на клиенте для плавного
 * вычисления позиции источника между двумя игровыми тиками.
 */
@FunctionalInterface
public interface EntityTickProgress {

	float getTickProgress(Entity entity);
}
