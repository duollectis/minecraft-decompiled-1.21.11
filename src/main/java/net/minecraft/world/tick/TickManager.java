package net.minecraft.world.tick;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.TimeHelper;

/**
 * Управляет скоростью тиков сервера: частотой, заморозкой и пошаговым режимом.
 * Используется командами {@code /tick} для отладки и замедления симуляции.
 */
public class TickManager {

	public static final float MIN_TICK_RATE = 1.0F;
	private static final float DEFAULT_TICK_RATE = 20.0F;

	protected float tickRate = DEFAULT_TICK_RATE;
	protected long nanosPerTick = TimeHelper.SECOND_IN_NANOS / (long) DEFAULT_TICK_RATE;
	protected int stepTicks = 0;
	protected boolean shouldTick = true;
	protected boolean frozen = false;

	/**
	 * Устанавливает частоту тиков в секунду. Значение ниже {@link #MIN_TICK_RATE} игнорируется.
	 * Автоматически пересчитывает {@code nanosPerTick}.
	 *
	 * @param tickRate новая частота тиков (тиков/сек)
	 */
	public void setTickRate(float tickRate) {
		this.tickRate = Math.max(tickRate, MIN_TICK_RATE);
		nanosPerTick = (long) ((double) TimeHelper.SECOND_IN_NANOS / this.tickRate);
	}

	public float getTickRate() {
		return tickRate;
	}

	public float getMillisPerTick() {
		return (float) nanosPerTick / (float) TimeHelper.MILLI_IN_NANOS;
	}

	public long getNanosPerTick() {
		return nanosPerTick;
	}

	public boolean shouldTick() {
		return shouldTick;
	}

	public boolean isStepping() {
		return stepTicks > 0;
	}

	public void setStepTicks(int stepTicks) {
		this.stepTicks = stepTicks;
	}

	public int getStepTicks() {
		return stepTicks;
	}

	public void setFrozen(boolean frozen) {
		this.frozen = frozen;
	}

	public boolean isFrozen() {
		return frozen;
	}

	/**
	 * Обновляет флаг {@code shouldTick} на основе состояния заморозки и пошагового режима.
	 * Если мир заморожен и шагов не осталось — тик пропускается.
	 * Вызывается один раз в начале каждого серверного тика.
	 */
	public void step() {
		shouldTick = !frozen || stepTicks > 0;

		if (stepTicks > 0) {
			stepTicks--;
		}
	}

	/**
	 * Определяет, нужно ли пропустить тик для данной сущности.
	 * Игроки и сущности с пассажирами-игроками всегда тикают, даже при заморозке.
	 *
	 * @param entity сущность для проверки
	 * @return {@code true}, если тик сущности следует пропустить
	 */
	public boolean shouldSkipTick(Entity entity) {
		return !shouldTick() && !(entity instanceof PlayerEntity) && entity.getPlayerPassengers() <= 0;
	}
}
