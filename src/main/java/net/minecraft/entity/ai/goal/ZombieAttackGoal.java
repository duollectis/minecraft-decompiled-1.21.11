package net.minecraft.entity.ai.goal;

import net.minecraft.entity.mob.ZombieEntity;

/**
 * Цель ближнего боя зомби: расширяет {@link MeleeAttackGoal}, включая анимацию атаки
 * через {@code ATTACK_ANIM_DELAY} тиков после начала замаха.
 */
public class ZombieAttackGoal extends MeleeAttackGoal {

	private static final int ATTACK_ANIM_DELAY = 5;

	private final ZombieEntity zombie;
	private int ticks;

	public ZombieAttackGoal(ZombieEntity zombie, double speed, boolean pauseWhenMobIdle) {
		super(zombie, speed, pauseWhenMobIdle);
		this.zombie = zombie;
	}

	@Override
	public void start() {
		super.start();
		ticks = 0;
	}

	@Override
	public void stop() {
		super.stop();
		zombie.setAttacking(false);
	}

	@Override
	public void tick() {
		super.tick();
		ticks++;
		boolean isAttacking = ticks >= ATTACK_ANIM_DELAY && getCooldown() < getMaxCooldown() / 2;
		zombie.setAttacking(isAttacking);
	}
}
