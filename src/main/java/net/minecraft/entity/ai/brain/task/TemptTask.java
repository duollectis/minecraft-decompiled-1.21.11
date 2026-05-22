package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.EntityLookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import java.util.Optional;
import java.util.function.Function;

/**
 * Задача мозга, заставляющая существо следовать за игроком, держащим приманку.
 * Останавливается при размножении, панике или выходе игрока из зоны видимости; после завершения устанавливает кулдаун.
 */
public class TemptTask extends MultiTickTask<PathAwareEntity> {

	public static final int TEMPTATION_COOLDOWN_TICKS = 100;
	public static final double DEFAULT_STOP_DISTANCE = 2.5;
	public static final double LARGE_ENTITY_STOP_DISTANCE = 3.5;
	private static final int WALK_COMPLETION_RANGE = 2;
	private final Function<LivingEntity, Float> speed;
	private final Function<LivingEntity, Double> stopDistanceGetter;
	private final boolean useEyeHeight;

	public TemptTask(Function<LivingEntity, Float> speed) {
		this(speed, entity -> DEFAULT_STOP_DISTANCE);
	}

	public TemptTask(Function<LivingEntity, Float> speed, Function<LivingEntity, Double> stopDistanceGetter) {
		this(speed, stopDistanceGetter, false);
	}

	public TemptTask(
			Function<LivingEntity, Float> speed,
			Function<LivingEntity, Double> stopDistanceGetter,
			boolean useEyeHeight
	) {
		super(Util.make(() -> {
			Builder<MemoryModuleType<?>, MemoryModuleState> builder = ImmutableMap.builder();
			builder.put(MemoryModuleType.LOOK_TARGET, MemoryModuleState.REGISTERED);
			builder.put(MemoryModuleType.WALK_TARGET, MemoryModuleState.REGISTERED);
			builder.put(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS, MemoryModuleState.VALUE_ABSENT);
			builder.put(MemoryModuleType.IS_TEMPTED, MemoryModuleState.VALUE_ABSENT);
			builder.put(MemoryModuleType.TEMPTING_PLAYER, MemoryModuleState.VALUE_PRESENT);
			builder.put(MemoryModuleType.BREED_TARGET, MemoryModuleState.VALUE_ABSENT);
			builder.put(MemoryModuleType.IS_PANICKING, MemoryModuleState.VALUE_ABSENT);
			return builder.build();
		}));
		this.speed = speed;
		this.stopDistanceGetter = stopDistanceGetter;
		this.useEyeHeight = useEyeHeight;
	}

	protected float getSpeed(PathAwareEntity entity) {
		return speed.apply(entity);
	}

	private Optional<PlayerEntity> getTemptingPlayer(PathAwareEntity entity) {
		return entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.TEMPTING_PLAYER);
	}

	@Override
	protected boolean isTimeLimitExceeded(long time) {
		return false;
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, PathAwareEntity entity, long time) {
		return getTemptingPlayer(entity).isPresent()
				&& !entity.getBrain().hasMemoryModule(MemoryModuleType.BREED_TARGET)
				&& !entity.getBrain().hasMemoryModule(MemoryModuleType.IS_PANICKING);
	}

	@Override
	protected void run(ServerWorld world, PathAwareEntity entity, long time) {
		entity.getBrain().remember(MemoryModuleType.IS_TEMPTED, true);
	}

	@Override
	protected void finishRunning(ServerWorld world, PathAwareEntity entity, long time) {
		Brain<?> brain = entity.getBrain();
		brain.remember(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS, TEMPTATION_COOLDOWN_TICKS);
		brain.forget(MemoryModuleType.IS_TEMPTED);
		brain.forget(MemoryModuleType.WALK_TARGET);
		brain.forget(MemoryModuleType.LOOK_TARGET);
	}

	@Override
	protected void keepRunning(ServerWorld world, PathAwareEntity entity, long time) {
		PlayerEntity temptingPlayer = getTemptingPlayer(entity).get();
		Brain<?> brain = entity.getBrain();
		brain.remember(MemoryModuleType.LOOK_TARGET, new EntityLookTarget(temptingPlayer, true));

		double stopDistance = stopDistanceGetter.apply(entity);

		if (entity.squaredDistanceTo(temptingPlayer) < MathHelper.square(stopDistance)) {
			brain.forget(MemoryModuleType.WALK_TARGET);
		} else {
			brain.remember(
					MemoryModuleType.WALK_TARGET,
					new WalkTarget(
							new EntityLookTarget(temptingPlayer, useEyeHeight, useEyeHeight),
							getSpeed(entity),
							WALK_COMPLETION_RANGE
					)
			);
		}
	}
}
