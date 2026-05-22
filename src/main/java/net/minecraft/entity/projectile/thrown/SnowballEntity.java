package net.minecraft.entity.projectile.thrown;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

/**
 * Снежок — метательный снаряд, наносящий урон блейзам и разбивающийся с частицами при попадании.
 */
public class SnowballEntity extends ThrownItemEntity {

	private static final byte STATUS_BREAK = 3;
	private static final int BLAZE_DAMAGE = 3;
	private static final int PARTICLE_COUNT = 8;

	public SnowballEntity(EntityType<? extends SnowballEntity> entityType, World world) {
		super(entityType, world);
	}

	public SnowballEntity(World world, LivingEntity owner, ItemStack stack) {
		super(EntityType.SNOWBALL, owner, world, stack);
	}

	public SnowballEntity(World world, double x, double y, double z, ItemStack stack) {
		super(EntityType.SNOWBALL, x, y, z, world, stack);
	}

	@Override
	protected Item getDefaultItem() {
		return Items.SNOWBALL;
	}

	private ParticleEffect getParticleParameters() {
		ItemStack stack = getStack();
		return stack.isEmpty()
				? ParticleTypes.ITEM_SNOWBALL
				: new ItemStackParticleEffect(ParticleTypes.ITEM, stack);
	}

	@Override
	public void handleStatus(byte status) {
		if (status != STATUS_BREAK) {
			return;
		}

		ParticleEffect particleEffect = getParticleParameters();

		for (int i = 0; i < PARTICLE_COUNT; i++) {
			getEntityWorld().addParticleClient(particleEffect, getX(), getY(), getZ(), 0.0, 0.0, 0.0);
		}
	}

	@Override
	protected void onEntityHit(EntityHitResult entityHitResult) {
		super.onEntityHit(entityHitResult);
		Entity entity = entityHitResult.getEntity();
		int damage = entity instanceof BlazeEntity ? BLAZE_DAMAGE : 0;
		entity.serverDamage(getDamageSources().thrown(this, getOwner()), damage);
	}

	@Override
	protected void onCollision(HitResult hitResult) {
		super.onCollision(hitResult);

		if (getEntityWorld().isClient()) {
			return;
		}

		getEntityWorld().sendEntityStatus(this, STATUS_BREAK);
		discard();
	}
}
