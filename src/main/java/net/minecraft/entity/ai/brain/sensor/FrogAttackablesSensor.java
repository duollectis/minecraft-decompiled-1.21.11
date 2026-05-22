package net.minecraft.entity.ai.brain.sensor;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.FrogEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Сенсор поиска ближайшей атакуемой цели для лягушки.
 * Цель должна быть допустимой едой лягушки, находиться в радиусе {@code RANGE},
 * не иметь кулдауна охоты и не быть в списке недостижимых целей.
 */
public class FrogAttackablesSensor extends NearestVisibleLivingEntitySensor {

	public static final float RANGE = 10.0F;

	@Override
	protected boolean matches(ServerWorld world, LivingEntity entity, LivingEntity target) {
		return entity.getBrain().hasMemoryModule(MemoryModuleType.HAS_HUNTING_COOLDOWN)
				? false
				: Sensor.testAttackableTargetPredicate(world, entity, target)
						&& FrogEntity.isValidFrogFood(target)
						&& !isTargetUnreachable(entity, target)
						&& target.isInRange(entity, RANGE);
	}

	private boolean isTargetUnreachable(LivingEntity entity, LivingEntity target) {
		List<UUID> unreachable = entity.getBrain()
				.getOptionalRegisteredMemory(MemoryModuleType.UNREACHABLE_TONGUE_TARGETS)
				.orElseGet(ArrayList::new);
		return unreachable.contains(target.getUuid());
	}

	@Override
	protected MemoryModuleType<LivingEntity> getOutputMemoryModule() {
		return MemoryModuleType.NEAREST_ATTACKABLE;
	}
}
