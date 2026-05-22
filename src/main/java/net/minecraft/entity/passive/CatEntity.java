package net.minecraft.entity.passive;

import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.component.ComponentType;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.spawn.SpawnContext;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTables;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.attribute.EnvironmentAttributes;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Кошка — прирученное животное, способное спать рядом с хозяином и приносить утренние подарки.
 * Поза (стоя/крадётся/бежит) определяется скоростью движения.
 * Анимации сна и опускания головы интерполируются покадрово.
 */
public class CatEntity extends TameableEntity {

	public static final double CROUCHING_SPEED = 0.6;
	public static final double NORMAL_SPEED = 0.8;
	public static final double SPRINTING_SPEED = 1.33;

	private static final int TAME_CHANCE_DENOMINATOR = 3;
	private static final int BEG_SOUND_INTERVAL = 100;
	private static final int PURR_INTERVAL = 5;
	private static final int RARE_AMBIENT_CHANCE = 4;
	private static final int DESPAWN_AGE_THRESHOLD = 2400;

	private static final TrackedData<RegistryEntry<CatVariant>> CAT_VARIANT = DataTracker.registerData(
		CatEntity.class,
		TrackedDataHandlerRegistry.CAT_VARIANT
	);
	private static final TrackedData<Boolean> IN_SLEEPING_POSE = DataTracker.registerData(
		CatEntity.class,
		TrackedDataHandlerRegistry.BOOLEAN
	);
	private static final TrackedData<Boolean> HEAD_DOWN = DataTracker.registerData(
		CatEntity.class,
		TrackedDataHandlerRegistry.BOOLEAN
	);
	private static final TrackedData<Integer> COLLAR_COLOR = DataTracker.registerData(
		CatEntity.class,
		TrackedDataHandlerRegistry.INTEGER
	);

	private static final RegistryKey<CatVariant> DEFAULT_VARIANT = CatVariants.BLACK;
	private static final DyeColor DEFAULT_COLLAR_COLOR = DyeColor.RED;

	private @Nullable CatFleeGoal<PlayerEntity> fleeGoal;
	private @Nullable TemptGoal temptGoal;
	private float sleepAnimation;
	private float lastSleepAnimation;
	private float tailCurlAnimation;
	private float lastTailCurlAnimation;
	private boolean nearSleepingPlayer;
	private float headDownAnimation;
	private float lastHeadDownAnimation;

	public CatEntity(EntityType<? extends CatEntity> entityType, World world) {
		super(entityType, world);
		onTamedChanged();
	}

	@Override
	protected void initGoals() {
		temptGoal = new TemptGoal(this, CROUCHING_SPEED, stack -> stack.isIn(ItemTags.CAT_FOOD), true);
		goalSelector.add(1, new SwimGoal(this));
		goalSelector.add(1, new TameableEntity.TameableEscapeDangerGoal(1.5));
		goalSelector.add(2, new SitGoal(this));
		goalSelector.add(3, new SleepWithOwnerGoal(this));
		goalSelector.add(4, temptGoal);
		goalSelector.add(5, new GoToBedAndSleepGoal(this, 1.1, 8));
		goalSelector.add(6, new FollowOwnerGoal(this, 1.0, 10.0F, 5.0F));
		goalSelector.add(7, new CatSitOnBlockGoal(this, NORMAL_SPEED));
		goalSelector.add(8, new PounceAtTargetGoal(this, 0.3F));
		goalSelector.add(9, new AttackGoal(this));
		goalSelector.add(10, new AnimalMateGoal(this, NORMAL_SPEED));
		goalSelector.add(11, new WanderAroundFarGoal(this, NORMAL_SPEED, 1.0000001E-5F));
		goalSelector.add(12, new LookAtEntityGoal(this, PlayerEntity.class, 10.0F));
		targetSelector.add(1, new UntamedActiveTargetGoal<>(this, RabbitEntity.class, false, null));
		targetSelector.add(
			1,
			new UntamedActiveTargetGoal<>(this, TurtleEntity.class, false, TurtleEntity.BABY_TURTLE_ON_LAND_FILTER)
		);
	}

	public RegistryEntry<CatVariant> getVariant() {
		return dataTracker.get(CAT_VARIANT);
	}

	private void setVariant(RegistryEntry<CatVariant> variant) {
		dataTracker.set(CAT_VARIANT, variant);
	}

