package net.minecraft.entity.projectile;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LazyEntityReference;
import net.minecraft.entity.ProjectileDeflection;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.explosion.AdvancedExplosionBehavior;
import net.minecraft.world.explosion.ExplosionBehavior;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

/**
 * Заряд ветра, выпускаемый игроком.
 * <p>
 * В отличие от {@link BreezeWindChargeEntity}, имеет меньшую мощность взрыва
 * и может быть отражён другими снарядами или атаками, но только по истечении
 * начального кулдауна ({@link #DEFLECT_COOLDOWN_TICKS} тиков).
 * Также не отображается в первые 2 тика при близком расстоянии до камеры.
 */
public class WindChargeEntity extends AbstractWindChargeEntity {

	private static final ExplosionBehavior EXPLOSION_BEHAVIOR = new AdvancedExplosionBehavior(
			true,
			false,
			Optional.of(1.22F),
			Registries.BLOCK.getOptional(BlockTags.BLOCKS_WIND_CHARGE_EXPLOSIONS).map(Function.identity())
	);

	private static final float EXPLOSION_POWER = 1.2F;
	private static final float MAX_RENDER_DISTANCE_SQUARED_NEW = MathHelper.square(3.5F);

	/** Количество тиков после спавна, в течение которых снаряд нельзя отразить. */
	private static final int DEFLECT_COOLDOWN_TICKS = 5;

	private int deflectCooldown = DEFLECT_COOLDOWN_TICKS;

	public WindChargeEntity(EntityType<? extends AbstractWindChargeEntity> entityType, World world) {
		super(entityType, world);
	}

	public WindChargeEntity(PlayerEntity player, World world, double x, double y, double z) {
		super(EntityType.WIND_CHARGE, world, player, x, y, z);
	}

	public WindChargeEntity(World world, double x, double y, double z, Vec3d velocity) {
		super(EntityType.WIND_CHARGE, x, y, z, velocity, world);
	}

	@Override
	public void tick() {
		super.tick();
		if (deflectCooldown > 0) {
			deflectCooldown--;
		}
	}

	@Override
	public boolean deflect(
			ProjectileDeflection deflection,
			@Nullable Entity deflector,
			@Nullable LazyEntityReference<Entity> lazyEntityReference,
			boolean fromAttack
	) {
		return deflectCooldown > 0
				? false
				: super.deflect(deflection, deflector, lazyEntityReference, fromAttack);
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
						SoundEvents.ENTITY_WIND_CHARGE_WIND_BURST
				);
	}

	@Override
	public boolean shouldRender(double distance) {
		return age < 2 && distance < MAX_RENDER_DISTANCE_SQUARED_NEW
				? false
				: super.shouldRender(distance);
	}
}
