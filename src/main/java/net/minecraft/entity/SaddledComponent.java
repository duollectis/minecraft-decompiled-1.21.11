package net.minecraft.entity;

import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

/**
 * Компонент управления ускорением (boost) для осёдланных сущностей (свиньи, страйдеры).
 * Хранит состояние буста и вычисляет множитель скорости движения на основе синусоиды.
 */
public class SaddledComponent {

	private static final int MIN_BOOST_TIME = 140;
	private static final int MAX_BOOST_TIME = 700;
	/**
	 * Диапазон случайной части длительности буста: MAX_BOOST_TIME - MIN_BOOST_TIME.
	 * Используется в {@link Random#nextInt} для генерации случайного значения.
	 */
	private static final int BOOST_TIME_RANDOM_RANGE = 841;
	private final DataTracker dataTracker;
	private final TrackedData<Integer> boostTime;
	private boolean boosted;
	private int boostedTime;

	public SaddledComponent(DataTracker dataTracker, TrackedData<Integer> boostTime) {
		this.dataTracker = dataTracker;
		this.boostTime = boostTime;
	}

	public void boost() {
		boosted = true;
		boostedTime = 0;
	}

	/**
	 * Активирует буст со случайной длительностью в диапазоне
	 * [{@code MIN_BOOST_TIME}, {@code MIN_BOOST_TIME + BOOST_TIME_RANDOM_RANGE}].
	 *
	 * @return {@code true} если буст успешно активирован, {@code false} если уже активен
	 */
	public boolean boost(Random random) {
		if (boosted) {
			return false;
		}

		boosted = true;
		boostedTime = 0;
		dataTracker.set(boostTime, random.nextInt(BOOST_TIME_RANDOM_RANGE) + MIN_BOOST_TIME);
		return true;
	}

	public void tickBoost() {
		if (boosted && boostedTime++ > getBoostTime()) {
			boosted = false;
		}
	}

	public float getMovementSpeedMultiplier() {
		return boosted
				? 1.0F + 1.15F * MathHelper.sin((float) boostedTime / getBoostTime() * (float) Math.PI)
				: 1.0F;
	}

	private int getBoostTime() {
		return dataTracker.get(boostTime);
	}
}
