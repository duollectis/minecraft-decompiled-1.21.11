package net.minecraft.world.entity;

import net.minecraft.server.world.ChunkLevelType;

/**
 * Статус отслеживания секции сущностей.
 * Определяет, должны ли сущности в данной секции тикаться и/или отслеживаться клиентом.
 */
public enum EntityTrackingStatus {
	/** Секция не загружена — сущности не тикаются и не отслеживаются. */
	HIDDEN(false, false),
	/** Секция загружена, но не тикается — сущности отслеживаются, но не обновляются. */
	TRACKED(true, false),
	/** Секция полностью активна — сущности и отслеживаются, и тикаются. */
	TICKING(true, true);

	private final boolean tracked;
	private final boolean tick;

	EntityTrackingStatus(boolean tracked, boolean tick) {
		this.tracked = tracked;
		this.tick = tick;
	}

	public boolean shouldTick() {
		return tick;
	}

	public boolean shouldTrack() {
		return tracked;
	}

	/**
	 * Преобразует тип уровня чанка в соответствующий статус отслеживания сущностей.
	 * ENTITY_TICKING → TICKING, FULL → TRACKED, остальные → HIDDEN.
	 *
	 * @param levelType тип уровня загрузки чанка
	 * @return соответствующий статус отслеживания
	 */
	public static EntityTrackingStatus fromLevelType(ChunkLevelType levelType) {
		if (levelType.isAfter(ChunkLevelType.ENTITY_TICKING)) {
			return TICKING;
		}

		return levelType.isAfter(ChunkLevelType.FULL) ? TRACKED : HIDDEN;
	}
}
