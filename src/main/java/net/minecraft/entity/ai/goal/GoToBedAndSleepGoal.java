package net.minecraft.entity.ai.goal;

import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;

import java.util.EnumSet;

/**
 * Цель, заставляющая прирученную кошку находить кровать и засыпать на ней.
 * Наследует логику навигации к целевой позиции от {@link MoveToTargetPosGoal}.
 */
public class GoToBedAndSleepGoal extends MoveToTargetPosGoal {

	private static final int SEARCH_INTERVAL_TICKS = 40;

	private final CatEntity cat;

	public GoToBedAndSleepGoal(CatEntity cat, double speed, int range) {
		super(cat, speed, range, 6);
		this.cat = cat;
		this.lowestY = -2;
		this.setControls(EnumSet.of(Goal.Control.JUMP, Goal.Control.MOVE));
	}

	@Override
	public boolean canStart() {
		return cat.isTamed() && !cat.isSitting() && !cat.isInSleepingPose() && super.canStart();
	}

	@Override
	public void start() {
		super.start();
		cat.setInSittingPose(false);
	}

	@Override
	protected int getInterval(PathAwareEntity mob) {
		return SEARCH_INTERVAL_TICKS;
	}

	@Override
	public void stop() {
		super.stop();
		cat.setInSleepingPose(false);
	}

	@Override
	public void tick() {
		super.tick();
		cat.setInSittingPose(false);

		if (!hasReached()) {
			cat.setInSleepingPose(false);
		} else if (!cat.isInSleepingPose()) {
			cat.setInSleepingPose(true);
		}
	}

	@Override
	protected boolean isTargetPos(WorldView world, BlockPos pos) {
		return world.isAir(pos.up()) && world.getBlockState(pos).isIn(BlockTags.BEDS);
	}
}
