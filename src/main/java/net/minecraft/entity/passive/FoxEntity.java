package net.minecraft.entity.passive;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.*;
import net.minecraft.component.ComponentType;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.function.ValueLists;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Лиса — хитрое животное, которое охотится на кур, кроликов и рыбу,
 * подбирает предметы с земли и может доверять конкретным игрокам.
 * Красная лиса предпочитает кур и черепах, снежная — рыбу.
 */
public class FoxEntity extends AnimalEntity {

	private static final TrackedData<Integer> VARIANT = DataTracker.registerData(
			FoxEntity.class,
			TrackedDataHandlerRegistry.INTEGER
	);
	private static final TrackedData<Byte> FOX_FLAGS = DataTracker.registerData(
			FoxEntity.class,
			TrackedDataHandlerRegistry.BYTE
	);
	private static final int SITTING_FLAG = 1;
	public static final int CROUCHING_FLAG = 4;
	public static final int ROLLING_HEAD_FLAG = 8;
	public static final int CHASING_FLAG = 16;
	private static final int SLEEPING_FLAG = 32;
	private static final int WALKING_FLAG = 64;
	private static final int AGGRESSIVE_FLAG = 128;
	private static final TrackedData<Optional<LazyEntityReference<LivingEntity>>> OWNER = DataTracker.registerData(
			FoxEntity.class,
			TrackedDataHandlerRegistry.LAZY_ENTITY_REFERENCE
	);
	private static final TrackedData<Optional<LazyEntityReference<LivingEntity>>> OTHER_TRUSTED = DataTracker.registerData(
			FoxEntity.class,
			TrackedDataHandlerRegistry.LAZY_ENTITY_REFERENCE
	);
	static final Predicate<ItemEntity> PICKABLE_DROP_FILTER = item -> !item.cannotPickup() && item.isAlive();
	private static final Predicate<Entity> JUST_ATTACKED_SOMETHING_FILTER = entity ->
			entity instanceof LivingEntity livingEntity
					&& livingEntity.getAttacking() != null
					&& livingEntity.getLastAttackTime() < livingEntity.age + 600;
	static final Predicate<Entity> CHICKEN_AND_RABBIT_FILTER =
			entity -> entity instanceof ChickenEntity || entity instanceof RabbitEntity;
	private static final Predicate<Entity> NOTICEABLE_PLAYER_FILTER =
			entity -> !entity.isSneaky() && EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(entity);
	private static final int EATING_DURATION = 600;
	private static final EntityDimensions BABY_BASE_DIMENSIONS = EntityType.FOX
			.getDimensions()
			.scaled(0.5F)
			.withEyeHeight(0.2975F);
	@SuppressWarnings("unchecked")
	private static final Codec<List<LazyEntityReference<LivingEntity>>> TRUSTED_ENTITIES_CODEC =
			((Codec<LazyEntityReference<LivingEntity>>) (Codec<?>) LazyEntityReference.createCodec()).listOf();
	private Goal followChickenAndRabbitGoal;
	private Goal followBabyTurtleGoal;
	private Goal followFishGoal;
	private float headRollProgress;
	private float lastHeadRollProgress;
	float extraRollingHeight;
	float lastExtraRollingHeight;
	private int eatingTime;

	public FoxEntity(EntityType<? extends FoxEntity> entityType, World world) {
		super(entityType, world);
		lookControl = new FoxEntity.FoxLookControl();
		moveControl = new FoxEntity.FoxMoveControl();
		setPathfindingPenalty(PathNodeType.DANGER_OTHER, 0.0F);
		setPathfindingPenalty(PathNodeType.DAMAGE_OTHER, 0.0F);
		setCanPickUpLoot(true);
		getNavigation().setMaxFollowRange(32.0F);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(OWNER, Optional.empty());
		builder.add(OTHER_TRUSTED, Optional.empty());
		builder.add(VARIANT, FoxEntity.Variant.DEFAULT.getIndex());
		builder.add(FOX_FLAGS, (byte) 0);
	}

	@Override
	protected void initGoals() {
		followChickenAndRabbitGoal = new ActiveTargetGoal<>(
				this,
				AnimalEntity.class,
				10,
				false,
				false,
				(entity, world) -> entity instanceof ChickenEntity || entity instanceof RabbitEntity
		);
		followBabyTurtleGoal = new ActiveTargetGoal<>(
				this,
				TurtleEntity.class,
				10,
				false,
				false,
				TurtleEntity.BABY_TURTLE_ON_LAND_FILTER
		);
		followFishGoal = new ActiveTargetGoal<>(
				this,
				FishEntity.class,
				20,
				false,
				false,
				(entity, world) -> entity instanceof SchoolingFishEntity
		);
		goalSelector.add(0, new FoxEntity.FoxSwimGoal());
		goalSelector.add(0, new PowderSnowJumpGoal(this, getEntityWorld()));
		goalSelector.add(1, new FoxEntity.StopWanderingGoal());
		goalSelector.add(2, new FoxEntity.EscapeWhenNotAggressiveGoal(2.2));
		goalSelector.add(3, new FoxEntity.MateGoal(1.0));
		goalSelector.add(
				4,
				new FleeEntityGoal<>(
						this,
						PlayerEntity.class,
						16.0F,
						1.6,
						1.4,
						entity -> NOTICEABLE_PLAYER_FILTER.test(entity) && !canTrust(entity) && !isAggressive()
				)
		);
		goalSelector.add(
				4,
				new FleeEntityGoal<>(
						this,
						WolfEntity.class,
						8.0F,
						1.6,
						1.4,
						entity -> !((WolfEntity) entity).isTamed() && !isAggressive()
				)
		);
		goalSelector.add(
				4,
				new FleeEntityGoal<>(this, PolarBearEntity.class, 8.0F, 1.6, 1.4, entity -> !isAggressive())
		);
		goalSelector.add(5, new FoxEntity.MoveToHuntGoal());
		goalSelector.add(6, new FoxEntity.JumpChasingGoal());
		goalSelector.add(6, new FoxEntity.AvoidDaylightGoal(1.25));
		goalSelector.add(7, new FoxEntity.AttackGoal(1.2F, true));
		goalSelector.add(7, new FoxEntity.DelayedCalmDownGoal());
		goalSelector.add(8, new FoxEntity.FollowParentGoal(this, 1.25));
		goalSelector.add(9, new FoxEntity.GoToVillageGoal(SLEEPING_FLAG, 200));
		goalSelector.add(10, new FoxEntity.EatBerriesGoal(1.2F, 12, 1));
		goalSelector.add(10, new PounceAtTargetGoal(this, 0.4F));
		goalSelector.add(11, new WanderAroundFarGoal(this, 1.0));
		goalSelector.add(11, new FoxEntity.PickupItemGoal());
		goalSelector.add(12, new FoxEntity.LookAtEntityGoal(this, PlayerEntity.class, 24.0F));
		goalSelector.add(13, new FoxEntity.SitDownAndLookAroundGoal());
		targetSelector.add(
				3,
				new FoxEntity.DefendFriendGoal(
						LivingEntity.class,
						false,
						false,
						(entity, world) -> JUST_ATTACKED_SOMETHING_FILTER.test(entity) && !canTrust(entity)
				)
		);
	}

