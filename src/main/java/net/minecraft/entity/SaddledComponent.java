package net.minecraft.entity;

import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

/**
 * {@code SaddledComponent}.
 */
public class SaddledComponent {

	private static final int MIN_BOOST_TIME = 140;
	private static final int MAX_BOOST_TIME = 700;
	private final DataTracker dataTracker;
	private final TrackedData<Integer> boostTime;
	private boolean boosted;
	private int boostedTime;

	public SaddledComponent(DataTracker dataTracker, TrackedData<Integer> boostTime) {
		this.dataTracker = dataTracker;
		this.boostTime = boostTime;
	}

	public void boost() {
		this.boosted = true;
		this.boostedTime = 0;
	}

	public boolean boost(Random random) {
		if (this.boosted) {
			return false;
		}
		else {
			this.boosted = true;
			this.boostedTime = 0;
			this.dataTracker.set(this.boostTime, random.nextInt(841) + 140);
			return true;
		}
	}

	public void tickBoost() {
		if (this.boosted && this.boostedTime++ > this.getBoostTime()) {
			this.boosted = false;
		}
	}

	public float getMovementSpeedMultiplier() {
		return this.boosted ? 1.0F + 1.15F * MathHelper.sin(
				(float) this.boostedTime / this.getBoostTime() * (float) Math.PI) : 1.0F;
	}

	private int getBoostTime() {
		return this.dataTracker.get(this.boostTime);
	}
}