	@Override
	public <T> @Nullable T get(ComponentType<? extends T> type) {
		if (type == DataComponentTypes.CAT_VARIANT) {
			return castComponentValue((ComponentType<T>) type, getVariant());
		}

		return type == DataComponentTypes.CAT_COLLAR
			? castComponentValue((ComponentType<T>) type, getCollarColor())
			: super.get(type);
	}

	@Override
	protected void copyComponentsFrom(ComponentsAccess from) {
		copyComponentFrom(from, DataComponentTypes.CAT_VARIANT);
		copyComponentFrom(from, DataComponentTypes.CAT_COLLAR);
		super.copyComponentsFrom(from);
	}

	@Override
	protected <T> boolean setApplicableComponent(ComponentType<T> type, T value) {
		if (type == DataComponentTypes.CAT_VARIANT) {
			setVariant(castComponentValue(DataComponentTypes.CAT_VARIANT, value));
			return true;
		}

		if (type == DataComponentTypes.CAT_COLLAR) {
			setCollarColor(castComponentValue(DataComponentTypes.CAT_COLLAR, value));
			return true;
		}

		return super.setApplicableComponent(type, value);
	}

	public void setInSleepingPose(boolean sleeping) {
		dataTracker.set(IN_SLEEPING_POSE, sleeping);
	}

	public boolean isInSleepingPose() {
		return dataTracker.get(IN_SLEEPING_POSE);
	}

	void setHeadDown(boolean headDown) {
		dataTracker.set(HEAD_DOWN, headDown);
	}

	boolean isHeadDown() {
		return dataTracker.get(HEAD_DOWN);
	}

	public DyeColor getCollarColor() {
		return DyeColor.byIndex(dataTracker.get(COLLAR_COLOR));
	}

	private void setCollarColor(DyeColor color) {
		dataTracker.set(COLLAR_COLOR, color.getIndex());
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(CAT_VARIANT, Variants.getOrDefaultOrThrow(getRegistryManager(), DEFAULT_VARIANT));
		builder.add(IN_SLEEPING_POSE, false);
		builder.add(HEAD_DOWN, false);
		builder.add(COLLAR_COLOR, DEFAULT_COLLAR_COLOR.getIndex());
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		Variants.writeData(view, getVariant());
		view.put("CollarColor", DyeColor.INDEX_CODEC, getCollarColor());
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		Variants.fromData(view, RegistryKeys.CAT_VARIANT).ifPresent(this::setVariant);
		setCollarColor(view.<DyeColor>read("CollarColor", DyeColor.INDEX_CODEC).orElse(DEFAULT_COLLAR_COLOR));
	}

	@Override
	public void mobTick(ServerWorld world) {
		double speed = getMoveControl().getSpeed();
		if (getMoveControl().isMoving()) {
			if (speed == CROUCHING_SPEED) {
				setPose(EntityPose.CROUCHING);
				setSprinting(false);
			} else if (speed == SPRINTING_SPEED) {
				setPose(EntityPose.STANDING);
				setSprinting(true);
			} else {
				setPose(EntityPose.STANDING);
				setSprinting(false);
			}
		} else {
			setPose(EntityPose.STANDING);
			setSprinting(false);
		}
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		if (!isTamed()) {
			return SoundEvents.ENTITY_CAT_STRAY_AMBIENT;
		}

		if (isInLove()) {
			return SoundEvents.ENTITY_CAT_PURR;
		}

		return random.nextInt(RARE_AMBIENT_CHANCE) == 0
			? SoundEvents.ENTITY_CAT_PURREOW
			: SoundEvents.ENTITY_CAT_AMBIENT;
	}

	@Override
	public int getMinAmbientSoundDelay() {
		return 120;
	}

	public void hiss() {
		playSound(SoundEvents.ENTITY_CAT_HISS);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_CAT_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_CAT_DEATH;
	}

	public static DefaultAttributeContainer.Builder createCatAttributes() {
		return AnimalEntity.createAnimalAttributes()
			.add(EntityAttributes.MAX_HEALTH, 10.0)
			.add(EntityAttributes.MOVEMENT_SPEED, 0.3F)
			.add(EntityAttributes.ATTACK_DAMAGE, 3.0);
	}

	@Override
	protected void playEatSound() {
		playSound(SoundEvents.ENTITY_CAT_EAT, 1.0F, 1.0F);
	}

