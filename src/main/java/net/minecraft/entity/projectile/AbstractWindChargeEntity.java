package net.minecraft.entity.projectile;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.explosion.AdvancedExplosionBehavior;
import net.minecraft.world.explosion.ExplosionBehavior;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

/**
 * Базовый класс для всех снарядов типа «заряд ветра».
 * <p>
 * Реализует общую логику: столкновение с блоком (с небольшим смещением взрыва),
 * столкновение с сущностью (урон + вызов взрыва), отсутствие горения,
 * нулевое ускорение и автоматическое уничтожение при выходе за верхнюю границу мира.
 */
public abstract class AbstractWindChargeEntity extends ExplosiveProjectileEntity implements FlyingItemEntity {

	/**
	 * Поведение взрыва по умолчанию: разрушает блоки, не помеченные тегом
	 * {@link BlockTags#BLOCKS_WIND_CHARGE_EXPLOSIONS}, без урона от давления.
	 */
	public static final ExplosionBehavior EXPLOSION_BEHAVIOR = new AdvancedExplosionBehavior(
			true,
			false,
			Optional.empty(),
			Registries.BLOCK.getOptional(BlockTags.BLOCKS_WIND_CHARGE_EXPLOSIONS).map(Function.identity())
	);

	/** Смещение точки взрыва от поверхности блока при попадании. */
	public static final double BLOCK_HIT_EXPLOSION_OFFSET = 0.25;

	/** Запас высоты над верхней границей мира, после которого снаряд взрывается и исчезает. */
	private static final int ABOVE_WORLD_EXPLODE_MARGIN = 30;

	/** Урон, наносимый сущности при прямом попадании. */
	private static final float ENTITY_HIT_DAMAGE = 1.0F;

	/** Смещение нижней границы хитбокса вниз для корректного визуального позиционирования. */
	private static final float BOUNDING_BOX_Y_OFFSET = 0.15F;

	public AbstractWindChargeEntity(EntityType<? extends AbstractWindChargeEntity> entityType, World world) {
		super(entityType, world);
		accelerationPower = 0.0;
	}

	public AbstractWindChargeEntity(
			EntityType<? extends AbstractWindChargeEntity> type,
			World world,
			Entity owner,
			double x,
			double y,
			double z
	) {
		super(type, x, y, z, world);
		setOwner(owner);
		accelerationPower = 0.0;
	}

	AbstractWindChargeEntity(
			EntityType<? extends AbstractWindChargeEntity> entityType,
			double x,
			double y,
			double z,
			Vec3d velocity,
			World world
	) {
		super(entityType, x, y, z, velocity, world);
		accelerationPower = 0.0;
	}

	@Override
	protected Box calculateDefaultBoundingBox(Vec3d pos) {
		float halfWidth = getType().getDimensions().width() / 2.0F;
		float height = getType().getDimensions().height();
		return new Box(
				pos.x - halfWidth,
				pos.y - BOUNDING_BOX_Y_OFFSET,
				pos.z - halfWidth,
				pos.x + halfWidth,
				pos.y - BOUNDING_BOX_Y_OFFSET + height,
				pos.z + halfWidth
		);
	}

	@Override
	public boolean collidesWith(Entity other) {
		return other instanceof AbstractWindChargeEntity
				? false
				: super.collidesWith(other);
	}

	@Override
	protected boolean canHit(Entity entity) {
		if (entity instanceof AbstractWindChargeEntity) {
			return false;
		}

		return entity.getType() == EntityType.END_CRYSTAL
				? false
				: super.canHit(entity);
	}

	@Override
	protected void onEntityHit(EntityHitResult entityHitResult) {
		super.onEntityHit(entityHitResult);
		if (!(getEntityWorld() instanceof ServerWorld serverWorld)) {
			return;
		}

		LivingEntity livingOwner = getOwner() instanceof LivingEntity living ? living : null;
		Entity target = entityHitResult.getEntity();

		if (livingOwner != null) {
			livingOwner.onAttacking(target);
		}

		DamageSource damageSource = getDamageSources().windCharge(this, livingOwner);

		if (target.damage(serverWorld, damageSource, ENTITY_HIT_DAMAGE)
				&& target instanceof LivingEntity livingTarget) {
			EnchantmentHelper.onTargetDamaged(serverWorld, livingTarget, damageSource);
		}

		createExplosion(getEntityPos());
	}

	@Override
	public void addVelocity(double deltaX, double deltaY, double deltaZ) {
		// Заряды ветра не получают внешних импульсов — намеренно пусто.
	}

	/**
	 * Создаёт взрыв ветра в указанной точке мира.
	 * Реализация определяет мощность, частицы и звук конкретного типа заряда.
	 *
	 * @param pos точка взрыва в мировых координатах
	 */
	protected abstract void createExplosion(Vec3d pos);

	@Override
	protected void onBlockHit(BlockHitResult blockHitResult) {
		super.onBlockHit(blockHitResult);
		if (getEntityWorld().isClient()) {
			return;
		}

		Vec3i sideVector = blockHitResult.getSide().getVector();
		Vec3d offset = Vec3d.of(sideVector)
				.multiply(BLOCK_HIT_EXPLOSION_OFFSET, BLOCK_HIT_EXPLOSION_OFFSET, BLOCK_HIT_EXPLOSION_OFFSET);
		Vec3d explosionPos = blockHitResult.getPos().add(offset);

		createExplosion(explosionPos);
		discard();
	}

	@Override
	protected void onCollision(HitResult hitResult) {
		super.onCollision(hitResult);
		if (!getEntityWorld().isClient()) {
			discard();
		}
	}

	@Override
	protected boolean isBurning() {
		return false;
	}

	@Override
	public ItemStack getStack() {
		return ItemStack.EMPTY;
	}

	@Override
	protected float getDrag() {
		return 1.0F;
	}

	@Override
	protected float getDragInWater() {
		return getDrag();
	}

	@Override
	protected @Nullable ParticleEffect getParticleType() {
		return null;
	}

	@Override
	public void tick() {
		if (!getEntityWorld().isClient()
				&& getBlockY() > getEntityWorld().getTopYInclusive() + ABOVE_WORLD_EXPLODE_MARGIN) {
			createExplosion(getEntityPos());
			discard();
			return;
		}

		super.tick();
	}
}
