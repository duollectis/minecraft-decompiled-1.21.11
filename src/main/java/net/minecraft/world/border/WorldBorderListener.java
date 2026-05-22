package net.minecraft.world.border;

/**
 * Слушатель событий изменения границы мира.
 * <p>
 * Реализации получают уведомления при любом изменении параметров {@link WorldBorder}:
 * размера, центра, урона, предупреждений и т.д. Используется для синхронизации
 * состояния границы между сервером и клиентами.
 */
public interface WorldBorderListener {

	/**
	 * Вызывается при мгновенном изменении размера границы без интерполяции.
	 *
	 * @param border изменённая граница мира
	 * @param size новый размер границы
	 */
	void onSizeChange(WorldBorder border, double size);

	/**
	 * Вызывается при запуске плавного изменения размера границы во времени.
	 *
	 * @param border изменённая граница мира
	 * @param fromSize начальный размер
	 * @param toSize целевой размер
	 * @param duration длительность перехода в тиках
	 * @param startTime игровое время начала перехода в миллисекундах
	 */
	void onInterpolateSize(WorldBorder border, double fromSize, double toSize, long duration, long startTime);

	/**
	 * Вызывается при смещении центра границы.
	 *
	 * @param border изменённая граница мира
	 * @param centerX новая координата X центра
	 * @param centerZ новая координата Z центра
	 */
	void onCenterChanged(WorldBorder border, double centerX, double centerZ);

	/**
	 * Вызывается при изменении времени предупреждения о приближении границы.
	 *
	 * @param border изменённая граница мира
	 * @param warningTime новое время предупреждения в секундах
	 */
	void onWarningTimeChanged(WorldBorder border, int warningTime);

	/**
	 * Вызывается при изменении дистанции предупреждения о приближении границы.
	 *
	 * @param border изменённая граница мира
	 * @param warningBlockDistance новая дистанция предупреждения в блоках
	 */
	void onWarningBlocksChanged(WorldBorder border, int warningBlockDistance);

	/**
	 * Вызывается при изменении урона за блок нахождения за границей.
	 *
	 * @param border изменённая граница мира
	 * @param damagePerBlock новый урон за блок в единицах здоровья за тик
	 */
	void onDamagePerBlockChanged(WorldBorder border, double damagePerBlock);

	/**
	 * Вызывается при изменении размера безопасной зоны у края границы.
	 *
	 * @param border изменённая граница мира
	 * @param safeZoneRadius новый радиус безопасной зоны в блоках
	 */
	void onSafeZoneChanged(WorldBorder border, double safeZoneRadius);
}
