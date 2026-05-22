package net.minecraft.entity.projectile;

import net.minecraft.block.AbstractBlock;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Плевок ламы — слабый снаряд, наносящий 1 единицу урона.
 * <p>
 * Движется с постоянным замедлением {@link #DRAG} и гравитацией.
 * Уничтожается при попадании в блок, воду или при нахождении внутри непрозрачного блока.
 */
public class LlamaSpitEntity extends ProjectileEntity {

	private static final float DRAG = 0.99F;
	private static final float ENTITY_HIT_DAMAGE = 1.0F;
	private static final int SPAWN_PARTICLE_COUNT = 7;
	private static final double SPAWN_PARTICLE_VELOCITY_BASE = 0.4;
	private static final double SPAWN_PARTICLE_VELOCITY_STEP = 0.1;

	public LlamaSpitEntity(EntityType<? extends LlamaSpitEntity> entityType, World world) {
		super(entityType, world);
	}

	public LlamaSpitEntity(World world, LlamaEntity owner) {
		this(EntityType.LLAMA_SPIT, world);
		setOwner(owner);
		setPosition(
				owner.getX() - (owner.getWidth() + 1.0F) * 0.5 * MathHelper.sin(
						owner.bodyYaw * (float) (Math.PI / 180.0)),
				owner.getEyeY() - 0.1F,
				owner.getZ() + (owner.getWidth() + 1.0F) * 0.5 * MathHelper.cos(
						owner.bodyYaw * (float) (Math.PI / 180.0))
		);
	}

	@Override
	protected double getGravity() {
		return 0.06;
	}

	@Override
	public void tick() {
		super.tick();
		Vec3d velocity = getVelocity();
		HitResult hitResult = ProjectileUtil.getCollision(this, this::canHit);
		hitOrDeflect(hitResult);

		double nextX = getX() + velocity.x;
		double nextY = getY() + velocity.y;
		double nextZ = getZ() + velocity.z;

		updateRotation();

		boolean insideBlock = getEntityWorld()
				.getStatesInBox(getBoundingBox())
				.noneMatch(AbstractBlock.AbstractBlockState::isAir);

		if (insideBlock || isTouchingWater()) {
			discard();
			return;
		}

		setVelocity(velocity.multiply(DRAG));
		applyGravity();
		setPosition(nextX, nextY, nextZ);
	}

	@Override
	protected void onEntityHit(EntityHitResult entityHitResult) {
		super.onEntityHit(entityHitResult);
		if (!(getOwner() instanceof LivingEntity livingOwner)) {
			return;
		}

		Entity target = entityHitResult.getEntity();
		DamageSource damageSource = getDamageSources().spit(this, livingOwner);

		if (getEntityWorld() instanceof ServerWorld serverWorld
				&& target.damage(serverWorld, damageSource, ENTITY_HIT_DAMAGE)) {
			EnchantmentHelper.onTargetDamaged(serverWorld, target, damageSource);
		}
	}

	@Override
	protected void onBlockHit(BlockHitResult blockHitResult) {
		super.onBlockHit(blockHitResult);
		if (!getEntityWorld().isClient()) {
			discard();
		}
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
	}

	@Override
	public void onSpawnPacket(EntitySpawnS2CPacket packet) {
		super.onSpawnPacket(packet);
		Vec3d velocity = packet.getVelocity();

		for (int index = 0; index < SPAWN_PARTICLE_COUNT; index++) {
			double velocityScale = SPAWN_PARTICLE_VELOCITY_BASE + SPAWN_PARTICLE_VELOCITY_STEP * index;
			getEntityWorld()
					.addParticleClient(
							ParticleTypes.SPIT,
							getX(),
							getY(),
							getZ(),
							velocity.x * velocityScale,
							velocity.y,
							velocity.z * velocityScale
					);
		}

		setVelocity(velocity);
	}
}
