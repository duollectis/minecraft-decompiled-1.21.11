package net.minecraft.entity.effect;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.joml.Vector3f;

import java.util.function.ToIntFunction;

/**
 * Эффект заражения (Infested).
 *
 * <p>При получении урона с заданной вероятностью спавнит чешуйниц рядом с сущностью.
 * Количество чешуйниц определяется функцией {@code silverfishCountFunction}.</p>
 */
class InfestedStatusEffect extends StatusEffect {

	/** Угол разброса скорости чешуйниц при спавне (±90°). */
	private static final float SPAWN_ANGLE_RANGE = (float) (Math.PI / 2);
	/** Горизонтальный множитель начальной скорости чешуйницы. */
	private static final float VELOCITY_HORIZONTAL_SCALE = 0.3F;
	/** Вертикальный множитель начальной скорости чешуйницы. */
	private static final float VELOCITY_VERTICAL_SCALE = 1.5F;

	private final float silverfishChance;
	private final ToIntFunction<Random> silverfishCountFunction;

	protected InfestedStatusEffect(
			StatusEffectCategory category,
			int color,
			float silverfishChance,
			ToIntFunction<Random> silverfishCountFunction
	) {
		super(category, color, ParticleTypes.INFESTED);
		this.silverfishChance = silverfishChance;
		this.silverfishCountFunction = silverfishCountFunction;
	}

	/**
	 * При получении урона с вероятностью {@code silverfishChance} спавнит чешуйниц
	 * в центре тела сущности.
	 */
	@Override
	public void onEntityDamage(
			ServerWorld world,
			LivingEntity entity,
			int amplifier,
			DamageSource source,
			float amount
	) {
		if (entity.getRandom().nextFloat() > silverfishChance) {
			return;
		}

		int count = silverfishCountFunction.applyAsInt(entity.getRandom());
		double spawnX = entity.getX();
		double spawnY = entity.getY() + entity.getHeight() / 2.0;
		double spawnZ = entity.getZ();

		for (int index = 0; index < count; index++) {
			spawnSilverfish(world, entity, spawnX, spawnY, spawnZ);
		}
	}

	/**
	 * Создаёт чешуйницу в указанной позиции и задаёт ей случайную скорость
	 * в направлении взгляда сущности с угловым разбросом ±90°.
	 */
	private void spawnSilverfish(ServerWorld world, LivingEntity entity, double x, double y, double z) {
		SilverfishEntity silverfish = EntityType.SILVERFISH.create(world, SpawnReason.TRIGGERED);
		if (silverfish == null) {
			return;
		}

		Random random = entity.getRandom();
		float angle = MathHelper.nextBetween(random, -SPAWN_ANGLE_RANGE, SPAWN_ANGLE_RANGE);
		Vector3f velocity = entity.getRotationVector()
				.toVector3f()
				.mul(VELOCITY_HORIZONTAL_SCALE)
				.mul(1.0F, VELOCITY_VERTICAL_SCALE, 1.0F)
				.rotateY(angle);

		silverfish.refreshPositionAndAngles(x, y, z, world.getRandom().nextFloat() * 360.0F, 0.0F);
		silverfish.setVelocity(new Vec3d(velocity));
		world.spawnEntity(silverfish);
		silverfish.playSoundIfNotSilent(SoundEvents.ENTITY_SILVERFISH_HURT);
	}
}
