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
 * {@code DashAttackTask}.
 */
public class DashAttackTask extends MultiTickTask<AnimalEntity> {

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
		this.velocity = Vec3d.ZERO;
		this.lastPos = Vec3d.ZERO;
	}

	protected boolean shouldRun(ServerWorld serverWorld, AnimalEntity animalEntity) {
		return animalEntity.getBrain().hasMemoryModule(MemoryModuleType.ATTACK_TARGET);
	}

	protected boolean shouldKeepRunning(ServerWorld serverWorld, AnimalEntity animalEntity, long l) {
		Brain<?> brain = animalEntity.getBrain();
		Optional<LivingEntity> optional = brain.getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET);
		if (optional.isEmpty()) {
			return false;
		}
		else {
			LivingEntity livingEntity = optional.get();
			if (animalEntity instanceof TameableEntity tameableEntity && tameableEntity.isTamed()) {
				return false;
			}
			else if (animalEntity.getEntityPos().subtract(this.lastPos).lengthSquared()
					>= this.maxEntitySpeed * this.maxEntitySpeed) {
				return false;
			}
			else if (livingEntity.getEntityPos().subtract(animalEntity.getEntityPos()).lengthSquared()
					>= this.maxDistance * this.maxDistance) {
				return false;
			}
			else {
				return !animalEntity.canSee(livingEntity) ? false
				                                          : !brain.hasMemoryModule(MemoryModuleType.CHARGE_COOLDOWN_TICKS);
			}
		}
	}

	protected void run(ServerWorld serverWorld, AnimalEntity animalEntity, long l) {
		Brain<?> brain = animalEntity.getBrain();
		this.lastPos = animalEntity.getEntityPos();
		LivingEntity livingEntity = brain.getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET).get();
		Vec3d vec3d = livingEntity.getEntityPos().subtract(animalEntity.getEntityPos()).normalize();
		this.velocity = vec3d.multiply(this.speed);
		if (this.shouldKeepRunning(serverWorld, animalEntity, l)) {
			animalEntity.playSoundIfNotSilent(this.sound);
		}
	}

	protected void keepRunning(ServerWorld serverWorld, AnimalEntity animalEntity, long l) {
		Brain<?> brain = animalEntity.getBrain();
		LivingEntity livingEntity = brain.getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET).orElseThrow();
		animalEntity.lookAtEntity(livingEntity, 360.0F, 360.0F);
		animalEntity.setVelocity(this.velocity);
		List<LivingEntity> list = new ArrayList<>(1);
		serverWorld.collectEntitiesByType(
				TypeFilter.instanceOf(LivingEntity.class),
				animalEntity.getBoundingBox(),
				target -> this.predicate.test(serverWorld, animalEntity, target),
				list,
				1
		);
		if (!list.isEmpty()) {
			LivingEntity livingEntity2 = list.get(0);
			if (animalEntity.hasPassenger(livingEntity2)) {
				return;
			}

			this.attack(serverWorld, animalEntity, livingEntity2);
			this.knockbackTarget(animalEntity, livingEntity2);
			this.finishRunning(serverWorld, animalEntity, l);
		}
	}

	private void attack(ServerWorld world, AnimalEntity entity, LivingEntity target) {
		DamageSource damageSource = world.getDamageSources().mobAttack(entity);
		float f = (float) entity.getAttributeValue(EntityAttributes.ATTACK_DAMAGE);
		if (target.damage(world, damageSource, f)) {
			EnchantmentHelper.onTargetDamaged(world, target, damageSource);
		}
	}

	private void knockbackTarget(AnimalEntity entity, LivingEntity target) {
		int
				i =
				entity.hasStatusEffect(StatusEffects.SPEED) ? entity.getStatusEffect(StatusEffects.SPEED).getAmplifier()
				                                              + 1 : 0;
		int
				j =
				entity.hasStatusEffect(StatusEffects.SLOWNESS) ?
				entity.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() + 1 : 0;
		float f = 0.25F * (i - j);
		float
				g =
				MathHelper.clamp(
						this.speed * (float) entity.getAttributeValue(EntityAttributes.MOVEMENT_SPEED),
						0.2F,
						2.0F
				) + f;
		entity.knockbackTarget(target, g * this.knockbackStrength, entity.getVelocity());
	}

	protected void finishRunning(ServerWorld serverWorld, AnimalEntity animalEntity, long l) {
		animalEntity.getBrain().remember(MemoryModuleType.CHARGE_COOLDOWN_TICKS, this.cooldownTicks);
		animalEntity.getBrain().forget(MemoryModuleType.ATTACK_TARGET);
	}
}
