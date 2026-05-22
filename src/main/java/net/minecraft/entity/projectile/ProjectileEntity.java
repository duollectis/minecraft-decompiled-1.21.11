package net.minecraft.entity.projectile;

import com.google.common.base.MoreObjects;
import it.unimi.dsi.fastutil.doubles.DoubleDoubleImmutablePair;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Базовый класс для всех снарядов (стрелы, огненные шары, трезубцы и т.д.).
 * <p>
 * Управляет владельцем снаряда, логикой отклонения, столкновений и спавна.
 * Подклассы реализуют конкретное поведение при попадании в блок или сущность.
 */
public abstract class ProjectileEntity extends Entity implements Ownable {

	private static final float VELOCITY_SPREAD_FACTOR = 0.0172275F;
	private static final float ROTATION_LERP_FACTOR = 0.2F;
	private static final float FULL_ROTATION = 360.0F;
	private static final float HALF_ROTATION = 180.0F;
	private static final double WORLD_BORDER_DEFLECT_SPEED_FACTOR = 0.2;
	private static final int DEFAULT_PORTAL_COOLDOWN = 2;

	protected @Nullable LazyEntityReference<Entity> owner;
	private boolean leftOwner;
	private boolean checkedForLeftOwner;
	private boolean shot;
	private @Nullable Entity lastDeflectedEntity;

	public ProjectileEntity(EntityType<? extends ProjectileEntity> entityType, World world) {
		super(entityType, world);
	}

	protected void setOwner(@Nullable LazyEntityReference<Entity> owner) {
		this.owner = owner;
	}

	public void setOwner(@Nullable Entity owner) {
		setOwner(LazyEntityReference.of(owner));
	}

	@Override
	public @Nullable Entity getOwner() {
		return LazyEntityReference.getEntity(owner, getEntityWorld());
	}

	/**
	 * Возвращает сущность, которая является причиной эффектов от этого снаряда.
	 * Если владелец задан — возвращает его, иначе сам снаряд.
	 */
	public Entity getEffectCause() {
		return (Entity) MoreObjects.firstNonNull(getOwner(), this);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		LazyEntityReference.writeData(owner, view, "Owner");
		if (leftOwner) {
			view.putBoolean("LeftOwner", true);
		}

		view.putBoolean("HasBeenShot", shot);
	}

	protected boolean isOwner(Entity entity) {
		return owner != null && owner.uuidEquals(entity);
	}

	@Override
	protected void readCustomData(ReadView view) {
		setOwner(LazyEntityReference.fromData(view, "Owner"));
		leftOwner = view.getBoolean("LeftOwner", false);
		shot = view.getBoolean("HasBeenShot", false);
	}

	@Override
	public void copyFrom(Entity original) {
		super.copyFrom(original);
		if (original instanceof ProjectileEntity projectile) {
			owner = projectile.owner;
		}
	}

	@Override
	public void tick() {
		if (!shot) {
			emitGameEvent(GameEvent.PROJECTILE_SHOOT, getOwner());
			shot = true;
		}

		tickLeftOwner();
		super.tick();
		checkedForLeftOwner = false;
	}

	/**
	 * Проверяет, покинул ли снаряд зону владельца, и обновляет флаг {@code leftOwner}.
	 * Вызывается один раз за тик до основной логики движения.
	 */
	protected void tickLeftOwner() {
		if (leftOwner || checkedForLeftOwner) {
			return;
		}

		leftOwner = hasLeftOwner();
		checkedForLeftOwner = true;
	}

	/**
	 * Определяет, вышел ли снаряд за пределы хитбокса владельца и его пассажиров.
	 * Используется для предотвращения самоповреждения сразу после выстрела.
	 */
	private boolean hasLeftOwner() {
		Entity ownerEntity = getOwner();
		if (ownerEntity == null) {
			return true;
		}

		Box expandedBox = getBoundingBox().stretch(getVelocity()).expand(1.0);
		return ownerEntity.getRootVehicle()
			.streamSelfAndPassengers()
			.filter(EntityPredicates.CAN_HIT)
			.noneMatch(passenger -> expandedBox.intersects(passenger.getBoundingBox()));
	}

