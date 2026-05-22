package net.minecraft.client.render;

import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Счётчик тиков рендеринга, предоставляющий интерполяционный прогресс между тиками.
 * Используется для плавной анимации между игровыми тиками.
 * Предоставляет две реализации: {@link Constant} для фиксированного значения
 * и {@link Dynamic} для динамического расчёта на основе реального времени.
 */
@Environment(EnvType.CLIENT)
public interface RenderTickCounter {

	RenderTickCounter ZERO = new RenderTickCounter.Constant(0.0F);
	RenderTickCounter ONE = new RenderTickCounter.Constant(1.0F);

	float getDynamicDeltaTicks();

	float getTickProgress(boolean ignoreFreeze);

	float getFixedDeltaTicks();

	/** Константная реализация, всегда возвращающая одно фиксированное значение. */
	@Environment(EnvType.CLIENT)
	public static class Constant implements RenderTickCounter {

		private final float value;

		Constant(float value) {
			this.value = value;
		}

		@Override
		public float getDynamicDeltaTicks() {
			return value;
		}

		@Override
		public float getTickProgress(boolean ignoreFreeze) {
			return value;
		}

		@Override
		public float getFixedDeltaTicks() {
			return value;
		}
	}

	/**
	 * Динамическая реализация, вычисляющая прогресс тика на основе реального времени.
	 * Поддерживает паузу, заморозку тиков и настраиваемую скорость через {@link FloatUnaryOperator}.
	 */
	@Environment(EnvType.CLIENT)
	public static class Dynamic implements RenderTickCounter {

		/** Порог фиксированной дельты, выше которого возвращается 0.5 для предотвращения рывков. */
		private static final float MAX_FIXED_DELTA_TICKS = 7.0F;
		private static final float FIXED_DELTA_FALLBACK = 0.5F;
		private static final float MILLIS_PER_SECOND = 1000.0F;

		private float dynamicDeltaTicks;
		private float tickProgress;
		private float fixedDeltaTicks;
		private float tickProgressBeforePause;
		private long lastTimeMillis;
		private long timeMillis;
		private final float tickTime;
		private final FloatUnaryOperator targetMillisPerTick;
		private boolean paused;
		private boolean tickFrozen;

		public Dynamic(float tps, long timeMillis, FloatUnaryOperator targetMillisPerTick) {
			tickTime = MILLIS_PER_SECOND / tps;
			this.timeMillis = lastTimeMillis = timeMillis;
			this.targetMillisPerTick = targetMillisPerTick;
		}

		/**
		 * Начинает новый кадр рендеринга: обновляет время и при необходимости вычисляет
		 * количество игровых тиков, которые нужно выполнить.
		 *
		 * @param timeMillis текущее время в миллисекундах
		 * @param tick       {@code true} — выполнять расчёт тиков
		 * @return количество игровых тиков для выполнения в этом кадре
		 */
		public int beginRenderTick(long timeMillis, boolean tick) {
			setTimeMillis(timeMillis);
			return tick ? beginRenderTick(timeMillis) : 0;
		}

		private int beginRenderTick(long timeMillis) {
			dynamicDeltaTicks = (float) (timeMillis - lastTimeMillis) / targetMillisPerTick.apply(tickTime);
			lastTimeMillis = timeMillis;
			tickProgress = tickProgress + dynamicDeltaTicks;
			int completedTicks = (int) tickProgress;
			tickProgress -= completedTicks;
			return completedTicks;
		}

		private void setTimeMillis(long timeMillis) {
			fixedDeltaTicks = (float) (timeMillis - this.timeMillis) / tickTime;
			this.timeMillis = timeMillis;
		}

		public void tick(boolean paused) {
			if (paused) {
				tickPaused();
			}
			else {
				tickUnpaused();
			}
		}

		private void tickPaused() {
			if (!paused) {
				tickProgressBeforePause = tickProgress;
			}

			paused = true;
		}

		private void tickUnpaused() {
			if (paused) {
				tickProgress = tickProgressBeforePause;
			}

			paused = false;
		}

		public void setTickFrozen(boolean tickFrozen) {
			this.tickFrozen = tickFrozen;
		}

		@Override
		public float getDynamicDeltaTicks() {
			return dynamicDeltaTicks;
		}

		@Override
		public float getTickProgress(boolean ignoreFreeze) {
			if (!ignoreFreeze && tickFrozen) {
				return 1.0F;
			}

			return paused ? tickProgressBeforePause : tickProgress;
		}

		@Override
		public float getFixedDeltaTicks() {
			return fixedDeltaTicks > MAX_FIXED_DELTA_TICKS ? FIXED_DELTA_FALLBACK : fixedDeltaTicks;
		}
	}
}
