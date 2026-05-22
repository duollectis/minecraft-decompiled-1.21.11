package net.minecraft.entity;

import net.minecraft.block.AbstractFireBlock;

import java.util.function.Consumer;

/**
 * Перечисление событий столкновения сущности с блоком окружающей среды.
 * Каждое событие несёт в себе действие, применяемое к сущности при срабатывании.
 */
public enum CollisionEvent {
	FREEZE(entity -> {
		entity.setInPowderSnow(true);
		if (entity.canFreeze()) {
			entity.setFrozenTicks(Math.min(entity.getMinFreezeDamageTicks(), entity.getFrozenTicks() + 1));
		}
	}),
	CLEAR_FREEZE(Entity::defrost),
	FIRE_IGNITE(AbstractFireBlock::igniteEntity),
	LAVA_IGNITE(Entity::igniteByLava),
	EXTINGUISH(Entity::extinguish);

	private final Consumer<Entity> action;

	CollisionEvent(Consumer<Entity> action) {
		this.action = action;
	}

	public Consumer<Entity> getAction() {
		return action;
	}
}
