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
 * Волк — прирученное животное, способное атаковать врагов владельца.
 * Поддерживает систему злости ({@link Angerable}), броню из чешуи броненосца,
 * анимацию встряхивания шерсти после намокания и несколько звуковых вариантов.
 */
public class WolfEntity extends TameableEntity implements Angerable {

	private static final TrackedData<Boolean> BEGGING = DataTracker.registerData(
			WolfEntity.class,
			TrackedDataHandlerRegistry.BOOLEAN
	);
	private static final TrackedData<Integer> COLLAR_COLOR = DataTracker.registerData(
			WolfEntity.class,
			TrackedDataHandlerRegistry.INTEGER
	);
	private static final TrackedData<Long> ANGER_END_TIME = DataTracker.registerData(
			WolfEntity.class,
			TrackedDataHandlerRegistry.LONG
	);
	private static final TrackedData<RegistryEntry<WolfVariant>> VARIANT = DataTracker.registerData(
			WolfEntity.class,
			TrackedDataHandlerRegistry.WOLF_VARIANT
	);
	private static final TrackedData<RegistryEntry<WolfSoundVariant>> SOUND_VARIANT = DataTracker.registerData(
			WolfEntity.class,
			TrackedDataHandlerRegistry.WOLF_SOUND_VARIANT
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
	private static final UniformIntProvider ANGER_TIME_RANGE = TimeHelper.betweenSeconds(20, 39);
	private float begAnimationProgress;
	private float lastBegAnimationProgress;
	private boolean furWet;
	private boolean canShakeWaterOff;
	private float shakeProgress;
	private float lastShakeProgress;
	private @Nullable LazyEntityReference<LivingEntity> angryAt;

	public WolfEntity(EntityType<? extends WolfEntity> entityType, World world) {
		super(entityType, world);
		setTamed(false, false);
		setPathfindingPenalty(PathNodeType.POWDER_SNOW, -1.0F);
		setPathfindingPenalty(PathNodeType.DANGER_POWDER_SNOW, -1.0F);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(1, new SwimGoal(this));
		goalSelector.add(
				1,
				new TameableEntity.TameableEscapeDangerGoal(1.5, DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES)
		);
		goalSelector.add(2, new SitGoal(this));
		goalSelector.add(3, new WolfEntity.AvoidLlamaGoal<>(this, LlamaEntity.class, 24.0F, 1.5, 1.5));
		goalSelector.add(4, new PounceAtTargetGoal(this, 0.4F));
		goalSelector.add(5, new MeleeAttackGoal(this, 1.0, true));
		goalSelector.add(6, new FollowOwnerGoal(this, 1.0, 10.0F, 2.0F));
		goalSelector.add(7, new AnimalMateGoal(this, 1.0));
		goalSelector.add(8, new WanderAroundFarGoal(this, 1.0));
		goalSelector.add(9, new WolfBegGoal(this, 8.0F));
		goalSelector.add(10, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
		goalSelector.add(10, new LookAroundGoal(this));
		targetSelector.add(1, new TrackOwnerAttackerGoal(this));
		targetSelector.add(2, new AttackWithOwnerGoal(this));
		targetSelector.add(3, new RevengeGoal(this).setGroupRevenge());
		targetSelector.add(
				4,
				new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false, this::shouldAngerAt)
		);
		targetSelector.add(
				5,
				new UntamedActiveTargetGoal<>(this, AnimalEntity.class, false, FOLLOW_TAMED_PREDICATE)
		);
		targetSelector.add(
				6,
				new UntamedActiveTargetGoal<>(this, TurtleEntity.class, false, TurtleEntity.BABY_TURTLE_ON_LAND_FILTER)
		);
		targetSelector.add(7, new ActiveTargetGoal<>(this, AbstractSkeletonEntity.class, false));
		targetSelector.add(8, new UniversalAngerGoal<>(this, true));
	}

	public Identifier getTextureId() {
		WolfVariant wolfVariant = getVariant().value();

		if (isTamed()) {
			return wolfVariant.assetInfo().tame().texturePath();
		}

		return hasAngerTime()
				? wolfVariant.assetInfo().angry().texturePath()
				: wolfVariant.assetInfo().wild().texturePath();
	}

	private RegistryEntry<WolfVariant> getVariant() {
		return dataTracker.get(VARIANT);
	}

	private void setVariant(RegistryEntry<WolfVariant> variant) {
		dataTracker.set(VARIANT, variant);
	}

	private RegistryEntry<WolfSoundVariant> getSoundVariant() {
		return dataTracker.get(SOUND_VARIANT);
	}

	private void setSoundVariant(RegistryEntry<WolfSoundVariant> soundVariant) {
		dataTracker.set(SOUND_VARIANT, soundVariant);
	}

	@Override
	public <T> @Nullable T get(ComponentType<? extends T> type) {
		if (type == DataComponentTypes.WOLF_VARIANT) {
			return castComponentValue((ComponentType<T>) type, getVariant());
		}

		if (type == DataComponentTypes.WOLF_SOUND_VARIANT) {
			return castComponentValue((ComponentType<T>) type, getSoundVariant());
		}

		if (type == DataComponentTypes.WOLF_COLLAR) {
			return castComponentValue((ComponentType<T>) type, getCollarColor());
		}

		return super.get(type);
	}

	@Override
	protected void copyComponentsFrom(ComponentsAccess from) {
		copyComponentFrom(from, DataComponentTypes.WOLF_VARIANT);
		copyComponentFrom(from, DataComponentTypes.WOLF_SOUND_VARIANT);
		copyComponentFrom(from, DataComponentTypes.WOLF_COLLAR);
		super.copyComponentsFrom(from);
	}

	@Override
	protected <T> boolean setApplicableComponent(ComponentType<T> type, T value) {
		if (type == DataComponentTypes.WOLF_VARIANT) {
			setVariant(castComponentValue(DataComponentTypes.WOLF_VARIANT, value));
			return true;
		}

		if (type == DataComponentTypes.WOLF_SOUND_VARIANT) {
			setSoundVariant(castComponentValue(DataComponentTypes.WOLF_SOUND_VARIANT, value));
			return true;
		}

		if (type == DataComponentTypes.WOLF_COLLAR) {
			setCollarColor(castComponentValue(DataComponentTypes.WOLF_COLLAR, value));
			return true;
		}

		return super.setApplicableComponent(type, value);
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
		Registry<WolfSoundVariant> registry = getRegistryManager().getOrThrow(RegistryKeys.WOLF_SOUND_VARIANT);
		builder.add(VARIANT, Variants.getOrDefaultOrThrow(getRegistryManager(), WolfVariants.DEFAULT));
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
		playSound(SoundEvents.ENTITY_WOLF_STEP, 0.15F, 1.0F);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.put("CollarColor", DyeColor.INDEX_CODEC, getCollarColor());
		Variants.writeData(view, getVariant());
		writeAngerToData(view);
		getSoundVariant()
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
		setCollarColor(view.<DyeColor>read("CollarColor", DyeColor.INDEX_CODEC).orElse(DEFAULT_COLLAR_COLOR));
		readAngerFromData(getEntityWorld(), view);
		view
				.<RegistryKey<WolfSoundVariant>>read(
						"sound_variant",
						RegistryKey.createCodec(RegistryKeys.WOLF_SOUND_VARIANT)
				)
				.flatMap(
						soundVariantKey -> getRegistryManager()
								.getOrThrow(RegistryKeys.WOLF_SOUND_VARIANT)
								.getOptional(soundVariantKey)
				)
				.ifPresent(this::setSoundVariant);
	}

	/**
	 * При спавне выбирает вариант окраски волка на основе биома через {@link Variants#select},
	 * а также случайный звуковой вариант из реестра.
	 */
	@Override
	public @Nullable EntityData initialize(
			ServerWorldAccess world,
			LocalDifficulty difficulty,
			SpawnReason spawnReason,
			@Nullable EntityData entityData
	) {
		if (entityData instanceof WolfEntity.WolfData wolfData) {
			setVariant(wolfData.variant);
		} else {
			Optional<? extends RegistryEntry<WolfVariant>> optional = Variants.select(
					SpawnContext.of(world, getBlockPos()),
					RegistryKeys.WOLF_VARIANT
			);

			if (optional.isPresent()) {
				RegistryEntry<WolfVariant> variant = optional.get();
				setVariant(variant);
				entityData = new WolfEntity.WolfData(variant);
			}
		}

		setSoundVariant(WolfSoundVariants.select(getRegistryManager(), world.getRandom()));

		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		if (hasAngerTime()) {
			return getSoundVariant().value().growlSound().value();
		}

		if (random.nextInt(3) == 0) {
			return isTamed() && getHealth() < 20.0F
					? getSoundVariant().value().whineSound().value()
					: getSoundVariant().value().pantSound().value();
		}

		return getSoundVariant().value().ambientSound().value();
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return shouldArmorAbsorbDamage(source)
				? SoundEvents.ITEM_WOLF_ARMOR_DAMAGE
				: getSoundVariant().value().hurtSound().value();
	}

	@Override
	protected SoundEvent getDeathSound() {
		return getSoundVariant().value().deathSound().value();
	}

	@Override
	protected float getSoundVolume() {
		return 0.4F;
	}

	@Override
	public void tickMovement() {
		super.tickMovement();

		if (!getEntityWorld().isClient()
				&& furWet
				&& !canShakeWaterOff
				&& !isNavigating()
				&& isOnGround()
		) {
			canShakeWaterOff = true;
			shakeProgress = 0.0F;
			lastShakeProgress = 0.0F;
			getEntityWorld().sendEntityStatus(this, (byte) 8);
		}

		if (!getEntityWorld().isClient()) {
			tickAngerLogic((ServerWorld) getEntityWorld(), true);
		}
	}

	@Override
	public void tick() {
		super.tick();

		if (!isAlive()) {
			return;
		}

		lastBegAnimationProgress = begAnimationProgress;

		if (isBegging()) {
			begAnimationProgress = begAnimationProgress + (1.0F - begAnimationProgress) * 0.4F;
		} else {
			begAnimationProgress = begAnimationProgress + (0.0F - begAnimationProgress) * 0.4F;
		}

		if (isTouchingWaterOrRain()) {
			furWet = true;

			if (canShakeWaterOff && !getEntityWorld().isClient()) {
				getEntityWorld().sendEntityStatus(this, (byte) 56);
				resetShake();
			}

			return;
		}

		if ((furWet || canShakeWaterOff) && canShakeWaterOff) {
			if (shakeProgress == 0.0F) {
				playSound(
						SoundEvents.ENTITY_WOLF_SHAKE,
						getSoundVolume(),
						(random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F
				);
				emitGameEvent(GameEvent.ENTITY_ACTION);
			}

			lastShakeProgress = shakeProgress;
			shakeProgress += 0.05F;

			if (lastShakeProgress >= 2.0F) {
				furWet = false;
				canShakeWaterOff = false;
				lastShakeProgress = 0.0F;
				shakeProgress = 0.0F;
			}

			if (shakeProgress > 0.4F) {
				float yPos = (float) getY();
				int particleCount = (int) (MathHelper.sin((shakeProgress - 0.4F) * (float) Math.PI) * 7.0F);
				Vec3d velocity = getVelocity();

				for (int particleIndex = 0; particleIndex < particleCount; particleIndex++) {
					float offsetX = (random.nextFloat() * 2.0F - 1.0F) * getWidth() * 0.5F;
					float offsetZ = (random.nextFloat() * 2.0F - 1.0F) * getWidth() * 0.5F;
					getEntityWorld().addParticleClient(
							ParticleTypes.SPLASH,
							getX() + offsetX,
							yPos + 0.8F,
							getZ() + offsetZ,
							velocity.x,
							velocity.y,
							velocity.z
					);
				}
			}
		}
	}

	private void resetShake() {
		canShakeWaterOff = false;
		shakeProgress = 0.0F;
		lastShakeProgress = 0.0F;
	}

	@Override
	public void onDeath(DamageSource damageSource) {
		furWet = false;
		canShakeWaterOff = false;
		lastShakeProgress = 0.0F;
		shakeProgress = 0.0F;
		super.onDeath(damageSource);
	}

	public float getFurWetBrightnessMultiplier(float tickProgress) {
		return !furWet ? 1.0F : Math.min(
				0.75F + MathHelper.lerp(tickProgress, lastShakeProgress, shakeProgress) / 2.0F * 0.25F,
				1.0F
		);
	}

	public float getShakeProgress(float tickProgress) {
		return MathHelper.lerp(tickProgress, lastShakeProgress, shakeProgress);
	}

	public float getBegAnimationProgress(float tickProgress) {
		return MathHelper.lerp(tickProgress, lastBegAnimationProgress, begAnimationProgress) * 0.15F * (float) Math.PI;
	}

	@Override
	public int getMaxLookPitchChange() {
		return isInSittingPose() ? 20 : super.getMaxLookPitchChange();
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		if (isInvulnerableTo(world, source)) {
			return false;
		}

		setSitting(false);

		return super.damage(world, source, amount);
	}

	/**
	 * Перехватывает урон, если на волке надета броня из чешуи броненосца.
	 * Вместо снижения HP повреждает предмет брони и при смене уровня трещин
	 * воспроизводит звук и спавнит частицы.
	 */
	@Override
	protected void applyDamage(ServerWorld world, DamageSource source, float amount) {
		if (!shouldArmorAbsorbDamage(source)) {
			super.applyDamage(world, source, amount);
			return;
		}

		ItemStack armor = getBodyArmor();
		int oldDamage = armor.getDamage();
		int maxDamage = armor.getMaxDamage();
		armor.damage(MathHelper.ceil(amount), this, EquipmentSlot.BODY);

		if (Cracks.WOLF_ARMOR.getCrackLevel(oldDamage, maxDamage) != Cracks.WOLF_ARMOR.getCrackLevel(getBodyArmor())) {
			playSoundIfNotSilent(SoundEvents.ITEM_WOLF_ARMOR_CRACK);
			world.spawnParticles(
					new ItemStackParticleEffect(ParticleTypes.ITEM, Items.ARMADILLO_SCUTE.getDefaultStack()),
					getX(),
					getY() + 1.0,
					getZ(),
					20,
					0.2,
					0.1,
					0.2,
					0.1
			);
		}
	}

	private boolean shouldArmorAbsorbDamage(DamageSource source) {
		return getBodyArmor().isOf(Items.WOLF_ARMOR) && !source.isIn(DamageTypeTags.BYPASSES_WOLF_ARMOR);
	}

	@Override
	protected void updateAttributesForTamed() {
		if (isTamed()) {
			getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(TAMED_MAX_HEALTH);
			setHealth(TAMED_MAX_HEALTH);
		} else {
			getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(WILD_MAX_HEALTH);
		}
	}

	@Override
	public void damageArmor(DamageSource source, float amount) {
		damageEquipment(source, amount, EquipmentSlot.BODY);
	}

	@Override
	public boolean canRemoveSaddle(PlayerEntity player) {
		return isOwner(player);
	}

	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		ItemStack itemStack = player.getStackInHand(hand);
		Item item = itemStack.getItem();

		if (isTamed()) {
			if (isBreedingItem(itemStack) && getHealth() < getMaxHealth()) {
				eat(player, hand, itemStack);
				FoodComponent foodComponent = itemStack.get(DataComponentTypes.FOOD);
				float healAmount = foodComponent != null ? foodComponent.nutrition() : 1.0F;
				heal(2.0F * healAmount);
				return ActionResult.SUCCESS;
			}

			if (item instanceof DyeItem dyeItem && isOwner(player)) {
				DyeColor dyeColor = dyeItem.getColor();

				if (dyeColor != getCollarColor()) {
					setCollarColor(dyeColor);
					itemStack.decrementUnlessCreative(1, player);
					return ActionResult.SUCCESS;
				}

				return super.interactMob(player, hand);
			}

			if (canEquip(itemStack, EquipmentSlot.BODY) && !isWearingBodyArmor() && isOwner(player) && !isBaby()) {
				equipBodyArmor(itemStack.copyWithCount(1));
				itemStack.decrementUnlessCreative(1, player);
				return ActionResult.SUCCESS;
			}

			if (isInSittingPose()
					&& isWearingBodyArmor()
					&& isOwner(player)
					&& getBodyArmor().isDamaged()
					&& getBodyArmor().canRepairWith(itemStack)
			) {
				itemStack.decrement(1);
				playSoundIfNotSilent(SoundEvents.ITEM_WOLF_ARMOR_REPAIR);
				ItemStack armor = getBodyArmor();
				int repairAmount = (int) (armor.getMaxDamage() * ARMOR_REPAIR_FRACTION);
				armor.setDamage(Math.max(0, armor.getDamage() - repairAmount));
				return ActionResult.SUCCESS;
			}

			ActionResult actionResult = super.interactMob(player, hand);

			if (!actionResult.isAccepted() && isOwner(player)) {
				setSitting(!isSitting());
				jumping = false;
				navigation.stop();
				setTarget(null);
				return ActionResult.SUCCESS.noIncrementStat();
			}

			return actionResult;
		}

		if (!getEntityWorld().isClient() && itemStack.isOf(Items.BONE) && !hasAngerTime()) {
			itemStack.decrementUnlessCreative(1, player);
			tryTame(player);
			return ActionResult.SUCCESS_SERVER;
		}

		return super.interactMob(player, hand);
	}

	private void tryTame(PlayerEntity player) {
		if (random.nextInt(3) == 0) {
			setTamedBy(player);
			navigation.stop();
			setTarget(null);
			setSitting(true);
			getEntityWorld().sendEntityStatus(this, (byte) 7);
		} else {
			getEntityWorld().sendEntityStatus(this, (byte) 6);
		}
	}

	@Override
	public void handleStatus(byte status) {
		if (status == 8) {
			canShakeWaterOff = true;
			shakeProgress = 0.0F;
			lastShakeProgress = 0.0F;
			return;
		}

		if (status == 56) {
			resetShake();
			return;
		}

		super.handleStatus(status);
	}

	/**
	 * Вычисляет угол хвоста в зависимости от состояния волка:
	 * злой — максимально поднят, ручной — зависит от здоровья, дикий — фиксированный угол.
	 */
	public float getTailAngle() {
		if (hasAngerTime()) {
			return 1.5393804F;
		}

		if (isTamed()) {
			float maxHealth = getMaxHealth();
			float healthFraction = (maxHealth - getHealth()) / maxHealth;
			return (0.55F - healthFraction * 0.4F) * (float) Math.PI;
		}

		return TAIL_ANGLE;
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
		return dataTracker.get(ANGER_END_TIME);
	}

	@Override
	public void setAngerEndTime(long angerEndTime) {
		dataTracker.set(ANGER_END_TIME, angerEndTime);
	}

	@Override
	public void chooseRandomAngerTime() {
		setAngerDuration(ANGER_TIME_RANGE.get(random));
	}

	@Override
	public @Nullable LazyEntityReference<LivingEntity> getAngryAt() {
		return angryAt;
	}

	@Override
	public void setAngryAt(@Nullable LazyEntityReference<LivingEntity> angryAt) {
		this.angryAt = angryAt;
	}

	public DyeColor getCollarColor() {
		return DyeColor.byIndex(dataTracker.get(COLLAR_COLOR));
	}

	private void setCollarColor(DyeColor color) {
		dataTracker.set(COLLAR_COLOR, color.getIndex());
	}

	/**
	 * Создаёт детёныша волка, наследующего вариант окраски одного из родителей.
	 * Если родитель приручён — детёныш также получает владельца и цвет ошейника
	 * как смешение цветов ошейников обоих родителей.
	 */
	@Override
	public @Nullable WolfEntity createChild(ServerWorld serverWorld, PassiveEntity passiveEntity) {
		WolfEntity child = EntityType.WOLF.create(serverWorld, SpawnReason.BREEDING);

		if (child == null || !(passiveEntity instanceof WolfEntity otherWolf)) {
			return child;
		}

		if (random.nextBoolean()) {
			child.setVariant(getVariant());
		} else {
			child.setVariant(otherWolf.getVariant());
		}

		if (isTamed()) {
			child.setOwner(getOwnerReference());
			child.setTamed(true, true);
			child.setCollarColor(DyeColor.mixColors(serverWorld, getCollarColor(), otherWolf.getCollarColor()));
		}

		child.setSoundVariant(WolfSoundVariants.select(getRegistryManager(), random));

		return child;
	}

	public void setBegging(boolean begging) {
		dataTracker.set(BEGGING, begging);
	}

	@Override
	public boolean canBreedWith(AnimalEntity other) {
		if (other == this) {
			return false;
		}

		if (!isTamed()) {
			return false;
		}

		if (!(other instanceof WolfEntity wolfEntity)) {
			return false;
		}

		if (!wolfEntity.isTamed()) {
			return false;
		}

		return !wolfEntity.isInSittingPose() && isInLove() && wolfEntity.isInLove();
	}

	public boolean isBegging() {
		return dataTracker.get(BEGGING);
	}

	/**
	 * Определяет, может ли волк атаковать цель вместе с владельцем.
	 * Запрещает атаку криперов, гастов, стоек для брони, прирученных волков того же владельца,
	 * игроков в мирном режиме и прирученных животных.
	 */
	@Override
	public boolean canAttackWithOwner(LivingEntity target, LivingEntity owner) {
		if (target instanceof CreeperEntity
				|| target instanceof GhastEntity
				|| target instanceof ArmorStandEntity
		) {
			return false;
		}

		if (target instanceof WolfEntity wolfEntity) {
			return !wolfEntity.isTamed() || wolfEntity.getOwner() != owner;
		}

		if (target instanceof PlayerEntity playerEntity
				&& owner instanceof PlayerEntity playerOwner
				&& !playerOwner.shouldDamagePlayer(playerEntity)
		) {
			return false;
		}

		if (target instanceof AbstractHorseEntity horseEntity && horseEntity.isTame()) {
			return false;
		}

		return !(target instanceof TameableEntity tameableEntity && tameableEntity.isTamed());
	}

	@Override
	public boolean canBeLeashed() {
		return !hasAngerTime();
	}

	@Override
	public Vec3d getLeashOffset() {
		return new Vec3d(0.0, 0.6F * getStandingEyeHeight(), getWidth() * 0.4F);
	}

	public static boolean canSpawn(
			EntityType<WolfEntity> type,
			WorldAccess world,
			SpawnReason spawnReason,
			BlockPos pos,
			Random random
	) {
		return world.getBlockState(pos.down()).isIn(BlockTags.WOLVES_SPAWNABLE_ON)
				&& isLightLevelValidForNaturalSpawn(world, pos);
	}

	class AvoidLlamaGoal<T extends LivingEntity> extends FleeEntityGoal<T> {

		private final WolfEntity wolf;

		public AvoidLlamaGoal(
				WolfEntity wolf,
				Class<T> fleeFromType,
				float distance,
				double slowSpeed,
				double fastSpeed
		) {
			super(wolf, fleeFromType, distance, slowSpeed, fastSpeed);
			this.wolf = wolf;
		}

		@Override
		public boolean canStart() {
			if (!super.canStart()) {
				return false;
			}

			return targetEntity instanceof LlamaEntity llama
					&& !wolf.isTamed()
					&& isScaredOf(llama);
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
	 * Данные спавна волка, хранящие выбранный вариант окраски для передачи детёнышам в стае.
	 */
	public static class WolfData extends PassiveEntity.PassiveData {

		public final RegistryEntry<WolfVariant> variant;

		public WolfData(RegistryEntry<WolfVariant> variant) {
			super(false);
			this.variant = variant;
		}
	}
}
