package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.NoPenaltySolidTargeting;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Задача мозга, заставляющая сущность убегать при получении опасного урона или наличии паники.
 * При горении ищет ближайшую воду; иначе использует переданный {@code pathFinder}.
 */
public class FleeTask<E extends PathAwareEntity> extends MultiTickTask<E> {

	private static final int MIN_RUN_TIME = 100;
	private static final int MAX_RUN_TIME = 120;
	private static final int HORIZONTAL_RANGE = 5;
	private static final int VERTICAL_RANGE = 4;
	private static final int WATER_SEARCH_RADIUS = 5;
	private static final int WATER_SEARCH_HEIGHT = 1;
	private static final int WIDE_ENTITY_WIDTH = 2;
	private static final float FLEE_HALF_PI = (float) (Math.PI / 2);

	private final float speed;
	private final Function<PathAwareEntity, TagKey<DamageType>> entityToDangerousDamageTypes;
	private final Function<E, Vec3d> pathFinder;

	public FleeTask(float speed) {
		this(speed, entity -> DamageTypeTags.PANIC_CAUSES, entity -> FuzzyTargeting.find(entity, HORIZONTAL_RANGE, VERTICAL_RANGE));
	}

	public FleeTask(float speed, int startHeight) {
		this(
				speed,
				entity -> DamageTypeTags.PANIC_CAUSES,
				entity -> NoPenaltySolidTargeting.find(
						entity,
						HORIZONTAL_RANGE,
						VERTICAL_RANGE,
						startHeight,
						entity.getRotationVec(0.0F).x,
						entity.getRotationVec(0.0F).z,
						FLEE_HALF_PI
				)
		);
	}

	public FleeTask(float speed, Function<PathAwareEntity, TagKey<DamageType>> entityToDangerousDamageTypes) {
		this(speed, entityToDangerousDamageTypes, entity -> FuzzyTargeting.find(entity, HORIZONTAL_RANGE, VERTICAL_RANGE));
	}

	public FleeTask(
			float speed,
			Function<PathAwareEntity, TagKey<DamageType>> entityToDangerousDamageTypes,
			Function<E, Vec3d> pathFinder
	) {
		super(
				Map.of(
						MemoryModuleType.IS_PANICKING,
						MemoryModuleState.REGISTERED,
						MemoryModuleType.HURT_BY,
						MemoryModuleState.REGISTERED
				),
				MIN_RUN_TIME,
				MAX_RUN_TIME
		);
		this.speed = speed;
		this.entityToDangerousDamageTypes = entityToDangerousDamageTypes;
		this.pathFinder = pathFinder;
	}

	@Override
	protected boolean shouldRun(ServerWorld world, E entity) {
		return entity.getBrain()
				.getOptionalRegisteredMemory(MemoryModuleType.HURT_BY)
				.map(hurtBy -> hurtBy.isIn(entityToDangerousDamageTypes.apply(entity)))
				.orElse(false)
				|| entity.getBrain().hasMemoryModule(MemoryModuleType.IS_PANICKING);
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, E entity, long time) {
		return true;
	}

	@Override
	protected void run(ServerWorld world, E entity, long time) {
		entity.getBrain().remember(MemoryModuleType.IS_PANICKING, true);
		entity.getBrain().forget(MemoryModuleType.WALK_TARGET);
		entity.getNavigation().stop();
	}

	@Override
	protected void finishRunning(ServerWorld world, E entity, long time) {
		entity.getBrain().forget(MemoryModuleType.IS_PANICKING);
	}

	@Override
	protected void keepRunning(ServerWorld world, E entity, long time) {
		if (!entity.getNavigation().isIdle()) {
			return;
		}

		Vec3d fleePos = findTarget(entity, world);

		if (fleePos != null) {
			entity.getBrain().remember(MemoryModuleType.WALK_TARGET, new WalkTarget(fleePos, speed, 0));
		}
	}

	private @Nullable Vec3d findTarget(E entity, ServerWorld world) {
		if (entity.isOnFire()) {
			Optional<Vec3d> water = findClosestWater(world, entity).map(Vec3d::ofBottomCenter);

			if (water.isPresent()) {
				return water.get();
			}
		}

		return pathFinder.apply(entity);
	}

	private Optional<BlockPos> findClosestWater(BlockView world, Entity entity) {
		BlockPos pos = entity.getBlockPos();

		if (!world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()) {
			return Optional.empty();
		}

		Predicate<BlockPos> waterPredicate = MathHelper.ceil(entity.getWidth()) == WIDE_ENTITY_WIDTH
				? p -> BlockPos.streamSouthEastSquare(p).allMatch(sq -> world.getFluidState(sq).isIn(FluidTags.WATER))
				: p -> world.getFluidState(p).isIn(FluidTags.WATER);

		return BlockPos.findClosest(pos, WATER_SEARCH_RADIUS, WATER_SEARCH_HEIGHT, waterPredicate);
	}
}
