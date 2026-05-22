package net.minecraft.entity.ai.goal;

import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

/**
 * Цель блуждания с предпочтением дальних позиций: с вероятностью {@code probability}
 * использует {@link FuzzyTargeting} для поиска случайной точки, иначе делегирует родителю.
 */
public class WanderAroundFarGoal extends WanderAroundGoal {

	public static final float CHANCE = 0.001F;

	private static final int WATER_HORIZONTAL_RANGE = 15;
	private static final int LAND_HORIZONTAL_RANGE = 10;
	private static final int VERTICAL_RANGE = 7;

	protected final float probability;

	public WanderAroundFarGoal(PathAwareEntity mob, double speed) {
		this(mob, speed, CHANCE);
	}

	public WanderAroundFarGoal(PathAwareEntity mob, double speed, float probability) {
		super(mob, speed);
		this.probability = probability;
	}

	@Override
	protected @Nullable Vec3d getWanderTarget() {
		if (mob.isTouchingWater()) {
			Vec3d waterTarget = FuzzyTargeting.find(mob, WATER_HORIZONTAL_RANGE, VERTICAL_RANGE);
			return waterTarget == null ? super.getWanderTarget() : waterTarget;
		}

		return mob.getRandom().nextFloat() >= probability
				? FuzzyTargeting.find(mob, LAND_HORIZONTAL_RANGE, VERTICAL_RANGE)
				: super.getWanderTarget();
	}
}
