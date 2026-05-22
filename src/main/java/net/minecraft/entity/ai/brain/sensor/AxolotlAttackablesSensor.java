package net.minecraft.entity.ai.brain.sensor;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.world.ServerWorld;

/**
 * Сенсор поиска ближайшей атакуемой цели для аксолотля.
 * Цель должна находиться в воде, в радиусе {@code TARGET_RANGE} и быть либо всегда враждебной,
 * либо подходящей для охоты (при отсутствии кулдауна охоты).
 */
public class AxolotlAttackablesSensor extends NearestVisibleLivingEntitySensor {

	public static final float TARGET_RANGE = 8.0F;
	private static final double TARGET_RANGE_SQUARED = TARGET_RANGE * TARGET_RANGE;

	@Override
	protected boolean matches(ServerWorld world, LivingEntity entity, LivingEntity target) {
		return isInRange(entity, target)
				&& target.isTouchingWater()
				&& (isAlwaysHostileTo(target) || canHunt(entity, target))
				&& Sensor.testAttackableTargetPredicate(world, entity, target);
	}

	private boolean canHunt(LivingEntity axolotl, LivingEntity target) {
		return !axolotl.getBrain().hasMemoryModule(MemoryModuleType.HAS_HUNTING_COOLDOWN)
				&& target.getType().isIn(EntityTypeTags.AXOLOTL_HUNT_TARGETS);
	}

	private boolean isAlwaysHostileTo(LivingEntity target) {
		return target.getType().isIn(EntityTypeTags.AXOLOTL_ALWAYS_HOSTILES);
	}

	private boolean isInRange(LivingEntity axolotl, LivingEntity target) {
		return target.squaredDistanceTo(axolotl) <= TARGET_RANGE_SQUARED;
	}

	@Override
	protected MemoryModuleType<LivingEntity> getOutputMemoryModule() {
		return MemoryModuleType.NEAREST_ATTACKABLE;
	}
}
