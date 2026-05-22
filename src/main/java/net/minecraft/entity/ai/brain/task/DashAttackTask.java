package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Задача мозга животного, выполняющая стремительный бросок к цели атаки.
 * При столкновении наносит урон и отбрасывает цель с учётом эффектов скорости/замедления.
 */
public class DashAttackTask extends MultiTickTask<AnimalEntity> {

	private static final float EFFECT_BONUS_MULTIPLIER = 0.25F;
	private static final float MIN_KNOCKBACK = 0.2F;
	private static final float MAX_KNOCKBACK = 2.0F;

	private final int cooldownTicks;
	private final TargetPredicate predicate;
	private final float speed;
	private final float knockbackStrength;
	private final double maxDistance;
	private final double maxEntitySpeed;
	private final SoundEvent sound;
	private Vec3d velocity;
	private Vec3d lastPos;

	public DashAttackTask(
			int cooldownTicks,
			TargetPredicate predicate,
			float speed,
			float knockbackStrength,
			double maxEntitySpeed,
			double maxDistance,
			SoundEvent sound
	) {
		super(
				ImmutableMap.of(
						MemoryModuleType.CHARGE_COOLDOWN_TICKS,
						MemoryModuleState.VALUE_ABSENT,
						MemoryModuleType.ATTACK_TARGET,
						MemoryModuleState.VALUE_PRESENT
				)
		);
		this.cooldownTicks = cooldownTicks;
		this.predicate = predicate;
		this.speed = speed;
		this.knockbackStrength = knockbackStrength;
		this.maxEntitySpeed = maxEntitySpeed;
		this.maxDistance = maxDistance;
		this.sound = sound;
		velocity = Vec3d.ZERO;
		lastPos = Vec3d.ZERO;
	}

	@Override
	protected boolean shouldRun(ServerWorld world, AnimalEntity entity) {
		return entity.getBrain().hasMemoryModule(MemoryModuleType.ATTACK_TARGET);
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, AnimalEntity entity, long time) {
		Brain<?> brain = entity.getBrain();
		Optional<LivingEntity> attackTarget = brain.getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET);
		if (attackTarget.isEmpty()) {
			return false;
		}

		LivingEntity target = attackTarget.get();
		if (entity instanceof TameableEntity tameable && tameable.isTamed()) {
			return false;
		}

		if (entity.getEntityPos().subtract(lastPos).lengthSquared() >= maxEntitySpeed * maxEntitySpeed) {
			return false;
		}

		if (target.getEntityPos().subtract(entity.getEntityPos()).lengthSquared() >= maxDistance * maxDistance) {
			return false;
		}

		return entity.canSee(target) && !brain.hasMemoryModule(MemoryModuleType.CHARGE_COOLDOWN_TICKS);
	}

	@Override
	protected void run(ServerWorld world, AnimalEntity entity, long time) {
		lastPos = entity.getEntityPos();
		LivingEntity target = entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET).get();
		Vec3d direction = target.getEntityPos().subtract(entity.getEntityPos()).normalize();
		velocity = direction.multiply(speed);
		if (shouldKeepRunning(world, entity, time)) {
			entity.playSoundIfNotSilent(sound);
		}
	}

	@Override
	protected void keepRunning(ServerWorld world, AnimalEntity entity, long time) {
		LivingEntity target = entity.getBrain()
				.getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET)
				.orElseThrow();
		entity.lookAtEntity(target, 360.0F, 360.0F);
		entity.setVelocity(velocity);

		List<LivingEntity> hit = new ArrayList<>(1);
		world.collectEntitiesByType(
				TypeFilter.instanceOf(LivingEntity.class),
				entity.getBoundingBox(),
				candidate -> predicate.test(world, entity, candidate),
				hit,
				1
		);
		if (hit.isEmpty()) {
			return;
		}

		LivingEntity hitTarget = hit.get(0);
		if (entity.hasPassenger(hitTarget)) {
			return;
		}

		attack(world, entity, hitTarget);
		knockbackTarget(entity, hitTarget);
		finishRunning(world, entity, time);
	}

	private void attack(ServerWorld world, AnimalEntity entity, LivingEntity target) {
		DamageSource damageSource = world.getDamageSources().mobAttack(entity);
		float damage = (float) entity.getAttributeValue(EntityAttributes.ATTACK_DAMAGE);
		if (target.damage(world, damageSource, damage)) {
			EnchantmentHelper.onTargetDamaged(world, target, damageSource);
		}
	}

	private void knockbackTarget(AnimalEntity entity, LivingEntity target) {
		int speedAmplifier = entity.hasStatusEffect(StatusEffects.SPEED)
				? entity.getStatusEffect(StatusEffects.SPEED).getAmplifier() + 1
				: 0;
		int slownessAmplifier = entity.hasStatusEffect(StatusEffects.SLOWNESS)
				? entity.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() + 1
				: 0;
		float effectBonus = EFFECT_BONUS_MULTIPLIER * (speedAmplifier - slownessAmplifier);
		float knockback = MathHelper.clamp(
				speed * (float) entity.getAttributeValue(EntityAttributes.MOVEMENT_SPEED),
				MIN_KNOCKBACK,
				MAX_KNOCKBACK
		) + effectBonus;
		entity.knockbackTarget(target, knockback * knockbackStrength, entity.getVelocity());
	}

	@Override
	protected void finishRunning(ServerWorld world, AnimalEntity entity, long time) {
		entity.getBrain().remember(MemoryModuleType.CHARGE_COOLDOWN_TICKS, cooldownTicks);
		entity.getBrain().forget(MemoryModuleType.ATTACK_TARGET);
	}
}
