package net.minecraft.world.entity;

import net.minecraft.entity.Entity;

/**
 * Слушатель изменений состояния сущности в системе отслеживания.
 * Уведомляет кэш секций об изменении позиции или удалении сущности.
 */
public interface EntityChangeListener {

	/** Реализация-заглушка, игнорирующая все события. */
	EntityChangeListener NONE = new EntityChangeListener() {
		@Override
		public void updateEntityPosition() {
		}

		@Override
		public void remove(Entity.RemovalReason reason) {
		}
	};

	/** Вызывается при изменении позиции сущности для обновления секции отслеживания. */
	void updateEntityPosition();

	/** Вызывается при удалении сущности из мира. */
	void remove(Entity.RemovalReason reason);
}
