package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Optional;

/**
 * Задача мозга, управляющая процессом размножения животных.
 * Ищет партнёра того же типа в памяти {@code VISIBLE_MOBS} и сближает пару до начала разведения.
 */
public class BreedTask extends MultiTickTask<AnimalEntity> {

	private static final int MAX_RANGE = 3;
	private static final int MIN_BREED_TIME = 60;
	private static final int MAX_BREED_DELAY_EXTRA = 50;
	private static final int RUN_TIME = 110;
	private static final int DEFAULT_APPROACH_DISTANCE = 2;
	private final EntityType<? extends AnimalEntity> targetType;
	private final float speed;
	private final int approachDistance;
	private long breedTime;

	public BreedTask(EntityType<? extends AnimalEntity> targetType) {
		this(targetType, 1.0F, 2);
	}

	public BreedTask(EntityType<? extends AnimalEntity> targetType, float speed, int approachDistance) {
		super(
				ImmutableMap.of(
						MemoryModuleType.VISIBLE_MOBS,
						MemoryModuleState.VALUE_PRESENT,
						MemoryModuleType.BREED_TARGET,
						MemoryModuleState.VALUE_ABSENT,
						MemoryModuleType.WALK_TARGET,
						MemoryModuleState.REGISTERED,
						MemoryModuleType.LOOK_TARGET,
						MemoryModuleState.REGISTERED,
						MemoryModuleType.IS_PANICKING,
						MemoryModuleState.VALUE_ABSENT
				),
				RUN_TIME
		);
		this.targetType = targetType;
		this.speed = speed;
		this.approachDistance = approachDistance;
	}

	@Override
	protected boolean shouldRun(ServerWorld world, AnimalEntity entity) {
		return entity.isInLove() && findBreedTarget(entity).isPresent();
	}

	@Override
	protected void run(ServerWorld world, AnimalEntity entity, long time) {
		AnimalEntity partner = findBreedTarget(entity).get();
		entity.getBrain().remember(MemoryModuleType.BREED_TARGET, partner);
		partner.getBrain().remember(MemoryModuleType.BREED_TARGET, entity);
		TargetUtil.lookAtAndWalkTowardsEachOther(entity, partner, speed, approachDistance);
		int delay = MIN_BREED_TIME + entity.getRandom().nextInt(MAX_BREED_DELAY_EXTRA);
		breedTime = time + delay;
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, AnimalEntity entity, long time) {
		if (!hasBreedTarget(entity)) {
			return false;
		}

		AnimalEntity partner = getBreedTarget(entity);
		return partner.isAlive()
				&& entity.canBreedWith(partner)
				&& TargetUtil.canSee(entity.getBrain(), partner)
				&& time <= breedTime
				&& !entity.isPanicking()
				&& !partner.isPanicking();
	}

	@Override
	protected void keepRunning(ServerWorld world, AnimalEntity entity, long time) {
		AnimalEntity partner = getBreedTarget(entity);
		TargetUtil.lookAtAndWalkTowardsEachOther(entity, partner, speed, approachDistance);
		if (entity.isInRange(partner, MAX_RANGE) && time >= breedTime) {
			entity.breed(world, partner);
			entity.getBrain().forget(MemoryModuleType.BREED_TARGET);
			partner.getBrain().forget(MemoryModuleType.BREED_TARGET);
		}
	}

	@Override
	protected void finishRunning(ServerWorld world, AnimalEntity entity, long time) {
		entity.getBrain().forget(MemoryModuleType.BREED_TARGET);
		entity.getBrain().forget(MemoryModuleType.WALK_TARGET);
		entity.getBrain().forget(MemoryModuleType.LOOK_TARGET);
		breedTime = 0L;
	}

	private AnimalEntity getBreedTarget(AnimalEntity animal) {
		return (AnimalEntity) animal.getBrain().getOptionalRegisteredMemory(MemoryModuleType.BREED_TARGET).get();
	}

	private boolean hasBreedTarget(AnimalEntity animal) {
		Brain<?> brain = animal.getBrain();
		return brain.hasMemoryModule(MemoryModuleType.BREED_TARGET)
				&& brain.getOptionalRegisteredMemory(MemoryModuleType.BREED_TARGET).get().getType() == targetType;
	}

	private Optional<? extends AnimalEntity> findBreedTarget(AnimalEntity animal) {
		return animal.getBrain()
				.getOptionalRegisteredMemory(MemoryModuleType.VISIBLE_MOBS)
				.get()
				.findFirst(
						entity -> entity.getType() == targetType
								&& entity instanceof AnimalEntity candidate
								&& animal.canBreedWith(candidate)
								&& !candidate.isPanicking()
				)
				.map(AnimalEntity.class::cast);
	}
}