	@Override
	public void tickMovement() {
		if (!getEntityWorld().isClient() && isAlive() && canActVoluntarily()) {
			eatingTime++;
			ItemStack heldItem = getEquippedStack(EquipmentSlot.MAINHAND);

			if (canEat(heldItem)) {
				if (eatingTime > EATING_DURATION) {
					ItemStack result = heldItem.finishUsing(getEntityWorld(), this);

					if (!result.isEmpty()) {
						equipStack(EquipmentSlot.MAINHAND, result);
					}

					eatingTime = 0;
				} else if (eatingTime > 560 && random.nextFloat() < 0.1F) {
					playEatSound();
					getEntityWorld().sendEntityStatus(this, (byte) 45);
				}
			}

			LivingEntity target = getTarget();

			if (target == null || !target.isAlive()) {
				setCrouching(false);
				setRollingHead(false);
			}
		}

		if (isSleeping() || isImmobile()) {
			jumping = false;
			sidewaysSpeed = 0.0F;
			forwardSpeed = 0.0F;
		}

		super.tickMovement();

		if (isAggressive() && random.nextFloat() < 0.05F) {
			playSound(SoundEvents.ENTITY_FOX_AGGRO, 1.0F, 1.0F);
		}
	}

	@Override
	protected boolean isImmobile() {
		return isDead();
	}

	private boolean canEat(ItemStack stack) {
		return isConsumableFood(stack) && getTarget() == null && isOnGround() && !isSleeping();
	}

	private boolean isConsumableFood(ItemStack stack) {
		return stack.contains(DataComponentTypes.FOOD) && stack.contains(DataComponentTypes.CONSUMABLE);
	}

	@Override
	protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
		if (random.nextFloat() >= 0.2F) {
			return;
		}

		float roll = random.nextFloat();
		ItemStack itemStack;

		if (roll < 0.05F) {
			itemStack = new ItemStack(Items.EMERALD);
		} else if (roll < 0.2F) {
			itemStack = new ItemStack(Items.EGG);
		} else if (roll < 0.4F) {
			itemStack = random.nextBoolean() ? new ItemStack(Items.RABBIT_FOOT) : new ItemStack(Items.RABBIT_HIDE);
		} else if (roll < 0.6F) {
			itemStack = new ItemStack(Items.WHEAT);
		} else if (roll < 0.8F) {
			itemStack = new ItemStack(Items.LEATHER);
		} else {
			itemStack = new ItemStack(Items.FEATHER);
		}

