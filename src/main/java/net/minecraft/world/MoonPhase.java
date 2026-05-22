package net.minecraft.world;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;

/**
 * Фаза луны в игровом мире.
 * Полный цикл состоит из 8 фаз, каждая длится ровно один игровой день ({@link #TICKS_PER_DAY} тиков).
 * Фаза луны влияет на спавн слизней и другие игровые механики.
 */
public enum MoonPhase implements StringIdentifiable {
	FULL_MOON(0, "full_moon"),
	WANING_GIBBOUS(1, "waning_gibbous"),
	THIRD_QUARTER(2, "third_quarter"),
	WANING_CRESCENT(3, "waning_crescent"),
	NEW_MOON(4, "new_moon"),
	WAXING_CRESCENT(5, "waxing_crescent"),
	FIRST_QUARTER(6, "first_quarter"),
	WAXING_GIBBOUS(7, "waxing_gibbous");

	public static final Codec<MoonPhase> CODEC = StringIdentifiable.createCodec(MoonPhase::values);
	public static final int COUNT = values().length;
	public static final int TICKS_PER_DAY = 24000;

	private final int index;
	private final String name;

	MoonPhase(final int index, final String name) {
		this.index = index;
		this.name = name;
	}

	public int getIndex() {
		return index;
	}

	/**
	 * Возвращает количество тиков от начала лунного цикла до начала данной фазы.
	 * Используется для вычисления текущей фазы луны по мировому времени.
	 */
	public int phaseTicks() {
		return index * TICKS_PER_DAY;
	}

	@Override
	public String asString() {
		return name;
	}
}
