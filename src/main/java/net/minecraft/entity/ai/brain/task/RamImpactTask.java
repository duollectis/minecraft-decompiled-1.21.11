package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.GoatEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.intprovider.UniformIntProvider;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

/**
 * Задача мозга козла, реализующая таранный удар по цели.
 * При столкновении наносит урон, применяет нокбэк с учётом скорости и эффектов,
 * а также может сломать рог при ударе о блок из тега {@code SNAPS_GOAT_HORN}.
 */
public class RamImpactTask extends MultiTickTask<GoatEntity> {

	public static final int RUN_TIME = 200;
	public static final float SPEED_STRENGTH_MULTIPLIER = 1.65F;

	private static final byte STATUS_FINISH_RAM = 59;
	private static final float MIN_KNOCKBACK_SPEED = 0.2F;
	private static final float MAX_KNOCKBACK_SPEED = 3.0F;
	private static final float SPEED_EFFECT_MULTIPLIER = 0.25F;
	private static final float KNOCKBACK_BLOCKED_FACTOR = 0.5F;
	private static final float KNOCKBACK_UNBLOCKED_FACTOR = 1.0F;
	private static final double RAM_REACHED_THRESHOLD = 0.25;

	private final Function<GoatEntity, UniformIntProvider> cooldownRangeFactory;
	private final TargetPredicate targetPredicate;
	private final float speed;
	private final ToDoubleFunction<GoatEntity> strengthMultiplierFactory;
	private final Function<GoatEntity, SoundEvent> impactSoundFactory;
	private final Function<GoatEntity, SoundEvent> hornBreakSoundFactory;
	private Vec3d direction;

	public RamImpactTask(
			Function<GoatEntity, UniformIntProvider> cooldownRangeFactory,
			TargetPredicate targetPredicate,
			float speed,
			ToDoubleFunction<GoatEntity> strengthMultiplierFactory,
			Function<GoatEntity, SoundEvent> impactSoundFactory,
			Function<GoatEntity, SoundEvent> hornBreakSoundFactory
	) {
		super(
				ImmutableMap.of(
						MemoryModuleType.RAM_COOLDOWN_TICKS,
						MemoryModuleState.VALUE_ABSENT,
						MemoryModuleType.RAM_TARGET,
						MemoryModuleState.VALUE_PRESENT
				),
				RUN_TIME
		);
		this.cooldownRangeFactory = cooldownRangeFactory;
		this.targetPredicate = targetPredicate;
		this.speed = speed;
		this.strengthMultiplierFactory = strengthMultiplierFactory;
		this.impactSoundFactory = impactSoundFactory;
		this.hornBreakSoundFactory = hornBreakSoundFactory;
		direction = Vec3d.ZERO;
	}

	@Override
	protected boolean shouldRun(ServerWorld world, GoatEntity entity) {
		return entity.getBrain().hasMemoryModule(MemoryModuleType.RAM_TARGET);
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, GoatEntity entity, long time) {
		return entity.getBrain().hasMemoryModule(MemoryModuleType.RAM_TARGET);
	}

	@Override
	protected void run(ServerWorld world, GoatEntity entity, long time) {
		BlockPos entityPos = entity.getBlockPos();
		Brain<?> brain = entity.getBrain();
		Vec3d ramTarget = brain.getOptionalRegisteredMemory(MemoryModuleType.RAM_TARGET).get();

		direction = new Vec3d(entityPos.getX() - ramTarget.getX(), 0.0, entityPos.getZ() - ramTarget.getZ()).normalize();
		brain.remember(MemoryModuleType.WALK_TARGET, new WalkTarget(ramTarget, speed, 0));
	}