		equipStack(EquipmentSlot.MAINHAND, itemStack);
	}

	@Override
	public void handleStatus(byte status) {
		if (status != 45) {
			super.handleStatus(status);
			return;
		}

		ItemStack itemStack = getEquippedStack(EquipmentSlot.MAINHAND);

		if (itemStack.isEmpty()) {
			return;
		}

		for (int i = 0; i < 8; i++) {
			Vec3d vec3d = new Vec3d(
					(random.nextFloat() - 0.5) * 0.1,
					random.nextFloat() * 0.1 + 0.1,
					0.0
			)
					.rotateX(-getPitch() * (float) (Math.PI / 180.0))
					.rotateY(-getYaw() * (float) (Math.PI / 180.0));
			getEntityWorld().addParticleClient(
					new ItemStackParticleEffect(ParticleTypes.ITEM, itemStack),
					getX() + getRotationVector().x / 2.0,
					getY(),
					getZ() + getRotationVector().z / 2.0,
					vec3d.x,
					vec3d.y + 0.05,
					vec3d.z
			);
		}
	}

	public static DefaultAttributeContainer.Builder createFoxAttributes() {
		return AnimalEntity.createAnimalAttributes()
				.add(EntityAttributes.MOVEMENT_SPEED, 0.3F)
				.add(EntityAttributes.MAX_HEALTH, 10.0)
				.add(EntityAttributes.ATTACK_DAMAGE, 2.0)
				.add(EntityAttributes.SAFE_FALL_DISTANCE, 5.0)
				.add(EntityAttributes.FOLLOW_RANGE, 32.0);
	}

	@Override
	public @Nullable FoxEntity createChild(ServerWorld serverWorld, PassiveEntity passiveEntity) {
		FoxEntity child = EntityType.FOX.create(serverWorld, SpawnReason.BREEDING);

		if (child != null) {
			child.setVariant(random.nextBoolean() ? getVariant() : ((FoxEntity) passiveEntity).getVariant());
		}

		return child;
	}

	public static boolean canSpawn(
			EntityType<FoxEntity> type,
			WorldAccess world,
			SpawnReason spawnReason,
			BlockPos pos,
			Random random
	) {
		return world.getBlockState(pos.down()).isIn(BlockTags.FOXES_SPAWNABLE_ON)
				&& isLightLevelValidForNaturalSpawn(world, pos);
	}

	/**
	 * При спавне определяет вариант лисы по биому, инициализирует снаряжение
	 * и добавляет цели охоты в зависимости от варианта (красная vs снежная).
	 */
	@Override
	public @Nullable EntityData initialize(
			ServerWorldAccess world,
			LocalDifficulty difficulty,
			SpawnReason spawnReason,
			@Nullable EntityData entityData
	) {
		RegistryEntry<Biome> biome = world.getBiome(getBlockPos());
		FoxEntity.Variant variant = FoxEntity.Variant.fromBiome(biome);
		boolean isBaby = false;

		if (entityData instanceof FoxEntity.FoxData foxData) {
			variant = foxData.type;

			if (foxData.getSpawnedCount() >= 2) {
				isBaby = true;
			}
		} else {
			entityData = new FoxEntity.FoxData(variant);
		}

		setVariant(variant);

		if (isBaby) {
			setBreedingAge(-24000);
		}

		if (world instanceof ServerWorld) {
			addTypeSpecificGoals();
		}

		initEquipment(world.getRandom(), difficulty);

		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	private void addTypeSpecificGoals() {
		if (getVariant() == FoxEntity.Variant.RED) {
			targetSelector.add(4, followChickenAndRabbitGoal);
			targetSelector.add(4, followBabyTurtleGoal);
			targetSelector.add(6, followFishGoal);
		} else {
			targetSelector.add(4, followFishGoal);
			targetSelector.add(6, followChickenAndRabbitGoal);
			targetSelector.add(6, followBabyTurtleGoal);
		}
	}

	@Override
	protected void playEatSound() {
		playSound(SoundEvents.ENTITY_FOX_EAT, 1.0F, 1.0F);
	}

	@Override
	public EntityDimensions getBaseDimensions(EntityPose pose) {
		return isBaby() ? BABY_BASE_DIMENSIONS : super.getBaseDimensions(pose);
	}

	public FoxEntity.Variant getVariant() {
		return FoxEntity.Variant.byIndex(dataTracker.get(VARIANT));
	}

	private void setVariant(FoxEntity.Variant variant) {
		dataTracker.set(VARIANT, variant.getIndex());
	}

	@Override
	public <T> @Nullable T get(ComponentType<? extends T> type) {
		return type == DataComponentTypes.FOX_VARIANT
				? castComponentValue((ComponentType<T>) type, getVariant())
				: super.get(type);
	}

	@Override
	protected void copyComponentsFrom(ComponentsAccess from) {
		copyComponentFrom(from, DataComponentTypes.FOX_VARIANT);
		super.copyComponentsFrom(from);
	}

	@Override
	protected <T> boolean setApplicableComponent(ComponentType<T> type, T value) {
		if (type == DataComponentTypes.FOX_VARIANT) {
			setVariant(castComponentValue(DataComponentTypes.FOX_VARIANT, value));
			return true;
		}

		return super.setApplicableComponent(type, value);
	}

	Stream<LazyEntityReference<LivingEntity>> getTrustedEntities() {
		return Stream.concat(dataTracker.get(OWNER).stream(), dataTracker.get(OTHER_TRUSTED).stream());
	}

	void trust(LivingEntity entity) {
		trust(LazyEntityReference.of(entity));
	}

	private void trust(LazyEntityReference<LivingEntity> entity) {
		if (dataTracker.get(OWNER).isPresent()) {
			dataTracker.set(OTHER_TRUSTED, Optional.of(entity));
		} else {
			dataTracker.set(OWNER, Optional.of(entity));
		}
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.put("Trusted", TRUSTED_ENTITIES_CODEC, getTrustedEntities().toList());
		view.putBoolean("Sleeping", isSleeping());
		view.put("Type", FoxEntity.Variant.CODEC, getVariant());
		view.putBoolean("Sitting", isSitting());
		view.putBoolean("Crouching", isInSneakingPose());
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		clearTrusted();
		view
				.<List<LazyEntityReference<LivingEntity>>>read("Trusted", TRUSTED_ENTITIES_CODEC)
				.orElse(List.of())
				.forEach(this::trust);
		setSleeping(view.getBoolean("Sleeping", false));
		setVariant(view.<FoxEntity.Variant>read("Type", FoxEntity.Variant.CODEC).orElse(FoxEntity.Variant.DEFAULT));
		setSitting(view.getBoolean("Sitting", false));
		setCrouching(view.getBoolean("Crouching", false));

		if (getEntityWorld() instanceof ServerWorld) {
			addTypeSpecificGoals();
		}
	}

	private void clearTrusted() {
		dataTracker.set(OWNER, Optional.empty());
		dataTracker.set(OTHER_TRUSTED, Optional.empty());
	}

	public boolean isSitting() {
		return getFoxFlag(1);
	}

	public void setSitting(boolean sitting) {
		setFoxFlag(1, sitting);
	}

	public boolean isWalking() {
		return getFoxFlag(WALKING_FLAG);
	}

	void setWalking(boolean walking) {
		setFoxFlag(WALKING_FLAG, walking);
	}

	boolean isAggressive() {
		return getFoxFlag(AGGRESSIVE_FLAG);
	}

	void setAggressive(boolean aggressive) {
		setFoxFlag(AGGRESSIVE_FLAG, aggressive);
	}

	@Override
	public boolean isSleeping() {
		return getFoxFlag(SLEEPING_FLAG);
	}

	void setSleeping(boolean sleeping) {
		setFoxFlag(SLEEPING_FLAG, sleeping);
	}

	private void setFoxFlag(int mask, boolean value) {
		if (value) {
			dataTracker.set(FOX_FLAGS, (byte) (dataTracker.get(FOX_FLAGS) | mask));
		} else {
			dataTracker.set(FOX_FLAGS, (byte) (dataTracker.get(FOX_FLAGS) & ~mask));
		}
	}

	private boolean getFoxFlag(int bitmask) {
		return (dataTracker.get(FOX_FLAGS) & bitmask) != 0;
	}

	@Override
	protected boolean canDispenserEquipSlot(EquipmentSlot slot) {
		return slot == EquipmentSlot.MAINHAND && canPickUpLoot();
	}

	@Override
	public boolean canPickupItem(ItemStack stack) {
		ItemStack heldItem = getEquippedStack(EquipmentSlot.MAINHAND);
		return heldItem.isEmpty() || eatingTime > 0 && isConsumableFood(stack) && !isConsumableFood(heldItem);
	}

	private void spit(ItemStack stack) {
		if (stack.isEmpty() || getEntityWorld().isClient()) {
			return;
		}

		ItemEntity itemEntity = new ItemEntity(
				getEntityWorld(),
				getX() + getRotationVector().x,
				getY() + 1.0,
				getZ() + getRotationVector().z,
				stack
		);
		itemEntity.setPickupDelay(EATING_DURATION);
		itemEntity.setThrower(this);
		playSound(SoundEvents.ENTITY_FOX_SPIT, 1.0F, 1.0F);
		getEntityWorld().spawnEntity(itemEntity);
	}

	private void dropItem(ItemStack stack) {
		ItemEntity itemEntity = new ItemEntity(getEntityWorld(), getX(), getY(), getZ(), stack);
		getEntityWorld().spawnEntity(itemEntity);
	}

	@Override
	protected void loot(ServerWorld world, ItemEntity itemEntity) {
		ItemStack itemStack = itemEntity.getStack();

		if (!canPickupItem(itemStack)) {
			return;
		}

		int count = itemStack.getCount();

		if (count > 1) {
			dropItem(itemStack.split(count - 1));
		}

		spit(getEquippedStack(EquipmentSlot.MAINHAND));
		triggerItemPickedUpByEntityCriteria(itemEntity);
		equipStack(EquipmentSlot.MAINHAND, itemStack.split(1));
		setDropGuaranteed(EquipmentSlot.MAINHAND);
		sendPickup(itemEntity, itemStack.getCount());
		itemEntity.discard();
		eatingTime = 0;
	}

	@Override
	public void tick() {
		super.tick();

		if (canActVoluntarily()) {
			boolean touchingWater = isTouchingWater();

			if (touchingWater || getTarget() != null || getEntityWorld().isThundering()) {
				stopSleeping();
			}

			if (touchingWater || isSleeping()) {
				setSitting(false);
			}

			if (isWalking() && getEntityWorld().random.nextFloat() < 0.2F) {
				BlockPos blockPos = getBlockPos();
				BlockState blockState = getEntityWorld().getBlockState(blockPos);
				getEntityWorld().syncWorldEvent(2001, blockPos, Block.getRawIdFromState(blockState));
			}
		}

		lastHeadRollProgress = headRollProgress;

		if (isRollingHead()) {
			headRollProgress = headRollProgress + (1.0F - headRollProgress) * 0.4F;
		} else {
			headRollProgress = headRollProgress + (0.0F - headRollProgress) * 0.4F;
		}

		lastExtraRollingHeight = extraRollingHeight;

		if (isInSneakingPose()) {
			extraRollingHeight += 0.2F;

			if (extraRollingHeight > 3.0F) {
				extraRollingHeight = 3.0F;
			}
		} else {
			extraRollingHeight = 0.0F;
		}
	}

	@Override
	public boolean isBreedingItem(ItemStack stack) {
		return stack.isIn(ItemTags.FOX_FOOD);
	}

	@Override
	protected void onPlayerSpawnedChild(PlayerEntity player, MobEntity child) {
		((FoxEntity) child).trust(player);
	}

	public boolean isChasing() {
		return getFoxFlag(CHASING_FLAG);
	}

	public void setChasing(boolean chasing) {
		setFoxFlag(CHASING_FLAG, chasing);
	}

	public boolean isFullyCrouched() {
		return extraRollingHeight == 3.0F;
	}

	public void setCrouching(boolean crouching) {
		setFoxFlag(CROUCHING_FLAG, crouching);
	}

	@Override
	public boolean isInSneakingPose() {
		return getFoxFlag(CROUCHING_FLAG);
	}

	public void setRollingHead(boolean rollingHead) {
		setFoxFlag(ROLLING_HEAD_FLAG, rollingHead);
	}

	public boolean isRollingHead() {
		return getFoxFlag(ROLLING_HEAD_FLAG);
	}

	public float getHeadRoll(float tickProgress) {
		return MathHelper.lerp(tickProgress, lastHeadRollProgress, headRollProgress) * 0.11F * (float) Math.PI;
	}

	public float getBodyRotationHeightOffset(float tickProgress) {
		return MathHelper.lerp(tickProgress, lastExtraRollingHeight, extraRollingHeight);
	}

	@Override
	public void setTarget(@Nullable LivingEntity target) {
		if (isAggressive() && target == null) {
			setAggressive(false);
		}

		super.setTarget(target);
	}

	void stopSleeping() {
		setSleeping(false);
	}

	void stopActions() {
		setRollingHead(false);
		setCrouching(false);
		setSitting(false);
		setSleeping(false);
		setAggressive(false);
		setWalking(false);
	}

	boolean wantsToPickupItem() {
		return !isSleeping() && !isSitting() && !isWalking();
	}

	@Override
	public void playAmbientSound() {
		SoundEvent soundEvent = getAmbientSound();
		if (soundEvent == SoundEvents.ENTITY_FOX_SCREECH) {
			playSound(soundEvent, 2.0F, getSoundPitch());
		} else {
			super.playAmbientSound();
		}
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		if (isSleeping()) {
			return SoundEvents.ENTITY_FOX_SLEEP;
		}

		if (!getEntityWorld().isDay() && random.nextFloat() < 0.1F) {
			List<PlayerEntity> players = getEntityWorld()
					.getEntitiesByClass(
							PlayerEntity.class,
							getBoundingBox().expand(16.0, 16.0, 16.0),
							EntityPredicates.EXCEPT_SPECTATOR
					);
			if (players.isEmpty()) {
				return SoundEvents.ENTITY_FOX_SCREECH;
			}
		}

		return SoundEvents.ENTITY_FOX_AMBIENT;
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_FOX_HURT;
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_FOX_DEATH;
	}

	boolean canTrust(LivingEntity entity) {
		return getTrustedEntities().anyMatch(trusted -> trusted.uuidEquals(entity));
	}

	@Override
	protected void drop(ServerWorld world, DamageSource damageSource) {
		ItemStack held = getEquippedStack(EquipmentSlot.MAINHAND);
		if (!held.isEmpty()) {
			dropStack(world, held);
			equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		}

		super.drop(world, damageSource);
	}

	/**
	 * Проверяет, может ли лиса совершить прыжок-атаку на цель.
	 * Алгоритм трассирует 6 шагов по горизонтали и 3 блока по вертикали,
	 * убеждаясь, что путь не заблокирован непроходимыми блоками.
	 */
	public static boolean canJumpChase(FoxEntity fox, LivingEntity chasedEntity) {
		double deltaZ = chasedEntity.getZ() - fox.getZ();
		double deltaX = chasedEntity.getX() - fox.getX();
		double slope = deltaZ / deltaX;
		int steps = 6;

		for (int step = 0; step < steps; step++) {
			double offsetZ = slope == 0.0 ? 0.0 : deltaZ * (step / 6.0F);
			double offsetX = slope == 0.0 ? deltaX * (step / 6.0F) : offsetZ / slope;

			for (int height = 1; height < 4; height++) {
				if (!fox
						.getEntityWorld()
						.getBlockState(BlockPos.ofFloored(fox.getX() + offsetX, fox.getY() + height, fox.getZ() + offsetZ))
						.isReplaceable()) {
					return false;
				}
			}
		}

		return true;
	}

	@Override
	public Vec3d getLeashOffset() {
		return new Vec3d(0.0, 0.55F * getStandingEyeHeight(), getWidth() * 0.4F);
	}

	class AttackGoal extends MeleeAttackGoal {

		public AttackGoal(final double speed, final boolean pauseWhenIdle) {
			super(FoxEntity.this, speed, pauseWhenIdle);
		}

		@Override
		protected void attack(LivingEntity target) {
			if (canAttack(target)) {
				resetCooldown();
				mob.tryAttack(getServerWorld(mob), target);
				FoxEntity.this.playSound(SoundEvents.ENTITY_FOX_BITE, 1.0F, 1.0F);
			}
		}

		@Override
		public void start() {
			FoxEntity.this.setRollingHead(false);
			super.start();
		}

		@Override
		public boolean canStart() {
			return !FoxEntity.this.isSitting()
					&& !FoxEntity.this.isSleeping()
					&& !FoxEntity.this.isInSneakingPose()
					&& !FoxEntity.this.isWalking()
					&& super.canStart();
		}
	}

	class AvoidDaylightGoal extends EscapeSunlightGoal {

		private int timer = toGoalTicks(100);

		public AvoidDaylightGoal(final double speed) {
			super(FoxEntity.this, speed);
		}

		@Override
		public boolean canStart() {
			if (FoxEntity.this.isSleeping() || mob.getTarget() != null) {
				return false;
			}

			if (FoxEntity.this.getEntityWorld().isThundering()
					&& FoxEntity.this.getEntityWorld().isSkyVisible(mob.getBlockPos())
			) {
				return targetShadedPos();
			}

			if (timer > 0) {
				timer--;
				return false;
			}

			timer = 100;
			BlockPos blockPos = mob.getBlockPos();
			return FoxEntity.this.getEntityWorld().isDay()
					&& FoxEntity.this.getEntityWorld().isSkyVisible(blockPos)
					&& !((ServerWorld) FoxEntity.this.getEntityWorld()).isNearOccupiedPointOfInterest(blockPos)
					&& targetShadedPos();
		}

		@Override
		public void start() {
			FoxEntity.this.stopActions();
			super.start();
		}
	}

	abstract class CalmDownGoal extends Goal {

		private final TargetPredicate WORRIABLE_ENTITY_PREDICATE = TargetPredicate.createAttackable()
				.setBaseMaxDistance(12.0)
				.ignoreVisibility()
				.setPredicate(FoxEntity.this.new WorriableEntityFilter());

		protected boolean isAtFavoredLocation() {
			BlockPos blockPos = BlockPos.ofFloored(
					FoxEntity.this.getX(),
					FoxEntity.this.getBoundingBox().maxY,
					FoxEntity.this.getZ()
			);
			return !FoxEntity.this.getEntityWorld().isSkyVisible(blockPos)
					&& FoxEntity.this.getPathfindingFavor(blockPos) >= 0.0F;
		}

		/**
		 * Проверяет, есть ли поблизости пугающие сущности (игроки, враги, ненирученные животные).
		 * Используется для принятия решения о том, может ли лиса успокоиться и лечь спать.
		 */
		protected boolean canCalmDown() {
			return !castToServerWorld(FoxEntity.this.getEntityWorld())
					.getTargets(
							LivingEntity.class,
							WORRIABLE_ENTITY_PREDICATE,
							FoxEntity.this,
							FoxEntity.this.getBoundingBox().expand(12.0, 6.0, 12.0)
					)
					.isEmpty();
		}
	}

	class DefendFriendGoal extends ActiveTargetGoal<LivingEntity> {

		private @Nullable LivingEntity offender;
		private @Nullable LivingEntity friend;
		private int lastAttackedTime;

		public DefendFriendGoal(
				final Class<LivingEntity> targetEntityClass,
				final boolean checkVisibility,
				final boolean checkCanNavigate,
				final TargetPredicate.EntityPredicate targetPredicate
		) {
			super(FoxEntity.this, targetEntityClass, 10, checkVisibility, checkCanNavigate, targetPredicate);
		}

		@Override
		public boolean canStart() {
			if (reciprocalChance > 0 && mob.getRandom().nextInt(reciprocalChance) != 0) {
				return false;
			}

			ServerWorld serverWorld = castToServerWorld(FoxEntity.this.getEntityWorld());
			for (LazyEntityReference<LivingEntity> ref : FoxEntity.this.getTrustedEntities().toList()) {
				LivingEntity livingEntity = ref.getEntityByClass(serverWorld, LivingEntity.class);
				if (livingEntity == null) {
					continue;
				}

				friend = livingEntity;
				offender = livingEntity.getAttacker();
				int attackedTime = livingEntity.getLastAttackedTime();
				return attackedTime != lastAttackedTime && canTrack(offender, targetPredicate);
			}

			return false;
		}

		@Override
		public void start() {
			setTargetEntity(offender);
			targetEntity = offender;
			if (friend != null) {
				lastAttackedTime = friend.getLastAttackedTime();
			}

			FoxEntity.this.playSound(SoundEvents.ENTITY_FOX_AGGRO, 1.0F, 1.0F);
			FoxEntity.this.setAggressive(true);
			FoxEntity.this.stopSleeping();
			super.start();
		}
	}

	class DelayedCalmDownGoal extends FoxEntity.CalmDownGoal {

		private static final int MAX_CALM_DOWN_TIME = toGoalTicks(140);
		private int timer = FoxEntity.this.random.nextInt(MAX_CALM_DOWN_TIME);

		public DelayedCalmDownGoal() {
			setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK, Goal.Control.JUMP));
		}

		@Override
		public boolean canStart() {
			return FoxEntity.this.sidewaysSpeed == 0.0F
					&& FoxEntity.this.upwardSpeed == 0.0F
					&& FoxEntity.this.forwardSpeed == 0.0F
					? canNotCalmDown() || FoxEntity.this.isSleeping()
					: false;
		}

		@Override
		public boolean shouldContinue() {
			return canNotCalmDown();
		}

		private boolean canNotCalmDown() {
			if (timer > 0) {
				timer--;
				return false;
			}

			return FoxEntity.this.getEntityWorld().isDay()
					&& isAtFavoredLocation()
					&& !canCalmDown()
					&& !FoxEntity.this.inPowderSnow;
		}

		@Override
		public void stop() {
			timer = FoxEntity.this.random.nextInt(MAX_CALM_DOWN_TIME);
			FoxEntity.this.stopActions();
		}

		@Override
		public void start() {
			FoxEntity.this.setSitting(false);
			FoxEntity.this.setCrouching(false);
			FoxEntity.this.setRollingHead(false);
			FoxEntity.this.setJumping(false);
			FoxEntity.this.setSleeping(true);
			FoxEntity.this.getNavigation().stop();
			FoxEntity.this.getMoveControl().moveTo(
					FoxEntity.this.getX(),
					FoxEntity.this.getY(),
					FoxEntity.this.getZ(),
					0.0
			);
		}
	}

	public class EatBerriesGoal extends MoveToTargetPosGoal {

		private static final int EATING_TIME = 40;
		protected int timer;

		public EatBerriesGoal(final double speed, final int range, final int maxYDifference) {
			super(FoxEntity.this, speed, range, maxYDifference);
		}

		@Override
		public double getDesiredDistanceToTarget() {
			return 2.0;
		}

		@Override
		public boolean shouldResetPath() {
			return tryingTime % 100 == 0;
		}

		@Override
		protected boolean isTargetPos(WorldView world, BlockPos pos) {
			BlockState blockState = world.getBlockState(pos);
			return blockState.isOf(Blocks.SWEET_BERRY_BUSH) && blockState.get(SweetBerryBushBlock.AGE) >= 2
					|| CaveVines.hasBerries(blockState);
		}

		@Override
		public void tick() {
			if (hasReached()) {
				if (timer >= EATING_TIME) {
					eatBerries();
				} else {
					timer++;
				}
			} else if (FoxEntity.this.random.nextFloat() < 0.05F) {
				FoxEntity.this.playSound(SoundEvents.ENTITY_FOX_SNIFF, 1.0F, 1.0F);
			}

			super.tick();
		}

		protected void eatBerries() {
			if (!castToServerWorld(FoxEntity.this.getEntityWorld()).getGameRules().getValue(GameRules.DO_MOB_GRIEFING)) {
				return;
			}

			BlockState blockState = FoxEntity.this.getEntityWorld().getBlockState(targetPos);
			if (blockState.isOf(Blocks.SWEET_BERRY_BUSH)) {
				pickSweetBerries(blockState);
			} else if (CaveVines.hasBerries(blockState)) {
				pickGlowBerries(blockState);
			}
		}

		private void pickGlowBerries(BlockState state) {
			CaveVines.pickBerries(FoxEntity.this, state, FoxEntity.this.getEntityWorld(), targetPos);
		}

		private void pickSweetBerries(BlockState state) {
			int age = state.get(SweetBerryBushBlock.AGE);
			int dropCount = 1 + FoxEntity.this.getEntityWorld().random.nextInt(2) + (age == 3 ? 1 : 0);
			ItemStack held = FoxEntity.this.getEquippedStack(EquipmentSlot.MAINHAND);
			if (held.isEmpty()) {
				FoxEntity.this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.SWEET_BERRIES));
				dropCount--;
			}

			if (dropCount > 0) {
				Block.dropStack(
						FoxEntity.this.getEntityWorld(),
						targetPos,
						new ItemStack(Items.SWEET_BERRIES, dropCount)
				);
			}

			FoxEntity.this.playSound(SoundEvents.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, 1.0F, 1.0F);
			FoxEntity.this.getEntityWorld().setBlockState(targetPos, state.with(SweetBerryBushBlock.AGE, 1), 2);
			FoxEntity.this.getEntityWorld().emitGameEvent(
					GameEvent.BLOCK_CHANGE,
					targetPos,
					GameEvent.Emitter.of(FoxEntity.this)
			);
		}

		@Override
		public boolean canStart() {
			return !FoxEntity.this.isSleeping() && super.canStart();
		}

		@Override
		public void start() {
			timer = 0;
			FoxEntity.this.setSitting(false);
			super.start();
		}
	}

	class EscapeWhenNotAggressiveGoal extends EscapeDangerGoal {

		public EscapeWhenNotAggressiveGoal(final double speed) {
			super(FoxEntity.this, speed);
		}

		@Override
		public boolean isInDanger() {
			return !FoxEntity.this.isAggressive() && super.isInDanger();
		}
	}

	static class FollowParentGoal extends net.minecraft.entity.ai.goal.FollowParentGoal {

		private final FoxEntity fox;

		public FollowParentGoal(FoxEntity fox, double speed) {
			super(fox, speed);
			this.fox = fox;
		}

		@Override
		public boolean canStart() {
			return !fox.isAggressive() && super.canStart();
		}

		@Override
		public boolean shouldContinue() {
			return !fox.isAggressive() && super.shouldContinue();
		}

		@Override
		public void start() {
			fox.stopActions();
			super.start();
		}
	}

	public static class FoxData extends PassiveEntity.PassiveData {

		public final FoxEntity.Variant type;

		public FoxData(FoxEntity.Variant type) {
			super(false);
			this.type = type;
		}
	}

	public class FoxLookControl extends LookControl {

		public FoxLookControl() {
			super(FoxEntity.this);
		}

		@Override
		public void tick() {
			if (!FoxEntity.this.isSleeping()) {
				super.tick();
			}
		}

		@Override
		protected boolean shouldStayHorizontal() {
			return !FoxEntity.this.isChasing()
					&& !FoxEntity.this.isInSneakingPose()
					&& !FoxEntity.this.isRollingHead()
					&& !FoxEntity.this.isWalking();
		}
	}

	class FoxMoveControl extends MoveControl {

		public FoxMoveControl() {
			super(FoxEntity.this);
		}

		@Override
		public void tick() {
			if (FoxEntity.this.wantsToPickupItem()) {
				super.tick();
			}
		}
	}

	class FoxSwimGoal extends SwimGoal {

		public FoxSwimGoal() {
			super(FoxEntity.this);
		}

		@Override
		public void start() {
			super.start();
			FoxEntity.this.stopActions();
		}

		@Override
		public boolean canStart() {
			return FoxEntity.this.isTouchingWater() && FoxEntity.this.getFluidHeight(FluidTags.WATER) > 0.25
					|| FoxEntity.this.isInLava();
		}
	}

	class GoToVillageGoal extends net.minecraft.entity.ai.goal.GoToVillageGoal {

		public GoToVillageGoal(final int unused, final int searchRange) {
			super(FoxEntity.this, searchRange);
		}

		@Override
		public void start() {
			FoxEntity.this.stopActions();
			super.start();
		}

		@Override
		public boolean canStart() {
			return super.canStart() && canGoToVillage();
		}

		@Override
		public boolean shouldContinue() {
			return super.shouldContinue() && canGoToVillage();
		}

		private boolean canGoToVillage() {
			return !FoxEntity.this.isSleeping()
					&& !FoxEntity.this.isSitting()
					&& !FoxEntity.this.isAggressive()
					&& FoxEntity.this.getTarget() == null;
		}
	}

	public class JumpChasingGoal extends DiveJumpingGoal {

		@Override
		public boolean canStart() {
			if (!FoxEntity.this.isFullyCrouched()) {
				return false;
			}

			LivingEntity target = FoxEntity.this.getTarget();
			if (target == null || !target.isAlive()) {
				return false;
			}

			if (target.getMovementDirection() != target.getHorizontalFacing()) {
				return false;
			}

			boolean canJump = FoxEntity.canJumpChase(FoxEntity.this, target);
			if (!canJump) {
				FoxEntity.this.getNavigation().findPathTo(target, 0);
				FoxEntity.this.setCrouching(false);
				FoxEntity.this.setRollingHead(false);
			}

			return canJump;
		}

		@Override
		public boolean shouldContinue() {
			LivingEntity target = FoxEntity.this.getTarget();
			if (target == null || !target.isAlive()) {
				return false;
			}

			double velY = FoxEntity.this.getVelocity().y;
			return (velY * velY >= 0.05F
					|| Math.abs(FoxEntity.this.getPitch()) >= 15.0F
					|| !FoxEntity.this.isOnGround()
			) && !FoxEntity.this.isWalking();
		}

		@Override
		public boolean canStop() {
			return false;
		}

		@Override
		public void start() {
			FoxEntity.this.setJumping(true);
			FoxEntity.this.setChasing(true);
			FoxEntity.this.setRollingHead(false);
			LivingEntity target = FoxEntity.this.getTarget();
			if (target != null) {
				FoxEntity.this.getLookControl().lookAt(target, 60.0F, 30.0F);
				Vec3d direction = new Vec3d(
						target.getX() - FoxEntity.this.getX(),
						target.getY() - FoxEntity.this.getY(),
						target.getZ() - FoxEntity.this.getZ()
				).normalize();
				FoxEntity.this.setVelocity(
						FoxEntity.this.getVelocity().add(direction.x * 0.8, 0.9, direction.z * 0.8)
				);
			}

			FoxEntity.this.getNavigation().stop();
		}

		@Override
		public void stop() {
			FoxEntity.this.setCrouching(false);
			FoxEntity.this.extraRollingHeight = 0.0F;
			FoxEntity.this.lastExtraRollingHeight = 0.0F;
			FoxEntity.this.setRollingHead(false);
			FoxEntity.this.setChasing(false);
		}

		@Override
		public void tick() {
			LivingEntity target = FoxEntity.this.getTarget();
			if (target != null) {
				FoxEntity.this.getLookControl().lookAt(target, 60.0F, 30.0F);
			}

			if (!FoxEntity.this.isWalking()) {
				Vec3d velocity = FoxEntity.this.getVelocity();
				if (velocity.y * velocity.y < 0.03F && FoxEntity.this.getPitch() != 0.0F) {
					FoxEntity.this.setPitch(MathHelper.lerpAngleDegrees(0.2F, FoxEntity.this.getPitch(), 0.0F));
				} else {
					double horizontal = velocity.horizontalLength();
					double pitch = Math.signum(-velocity.y)
							* Math.acos(horizontal / velocity.length())
							* 180.0F / (float) Math.PI;
					FoxEntity.this.setPitch((float) pitch);
				}
			}

			if (target != null && FoxEntity.this.distanceTo(target) <= 2.0F) {
				FoxEntity.this.tryAttack(castToServerWorld(FoxEntity.this.getEntityWorld()), target);
			} else if (FoxEntity.this.getPitch() > 0.0F
					&& FoxEntity.this.isOnGround()
					&& (float) FoxEntity.this.getVelocity().y != 0.0F
					&& FoxEntity.this.getEntityWorld().getBlockState(FoxEntity.this.getBlockPos()).isOf(Blocks.SNOW)
			) {
				FoxEntity.this.setPitch(60.0F);
				FoxEntity.this.setTarget(null);
				FoxEntity.this.setWalking(true);
			}
		}
	}

	class LookAtEntityGoal extends net.minecraft.entity.ai.goal.LookAtEntityGoal {

		public LookAtEntityGoal(
				final MobEntity fox,
				final Class<? extends LivingEntity> targetType,
				final float range
		) {
			super(fox, targetType, range);
		}

		@Override
		public boolean canStart() {
			return super.canStart() && !FoxEntity.this.isWalking() && !FoxEntity.this.isRollingHead();
		}

		@Override
		public boolean shouldContinue() {
			return super.shouldContinue() && !FoxEntity.this.isWalking() && !FoxEntity.this.isRollingHead();
		}
	}

	class MateGoal extends AnimalMateGoal {

		public MateGoal(final double chance) {
			super(FoxEntity.this, chance);
		}

		@Override
		public void start() {
			((FoxEntity) animal).stopActions();
			((FoxEntity) mate).stopActions();
			super.start();
		}

		@Override
		protected void breed() {
			FoxEntity child = (FoxEntity) animal.createChild(world, mate);
			if (child == null) {
				return;
			}

			ServerPlayerEntity player1 = animal.getLovingPlayer();
			ServerPlayerEntity player2 = mate.getLovingPlayer();
			ServerPlayerEntity triggerPlayer = player1;
			if (player1 != null) {
				child.trust(player1);
			} else {
				triggerPlayer = player2;
			}

			if (player2 != null && player1 != player2) {
				child.trust(player2);
			}

			if (triggerPlayer != null) {
				triggerPlayer.incrementStat(Stats.ANIMALS_BRED);
				Criteria.BRED_ANIMALS.trigger(triggerPlayer, animal, mate, child);
			}

			animal.setBreedingAge(6000);
			mate.setBreedingAge(6000);
			animal.resetLoveTicks();
			mate.resetLoveTicks();
			child.setBreedingAge(-24000);
			child.refreshPositionAndAngles(animal.getX(), animal.getY(), animal.getZ(), 0.0F, 0.0F);
			world.spawnEntityAndPassengers(child);
			world.sendEntityStatus(animal, (byte) 18);
			if (world.getGameRules().getValue(GameRules.DO_MOB_LOOT)) {
				world.spawnEntity(new ExperienceOrbEntity(
						world,
						animal.getX(),
						animal.getY(),
						animal.getZ(),
						animal.getRandom().nextInt(7) + 1
				));
			}
		}
	}

	class MoveToHuntGoal extends Goal {

		public MoveToHuntGoal() {
			setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
		}

		@Override
		public boolean canStart() {
			if (FoxEntity.this.isSleeping()) {
				return false;
			}

			LivingEntity target = FoxEntity.this.getTarget();
			return target != null
					&& target.isAlive()
					&& FoxEntity.CHICKEN_AND_RABBIT_FILTER.test(target)
					&& FoxEntity.this.squaredDistanceTo(target) > 36.0
					&& !FoxEntity.this.isInSneakingPose()
					&& !FoxEntity.this.isRollingHead()
					&& !FoxEntity.this.jumping;
		}

		@Override
		public void start() {
			FoxEntity.this.setSitting(false);
			FoxEntity.this.setWalking(false);
		}

		@Override
		public void stop() {
			LivingEntity target = FoxEntity.this.getTarget();
			if (target != null && FoxEntity.canJumpChase(FoxEntity.this, target)) {
				FoxEntity.this.setRollingHead(true);
				FoxEntity.this.setCrouching(true);
				FoxEntity.this.getNavigation().stop();
				FoxEntity.this.getLookControl().lookAt(
						target,
						FoxEntity.this.getMaxHeadRotation(),
						FoxEntity.this.getMaxLookPitchChange()
				);
			} else {
				FoxEntity.this.setRollingHead(false);
				FoxEntity.this.setCrouching(false);
			}
		}

		@Override
		public void tick() {
			LivingEntity target = FoxEntity.this.getTarget();
			if (target == null) {
				return;
			}

			FoxEntity.this.getLookControl().lookAt(
					target,
					FoxEntity.this.getMaxHeadRotation(),
					FoxEntity.this.getMaxLookPitchChange()
			);
			if (FoxEntity.this.squaredDistanceTo(target) <= 36.0) {
				FoxEntity.this.setRollingHead(true);
				FoxEntity.this.setCrouching(true);
				FoxEntity.this.getNavigation().stop();
			} else {
				FoxEntity.this.getNavigation().startMovingTo(target, 1.5);
			}
		}
	}

	class PickupItemGoal extends Goal {

		public PickupItemGoal() {
			setControls(EnumSet.of(Goal.Control.MOVE));
		}

		@Override
		public boolean canStart() {
			if (!FoxEntity.this.getEquippedStack(EquipmentSlot.MAINHAND).isEmpty()) {
				return false;
			}

			if (FoxEntity.this.getTarget() != null || FoxEntity.this.getAttacker() != null) {
				return false;
			}

			if (!FoxEntity.this.wantsToPickupItem()) {
				return false;
			}

			if (FoxEntity.this.getRandom().nextInt(toGoalTicks(10)) != 0) {
					return false;
				}
	
				List<ItemEntity> items = FoxEntity.this.getEntityWorld()
						.getEntitiesByClass(
								ItemEntity.class,
								FoxEntity.this.getBoundingBox().expand(8.0, 8.0, 8.0),
								FoxEntity.PICKABLE_DROP_FILTER
						);
				return !items.isEmpty() && FoxEntity.this.getEquippedStack(EquipmentSlot.MAINHAND).isEmpty();
			}
	
			@Override
			public void tick() {
				List<ItemEntity> items = FoxEntity.this.getEntityWorld()
						.getEntitiesByClass(
								ItemEntity.class,
								FoxEntity.this.getBoundingBox().expand(8.0, 8.0, 8.0),
								FoxEntity.PICKABLE_DROP_FILTER
						);
				ItemStack held = FoxEntity.this.getEquippedStack(EquipmentSlot.MAINHAND);
				if (held.isEmpty() && !items.isEmpty()) {
					FoxEntity.this.getNavigation().startMovingTo(items.get(0), 1.2F);
				}
			}
	
			@Override
			public void start() {
				List<ItemEntity> items = FoxEntity.this.getEntityWorld()
						.getEntitiesByClass(
								ItemEntity.class,
								FoxEntity.this.getBoundingBox().expand(8.0, 8.0, 8.0),
								FoxEntity.PICKABLE_DROP_FILTER
						);
				if (!items.isEmpty()) {
					FoxEntity.this.getNavigation().startMovingTo(items.get(0), 1.2F);
				}
			}
		}
	
		class SitDownAndLookAroundGoal extends FoxEntity.CalmDownGoal {
	
			private double lookX;
			private double lookZ;
			private int timer;
			private int counter;
	
			public SitDownAndLookAroundGoal() {
				setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
			}
	
			@Override
			public boolean canStart() {
				return FoxEntity.this.getAttacker() == null
						&& FoxEntity.this.getRandom().nextFloat() < 0.02F
						&& !FoxEntity.this.isSleeping()
						&& FoxEntity.this.getTarget() == null
						&& FoxEntity.this.getNavigation().isIdle()
						&& !canCalmDown()
						&& !FoxEntity.this.isChasing()
						&& !FoxEntity.this.isInSneakingPose();
			}
	
			@Override
			public boolean shouldContinue() {
				return counter > 0;
			}
	
			@Override
			public void start() {
				chooseNewAngle();
				counter = 2 + FoxEntity.this.getRandom().nextInt(3);
				FoxEntity.this.setSitting(true);
				FoxEntity.this.getNavigation().stop();
			}
	
			@Override
			public void stop() {
				FoxEntity.this.setSitting(false);
			}
	
			@Override
			public void tick() {
				timer--;
				if (timer <= 0) {
					counter--;
					chooseNewAngle();
				}
	
				FoxEntity.this.getLookControl().lookAt(
						FoxEntity.this.getX() + lookX,
						FoxEntity.this.getEyeY(),
						FoxEntity.this.getZ() + lookZ,
						FoxEntity.this.getMaxHeadRotation(),
						FoxEntity.this.getMaxLookPitchChange()
				);
			}
	
			private void chooseNewAngle() {
				double angle = (Math.PI * 2) * FoxEntity.this.getRandom().nextDouble();
				lookX = Math.cos(angle);
				lookZ = Math.sin(angle);
				timer = getTickCount(80 + FoxEntity.this.getRandom().nextInt(20));
			}
		}
	
		class StopWanderingGoal extends Goal {
	
			int timer;
	
			public StopWanderingGoal() {
				setControls(EnumSet.of(Goal.Control.LOOK, Goal.Control.JUMP, Goal.Control.MOVE));
			}
	
			@Override
			public boolean canStart() {
				return FoxEntity.this.isWalking();
			}
	
			@Override
			public boolean shouldContinue() {
				return canStart() && timer > 0;
			}
	
			@Override
			public void start() {
				timer = getTickCount(40);
			}
	
			@Override
			public void stop() {
				FoxEntity.this.setWalking(false);
			}
	
			@Override
			public void tick() {
				timer--;
			}
		}
	
		public enum Variant implements StringIdentifiable {
			RED(0, "red"),
			SNOW(1, "snow");
	
			public static final FoxEntity.Variant DEFAULT = RED;
			public static final StringIdentifiable.EnumCodec<FoxEntity.Variant> CODEC =
					StringIdentifiable.createCodec(FoxEntity.Variant::values);
			private static final IntFunction<FoxEntity.Variant> INDEX_MAPPER = ValueLists.createIndexToValueFunction(
					FoxEntity.Variant::getIndex,
					values(),
					ValueLists.OutOfBoundsHandling.ZERO
			);
			public static final PacketCodec<ByteBuf, FoxEntity.Variant> PACKET_CODEC =
					PacketCodecs.indexed(INDEX_MAPPER, FoxEntity.Variant::getIndex);
			private final int index;
			private final String id;
	
			Variant(final int index, final String id) {
				this.index = index;
				this.id = id;
			}
	
			@Override
			public String asString() {
				return id;
			}
	
			public int getIndex() {
				return index;
			}
	
			public static FoxEntity.Variant byIndex(int index) {
				return INDEX_MAPPER.apply(index);
			}
	
			public static FoxEntity.Variant fromBiome(RegistryEntry<Biome> biome) {
				return biome.isIn(BiomeTags.SPAWNS_SNOW_FOXES) ? SNOW : RED;
			}
		}
	
		public class WorriableEntityFilter implements TargetPredicate.EntityPredicate {
	
			@Override
			public boolean test(LivingEntity livingEntity, ServerWorld serverWorld) {
				if (livingEntity instanceof FoxEntity) {
					return false;
				}
	
				if (livingEntity instanceof ChickenEntity || livingEntity instanceof RabbitEntity
						|| livingEntity instanceof HostileEntity
				) {
					return true;
				}
	
				if (livingEntity instanceof TameableEntity tameable) {
					return !tameable.isTamed();
				}
	
				if (livingEntity instanceof PlayerEntity playerEntity
						&& (playerEntity.isSpectator() || playerEntity.isCreative())
				) {
					return false;
				}
	
				return !FoxEntity.this.canTrust(livingEntity) && !livingEntity.isSleeping() && !livingEntity.isSneaky();
			}
		}
	}
