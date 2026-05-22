package net.minecraft.entity.projectile;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.rule.GameRules;

/**
 * Огненный шар, выпускаемый гастами и диспенсерами.
 * <p>
 * При столкновении создаёт взрыв с мощностью {@code explosionPower}.
 * Разрушение блоков зависит от правила игры {@link GameRules#DO_MOB_GRIEFING}.
 * При попадании в сущность наносит 6 единиц урона до взрыва.
 */
public class FireballEntity extends AbstractFireballEntity {

	private static final float ENTITY_HIT_DAMAGE = 6.0F;
	private static final byte DEFAULT_EXPLOSION_POWER = 1;

	private int explosionPower = DEFAULT_EXPLOSION_POWER;

	public FireballEntity(EntityType<? extends FireballEntity> entityType, World world) {
		super(entityType, world);
	}

	public FireballEntity(World world, LivingEntity owner, Vec3d velocity, int explosionPower) {
		super(EntityType.FIREBALL, owner, velocity, world);
		this.explosionPower = explosionPower;
	}

	@Override
	protected void onCollision(HitResult hitResult) {
		super.onCollision(hitResult);
		if (!(getEntityWorld() instanceof ServerWorld serverWorld)) {
			return;
		}

		boolean canGrief = serverWorld.getGameRules().getValue(GameRules.DO_MOB_GRIEFING);
		getEntityWorld().createExplosion(
			this,
			getX(),
			getY(),
			getZ(),
			explosionPower,
			canGrief,
			World.ExplosionSourceType.MOB
		);
		discard();
	}

	@Override
	protected void onEntityHit(EntityHitResult entityHitResult) {
		super.onEntityHit(entityHitResult);
		if (!(getEntityWorld() instanceof ServerWorld serverWorld)) {
			return;
		}

		Entity target = entityHitResult.getEntity();
		Entity ownerEntity = getOwner();
		DamageSource damageSource = getDamageSources().fireball(this, ownerEntity);
		target.damage(serverWorld, damageSource, ENTITY_HIT_DAMAGE);
		EnchantmentHelper.onTargetDamaged(serverWorld, target, damageSource);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putByte("ExplosionPower", (byte) explosionPower);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		explosionPower = view.getByte("ExplosionPower", DEFAULT_EXPLOSION_POWER);
	}
}