	@Override
	public void tick() {
		super.tick();
		if (temptGoal != null && temptGoal.isActive() && !isTamed() && age % BEG_SOUND_INTERVAL == 0) {
			playSound(SoundEvents.ENTITY_CAT_BEG_FOR_FOOD, 1.0F, 1.0F);
		}

		updateAnimations();
	}

	private void updateAnimations() {
		if ((isInSleepingPose() || isHeadDown()) && age % PURR_INTERVAL == 0) {
			playSound(
				SoundEvents.ENTITY_CAT_PURR,
				(float) (CROUCHING_SPEED + 0.4F * (random.nextFloat() - random.nextFloat())),
				1.0F
			);
		}

		updateSleepAnimation();
		updateHeadDownAnimation();
		nearSleepingPlayer = false;
		if (!isInSleepingPose()) {
			return;
		}

		BlockPos blockPos = getBlockPos();
		for (PlayerEntity player : getEntityWorld().getNonSpectatingEntities(
			PlayerEntity.class,
			new Box(blockPos).expand(2.0, 2.0, 2.0)
		)) {
			if (player.isSleeping()) {
				nearSleepingPlayer = true;
				break;
			}
		}
	}

	public boolean isNearSleepingPlayer() {
		return nearSleepingPlayer;
	}

	private void updateSleepAnimation() {
		lastSleepAnimation = sleepAnimation;
		lastTailCurlAnimation = tailCurlAnimation;
		if (isInSleepingPose()) {
			sleepAnimation = Math.min(1.0F, sleepAnimation + 0.15F);
			tailCurlAnimation = Math.min(1.0F, tailCurlAnimation + 0.08F);
		} else {
			sleepAnimation = Math.max(0.0F, sleepAnimation - 0.22F);
			tailCurlAnimation = Math.max(0.0F, tailCurlAnimation - 0.13F);
		}
	}

	private void updateHeadDownAnimation() {
		lastHeadDownAnimation = headDownAnimation;
		if (isHeadDown()) {
			headDownAnimation = Math.min(1.0F, headDownAnimation + 0.1F);
		} else {
			headDownAnimation = Math.max(0.0F, headDownAnimation - 0.13F);
		}
	}

	public float getSleepAnimationProgress(float tickProgress) {
		return MathHelper.lerp(tickProgress, lastSleepAnimation, sleepAnimation);
	}

	public float getTailCurlAnimationProgress(float tickProgress) {
		return MathHelper.lerp(tickProgress, lastTailCurlAnimation, tailCurlAnimation);
	}

	public float getHeadDownAnimationProgress(float tickProgress) {
		return MathHelper.lerp(tickProgress, lastHeadDownAnimation, headDownAnimation);
	}

	/**
	 * Создаёт котёнка. Вариант наследуется случайно от одного из родителей.
	 * Если хозяин приручён — котёнок тоже получает хозяина и смешанный цвет ошейника.
	 */
	public @Nullable CatEntity createChild(ServerWorld serverWorld, PassiveEntity passiveEntity) {
		CatEntity child = EntityType.CAT.create(serverWorld, SpawnReason.BREEDING);
		if (child == null || !(passiveEntity instanceof CatEntity otherParent)) {
			return child;
		}

		child.setVariant(random.nextBoolean() ? getVariant() : otherParent.getVariant());
		if (isTamed()) {
			child.setOwner(getOwnerReference());
			child.setTamed(true, true);
			child.setCollarColor(DyeColor.mixColors(serverWorld, getCollarColor(), otherParent.getCollarColor()));
		}

		return child;
	}

	@Override
	public boolean canBreedWith(AnimalEntity other) {
		if (!isTamed()) {
			return false;
		}

		if (!(other instanceof CatEntity otherCat)) {
			return false;
		}

		return otherCat.isTamed() && super.canBreedWith(other);
	}