	@Override
	protected void keepRunning(ServerWorld world, GoatEntity entity, long time) {
		List<LivingEntity> targets = world.getTargets(
				LivingEntity.class,
				targetPredicate,
				entity,
				entity.getBoundingBox()
		);
		Brain<?> brain = entity.getBrain();

		if (!targets.isEmpty()) {
			LivingEntity target = targets.get(0);
			DamageSource damageSource = world.getDamageSources().mobAttackNoAggro(entity);
			float attackDamage = (float) entity.getAttributeValue(EntityAttributes.ATTACK_DAMAGE);

			if (target.damage(world, damageSource, attackDamage)) {
				EnchantmentHelper.onTargetDamaged(world, target, damageSource);
			}

			int speedAmplifier = entity.hasStatusEffect(StatusEffects.SPEED)
					? entity.getStatusEffect(StatusEffects.SPEED).getAmplifier() + 1
					: 0;
			int slownessAmplifier = entity.hasStatusEffect(StatusEffects.SLOWNESS)
					? entity.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() + 1
					: 0;
			float effectBonus = SPEED_EFFECT_MULTIPLIER * (speedAmplifier - slownessAmplifier);
			float knockbackSpeed = MathHelper.clamp(
					entity.getMovementSpeed() * SPEED_STRENGTH_MULTIPLIER,
					MIN_KNOCKBACK_SPEED,
					MAX_KNOCKBACK_SPEED
			) + effectBonus;

			DamageSource blockCheckSource = world.getDamageSources().mobAttack(entity);
			float blockedAmount = target.getDamageBlockedAmount(world, blockCheckSource, attackDamage);
			float knockbackFactor = blockedAmount > 0.0F ? KNOCKBACK_BLOCKED_FACTOR : KNOCKBACK_UNBLOCKED_FACTOR;

			target.takeKnockback(
					knockbackFactor * knockbackSpeed * strengthMultiplierFactory.applyAsDouble(entity),
					direction.getX(),
					direction.getZ()
			);
			finishRam(world, entity);
			world.playSoundFromEntity(null, entity, impactSoundFactory.apply(entity), SoundCategory.NEUTRAL, 1.0F, 1.0F);
			return;
		}

		if (shouldSnapHorn(world, entity)) {
			world.playSoundFromEntity(null, entity, impactSoundFactory.apply(entity), SoundCategory.NEUTRAL, 1.0F, 1.0F);

			boolean hornDropped = entity.dropHorn();

			if (hornDropped) {
				world.playSoundFromEntity(
						null,
						entity,
						hornBreakSoundFactory.apply(entity),
						SoundCategory.NEUTRAL,
						1.0F,
						1.0F
				);
			}

			finishRam(world, entity);
			return;
		}

		Optional<WalkTarget> walkTarget = brain.getOptionalRegisteredMemory(MemoryModuleType.WALK_TARGET);
		Optional<Vec3d> ramTarget = brain.getOptionalRegisteredMemory(MemoryModuleType.RAM_TARGET);
		boolean reachedTarget = walkTarget.isEmpty()
				|| ramTarget.isEmpty()
				|| walkTarget.get().getLookTarget().getPos().isInRange(ramTarget.get(), RAM_REACHED_THRESHOLD);

		if (reachedTarget) {
			finishRam(world, entity);
		}
	}

	private boolean shouldSnapHorn(ServerWorld world, GoatEntity goat) {
		Vec3d moveDir = goat.getVelocity().multiply(1.0, 0.0, 1.0).normalize();
		BlockPos frontPos = BlockPos.ofFloored(goat.getEntityPos().add(moveDir));

		return world.getBlockState(frontPos).isIn(BlockTags.SNAPS_GOAT_HORN)
				|| world.getBlockState(frontPos.up()).isIn(BlockTags.SNAPS_GOAT_HORN);
	}

	protected void finishRam(ServerWorld world, GoatEntity goat) {
		world.sendEntityStatus(goat, STATUS_FINISH_RAM);
		goat.getBrain().remember(
				MemoryModuleType.RAM_COOLDOWN_TICKS,
				cooldownRangeFactory.apply(goat).get(world.random)
		);
		goat.getBrain().forget(MemoryModuleType.RAM_TARGET);
	}
}
