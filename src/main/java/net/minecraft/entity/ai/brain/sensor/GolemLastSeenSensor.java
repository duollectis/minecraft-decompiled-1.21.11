package net.minecraft.entity.ai.brain.sensor;

import com.google.common.collect.ImmutableSet;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.server.world.ServerWorld;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Сенсор, отслеживающий факт недавнего обнаружения железного голема.
 * При обнаружении голема в памяти {@code MOBS} записывает флаг {@code GOLEM_DETECTED_RECENTLY}
 * с временем жизни {@code GOLEM_DETECTED_WARMUP} тиков.
 */
public class GolemLastSeenSensor extends Sensor<LivingEntity> {

	private static final int RUN_TIME = 200;
	private static final int GOLEM_DETECTED_WARMUP = 599;

	public GolemLastSeenSensor() {
		this(RUN_TIME);
	}

	public GolemLastSeenSensor(int senseInterval) {
		super(senseInterval);
	}

	@Override
	protected void sense(ServerWorld world, LivingEntity entity) {
		senseIronGolem(entity);
	}

	@Override
	public Set<MemoryModuleType<?>> getOutputMemoryModules() {
		return ImmutableSet.of(MemoryModuleType.MOBS);
	}

	public static void senseIronGolem(LivingEntity entity) {
		Optional<List<LivingEntity>> mobs = entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.MOBS);

		if (mobs.isEmpty()) {
			return;
		}

		boolean golemSeen = mobs.get().stream().anyMatch(seen -> seen.getType().equals(EntityType.IRON_GOLEM));

		if (golemSeen) {
			rememberIronGolem(entity);
		}
	}

	public static void rememberIronGolem(LivingEntity entity) {
		entity.getBrain().remember(MemoryModuleType.GOLEM_DETECTED_RECENTLY, true, GOLEM_DETECTED_WARMUP);
	}
}