	/**
	 * Вычисляет вектор скорости снаряда с учётом разброса.
	 *
	 * @param x         направление по X
	 * @param y         направление по Y
	 * @param z         направление по Z
	 * @param power     множитель скорости
	 * @param uncertainty угол разброса (чем больше — тем хаотичнее траектория)
	 * @return нормализованный вектор скорости с применённым разбросом и мощностью
	 */
	public Vec3d calculateVelocity(double x, double y, double z, float power, float uncertainty) {
		return new Vec3d(x, y, z)
			.normalize()
			.add(
				random.nextTriangular(0.0, VELOCITY_SPREAD_FACTOR * uncertainty),
				random.nextTriangular(0.0, VELOCITY_SPREAD_FACTOR * uncertainty),
				random.nextTriangular(0.0, VELOCITY_SPREAD_FACTOR * uncertainty)
			)
			.multiply(power);
	}

	public void setVelocity(double x, double y, double z, float power, float uncertainty) {
		Vec3d velocity = calculateVelocity(x, y, z, power, uncertainty);
		setVelocity(velocity);
		velocityDirty = true;
		double horizontalLen = velocity.horizontalLength();
		setYaw((float) (MathHelper.atan2(velocity.x, velocity.z) * HALF_ROTATION / (float) Math.PI));
		setPitch((float) (MathHelper.atan2(velocity.y, horizontalLen) * HALF_ROTATION / (float) Math.PI));
		lastYaw = getYaw();
		lastPitch = getPitch();
	}

	/**
	 * Задаёт скорость снаряда на основе угла поворота стрелка.
	 * Учитывает горизонтальное движение стрелка (но не вертикальное, если он на земле).
	 *
	 * @param shooter    сущность, выпустившая снаряд
	 * @param pitch      угол наклона (вертикаль)
	 * @param yaw        угол поворота (горизонталь)
	 * @param roll       дополнительный угол наклона (например, для навесной стрельбы)
	 * @param speed      скорость снаряда
	 * @param divergence разброс
	 */
	public void setVelocity(Entity shooter, float pitch, float yaw, float roll, float speed, float divergence) {
		float dirX = -MathHelper.sin(yaw * (float) (Math.PI / HALF_ROTATION))
			* MathHelper.cos(pitch * (float) (Math.PI / HALF_ROTATION));
		float dirY = -MathHelper.sin((pitch + roll) * (float) (Math.PI / HALF_ROTATION));
		float dirZ = MathHelper.cos(yaw * (float) (Math.PI / HALF_ROTATION))
			* MathHelper.cos(pitch * (float) (Math.PI / HALF_ROTATION));
		setVelocity(dirX, dirY, dirZ, speed, divergence);
		Vec3d shooterMovement = shooter.getMovement();
		setVelocity(getVelocity().add(
			shooterMovement.x,
			shooter.isOnGround() ? 0.0 : shooterMovement.y,
			shooterMovement.z
		));
	}

	@Override
	public void onBubbleColumnSurfaceCollision(boolean drag, BlockPos pos) {
		double deltaY = drag ? -0.03 : 0.1;
		setVelocity(getVelocity().add(0.0, deltaY, 0.0));
		spawnBubbleColumnParticles(getEntityWorld(), pos);
	}

	@Override
	public void onBubbleColumnCollision(boolean drag) {
		double deltaY = drag ? -0.03 : 0.06;
		setVelocity(getVelocity().add(0.0, deltaY, 0.0));
		onLanding();
	}

	/**
	 * Создаёт снаряд через фабрику, задаёт ему скорость на основе угла стрелка и спавнит в мире.
	 *
	 * @param creator        фабрика снаряда
	 * @param world          серверный мир
	 * @param projectileStack предмет-снаряд (для триггеров зачарований)
	 * @param shooter        стрелок
	 * @param roll           дополнительный угол наклона
	 * @param power          скорость
	 * @param divergence     разброс
	 * @return созданный и заспавненный снаряд
	 */
	public static <T extends ProjectileEntity> T spawnWithVelocity(
		ProjectileCreator<T> creator,
		ServerWorld world,
		ItemStack projectileStack,
		LivingEntity shooter,
		float roll,
		float power,
		float divergence
	) {
		return spawn(
			creator.create(world, shooter, projectileStack),
			world,
			projectileStack,
			entity -> entity.setVelocity(shooter, shooter.getPitch(), shooter.getYaw(), roll, power, divergence)
		);
	}

