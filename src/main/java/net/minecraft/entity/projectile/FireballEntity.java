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
 * {@code FireballEntity}.
 */
public class FireballEntity extends AbstractFireballEntity {

	private static final byte DEFAULT_EXPLOSION_POWER = 1;
	private int explosionPower = 1;

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
		if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
			boolean bl = serverWorld.getGameRules().getValue(GameRules.DO_MOB_GRIEFING);
			this
					.getEntityWorld()
					.createExplosion(
							this,
							this.getX(),
							this.getY(),
							this.getZ(),
							this.explosionPower,
							bl,
							World.ExplosionSourceType.MOB
					);
			this.discard();
		}
	}

	@Override
	protected void onEntityHit(EntityHitResult entityHitResult) {
		super.onEntityHit(entityHitResult);
		if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
			Entity var6 = entityHitResult.getEntity();
			Entity entity2 = this.getOwner();
			DamageSource damageSource = this.getDamageSources().fireball(this, entity2);
			var6.damage(serverWorld, damageSource, 6.0F);
			EnchantmentHelper.onTargetDamaged(serverWorld, var6, damageSource);
		}
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putByte("ExplosionPower", (byte) this.explosionPower);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		this.explosionPower = view.getByte("ExplosionPower", (byte) 1);
	}
}
