package net.minecraft.entity.projectile;

import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.fluid.FluidState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;

/**
 * Череп иссушителя — снаряд, выпускаемый боссом Иссушителем.
 * <p>
 * Заряженный череп (синий) имеет повышенное сопротивление воздуха и может разрушать
 * блоки с сопротивлением взрыву до 0.8. При попадании в живую сущность накладывает
 * эффект иссушения на длительность, зависящую от сложности мира.
 * Владелец-иссушитель восстанавливает здоровье при убийстве цели.
 */
public class WitherSkullEntity extends ExplosiveProjectileEntity {

	private static final float CHARGED_DRAG = 0.73F;
	private static final float EXPLOSION_POWER = 1.0F;
	private static final float OWNER_DAMAGE = 8.0F;
	private static final float MAGIC_DAMAGE = 5.0F;
	private static final float OWNER_HEAL_ON_KILL = 5.0F;
	private static final float MAX_EXPLOSION_RESISTANCE = 0.8F;
	private static final int WITHER_DURATION_NORMAL = 10;
	private static final int WITHER_DURATION_HARD = 40;
	private static final int WITHER_AMPLIFIER = 1;

	private static final TrackedData<Boolean> CHARGED =
		DataTracker.registerData(WitherSkullEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

	public WitherSkullEntity(EntityType<? extends WitherSkullEntity> entityType, World world) {
		super(entityType, world);
	}

	public WitherSkullEntity(World world, LivingEntity owner, Vec3d velocity) {
		super(EntityType.WITHER_SKULL, owner, velocity, world);
	}

	@Override
	protected float getDrag() {
		return isCharged() ? CHARGED_DRAG : super.getDrag();
	}

	@Override
	public boolean isOnFire() {
		return false;
	}

	/**
	 * Заряженный череп снижает сопротивление взрыву блоков до {@value #MAX_EXPLOSION_RESISTANCE},
	 * позволяя разрушать блоки, которые обычно выдерживают взрывы (например, обсидиан).
	 */
	@Override
	public float getEffectiveExplosionResistance(
		Explosion explosion,
		BlockView world,
		BlockPos pos,
		BlockState blockState,
		FluidState fluidState,
		float max
	) {
		return isCharged() && WitherEntity.canDestroy(blockState) ? Math.min(MAX_EXPLOSION_RESISTANCE, max) : max;
	}

	@Override
	protected void onEntityHit(EntityHitResult entityHitResult) {
		super.onEntityHit(entityHitResult);
		if (!(getEntityWorld() instanceof ServerWorld serverWorld)) {
			return;
		}

		Entity target = entityHitResult.getEntity();
		boolean damaged;

		if (getOwner() instanceof LivingEntity livingOwner) {
			DamageSource damageSource = getDamageSources().witherSkull(this, livingOwner);
			damaged = target.damage(serverWorld, damageSource, OWNER_DAMAGE);
			if (damaged) {
				if (target.isAlive()) {
					EnchantmentHelper.onTargetDamaged(serverWorld, target, damageSource);
				} else {
					livingOwner.heal(OWNER_HEAL_ON_KILL);
				}
			}
		} else {
			damaged = target.damage(serverWorld, getDamageSources().magic(), MAGIC_DAMAGE);
		}

		if (damaged && target instanceof LivingEntity livingTarget) {
			int witherDurationSeconds = switch (getEntityWorld().getDifficulty()) {
				case NORMAL -> WITHER_DURATION_NORMAL;
				case HARD -> WITHER_DURATION_HARD;
				default -> 0;
			};

			if (witherDurationSeconds > 0) {
				livingTarget.addStatusEffect(
					new StatusEffectInstance(StatusEffects.WITHER, 20 * witherDurationSeconds, WITHER_AMPLIFIER),
					getEffectCause()
				);
			}
		}
	}

	@Override
	protected void onCollision(HitResult hitResult) {
		super.onCollision(hitResult);
		if (!getEntityWorld().isClient()) {
			getEntityWorld().createExplosion(
				this,
				getX(),
				getY(),
				getZ(),
				EXPLOSION_POWER,
				false,
				World.ExplosionSourceType.MOB
			);
			discard();
		}
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(CHARGED, false);
	}

	public boolean isCharged() {
		return dataTracker.get(CHARGED);
	}

	public void setCharged(boolean charged) {
		dataTracker.set(CHARGED, charged);
	}

	@Override
	protected boolean isBurning() {
		return false;
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putBoolean("dangerous", isCharged());
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		setCharged(view.getBoolean("dangerous", false));
	}
}
