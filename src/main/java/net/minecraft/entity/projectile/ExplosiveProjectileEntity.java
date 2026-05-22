package net.minecraft.entity.projectile;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Базовый класс для взрывных снарядов (огненные шары, черепа иссушителя, заряды ветра).
 * <p>
 * Движется с постоянным ускорением {@code accelerationPower} в направлении скорости.
 * Применяет сопротивление воздуха или воды на каждом тике.
 * Подклассы определяют поведение при столкновении через {@link #onCollision}.
 */
public abstract class ExplosiveProjectileEntity extends ProjectileEntity {

	public static final double DEFAULT_ACCELERATION_POWER = 0.1;
	/** Множитель ускорения при отклонении от атаки (не от блока). */
	public static final double DEFLECTION_POWER_FACTOR = 0.5;

	private static final float BUBBLE_PARTICLE_OFFSET = 0.25F;
	private static final int BUBBLE_PARTICLE_COUNT = 4;

	public double accelerationPower = DEFAULT_ACCELERATION_POWER;

	protected ExplosiveProjectileEntity(EntityType<? extends ExplosiveProjectileEntity> entityType, World world) {
		super(entityType, world);
	}

	protected ExplosiveProjectileEntity(
		EntityType<? extends ExplosiveProjectileEntity> type,
		double x,
		double y,
		double z,
		World world
	) {
		this(type, world);
		setPosition(x, y, z);
	}

	public ExplosiveProjectileEntity(
		EntityType<? extends ExplosiveProjectileEntity> type,
		double x,
		double y,
		double z,
		Vec3d velocity,
		World world
	) {
		this(type, world);
		refreshPositionAndAngles(x, y, z, getYaw(), getPitch());
		refreshPosition();
		setVelocityWithAcceleration(velocity, accelerationPower);
	}

	public ExplosiveProjectileEntity(
		EntityType<? extends ExplosiveProjectileEntity> type,
		LivingEntity owner,
		Vec3d velocity,
		World world
	) {
		this(type, owner.getX(), owner.getY(), owner.getZ(), velocity, world);
		setOwner(owner);
		setRotation(owner.getYaw(), owner.getPitch());
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
	}

	@Override
	public boolean shouldRender(double distance) {
		double sideLen = getBoundingBox().getAverageSideLength() * 4.0;
		if (Double.isNaN(sideLen)) {
			sideLen = 4.0;
		}

		sideLen *= 64.0;
		return distance < sideLen * sideLen;
	}

	protected RaycastContext.ShapeType getRaycastShapeType() {
		return RaycastContext.ShapeType.COLLIDER;
	}

	@Override
	public void tick() {
		Entity ownerEntity = getOwner();
		boolean ownerValid = ownerEntity == null || !ownerEntity.isRemoved();
		boolean chunkLoaded = getEntityWorld().isChunkLoaded(getBlockPos());

		if (!getEntityWorld().isClient() && !ownerValid && !chunkLoaded) {
			discard();
			return;
		}

		applyDrag();
		HitResult hitResult = ProjectileUtil.getCollision(this, this::canHit, getRaycastShapeType());
		Vec3d nextPos = hitResult.getType() != HitResult.Type.MISS
			? hitResult.getPos()
			: getEntityPos().add(getVelocity());

		ProjectileUtil.setRotationFromVelocity(this, 0.2F);
		setPosition(nextPos);
		tickBlockCollision();
		super.tick();

		if (isBurning()) {
			setOnFireFor(1.0F);
		}

		if (hitResult.getType() != HitResult.Type.MISS && isAlive()) {
			hitOrDeflect(hitResult);
		}

		addParticles();
	}

	/**
	 * Применяет сопротивление среды и ускорение снаряда.
	 * В воде — спавнит пузырьки и применяет водное сопротивление.
	 */
	private void applyDrag() {
		Vec3d velocity = getVelocity();
		Vec3d pos = getEntityPos();
		float drag;

		if (isTouchingWater()) {
			for (int i = 0; i < BUBBLE_PARTICLE_COUNT; i++) {
				getEntityWorld().addParticleClient(
					ParticleTypes.BUBBLE,
					pos.x - velocity.x * BUBBLE_PARTICLE_OFFSET,
					pos.y - velocity.y * BUBBLE_PARTICLE_OFFSET,
					pos.z - velocity.z * BUBBLE_PARTICLE_OFFSET,
					velocity.x,
					velocity.y,
					velocity.z
				);
			}

			drag = getDragInWater();
		} else {
			drag = getDrag();
		}

		setVelocity(velocity.add(velocity.normalize().multiply(accelerationPower)).multiply(drag));
	}

	private void addParticles() {
		ParticleEffect particleType = getParticleType();
		if (particleType == null) {
			return;
		}

		Vec3d pos = getEntityPos();
		getEntityWorld().addParticleClient(particleType, pos.x, pos.y + 0.5, pos.z, 0.0, 0.0, 0.0);
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		return false;
	}

	@Override
	protected boolean canHit(Entity entity) {
		return super.canHit(entity) && !entity.noClip;
	}

	protected boolean isBurning() {
		return true;
	}

	protected @Nullable ParticleEffect getParticleType() {
		return ParticleTypes.SMOKE;
	}

	protected float getDrag() {
		return 0.95F;
	}

	protected float getDragInWater() {
		return 0.8F;
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putDouble("acceleration_power", accelerationPower);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		accelerationPower = view.getDouble("acceleration_power", DEFAULT_ACCELERATION_POWER);
	}

	@Override
	public float getBrightnessAtEyes() {
		return 1.0F;
	}

	/**
	 * Задаёт начальную скорость снаряда, нормализуя вектор и умножая на ускорение.
	 *
	 * @param velocity         вектор направления
	 * @param accelerationPower начальное ускорение
	 */
	private void setVelocityWithAcceleration(Vec3d velocity, double accelerationPower) {
		setVelocity(velocity.normalize().multiply(accelerationPower));
		velocityDirty = true;
	}

	/**
	 * При отклонении от атаки восстанавливает стандартное ускорение.
	 * При пассивном отклонении (блок, щит) — уменьшает ускорение вдвое.
	 */
	@Override
	protected void onDeflected(boolean fromAttack) {
		super.onDeflected(fromAttack);
		if (fromAttack) {
			accelerationPower = DEFAULT_ACCELERATION_POWER;
		} else {
			accelerationPower *= DEFLECTION_POWER_FACTOR;
		}
	}
}
