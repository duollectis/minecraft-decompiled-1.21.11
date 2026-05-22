package net.minecraft.entity.ai.brain.sensor;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.server.world.ServerWorld;

/**
 * Сенсор обнаружения ближайшего враждебного моба для жителя деревни.
 * Использует таблицу {@code SQUARED_DISTANCES_FOR_DANGER} для определения типов врагов
 * и их максимальных дистанций опасности.
 */
public class VillagerHostilesSensor extends NearestVisibleLivingEntitySensor {

	private static final ImmutableMap<EntityType<?>, Float> SQUARED_DISTANCES_FOR_DANGER =
			ImmutableMap.<EntityType<?>, Float>builder()
					.put(EntityType.DROWNED, 8.0F)
					.put(EntityType.EVOKER, 12.0F)
					.put(EntityType.HUSK, 8.0F)
					.put(EntityType.ILLUSIONER, 12.0F)
					.put(EntityType.PILLAGER, 15.0F)
					.put(EntityType.RAVAGER, 12.0F)
					.put(EntityType.VEX, 8.0F)
					.put(EntityType.VINDICATOR, 10.0F)
					.put(EntityType.ZOGLIN, 10.0F)
					.put(EntityType.ZOMBIE, 8.0F)
					.put(EntityType.ZOMBIE_VILLAGER, 8.0F)
					.build();

	@Override
	protected boolean matches(ServerWorld world, LivingEntity entity, LivingEntity target) {
		return isHostile(target) && isCloseEnoughForDanger(entity, target);
	}

	private boolean isCloseEnoughForDanger(LivingEntity villager, LivingEntity target) {
		float dangerRadius = SQUARED_DISTANCES_FOR_DANGER.get(target.getType());
		return target.squaredDistanceTo(villager) <= dangerRadius * dangerRadius;
	}

	@Override
	protected MemoryModuleType<LivingEntity> getOutputMemoryModule() {
		return MemoryModuleType.NEAREST_HOSTILE;
	}

	private boolean isHostile(LivingEntity entity) {
		return SQUARED_DISTANCES_FOR_DANGER.containsKey(entity.getType());
	}
}
