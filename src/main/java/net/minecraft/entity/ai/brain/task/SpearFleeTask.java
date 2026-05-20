package net.minecraft.entity.ai.brain.task;

import java.util.Map;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.brain.EntityLookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

public class SpearFleeTask extends MultiTickTask<PathAwareEntity> {
   public static final int field_64633 = 9;
   public static final int field_64634 = 11;
   public static final int RUN_TIME = 100;
   double speed;

   public SpearFleeTask(double speedFactor) {
      super(Map.of(MemoryModuleType.SPEAR_STATUS, MemoryModuleState.VALUE_PRESENT), 100);
      this.speed = speedFactor;
   }

   private @Nullable LivingEntity getAttackTarget(PathAwareEntity entity) {
      return entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
   }

   private boolean shouldAttack(PathAwareEntity entity) {
      return this.getAttackTarget(entity) != null && entity.getMainHandStack().contains(DataComponentTypes.KINETIC_WEAPON);
   }

   protected boolean shouldRun(ServerWorld serverWorld, PathAwareEntity pathAwareEntity) {
      if (this.shouldAttack(pathAwareEntity) && !pathAwareEntity.isUsingItem()) {
         if (pathAwareEntity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.SPEAR_STATUS).orElse(SpearChargeTask.AdvanceState.APPROACH)
            != SpearChargeTask.AdvanceState.RETREAT) {
            return false;
         } else {
            LivingEntity livingEntity = this.getAttackTarget(pathAwareEntity);
            double d = pathAwareEntity.squaredDistanceTo(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ());
            int i = pathAwareEntity.hasVehicle() ? 2 : 0;
            double e = Math.sqrt(d);
            Vec3d vec3d = FuzzyTargeting.findFrom(pathAwareEntity, Math.max(0.0, 9 + i - e), Math.max(1.0, 11 + i - e), 7, livingEntity.getEntityPos());
            if (vec3d == null) {
               return false;
            } else {
               pathAwareEntity.getBrain().remember(MemoryModuleType.SPEAR_FLEEING_POSITION, vec3d);
               return true;
            }
         }
      } else {
         return false;
      }
   }

   protected void run(ServerWorld serverWorld, PathAwareEntity pathAwareEntity, long l) {
      pathAwareEntity.setAttacking(true);
      pathAwareEntity.getBrain().remember(MemoryModuleType.SPEAR_FLEEING_TIME, 0);
      super.run(serverWorld, pathAwareEntity, l);
   }

   protected boolean shouldKeepRunning(ServerWorld serverWorld, PathAwareEntity pathAwareEntity, long l) {
      return pathAwareEntity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.SPEAR_FLEEING_TIME).orElse(100) < 100
         && pathAwareEntity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.SPEAR_FLEEING_POSITION).isPresent()
         && !pathAwareEntity.getNavigation().isIdle()
         && this.shouldAttack(pathAwareEntity);
   }

   protected void keepRunning(ServerWorld serverWorld, PathAwareEntity pathAwareEntity, long l) {
      LivingEntity livingEntity = this.getAttackTarget(pathAwareEntity);
      float f = pathAwareEntity.getRootVehicle() instanceof MobEntity mobEntity ? mobEntity.getRiderChargingSpeedMultiplier() : 1.0F;
      pathAwareEntity.getBrain().remember(MemoryModuleType.LOOK_TARGET, new EntityLookTarget(livingEntity, true));
      pathAwareEntity.getBrain()
         .remember(
            MemoryModuleType.SPEAR_FLEEING_TIME, pathAwareEntity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.SPEAR_FLEEING_TIME).orElse(0) + 1
         );
      pathAwareEntity.getBrain()
         .getOptionalRegisteredMemory(MemoryModuleType.SPEAR_FLEEING_POSITION)
         .ifPresent(pos -> pathAwareEntity.getNavigation().startMovingTo(pos.x, pos.y, pos.z, f * this.speed));
   }

   protected void finishRunning(ServerWorld serverWorld, PathAwareEntity pathAwareEntity, long l) {
      pathAwareEntity.getNavigation().stop();
      pathAwareEntity.setAttacking(false);
      pathAwareEntity.clearActiveItem();
      pathAwareEntity.getBrain().forget(MemoryModuleType.SPEAR_FLEEING_TIME);
      pathAwareEntity.getBrain().forget(MemoryModuleType.SPEAR_FLEEING_POSITION);
      pathAwareEntity.getBrain().forget(MemoryModuleType.SPEAR_STATUS);
   }
}
