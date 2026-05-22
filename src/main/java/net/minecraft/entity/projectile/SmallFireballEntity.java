package net.minecraft.entity.projectile;

import net.minecraft.block.AbstractFireBlock;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.rule.GameRules;

/**
 * Маленький огненный шар, выпускаемый блейзами.
 * <p>
 * При попадании в сущность поджигает её и наносит 5 единиц урона.
 * При попадании в блок — поджигает соседний воздушный блок (если разрешено правилами).
 * После любого столкновения удаляется.
 */
public class SmallFireballEntity extends AbstractFireballEntity {

	private static final float ENTITY_HIT_DAMAGE = 5.0F;
	private static final float FIRE_DURATION = 5.0F;

	public SmallFireballEntity(EntityType<? extends SmallFireballEntity> entityType, World world) {
		super(entityType, world);
	}

	public SmallFireballEntity(World world, LivingEntity owner, Vec3d velocity) {
		super(EntityType.SMALL_FIREBALL, owner, velocity, world);
	}

	public SmallFireballEntity(World world, double x, double y, double z, Vec3d velocity) {
		super(EntityType.SMALL_FIREBALL, x, y, z, velocity, world);
	}

	@Override
	protected void onEntityHit(EntityHitResult entityHitResult) {
		super.onEntityHit(entityHitResult);
		if (!(getEntityWorld() instanceof ServerWorld serverWorld)) {
			return;
		}

		Entity target = entityHitResult.getEntity();
		Entity ownerEntity = getOwner();
		int prevFireTicks = target.getFireTicks();
		target.setOnFireFor(FIRE_DURATION);
		DamageSource damageSource = getDamageSources().fireball(this, ownerEntity);
		if (!target.damage(serverWorld, damageSource, ENTITY_HIT_DAMAGE)) {
			target.setFireTicks(prevFireTicks);
		} else {
			EnchantmentHelper.onTargetDamaged(serverWorld, target, damageSource);
		}
	}

	@Override
	protected void onBlockHit(BlockHitResult blockHitResult) {
		super.onBlockHit(blockHitResult);
		if (!(getEntityWorld() instanceof ServerWorld serverWorld)) {
			return;
		}

		Entity ownerEntity = getOwner();
		boolean canGrief = !(ownerEntity instanceof MobEntity)
			|| serverWorld.getGameRules().getValue(GameRules.DO_MOB_GRIEFING);
		if (!canGrief) {
			return;
		}

		BlockPos firePos = blockHitResult.getBlockPos().offset(blockHitResult.getSide());
		if (getEntityWorld().isAir(firePos)) {
			getEntityWorld().setBlockState(firePos, AbstractFireBlock.getState(getEntityWorld(), firePos));
		}
	}

	@Override
	protected void onCollision(HitResult hitResult) {
		super.onCollision(hitResult);
		if (!getEntityWorld().isClient()) {
			discard();
		}
	}
}
