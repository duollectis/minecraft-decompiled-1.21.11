package net.minecraft.entity.projectile;

import it.unimi.dsi.fastutil.doubles.DoubleDoubleImmutablePair;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.OptionalInt;

/**
 * Ракета фейерверка — снаряд, который либо летит самостоятельно, либо
 * прикреплён к планирующей сущности (режим {@code wasShotByEntity}).
 * <p>
 * В режиме планирования ракета толкает владельца вперёд по вектору взгляда.
 * По истечении {@link #lifeTime} тиков или при столкновении с блоком/сущностью
 * происходит взрыв, наносящий урон в радиусе {@link #EXPLOSION_RADIUS} блоков.
 */
public class FireworkRocketEntity extends ProjectileEntity implements FlyingItemEntity {

	private static final TrackedData<ItemStack> ITEM =
			DataTracker.registerData(FireworkRocketEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
	private static final TrackedData<OptionalInt> SHOOTER_ENTITY_ID =
			DataTracker.registerData(FireworkRocketEntity.class, TrackedDataHandlerRegistry.OPTIONAL_INT);
	private static final TrackedData<Boolean> SHOT_AT_ANGLE =
			DataTracker.registerData(FireworkRocketEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

	/** Статус-байт, отправляемый клиенту для воспроизведения анимации взрыва. */
	private static final byte EXPLODE_STATUS = 17;

	/** Статус-байт, отправляемый клиенту для воспроизведения анимации подтягивания. */
	private static final byte PULL_STATUS = 31;

	/** Базовое время жизни на один уровень полёта (в тиках). */
	private static final int LIFE_PER_FLIGHT_LEVEL = 10;

	/** Случайный разброс времени жизни (в тиках). */
	private static final int LIFE_RANDOM_RANGE_A = 6;
	private static final int LIFE_RANDOM_RANGE_B = 7;

	/** Ускорение по горизонтали при полёте без угла. */
	private static final double HORIZONTAL_ACCELERATION = 1.15;

	/** Вертикальное ускорение при полёте без угла. */
	private static final double VERTICAL_ACCELERATION = 0.04;

	/** Начальный разброс скорости при спавне. */
	private static final double INITIAL_VELOCITY_SPREAD = 0.002297;

	/** Начальная вертикальная скорость при спавне. */
	private static final double INITIAL_VERTICAL_VELOCITY = 0.05;

	/** Радиус взрыва (в блоках). */
	private static final double EXPLOSION_RADIUS = 5.0;

	/** Квадрат радиуса взрыва. */
	private static final double EXPLOSION_RADIUS_SQUARED = EXPLOSION_RADIUS * EXPLOSION_RADIUS;

	/** Базовый урон от взрыва. */
	private static final float BASE_EXPLOSION_DAMAGE = 5.0F;

	/** Дополнительный урон за каждый эффект взрыва. */
	private static final float DAMAGE_PER_EXPLOSION = 2.0F;

	/** Коэффициент ускорения планирующей сущности. */
	private static final double GLIDE_BOOST_FACTOR = 0.1;

	/** Целевая скорость планирующей сущности. */
	private static final double GLIDE_TARGET_SPEED = 1.5;

	/** Коэффициент интерполяции скорости планирования. */
	private static final double GLIDE_LERP_FACTOR = 0.5;

	/** Максимальная дистанция рендера (квадрат). */
	private static final double MAX_RENDER_DISTANCE_SQUARED = 4096.0;

	private int life = 0;
	private int lifeTime = 0;
	private @Nullable LivingEntity shooter;

	public FireworkRocketEntity(EntityType<? extends FireworkRocketEntity> entityType, World world) {
		super(entityType, world);
	}

	public FireworkRocketEntity(World world, double x, double y, double z, ItemStack stack) {
		super(EntityType.FIREWORK_ROCKET, world);
		setPosition(x, y, z);
		dataTracker.set(ITEM, stack.copy());

		int flightDuration = 1;
		FireworksComponent fireworksComponent = stack.get(DataComponentTypes.FIREWORKS);
		if (fireworksComponent != null) {
			flightDuration += fireworksComponent.flightDuration();
		}

		setVelocity(
				random.nextTriangular(0.0, INITIAL_VELOCITY_SPREAD),
				INITIAL_VERTICAL_VELOCITY,
				random.nextTriangular(0.0, INITIAL_VELOCITY_SPREAD)
		);
		lifeTime = LIFE_PER_FLIGHT_LEVEL * flightDuration
				+ random.nextInt(LIFE_RANDOM_RANGE_A)
				+ random.nextInt(LIFE_RANDOM_RANGE_B);
	}

	public FireworkRocketEntity(World world, @Nullable Entity entity, double x, double y, double z, ItemStack stack) {
		this(world, x, y, z, stack);
		setOwner(entity);
	}

	public FireworkRocketEntity(World world, ItemStack stack, LivingEntity shooter) {
		this(world, shooter, shooter.getX(), shooter.getY(), shooter.getZ(), stack);
		dataTracker.set(SHOOTER_ENTITY_ID, OptionalInt.of(shooter.getId()));
		this.shooter = shooter;
	}

	public FireworkRocketEntity(World world, ItemStack stack, double x, double y, double z, boolean shotAtAngle) {
		this(world, x, y, z, stack);
		dataTracker.set(SHOT_AT_ANGLE, shotAtAngle);
	}

	public FireworkRocketEntity(
			World world,
			ItemStack stack,
			Entity entity,
			double x,
			double y,
			double z,
			boolean shotAtAngle
	) {
		this(world, stack, x, y, z, shotAtAngle);
		setOwner(entity);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(ITEM, getDefaultStack());
		builder.add(SHOOTER_ENTITY_ID, OptionalInt.empty());
		builder.add(SHOT_AT_ANGLE, false);
	}

	@Override
	public boolean shouldRender(double distance) {
		return distance < MAX_RENDER_DISTANCE_SQUARED && !wasShotByEntity();
	}

	@Override
	public boolean shouldRender(double cameraX, double cameraY, double cameraZ) {
		return super.shouldRender(cameraX, cameraY, cameraZ) && !wasShotByEntity();
	}

	@Override
	public void tick() {
		super.tick();
		HitResult hitResult;

		if (wasShotByEntity()) {
			hitResult = tickGlidingMode();
		} else {
			hitResult = tickFreeFlightMode();
		}

		if (!noClip && isAlive() && hitResult.getType() != HitResult.Type.MISS) {
			hitOrDeflect(hitResult);
			velocityDirty = true;
		}

		updateRotation();
		playLaunchSoundOnFirstTick();
		life++;
		spawnTrailParticles();

		if (life > lifeTime && getEntityWorld() instanceof ServerWorld serverWorld) {
			explodeAndRemove(serverWorld);
		}
	}

	private HitResult tickGlidingMode() {
		if (shooter == null) {
			dataTracker.get(SHOOTER_ENTITY_ID).ifPresent(id -> {
				Entity entity = getEntityWorld().getEntityById(id);
				if (entity instanceof LivingEntity living) {
					shooter = living;
				}
			});
		}

		if (shooter != null) {
			Vec3d handOffset;

			if (shooter.isGliding()) {
				Vec3d lookVec = shooter.getRotationVector();
				Vec3d shooterVelocity = shooter.getVelocity();
				shooter.setVelocity(shooterVelocity.add(
						lookVec.x * GLIDE_BOOST_FACTOR + (lookVec.x * GLIDE_TARGET_SPEED - shooterVelocity.x) * GLIDE_LERP_FACTOR,
						lookVec.y * GLIDE_BOOST_FACTOR + (lookVec.y * GLIDE_TARGET_SPEED - shooterVelocity.y) * GLIDE_LERP_FACTOR,
						lookVec.z * GLIDE_BOOST_FACTOR + (lookVec.z * GLIDE_TARGET_SPEED - shooterVelocity.z) * GLIDE_LERP_FACTOR
				));
				handOffset = shooter.getHandPosOffset(Items.FIREWORK_ROCKET);
			} else {
				handOffset = Vec3d.ZERO;
			}

			setPosition(
					shooter.getX() + handOffset.x,
					shooter.getY() + handOffset.y,
					shooter.getZ() + handOffset.z
			);
			setVelocity(shooter.getVelocity());
		}

		return ProjectileUtil.getCollision(this, this::canHit);
	}

	private HitResult tickFreeFlightMode() {
		if (!wasShotAtAngle()) {
			double horizontalFactor = horizontalCollision ? 1.0 : HORIZONTAL_ACCELERATION;
			setVelocity(getVelocity().multiply(horizontalFactor, 1.0, horizontalFactor).add(0.0, VERTICAL_ACCELERATION, 0.0));
		}

		Vec3d velocity = getVelocity();
		HitResult hitResult = ProjectileUtil.getCollision(this, this::canHit);
		move(MovementType.SELF, velocity);
		tickBlockCollision();
		setVelocity(velocity);
		return hitResult;
	}

	private void playLaunchSoundOnFirstTick() {
		if (life == 0 && !isSilent()) {
			getEntityWorld().playSound(
					null,
					getX(),
					getY(),
					getZ(),
					SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH,
					SoundCategory.AMBIENT,
					3.0F,
					1.0F
			);
		}
	}

	private void spawnTrailParticles() {
		if (getEntityWorld().isClient() && life % 2 < 2) {
			getEntityWorld().addParticleClient(
					ParticleTypes.FIREWORK,
					getX(),
					getY(),
					getZ(),
					random.nextGaussian() * 0.05,
					-getVelocity().y * 0.5,
					random.nextGaussian() * 0.05
			);
		}
	}

	private void explodeAndRemove(ServerWorld world) {
		world.sendEntityStatus(this, EXPLODE_STATUS);
		emitGameEvent(GameEvent.EXPLODE, getOwner());
		explode(world);
		discard();
	}

	@Override
	protected void onEntityHit(EntityHitResult entityHitResult) {
		super.onEntityHit(entityHitResult);
		if (getEntityWorld() instanceof ServerWorld serverWorld) {
			explodeAndRemove(serverWorld);
		}
	}

	@Override
	protected void onBlockHit(BlockHitResult blockHitResult) {
		BlockPos blockPos = new BlockPos(blockHitResult.getBlockPos());
		getEntityWorld()
				.getBlockState(blockPos)
				.onEntityCollision(getEntityWorld(), blockPos, this, EntityCollisionHandler.DUMMY, true);

		if (getEntityWorld() instanceof ServerWorld serverWorld && hasExplosionEffects()) {
			explodeAndRemove(serverWorld);
		}

		super.onBlockHit(blockHitResult);
	}

	private boolean hasExplosionEffects() {
		return !getExplosions().isEmpty();
	}

	/**
	 * Наносит урон всем живым сущностям в радиусе взрыва, у которых есть прямая
	 * видимость до ракеты (проверяется через рейкаст по двум точкам тела).
	 * Урон масштабируется по расстоянию и количеству эффектов взрыва.
	 *
	 * @param world серверный мир, в котором происходит взрыв
	 */
	private void explode(ServerWorld world) {
		List<FireworkExplosionComponent> explosions = getExplosions();
		if (explosions.isEmpty()) {
			return;
		}

		float totalDamage = BASE_EXPLOSION_DAMAGE + explosions.size() * DAMAGE_PER_EXPLOSION;

		if (shooter != null) {
			shooter.damage(world, getDamageSources().fireworks(this, getOwner()), totalDamage);
		}

		Vec3d origin = getEntityPos();

		for (LivingEntity nearby : getEntityWorld().getNonSpectatingEntities(
				LivingEntity.class,
				getBoundingBox().expand(EXPLOSION_RADIUS)
		)) {
			if (nearby == shooter || squaredDistanceTo(nearby) > EXPLOSION_RADIUS_SQUARED) {
				continue;
			}

			boolean hasLineOfSight = false;

			for (int check = 0; check < 2; check++) {
				Vec3d checkPos = new Vec3d(nearby.getX(), nearby.getBodyY(0.5 * check), nearby.getZ());
				HitResult raycast = getEntityWorld().raycast(new RaycastContext(
						origin,
						checkPos,
						RaycastContext.ShapeType.COLLIDER,
						RaycastContext.FluidHandling.NONE,
						this
				));

				if (raycast.getType() == HitResult.Type.MISS) {
					hasLineOfSight = true;
					break;
				}
			}

			if (hasLineOfSight) {
				float scaledDamage = totalDamage * (float) Math.sqrt(
						(EXPLOSION_RADIUS - distanceTo(nearby)) / EXPLOSION_RADIUS
				);
				nearby.damage(world, getDamageSources().fireworks(this, getOwner()), scaledDamage);
			}
		}
	}

	private boolean wasShotByEntity() {
		return dataTracker.get(SHOOTER_ENTITY_ID).isPresent();
	}

	public boolean wasShotAtAngle() {
		return dataTracker.get(SHOT_AT_ANGLE);
	}

	@Override
	public void handleStatus(byte status) {
		if (status == EXPLODE_STATUS && getEntityWorld().isClient()) {
			Vec3d velocity = getVelocity();
			getEntityWorld().addFireworkParticle(
					getX(),
					getY(),
					getZ(),
					velocity.x,
					velocity.y,
					velocity.z,
					getExplosions()
			);
		}

		super.handleStatus(status);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putInt("Life", life);
		view.putInt("LifeTime", lifeTime);
		view.put("FireworksItem", ItemStack.CODEC, getStack());
		view.putBoolean("ShotAtAngle", dataTracker.get(SHOT_AT_ANGLE));
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		life = view.getInt("Life", 0);
		lifeTime = view.getInt("LifeTime", 0);
		dataTracker.set(ITEM, view.<ItemStack>read("FireworksItem", ItemStack.CODEC).orElse(getDefaultStack()));
		dataTracker.set(SHOT_AT_ANGLE, view.getBoolean("ShotAtAngle", false));
	}

	private List<FireworkExplosionComponent> getExplosions() {
		ItemStack itemStack = dataTracker.get(ITEM);
		FireworksComponent fireworksComponent = itemStack.get(DataComponentTypes.FIREWORKS);
		return fireworksComponent != null ? fireworksComponent.explosions() : List.of();
	}

	@Override
	public ItemStack getStack() {
		return dataTracker.get(ITEM);
	}

	@Override
	public boolean isAttackable() {
		return false;
	}

	private static ItemStack getDefaultStack() {
		return new ItemStack(Items.FIREWORK_ROCKET);
	}

	@Override
	public DoubleDoubleImmutablePair getKnockback(LivingEntity target, DamageSource source) {
		double deltaX = target.getEntityPos().x - getEntityPos().x;
		double deltaZ = target.getEntityPos().z - getEntityPos().z;
		return DoubleDoubleImmutablePair.of(deltaX, deltaZ);
	}
}
