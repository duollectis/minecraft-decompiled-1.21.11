package net.minecraft.item;

import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.ToolComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/**
 * Предмет булавы. Наносит дополнительный урон при падении с высоты,
 * а также отбрасывает ближайших существ при ударе о землю.
 */
public class MaceItem extends Item {

	private static final int ATTACK_DAMAGE_MODIFIER_VALUE = 5;
	private static final float ATTACK_SPEED_MODIFIER_VALUE = -3.4F;
	public static final float MINING_SPEED_MULTIPLIER = 1.5F;
	private static final float HEAVY_SMASH_SOUND_FALL_DISTANCE_THRESHOLD = 5.0F;
	public static final float KNOCKBACK_RANGE = 3.5F;
	private static final float KNOCKBACK_POWER = 0.7F;

	/** Пороговые значения дистанции падения для расчёта бонусного урона. */
	private static final double FALL_TIER_1_MAX = 3.0;
	private static final double FALL_TIER_2_MAX = 8.0;
	private static final double FALL_TIER_1_MULTIPLIER = 4.0;
	private static final double FALL_TIER_2_BASE = 12.0;
	private static final double FALL_TIER_2_MULTIPLIER = 2.0;
	private static final double FALL_TIER_3_BASE = 22.0;

	public MaceItem(Item.Settings settings) {
		super(settings);
	}

	public static AttributeModifiersComponent createAttributeModifiers() {
		return AttributeModifiersComponent.builder()
			.add(
				EntityAttributes.ATTACK_DAMAGE,
				new EntityAttributeModifier(
					BASE_ATTACK_DAMAGE_MODIFIER_ID,
					ATTACK_DAMAGE_MODIFIER_VALUE,
					EntityAttributeModifier.Operation.ADD_VALUE
				),
				AttributeModifierSlot.MAINHAND
			)
			.add(
				EntityAttributes.ATTACK_SPEED,
				new EntityAttributeModifier(
					BASE_ATTACK_SPEED_MODIFIER_ID,
					ATTACK_SPEED_MODIFIER_VALUE,
					EntityAttributeModifier.Operation.ADD_VALUE
				),
				AttributeModifierSlot.MAINHAND
			)
			.build();
	}

	public static ToolComponent createToolComponent() {
		return new ToolComponent(List.of(), 1.0F, 2, false);
	}

	@Override
	public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (!shouldDealAdditionalDamage(attacker)) {
			return;
		}

		ServerWorld serverWorld = (ServerWorld) attacker.getEntityWorld();
		attacker.setVelocity(attacker.getVelocity().withAxis(Direction.Axis.Y, 0.01F));

