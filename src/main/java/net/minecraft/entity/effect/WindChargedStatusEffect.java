package net.minecraft.entity.effect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.AbstractWindChargeEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.collection.Pool;
import net.minecraft.world.World;

/**
 * Эффект заряда ветра (Wind Charged).
 *
 * <p>При гибели сущности создаёт взрыв ветра в центре её тела.
 * Радиус взрыва случаен: от 3.0 до 5.0 блоков.</p>
 */
class WindChargedStatusEffect extends StatusEffect {

	/** Минимальный радиус взрыва ветра (блоки). */
	private static final float MIN_EXPLOSION_RADIUS = 3.0F;
	/** Максимальный случайный добавок к радиусу взрыва (блоки). */
	private static final float EXPLOSION_RADIUS_RANDOM_RANGE = 2.0F;

	protected WindChargedStatusEffect(StatusEffectCategory category, int color) {
		super(category, color, ParticleTypes.SMALL_GUST);
	}

	/**
	 * При гибели создаёт взрыв ветра в центре тела сущности.
	 * Использует поведение взрыва {@link AbstractWindChargeEntity#EXPLOSION_BEHAVIOR}.
	 */
	@Override
	public void onEntityRemoval(ServerWorld world, LivingEntity entity, int amplifier, Entity.RemovalReason reason) {
		if (reason != Entity.RemovalReason.KILLED) {
			return;
		}

		double x = entity.getX();
		double y = entity.getY() + entity.getHeight() / 2.0F;
		double z = entity.getZ();
		float explosionRadius = MIN_EXPLOSION_RADIUS + entity.getRandom().nextFloat() * EXPLOSION_RADIUS_RANDOM_RANGE;

		world.createExplosion(
				entity,
				null,
				AbstractWindChargeEntity.EXPLOSION_BEHAVIOR,
				x,
				y,
				z,
				explosionRadius,
				false,
				World.ExplosionSourceType.TRIGGER,
				ParticleTypes.GUST_EMITTER_SMALL,
				ParticleTypes.GUST_EMITTER_LARGE,
				Pool.empty(),
				SoundEvents.ENTITY_BREEZE_WIND_BURST
		);
	}
}
