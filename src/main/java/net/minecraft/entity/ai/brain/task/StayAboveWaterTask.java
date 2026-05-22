package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;

/**
 * Задача мозга, заставляющая существо выпрыгивать из воды или лавы.
 * С заданной вероятностью активирует прыжок каждый тик, пока существо находится под поверхностью жидкости.
 */
public class StayAboveWaterTask<T extends MobEntity> extends MultiTickTask<T> {

	private final float chance;

	public StayAboveWaterTask(float chance) {
		super(ImmutableMap.of());
		this.chance = chance;
	}

	public static <T extends MobEntity> boolean isUnderwater(T entity) {
		return entity.isTouchingWater() && entity.getFluidHeight(FluidTags.WATER) > entity.getSwimHeight()
				|| entity.isInLava();
	}

	@Override
	protected boolean shouldRun(ServerWorld world, T entity) {
		return isUnderwater(entity);
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, T entity, long time) {
		return shouldRun(world, entity);
	}

	@Override
	protected void keepRunning(ServerWorld world, T entity, long time) {
		if (entity.getRandom().nextFloat() < chance) {
			entity.getJumpControl().setActive();
		}
	}
}
