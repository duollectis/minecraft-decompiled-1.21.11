package net.minecraft.entity.conversion;

import java.util.Set;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Scoreboard;

public enum EntityConversionType {
   SINGLE(true) {
      @Override
      public void setUpNewEntity(MobEntity oldEntity, MobEntity newEntity, EntityConversionContext context) {
         Entity entity = oldEntity.getFirstPassenger();
         newEntity.copyPositionAndRotation(oldEntity);
         newEntity.setVelocity(oldEntity.getVelocity());
         if (entity != null) {
            entity.stopRiding();
            entity.ridingCooldown = 0;

            for (Entity entity2 : newEntity.getPassengerList()) {
               entity2.stopRiding();
               entity2.remove(Entity.RemovalReason.DISCARDED);
            }

            entity.startRiding(newEntity);
         }

         Entity entity3 = oldEntity.getVehicle();
         if (entity3 != null) {
            oldEntity.stopRiding();
            newEntity.startRiding(entity3, false, false);
         }

         if (context.keepEquipment()) {
            for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {
               ItemStack itemStack = oldEntity.getEquippedStack(equipmentSlot);
               if (!itemStack.isEmpty()) {
                  newEntity.equipStack(equipmentSlot, itemStack.copyAndEmpty());
                  newEntity.setEquipmentDropChance(equipmentSlot, oldEntity.getEquipmentDropChances().get(equipmentSlot));
               }
            }
         }

         newEntity.fallDistance = oldEntity.fallDistance;
         newEntity.setFlag(7, oldEntity.isGliding());
         newEntity.playerHitTimer = oldEntity.playerHitTimer;
         newEntity.hurtTime = oldEntity.hurtTime;
         newEntity.bodyYaw = oldEntity.bodyYaw;
         newEntity.setOnGround(oldEntity.isOnGround());
         oldEntity.getSleepingPosition().ifPresent(newEntity::setSleepingPosition);
         Entity entity2 = oldEntity.getLeashHolder();
         if (entity2 != null) {
            newEntity.attachLeash(entity2, true);
         }

         this.copyData(oldEntity, newEntity, context);
      }
   },
   SPLIT_ON_DEATH(false) {
      @Override
      public void setUpNewEntity(MobEntity oldEntity, MobEntity newEntity, EntityConversionContext context) {
         Entity entity = oldEntity.getFirstPassenger();
         if (entity != null) {
            entity.stopRiding();
         }

         Entity entity2 = oldEntity.getLeashHolder();
         if (entity2 != null) {
            oldEntity.detachLeash();
         }

         this.copyData(oldEntity, newEntity, context);
      }
   };

   private static final Set<ComponentType<?>> CUSTOM_COMPONENTS = Set.of(DataComponentTypes.CUSTOM_NAME, DataComponentTypes.CUSTOM_DATA);
   private final boolean discardOldEntity;

   EntityConversionType(final boolean discardOldEntity) {
      this.discardOldEntity = discardOldEntity;
   }

   public boolean shouldDiscardOldEntity() {
      return this.discardOldEntity;
   }

   public abstract void setUpNewEntity(MobEntity oldEntity, MobEntity newEntity, EntityConversionContext context);

   void copyData(MobEntity oldEntity, MobEntity newEntity, EntityConversionContext context) {
      newEntity.setAbsorptionAmount(oldEntity.getAbsorptionAmount());

      for (StatusEffectInstance statusEffectInstance : oldEntity.getStatusEffects()) {
         newEntity.addStatusEffect(new StatusEffectInstance(statusEffectInstance));
      }

      if (oldEntity.isBaby()) {
         newEntity.setBaby(true);
      }

      if (oldEntity instanceof PassiveEntity passiveEntity && newEntity instanceof PassiveEntity passiveEntity2) {
         passiveEntity2.setBreedingAge(passiveEntity.getBreedingAge());
         passiveEntity2.forcedAge = passiveEntity.forcedAge;
         passiveEntity2.happyTicksRemaining = passiveEntity.happyTicksRemaining;
      }

      Brain<?> brain = oldEntity.getBrain();
      Brain<?> brain2 = newEntity.getBrain();
      if (brain.isMemoryInState(MemoryModuleType.ANGRY_AT, MemoryModuleState.REGISTERED) && brain.hasMemoryModule(MemoryModuleType.ANGRY_AT)) {
         brain2.remember(MemoryModuleType.ANGRY_AT, brain.getOptionalRegisteredMemory(MemoryModuleType.ANGRY_AT));
      }

      if (context.preserveCanPickUpLoot()) {
         newEntity.setCanPickUpLoot(oldEntity.canPickUpLoot());
      }

      newEntity.setLeftHanded(oldEntity.isLeftHanded());
      newEntity.setAiDisabled(oldEntity.isAiDisabled());
      if (oldEntity.isPersistent()) {
         newEntity.setPersistent();
      }

      newEntity.setCustomNameVisible(oldEntity.isCustomNameVisible());
      newEntity.setOnFire(oldEntity.isOnFire());
      newEntity.setInvulnerable(oldEntity.isInvulnerable());
      newEntity.setNoGravity(oldEntity.hasNoGravity());
      newEntity.setPortalCooldown(oldEntity.getPortalCooldown());
      newEntity.setSilent(oldEntity.isSilent());
      oldEntity.getCommandTags().forEach(newEntity::addCommandTag);

      for (ComponentType<?> componentType : CUSTOM_COMPONENTS) {
         copyComponent(oldEntity, newEntity, componentType);
      }

      if (context.team() != null) {
         Scoreboard scoreboard = newEntity.getEntityWorld().getScoreboard();
         scoreboard.addScoreHolderToTeam(newEntity.getUuidAsString(), context.team());
         if (oldEntity.getScoreboardTeam() != null && oldEntity.getScoreboardTeam() == context.team()) {
            scoreboard.removeScoreHolderFromTeam(oldEntity.getUuidAsString(), oldEntity.getScoreboardTeam());
         }
      }

      if (oldEntity instanceof ZombieEntity zombieEntity && zombieEntity.canBreakDoors() && newEntity instanceof ZombieEntity zombieEntity2) {
         zombieEntity2.setCanBreakDoors(true);
      }
   }

   private static <T> void copyComponent(MobEntity oldEntity, MobEntity newEntity, ComponentType<T> type) {
      T object = oldEntity.get(type);
      if (object != null) {
         newEntity.setComponent(type, object);
      }
   }
}
