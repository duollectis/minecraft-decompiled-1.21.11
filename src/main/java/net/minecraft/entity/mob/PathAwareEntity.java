package net.minecraft.entity.mob;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

/**
 * Базовый класс для мобов с поиском пути. Управляет навигацией и привязью.
 */
public abstract class PathAwareEntity extends MobEntity {

	private static final float LEASH_FOLLOW_DISTANCE = 2.0F;

	protected PathAwareEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
		super(entityType, world);
	}

	public float getPathfindingFavor(BlockPos pos) {
		return getPathfindingFavor(pos, getEntityWorld());
	}

	public float getPathfindingFavor(BlockPos pos, WorldView world) {
		return 0.0F;
	}

	@Override
	public boolean canSpawn(WorldAccess world, SpawnReason spawnReason) {
		return getPathfindingFavor(getBlockPos(), world) >= 0.0F;
	}

	public boolean isNavigating() {
		return !getNavigation().isIdle();
	}

	public boolean isPanicking() {
		if (brain.hasMemoryModule(MemoryModuleType.IS_PANICKING)) {
			return brain.getOptionalRegisteredMemory(MemoryModuleType.IS_PANICKING).isPresent();
		}

		for (PrioritizedGoal goal : goalSelector.getGoals()) {
			if (goal.isRunning() && goal.getGoal() instanceof EscapeDangerGoal) {
				return true;
			}
		}

		return false;
	}

	protected boolean shouldFollowLeash() {
		return true;
	}

	@Override
	public void onShortLeashTick(Entity entity) {
		super.onShortLeashTick(entity);
		if (!shouldFollowLeash() || isPanicking()) {
			return;
		}

		goalSelector.enableControl(Goal.Control.MOVE);
		float distanceToHolder = distanceTo(entity);
		Vec3d direction = new Vec3d(
				entity.getX() - getX(),
				entity.getY() - getY(),
				entity.getZ() - getZ()
		)
				.normalize()
				.multiply(Math.max(distanceToHolder - LEASH_FOLLOW_DISTANCE, 0.0F));
		getNavigation()
				.startMovingTo(
						getX() + direction.x,
						getY() + direction.y,
						getZ() + direction.z,
						getFollowLeashSpeed()
				);
	}

	@Override
	public void beforeLeashTick(Entity leashHolder) {
		setPositionTarget(leashHolder.getBlockPos(), (int) getElasticLeashDistance() - 1);
		super.beforeLeashTick(leashHolder);
	}

	protected double getFollowLeashSpeed() {
		return 1.0;
	}
}
