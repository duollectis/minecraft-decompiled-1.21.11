package net.minecraft.entity.ai.brain.task;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.BreezeEntity;
import net.minecraft.entity.projectile.BreezeWindChargeEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Unit;

/**
 * Задача мозга бриза, управляющая стрельбой зарядом ветра по цели атаки.
 * Проходит фазы: зарядка (SHOOTING) → выстрел → восстановление → кулдаун.
 */
public class BreezeShootTask extends MultiTickTask<BreezeEntity> {

	private static final int MAX_SQUARED_RANGE = 256;
	private static final int BASE_PROJECTILE_DIVERGENCY = 5;
	private static final int PROJECTILE_DIVERGENCY_DIFFICULTY_MODIFIER = 4;
	private static final float PROJECTILE_SPEED = 0.7F;
	private static final int SHOOT_CHARGING_EXPIRY = Math.round(15.0F);
	private static final int RECOVER_EXPIRY = Math.round(4.0F);
	private static final int SHOOT_COOLDOWN_EXPIRY = Math.round(10.0F);

	@VisibleForTesting
	public BreezeShootTask() {
		super(
				ImmutableMap.of(
						MemoryModuleType.ATTACK_TARGET,
						MemoryModuleState.VALUE_PRESENT,
						MemoryModuleType.BREEZE_SHOOT_COOLDOWN,
						MemoryModuleState.VALUE_ABSENT,
						MemoryModuleType.BREEZE_SHOOT_CHARGING,
						MemoryModuleState.VALUE_ABSENT,
						MemoryModuleType.BREEZE_SHOOT_RECOVER,
						MemoryModuleState.VALUE_ABSENT,
						MemoryModuleType.BREEZE_SHOOT,
						MemoryModuleState.VALUE_PRESENT,
						MemoryModuleType.WALK_TARGET,
						MemoryModuleState.VALUE_ABSENT,
						MemoryModuleType.BREEZE_JUMP_TARGET,
						MemoryModuleState.VALUE_ABSENT
				),
				SHOOT_CHARGING_EXPIRY + 1 + RECOVER_EXPIRY
		);
	}

	@Override
	protected boolean shouldRun(ServerWorld world, BreezeEntity entity) {
		if (entity.getPose() != EntityPose.STANDING) {
			return false;
		}

		return entity.getBrain()
				.getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET)
				.map(target -> isTargetWithinRange(entity, target))
				.map(withinRange -> {
					if (!withinRange) {
						entity.getBrain().forget(MemoryModuleType.BREEZE_SHOOT);
					}

					return withinRange;
				})
				.orElse(false);
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, BreezeEntity entity, long time) {
		Brain<BreezeEntity> brain = entity.getBrain();
		return brain.hasMemoryModule(MemoryModuleType.ATTACK_TARGET)
				&& brain.hasMemoryModule(MemoryModuleType.BREEZE_SHOOT);
	}

	@Override
	protected void run(ServerWorld world, BreezeEntity entity, long time) {
		entity.getBrain()
				.getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET)
				.ifPresent(target -> entity.setPose(EntityPose.SHOOTING));
		entity.getBrain().remember(MemoryModuleType.BREEZE_SHOOT_CHARGING, Unit.INSTANCE, SHOOT_CHARGING_EXPIRY);
		entity.playSound(SoundEvents.ENTITY_BREEZE_INHALE, 1.0F, 1.0F);
	}

	@Override
	protected void finishRunning(ServerWorld world, BreezeEntity entity, long time) {
		if (entity.getPose() == EntityPose.SHOOTING) {
			entity.setPose(EntityPose.STANDING);
		}

		entity.getBrain().remember(MemoryModuleType.BREEZE_SHOOT_COOLDOWN, Unit.INSTANCE, SHOOT_COOLDOWN_EXPIRY);
		entity.getBrain().forget(MemoryModuleType.BREEZE_SHOOT);
	}

	@Override
	protected void keepRunning(ServerWorld world, BreezeEntity entity, long time) {
		Brain<BreezeEntity> brain = entity.getBrain();
		LivingEntity target = brain.getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
		if (target == null) {
			return;
		}

		entity.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, target.getEntityPos());
		if (brain.getOptionalRegisteredMemory(MemoryModuleType.BREEZE_SHOOT_CHARGING).isPresent()
				|| brain.getOptionalRegisteredMemory(MemoryModuleType.BREEZE_SHOOT_RECOVER).isPresent()) {
			return;
		}

		brain.remember(MemoryModuleType.BREEZE_SHOOT_RECOVER, Unit.INSTANCE, RECOVER_EXPIRY);
		double dx = target.getX() - entity.getX();
		double dy = target.getBodyY(target.hasVehicle() ? 0.8 : 0.3) - entity.getChargeY();
		double dz = target.getZ() - entity.getZ();
		ProjectileEntity.spawnWithVelocity(
				new BreezeWindChargeEntity(entity, world),
				world,
				ItemStack.EMPTY,
				dx,
				dy,
				dz,
				PROJECTILE_SPEED,
				BASE_PROJECTILE_DIVERGENCY - world.getDifficulty().getId() * PROJECTILE_DIVERGENCY_DIFFICULTY_MODIFIER
		);
		entity.playSound(SoundEvents.ENTITY_BREEZE_SHOOT, 1.5F, 1.0F);
	}

	private static boolean isTargetWithinRange(BreezeEntity breeze, LivingEntity target) {
		return breeze.getEntityPos().squaredDistanceTo(target.getEntityPos()) < MAX_SQUARED_RANGE;
	}
}
