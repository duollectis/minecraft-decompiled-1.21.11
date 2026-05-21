package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;

/**
 * {@code StayAboveWaterTask}.
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

	/**
	 * Определяет, следует ли run.
	 *
	 * @param serverWorld server world
	 * @param mobEntity mob entity
	 *
	 * @return boolean — результат операции
	 */
	protected boolean shouldRun(ServerWorld serverWorld, MobEntity mobEntity) {
		return isUnderwater(mobEntity);
	}

	/**
	 * Определяет, следует ли keep running.
	 *
	 * @param serverWorld server world
	 * @param mobEntity mob entity
	 * @param l l
	 *
	 * @return boolean — результат операции
	 */
	protected boolean shouldKeepRunning(ServerWorld serverWorld, MobEntity mobEntity, long l) {
		return this.shouldRun(serverWorld, mobEntity);
	}

	/**
	 * Keep running.
	 *
	 * @param serverWorld server world
	 * @param mobEntity mob entity
	 * @param l l
	 */
	protected void keepRunning(ServerWorld serverWorld, MobEntity mobEntity, long l) {
		if (mobEntity.getRandom().nextFloat() < this.chance) {
			mobEntity.getJumpControl().setActive();
		}
	}
}
