package net.minecraft.entity.ai.goal;

import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.tag.FluidTags;

import java.util.EnumSet;

/** Цель плавания: активирует прыжок, чтобы моб держался на поверхности воды или лавы. */
public class SwimGoal extends Goal {

	private static final float JUMP_CHANCE = 0.8F;

	private final MobEntity mob;

	public SwimGoal(MobEntity mob) {
		this.mob = mob;
		setControls(EnumSet.of(Goal.Control.JUMP));
		mob.getNavigation().setCanSwim(true);
	}

	@Override
	public boolean canStart() {
		return (mob.isTouchingWater() && mob.getFluidHeight(FluidTags.WATER) > mob.getSwimHeight())
				|| mob.isInLava();
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (mob.getRandom().nextFloat() < JUMP_CHANCE) {
			mob.getJumpControl().setActive();
		}
	}
}
