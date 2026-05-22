package net.minecraft.entity.ai.goal;

import com.google.common.collect.Sets;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.raid.Raid;
import net.minecraft.village.raid.RaidManager;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Цель рейдера: двигаться к центру деревни, если он находится вне зоны POI.
 * Периодически подбирает свободных рейдеров поблизости и добавляет их в рейд.
 */
public class MoveToRaidCenterGoal<T extends RaiderEntity> extends Goal {

	private static final int FREE_RAIDER_CHECK_INTERVAL = 20;
	private static final int SEARCH_HORIZONTAL_RANGE = 15;
	private static final int SEARCH_VERTICAL_RANGE = 4;
	private static final double FREE_RAIDER_SEARCH_RADIUS = 16.0;

	private final T actor;
	private int nextFreeRaiderCheckAge;

	public MoveToRaidCenterGoal(T actor) {
		this.actor = actor;
		this.setControls(EnumSet.of(Goal.Control.MOVE));
	}

	@Override
	public boolean canStart() {
		return actor.getTarget() == null
			&& !actor.hasControllingPassenger()
			&& actor.hasActiveRaid()
			&& !actor.getRaid().isFinished()
			&& !castToServerWorld(actor.getEntityWorld()).isNearOccupiedPointOfInterest(actor.getBlockPos());
	}

	@Override
	public boolean shouldContinue() {
		return actor.hasActiveRaid()
			&& !actor.getRaid().isFinished()
			&& !castToServerWorld(actor.getEntityWorld()).isNearOccupiedPointOfInterest(actor.getBlockPos());
	}

	@Override
	public void tick() {
		if (!actor.hasActiveRaid()) {
			return;
		}

		Raid raid = actor.getRaid();

		if (actor.age > nextFreeRaiderCheckAge) {
			nextFreeRaiderCheckAge = actor.age + FREE_RAIDER_CHECK_INTERVAL;
			includeFreeRaiders(raid);
		}

		if (!actor.isNavigating()) {
			Vec3d target = NoPenaltyTargeting.findTo(
				actor,
				SEARCH_HORIZONTAL_RANGE,
				SEARCH_VERTICAL_RANGE,
				Vec3d.ofBottomCenter(raid.getCenter()),
				(float) (Math.PI / 2)
			);

			if (target != null) {
				actor.getNavigation().startMovingTo(target.x, target.y, target.z, 1.0);
			}
		}
	}

	private void includeFreeRaiders(Raid raid) {
		if (!raid.isActive()) {
			return;
		}

		ServerWorld serverWorld = castToServerWorld(actor.getEntityWorld());
		List<RaiderEntity> nearby = serverWorld.getEntitiesByClass(
			RaiderEntity.class,
			actor.getBoundingBox().expand(FREE_RAIDER_SEARCH_RADIUS),
			raider -> !raider.hasActiveRaid() && RaidManager.isValidRaiderFor(raider)
		);

		Set<RaiderEntity> freeRaiders = Sets.newHashSet(nearby);

		for (RaiderEntity raider : freeRaiders) {
			raid.addRaider(serverWorld, raid.getGroupsSpawned(), raider, null, true);
		}
	}
}
