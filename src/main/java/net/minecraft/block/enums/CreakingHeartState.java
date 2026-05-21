package net.minecraft.block.enums;

import net.minecraft.util.StringIdentifiable;

/**
 * {@code CreakingHeartState}.
 */
public enum CreakingHeartState implements StringIdentifiable {
	UPROOTED("uprooted"),
	DORMANT("dormant"),
	AWAKE("awake");

	private final String id;

	private CreakingHeartState(final String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return this.id;
	}

	@Override
	public String asString() {
		return this.id;
	}
}
