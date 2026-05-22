package net.minecraft.entity.ai.brain.sensor;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.server.world.ServerWorld;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Сенсор обнаружения угрозы для броненосца.
 * Проверяет список видимых мобов через {@code threateningEntityPredicate} и при обнаружении угрозы
 * записывает в указанный модуль памяти флаг с заданным временем жизни.
 *
 * @param <T> тип сущности-броненосца
 */
public class ArmadilloScareDetectedSensor<T extends LivingEntity> extends Sensor<T> {

	private final BiPredicate<T, LivingEntity> threateningEntityPredicate;
	private final Predicate<T> canRollUpPredicate;
	private final MemoryModuleType<Boolean> memoryModuleType;
	private final int expiry;

	public ArmadilloScareDetectedSensor(
			int senseInterval,
			BiPredicate<T, LivingEntity> threateningEntityPredicate,
			Predicate<T> canRollUpPredicate,
			MemoryModuleType<Boolean> memoryModuleType,
			int expiry
	) {
		super(senseInterval);
		this.threateningEntityPredicate = threateningEntityPredicate;
		this.canRollUpPredicate = canRollUpPredicate;
		this.memoryModuleType = memoryModuleType;
		this.expiry = expiry;
	}

	@Override
	protected void sense(ServerWorld world, T entity) {
		if (canRollUpPredicate.test(entity)) {
			tryDetectThreat(entity);
		} else {
			clear(entity);
		}
	}

	@Override
	public Set<MemoryModuleType<?>> getOutputMemoryModules() {
		return Set.of(MemoryModuleType.MOBS);
	}

	public void tryDetectThreat(T entity) {
		Optional<List<LivingEntity>> mobs = entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.MOBS);

		if (mobs.isEmpty()) {
			return;
		}

		boolean threatDetected = mobs.get().stream().anyMatch(threat -> threateningEntityPredicate.test(entity, threat));

		if (threatDetected) {
			onDetected(entity);
		}
	}

	/** Запоминает факт обнаружения угрозы с заданным временем жизни памяти. */
	public void onDetected(T entity) {
		entity.getBrain().remember(memoryModuleType, true, expiry);
	}

	public void clear(T entity) {
		entity.getBrain().forget(memoryModuleType);
	}
}