	/**
	 * Создаёт снаряд через фабрику, задаёт ему скорость по вектору и спавнит в мире.
	 *
	 * @param creator        фабрика снаряда
	 * @param world          серверный мир
	 * @param projectileStack предмет-снаряд
	 * @param shooter        стрелок
	 * @param velocityX      скорость по X
	 * @param velocityY      скорость по Y
	 * @param velocityZ      скорость по Z
	 * @param power          множитель скорости
	 * @param divergence     разброс
	 * @return созданный и заспавненный снаряд
	 */
	public static <T extends ProjectileEntity> T spawnWithVelocity(
		ProjectileCreator<T> creator,
		ServerWorld world,
		ItemStack projectileStack,
		LivingEntity shooter,
		double velocityX,
		double velocityY,
		double velocityZ,
		float power,
		float divergence
	) {
		return spawn(
			creator.create(world, shooter, projectileStack),
			world,
			projectileStack,
			entity -> entity.setVelocity(velocityX, velocityY, velocityZ, power, divergence)
		);
	}

	public static <T extends ProjectileEntity> T spawnWithVelocity(
		T projectile,
		ServerWorld world,
		ItemStack projectileStack,
		double velocityX,
		double velocityY,
		double velocityZ,
		float power,
		float divergence
	) {
		return spawn(
			projectile,
			world,
			projectileStack,
			entity -> projectile.setVelocity(velocityX, velocityY, velocityZ, power, divergence)
		);
	}

	public static <T extends ProjectileEntity> T spawn(T projectile, ServerWorld world, ItemStack projectileStack) {
		return spawn(projectile, world, projectileStack, entity -> {});
	}

	/**
	 * Спавнит снаряд в мире, выполняя предварительную настройку через {@code beforeSpawn},
	 * и запускает триггеры зачарований на предмете-снаряде.
	 *
	 * @param projectile    снаряд для спавна
	 * @param world         серверный мир
	 * @param projectileStack предмет-снаряд
	 * @param beforeSpawn   действие, выполняемое перед спавном (например, задать скорость)
	 * @return заспавненный снаряд
	 */
	public static <T extends ProjectileEntity> T spawn(
		T projectile,
		ServerWorld world,
		ItemStack projectileStack,
		Consumer<T> beforeSpawn
	) {
		beforeSpawn.accept(projectile);
		world.spawnEntity(projectile);
		projectile.triggerProjectileSpawned(world, projectileStack);
		return projectile;
	}

	/**
	 * Запускает триггеры зачарований при спавне снаряда.
	 * Для {@link PersistentProjectileEntity} дополнительно обрабатывает зачарования оружия.
	 *
	 * @param world          серверный мир
	 * @param projectileStack предмет-снаряд
	 */
	public void triggerProjectileSpawned(ServerWorld world, ItemStack projectileStack) {
		EnchantmentHelper.onProjectileSpawned(world, projectileStack, this, item -> {});
		if (!(this instanceof PersistentProjectileEntity persistentProjectile)) {
			return;
		}

		ItemStack weaponStack = persistentProjectile.getWeaponStack();
		if (weaponStack != null && !weaponStack.isEmpty() && !projectileStack.getItem().equals(weaponStack.getItem())) {
			EnchantmentHelper.onProjectileSpawned(world, weaponStack, this, persistentProjectile::onBroken);
		}
	}

