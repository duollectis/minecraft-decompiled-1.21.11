package net.minecraft.entity.projectile;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ProjectileDeflection;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Трезубец — метательное оружие, которое может возвращаться к владельцу благодаря зачарованию «Верность».
 * <p>
 * После нанесения урона или застревания в блоке начинает обратный полёт к владельцу.
 * Уровень верности определяет скорость возврата. Если владелец мёртв или недоступен —
 * трезубец дропается на землю.
 */
public class TridentEntity extends PersistentProjectileEntity {

	private static final float BASE_DAMAGE = 8.0F;
	private static final float RETURN_VELOCITY_FACTOR = 0.95F;
	private static final float RETURN_Y_STEP_FACTOR = 0.015F;
	private static final float RETURN_SPEED_PER_LOYALTY = 0.05F;
	private static final float RETURN_VELOCITY_DAMPING_X = 0.02F;
	private static final float RETURN_VELOCITY_DAMPING_Y = 0.2F;
	private static final float RETURN_VELOCITY_DAMPING_Z = 0.02F;
	private static final int GROUND_TIME_BEFORE_RETURN = 4;
	private static final double NON_PLAYER_PICKUP_DISTANCE = 1.0;
	private static final int MAX_LOYALTY_BYTE = 127;

	private static final TrackedData<Byte> LOYALTY =
		DataTracker.registerData(TridentEntity.class, TrackedDataHandlerRegistry.BYTE);
	private static final TrackedData<Boolean> ENCHANTED =
		DataTracker.registerData(TridentEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

	/** Нестандартное сопротивление воды — трезубец почти не замедляется в воде. */
	private static final float DRAG_IN_WATER = 0.99F;

	private boolean dealtDamage;
	public int returnTimer;

	public TridentEntity(EntityType<? extends TridentEntity> entityType, World world) {
		super(entityType, world);
	}

	public TridentEntity(World world, LivingEntity owner, ItemStack stack) {
		super(EntityType.TRIDENT, owner, world, stack, null);
		dataTracker.set(LOYALTY, getLoyaltyLevel(stack));
		dataTracker.set(ENCHANTED, stack.hasGlint());
	}

	public TridentEntity(World world, double x, double y, double z, ItemStack stack) {
		super(EntityType.TRIDENT, x, y, z, world, stack, stack);
		dataTracker.set(LOYALTY, getLoyaltyLevel(stack));
		dataTracker.set(ENCHANTED, stack.hasGlint());
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(LOYALTY, (byte) 0);
		builder.add(ENCHANTED, false);
	}

	@Override
	public void tick() {
		if (inGroundTime > GROUND_TIME_BEFORE_RETURN) {
			dealtDamage = true;
		}

		Entity ownerEntity = getOwner();
		int loyaltyLevel = dataTracker.get(LOYALTY);
		if (loyaltyLevel > 0 && (dealtDamage || isNoClip()) && ownerEntity != null) {
			if (!isOwnerAlive()) {
				if (getEntityWorld() instanceof ServerWorld serverWorld
					&& pickupType == PickupPermission.ALLOWED
				) {
					dropStack(serverWorld, asItemStack(), 0.1F);
				}

				discard();
			} else {
				// Не-игроки подбирают трезубец при достаточном сближении
				if (!(ownerEntity instanceof PlayerEntity)
					&& getEntityPos().distanceTo(ownerEntity.getEyePos()) < ownerEntity.getWidth() + NON_PLAYER_PICKUP_DISTANCE
				) {
					discard();
					return;
				}

				setNoClip(true);
				Vec3d toOwner = ownerEntity.getEyePos().subtract(getEntityPos());
				setPos(getX(), getY() + toOwner.y * RETURN_Y_STEP_FACTOR * loyaltyLevel, getZ());
				double returnSpeed = RETURN_SPEED_PER_LOYALTY * loyaltyLevel;
				setVelocity(
					getVelocity()
						.multiply(RETURN_VELOCITY_FACTOR)
						.add(toOwner.normalize().multiply(returnSpeed))
				);
				if (returnTimer == 0) {
					playSound(SoundEvents.ITEM_TRIDENT_RETURN, 10.0F, 1.0F);
				}

				returnTimer++;
			}
		}

		super.tick();
	}

	/**
	 * Проверяет, жив ли владелец и доступен ли он для возврата трезубца.
	 * Игроки-наблюдатели считаются недоступными.
	 */
	private boolean isOwnerAlive() {
		Entity ownerEntity = getOwner();
		if (ownerEntity == null || !ownerEntity.isAlive()) {
			return false;
		}

		return !(ownerEntity instanceof ServerPlayerEntity) || !ownerEntity.isSpectator();
	}

	public boolean isEnchanted() {
		return dataTracker.get(ENCHANTED);
	}

	@Override
	protected @Nullable EntityHitResult getEntityCollision(Vec3d currentPosition, Vec3d nextPosition) {
		return dealtDamage ? null : super.getEntityCollision(currentPosition, nextPosition);
	}

	@Override
	protected Collection<EntityHitResult> collectPiercingCollisions(Vec3d from, Vec3d to) {
		EntityHitResult entityHit = getEntityCollision(from, to);
		return entityHit != null ? List.of(entityHit) : List.of();
	}

	@Override
	protected void onEntityHit(EntityHitResult entityHitResult) {
		Entity target = entityHitResult.getEntity();
		float damage = BASE_DAMAGE;
		Entity ownerEntity = getOwner();
		DamageSource damageSource = getDamageSources().trident(this, ownerEntity == null ? this : ownerEntity);

		if (getEntityWorld() instanceof ServerWorld serverWorld) {
			damage = EnchantmentHelper.getDamage(serverWorld, getWeaponStack(), target, damageSource, damage);
		}

		dealtDamage = true;
		if (target.sidedDamage(damageSource, damage)) {
			if (target.getType() == EntityType.ENDERMAN) {
				return;
			}

			if (getEntityWorld() instanceof ServerWorld serverWorld) {
				EnchantmentHelper.onTargetDamaged(
					serverWorld,
					target,
					damageSource,
					getWeaponStack(),
					item -> kill(serverWorld)
				);
			}

			if (target instanceof LivingEntity livingTarget) {
				knockback(livingTarget, damageSource);
				onHit(livingTarget);
			}
		}

		deflect(ProjectileDeflection.SIMPLE, target, owner, false);
		setVelocity(getVelocity().multiply(RETURN_VELOCITY_DAMPING_X, RETURN_VELOCITY_DAMPING_Y, RETURN_VELOCITY_DAMPING_Z));
		playSound(SoundEvents.ITEM_TRIDENT_HIT, 1.0F, 1.0F);
	}

	@Override
	protected void onBlockHitEnchantmentEffects(
		ServerWorld world,
		BlockHitResult blockHit,
		ItemStack weaponStack
	) {
		Vec3d hitPos = blockHit.getBlockPos().clampToWithin(blockHit.getPos());
		EnchantmentHelper.onHitBlock(
			world,
			weaponStack,
			getOwner() instanceof LivingEntity livingOwner ? livingOwner : null,
			this,
			null,
			hitPos,
			world.getBlockState(blockHit.getBlockPos()),
			item -> kill(world)
		);
	}

	@Override
	public ItemStack getWeaponStack() {
		return getItemStack();
	}

	@Override
	protected boolean tryPickup(PlayerEntity player) {
		return super.tryPickup(player)
			|| (isNoClip() && isOwner(player) && player.getInventory().insertStack(asItemStack()));
	}

	@Override
	protected ItemStack getDefaultItemStack() {
		return new ItemStack(Items.TRIDENT);
	}

	@Override
	protected SoundEvent getHitSound() {
		return SoundEvents.ITEM_TRIDENT_HIT_GROUND;
	}

	@Override
	public void onPlayerCollision(PlayerEntity player) {
		if (isOwner(player) || getOwner() == null) {
			super.onPlayerCollision(player);
		}
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		dealtDamage = view.getBoolean("DealtDamage", false);
		dataTracker.set(LOYALTY, getLoyaltyLevel(getItemStack()));
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putBoolean("DealtDamage", dealtDamage);
	}

	/**
	 * Считывает уровень зачарования «Верность» из стека предмета.
	 * Возвращает 0 на клиенте (зачарования доступны только на сервере).
	 *
	 * @param stack стек трезубца
	 * @return уровень верности в диапазоне [0, 127]
	 */
	private byte getLoyaltyLevel(ItemStack stack) {
		return getEntityWorld() instanceof ServerWorld serverWorld
			? (byte) MathHelper.clamp(
				EnchantmentHelper.getTridentReturnAcceleration(serverWorld, stack, this),
				0,
				MAX_LOYALTY_BYTE
			)
			: 0;
	}

	/**
	 * Переопределяет логику устаревания: трезубец с верностью не исчезает со временем.
	 */
	@Override
	public void age() {
		int loyaltyLevel = dataTracker.get(LOYALTY);
		if (pickupType != PickupPermission.ALLOWED || loyaltyLevel <= 0) {
			super.age();
		}
	}

	@Override
	protected float getDragInWater() {
		return DRAG_IN_WATER;
	}

	@Override
	public boolean shouldRender(double cameraX, double cameraY, double cameraZ) {
		return true;
	}
}
