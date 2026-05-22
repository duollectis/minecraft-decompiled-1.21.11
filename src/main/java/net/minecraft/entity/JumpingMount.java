package net.minecraft.entity;

/**
 * Интерфейс для верховых животных, поддерживающих прыжок с зарядкой (лошади, ламы).
 * Сила прыжка задаётся игроком через зажатие пробела и передаётся через {@link #setJumpStrength}.
 */
public interface JumpingMount extends Mount {

	int MAX_JUMP_STRENGTH = 90;
	float MIN_JUMP_POWER = 0.4F;
	float JUMP_POWER_RANGE = 0.4F;

	void setJumpStrength(int strength);

	boolean canJump();

	void startJumping(int height);

	void stopJumping();

	default int getJumpCooldown() {
		return 0;
	}

	/**
	 * Преобразует целочисленную силу прыжка (0–90) в нормализованную мощность (0.4–1.0).
	 * При значении {@code >= 90} возвращает максимальную мощность {@code 1.0F}.
	 *
	 * @param strength сила прыжка от 0 до 90
	 * @return нормализованная мощность прыжка
	 */
	default float clampJumpStrength(int strength) {
		return strength >= MAX_JUMP_STRENGTH
			? 1.0F
			: MIN_JUMP_POWER + JUMP_POWER_RANGE * strength / MAX_JUMP_STRENGTH;
	}
}