		if (attacker instanceof ServerPlayerEntity serverPlayer) {
			serverPlayer.currentExplosionImpactPos = getCurrentExplosionImpactPos(serverPlayer);
			serverPlayer.setIgnoreFallDamageFromCurrentExplosion(true);
			serverPlayer.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(serverPlayer));
		}

		if (target.isOnGround()) {
			if (attacker instanceof ServerPlayerEntity serverPlayer) {
				serverPlayer.setSpawnExtraParticlesOnFall(true);
			}

			SoundEvent smashSound = attacker.fallDistance > HEAVY_SMASH_SOUND_FALL_DISTANCE_THRESHOLD
				? SoundEvents.ITEM_MACE_SMASH_GROUND_HEAVY
				: SoundEvents.ITEM_MACE_SMASH_GROUND;

			serverWorld.playSound(
				null,
				attacker.getX(),
				attacker.getY(),
				attacker.getZ(),
				smashSound,
				attacker.getSoundCategory(),
				1.0F,
				1.0F
			);
		} else {
			serverWorld.playSound(
				null,
				attacker.getX(),
				attacker.getY(),
				attacker.getZ(),
				SoundEvents.ITEM_MACE_SMASH_AIR,
				attacker.getSoundCategory(),
				1.0F,
				1.0F
			);
		}

		knockbackNearbyEntities(serverWorld, attacker, target);
	}

	private Vec3d getCurrentExplosionImpactPos(ServerPlayerEntity player) {
		return player.shouldIgnoreFallDamageFromCurrentExplosion()
			&& player.currentExplosionImpactPos != null
			&& player.currentExplosionImpactPos.y <= player.getEntityPos().y
			? player.currentExplosionImpactPos
			: player.getEntityPos();
	}

	@Override
	public void postDamageEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (shouldDealAdditionalDamage(attacker)) {
			attacker.onLanding();
		}
	}

	/**
	 * Вычисляет бонусный урон от падения по трёхуровневой формуле:
	 * до 3 блоков — 4× дистанция, до 8 — 12 + 2×(dist-3), свыше 8 — 22 + (dist-8).
	 * Дополнительно учитывает зачарование «Плотный удар».
	 */
	@Override
	public float getBonusAttackDamage(Entity target, float baseAttackDamage, DamageSource damageSource) {
		if (!(damageSource.getSource() instanceof LivingEntity attacker)) {
			return 0.0F;
		}

		if (!shouldDealAdditionalDamage(attacker)) {
			return 0.0F;
		}

		double fallDist = attacker.fallDistance;
		double bonusDamage;

		if (fallDist <= FALL_TIER_1_MAX) {
			bonusDamage = FALL_TIER_1_MULTIPLIER * fallDist;
		} else if (fallDist <= FALL_TIER_2_MAX) {
			bonusDamage = FALL_TIER_2_BASE + FALL_TIER_2_MULTIPLIER * (fallDist - FALL_TIER_1_MAX);
		} else {
			bonusDamage = FALL_TIER_3_BASE + (fallDist - FALL_TIER_2_MAX);
		}

		if (attacker.getEntityWorld() instanceof ServerWorld serverWorld) {
			bonusDamage += EnchantmentHelper.getSmashDamagePerFallenBlock(
				serverWorld,
				attacker.getWeaponStack(),
				target,
				damageSource,
				0.0F
			) * fallDist;
		}

		return (float) bonusDamage;
	}

	private static void knockbackNearbyEntities(World world, Entity attacker, Entity attacked) {
		world.syncWorldEvent(2013, attacked.getSteppingPos(), 750);
		world.getEntitiesByClass(
			LivingEntity.class,
			attacked.getBoundingBox().expand(KNOCKBACK_RANGE),
			getKnockbackPredicate(attacker, attacked)
		).forEach(entity -> {
			Vec3d displacement = entity.getEntityPos().subtract(attacked.getEntityPos());
			double knockback = getKnockback(attacker, entity, displacement);

			if (knockback > 0.0) {
				Vec3d knockbackVec = displacement.normalize().multiply(knockback);
				entity.addVelocity(knockbackVec.x, KNOCKBACK_POWER, knockbackVec.z);

				if (entity instanceof ServerPlayerEntity serverPlayer) {
					serverPlayer.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(serverPlayer));
				}
			}
		});
	}

	private static Predicate<LivingEntity> getKnockbackPredicate(Entity attacker, Entity attacked) {
		return entity -> {
			boolean isNotSpectator = !entity.isSpectator();
			boolean isNotAttackerOrTarget = entity != attacker && entity != attacked;
			boolean isNotTeammate = !attacker.isTeammate(entity);
			boolean isNotOwnerPet = !(
				entity instanceof TameableEntity tameable
					&& attacked instanceof LivingEntity livingAttacked
					&& tameable.isTamed()
					&& tameable.isOwner(livingAttacked)
			);
			boolean isNotMarkerArmorStand = !(entity instanceof ArmorStandEntity stand && stand.isMarker());
			boolean isInRange = attacked.squaredDistanceTo(entity) <= Math.pow(KNOCKBACK_RANGE, 2.0);
			boolean isNotCreativeFlying = !(
				entity instanceof PlayerEntity player
					&& player.isCreative()
					&& player.getAbilities().flying
			);

			return isNotSpectator
				&& isNotAttackerOrTarget
				&& isNotTeammate
				&& isNotOwnerPet
				&& isNotMarkerArmorStand
				&& isInRange
				&& isNotCreativeFlying;
		};
	}

	private static double getKnockback(Entity attacker, LivingEntity attacked, Vec3d distance) {
		return (KNOCKBACK_RANGE - distance.length())
			* KNOCKBACK_POWER
			* (attacker.fallDistance > HEAVY_SMASH_SOUND_FALL_DISTANCE_THRESHOLD ? 2 : 1)
			* (1.0 - attacked.getAttributeValue(EntityAttributes.KNOCKBACK_RESISTANCE));
	}

	/**
	 * Проверяет, должна ли булава наносить дополнительный урон от падения.
	 * Условие: дистанция падения больше {@link #MINING_SPEED_MULTIPLIER} и атакующий не планирует.
	 */
	public static boolean shouldDealAdditionalDamage(LivingEntity attacker) {
		return attacker.fallDistance > MINING_SPEED_MULTIPLIER && !attacker.isGliding();
	}

	@Override
	public @Nullable DamageSource getDamageSource(LivingEntity user) {
		return shouldDealAdditionalDamage(user)
			? user.getDamageSources().maceSmash(user)
			: super.getDamageSource(user);
	}
}
