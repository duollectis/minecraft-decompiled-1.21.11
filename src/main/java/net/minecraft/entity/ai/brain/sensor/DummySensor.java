package net.minecraft.entity.ai.brain.sensor;

import com.google.common.collect.ImmutableSet;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.server.world.ServerWorld;

import java.util.Set;

/**
 * {@code DummySensor}.
 */
public class DummySensor extends Sensor<LivingEntity> {

	@Override
	protected void sense(ServerWorld world, LivingEntity entity) {
	}

	@Override
	public Set<MemoryModuleType<?>> getOutputMemoryModules() {
		return ImmutableSet.of();
	}
}
