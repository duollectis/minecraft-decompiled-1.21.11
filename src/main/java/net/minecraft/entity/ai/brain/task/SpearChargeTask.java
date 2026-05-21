package net.minecraft.entity.ai.brain.task;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.KineticWeaponComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.brain.EntityLookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * {@code SpearChargeTask}.
 */
public class SpearChargeTask extends MultiTickTask<PathAwareEntity> {

	public static final int MIN_CHARGE_DISTANCE = 6;
	public static final int MAX_CHARGE_DISTANCE = 7;
	double chargeStartSpeed;
	double chargeSpeed;
	float squaredMaxDistance;
	float squaredChargeRange;

	public SpearChargeTask(double chargeStartSpeed, double chargeSpeed, float f, float chargeRange) {
		super(Map.of(MemoryModuleType.SPEAR_STATUS, MemoryModuleState.VALUE_PRESENT));
		this.chargeStartSpeed = chargeStartSpeed;
		this.chargeSpeed = chargeSpeed;
		this.squaredMaxDistance = f * f;
		this.squaredChargeRange = chargeRange * chargeRange;
	}

	private @Nullable LivingEntity getTarget(PathAwareEntity entity) {
		return entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
	}

	private boolean shouldAttack(PathAwareEntity entity) {
		return this.getTarget(entity) != null && entity.getMainHandStack().contains(DataComponentTypes.KINETIC_WEAPON);
	}

	private int getSpearUseTicks(PathAwareEntity entity) {
		return Optional
				.ofNullable(entity.getMainHandStack().get(DataComponentTypes.KINETIC_WEAPON))
				.map(KineticWeaponComponent::getUseTicks)
				.orElse(0);
	}

	protected boolean shouldRun(ServerWorld serverWorld, PathAwareEntity pathAwareEntity) {
		return pathAwareEntity
				.getBrain()
				.getOptionalRegisteredMemory(MemoryModuleType.SPEAR_STATUS)
				.orElse(SpearChargeTask.AdvanceState.APPROACH)
				== SpearChargeTask.AdvanceState.CHARGING
				&& this.shouldAttack(pathAwareEntity)
				&& !pathAwareEntity.isUsingItem();
	}

	protected void run(ServerWorld serverWorld, PathAwareEntity pathAwareEntity, long l) {
		pathAwareEntity.setAttacking(true);
		pathAwareEntity.getBrain().remember(MemoryModuleType.SPEAR_ENGAGE_TIME, this.getSpearUseTicks(pathAwareEntity));
		pathAwareEntity.getBrain().forget(MemoryModuleType.SPEAR_CHARGE_POSITION);
		pathAwareEntity.setCurrentHand(Hand.MAIN_HAND);
		super.run(serverWorld, pathAwareEntity, l);
	}

	protected boolean shouldKeepRunning(ServerWorld serverWorld, PathAwareEntity pathAwareEntity, long l) {
		return pathAwareEntity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.SPEAR_ENGAGE_TIME).orElse(0) > 0
				&& this.shouldAttack(pathAwareEntity);
	}

	protected void keepRunning(ServerWorld serverWorld, PathAwareEntity pathAwareEntity, long l) {
		LivingEntity livingEntity = this.getTarget(pathAwareEntity);
		double d = pathAwareEntity.squaredDistanceTo(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ());
		Entity entity = pathAwareEntity.getRootVehicle();
		float f = 1.0F;
		if (entity instanceof MobEntity mobEntity) {
			f = mobEntity.getRiderChargingSpeedMultiplier();
		}

		int i = pathAwareEntity.hasVehicle() ? 2 : 0;
		pathAwareEntity.getBrain().remember(MemoryModuleType.LOOK_TARGET, new EntityLookTarget(livingEntity, true));
		pathAwareEntity.getBrain()
		               .remember(MemoryModuleType.SPEAR_ENGAGE_TIME,
				               pathAwareEntity
						               .getBrain()
						               .getOptionalRegisteredMemory(MemoryModuleType.SPEAR_ENGAGE_TIME)
						               .orElse(0) - 1
		               );
		Vec3d
				vec3d =
				pathAwareEntity
						.getBrain()
						.getOptionalRegisteredMemory(MemoryModuleType.SPEAR_CHARGE_POSITION)
						.orElse(null);
		if (vec3d != null) {
			pathAwareEntity.getNavigation().startMovingTo(vec3d.x, vec3d.y, vec3d.z, f * this.chargeSpeed);
			if (pathAwareEntity.getNavigation().isIdle()) {
				pathAwareEntity.getBrain().forget(MemoryModuleType.SPEAR_CHARGE_POSITION);
			}
		}
		else {
			pathAwareEntity.getNavigation().startMovingTo(livingEntity, f * this.chargeStartSpeed);
			if (d < this.squaredChargeRange || pathAwareEntity.getNavigation().isIdle()) {
				double e = Math.sqrt(d);
				Vec3d
						vec3d2 =
						FuzzyTargeting.findFrom(pathAwareEntity, 6 + i - e, 7 + i - e, 7, livingEntity.getEntityPos());
				pathAwareEntity.getBrain().remember(MemoryModuleType.SPEAR_CHARGE_POSITION, vec3d2);
			}
		}
	}

	protected void finishRunning(ServerWorld serverWorld, PathAwareEntity pathAwareEntity, long l) {
		pathAwareEntity.getNavigation().stop();
		pathAwareEntity.clearActiveItem();
		pathAwareEntity.getBrain().forget(MemoryModuleType.SPEAR_CHARGE_POSITION);
		pathAwareEntity.getBrain().forget(MemoryModuleType.SPEAR_ENGAGE_TIME);
		pathAwareEntity.getBrain().remember(MemoryModuleType.SPEAR_STATUS, SpearChargeTask.AdvanceState.RETREAT);
	}

	@Override
	protected boolean isTimeLimitExceeded(long time) {
		return false;
	}

	/**
	 * {@code AdvanceState}.
	 */
	public static enum AdvanceState {
		APPROACH,
		CHARGING,
		RETREAT;
	}
}
