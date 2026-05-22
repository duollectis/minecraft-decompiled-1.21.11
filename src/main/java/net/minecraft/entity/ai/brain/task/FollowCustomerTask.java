package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.EntityLookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Задача мозга жителя, заставляющая его следовать за торговым покупателем-игроком.
 * Активна, пока игрок находится в радиусе {@code MAX_FOLLOW_DISTANCE_SQ} блоков.
 */
public class FollowCustomerTask extends MultiTickTask<VillagerEntity> {

	private static final double MAX_FOLLOW_DISTANCE_SQ = 16.0;
	private static final int FOLLOW_COMPLETION_RANGE = 2;

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

	@Override
	protected boolean shouldRun(ServerWorld world, VillagerEntity entity) {
		PlayerEntity customer = entity.getCustomer();
		return entity.isAlive()
				&& customer != null
				&& !entity.isTouchingWater()
				&& !entity.knockedBack
				&& entity.squaredDistanceTo(customer) <= MAX_FOLLOW_DISTANCE_SQ;
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, VillagerEntity entity, long time) {
		return shouldRun(world, entity);
	}

	@Override
	protected void run(ServerWorld world, VillagerEntity entity, long time) {
		update(entity);
	}

	@Override
	protected void finishRunning(ServerWorld world, VillagerEntity entity, long time) {
		Brain<?> brain = entity.getBrain();
		brain.forget(MemoryModuleType.WALK_TARGET);
		brain.forget(MemoryModuleType.LOOK_TARGET);
	}

	@Override
	protected void keepRunning(ServerWorld world, VillagerEntity entity, long time) {
		update(entity);
	}

	@Override
	protected boolean isTimeLimitExceeded(long time) {
		return false;
	}

	private void update(VillagerEntity villager) {
		Brain<?> brain = villager.getBrain();
		brain.remember(
				MemoryModuleType.WALK_TARGET,
				new WalkTarget(new EntityLookTarget(villager.getCustomer(), false), speed, FOLLOW_COMPLETION_RANGE)
		);
		brain.remember(MemoryModuleType.LOOK_TARGET, new EntityLookTarget(villager.getCustomer(), true));
	}
}
