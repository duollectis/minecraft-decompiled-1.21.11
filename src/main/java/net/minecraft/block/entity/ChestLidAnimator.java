package net.minecraft.block.entity;

import net.minecraft.util.math.MathHelper;

/**
 * Управляет анимацией крышки сундука: плавно открывает и закрывает её
 * с фиксированной скоростью {@code LID_ANIMATION_SPEED} за тик.
 */
public class ChestLidAnimator {

	private static final float LID_ANIMATION_SPEED = 0.1F;

	private boolean open;
	private float progress;
	private float lastProgress;

	public void step() {
		lastProgress = progress;

		if (!open && progress > 0.0F) {
			progress = Math.max(progress - LID_ANIMATION_SPEED, 0.0F);
		} else if (open && progress < 1.0F) {
			progress = Math.min(progress + LID_ANIMATION_SPEED, 1.0F);
		}
	}

	public float getProgress(float tickProgress) {
		return MathHelper.lerp(tickProgress, lastProgress, progress);
	}

	public void setOpen(boolean open) {
		this.open = open;
	}
}
