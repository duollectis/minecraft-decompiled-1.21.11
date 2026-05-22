package net.minecraft.entity.ai.goal;

import net.minecraft.entity.mob.MobEntity;

import java.util.EnumSet;

/** Цель случайного осмотра по сторонам. Моб поворачивает голову в случайном направлении. */
public class LookAroundGoal extends Goal {

	private static final float START_CHANCE = 0.02F;
	private static final int LOOK_TIME_BASE = 20;
	private static final int LOOK_TIME_JITTER = 20;

	private final MobEntity mob;
	private double deltaX;
	private double deltaZ;
	private int lookTime;

	public LookAroundGoal(MobEntity mob) {
		this.mob = mob;
		setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		return mob.getRandom().nextFloat() < START_CHANCE;
	}

	@Override
	public boolean shouldContinue() {
		return lookTime >= 0;
	}

	@Override
	public void start() {
		double angle = Math.PI * 2 * mob.getRandom().nextDouble();
		deltaX = Math.cos(angle);
		deltaZ = Math.sin(angle);
		lookTime = LOOK_TIME_BASE + mob.getRandom().nextInt(LOOK_TIME_JITTER);
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		lookTime--;
		mob.getLookControl().lookAt(mob.getX() + deltaX, mob.getEyeY(), mob.getZ() + deltaZ);
	}
}
