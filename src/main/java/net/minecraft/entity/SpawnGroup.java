package net.minecraft.entity;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;

/**
 * Группа спауна мобов, определяющая лимит одновременно существующих сущностей,
 * дальность деспауна и поведение при мирном режиме сложности.
 */
public enum SpawnGroup implements StringIdentifiable {
	MONSTER("monster", 70, false, false, 128),
	CREATURE("creature", 10, true, true, 128),
	AMBIENT("ambient", 15, true, false, 128),
	AXOLOTLS("axolotls", 5, true, false, 128),
	UNDERGROUND_WATER_CREATURE("underground_water_creature", 5, true, false, 128),
	WATER_CREATURE("water_creature", 5, true, false, 128),
	WATER_AMBIENT("water_ambient", 20, true, false, 64),
	MISC("misc", -1, true, true, 128);

	public static final Codec<SpawnGroup> CODEC = StringIdentifiable.createCodec(SpawnGroup::values);
	private static final int DESPAWN_START_RANGE = 32;

	private final int capacity;
	private final boolean peaceful;
	private final boolean rare;
	private final String name;
	private final int immediateDespawnRange;

	SpawnGroup(
			String name,
			int spawnCap,
			boolean peaceful,
			boolean rare,
			int immediateDespawnRange
	) {
		this.name = name;
		this.capacity = spawnCap;
		this.peaceful = peaceful;
		this.rare = rare;
		this.immediateDespawnRange = immediateDespawnRange;
	}

	@Override
	public String asString() {
		return name;
	}

	/**
	 * Возвращает дистанцию, начиная с которой моб начинает деспауниться.
	 * Фиксировано для всех групп — 32 блока от игрока.
	 */
	public int getDespawnStartRange() {
		return DESPAWN_START_RANGE;
	}

	public boolean isPeaceful() {
		return peaceful;
	}

	public int getImmediateDespawnRange() {
		return immediateDespawnRange;
	}

	public int getCapacity() {
		return capacity;
	}

	public boolean isRare() {
		return rare;
	}

	public String getName() {
		return name;
	}

}
