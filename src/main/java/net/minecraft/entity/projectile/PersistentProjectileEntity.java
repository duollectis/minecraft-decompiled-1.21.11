package net.minecraft.entity.projectile;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Unit;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * {@code PersistentProjectileEntity}.
 */
public abstract class PersistentProjectileEntity extends ProjectileEntity {

	private static final double DEFAULT_DAMAGE = 2.0;
	private static final int DEFAULT_SHAKE_TICKS = 7;
	private static final float DRAG_IN_WATER = 0.6F;
	private static final float DEFAULT_DRAG = 0.99F;
	private static final short DEFAULT_LIFE = 0;
	private static final byte DEFAULT_SHAKE = 0;
	private static final boolean DEFAULT_IN_GROUND = false;
	private static final boolean DEFAULT_CRITICAL = false;
	private static final byte DEFAULT_PIERCE_LEVEL = 0;
	private static final TrackedData<Byte>
			PROJECTILE_FLAGS =
			DataTracker.registerData(PersistentProjectileEntity.class, TrackedDataHandlerRegistry.BYTE);
	private static final TrackedData<Byte>
			PIERCE_LEVEL =
			DataTracker.registerData(PersistentProjectileEntity.class, TrackedDataHandlerRegistry.BYTE);
	private static final TrackedData<Boolean>
			IN_GROUND =
			DataTracker.registerData(PersistentProjectileEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final int CRITICAL_FLAG = 1;
	private static final int NO_CLIP_FLAG = 2;
	private @Nullable BlockState inBlockState;
	protected int inGroundTime;
	public PersistentProjectileEntity.PickupPermission
			pickupType =
			PersistentProjectileEntity.PickupPermission.DISALLOWED;
	public int shake = 0;
	private int life = 0;
	private double damage = 2.0;
	private SoundEvent sound = this.getHitSound();
	private @Nullable IntOpenHashSet piercedEntities;
	private @Nullable List<Entity> piercingKilledEntities;
	private ItemStack stack = this.getDefaultItemStack();
	private @Nullable ItemStack weapon = null;

	protected PersistentProjectileEntity(EntityType<? extends PersistentProjectileEntity> entityType, World world) {
		super(entityType, world);
	}

	protected PersistentProjectileEntity(
			EntityType<? extends PersistentProjectileEntity> type,
			double x,
			double y,
			double z,
			World world,
			ItemStack stack,
			@Nullable ItemStack weapon
	) {
		this(type, world);
		this.stack = stack.copy();
		this.copyComponentsFrom(stack);
		Unit unit = stack.remove(DataComponentTypes.INTANGIBLE_PROJECTILE);
		if (unit != null) {
			this.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;
		}

		this.setPosition(x, y, z);
		if (weapon != null && world instanceof ServerWorld serverWorld) {
			if (weapon.isEmpty()) {
				throw new IllegalArgumentException("Invalid weapon firing an arrow");
			}

			this.weapon = weapon.copy();
			int i = EnchantmentHelper.getProjectilePiercing(serverWorld, weapon, this.stack);
			if (i > 0) {
				this.setPierceLevel((byte) i);
			}
		}
	}

	protected PersistentProjectileEntity(
			EntityType<? extends PersistentProjectileEntity> type,
			LivingEntity owner,
			World world,
			ItemStack stack,
			@Nullable ItemStack shotFrom
	) {
		this(type, owner.getX(), owner.getEyeY() - 0.1F, owner.getZ(), world, stack, shotFrom);
		this.setOwner(owner);
	}

	public void setSound(SoundEvent sound) {
		this.sound = sound;
	}

	@Override
	public boolean shouldRender(double distance) {
		double d = this.getBoundingBox().getAverageSideLength() * 10.0;
		if (Double.isNaN(d)) {
			d = 1.0;
		}

		d *= 64.0 * getRenderDistanceMultiplier();
		return distance < d * d;
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(PROJECTILE_FLAGS, (byte) 0);
		builder.add(PIERCE_LEVEL, (byte) 0);
		builder.add(IN_GROUND, false);
	}

	@Override
	public void setVelocity(double x, double y, double z, float power, float uncertainty) {
		super.setVelocity(x, y, z, power, uncertainty);
		this.life = 0;
	}

	@Override
	public void setVelocityClient(Vec3d clientVelocity) {
		super.setVelocityClient(clientVelocity);
		this.life = 0;
		if (this.isInGround() && clientVelocity.lengthSquared() > 0.0) {
			this.setInGround(false);
		}
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		super.onTrackedDataSet(data);
		if (!this.firstUpdate && this.shake <= 0 && data.equals(IN_GROUND) && this.isInGround()) {
			this.shake = 7;
		}
	}

	@Override
	public void tick() {
		boolean bl = !this.isNoClip();
		Vec3d vec3d = this.getVelocity();
		BlockPos blockPos = this.getBlockPos();
		BlockState blockState = this.getEntityWorld().getBlockState(blockPos);
		if (!blockState.isAir() && bl) {
			VoxelShape voxelShape = blockState.getCollisionShape(this.getEntityWorld(), blockPos);
			if (!voxelShape.isEmpty()) {
				Vec3d vec3d2 = this.getEntityPos();

				for (Box box : voxelShape.getBoundingBoxes()) {
					if (box.offset(blockPos).contains(vec3d2)) {
						this.setVelocity(Vec3d.ZERO);
						this.setInGround(true);
						break;
					}
				}
			}
		}

		if (this.shake > 0) {
			this.shake--;
		}

		if (this.isTouchingWaterOrRain()) {
			this.extinguish();
		}

		if (this.isInGround() && bl) {
			if (!this.getEntityWorld().isClient()) {
				if (this.inBlockState != blockState && this.shouldFall()) {
					this.fall();
				}
				else {
					this.age();
				}
			}

			this.inGroundTime++;
			if (this.isAlive()) {
				this.tickBlockCollision();
			}

			if (!this.getEntityWorld().isClient()) {
				this.setOnFire(this.getFireTicks() > 0);
			}
		}
		else {
			this.inGroundTime = 0;
			Vec3d vec3d3 = this.getEntityPos();
			if (this.isTouchingWater()) {
				this.applyDrag(this.getDragInWater());
				this.spawnBubbleParticles(vec3d3);
			}

			if (this.isCritical()) {
				for (int i = 0; i < 4; i++) {
					this.getEntityWorld()
					    .addParticleClient(
							    ParticleTypes.CRIT,
							    vec3d3.x + vec3d.x * i / 4.0,
							    vec3d3.y + vec3d.y * i / 4.0,
							    vec3d3.z + vec3d.z * i / 4.0,
							    -vec3d.x,
							    -vec3d.y + 0.2,
							    -vec3d.z
					    );
				}
			}

			float f;
			if (!bl) {
				f = (float) (MathHelper.atan2(-vec3d.x, -vec3d.z) * 180.0F / (float) Math.PI);
			}
			else {
				f = (float) (MathHelper.atan2(vec3d.x, vec3d.z) * 180.0F / (float) Math.PI);
			}

			float g = (float) (MathHelper.atan2(vec3d.y, vec3d.horizontalLength()) * 180.0F / (float) Math.PI);
			this.setPitch(updateRotation(this.getPitch(), g));
			this.setYaw(updateRotation(this.getYaw(), f));
			this.tickLeftOwner();
			if (bl) {
				BlockHitResult blockHitResult = this.getEntityWorld()
				                                    .getCollisionsIncludingWorldBorder(
						                                    new RaycastContext(
								                                    vec3d3,
								                                    vec3d3.add(vec3d),
								                                    RaycastContext.ShapeType.COLLIDER,
								                                    RaycastContext.FluidHandling.NONE,
								                                    this
						                                    )
				                                    );
				this.applyCollision(blockHitResult);
			}
			else {
				this.setPosition(vec3d3.add(vec3d));
				this.tickBlockCollision();
			}

			if (!this.isTouchingWater()) {
				this.applyDrag(0.99F);
			}

			if (bl && !this.isInGround()) {
				this.applyGravity();
			}

			super.tick();
		}
	}

	private void applyCollision(BlockHitResult blockHitResult) {
		while (this.isAlive()) {
			Vec3d vec3d = this.getEntityPos();
			ArrayList<EntityHitResult>
					arrayList =
					new ArrayList<>(this.collectPiercingCollisions(vec3d, blockHitResult.getPos()));
			arrayList.sort(Comparator.comparingDouble(entityHitResultx -> vec3d.squaredDistanceTo(entityHitResultx
					.getEntity()
					.getEntityPos())));
			EntityHitResult entityHitResult = arrayList.isEmpty() ? null : arrayList.getFirst();
			Vec3d vec3d2 = Objects.requireNonNullElse(entityHitResult, blockHitResult).getPos();
			this.setPosition(vec3d2);
			this.tickBlockCollision(vec3d, vec3d2);
			if (this.portalManager != null && this.portalManager.isInPortal()) {
				this.tickPortalTeleportation();
			}

			if (arrayList.isEmpty()) {
				if (this.isAlive() && blockHitResult.getType() != HitResult.Type.MISS) {
					this.hitOrDeflect(blockHitResult);
					this.velocityDirty = true;
				}
				break;
			}
			else if (this.isAlive() && !this.noClip) {
				ProjectileDeflection projectileDeflection = this.hitOrDeflect(arrayList);
				this.velocityDirty = true;
				if (this.getPierceLevel() > 0 && projectileDeflection == ProjectileDeflection.NONE) {
					continue;
				}
				break;
			}
		}
	}

	private ProjectileDeflection hitOrDeflect(Collection<EntityHitResult> hitResults) {
		for (EntityHitResult entityHitResult : hitResults) {
			ProjectileDeflection projectileDeflection = this.hitOrDeflect(entityHitResult);
			if (!this.isAlive() || projectileDeflection != ProjectileDeflection.NONE) {
				return projectileDeflection;
			}
		}

		return ProjectileDeflection.NONE;
	}

	private void applyDrag(float drag) {
		Vec3d vec3d = this.getVelocity();
		this.setVelocity(vec3d.multiply(drag));
	}

	private void spawnBubbleParticles(Vec3d pos) {
		Vec3d vec3d = this.getVelocity();

		for (int i = 0; i < 4; i++) {
			float f = 0.25F;
			this.getEntityWorld()
			    .addParticleClient(
					    ParticleTypes.BUBBLE,
					    pos.x - vec3d.x * 0.25,
					    pos.y - vec3d.y * 0.25,
					    pos.z - vec3d.z * 0.25,
					    vec3d.x,
					    vec3d.y,
					    vec3d.z
			    );
		}
	}

	@Override
	protected double getGravity() {
		return 0.05;
	}

	private boolean shouldFall() {
		return this.isInGround() && this
				.getEntityWorld()
				.isSpaceEmpty(new Box(this.getEntityPos(), this.getEntityPos()).expand(0.06));
	}

	private void fall() {
		this.setInGround(false);
		Vec3d vec3d = this.getVelocity();
		this.setVelocity(vec3d.multiply(
				this.random.nextFloat() * 0.2F,
				this.random.nextFloat() * 0.2F,
				this.random.nextFloat() * 0.2F
		));
		this.life = 0;
	}

	protected boolean isInGround() {
		return this.dataTracker.get(IN_GROUND);
	}

	protected void setInGround(boolean inGround) {
		this.dataTracker.set(IN_GROUND, inGround);
	}

	@Override
	public boolean isPushedByFluids() {
		return !this.isInGround();
	}

	@Override
	public void move(MovementType type, Vec3d movement) {
		super.move(type, movement);
		if (type != MovementType.SELF && this.shouldFall()) {
			this.fall();
		}
	}

	/**
	 * Age.
	 */
	protected void age() {
		this.life++;
		if (this.life >= 1200) {
			this.discard();
		}
	}

	private void clearPiercingStatus() {
		if (this.piercingKilledEntities != null) {
			this.piercingKilledEntities.clear();
		}

		if (this.piercedEntities != null) {
			this.piercedEntities.clear();
		}
	}

	@Override
	public void onBroken(Item item) {
		this.weapon = null;
	}

	@Override
	public void onBubbleColumnSurfaceCollision(boolean drag, BlockPos pos) {
		if (!this.isInGround()) {
			super.onBubbleColumnSurfaceCollision(drag, pos);
		}
	}

	@Override
	public void onBubbleColumnCollision(boolean drag) {
		if (!this.isInGround()) {
			super.onBubbleColumnCollision(drag);
		}
	}

	@Override
	public void addVelocity(double deltaX, double deltaY, double deltaZ) {
		if (!this.isInGround()) {
			super.addVelocity(deltaX, deltaY, deltaZ);
		}
	}

	@Override
	protected void onEntityHit(EntityHitResult entityHitResult) {
		super.onEntityHit(entityHitResult);
		Entity entity = entityHitResult.getEntity();
		float f = (float) this.getVelocity().length();
		double d = this.damage;
		Entity entity2 = this.getOwner();
		DamageSource damageSource = this.getDamageSources().arrow(this, (Entity) (entity2 != null ? entity2 : this));
		if (this.getWeaponStack() != null && this.getEntityWorld() instanceof ServerWorld serverWorld) {
			d = EnchantmentHelper.getDamage(serverWorld, this.getWeaponStack(), entity, damageSource, (float) d);
		}

		int i = MathHelper.ceil(MathHelper.clamp(f * d, 0.0, 2.147483647E9));
		if (this.getPierceLevel() > 0) {
			if (this.piercedEntities == null) {
				this.piercedEntities = new IntOpenHashSet(5);
			}

			if (this.piercingKilledEntities == null) {
				this.piercingKilledEntities = Lists.newArrayListWithCapacity(5);
			}

			if (this.piercedEntities.size() >= this.getPierceLevel() + 1) {
				this.discard();
				return;
			}

			this.piercedEntities.add(entity.getId());
		}

		if (this.isCritical()) {
			long l = this.random.nextInt(i / 2 + 2);
			i = (int) Math.min(l + i, 2147483647L);
		}

		if (entity2 instanceof LivingEntity livingEntity) {
			livingEntity.onAttacking(entity);
		}

		boolean bl = entity.getType() == EntityType.ENDERMAN;
		int j = entity.getFireTicks();
		if (this.isOnFire() && !bl) {
			entity.setOnFireFor(5.0F);
		}

		if (entity.sidedDamage(damageSource, i)) {
			if (bl) {
				return;
			}

			if (entity instanceof LivingEntity livingEntity2) {
				if (!this.getEntityWorld().isClient() && this.getPierceLevel() <= 0) {
					livingEntity2.setStuckArrowCount(livingEntity2.getStuckArrowCount() + 1);
				}

				this.knockback(livingEntity2, damageSource);
				if (this.getEntityWorld() instanceof ServerWorld serverWorld2) {
					EnchantmentHelper.onTargetDamaged(serverWorld2, livingEntity2, damageSource, this.getWeaponStack());
				}

				this.onHit(livingEntity2);
				if (livingEntity2 instanceof PlayerEntity
						&& entity2 instanceof ServerPlayerEntity serverPlayerEntity
						&& !this.isSilent()
						&& livingEntity2 != serverPlayerEntity) {
					serverPlayerEntity.networkHandler.sendPacket(new GameStateChangeS2CPacket(
							GameStateChangeS2CPacket.PROJECTILE_HIT_PLAYER,
							0.0F
					));
				}

				if (!entity.isAlive() && this.piercingKilledEntities != null) {
					this.piercingKilledEntities.add(livingEntity2);
				}

				if (!this.getEntityWorld().isClient() && entity2 instanceof ServerPlayerEntity serverPlayerEntity) {
					if (this.piercingKilledEntities != null) {
						Criteria.KILLED_BY_ARROW.trigger(serverPlayerEntity, this.piercingKilledEntities, this.weapon);
					}
					else if (!entity.isAlive()) {
						Criteria.KILLED_BY_ARROW.trigger(serverPlayerEntity, List.of(entity), this.weapon);
					}
				}
			}

			this.playSound(this.sound, 1.0F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));
			if (this.getPierceLevel() <= 0) {
				this.discard();
			}
		}
		else {
			entity.setFireTicks(j);
			this.deflect(ProjectileDeflection.SIMPLE, entity, this.owner, false);
			this.setVelocity(this.getVelocity().multiply(0.2));
			if (this.getEntityWorld() instanceof ServerWorld serverWorld3
					&& this.getVelocity().lengthSquared() < 1.0E-7) {
				if (this.pickupType == PersistentProjectileEntity.PickupPermission.ALLOWED) {
					this.dropStack(serverWorld3, this.asItemStack(), 0.1F);
				}

				this.discard();
			}
		}
	}

