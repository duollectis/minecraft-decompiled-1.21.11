package net.minecraft.entity;

/**
 * Причина появления сущности в мире.
 * Используется для определения специфического поведения при спауне
 * (например, применение снаряжения только при естественном спауне).
 */
public enum SpawnReason {
	NATURAL,
	CHUNK_GENERATION,
	SPAWNER,
	STRUCTURE,
	BREEDING,
	MOB_SUMMONED,
	JOCKEY,
	EVENT,
	CONVERSION,
	REINFORCEMENT,
	TRIGGERED,
	BUCKET,
	SPAWN_ITEM_USE,
	COMMAND,
	DISPENSER,
	PATROL,
	TRIAL_SPAWNER,
	LOAD,
	DIMENSION_TRAVEL;

	public static boolean isAnySpawner(SpawnReason reason) {
		return reason == SPAWNER || reason == TRIAL_SPAWNER;
	}

	public static boolean isTrialSpawner(SpawnReason reason) {
		return reason == TRIAL_SPAWNER;
	}
}
