package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.ai.brain.*;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * {@code FollowCustomerTask}.
 */
public class FollowCustomerTask extends MultiTickTask<VillagerEntity> {

	private final float speed;

	public FollowCustomerTask(float speed) {
		super(
				ImmutableMap.of(
						MemoryModuleType.WALK_TARGET,
						MemoryModuleState.REGISTERED,
						MemoryModuleType.LOOK_TARGET,
						MemoryModuleState.REGISTERED
				),
				Integer.MAX_VALUE
		);
		this.speed = speed;
	}

	/**
	 * Определяет, следует ли run.
	 *
	 * @param serverWorld server world
	 * @param villagerEntity villager entity
	 *
	 * @return boolean — результат операции
	 */
	protected boolean shouldRun(ServerWorld serverWorld, VillagerEntity villagerEntity) {
		PlayerEntity playerEntity = villagerEntity.getCustomer();
		return villagerEntity.isAlive()
				&& playerEntity != null
				&& !villagerEntity.isTouchingWater()
				&& !villagerEntity.knockedBack
				&& villagerEntity.squaredDistanceTo(playerEntity) <= 16.0;
	}

	/**
	 * Определяет, следует ли keep running.
	 *
	 * @param serverWorld server world
	 * @param villagerEntity villager entity
	 * @param l l
	 *
	 * @return boolean — результат операции
	 */
	protected boolean shouldKeepRunning(ServerWorld serverWorld, VillagerEntity villagerEntity, long l) {
		return this.shouldRun(serverWorld, villagerEntity);
	}

	/**
	 * Run.
	 *
	 * @param serverWorld server world
	 * @param villagerEntity villager entity
	 * @param l l
	 */
	protected void run(ServerWorld serverWorld, VillagerEntity villagerEntity, long l) {
		this.update(villagerEntity);
	}

	/**
	 * Finish running.
	 *
	 * @param serverWorld server world
	 * @param villagerEntity villager entity
	 * @param l l
	 */
	protected void finishRunning(ServerWorld serverWorld, VillagerEntity villagerEntity, long l) {
		Brain<?> brain = villagerEntity.getBrain();
		brain.forget(MemoryModuleType.WALK_TARGET);
		brain.forget(MemoryModuleType.LOOK_TARGET);
	}

	/**
	 * Keep running.
	 *
	 * @param serverWorld server world
	 * @param villagerEntity villager entity
	 * @param l l
	 */
	protected void keepRunning(ServerWorld serverWorld, VillagerEntity villagerEntity, long l) {
		this.update(villagerEntity);
	}

	@Override
	protected boolean isTimeLimitExceeded(long time) {
		return false;
	}

	private void update(VillagerEntity villager) {
		Brain<?> brain = villager.getBrain();
		brain.remember(
				MemoryModuleType.WALK_TARGET,
				new WalkTarget(new EntityLookTarget(villager.getCustomer(), false), this.speed, 2)
		);
		brain.remember(MemoryModuleType.LOOK_TARGET, new EntityLookTarget(villager.getCustomer(), true));
	}
}
