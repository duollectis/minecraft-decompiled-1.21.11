package net.minecraft.world.spawner;

import net.minecraft.server.world.ServerWorld;

/**
 * Интерфейс специального спаунера — компонента, отвечающего за периодический спаун
 * особых мобов (фантомов, патрулей, кошек и т.д.) в серверном мире.
 */
public interface SpecialSpawner {

	/**
	 * Выполняет один тик спаунера. Вызывается каждый игровой тик.
	 *
	 * @param world         серверный мир
	 * @param spawnMonsters разрешён ли спаун монстров в данный момент
	 */
	void spawn(ServerWorld world, boolean spawnMonsters);
}
