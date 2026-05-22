package net.minecraft.entity.mob;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

/**
 * Интерфейс для хоглинов.
 */
public interface Hoglin {

	int MIN_MOVEMENT_COOLDOWN = 10;

	float BABY_SPAWN_CHANCE = 0.2F;

	int getMovementCooldownTicks();

	static boolean tryAttack(ServerWorld world, LivingEntity attacker, LivingEntity target) {
		float attackDamage = (float) attacker.getAttributeValue(EntityAttributes.ATTACK_DAMAGE);
		float finalDamage = (!attacker.isBaby() && (int) attackDamage > 0)
				? attackDamage / 2.0F + world.random.nextInt((int) attackDamage)
				: attackDamage;

		DamageSource damageSource = attacker.getDamageSources().mobAttack(attacker);
		boolean hit = target.damage(world, damageSource, finalDamage);

		if (hit) {
			EnchantmentHelper.onTargetDamaged(world, target, damageSource);

			if (!attacker.isBaby()) {
				knockback(attacker, target);
			}
		}

		return hit;
	}

	static void knockback(LivingEntity attacker, LivingEntity target) {
		double knockbackStrength = attacker.getAttributeValue(EntityAttributes.ATTACK_KNOCKBACK);
		double knockbackResistance = target.getAttributeValue(EntityAttributes.KNOCKBACK_RESISTANCE);
		double netKnockback = knockbackStrength - knockbackResistance;

		if (netKnockback <= 0.0) {
			return;
		}

		double deltaX = target.getX() - attacker.getX();
		double deltaZ = target.getZ() - attacker.getZ();
		float yawOffset = attacker.getEntityWorld().random.nextInt(21) - 10;
		double horizontalForce = netKnockback * (attacker.getEntityWorld().random.nextFloat() * 0.5F + 0.2F);
		Vec3d knockbackVec = new Vec3d(deltaX, 0.0, deltaZ).normalize().multiply(horizontalForce).rotateY(yawOffset);
		double verticalForce = netKnockback * attacker.getEntityWorld().random.nextFloat() * 0.5;

		target.addVelocity(knockbackVec.x, verticalForce, knockbackVec.z);
		target.knockedBack = true;
	}
}