	@Override
	public @Nullable EntityData initialize(
		ServerWorldAccess world,
		LocalDifficulty difficulty,
		SpawnReason spawnReason,
		@Nullable EntityData entityData
	) {
		entityData = super.initialize(world, difficulty, spawnReason, entityData);
		Variants.select(SpawnContext.of(world, getBlockPos()), RegistryKeys.CAT_VARIANT).ifPresent(this::setVariant);
		return entityData;
	}

	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		ItemStack stack = player.getStackInHand(hand);
		Item item = stack.getItem();
		if (isTamed()) {
			if (isOwner(player)) {
				if (item instanceof DyeItem dyeItem) {
					DyeColor dyeColor = dyeItem.getColor();
					if (dyeColor != getCollarColor()) {
						if (!getEntityWorld().isClient()) {
							setCollarColor(dyeColor);
							stack.decrementUnlessCreative(1, player);
							setPersistent();
						}

						return ActionResult.SUCCESS;
					}
				} else if (isBreedingItem(stack) && getHealth() < getMaxHealth()) {
					if (!getEntityWorld().isClient()) {
						eat(player, hand, stack);
						FoodComponent food = stack.get(DataComponentTypes.FOOD);
						heal(food != null ? food.nutrition() : 1.0F);
						playEatSound();
					}

					return ActionResult.SUCCESS;
				}

				ActionResult result = super.interactMob(player, hand);
				if (!result.isAccepted()) {
					setSitting(!isSitting());
					return ActionResult.SUCCESS;
				}

				return result;
			}
		} else if (isBreedingItem(stack)) {
			if (!getEntityWorld().isClient()) {
				eat(player, hand, stack);
				tryTame(player);
				setPersistent();
				playEatSound();
			}

			return ActionResult.SUCCESS;
		}

		ActionResult result = super.interactMob(player, hand);
		if (result.isAccepted()) {
			setPersistent();
		}

