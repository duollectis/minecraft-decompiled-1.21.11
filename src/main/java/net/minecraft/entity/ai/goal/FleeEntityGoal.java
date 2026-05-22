package net.minecraft.entity.ai.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.function.Predicate;

/**
 * Цель побега от сущностей заданного класса.
 * Ищет безопасную позицию подальше от угрозы и бежит к ней,
 * ускоряясь при сближении с преследователем.
 */
public class FleeEntityGoal<T extends LivingEntity> extends Goal {

	private static final double FLEE_SEARCH_HEIGHT = 3.0;
	private static final int FLEE_SEARCH_HORIZONTAL = 16;
	private static final int FLEE_SEARCH_VERTICAL = 7;
	private static final double FAST_SPEED_THRESHOLD_SQ = 49.0;

	protected final PathAwareEntity mob;
	private final double slowSpeed;
	private final double fastSpeed;
	protected @Nullable T targetEntity;
	protected final float fleeDistance;
	protected @Nullable Path fleePath;
	protected final EntityNavigation fleeingEntityNavigation;
	protected final Class<T> classToFleeFrom;
	protected final Predicate<? super LivingEntity> extraInclusionSelector;
	protected final Predicate<? super LivingEntity> inclusionSelector;
	private final TargetPredicate withinRangePredicate;

	public FleeEntityGoal(
			PathAwareEntity mob,
			Class<T> fleeFromType,
			float distance,
			double slowSpeed,
			double fastSpeed
	) {
		this(
				mob,
				fleeFromType,
				entity -> true,
				distance,
				slowSpeed,
				fastSpeed,
				EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR
		);
	}

	public FleeEntityGoal(
			PathAwareEntity mob,
			Class<T> fleeFromType,
			Predicate<LivingEntity> extraInclusionSelector,
			float distance,
			double slowSpeed,
			double fastSpeed,
			Predicate<? super LivingEntity> inclusionSelector
	) {
		this.mob = mob;
		this.classToFleeFrom = fleeFromType;
		this.extraInclusionSelector = extraInclusionSelector;
		this.fleeDistance = distance;
		this.slowSpeed = slowSpeed;
		this.fastSpeed = fastSpeed;
		this.inclusionSelector = inclusionSelector;
		this.fleeingEntityNavigation = mob.getNavigation();
		setControls(EnumSet.of(Goal.Control.MOVE));
		this.withinRangePredicate = TargetPredicate.createAttackable()
				.setBaseMaxDistance(distance)
				.setPredicate((entity, world) -> inclusionSelector.test(entity) && extraInclusionSelector.test(entity));
	}

	public FleeEntityGoal(
			PathAwareEntity mob,
			Class<T> classToFleeFrom,
			float fleeDistance,
			double slowSpeed,
			double fastSpeed,
			Predicate<? super LivingEntity> inclusionSelector
	) {
		this(mob, classToFleeFrom, entity -> true, fleeDistance, slowSpeed, fastSpeed, inclusionSelector);
	}

	@Override
	public boolean canStart() {
		targetEntity = getServerWorld(mob).getClosestEntity(
				mob.getEntityWorld().getEntitiesByClass(
						classToFleeFrom,
						mob.getBoundingBox().expand(fleeDistance, FLEE_SEARCH_HEIGHT, fleeDistance),
						livingEntity -> true
				),
				withinRangePredicate,
				mob,
				mob.getX(),
				mob.getY(),
				mob.getZ()
		);

		if (targetEntity == null) {
			return false;
		}

		Vec3d fleeTarget = NoPenaltyTargeting.findFrom(mob, FLEE_SEARCH_HORIZONTAL, FLEE_SEARCH_VERTICAL, targetEntity.getEntityPos());

		if (fleeTarget == null) {
			return false;
		}

		if (targetEntity.squaredDistanceTo(fleeTarget.x, fleeTarget.y, fleeTarget.z)
				< targetEntity.squaredDistanceTo(mob)) {
			return false;
		}

		fleePath = fleeingEntityNavigation.findPathTo(fleeTarget.x, fleeTarget.y, fleeTarget.z, 0);
		return fleePath != null;
	}

	@Override
	public boolean shouldContinue() {
		return !fleeingEntityNavigation.isIdle();
	}

	@Override
	public void start() {
		fleeingEntityNavigation.startMovingAlong(fleePath, slowSpeed);
	}

	@Override
	public void stop() {
		targetEntity = null;
	}

	@Override
	public void tick() {
		double speed = mob.squaredDistanceTo(targetEntity) < FAST_SPEED_THRESHOLD_SQ ? fastSpeed : slowSpeed;
		mob.getNavigation().setSpeed(speed);
	}
}
