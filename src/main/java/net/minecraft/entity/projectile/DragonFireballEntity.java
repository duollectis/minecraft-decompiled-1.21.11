package net.minecraft.entity.projectile;

import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.DragonBreathParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * Огненный шар дракона Края, создающий облако дыхания дракона при столкновении.
 * <p>
 * При попадании в блок или сущность (кроме владельца) спавнит {@link AreaEffectCloudEntity}
 * с эффектом мгновенного урона. Облако позиционируется у ближайшей живой сущности
 * в радиусе {@value #DAMAGE_RANGE} блоков для максимального охвата.
 */
public class DragonFireballEntity extends ExplosiveProjectileEntity {

	public static final float DAMAGE_RANGE = 4.0F;
	private static final double DAMAGE_RANGE_SQUARED = 16.0;
	private static final float CLOUD_RADIUS = 3.0F;
	private static final float CLOUD_RADIUS_ON_USE = -0.5F;
	private static final float CLOUD_POTION_DURATION_SCALE = 0.25F;
	private static final int INSTANT_DAMAGE_AMPLIFIER = 1;
	private static final int CLOUD_WAIT_TIME = 0;

	public DragonFireballEntity(EntityType<? extends DragonFireballEntity> entityType, World world) {
		super(entityType, world);
	}

	public DragonFireballEntity(World world, LivingEntity owner, Vec3d velocity) {
		super(EntityType.DRAGON_FIREBALL, owner, velocity, world);
	}

	@Override
	protected void onCollision(HitResult hitResult) {
		super.onCollision(hitResult);
		if (hitResult.getType() == HitResult.Type.ENTITY
			&& isOwner(((EntityHitResult) hitResult).getEntity())
		) {
			return;
		}

		if (getEntityWorld().isClient()) {
			return;
		}

		List<LivingEntity> nearbyEntities = getEntityWorld().getNonSpectatingEntities(
			LivingEntity.class,
			getBoundingBox().expand(DAMAGE_RANGE, 2.0, DAMAGE_RANGE)
		);
		AreaEffectCloudEntity cloud = new AreaEffectCloudEntity(
			getEntityWorld(),
			getX(),
			getY(),
			getZ()
		);
		Entity ownerEntity = getOwner();
		if (ownerEntity instanceof LivingEntity livingOwner) {
			cloud.setOwner(livingOwner);
		}

		cloud.setParticleType(DragonBreathParticleEffect.of(ParticleTypes.DRAGON_BREATH, 1.0F));
		cloud.setRadius(CLOUD_RADIUS);
		cloud.setDuration(AreaEffectCloudEntity.DEFAULT_LINGERING_DURATION);
		cloud.setRadiusGrowth((7.0F - cloud.getRadius()) / cloud.getDuration());
		cloud.setPotionDurationScale(CLOUD_POTION_DURATION_SCALE);
		cloud.addEffect(new StatusEffectInstance(StatusEffects.INSTANT_DAMAGE, 1, INSTANT_DAMAGE_AMPLIFIER));

		// Позиционируем облако у ближайшей сущности для максимального урона
		for (LivingEntity nearby : nearbyEntities) {
			if (squaredDistanceTo(nearby) < DAMAGE_RANGE_SQUARED) {
				cloud.setPosition(nearby.getX(), nearby.getY(), nearby.getZ());
				break;
			}
		}

		getEntityWorld().syncWorldEvent(2006, getBlockPos(), isSilent() ? -1 : 1);
		getEntityWorld().spawnEntity(cloud);
		discard();
	}

	@Override
	protected ParticleEffect getParticleType() {
		return DragonBreathParticleEffect.of(ParticleTypes.DRAGON_BREATH, 1.0F);
	}

	@Override
	protected boolean isBurning() {
		return false;
	}
}
