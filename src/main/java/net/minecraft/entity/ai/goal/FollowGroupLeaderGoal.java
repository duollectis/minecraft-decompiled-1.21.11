package net.minecraft.entity.ai.goal;

import com.mojang.datafixers.DataFixUtils;
import net.minecraft.entity.passive.SchoolingFishEntity;

import java.util.List;
import java.util.function.Predicate;

/**
 * Цель следования за лидером стаи рыб: ищет ближайшую группу и присоединяется к ней.
 * Если лидер уже есть — просто следует за ним; иначе периодически сканирует окружение.
 */
public class FollowGroupLeaderGoal extends Goal {

	private static final int MIN_SEARCH_DELAY = 200;
	private static final double GROUP_SEARCH_RADIUS = 8.0;
	private static final int MOVE_UPDATE_TICKS = 10;

	private final SchoolingFishEntity fish;
	private int moveDelay;
	private int checkSurroundingDelay;

	public FollowGroupLeaderGoal(SchoolingFishEntity fish) {
		this.fish = fish;
		checkSurroundingDelay = getSurroundingSearchDelay(fish);
	}

	protected int getSurroundingSearchDelay(SchoolingFishEntity entity) {
		return toGoalTicks(MIN_SEARCH_DELAY + entity.getRandom().nextInt(MIN_SEARCH_DELAY) % 20);
	}

	@Override
	public boolean canStart() {
		if (fish.hasOtherFishInGroup()) {
			return false;
		}

		if (fish.hasLeader()) {
			return true;
		}

		if (checkSurroundingDelay > 0) {
			checkSurroundingDelay--;
			return false;
		}

		checkSurroundingDelay = getSurroundingSearchDelay(fish);
		Predicate<SchoolingFishEntity> groupPredicate = candidate -> candidate.canHaveMoreFishInGroup() || !candidate.hasLeader();
		List<? extends SchoolingFishEntity> nearby = fish
				.getEntityWorld()
				.getEntitiesByClass(
						(Class<? extends SchoolingFishEntity>) fish.getClass(),
						fish.getBoundingBox().expand(GROUP_SEARCH_RADIUS, GROUP_SEARCH_RADIUS, GROUP_SEARCH_RADIUS),
						groupPredicate
				);
		SchoolingFishEntity leader = (SchoolingFishEntity) DataFixUtils.orElse(
				nearby.stream().filter(SchoolingFishEntity::canHaveMoreFishInGroup).findAny(),
				fish
		);
		leader.pullInOtherFish(nearby.stream().filter(candidate -> !candidate.hasLeader()));
		return fish.hasLeader();
	}

	@Override
	public boolean shouldContinue() {
		return fish.hasLeader() && fish.isCloseEnoughToLeader();
	}

	@Override
	public void start() {
		moveDelay = 0;
	}

	@Override
	public void stop() {
		fish.leaveGroup();
	}

	@Override
	public void tick() {
		if (--moveDelay <= 0) {
			moveDelay = getTickCount(MOVE_UPDATE_TICKS);
			fish.moveTowardLeader();
		}
	}
}
