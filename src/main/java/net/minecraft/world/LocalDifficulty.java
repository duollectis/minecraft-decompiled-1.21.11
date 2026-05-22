package net.minecraft.world;

import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Локальная сложность в конкретной позиции мира.
 * Вычисляется на основе глобальной сложности, времени мира,
 * времени обитания в чанке и фазы луны.
 */
@Unmodifiable
public class LocalDifficulty {

	/** Смещение времени: первые 72000 тиков (1 игровой день) не влияют на сложность. */
	private static final float TIME_OFFSET_TICKS = -72000.0F;

	/** Максимальное время мира, при котором сложность достигает пика (1440000 тиков = 20 дней). */
	private static final float MAX_TIME_DIFFICULTY_TICKS = 1440000.0F;

	/** Максимальное время обитания в чанке для расчёта бонуса (3600000 тиков = 50 дней). */
	private static final float MAX_INHABITED_TIME_TICKS = 3600000.0F;

	/** Нижняя граница «жёсткой» зоны локальной сложности. */
	private static final float HARD_DIFFICULTY_LOWER_BOUND = 2.0F;

	/** Верхняя граница «жёсткой» зоны локальной сложности. */
	private static final float HARD_DIFFICULTY_UPPER_BOUND = 4.0F;

	private final Difficulty globalDifficulty;
	private final float localDifficulty;

	public LocalDifficulty(Difficulty difficulty, long timeOfDay, long inhabitedTime, float moonSize) {
		this.globalDifficulty = difficulty;
		this.localDifficulty = calculateLocalDifficulty(difficulty, timeOfDay, inhabitedTime, moonSize);
	}

	public Difficulty getGlobalDifficulty() {
		return globalDifficulty;
	}

	public float getLocalDifficulty() {
		return localDifficulty;
	}

	public boolean isAtLeastHard() {
		return localDifficulty >= Difficulty.HARD.ordinal();
	}

	public boolean isHarderThan(float difficulty) {
		return localDifficulty > difficulty;
	}

	/**
	 * Возвращает нормализованное значение сложности в диапазоне [0.0, 1.0].
	 * Значения ниже 2.0 дают 0.0, выше 4.0 — 1.0.
	 */
	public float getClampedLocalDifficulty() {
		if (localDifficulty < HARD_DIFFICULTY_LOWER_BOUND) {
			return 0.0F;
		}

		return localDifficulty > HARD_DIFFICULTY_UPPER_BOUND
			? 1.0F
			: (localDifficulty - HARD_DIFFICULTY_LOWER_BOUND) / HARD_DIFFICULTY_LOWER_BOUND;
	}

	/**
	 * Вычисляет локальную сложность как произведение ID сложности на взвешенную сумму
	 * трёх факторов: времени мира, времени обитания в чанке и фазы луны.
	 */
	private static float calculateLocalDifficulty(
		Difficulty difficulty,
		long timeOfDay,
		long inhabitedTime,
		float moonSize
	) {
		if (difficulty == Difficulty.PEACEFUL) {
			return 0.0F;
		}

		boolean isHard = difficulty == Difficulty.HARD;

		float timeFactor = MathHelper.clamp(
			((float) timeOfDay + TIME_OFFSET_TICKS) / MAX_TIME_DIFFICULTY_TICKS,
			0.0F,
			1.0F
		) * 0.25F;

		float base = 0.75F + timeFactor;

		float inhabitedFactor = MathHelper.clamp(
			(float) inhabitedTime / MAX_INHABITED_TIME_TICKS,
			0.0F,
			1.0F
		) * (isHard ? 1.0F : 0.75F);

		float moonFactor = MathHelper.clamp(moonSize * 0.25F, 0.0F, timeFactor);
		float bonus = inhabitedFactor + moonFactor;

		if (difficulty == Difficulty.EASY) {
			bonus *= 0.5F;
		}

		return difficulty.getId() * (base + bonus);
	}
}