		return result;
	}

	@Override
	public boolean isBreedingItem(ItemStack stack) {
		return stack.isIn(ItemTags.CAT_FOOD);
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return !isTamed() && age > DESPAWN_AGE_THRESHOLD;
	}

	@Override
	public void setTamed(boolean tamed, boolean updateAttributes) {
		super.setTamed(tamed, updateAttributes);
		onTamedChanged();
	}

	protected void onTamedChanged() {
		if (fleeGoal == null) {
			fleeGoal = new CatFleeGoal<>(this, PlayerEntity.class, 16.0F, NORMAL_SPEED, SPRINTING_SPEED);
		}

		goalSelector.remove(fleeGoal);
		if (!isTamed()) {
			goalSelector.add(4, fleeGoal);
		}
	}

	/**
	 * Попытка приручения: 1/3 шанс успеха. При успехе кошка садится и показывает сердечки,
	 * при неудаче — дымок.
	 */
	private void tryTame(PlayerEntity player) {
		if (random.nextInt(TAME_CHANCE_DENOMINATOR) == 0) {
			setTamedBy(player);
			setSitting(true);
			getEntityWorld().sendEntityStatus(this, (byte) 7);
		} else {
			getEntityWorld().sendEntityStatus(this, (byte) 6);
		}
	}

	@Override
	public boolean bypassesSteppingEffects() {
		return isInSneakingPose() || super.bypassesSteppingEffects();
	}

	static class CatFleeGoal<T extends LivingEntity> extends FleeEntityGoal<T> {

		private final CatEntity cat;

		public CatFleeGoal(CatEntity cat, Class<T> fleeFromType, float distance, double slowSpeed, double fastSpeed) {
			super(cat, fleeFromType, distance, slowSpeed, fastSpeed, EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR);
			this.cat = cat;
		}

		@Override
		public boolean canStart() {
			return !cat.isTamed() && super.canStart();
		}

		@Override
		public boolean shouldContinue() {
			return !cat.isTamed() && super.shouldContinue();
		}
	}

	/**
	 * Цель сна рядом с хозяином: кошка ищет кровать хозяина и укладывается рядом.
	 * При пробуждении хозяина с достаточным временем сна — роняет утренний подарок.
	 */
	static class SleepWithOwnerGoal extends Goal {

		private final CatEntity cat;
		private @Nullable PlayerEntity owner;
		private @Nullable BlockPos bedPos;
		private int ticksOnBed;

		public SleepWithOwnerGoal(CatEntity cat) {
			this.cat = cat;
		}

		@Override
		public boolean canStart() {
			if (!cat.isTamed() || cat.isSitting()) {
				return false;
			}

			if (!(cat.getOwner() instanceof PlayerEntity playerOwner)) {
				return false;
			}

			owner = playerOwner;
			if (!owner.isSleeping()) {
				return false;
			}

			if (cat.squaredDistanceTo(owner) > 100.0) {
				return false;
			}

			BlockPos ownerPos = owner.getBlockPos();
			BlockState blockState = cat.getEntityWorld().getBlockState(ownerPos);
			if (!blockState.isIn(BlockTags.BEDS)) {
				return false;
			}

			bedPos = blockState.getOrEmpty(BedBlock.FACING)
				.map(direction -> ownerPos.offset(direction.getOpposite()))
				.orElseGet(() -> new BlockPos(ownerPos));

			return !isBedOccupied();
		}

		private boolean isBedOccupied() {
			for (CatEntity other : cat.getEntityWorld().getNonSpectatingEntities(
				CatEntity.class,
				new Box(bedPos).expand(2.0)
			)) {
				if (other != cat && (other.isInSleepingPose() || other.isHeadDown())) {
					return true;
				}
			}

			return false;
		}

		@Override
		public boolean shouldContinue() {
			return cat.isTamed()
				&& !cat.isSitting()
				&& owner != null
				&& owner.isSleeping()
				&& bedPos != null
				&& !isBedOccupied();
		}

		@Override
		public void start() {
			if (bedPos == null) {
				return;
			}

			cat.setInSittingPose(false);
			cat.getNavigation().startMovingTo(bedPos.getX(), bedPos.getY(), bedPos.getZ(), 1.1F);
		}

		@Override
		public void stop() {
			cat.setInSleepingPose(false);
			if (owner.getSleepTimer() >= 100
				&& cat.getEntityWorld().getRandom().nextFloat()
				< cat.getEntityWorld().getEnvironmentAttributes().getAttributeValue(
					EnvironmentAttributes.CAT_WAKING_UP_GIFT_CHANCE_GAMEPLAY,
					cat.getEntityPos()
				)
			) {
				dropMorningGifts();
			}

			ticksOnBed = 0;
			cat.setHeadDown(false);
			cat.getNavigation().stop();
		}

		private void dropMorningGifts() {
			Random random = cat.getRandom();
			BlockPos.Mutable mutable = new BlockPos.Mutable();
			mutable.set(cat.isLeashed() ? cat.getLeashHolder().getBlockPos() : cat.getBlockPos());
			cat.teleport(
				mutable.getX() + random.nextInt(11) - 5,
				mutable.getY() + random.nextInt(5) - 2,
				mutable.getZ() + random.nextInt(11) - 5,
				false
			);
			mutable.set(cat.getBlockPos());
			cat.forEachGiftedItem(
				getServerWorld(cat),
				LootTables.CAT_MORNING_GIFT_GAMEPLAY,
				(world, stack) -> world.spawnEntity(
					new ItemEntity(
						world,
						(double) mutable.getX() - MathHelper.sin(cat.bodyYaw * (float) (Math.PI / 180.0)),
						mutable.getY(),
						(double) mutable.getZ() + MathHelper.cos(cat.bodyYaw * (float) (Math.PI / 180.0)),
						stack
					)
				)
			);
		}

		@Override
		public void tick() {
			if (owner == null || bedPos == null) {
				return;
			}

			cat.setInSittingPose(false);
			cat.getNavigation().startMovingTo(bedPos.getX(), bedPos.getY(), bedPos.getZ(), 1.1F);
			if (cat.squaredDistanceTo(owner) >= 2.5) {
				cat.setInSleepingPose(false);
				return;
			}

			ticksOnBed++;
			if (ticksOnBed > getTickCount(16)) {
				cat.setInSleepingPose(true);
				cat.setHeadDown(false);
			} else {
				cat.lookAtEntity(owner, 45.0F, 45.0F);
				cat.setHeadDown(true);
			}
		}
	}

	static class TemptGoal extends net.minecraft.entity.ai.goal.TemptGoal {

		private @Nullable PlayerEntity player;
		private final CatEntity cat;

		public TemptGoal(CatEntity cat, double speed, Predicate<ItemStack> foodPredicate, boolean canBeScared) {
			super(cat, speed, foodPredicate, canBeScared);
			this.cat = cat;
		}

		@Override
		public void tick() {
			super.tick();
			if (player == null && mob.getRandom().nextInt(getTickCount(600)) == 0) {
				player = closestPlayer;
			} else if (mob.getRandom().nextInt(getTickCount(500)) == 0) {
				player = null;
			}
		}

		@Override
		protected boolean canBeScared() {
			return (player == null || !player.equals(closestPlayer)) && super.canBeScared();
		}

		@Override
		public boolean canStart() {
			return super.canStart() && !cat.isTamed();
		}
	}
}
