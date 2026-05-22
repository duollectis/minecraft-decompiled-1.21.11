package net.minecraft.component.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
	 * Компонент кинетического оружия (трезубец, копьё). Описывает поведение оружия
	 * при использовании: задержку, условия нанесения урона/отбрасывания/спешивания,
	 * множитель урона и звуки.
	 */
public record KineticWeaponComponent(
		int contactCooldownTicks,
		int delayTicks,
		Optional<KineticWeaponComponent.Condition> dismountConditions,
		Optional<KineticWeaponComponent.Condition> knockbackConditions,
		Optional<KineticWeaponComponent.Condition> damageConditions,
		float forwardMovement,
		float damageMultiplier,
		Optional<RegistryEntry<SoundEvent>> sound,
		Optional<RegistryEntry<SoundEvent>> hitSound
) {

	public static final int DEFAULT_CHARGE_TICKS = 10;
	public static final Codec<KineticWeaponComponent> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
										Codecs.NON_NEGATIVE_INT
												.optionalFieldOf("contact_cooldown_ticks", DEFAULT_CHARGE_TICKS)
												.forGetter(KineticWeaponComponent::contactCooldownTicks),
										Codecs.NON_NEGATIVE_INT
												.optionalFieldOf("delay_ticks", 0)
												.forGetter(KineticWeaponComponent::delayTicks),
										KineticWeaponComponent.Condition.CODEC
												.optionalFieldOf("dismount_conditions")
												.forGetter(KineticWeaponComponent::dismountConditions),
										KineticWeaponComponent.Condition.CODEC
												.optionalFieldOf("knockback_conditions")
												.forGetter(KineticWeaponComponent::knockbackConditions),
										KineticWeaponComponent.Condition.CODEC
												.optionalFieldOf("damage_conditions")
												.forGetter(KineticWeaponComponent::damageConditions),
										Codec.FLOAT
												.optionalFieldOf("forward_movement", 0.0F)
												.forGetter(KineticWeaponComponent::forwardMovement),
										Codec.FLOAT
												.optionalFieldOf("damage_multiplier", 1.0F)
												.forGetter(KineticWeaponComponent::damageMultiplier),
										SoundEvent.ENTRY_CODEC.optionalFieldOf("sound").forGetter(KineticWeaponComponent::sound),
										SoundEvent.ENTRY_CODEC.optionalFieldOf("hit_sound").forGetter(KineticWeaponComponent::hitSound)
								)
								.apply(instance, KineticWeaponComponent::new)
	);
	public static final PacketCodec<RegistryByteBuf, KineticWeaponComponent> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT,
			KineticWeaponComponent::contactCooldownTicks,
			PacketCodecs.VAR_INT,
			KineticWeaponComponent::delayTicks,
			KineticWeaponComponent.Condition.PACKET_CODEC.collect(PacketCodecs::optional),
			KineticWeaponComponent::dismountConditions,
			KineticWeaponComponent.Condition.PACKET_CODEC.collect(PacketCodecs::optional),
			KineticWeaponComponent::knockbackConditions,
			KineticWeaponComponent.Condition.PACKET_CODEC.collect(PacketCodecs::optional),
			KineticWeaponComponent::damageConditions,
			PacketCodecs.FLOAT,
			KineticWeaponComponent::forwardMovement,
			PacketCodecs.FLOAT,
			KineticWeaponComponent::damageMultiplier,
			SoundEvent.ENTRY_PACKET_CODEC.collect(PacketCodecs::optional),
			KineticWeaponComponent::sound,
			SoundEvent.ENTRY_PACKET_CODEC.collect(PacketCodecs::optional),
			KineticWeaponComponent::hitSound,
			KineticWeaponComponent::new
	);

	public static Vec3d getAmplifiedMovement(Entity entity) {
		if (!(entity instanceof PlayerEntity) && entity.hasVehicle()) {
			entity = entity.getRootVehicle();
		}

		return entity.getKineticAttackMovement().multiply(20.0);
	}

	/**
		 * Воспроизводит звук использования оружия в позиции сущности.
		 *
		 * @param entity сущность, в позиции которой воспроизводится звук
		 */
	public void playSound(Entity entity) {
		sound.ifPresent(
				soundEntry -> entity.getEntityWorld()
									.playSound(
											entity,
											entity.getX(),
											entity.getY(),
											entity.getZ(),
											(RegistryEntry<SoundEvent>) soundEntry,
											entity.getSoundCategory(),
											1.0F,
											1.0F
									)
		);
	}

	/**
		 * Воспроизводит звук попадания оружия на стороне клиента.
		 *
		 * @param entity сущность-источник звука
		 */
	public void playHitSound(Entity entity) {
		hitSound.ifPresent(
				hitSoundEntry -> entity.getEntityWorld()
										.playSoundFromEntityClient(
												entity,
												hitSoundEntry.value(),
												entity.getSoundCategory(),
												1.0F,
												1.0F
										)
		);
	}

	public int getUseTicks() {
		return this.delayTicks + this.damageConditions
				.map(KineticWeaponComponent.Condition::maxDurationTicks)
				.orElse(0);
	}

	/**
		 * Обрабатывает один тик использования кинетического оружия: собирает коллизии,
		 * проверяет условия урона/отбрасывания/спешивания и применяет эффекты к целям.
		 *
		 * @param stack             стек предмета
		 * @param remainingUseTicks оставшиеся тики использования
		 * @param user              сущность, использующая оружие
		 * @param slot              слот экипировки
		 */
	public void usageTick(ItemStack stack, int remainingUseTicks, LivingEntity user, EquipmentSlot slot) {
		int elapsedTicks = stack.getMaxUseTime(user) - remainingUseTicks;

		if (elapsedTicks < delayTicks) {
			return;
		}

		elapsedTicks -= delayTicks;

		Vec3d lookVector = user.getRotationVector();
		double userDotProduct = lookVector.dotProduct(getAmplifiedMovement(user));
		float playerFactor = user instanceof PlayerEntity ? 1.0F : 0.2F;
		AttackRangeComponent attackRange = user.getAttackRange();
		double baseDamage = user.getAttributeBaseValue(EntityAttributes.ATTACK_DAMAGE);
		boolean anyHit = false;

		for (EntityHitResult entityHitResult : (Collection<EntityHitResult>) ProjectileUtil
				.collectPiercingCollisions(
						user,
						attackRange,
						target -> PiercingWeaponComponent.canHit(user, target),
						RaycastContext.ShapeType.COLLIDER
				)
				.map(blockHit -> List.of(), entityHits -> entityHits)) {
			Entity entity = entityHitResult.getEntity();

			if (entity instanceof EnderDragonPart enderDragonPart) {
				entity = enderDragonPart.owner;
			}

			boolean inCooldown = user.isInPiercingCooldown(entity, contactCooldownTicks);

			if (inCooldown) {
				continue;
			}

			user.startPiercingCooldown(entity);

			double entityDotProduct = lookVector.dotProduct(getAmplifiedMovement(entity));
			double relativeSpeed = Math.max(0.0, userDotProduct - entityDotProduct);
			boolean shouldDismount = dismountConditions.isPresent()
					&& dismountConditions.get().isSatisfied(elapsedTicks, userDotProduct, relativeSpeed, playerFactor);
			boolean shouldKnockback = knockbackConditions.isPresent()
					&& knockbackConditions.get().isSatisfied(elapsedTicks, userDotProduct, relativeSpeed, playerFactor);
			boolean shouldDamage = damageConditions.isPresent()
					&& damageConditions.get().isSatisfied(elapsedTicks, userDotProduct, relativeSpeed, playerFactor);

			if (shouldDismount || shouldKnockback || shouldDamage) {
				float damage = (float) baseDamage + MathHelper.floor(relativeSpeed * damageMultiplier);
				anyHit |= user.pierce(slot, entity, damage, shouldDamage, shouldKnockback, shouldDismount);
			}
		}

		if (anyHit) {
			user.getEntityWorld().sendEntityStatus(user, (byte) 2);

			if (user instanceof ServerPlayerEntity serverPlayerEntity) {
				Criteria.SPEAR_MOBS.trigger(
						serverPlayerEntity,
						user.getPiercedEntityCount(entityx -> entityx instanceof LivingEntity)
				);
			}
		}
	}

	/**
		 * Условие активации эффекта кинетического оружия. Проверяет, что прошедшее время
		 * не превышает максимум, а скорость пользователя и относительная скорость сближения
		 * с целью достаточны для срабатывания.
		 */
	public record Condition(int maxDurationTicks, float minSpeed, float minRelativeSpeed) {

		public static final Codec<KineticWeaponComponent.Condition> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
											Codecs.NON_NEGATIVE_INT
													.fieldOf("max_duration_ticks")
													.forGetter(KineticWeaponComponent.Condition::maxDurationTicks),
											Codec.FLOAT
													.optionalFieldOf("min_speed", 0.0F)
													.forGetter(KineticWeaponComponent.Condition::minSpeed),
											Codec.FLOAT
													.optionalFieldOf("min_relative_speed", 0.0F)
													.forGetter(KineticWeaponComponent.Condition::minRelativeSpeed)
									)
									.apply(instance, KineticWeaponComponent.Condition::new)
		);
		public static final PacketCodec<ByteBuf, KineticWeaponComponent.Condition> PACKET_CODEC = PacketCodec.tuple(
				PacketCodecs.VAR_INT,
				KineticWeaponComponent.Condition::maxDurationTicks,
				PacketCodecs.FLOAT,
				KineticWeaponComponent.Condition::minSpeed,
				PacketCodecs.FLOAT,
				KineticWeaponComponent.Condition::minRelativeSpeed,
				KineticWeaponComponent.Condition::new
		);

		public boolean isSatisfied(int durationTicks, double speed, double relativeSpeed, double minSpeedMultiplier) {
			return durationTicks <= this.maxDurationTicks
					&& speed >= this.minSpeed * minSpeedMultiplier
					&& relativeSpeed >= this.minRelativeSpeed * minSpeedMultiplier;
		}

		public static Optional<KineticWeaponComponent.Condition> ofMinSpeed(int maxDurationTicks, float minSpeed) {
			return Optional.of(new KineticWeaponComponent.Condition(maxDurationTicks, minSpeed, 0.0F));
		}

		public static Optional<KineticWeaponComponent.Condition> ofMinRelativeSpeed(
				int maxDurationTicks,
				float minRelativeSpeed
		) {
			return Optional.of(new KineticWeaponComponent.Condition(maxDurationTicks, 0.0F, minRelativeSpeed));
		}
	}
}
