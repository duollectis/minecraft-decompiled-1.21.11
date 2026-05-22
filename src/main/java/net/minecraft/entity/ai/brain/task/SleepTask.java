package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Задача мозга, укладывающая существо спать в кровати по адресу памяти {@code HOME}.
 * Перед сном закрывает двери через {@link OpenDoorsTask}; не запускается в течение {@code WAKE_COOLDOWN} тиков после пробуждения.
 */
public class SleepTask extends MultiTickTask<LivingEntity> {

	public static final int RUN_TIME = 100;

	private static final long WAKE_COOLDOWN = 100L;
	private static final long WAKE_DELAY = 40L;
	private static final double BED_PROXIMITY = 2.0;
	private static final double SLEEP_PROXIMITY = 1.14;
	private static final double SLEEP_HEIGHT_OFFSET = 0.4;

	private long startTime;

	public SleepTask() {
		super(ImmutableMap.of(
				MemoryModuleType.HOME,
				MemoryModuleState.VALUE_PRESENT,
				MemoryModuleType.LAST_WOKEN,
				MemoryModuleState.REGISTERED
		));
	}

	@Override
	protected boolean shouldRun(ServerWorld world, LivingEntity entity) {
		if (entity.hasVehicle()) {
			return false;
		}

		Brain<?> brain = entity.getBrain();
		GlobalPos home = brain.getOptionalRegisteredMemory(MemoryModuleType.HOME).get();

		if (world.getRegistryKey() != home.dimension()) {
			return false;
		}

		Optional<Long> lastWoken = brain.getOptionalRegisteredMemory(MemoryModuleType.LAST_WOKEN);

		if (lastWoken.isPresent()) {
			long ticksSinceWoken = world.getTime() - lastWoken.get();

			if (ticksSinceWoken > 0L && ticksSinceWoken < WAKE_COOLDOWN) {
				return false;
			}
		}

		BlockState bedState = world.getBlockState(home.pos());
		return home.pos().isWithinDistance(entity.getEntityPos(), BED_PROXIMITY)
				&& bedState.isIn(BlockTags.BEDS)
				&& !bedState.get(BedBlock.OCCUPIED);
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, LivingEntity entity, long time) {
		Optional<GlobalPos> home = entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.HOME);

		if (home.isEmpty()) {
			return false;
		}

		BlockPos bedPos = home.get().pos();
		return entity.getBrain().hasActivity(Activity.REST)
				&& entity.getY() > bedPos.getY() + SLEEP_HEIGHT_OFFSET
				&& bedPos.isWithinDistance(entity.getEntityPos(), SLEEP_PROXIMITY);
	}

	@Override
	protected void run(ServerWorld world, LivingEntity entity, long time) {
		if (time <= startTime) {
			return;
		}

		Brain<?> brain = entity.getBrain();

		if (brain.hasMemoryModule(MemoryModuleType.DOORS_TO_CLOSE)) {
			Set<GlobalPos> doorsToClose = brain.getOptionalRegisteredMemory(MemoryModuleType.DOORS_TO_CLOSE).get();
			Optional<List<LivingEntity>> mobs = brain.hasMemoryModule(MemoryModuleType.MOBS)
					? brain.getOptionalRegisteredMemory(MemoryModuleType.MOBS)
					: Optional.empty();

			OpenDoorsTask.pathToDoor(world, entity, null, null, doorsToClose, mobs);
		}

		entity.sleep(brain.getOptionalRegisteredMemory(MemoryModuleType.HOME).get().pos());
	}

	@Override
	protected boolean isTimeLimitExceeded(long time) {
		return false;
	}

	@Override
	protected void finishRunning(ServerWorld world, LivingEntity entity, long time) {
		if (entity.isSleeping()) {
			entity.wakeUp();
			startTime = time + WAKE_DELAY;
		}
	}
}
