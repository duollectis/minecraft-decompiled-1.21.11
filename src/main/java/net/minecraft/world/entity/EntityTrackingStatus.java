package net.minecraft.world.entity;

import net.minecraft.server.world.ChunkLevelType;

/**
 * {@code EntityTrackingStatus}.
 */
public enum EntityTrackingStatus {
	HIDDEN(false, false),
	TRACKED(true, false),
	TICKING(true, true);

	private final boolean tracked;
	private final boolean tick;

	private EntityTrackingStatus(final boolean tracked, final boolean tick) {
		this.tracked = tracked;
		this.tick = tick;
	}

	/**
	 * Определяет, следует ли tick.
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldTick() {
		return this.tick;
	}

	/**
	 * Определяет, следует ли track.
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldTrack() {
		return this.tracked;
	}

	/**
	 * From level type.
	 *
	 * @param levelType level type
	 *
	 * @return EntityTrackingStatus — результат операции
	 */
	public static EntityTrackingStatus fromLevelType(ChunkLevelType levelType) {
		if (levelType.isAfter(ChunkLevelType.ENTITY_TICKING)) {
			return TICKING;
		}
		else {
			return levelType.isAfter(ChunkLevelType.FULL) ? TRACKED : HIDDEN;
		}
	}
}
