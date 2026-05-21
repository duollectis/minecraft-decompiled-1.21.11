package net.minecraft.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
/**
 * {@code EndLightFlashManager}.
 */
public class EndLightFlashManager {

	public static final int FLASH_DURATION_TICKS = 30;
	private static final int INTERVAL = 600;
	private static final int MAX_START_TIME = 200;
	private static final int MIN_DURATION = 100;
	private static final int FLASH_INTERVAL_TICKS = 380;
	private long currentWindow;
	private int startTime;
	private int duration;
	private float nextSkyFactor;
	private float lastSkyFactor;
	private float pitch;
	private float yaw;

	public void tick(long time) {
		this.update(time);
		this.lastSkyFactor = this.nextSkyFactor;
		this.nextSkyFactor = this.calcSkyFactor(time);
	}

	private void update(long time) {
		long l = time / 600L;
		if (l != this.currentWindow) {
			Random random = Random.create(l);
			random.nextFloat();
			this.startTime = MathHelper.nextBetween(random, 0, 200);
			this.duration = MathHelper.nextBetween(random, 100, Math.min(380, 600 - this.startTime));
			this.pitch = MathHelper.nextBetween(random, -60.0F, 10.0F);
			this.yaw = MathHelper.nextBetween(random, -180.0F, 180.0F);
			this.currentWindow = l;
		}
	}

	private float calcSkyFactor(long time) {
		long l = time % 600L;
		return l >= this.startTime && l <= this.startTime + this.duration ? MathHelper.sin(
				(float) (l - this.startTime) * (float) Math.PI / this.duration) : 0.0F;
	}

	public float getPitch() {
		return this.pitch;
	}

	public float getYaw() {
		return this.yaw;
	}

	public float getSkyFactor(float tickProgress) {
		return MathHelper.lerp(tickProgress, this.lastSkyFactor, this.nextSkyFactor);
	}

	public boolean shouldFlash() {
		return this.nextSkyFactor > 0.0F && this.lastSkyFactor <= 0.0F;
	}
}
