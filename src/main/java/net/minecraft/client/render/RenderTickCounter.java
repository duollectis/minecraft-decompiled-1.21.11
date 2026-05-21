package net.minecraft.client.render;

import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
/**
 * {@code RenderTickCounter}.
 */
public interface RenderTickCounter {

	RenderTickCounter ZERO = new RenderTickCounter.Constant(0.0F);

	RenderTickCounter ONE = new RenderTickCounter.Constant(1.0F);

	float getDynamicDeltaTicks();

	float getTickProgress(boolean ignoreFreeze);

	float getFixedDeltaTicks();

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Constant}.
	 */
	public static class Constant implements RenderTickCounter {

		private final float value;

		Constant(float value) {
			this.value = value;
		}

		@Override
		public float getDynamicDeltaTicks() {
			return this.value;
		}

		@Override
		public float getTickProgress(boolean ignoreFreeze) {
			return this.value;
		}

		@Override
		public float getFixedDeltaTicks() {
			return this.value;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Dynamic}.
	 */
	public static class Dynamic implements RenderTickCounter {

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
			this.tickTime = 1000.0F / tps;
			this.timeMillis = this.lastTimeMillis = timeMillis;
			this.targetMillisPerTick = targetMillisPerTick;
		}

		/**
		 * Begin render tick.
		 *
		 * @param timeMillis time millis
		 * @param tick tick
		 *
		 * @return int — результат операции
		 */
		public int beginRenderTick(long timeMillis, boolean tick) {
			this.setTimeMillis(timeMillis);
			return tick ? this.beginRenderTick(timeMillis) : 0;
		}

		private int beginRenderTick(long timeMillis) {
			this.dynamicDeltaTicks =
					(float) (timeMillis - this.lastTimeMillis) / this.targetMillisPerTick.apply(this.tickTime);
			this.lastTimeMillis = timeMillis;
			this.tickProgress = this.tickProgress + this.dynamicDeltaTicks;
			int i = (int) this.tickProgress;
			this.tickProgress -= i;
			return i;
		}

		private void setTimeMillis(long timeMillis) {
			this.fixedDeltaTicks = (float) (timeMillis - this.timeMillis) / this.tickTime;
			this.timeMillis = timeMillis;
		}

		/**
		 * Tick.
		 *
		 * @param paused paused
		 */
		public void tick(boolean paused) {
			if (paused) {
				this.tickPaused();
			}
			else {
				this.tickUnpaused();
			}
		}

		private void tickPaused() {
			if (!this.paused) {
				this.tickProgressBeforePause = this.tickProgress;
			}

			this.paused = true;
		}

		private void tickUnpaused() {
			if (this.paused) {
				this.tickProgress = this.tickProgressBeforePause;
			}

			this.paused = false;
		}

		public void setTickFrozen(boolean tickFrozen) {
			this.tickFrozen = tickFrozen;
		}

		@Override
		public float getDynamicDeltaTicks() {
			return this.dynamicDeltaTicks;
		}

		@Override
		public float getTickProgress(boolean ignoreFreeze) {
			if (!ignoreFreeze && this.tickFrozen) {
				return 1.0F;
			}
			else {
				return this.paused ? this.tickProgressBeforePause : this.tickProgress;
			}
		}

		@Override
		public float getFixedDeltaTicks() {
			return this.fixedDeltaTicks > 7.0F ? 0.5F : this.fixedDeltaTicks;
		}
	}
}