	/**
	 * Обрабатывает попадание снаряда: либо отклоняет его, либо вызывает {@link #onCollision}.
	 * При попадании в сущность с флагом отклонения — снаряд меняет направление.
	 * При попадании в границу мира — применяется простое отклонение.
	 *
	 * @param hitResult результат трассировки луча
	 * @return тип отклонения (NONE если столкновение обработано как попадание)
	 */
	protected ProjectileDeflection hitOrDeflect(HitResult hitResult) {
		if (hitResult.getType() == HitResult.Type.ENTITY) {
			EntityHitResult entityHit = (EntityHitResult) hitResult;
			Entity hitEntity = entityHit.getEntity();
			ProjectileDeflection deflection = hitEntity.getProjectileDeflection(this);
			if (deflection != ProjectileDeflection.NONE) {
				if (hitEntity != lastDeflectedEntity && deflect(deflection, hitEntity, owner, false)) {
					lastDeflectedEntity = hitEntity;
				}

				return deflection;
			}
		} else if (deflectsAgainstWorldBorder()
			&& hitResult instanceof BlockHitResult blockHit
			&& blockHit.isAgainstWorldBorder()
		) {
			ProjectileDeflection borderDeflection = ProjectileDeflection.SIMPLE;
			if (deflect(borderDeflection, null, owner, false)) {
				setVelocity(getVelocity().multiply(WORLD_BORDER_DEFLECT_SPEED_FACTOR));
				return borderDeflection;
			}
		}

		onCollision(hitResult);
		return ProjectileDeflection.NONE;
	}

	protected boolean deflectsAgainstWorldBorder() {
		return false;
	}

	/**
	 * Применяет отклонение снаряда: меняет его направление и при необходимости владельца.
	 *
	 * @param deflection          тип отклонения
	 * @param deflector           сущность, отклонившая снаряд (может быть null)
	 * @param newOwnerRef         новый владелец снаряда после отклонения
	 * @param fromAttack          true если отклонение произошло от атаки
	 * @return всегда true (отклонение применено)
	 */
	public boolean deflect(
		ProjectileDeflection deflection,
		@Nullable Entity deflector,
		@Nullable LazyEntityReference<Entity> newOwnerRef,
		boolean fromAttack
	) {
		deflection.deflect(this, deflector, random);
		if (!getEntityWorld().isClient()) {
			setOwner(newOwnerRef);
			onDeflected(fromAttack);
		}

		return true;
	}

	protected void onDeflected(boolean fromAttack) {
	}

	protected void onBroken(Item item) {
	}

	/**
	 * Обрабатывает столкновение снаряда с блоком или сущностью.
	 * При попадании в перенаправляемый снаряд — отклоняет его.
	 * Генерирует игровое событие {@link GameEvent#PROJECTILE_LAND}.
	 *
	 * @param hitResult результат трассировки луча
	 */
	protected void onCollision(HitResult hitResult) {
		HitResult.Type type = hitResult.getType();
		if (type == HitResult.Type.ENTITY) {
			EntityHitResult entityHit = (EntityHitResult) hitResult;
			Entity hitEntity = entityHit.getEntity();
			if (hitEntity.getType().isIn(EntityTypeTags.REDIRECTABLE_PROJECTILE)
				&& hitEntity instanceof ProjectileEntity redirectable
			) {
				redirectable.deflect(ProjectileDeflection.REDIRECTED, getOwner(), owner, true);
			}

			onEntityHit(entityHit);
			getEntityWorld().emitGameEvent(
				GameEvent.PROJECTILE_LAND,
				hitResult.getPos(),
				GameEvent.Emitter.of(this, null)
			);
		} else if (type == HitResult.Type.BLOCK) {
			BlockHitResult blockHit = (BlockHitResult) hitResult;
			onBlockHit(blockHit);
			BlockPos blockPos = blockHit.getBlockPos();
			getEntityWorld().emitGameEvent(
				GameEvent.PROJECTILE_LAND,
				blockPos,
				GameEvent.Emitter.of(this, getEntityWorld().getBlockState(blockPos))
			);
		}
	}

	protected void onEntityHit(EntityHitResult entityHitResult) {
	}

	protected void onBlockHit(BlockHitResult blockHitResult) {
		BlockState blockState = getEntityWorld().getBlockState(blockHitResult.getBlockPos());
		blockState.onProjectileHit(getEntityWorld(), blockState, blockHitResult, this);
	}

	/**
	 * Проверяет, может ли снаряд поразить данную сущность.
	 * Запрещает попадание в сущности, связанные с владельцем через транспортное средство,
	 * пока снаряд не покинул зону владельца.
	 *
	 * @param entity проверяемая сущность
	 * @return true если попадание допустимо
	 */
	protected boolean canHit(Entity entity) {
		if (!entity.canBeHitByProjectile()) {
			return false;
		}

		Entity ownerEntity = getOwner();
		return ownerEntity == null || leftOwner || !ownerEntity.isConnectedThroughVehicle(entity);
	}

