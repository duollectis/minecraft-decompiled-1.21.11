package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.EntityLookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * Задача мозга, подготавливающая таранный удар: ищет позицию разгона и ждёт время подготовки.
 * После завершения подготовки устанавливает память {@code RAM_TARGET} для выполнения удара.
 */
public class PrepareRamTask<E extends PathAwareEntity> extends MultiTickTask<E> {

	public static final int RUN_TIME = 160;

	private static final double RAM_TARGET_OFFSET = 0.5;
	private static final byte STATUS_CANCEL_RAM = 59;
	private static final byte STATUS_START_PREPARE = 58;

	private final ToIntFunction<E> cooldownFactory;
	private final int minRamDistance;
	private final int maxRamDistance;
	private final float speed;
	private final TargetPredicate targetPredicate;
	private final int prepareTime;
	private final Function<E, SoundEvent> soundFactory;
	private Optional<Long> prepareStartTime = Optional.empty();
	private Optional<PrepareRamTask.Ram> ram = Optional.empty();

	public PrepareRamTask(
			ToIntFunction<E> cooldownFactory,
			int minDistance,
			int maxDistance,
			float speed,
			TargetPredicate targetPredicate,
			int prepareTime,
			Function<E, SoundEvent> soundFactory
	) {
		super(
				ImmutableMap.of(
						MemoryModuleType.LOOK_TARGET,
						MemoryModuleState.REGISTERED,
						MemoryModuleType.RAM_COOLDOWN_TICKS,
						MemoryModuleState.VALUE_ABSENT,
						MemoryModuleType.VISIBLE_MOBS,
						MemoryModuleState.VALUE_PRESENT,
						MemoryModuleType.RAM_TARGET,
						MemoryModuleState.VALUE_ABSENT
				),
				RUN_TIME
		);
		this.cooldownFactory = cooldownFactory;
		this.minRamDistance = minDistance;
		this.maxRamDistance = maxDistance;
		this.speed = speed;
		this.targetPredicate = targetPredicate;
		this.prepareTime = prepareTime;
		this.soundFactory = soundFactory;
	}

	@Override
	protected void run(ServerWorld world, E entity, long time) {
		entity.getBrain()
		      .getOptionalRegisteredMemory(MemoryModuleType.VISIBLE_MOBS)
		      .flatMap(mobs -> mobs.findFirst(mob -> targetPredicate.test(world, entity, mob)))
		      .ifPresent(mob -> findRam(entity, mob));
	}

	@Override
	protected void finishRunning(ServerWorld world, E entity, long time) {
		Brain<?> brain = entity.getBrain();

		if (!brain.hasMemoryModule(MemoryModuleType.RAM_TARGET)) {
			world.sendEntityStatus(entity, STATUS_CANCEL_RAM);
			brain.remember(MemoryModuleType.RAM_COOLDOWN_TICKS, cooldownFactory.applyAsInt(entity));
		}
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, E entity, long time) {
		return ram.isPresent() && ram.get().entity().isAlive();
	}

	@Override
	protected void keepRunning(ServerWorld world, E entity, long time) {
		if (ram.isEmpty()) {
			return;
		}

		entity.getBrain().remember(MemoryModuleType.WALK_TARGET, new WalkTarget(ram.get().start(), speed, 0));
		entity.getBrain().remember(MemoryModuleType.LOOK_TARGET, new EntityLookTarget(ram.get().entity(), true));

		boolean targetMoved = !ram.get().entity().getBlockPos().equals(ram.get().end());

		if (targetMoved) {
			world.sendEntityStatus(entity, STATUS_CANCEL_RAM);
			entity.getNavigation().stop();
			findRam(entity, ram.get().entity());
			return;
		}

		BlockPos entityPos = entity.getBlockPos();

		if (!entityPos.equals(ram.get().start())) {
			return;
		}

		world.sendEntityStatus(entity, STATUS_START_PREPARE);

		if (prepareStartTime.isEmpty()) {
			prepareStartTime = Optional.of(time);
		}

		if (time - prepareStartTime.get() < prepareTime) {
			return;
		}

		entity.getBrain().remember(
				MemoryModuleType.RAM_TARGET,
				calculateRamTarget(entityPos, ram.get().end())
		);
		world.playSoundFromEntity(
				null,
				entity,
				soundFactory.apply(entity),
				SoundCategory.NEUTRAL,
				1.0F,
				entity.getSoundPitch()
		);
		ram = Optional.empty();
	}

	private Vec3d calculateRamTarget(BlockPos start, BlockPos end) {
		double offsetX = RAM_TARGET_OFFSET * MathHelper.sign(end.getX() - start.getX());
		double offsetZ = RAM_TARGET_OFFSET * MathHelper.sign(end.getZ() - start.getZ());
		return Vec3d.ofBottomCenter(end).add(offsetX, 0.0, offsetZ);
	}

	private Optional<BlockPos> findRamStart(PathAwareEntity entity, LivingEntity target) {
		BlockPos targetPos = target.getBlockPos();

		if (!canReach(entity, targetPos)) {
			return Optional.empty();
		}

		List<BlockPos> candidates = new ArrayList<>();
		BlockPos.Mutable mutable = targetPos.mutableCopy();

		for (Direction direction : Direction.Type.HORIZONTAL) {
			mutable.set(targetPos);

			for (int step = 0; step < maxRamDistance; step++) {
				if (!canReach(entity, mutable.move(direction))) {
					mutable.move(direction.getOpposite());
					break;
				}
			}

			if (mutable.getManhattanDistance(targetPos) >= minRamDistance) {
				candidates.add(mutable.toImmutable());
			}
		}

		EntityNavigation navigation = entity.getNavigation();

		return candidates.stream()
		                 .sorted(Comparator.comparingDouble(entity.getBlockPos()::getSquaredDistance))
		                 .filter(start -> {
			                 Path path = navigation.findPathTo(start, 0);
			                 return path != null && path.reachesTarget();
		                 })
		                 .findFirst();
	}

	private boolean canReach(PathAwareEntity entity, BlockPos target) {
		return entity.getNavigation().isValidPosition(target)
				&& entity.getPathfindingPenalty(LandPathNodeMaker.getLandNodeType(entity, target)) == 0.0F;
	}

	private void findRam(PathAwareEntity entity, LivingEntity target) {
		prepareStartTime = Optional.empty();
		ram = findRamStart(entity, target)
				.map(start -> new PrepareRamTask.Ram(start, target.getBlockPos(), target));
	}

	public record Ram(BlockPos start, BlockPos end, LivingEntity entity) {
	}
}
