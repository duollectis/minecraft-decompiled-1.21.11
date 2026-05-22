package net.minecraft.entity.passive;

import net.minecraft.util.function.ValueLists;

import java.util.function.IntFunction;

/**
 * Маркировка лошади (узор на шкуре). Упакована в старший байт варианта лошади (биты 8–15).
 */
public enum HorseMarking {
	NONE(0),
	WHITE(1),
	WHITE_FIELD(2),
	WHITE_DOTS(3),
	BLACK_DOTS(4);

	private static final IntFunction<HorseMarking> INDEX_MAPPER = ValueLists.createIndexToValueFunction(
		HorseMarking::getIndex,
		values(),
		ValueLists.OutOfBoundsHandling.WRAP
	);

	private final int index;

	HorseMarking(int index) {
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	public static HorseMarking byIndex(int index) {
		return INDEX_MAPPER.apply(index);
	}
}