	/**
	 * Knockback.
	 *
	 * @param target target
	 * @param source source
	 */
	protected void knockback(LivingEntity target, DamageSource source) {
		double d = this.weapon != null && this.getEntityWorld() instanceof ServerWorld serverWorld
		           ? EnchantmentHelper.modifyKnockback(serverWorld, this.weapon, target, source, 0.0F)
		           : 0.0F;
		if (d > 0.0) {
			double e = Math.max(0.0, 1.0 - target.getAttributeValue(EntityAttributes.KNOCKBACK_RESISTANCE));
			Vec3d vec3d = this.getVelocity().multiply(1.0, 0.0, 1.0).normalize().multiply(d * 0.6 * e);
			if (vec3d.lengthSquared() > 0.0) {
				target.addVelocity(vec3d.x, 0.1, vec3d.z);
			}
		}
	}

	@Override
	protected void onBlockHit(BlockHitResult blockHitResult) {
		this.inBlockState = this.getEntityWorld().getBlockState(blockHitResult.getBlockPos());
		super.onBlockHit(blockHitResult);
		ItemStack itemStack = this.getWeaponStack();
		if (this.getEntityWorld() instanceof ServerWorld serverWorld && itemStack != null) {
			this.onBlockHitEnchantmentEffects(serverWorld, blockHitResult, itemStack);
		}

		Vec3d vec3d = this.getVelocity();
		Vec3d vec3d2 = new Vec3d(Math.signum(vec3d.x), Math.signum(vec3d.y), Math.signum(vec3d.z));
		Vec3d vec3d3 = vec3d2.multiply(0.05F);
		this.setPosition(this.getEntityPos().subtract(vec3d3));
		this.setVelocity(Vec3d.ZERO);
		this.playSound(this.getSound(), 1.0F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));
		this.setInGround(true);
		this.shake = 7;
		this.setCritical(false);
		this.setPierceLevel((byte) 0);
		this.setSound(SoundEvents.ENTITY_ARROW_HIT);
		this.clearPiercingStatus();
	}

	protected void onBlockHitEnchantmentEffects(
			ServerWorld world,
			BlockHitResult blockHitResult,
			ItemStack weaponStack
	) {
		Vec3d vec3d = blockHitResult.getBlockPos().clampToWithin(blockHitResult.getPos());
		EnchantmentHelper.onHitBlock(
				world,
				weaponStack,
				this.getOwner() instanceof LivingEntity livingEntity ? livingEntity : null,
				this,
				null,
				vec3d,
				world.getBlockState(blockHitResult.getBlockPos()),
				item -> this.weapon = null
		);
	}

	@Override
	public @Nullable ItemStack getWeaponStack() {
		return this.weapon;
	}

	protected SoundEvent getHitSound() {
		return SoundEvents.ENTITY_ARROW_HIT;
	}

	protected final SoundEvent getSound() {
		return this.sound;
	}

	/**
	 * Обрабатывает событие hit.
	 *
	 * @param target target
	 */
	protected void onHit(LivingEntity target) {
	}

	protected @Nullable EntityHitResult getEntityCollision(Vec3d currentPosition, Vec3d nextPosition) {
		return ProjectileUtil.getEntityCollision(
				this.getEntityWorld(),
				this,
				currentPosition,
				nextPosition,
				this.getBoundingBox().stretch(this.getVelocity()).expand(1.0),
				this::canHit
		);
	}

	/**
	 * Collect piercing collisions.
	 *
	 * @param from from
	 * @param to to
	 *
	 * @return Collection — результат операции
	 */
	protected Collection<EntityHitResult> collectPiercingCollisions(Vec3d from, Vec3d to) {
		return ProjectileUtil.collectPiercingCollisions(
				this.getEntityWorld(),
				this,
				from,
				to,
				this.getBoundingBox().stretch(this.getVelocity()).expand(1.0),
				this::canHit,
				false
		);
	}

	@Override
	protected boolean canHit(Entity entity) {
		return entity instanceof PlayerEntity && this.getOwner() instanceof PlayerEntity playerEntity
				       && !playerEntity.shouldDamagePlayer((PlayerEntity) entity)
		       ? false
		       : super.canHit(entity) && (this.piercedEntities == null
		                                  || !this.piercedEntities.contains(entity.getId())
		       );
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putShort("life", (short) this.life);
		view.putNullable("inBlockState", BlockState.CODEC, this.inBlockState);
		view.putByte("shake", (byte) this.shake);
		view.putBoolean("inGround", this.isInGround());
		view.put("pickup", PersistentProjectileEntity.PickupPermission.CODEC, this.pickupType);
		view.putDouble("damage", this.damage);
		view.putBoolean("crit", this.isCritical());
		view.putByte("PierceLevel", this.getPierceLevel());
		view.put("SoundEvent", Registries.SOUND_EVENT.getCodec(), this.sound);
		view.put("item", ItemStack.CODEC, this.stack);
		view.putNullable("weapon", ItemStack.CODEC, this.weapon);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		this.life = view.getShort("life", (short) 0);
		this.inBlockState = view.<BlockState>read("inBlockState", BlockState.CODEC).orElse(null);
		this.shake = view.getByte("shake", (byte) 0) & 255;
		this.setInGround(view.getBoolean("inGround", false));
		this.damage = view.getDouble("damage", 2.0);
		this.pickupType =
				view
						.<PersistentProjectileEntity.PickupPermission>read(
								"pickup",
								PersistentProjectileEntity.PickupPermission.CODEC
						)
						.orElse(PersistentProjectileEntity.PickupPermission.DISALLOWED);
		this.setCritical(view.getBoolean("crit", false));
		this.setPierceLevel(view.getByte("PierceLevel", (byte) 0));
		this.sound = view.<SoundEvent>read("SoundEvent", Registries.SOUND_EVENT.getCodec()).orElse(this.getHitSound());
		this.setStack(view.<ItemStack>read("item", ItemStack.CODEC).orElse(this.getDefaultItemStack()));
		this.weapon = view.<ItemStack>read("weapon", ItemStack.CODEC).orElse(null);
	}

	@Override
	public void setOwner(@Nullable Entity owner) {
		super.setOwner(owner);

		this.pickupType = switch (owner) {
			case PlayerEntity playerEntity when this.pickupType
					== PersistentProjectileEntity.PickupPermission.DISALLOWED ->
					PersistentProjectileEntity.PickupPermission.ALLOWED;
			case OminousItemSpawnerEntity ominousItemSpawnerEntity ->
					PersistentProjectileEntity.PickupPermission.DISALLOWED;
			case null, default -> this.pickupType;
		};
	}

	@Override
	public void onPlayerCollision(PlayerEntity player) {
		if (!this.getEntityWorld().isClient() && (this.isInGround() || this.isNoClip()) && this.shake <= 0) {
			if (this.tryPickup(player)) {
				player.sendPickup(this, 1);
				this.discard();
			}
		}
	}

	/**
	 * Try pickup.
	 *
	 * @param player player
	 *
	 * @return boolean — результат операции
	 */
	protected boolean tryPickup(PlayerEntity player) {
		return switch (this.pickupType) {
			case DISALLOWED -> false;
			case ALLOWED -> player.getInventory().insertStack(this.asItemStack());
			case CREATIVE_ONLY -> player.isInCreativeMode();
		};
	}

	/**
	 * As item stack.
	 *
	 * @return ItemStack — результат операции
	 */
	protected ItemStack asItemStack() {
		return this.stack.copy();
	}

	protected abstract ItemStack getDefaultItemStack();

	@Override
	protected Entity.MoveEffect getMoveEffect() {
		return Entity.MoveEffect.NONE;
	}

	public ItemStack getItemStack() {
		return this.stack;
	}

	public void setDamage(double damage) {
		this.damage = damage;
	}

	@Override
	public boolean isAttackable() {
		return this.getType().isIn(EntityTypeTags.REDIRECTABLE_PROJECTILE);
	}

	public void setCritical(boolean critical) {
		this.setProjectileFlag(1, critical);
	}

	private void setPierceLevel(byte level) {
		this.dataTracker.set(PIERCE_LEVEL, level);
	}

	private void setProjectileFlag(int index, boolean flag) {
		byte b = this.dataTracker.get(PROJECTILE_FLAGS);
		if (flag) {
			this.dataTracker.set(PROJECTILE_FLAGS, (byte) (b | index));
		}
		else {
			this.dataTracker.set(PROJECTILE_FLAGS, (byte) (b & ~index));
		}
	}

	protected void setStack(ItemStack stack) {
		if (!stack.isEmpty()) {
			this.stack = stack;
		}
		else {
			this.stack = this.getDefaultItemStack();
		}
	}

	public boolean isCritical() {
		byte b = this.dataTracker.get(PROJECTILE_FLAGS);
		return (b & 1) != 0;
	}

	public byte getPierceLevel() {
		return this.dataTracker.get(PIERCE_LEVEL);
	}

	/**
	 * Применяет damage modifier.
	 *
	 * @param damageModifier damage modifier
	 */
	public void applyDamageModifier(float damageModifier) {
		this.setDamage(damageModifier * 2.0F + this.random.nextTriangular(
				this.getEntityWorld().getDifficulty().getId() * 0.11,
				0.57425
		));
	}

	protected float getDragInWater() {
		return 0.6F;
	}

	public void setNoClip(boolean noClip) {
		this.noClip = noClip;
		this.setProjectileFlag(2, noClip);
	}

	public boolean isNoClip() {
		return !this.getEntityWorld().isClient() ? this.noClip : (this.dataTracker.get(PROJECTILE_FLAGS) & 2) != 0;
	}

	@Override
	public boolean canHit() {
		return super.canHit() && !this.isInGround();
	}

	@Override
	public @Nullable StackReference getStackReference(int slot) {
		return slot == 0 ? StackReference.of(this::getItemStack, this::setStack) : super.getStackReference(slot);
	}

	@Override
	protected boolean deflectsAgainstWorldBorder() {
		return true;
	}

	/**
	 * {@code PickupPermission}.
	 */
	public static enum PickupPermission {
		DISALLOWED,
		ALLOWED,
		CREATIVE_ONLY;

		public static final Codec<PersistentProjectileEntity.PickupPermission> CODEC = Codec.BYTE
				.xmap(
						PersistentProjectileEntity.PickupPermission::fromOrdinal,
						pickupPermission -> (byte) pickupPermission.ordinal()
				);

		public static PersistentProjectileEntity.PickupPermission fromOrdinal(int ordinal) {
			if (ordinal < 0 || ordinal > values().length) {
				ordinal = 0;
			}

			return values()[ordinal];
		}
	}
}
