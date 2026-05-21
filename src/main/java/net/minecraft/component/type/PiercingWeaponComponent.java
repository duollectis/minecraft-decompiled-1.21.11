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
 * {@code PiercingWeaponComponent}.
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
	 * Play sound.
	 *
	 * @param entity entity
	 */
	public void playSound(Entity entity) {
		this.sound
				.ifPresent(
						sound -> entity.getEntityWorld()
						               .playSound(
								               entity,
								               entity.getX(),
								               entity.getY(),
								               entity.getZ(),
								               (RegistryEntry<SoundEvent>) sound,
								               entity.getSoundCategory(),
								               1.0F,
								               1.0F
						               )
				);
	}

	/**
	 * Play hit sound.
	 *
	 * @param entity entity
	 */
	public void playHitSound(Entity entity) {
		this.hitSound
				.ifPresent(
						sound -> entity.getEntityWorld()
						               .playSound(
								               null,
								               entity.getX(),
								               entity.getY(),
								               entity.getZ(),
								               (RegistryEntry<SoundEvent>) sound,
								               entity.getSoundCategory(),
								               1.0F,
								               1.0F
						               )
				);
	}

	/**
	 * Проверяет возможность hit.
	 *
	 * @param attacker attacker
	 * @param target target
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public static boolean canHit(Entity attacker, Entity target) {
		if (target.isInvulnerable() || !target.isAlive()) {
			return false;
		}
		else if (target instanceof InteractionEntity) {
			return true;
		}
		else if (!target.canBeHitByProjectile()) {
			return false;
		}
		else {
			return target instanceof PlayerEntity playerEntity
					       && attacker instanceof PlayerEntity playerEntity2
					       && !playerEntity2.shouldDamagePlayer(playerEntity)
			       ? false
			       : !attacker.isConnectedThroughVehicle(target);
		}
	}

	/**
	 * Stab.
	 *
	 * @param attacker attacker
	 * @param slot slot
	 */
	public void stab(LivingEntity attacker, EquipmentSlot slot) {
		float f = (float) attacker.getAttributeValue(EntityAttributes.ATTACK_DAMAGE);
		AttackRangeComponent attackRangeComponent = attacker.getAttackRange();
		boolean bl = false;

		for (EntityHitResult entityHitResult : (Collection<EntityHitResult>) ProjectileUtil.collectPiercingCollisions(
				                                                                                   attacker, attackRangeComponent, target -> canHit(attacker, target), RaycastContext.ShapeType.COLLIDER
		                                                                                   )
		                                                                                   .map(
				                                                                                   blockHit -> List.of(),
				                                                                                   entityHits -> entityHits
		                                                                                   )) {
			bl |= attacker.pierce(slot, entityHitResult.getEntity(), f, true, this.dealsKnockback, this.dismounts);
		}

		attacker.beforePlayerAttack();
		attacker.useAttackEnchantmentEffects();
		if (bl) {
			this.playHitSound(attacker);
		}

		this.playSound(attacker);
		attacker.swingHand(Hand.MAIN_HAND, false);
	}
}
