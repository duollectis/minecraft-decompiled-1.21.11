package net.minecraft.entity.ai.goal;

import net.minecraft.entity.passive.AnimalEntity;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Цель детёныша, следующего за ближайшим взрослым особью того же вида.
 * Активируется только пока животное является детёнышем ({@code breedingAge < 0}).
 */
public class FollowParentGoal extends Goal {

	public static final int HORIZONTAL_CHECK_RANGE = 8;
	public static final int VERTICAL_CHECK_RANGE = 4;
	public static final int MIN_DISTANCE = 3;
	private static final double MIN_FOLLOW_DISTANCE_SQ = 9.0;
	private static final double MAX_FOLLOW_DISTANCE_SQ = 256.0;
	private static final int UPDATE_INTERVAL_TICKS = 10;

	private final AnimalEntity animal;
	private @Nullable AnimalEntity parent;
	private final double speed;
	private int delay;

	public FollowParentGoal(AnimalEntity animal, double speed) {
		this.animal = animal;
		this.speed = speed;
	}

	@Override
	public boolean canStart() {
		if (animal.getBreedingAge() >= 0) {
			return false;
		}

		List<? extends AnimalEntity> nearby = animal
				.getEntityWorld()
				.getNonSpectatingEntities(
						(Class<? extends AnimalEntity>) animal.getClass(),
						animal.getBoundingBox().expand(HORIZONTAL_CHECK_RANGE, VERTICAL_CHECK_RANGE, HORIZONTAL_CHECK_RANGE)
				);
		AnimalEntity closest = null;
		double closestDistSq = Double.MAX_VALUE;

		for (AnimalEntity candidate : nearby) {
			if (candidate.getBreedingAge() >= 0) {
				double distSq = animal.squaredDistanceTo(candidate);
				if (distSq <= closestDistSq) {
					closestDistSq = distSq;
					closest = candidate;
				}
			}
		}

		if (closest == null || closestDistSq < MIN_FOLLOW_DISTANCE_SQ) {
			return false;
		}

		parent = closest;
		return true;
	}

	@Override
	public boolean shouldContinue() {
		if (animal.getBreedingAge() >= 0) {
			return false;
		}

		if (!parent.isAlive()) {
			return false;
		}

		double distSq = animal.squaredDistanceTo(parent);
		return distSq >= MIN_FOLLOW_DISTANCE_SQ && distSq <= MAX_FOLLOW_DISTANCE_SQ;
	}

	@Override
	public void start() {
		delay = 0;
	}

	@Override
	public void stop() {
		parent = null;
	}

	@Override
	public void tick() {
		if (--delay <= 0) {
			delay = getTickCount(UPDATE_INTERVAL_TICKS);
			animal.getNavigation().startMovingTo(parent, speed);
		}
	}
}
