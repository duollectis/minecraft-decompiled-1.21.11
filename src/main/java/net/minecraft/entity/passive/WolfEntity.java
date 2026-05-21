package net.minecraft.entity.passive;

import net.minecraft.block.BlockState;
import net.minecraft.component.ComponentType;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.spawn.SpawnContext;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * {@code WolfEntity}.
 */
public class WolfEntity extends TameableEntity implements Angerable {

	private static final TrackedData<Boolean>
			BEGGING =
			DataTracker.registerData(WolfEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Integer>
			COLLAR_COLOR =
			DataTracker.registerData(WolfEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Long>
			ANGER_END_TIME =
			DataTracker.registerData(WolfEntity.class, TrackedDataHandlerRegistry.LONG);
	private static final TrackedData<RegistryEntry<WolfVariant>>
			VARIANT =
			DataTracker.registerData(WolfEntity.class, TrackedDataHandlerRegistry.WOLF_VARIANT);
	private static final TrackedData<RegistryEntry<WolfSoundVariant>> SOUND_VARIANT = DataTracker.registerData(
			WolfEntity.class, TrackedDataHandlerRegistry.WOLF_SOUND_VARIANT
	);
	public static final TargetPredicate.EntityPredicate FOLLOW_TAMED_PREDICATE = (entity, world) -> {
		EntityType<?> entityType = entity.getType();
		return entityType == EntityType.SHEEP || entityType == EntityType.RABBIT || entityType == EntityType.FOX;
	};
	private static final float WILD_MAX_HEALTH = 8.0F;
	private static final float TAMED_MAX_HEALTH = 40.0F;
	private static final float ARMOR_REPAIR_FRACTION = 0.125F;
	public static final float TAIL_ANGLE = (float) (Math.PI / 5);
	private static final DyeColor DEFAULT_COLLAR_COLOR = DyeColor.RED;
	private float begAnimationProgress;
	private float lastBegAnimationProgress;
	private boolean furWet;
	private boolean canShakeWaterOff;
	private float shakeProgress;
	private float lastShakeProgress;
	private static final UniformIntProvider ANGER_TIME_RANGE = TimeHelper.betweenSeconds(20, 39);
	private @Nullable LazyEntityReference<LivingEntity> angryAt;

	public WolfEntity(EntityType<? extends WolfEntity> entityType, World world) {
		super(entityType, world);
		this.setTamed(false, false);
		this.setPathfindingPenalty(PathNodeType.POWDER_SNOW, -1.0F);
		this.setPathfindingPenalty(PathNodeType.DANGER_POWDER_SNOW, -1.0F);
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(1, new SwimGoal(this));
		this.goalSelector.add(
				1,
				new TameableEntity.TameableEscapeDangerGoal(1.5, DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES)
		);
		this.goalSelector.add(2, new SitGoal(this));
		this.goalSelector.add(3, new WolfEntity.AvoidLlamaGoal<>(this, LlamaEntity.class, 24.0F, 1.5, 1.5));
		this.goalSelector.add(4, new PounceAtTargetGoal(this, 0.4F));
		this.goalSelector.add(5, new MeleeAttackGoal(this, 1.0, true));
		this.goalSelector.add(6, new FollowOwnerGoal(this, 1.0, 10.0F, 2.0F));
		this.goalSelector.add(7, new AnimalMateGoal(this, 1.0));
		this.goalSelector.add(8, new WanderAroundFarGoal(this, 1.0));
		this.goalSelector.add(9, new WolfBegGoal(this, 8.0F));
		this.goalSelector.add(10, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
		this.goalSelector.add(10, new LookAroundGoal(this));
		this.targetSelector.add(1, new TrackOwnerAttackerGoal(this));
		this.targetSelector.add(2, new AttackWithOwnerGoal(this));
		this.targetSelector.add(3, new RevengeGoal(this).setGroupRevenge());
		this.targetSelector.add(
				4,
				new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false, this::shouldAngerAt)
		);
		this.targetSelector.add(
				5,
				new UntamedActiveTargetGoal<>(this, AnimalEntity.class, false, FOLLOW_TAMED_PREDICATE)
		);
		this.targetSelector.add(
				6,
				new UntamedActiveTargetGoal<>(this, TurtleEntity.class, false, TurtleEntity.BABY_TURTLE_ON_LAND_FILTER)
		);
		this.targetSelector.add(7, new ActiveTargetGoal<>(this, AbstractSkeletonEntity.class, false));
		this.targetSelector.add(8, new UniversalAngerGoal<>(this, true));
	}

	public Identifier getTextureId() {
		WolfVariant wolfVariant = this.getVariant().value();
		if (this.isTamed()) {
			return wolfVariant.assetInfo().tame().texturePath();
		}
		else {
			return this.hasAngerTime() ? wolfVariant.assetInfo().angry().texturePath()
			                           : wolfVariant.assetInfo().wild().texturePath();
		}
	}

	private RegistryEntry<WolfVariant> getVariant() {
		return this.dataTracker.get(VARIANT);
	}

	private void setVariant(RegistryEntry<WolfVariant> variant) {
		this.dataTracker.set(VARIANT, variant);
	}

	private RegistryEntry<WolfSoundVariant> getSoundVariant() {
		return this.dataTracker.get(SOUND_VARIANT);
	}

	private void setSoundVariant(RegistryEntry<WolfSoundVariant> soundVariant) {
		this.dataTracker.set(SOUND_VARIANT, soundVariant);
	}

	@Override
	public <T> @Nullable T get(ComponentType<? extends T> type) {
		if (type == DataComponentTypes.WOLF_VARIANT) {
			return castComponentValue((ComponentType<T>) type, this.getVariant());
		}
		else if (type == DataComponentTypes.WOLF_SOUND_VARIANT) {
			return castComponentValue((ComponentType<T>) type, this.getSoundVariant());
		}
		else {
			return type == DataComponentTypes.WOLF_COLLAR ? castComponentValue(
					(ComponentType<T>) type,
					this.getCollarColor()
			) : super.get(type);
		}
	}

	@Override
	protected void copyComponentsFrom(ComponentsAccess from) {
		this.copyComponentFrom(from, DataComponentTypes.WOLF_VARIANT);
		this.copyComponentFrom(from, DataComponentTypes.WOLF_SOUND_VARIANT);
		this.copyComponentFrom(from, DataComponentTypes.WOLF_COLLAR);
		super.copyComponentsFrom(from);
	}

	@Override
	protected <T> boolean setApplicableComponent(ComponentType<T> type, T value) {
		if (type == DataComponentTypes.WOLF_VARIANT) {
			this.setVariant(castComponentValue(DataComponentTypes.WOLF_VARIANT, value));
			return true;
		}
		else if (type == DataComponentTypes.WOLF_SOUND_VARIANT) {
			this.setSoundVariant(castComponentValue(DataComponentTypes.WOLF_SOUND_VARIANT, value));
			return true;
		}
		else if (type == DataComponentTypes.WOLF_COLLAR) {
			this.setCollarColor(castComponentValue(DataComponentTypes.WOLF_COLLAR, value));
			return true;
		}
		else {
			return super.setApplicableComponent(type, value);
		}
	}

	public static DefaultAttributeContainer.Builder createWolfAttributes() {
		return AnimalEntity.createAnimalAttributes()
		                   .add(EntityAttributes.MOVEMENT_SPEED, 0.3F)
		                   .add(EntityAttributes.MAX_HEALTH, 8.0)
		                   .add(EntityAttributes.ATTACK_DAMAGE, 4.0);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		Registry<WolfSoundVariant> registry = this.getRegistryManager().getOrThrow(RegistryKeys.WOLF_SOUND_VARIANT);
		builder.add(VARIANT, Variants.getOrDefaultOrThrow(this.getRegistryManager(), WolfVariants.DEFAULT));
		builder.add(
				SOUND_VARIANT,
				registry.getOptional(WolfSoundVariants.CLASSIC).or(registry::getDefaultEntry).orElseThrow()
		);
		builder.add(BEGGING, false);
		builder.add(COLLAR_COLOR, DEFAULT_COLLAR_COLOR.getIndex());
		builder.add(ANGER_END_TIME, -1L);
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		this.playSound(SoundEvents.ENTITY_WOLF_STEP, 0.15F, 1.0F);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.put("CollarColor", DyeColor.INDEX_CODEC, this.getCollarColor());
		Variants.writeData(view, this.getVariant());
		this.writeAngerToData(view);
		this.getSoundVariant()
		    .getKey()
		    .ifPresent(soundVariant -> view.put(
				    "sound_variant",
				    RegistryKey.createCodec(RegistryKeys.WOLF_SOUND_VARIANT),
				    soundVariant
		    ));
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		Variants.fromData(view, RegistryKeys.WOLF_VARIANT).ifPresent(this::setVariant);
		this.setCollarColor(view.<DyeColor>read("CollarColor", DyeColor.INDEX_CODEC).orElse(DEFAULT_COLLAR_COLOR));
		this.readAngerFromData(this.getEntityWorld(), view);
		view
				.<RegistryKey<WolfSoundVariant>>read(
						"sound_variant",
						RegistryKey.createCodec(RegistryKeys.WOLF_SOUND_VARIANT)
				)
				.flatMap(
						soundVariantKey -> this.getRegistryManager()
						                       .getOrThrow(RegistryKeys.WOLF_SOUND_VARIANT)
						                       .getOptional(soundVariantKey)
				)
				.ifPresent(this::setSoundVariant);
	}

	@Override
	public @Nullable EntityData initialize(
			ServerWorldAccess world,
			LocalDifficulty difficulty,
			SpawnReason spawnReason,
			@Nullable EntityData entityData
	) {
		if (entityData instanceof WolfEntity.WolfData wolfData) {
			this.setVariant(wolfData.variant);
		}
		else {
			Optional<? extends RegistryEntry<WolfVariant>>
					optional =
					Variants.select(SpawnContext.of(world, this.getBlockPos()), RegistryKeys.WOLF_VARIANT);
			if (optional.isPresent()) {
				this.setVariant((RegistryEntry<WolfVariant>) optional.get());
				entityData = new WolfEntity.WolfData((RegistryEntry<WolfVariant>) optional.get());
			}
		}

		this.setSoundVariant(WolfSoundVariants.select(this.getRegistryManager(), world.getRandom()));
		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		if (this.hasAngerTime()) {
			return this.getSoundVariant().value().growlSound().value();
		}
		else if (this.random.nextInt(3) == 0) {
			return this.isTamed() && this.getHealth() < 20.0F
			       ? this.getSoundVariant().value().whineSound().value()
			       : this.getSoundVariant().value().pantSound().value();
		}
		else {
			return this.getSoundVariant().value().ambientSound().value();
		}
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return this.shouldArmorAbsorbDamage(source) ? SoundEvents.ITEM_WOLF_ARMOR_DAMAGE
		                                            : this.getSoundVariant().value().hurtSound().value();
	}

	@Override
	protected SoundEvent getDeathSound() {
		return this.getSoundVariant().value().deathSound().value();
	}

	@Override
	protected float getSoundVolume() {
		return 0.4F;
	}

	@Override
	public void tickMovement() {
		super.tickMovement();
		if (!this.getEntityWorld().isClient() && this.furWet && !this.canShakeWaterOff && !this.isNavigating()
				&& this.isOnGround()) {
			this.canShakeWaterOff = true;
			this.shakeProgress = 0.0F;
			this.lastShakeProgress = 0.0F;
			this.getEntityWorld().sendEntityStatus(this, (byte) 8);
		}

		if (!this.getEntityWorld().isClient()) {
			this.tickAngerLogic((ServerWorld) this.getEntityWorld(), true);
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (this.isAlive()) {
			this.lastBegAnimationProgress = this.begAnimationProgress;
			if (this.isBegging()) {
				this.begAnimationProgress = this.begAnimationProgress + (1.0F - this.begAnimationProgress) * 0.4F;
			}
			else {
				this.begAnimationProgress = this.begAnimationProgress + (0.0F - this.begAnimationProgress) * 0.4F;
			}

			if (this.isTouchingWaterOrRain()) {
				this.furWet = true;
				if (this.canShakeWaterOff && !this.getEntityWorld().isClient()) {
					this.getEntityWorld().sendEntityStatus(this, (byte) 56);
					this.resetShake();
				}
			}
			else if ((this.furWet || this.canShakeWaterOff) && this.canShakeWaterOff) {
				if (this.shakeProgress == 0.0F) {
					this.playSound(
							SoundEvents.ENTITY_WOLF_SHAKE,
							this.getSoundVolume(),
							(this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F
					);
					this.emitGameEvent(GameEvent.ENTITY_ACTION);
				}

				this.lastShakeProgress = this.shakeProgress;
				this.shakeProgress += 0.05F;
				if (this.lastShakeProgress >= 2.0F) {
					this.furWet = false;
					this.canShakeWaterOff = false;
					this.lastShakeProgress = 0.0F;
					this.shakeProgress = 0.0F;
				}

				if (this.shakeProgress > 0.4F) {
					float f = (float) this.getY();
					int i = (int) (MathHelper.sin((this.shakeProgress - 0.4F) * (float) Math.PI) * 7.0F);
					Vec3d vec3d = this.getVelocity();

					for (int j = 0; j < i; j++) {
						float g = (this.random.nextFloat() * 2.0F - 1.0F) * this.getWidth() * 0.5F;
						float h = (this.random.nextFloat() * 2.0F - 1.0F) * this.getWidth() * 0.5F;
						this
								.getEntityWorld()
								.addParticleClient(
										ParticleTypes.SPLASH,
										this.getX() + g,
										f + 0.8F,
										this.getZ() + h,
										vec3d.x,
										vec3d.y,
										vec3d.z
								);
					}
				}
			}
		}
	}

	private void resetShake() {
		this.canShakeWaterOff = false;
		this.shakeProgress = 0.0F;
		this.lastShakeProgress = 0.0F;
	}

	@Override
	public void onDeath(DamageSource damageSource) {
		this.furWet = false;
		this.canShakeWaterOff = false;
		this.lastShakeProgress = 0.0F;
		this.shakeProgress = 0.0F;
		super.onDeath(damageSource);
	}

	public float getFurWetBrightnessMultiplier(float tickProgress) {
		return !this.furWet ? 1.0F : Math.min(
				0.75F + MathHelper.lerp(tickProgress, this.lastShakeProgress, this.shakeProgress) / 2.0F * 0.25F,
				1.0F
		);
	}

	public float getShakeProgress(float tickProgress) {
		return MathHelper.lerp(tickProgress, this.lastShakeProgress, this.shakeProgress);
	}

	public float getBegAnimationProgress(float tickProgress) {
		return MathHelper.lerp(tickProgress, this.lastBegAnimationProgress, this.begAnimationProgress) * 0.15F
				* (float) Math.PI;
	}

	@Override
	public int getMaxLookPitchChange() {
		return this.isInSittingPose() ? 20 : super.getMaxLookPitchChange();
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		if (this.isInvulnerableTo(world, source)) {
			return false;
		}
		else {
			this.setSitting(false);
			return super.damage(world, source, amount);
		}
	}

	@Override
	protected void applyDamage(ServerWorld world, DamageSource source, float amount) {
		if (!this.shouldArmorAbsorbDamage(source)) {
			super.applyDamage(world, source, amount);
		}
		else {
			ItemStack itemStack = this.getBodyArmor();
			int i = itemStack.getDamage();
			int j = itemStack.getMaxDamage();
			itemStack.damage(MathHelper.ceil(amount), this, EquipmentSlot.BODY);
			if (Cracks.WOLF_ARMOR.getCrackLevel(i, j) != Cracks.WOLF_ARMOR.getCrackLevel(this.getBodyArmor())) {
				this.playSoundIfNotSilent(SoundEvents.ITEM_WOLF_ARMOR_CRACK);
				world.spawnParticles(
						new ItemStackParticleEffect(ParticleTypes.ITEM, Items.ARMADILLO_SCUTE.getDefaultStack()),
						this.getX(),
						this.getY() + 1.0,
						this.getZ(),
						20,
						0.2,
						0.1,
						0.2,
						0.1
				);
			}
		}
	}

	private boolean shouldArmorAbsorbDamage(DamageSource source) {
		return this.getBodyArmor().isOf(Items.WOLF_ARMOR) && !source.isIn(DamageTypeTags.BYPASSES_WOLF_ARMOR);
	}

	@Override
	protected void updateAttributesForTamed() {
		if (this.isTamed()) {
			this.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(40.0);
			this.setHealth(40.0F);
		}
		else {
			this.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(8.0);
		}
	}

	@Override
	public void damageArmor(DamageSource source, float amount) {
		this.damageEquipment(source, amount, EquipmentSlot.BODY);
	}

	@Override
	public boolean canRemoveSaddle(PlayerEntity player) {
		return this.isOwner(player);
	}

	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		ItemStack itemStack = player.getStackInHand(hand);
		Item item = itemStack.getItem();
		if (this.isTamed()) {
			if (this.isBreedingItem(itemStack) && this.getHealth() < this.getMaxHealth()) {
				this.eat(player, hand, itemStack);
				FoodComponent foodComponent = itemStack.get(DataComponentTypes.FOOD);
				float f = foodComponent != null ? foodComponent.nutrition() : 1.0F;
				this.heal(2.0F * f);
				return ActionResult.SUCCESS;
			}

			if (!(item instanceof DyeItem dyeItem && this.isOwner(player))) {
				if (this.canEquip(itemStack, EquipmentSlot.BODY) && !this.isWearingBodyArmor() && this.isOwner(player)
						&& !this.isBaby()) {
					this.equipBodyArmor(itemStack.copyWithCount(1));
					itemStack.decrementUnlessCreative(1, player);
					return ActionResult.SUCCESS;
				}

				if (this.isInSittingPose()
						&& this.isWearingBodyArmor()
						&& this.isOwner(player)
						&& this.getBodyArmor().isDamaged()
						&& this.getBodyArmor().canRepairWith(itemStack)) {
					itemStack.decrement(1);
					this.playSoundIfNotSilent(SoundEvents.ITEM_WOLF_ARMOR_REPAIR);
					ItemStack itemStack2 = this.getBodyArmor();
					int i = (int) (itemStack2.getMaxDamage() * ARMOR_REPAIR_FRACTION);
					itemStack2.setDamage(Math.max(0, itemStack2.getDamage() - i));
					return ActionResult.SUCCESS;
				}

				ActionResult actionResult = super.interactMob(player, hand);
				if (!actionResult.isAccepted() && this.isOwner(player)) {
					this.setSitting(!this.isSitting());
					this.jumping = false;
					this.navigation.stop();
					this.setTarget(null);
					return ActionResult.SUCCESS.noIncrementStat();
				}

				return actionResult;
			}

			DyeColor dyeColor = dyeItem.getColor();
			if (dyeColor != this.getCollarColor()) {
				this.setCollarColor(dyeColor);
				itemStack.decrementUnlessCreative(1, player);
				return ActionResult.SUCCESS;
			}
		}
		else if (!this.getEntityWorld().isClient() && itemStack.isOf(Items.BONE) && !this.hasAngerTime()) {
			itemStack.decrementUnlessCreative(1, player);
			this.tryTame(player);
			return ActionResult.SUCCESS_SERVER;
		}

		return super.interactMob(player, hand);
	}

	private void tryTame(PlayerEntity player) {
		if (this.random.nextInt(3) == 0) {
			this.setTamedBy(player);
			this.navigation.stop();
			this.setTarget(null);
			this.setSitting(true);
			this.getEntityWorld().sendEntityStatus(this, (byte) 7);
		}
		else {
			this.getEntityWorld().sendEntityStatus(this, (byte) 6);
		}
	}

	@Override
	public void handleStatus(byte status) {
		if (status == 8) {
			this.canShakeWaterOff = true;
			this.shakeProgress = 0.0F;
			this.lastShakeProgress = 0.0F;
		}
		else if (status == 56) {
			this.resetShake();
		}
		else {
			super.handleStatus(status);
		}
	}

	public float getTailAngle() {
		if (this.hasAngerTime()) {
			return 1.5393804F;
		}
		else if (this.isTamed()) {
			float f = this.getMaxHealth();
			float g = (f - this.getHealth()) / f;
			return (0.55F - g * 0.4F) * (float) Math.PI;
		}
		else {
			return TAIL_ANGLE;
		}
	}

	@Override
	public boolean isBreedingItem(ItemStack stack) {
		return stack.isIn(ItemTags.WOLF_FOOD);
	}

	@Override
	public int getLimitPerChunk() {
		return 8;
	}

	@Override
	public long getAngerEndTime() {
		return this.dataTracker.get(ANGER_END_TIME);
	}

	@Override
	public void setAngerEndTime(long angerEndTime) {
		this.dataTracker.set(ANGER_END_TIME, angerEndTime);
	}

	@Override
	public void chooseRandomAngerTime() {
		this.setAngerDuration(ANGER_TIME_RANGE.get(this.random));
	}

	@Override
	public @Nullable LazyEntityReference<LivingEntity> getAngryAt() {
		return this.angryAt;
	}

	@Override
	public void setAngryAt(@Nullable LazyEntityReference<LivingEntity> angryAt) {
		this.angryAt = angryAt;
	}

	public DyeColor getCollarColor() {
		return DyeColor.byIndex(this.dataTracker.get(COLLAR_COLOR));
	}

	private void setCollarColor(DyeColor color) {
		this.dataTracker.set(COLLAR_COLOR, color.getIndex());
	}

	public @Nullable WolfEntity createChild(ServerWorld serverWorld, PassiveEntity passiveEntity) {
		WolfEntity wolfEntity = EntityType.WOLF.create(serverWorld, SpawnReason.BREEDING);
		if (wolfEntity != null && passiveEntity instanceof WolfEntity wolfEntity2) {
			if (this.random.nextBoolean()) {
				wolfEntity.setVariant(this.getVariant());
			}
			else {
				wolfEntity.setVariant(wolfEntity2.getVariant());
			}

			if (this.isTamed()) {
				wolfEntity.setOwner(this.getOwnerReference());
				wolfEntity.setTamed(true, true);
				DyeColor dyeColor = this.getCollarColor();
				DyeColor dyeColor2 = wolfEntity2.getCollarColor();
				wolfEntity.setCollarColor(DyeColor.mixColors(serverWorld, dyeColor, dyeColor2));
			}

			wolfEntity.setSoundVariant(WolfSoundVariants.select(this.getRegistryManager(), this.random));
		}

		return wolfEntity;
	}

	public void setBegging(boolean begging) {
		this.dataTracker.set(BEGGING, begging);
	}

	@Override
	public boolean canBreedWith(AnimalEntity other) {
		if (other == this) {
			return false;
		}
		else if (!this.isTamed()) {
			return false;
		}
		else if (!(other instanceof WolfEntity wolfEntity)) {
			return false;
		}
		else if (!wolfEntity.isTamed()) {
			return false;
		}
		else {
			return wolfEntity.isInSittingPose() ? false : this.isInLove() && wolfEntity.isInLove();
		}
	}

	public boolean isBegging() {
		return this.dataTracker.get(BEGGING);
	}

	@Override
	public boolean canAttackWithOwner(LivingEntity target, LivingEntity owner) {
		if (target instanceof CreeperEntity || target instanceof GhastEntity || target instanceof ArmorStandEntity) {
			return false;
		}
		else if (target instanceof WolfEntity wolfEntity) {
			return !wolfEntity.isTamed() || wolfEntity.getOwner() != owner;
		}
		else if (target instanceof PlayerEntity playerEntity && owner instanceof PlayerEntity playerEntity2
				&& !playerEntity2.shouldDamagePlayer(playerEntity)) {
			return false;
		}
		else {
			return target instanceof AbstractHorseEntity abstractHorseEntity && abstractHorseEntity.isTame()
			       ? false
			       : !(target instanceof TameableEntity tameableEntity && tameableEntity.isTamed());
		}
	}

	@Override
	public boolean canBeLeashed() {
		return !this.hasAngerTime();
	}

	@Override
	public Vec3d getLeashOffset() {
		return new Vec3d(0.0, 0.6F * this.getStandingEyeHeight(), this.getWidth() * 0.4F);
	}

	public static boolean canSpawn(
			EntityType<WolfEntity> type,
			WorldAccess world,
			SpawnReason spawnReason,
			BlockPos pos,
			Random random
	) {
		return world.getBlockState(pos.down()).isIn(BlockTags.WOLVES_SPAWNABLE_ON) && isLightLevelValidForNaturalSpawn(
				world,
				pos
		);
	}

	/**
	 * {@code AvoidLlamaGoal}.
	 */
	class AvoidLlamaGoal<T extends LivingEntity> extends FleeEntityGoal<T> {

		private final WolfEntity wolf;

		public AvoidLlamaGoal(
				final WolfEntity wolf,
				final Class<T> fleeFromType,
				final float distance,
				final double slowSpeed,
				final double fastSpeed
		) {
			super(wolf, fleeFromType, distance, slowSpeed, fastSpeed);
			this.wolf = wolf;
		}

		@Override
		public boolean canStart() {
			return super.canStart() && this.targetEntity instanceof LlamaEntity ? !this.wolf.isTamed()
			                                                                      && this.isScaredOf((LlamaEntity) this.targetEntity)
			                                                                    : false;
		}

		private boolean isScaredOf(LlamaEntity llama) {
			return llama.getStrength() >= WolfEntity.this.random.nextInt(5);
		}

		@Override
		public void start() {
			WolfEntity.this.setTarget(null);
			super.start();
		}

		@Override
		public void tick() {
			WolfEntity.this.setTarget(null);
			super.tick();
		}
	}

	/**
	 * {@code WolfData}.
	 */
	public static class WolfData extends PassiveEntity.PassiveData {

		public final RegistryEntry<WolfVariant> variant;

		public WolfData(RegistryEntry<WolfVariant> variant) {
			super(false);
			this.variant = variant;
		}
	}
}