	/**
	 * Обновляет углы поворота снаряда на основе текущего вектора скорости.
	 * Использует интерполяцию для плавного вращения.
	 */
	protected void updateRotation() {
		Vec3d velocity = getVelocity();
		double horizontalLen = velocity.horizontalLength();
		setPitch(updateRotation(
			lastPitch,
			(float) (MathHelper.atan2(velocity.y, horizontalLen) * HALF_ROTATION / (float) Math.PI)
		));
		setYaw(updateRotation(
			lastYaw,
			(float) (MathHelper.atan2(velocity.x, velocity.z) * HALF_ROTATION / (float) Math.PI)
		));
	}

	/**
	 * Интерполирует угол поворота от предыдущего к новому значению,
	 * нормализуя разницу в диапазон [-180, 180].
	 *
	 * @param lastRot предыдущий угол
	 * @param newRot  целевой угол
	 * @return интерполированный угол
	 */
	protected static float updateRotation(float lastRot, float newRot) {
		while (newRot - lastRot < -HALF_ROTATION) {
			lastRot -= FULL_ROTATION;
		}

		while (newRot - lastRot >= HALF_ROTATION) {
			lastRot += FULL_ROTATION;
		}

		return MathHelper.lerp(ROTATION_LERP_FACTOR, lastRot, newRot);
	}

	@Override
	public Packet<ClientPlayPacketListener> createSpawnPacket(EntityTrackerEntry entityTrackerEntry) {
		Entity ownerEntity = getOwner();
		return new EntitySpawnS2CPacket(this, entityTrackerEntry, ownerEntity == null ? 0 : ownerEntity.getId());
	}

	@Override
	public void onSpawnPacket(EntitySpawnS2CPacket packet) {
		super.onSpawnPacket(packet);
		Entity ownerEntity = getEntityWorld().getEntityById(packet.getEntityData());
		if (ownerEntity != null) {
			setOwner(ownerEntity);
		}
	}

	@Override
	public boolean canModifyAt(ServerWorld world, BlockPos pos) {
		Entity ownerEntity = getOwner();
		if (ownerEntity instanceof PlayerEntity) {
			return ownerEntity.canModifyAt(world, pos);
		}

		return ownerEntity == null || world.getGameRules().getValue(GameRules.DO_MOB_GRIEFING);
	}

	/**
	 * Проверяет, может ли снаряд разрушать блоки в данном мире.
	 * Требует тег {@link EntityTypeTags#IMPACT_PROJECTILES} и включённое правило игры.
	 *
	 * @param world серверный мир
	 * @return true если разрушение блоков разрешено
	 */
	public boolean canBreakBlocks(ServerWorld world) {
		return getType().isIn(EntityTypeTags.IMPACT_PROJECTILES)
			&& world.getGameRules().getValue(GameRules.PROJECTILES_CAN_BREAK_BLOCKS);
	}

	@Override
	public boolean canHit() {
		return getType().isIn(EntityTypeTags.REDIRECTABLE_PROJECTILE);
	}

	@Override
	public float getTargetingMargin() {
		return canHit() ? 1.0F : 0.0F;
	}

	public DoubleDoubleImmutablePair getKnockback(LivingEntity target, DamageSource source) {
		return DoubleDoubleImmutablePair.of(getVelocity().x, getVelocity().z);
	}

	@Override
	public int getDefaultPortalCooldown() {
		return DEFAULT_PORTAL_COOLDOWN;
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		if (!isAlwaysInvulnerableTo(source)) {
			scheduleVelocityUpdate();
		}

		return false;
	}

	/**
	 * Фабричный интерфейс для создания снарядов.
	 * Используется в статических методах {@code spawn} и {@code spawnWithVelocity}.
	 */
	@FunctionalInterface
	public interface ProjectileCreator<T extends ProjectileEntity> {

		T create(ServerWorld world, LivingEntity shooter, ItemStack stack);
	}
}
