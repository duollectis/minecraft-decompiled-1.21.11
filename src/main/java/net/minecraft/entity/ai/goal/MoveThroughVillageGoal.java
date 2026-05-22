package net.minecraft.entity.ai.goal;

import com.google.common.collect.Lists;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.NavigationConditions;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.registry.tag.PointOfInterestTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.poi.PointOfInterestStorage;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/**
 * Цель перемещения через деревню: ищет занятую точку интереса (POI) в радиусе
 * и прокладывает к ней путь, при необходимости открывая двери.
 * Запоминает до 15 посещённых точек, чтобы не возвращаться к ним.
 */
public class MoveThroughVillageGoal extends Goal {

	private static final int FUZZY_HORIZONTAL_RANGE = 15;
	private static final int FUZZY_VERTICAL_RANGE = 7;
	private static final int POI_NEAR_RADIUS = 6;
	private static final int POI_SEARCH_RADIUS = 10;
	private static final int FALLBACK_HORIZONTAL_RANGE = 10;
	private static final int MAX_VISITED_TARGETS = 15;

	protected final PathAwareEntity mob;
	private final double speed;
	private @Nullable Path targetPath;
	private BlockPos target;
	private final boolean requiresNighttime;
	private final List<BlockPos> visitedTargets = Lists.newArrayList();
	private final int distance;
	private final BooleanSupplier doorPassingThroughGetter;

	public MoveThroughVillageGoal(
		PathAwareEntity entity,
		double speed,
		boolean requiresNighttime,
		int distance,
		BooleanSupplier doorPassingThroughGetter
	) {
		this.mob = entity;
		this.speed = speed;
		this.requiresNighttime = requiresNighttime;
		this.distance = distance;
		this.doorPassingThroughGetter = doorPassingThroughGetter;
		this.setControls(EnumSet.of(Goal.Control.MOVE));

		if (!NavigationConditions.hasMobNavigation(entity)) {
			throw new IllegalArgumentException("Unsupported mob for MoveThroughVillageGoal");
		}
	}

	@Override
	public boolean canStart() {
		if (!NavigationConditions.hasMobNavigation(mob)) {
			return false;
		}

		forgetOldTarget();

		if (requiresNighttime && mob.getEntityWorld().isDay()) {
			return false;
		}

		ServerWorld serverWorld = (ServerWorld) mob.getEntityWorld();
		BlockPos mobPos = mob.getBlockPos();

		if (!serverWorld.isNearOccupiedPointOfInterest(mobPos, POI_NEAR_RADIUS)) {
			return false;
		}

		Vec3d fuzzyTarget = FuzzyTargeting.find(
			mob,
			FUZZY_HORIZONTAL_RANGE,
			FUZZY_VERTICAL_RANGE,
			pos -> {
				if (!serverWorld.isNearOccupiedPointOfInterest(pos)) {
					return Double.NEGATIVE_INFINITY;
				}

				Optional<BlockPos> nearestPoi = serverWorld.getPointOfInterestStorage()
					.getPosition(
						poiType -> poiType.isIn(PointOfInterestTypeTags.VILLAGE),
						this::shouldVisit,
						pos,
						POI_SEARCH_RADIUS,
						PointOfInterestStorage.OccupationStatus.IS_OCCUPIED
					);

				return nearestPoi
					.<Double>map(poiPos -> -poiPos.getSquaredDistance(mobPos))
					.orElse(Double.NEGATIVE_INFINITY);
			}
		);

		if (fuzzyTarget == null) {
			return false;
		}

		Optional<BlockPos> poiPos = serverWorld.getPointOfInterestStorage()
			.getPosition(
				poiType -> poiType.isIn(PointOfInterestTypeTags.VILLAGE),
				this::shouldVisit,
				BlockPos.ofFloored(fuzzyTarget),
				POI_SEARCH_RADIUS,
				PointOfInterestStorage.OccupationStatus.IS_OCCUPIED
			);

		if (poiPos.isEmpty()) {
			return false;
		}

		target = poiPos.get().toImmutable();
		EntityNavigation navigation = mob.getNavigation();
		navigation.setCanOpenDoors(doorPassingThroughGetter.getAsBoolean());
		targetPath = navigation.findPathTo(target, 0);
		navigation.setCanOpenDoors(true);

		if (targetPath == null) {
			Vec3d fallback = NoPenaltyTargeting.findTo(
				mob,
				FALLBACK_HORIZONTAL_RANGE,
				FUZZY_VERTICAL_RANGE,
				Vec3d.ofBottomCenter(target),
				(float) (Math.PI / 2)
			);

			if (fallback == null) {
				return false;
			}

			navigation.setCanOpenDoors(doorPassingThroughGetter.getAsBoolean());
			targetPath = mob.getNavigation().findPathTo(fallback.x, fallback.y, fallback.z, 0);
			navigation.setCanOpenDoors(true);

			if (targetPath == null) {
				return false;
			}
		}

		for (int i = 0; i < targetPath.getLength(); i++) {
			PathNode pathNode = targetPath.getNode(i);
			BlockPos doorPos = new BlockPos(pathNode.x, pathNode.y + 1, pathNode.z);

			if (DoorBlock.canOpenByHand(mob.getEntityWorld(), doorPos)) {
				targetPath = mob.getNavigation().findPathTo(pathNode.x, pathNode.y, pathNode.z, 0);
				break;
			}
		}

		return targetPath != null;
	}

	@Override
	public boolean shouldContinue() {
		return !mob.getNavigation().isIdle()
			&& !target.isWithinDistance(mob.getEntityPos(), mob.getWidth() + distance);
	}

	@Override
	public void start() {
		mob.getNavigation().startMovingAlong(targetPath, speed);
	}

	@Override
	public void stop() {
		if (mob.getNavigation().isIdle() || target.isWithinDistance(mob.getEntityPos(), distance)) {
			visitedTargets.add(target);
		}
	}

	private boolean shouldVisit(BlockPos pos) {
		for (BlockPos visited : visitedTargets) {
			if (Objects.equals(pos, visited)) {
				return false;
			}
		}

		return true;
	}

	private void forgetOldTarget() {
		if (visitedTargets.size() > MAX_VISITED_TARGETS) {
			visitedTargets.remove(0);
		}
	}
}
