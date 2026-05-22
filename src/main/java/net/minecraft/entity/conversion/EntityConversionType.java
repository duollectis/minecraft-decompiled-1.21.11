package net.minecraft.entity.conversion;

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

import java.util.Set;

/**
 * Стратегия конверсии сущности: определяет, как именно старая сущность
 * заменяется новой — полная замена с переносом состояния ({@link #SINGLE})
 * или разделение при смерти без переноса позиции ({@link #SPLIT_ON_DEATH}).
 */
public enum EntityConversionType {
	SINGLE(true) {
		@Override
		public void setUpNewEntity(MobEntity oldEntity, MobEntity newEntity, EntityConversionContext context) {
			Entity firstPassenger = oldEntity.getFirstPassenger();
			newEntity.copyPositionAndRotation(oldEntity);
			newEntity.setVelocity(oldEntity.getVelocity());

			if (firstPassenger != null) {
				firstPassenger.stopRiding();
				firstPassenger.ridingCooldown = 0;

				for (Entity passenger : newEntity.getPassengerList()) {
					passenger.stopRiding();
					passenger.remove(Entity.RemovalReason.DISCARDED);
				}

				firstPassenger.startRiding(newEntity);
			}

			Entity vehicle = oldEntity.getVehicle();
			if (vehicle != null) {
				oldEntity.stopRiding();
				newEntity.startRiding(vehicle, false, false);
			}

			if (context.keepEquipment()) {
				for (EquipmentSlot slot : EquipmentSlot.VALUES) {
					ItemStack stack = oldEntity.getEquippedStack(slot);
					if (stack.isEmpty()) {
						continue;
					}

					newEntity.equipStack(slot, stack.copyAndEmpty());
					newEntity.setEquipmentDropChance(slot, oldEntity.getEquipmentDropChances().get(slot));
				}
			}

			newEntity.fallDistance = oldEntity.fallDistance;
			newEntity.setFlag(7, oldEntity.isGliding());
			newEntity.playerHitTimer = oldEntity.playerHitTimer;
			newEntity.hurtTime = oldEntity.hurtTime;
			newEntity.bodyYaw = oldEntity.bodyYaw;
			newEntity.setOnGround(oldEntity.isOnGround());
			oldEntity.getSleepingPosition().ifPresent(newEntity::setSleepingPosition);

			Entity leashHolder = oldEntity.getLeashHolder();
			if (leashHolder != null) {
				newEntity.attachLeash(leashHolder, true);
			}

			copyData(oldEntity, newEntity, context);
		}
	},
	SPLIT_ON_DEATH(false) {
		@Override
		public void setUpNewEntity(MobEntity oldEntity, MobEntity newEntity, EntityConversionContext context) {
			Entity firstPassenger = oldEntity.getFirstPassenger();
			if (firstPassenger != null) {
				firstPassenger.stopRiding();
			}

			Entity leashHolder = oldEntity.getLeashHolder();
			if (leashHolder != null) {
				oldEntity.detachLeash();
			}

			copyData(oldEntity, newEntity, context);
		}
	};

	private static final Set<ComponentType<?>> CUSTOM_COMPONENTS = Set.of(
		DataComponentTypes.CUSTOM_NAME,
		DataComponentTypes.CUSTOM_DATA
	);

	private final boolean discardOldEntity;

	EntityConversionType(boolean discardOldEntity) {
		this.discardOldEntity = discardOldEntity;
	}

	public boolean shouldDiscardOldEntity() {
		return discardOldEntity;
	}

	public abstract void setUpNewEntity(MobEntity oldEntity, MobEntity newEntity, EntityConversionContext context);

	/**
	 * Копирует общие данные (эффекты, возраст, мозг, флаги, команду скорборда)
	 * из старой сущности в новую. Вызывается обоими вариантами конверсии.
	 *
	 * @param oldEntity исходная сущность
	 * @param newEntity целевая сущность
	 * @param context контекст конверсии с параметрами переноса
	 */
	void copyData(MobEntity oldEntity, MobEntity newEntity, EntityConversionContext context) {
		newEntity.setAbsorptionAmount(oldEntity.getAbsorptionAmount());

		for (StatusEffectInstance effect : oldEntity.getStatusEffects()) {
			newEntity.addStatusEffect(new StatusEffectInstance(effect));
		}

		if (oldEntity.isBaby()) {
			newEntity.setBaby(true);
		}

		if (oldEntity instanceof PassiveEntity oldPassive && newEntity instanceof PassiveEntity newPassive) {
			newPassive.setBreedingAge(oldPassive.getBreedingAge());
			newPassive.forcedAge = oldPassive.forcedAge;
			newPassive.happyTicksRemaining = oldPassive.happyTicksRemaining;
		}

		Brain<?> oldBrain = oldEntity.getBrain();
		Brain<?> newBrain = newEntity.getBrain();
		if (oldBrain.isMemoryInState(MemoryModuleType.ANGRY_AT, MemoryModuleState.REGISTERED)
			&& oldBrain.hasMemoryModule(MemoryModuleType.ANGRY_AT)
		) {
			newBrain.remember(
				MemoryModuleType.ANGRY_AT,
				oldBrain.getOptionalRegisteredMemory(MemoryModuleType.ANGRY_AT)
			);
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

		if (oldEntity instanceof ZombieEntity oldZombie
			&& oldZombie.canBreakDoors()
			&& newEntity instanceof ZombieEntity newZombie
		) {
			newZombie.setCanBreakDoors(true);
		}
	}

	private static <T> void copyComponent(MobEntity oldEntity, MobEntity newEntity, ComponentType<T> type) {
		T value = oldEntity.get(type);
		if (value != null) {
			newEntity.setComponent(type, value);
		}
	}
}
