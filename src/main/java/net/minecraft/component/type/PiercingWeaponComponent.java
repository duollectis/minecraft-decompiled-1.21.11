package net.minecraft.component.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.InteractionEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.RaycastContext;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
	 * Компонент пронизывающего оружия (мач, копьё в режиме удара). Описывает поведение
	 * при ударе: отбрасывание, спешивание, звуки и логику попадания по нескольким целям.
	 */
public record PiercingWeaponComponent(
		boolean dealsKnockback,
		boolean dismounts,
		Optional<RegistryEntry<SoundEvent>> sound,
		Optional<RegistryEntry<SoundEvent>> hitSound
) {

	public static final Codec<PiercingWeaponComponent> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
										Codec.BOOL
												.optionalFieldOf("deals_knockback", true)
												.forGetter(PiercingWeaponComponent::dealsKnockback),
										Codec.BOOL.optionalFieldOf("dismounts", false).forGetter(PiercingWeaponComponent::dismounts),
										SoundEvent.ENTRY_CODEC.optionalFieldOf("sound").forGetter(PiercingWeaponComponent::sound),
										SoundEvent.ENTRY_CODEC.optionalFieldOf("hit_sound").forGetter(PiercingWeaponComponent::hitSound)
								)
								.apply(instance, PiercingWeaponComponent::new)
	);
	public static final PacketCodec<RegistryByteBuf, PiercingWeaponComponent> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.BOOLEAN,
			PiercingWeaponComponent::dealsKnockback,
			PacketCodecs.BOOLEAN,
			PiercingWeaponComponent::dismounts,
			SoundEvent.ENTRY_PACKET_CODEC.collect(PacketCodecs::optional),
			PiercingWeaponComponent::sound,
			SoundEvent.ENTRY_PACKET_CODEC.collect(PacketCodecs::optional),
			PiercingWeaponComponent::hitSound,
			PiercingWeaponComponent::new
	);

	/**
		 * Воспроизводит звук использования оружия в позиции сущности.
		 *
		 * @param entity сущность-источник звука
		 */
	public void playSound(Entity entity) {
		sound.ifPresent(
				soundEntry -> entity.getEntityWorld()
									.playSound(
											entity,
											entity.getX(),
											entity.getY(),
											entity.getZ(),
											soundEntry,
											entity.getSoundCategory(),
											1.0F,
											1.0F
									)
		);
	}

	/**
		 * Воспроизводит звук попадания оружия (без привязки к источнику, слышен всем).
		 *
		 * @param entity сущность, в позиции которой воспроизводится звук
		 */
	public void playHitSound(Entity entity) {
		hitSound.ifPresent(
				soundEntry -> entity.getEntityWorld()
									.playSound(
											null,
											entity.getX(),
											entity.getY(),
											entity.getZ(),
											soundEntry,
											entity.getSoundCategory(),
											1.0F,
											1.0F
									)
		);
	}

	/**
		 * Проверяет, может ли атакующий нанести удар по цели пронизывающим оружием.
		 * Учитывает неуязвимость, живость, тип сущности и правила PvP.
		 *
		 * @param attacker атакующая сущность
		 * @param target   цель атаки
		 * @return {@code true} если удар допустим
		 */
	public static boolean canHit(Entity attacker, Entity target) {
		if (target.isInvulnerable() || !target.isAlive()) {
			return false;
		}

		if (target instanceof InteractionEntity) {
			return true;
		}

		if (!target.canBeHitByProjectile()) {
			return false;
		}

		if (target instanceof PlayerEntity playerTarget
				&& attacker instanceof PlayerEntity playerAttacker
				&& !playerAttacker.shouldDamagePlayer(playerTarget)
		) {
			return false;
		}

		return !attacker.isConnectedThroughVehicle(target);
	}

	/**
		 * Выполняет удар пронизывающим оружием: собирает все цели в зоне атаки,
		 * наносит урон каждой и воспроизводит звуки.
		 *
		 * @param attacker атакующая сущность
		 * @param slot     слот экипировки оружия
		 */
	public void stab(LivingEntity attacker, EquipmentSlot slot) {
		float damage = (float) attacker.getAttributeValue(EntityAttributes.ATTACK_DAMAGE);
		AttackRangeComponent attackRange = attacker.getAttackRange();
		boolean anyHit = false;

		for (EntityHitResult entityHitResult : (Collection<EntityHitResult>) ProjectileUtil
				.collectPiercingCollisions(
						attacker,
						attackRange,
						target -> canHit(attacker, target),
						RaycastContext.ShapeType.COLLIDER
				)
				.map(blockHit -> List.of(), entityHits -> entityHits)) {
			anyHit |= attacker.pierce(slot, entityHitResult.getEntity(), damage, true, dealsKnockback, dismounts);
		}

		attacker.beforePlayerAttack();
		attacker.useAttackEnchantmentEffects();

		if (anyHit) {
			playHitSound(attacker);
		}

		playSound(attacker);
		attacker.swingHand(Hand.MAIN_HAND, false);
	}
}
