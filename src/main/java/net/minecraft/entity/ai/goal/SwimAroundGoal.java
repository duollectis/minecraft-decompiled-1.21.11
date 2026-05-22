package net.minecraft.entity.ai.goal;

import net.minecraft.entity.ai.brain.task.TargetUtil;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

/**
 * Цель случайного плавания водного моба: использует {@link TargetUtil#find}
 * для поиска случайной позиции в воде.
 */
public class SwimAroundGoal extends WanderAroundGoal {

	private static final int SWIM_HORIZONTAL_RANGE = 10;
	private static final int SWIM_VERTICAL_RANGE = 7;

	public SwimAroundGoal(PathAwareEntity mob, double speed, int chance) {
		super(mob, speed, chance);
	}

	@Override
	protected @Nullable Vec3d getWanderTarget() {
		return TargetUtil.find(mob, SWIM_HORIZONTAL_RANGE, SWIM_VERTICAL_RANGE);
	}
}
