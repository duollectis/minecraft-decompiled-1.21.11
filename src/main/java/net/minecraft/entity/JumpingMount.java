package net.minecraft.entity;

/**
 * {@code JumpingMount}.
 */
public interface JumpingMount extends Mount {

	void setJumpStrength(int strength);

	boolean canJump();

	void startJumping(int height);

	void stopJumping();

	default int getJumpCooldown() {
		return 0;
	}

	default float clampJumpStrength(int strength) {
		return strength >= 90 ? 1.0F : 0.4F + 0.4F * strength / 90.0F;
	}
}
