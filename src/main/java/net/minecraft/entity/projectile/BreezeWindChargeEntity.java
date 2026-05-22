package net.minecraft.entity.projectile;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.BreezeEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Заряд ветра, выпускаемый мобом «Бриз» (Breeze).
 * <p>
 * Использует унаследованное {@link AbstractWindChargeEntity#EXPLOSION_BEHAVIOR} и
 * имеет значительно большую мощность взрыва ({@link #EXPLOSION_POWER}), чем
 * {@link WindChargeEntity} игрока. Не может быть отражён.
 */
public class BreezeWindChargeEntity extends AbstractWindChargeEntity {

	private static final float EXPLOSION_POWER = 3.0F;

	public BreezeWindChargeEntity(EntityType<? extends AbstractWindChargeEntity> entityType, World world) {
		super(entityType, world);
	}

	public BreezeWindChargeEntity(BreezeEntity breeze, World world) {
		super(EntityType.BREEZE_WIND_CHARGE, world, breeze, breeze.getX(), breeze.getChargeY(), breeze.getZ());
	}

	@Override
	protected void createExplosion(Vec3d pos) {
		getEntityWorld()
				.createExplosion(
						this,
						null,
						EXPLOSION_BEHAVIOR,
						pos.getX(),
						pos.getY(),
						pos.getZ(),
						EXPLOSION_POWER,
						false,
						World.ExplosionSourceType.TRIGGER,
						ParticleTypes.GUST_EMITTER_SMALL,
						ParticleTypes.GUST_EMITTER_LARGE,
						Pool.empty(),
						SoundEvents.ENTITY_BREEZE_WIND_BURST
				);
	}
}
