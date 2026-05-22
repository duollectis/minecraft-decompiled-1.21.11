package net.minecraft.entity.passive;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.block.*;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.AboveGroundTargeting;
import net.minecraft.entity.ai.NoPenaltySolidTargeting;
import net.minecraft.entity.ai.NoWaterTargeting;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.PointOfInterestTypeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.debug.DebugSubscriptionTypes;
import net.minecraft.world.debug.DebugTrackable;
import net.minecraft.world.debug.data.BeeDebugData;
import net.minecraft.world.poi.PointOfInterest;
import net.minecraft.world.poi.PointOfInterestStorage;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Пчела — пассивное летающее существо, опыляющее цветы и возвращающееся в улей.
 * Управляет сложной системой целей: поиск улья ({@link MoveToHiveGoal}), опыление ({@link PollinateGoal}),
 * рост урожая ({@link GrowCropsGoal}) и атака при раздражении ({@link StingGoal}).
 * После ужаления пчела погибает через некоторое время.
 */
public class BeeEntity extends AnimalEntity implements Angerable, Flutterer {

	public static final float WING_FLAP_FREQUENCY = 120.32113F;
	public static final int WING_FLAP_TICKS = MathHelper.ceil(1.4959966F);
	private static final TrackedData<Byte>
			BEE_FLAGS =
			DataTracker.registerData(BeeEntity.class, TrackedDataHandlerRegistry.BYTE);
	private static final TrackedData<Long>
			ANGER_END_TIME =
			DataTracker.registerData(BeeEntity.class, TrackedDataHandlerRegistry.LONG);
	private static final int NEAR_TARGET_FLAG = 2;
	private static final int HAS_STUNG_FLAG = 4;
	private static final int HAS_NECTAR_FLAG = 8;
	private static final int MAX_LIFETIME_AFTER_STINGING = 1200;
	private static final int FLOWER_NAVIGATION_START_TICKS = 600;
	private static final int POLLINATION_FAIL_TICKS = 3600;
	private static final int MIN_CROPS_TO_GROW = 4;
	private static final int MAX_POLLINATED_CROPS = 10;
	private static final int NORMAL_DIFFICULTY_STING_POISON_DURATION = 10;
	private static final int HARD_DIFFICULTY_STING_POISON_DURATION = 18;
	private static final int TOO_FAR_DISTANCE = 48;
	private static final int HIVE_SEARCH_RANGE = 2;
	private static final int MAX_HIVE_SEARCH_DISTANCE = 24;
	private static final int HIVE_RETURN_DISTANCE = 16;
	private static final int MIN_HIVE_RETURN_DISTANCE = 16;
	private static final int POLLINATE_TICKS_MIN = 20;
	public static final String CROPS_GROWN_SINCE_POLLINATION_KEY = "CropsGrownSincePollination";
	public static final String CANNOT_ENTER_HIVE_TICKS_KEY = "CannotEnterHiveTicks";
	public static final String TICKS_SINCE_POLLINATION_KEY = "TicksSincePollination";
	public static final String HAS_STUNG_KEY = "HasStung";
	public static final String HAS_NECTAR_KEY = "HasNectar";
	public static final String FLOWER_POS_KEY = "flower_pos";
	public static final String HIVE_POS_KEY = "hive_pos";
	public static final boolean DEFAULT_HAS_NECTAR = false;
	private static final boolean DEFAULT_HAS_STUNG = false;
	private static final int DEFAULT_TICKS_SINCE_POLLINATION = 0;
	private static final int DEFAULT_CANNOT_ENTER_HIVE_TICKS = 0;
	private static final int DEFAULT_CROPS_GROWN_SINCE_POLLINATION = 0;
	private static final UniformIntProvider ANGER_TIME_RANGE = TimeHelper.betweenSeconds(POLLINATE_TICKS_MIN, 39);
	private @Nullable LazyEntityReference<LivingEntity> angryAt;
	private float currentPitch;
	private float lastPitch;
	private int ticksSinceSting;
	int ticksSincePollination = 0;
	private int cannotEnterHiveTicks = 0;
	private int cropsGrownSincePollination = 0;
	private static final int MAX_TICKS_TO_FIND_HIVE = 200;
	int ticksLeftToFindHive;
	private static final int MAX_TICKS_UNTIL_POLLINATE = 200;
	private static final int MIN_TICKS_UNTIL_POLLINATE = 20;
	private static final int MAX_TICKS_UNTIL_POLLINATE_INIT = 60;
	int ticksUntilCanPollinate = MathHelper.nextInt(random, 20, 60);
	@Nullable BlockPos flowerPos;
	@Nullable BlockPos hivePos;
	BeeEntity.PollinateGoal pollinateGoal;
	BeeEntity.MoveToHiveGoal moveToHiveGoal;
	private BeeEntity.MoveToFlowerGoal moveToFlowerGoal;
	private int ticksInsideWater;

	public BeeEntity(EntityType<? extends BeeEntity> entityType, World world) {
		super(entityType, world);
		this.moveControl = new FlightMoveControl(this, 20, true);
		this.lookControl = new BeeEntity.BeeLookControl(this);
		this.setPathfindingPenalty(PathNodeType.DANGER_FIRE, -1.0F);
		this.setPathfindingPenalty(PathNodeType.WATER, -1.0F);
		this.setPathfindingPenalty(PathNodeType.WATER_BORDER, 16.0F);
		this.setPathfindingPenalty(PathNodeType.COCOA, -1.0F);
		this.setPathfindingPenalty(PathNodeType.FENCE, -1.0F);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(BEE_FLAGS, (byte) 0);
		builder.add(ANGER_END_TIME, -1L);
	}

	@Override
	public float getPathfindingFavor(BlockPos pos, WorldView world) {
		return world.getBlockState(pos).isAir() ? 10.0F : 0.0F;
	}

	@Override
	protected void initGoals() {
		goalSelector.add(0, new BeeEntity.StingGoal(this, 1.4F, true));
		goalSelector.add(1, new BeeEntity.EnterHiveGoal());
		goalSelector.add(2, new AnimalMateGoal(this, 1.0));
		goalSelector.add(3, new TemptGoal(this, 1.25, stack -> stack.isIn(ItemTags.BEE_FOOD), false));
		goalSelector.add(3, new BeeEntity.ValidateHiveGoal());
		goalSelector.add(3, new BeeEntity.ValidateFlowerGoal());
		pollinateGoal = new BeeEntity.PollinateGoal();
		goalSelector.add(4, pollinateGoal);
		goalSelector.add(5, new FollowParentGoal(this, 1.25));
		goalSelector.add(5, new BeeEntity.FindHiveGoal());
		moveToHiveGoal = new BeeEntity.MoveToHiveGoal();
		goalSelector.add(5, moveToHiveGoal);
		moveToFlowerGoal = new BeeEntity.MoveToFlowerGoal();
		goalSelector.add(6, moveToFlowerGoal);
		goalSelector.add(7, new BeeEntity.GrowCropsGoal());
		goalSelector.add(8, new BeeEntity.BeeWanderAroundGoal());
		goalSelector.add(9, new SwimGoal(this));
		targetSelector.add(1, new BeeEntity.BeeRevengeGoal(this).setGroupRevenge());
		targetSelector.add(2, new BeeEntity.StingTargetGoal(this));
		targetSelector.add(3, new UniversalAngerGoal<>(this, true));
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putNullable("hive_pos", BlockPos.CODEC, hivePos);
		view.putNullable("flower_pos", BlockPos.CODEC, flowerPos);
		view.putBoolean("HasNectar", hasNectar());
		view.putBoolean("HasStung", hasStung());
		view.putInt("TicksSincePollination", ticksSincePollination);
		view.putInt("CannotEnterHiveTicks", cannotEnterHiveTicks);
		view.putInt("CropsGrownSincePollination", cropsGrownSincePollination);
		writeAngerToData(view);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		setHasNectar(view.getBoolean("HasNectar", false));
		setHasStung(view.getBoolean("HasStung", false));
		ticksSincePollination = view.getInt("TicksSincePollination", 0);
		cannotEnterHiveTicks = view.getInt("CannotEnterHiveTicks", 0);
		cropsGrownSincePollination = view.getInt("CropsGrownSincePollination", 0);
		hivePos = view.<BlockPos>read("hive_pos", BlockPos.CODEC).orElse(null);
		flowerPos = view.<BlockPos>read("flower_pos", BlockPos.CODEC).orElse(null);
		readAngerFromData(getEntityWorld(), view);
	}

	/**
	 * Атакует цель жалом: наносит урон, добавляет яд в зависимости от сложности,
	 * увеличивает счётчик жал у цели и помечает пчелу как ужалившую.
	 * После ужаления пчела начинает умирать через {@link #MAX_LIFETIME_AFTER_STINGING} тиков.
	 */
	@Override
	public boolean tryAttack(ServerWorld world, Entity target) {
		DamageSource damageSource = getDamageSources().sting(this);
		boolean damaged = target.damage(world, damageSource, (int) getAttributeValue(EntityAttributes.ATTACK_DAMAGE));

		if (damaged) {
			EnchantmentHelper.onTargetDamaged(world, target, damageSource);

			if (target instanceof LivingEntity livingEntity) {
				livingEntity.setStingerCount(livingEntity.getStingerCount() + 1);
				int poisonDuration = 0;

				if (getEntityWorld().getDifficulty() == Difficulty.NORMAL) {
					poisonDuration = NORMAL_DIFFICULTY_STING_POISON_DURATION;
				}
				else if (getEntityWorld().getDifficulty() == Difficulty.HARD) {
					poisonDuration = HARD_DIFFICULTY_STING_POISON_DURATION;
				}

				if (poisonDuration > 0) {
					livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, poisonDuration * POLLINATE_TICKS_MIN, 0), this);
				}
			}

			setHasStung(true);
			stopAnger();
			playSound(SoundEvents.ENTITY_BEE_STING, 1.0F, 1.0F);
		}

		return damaged;
	}

	@Override
	public void tick() {
		super.tick();

		if (hasNectar() && getCropsGrownSincePollination() < MAX_POLLINATED_CROPS && random.nextFloat() < 0.05F) {
			int particleCount = random.nextInt(2) + 1;

			for (int idx = 0; idx < particleCount; idx++) {
				addParticle(
					getEntityWorld(),
					getX() - 0.3F,
					getX() + 0.3F,
					getZ() - 0.3F,
					getZ() + 0.3F,
					getBodyY(0.5),
					ParticleTypes.FALLING_NECTAR
				);
			}
		}

		updateBodyPitch();
	}

	/**
	 * Добавляет одну частицу нектара в случайную точку внутри заданного горизонтального прямоугольника.
	 * Координаты интерполируются через {@link MathHelper#lerp} с рандомным коэффициентом.
	 */
	private void addParticle(
			World world,
			double lastX,
			double x,
			double lastZ,
			double z,
			double y,
			ParticleEffect effect
	) {
		world.addParticleClient(
				effect,
				MathHelper.lerp(world.random.nextDouble(), lastX, x),
				y,
				MathHelper.lerp(world.random.nextDouble(), lastZ, z),
				0.0,
				0.0,
				0.0
		);
	}

	/**
	 * Вычисляет промежуточную точку навигации к целевой позиции с учётом перепада высот.
	 * При большом перепаде высот добавляет вертикальное смещение, чтобы пчела не застревала.
	 * Если расстояние меньше 15 блоков, уменьшает радиус поиска пропорционально дистанции.
	 */
	void startMovingTo(BlockPos pos) {
		Vec3d targetCenter = Vec3d.ofBottomCenter(pos);
		BlockPos currentPos = getBlockPos();
		int heightDiff = (int) targetCenter.y - currentPos.getY();
		int yOffset = 0;

		if (heightDiff > 2) {
			yOffset = 4;
		}
		else if (heightDiff < -2) {
			yOffset = -4;
		}

		int horizontalRange = 6;
		int verticalRange = 8;
		int manhattanDist = currentPos.getManhattanDistance(pos);

		if (manhattanDist < 15) {
			horizontalRange = manhattanDist / 2;
			verticalRange = manhattanDist / 2;
		}

		Vec3d wanderTarget = NoWaterTargeting.find(this, horizontalRange, verticalRange, yOffset, targetCenter, (float) (Math.PI / 10));

		if (wanderTarget != null) {
			navigation.setRangeMultiplier(0.5F);
			navigation.startMovingTo(wanderTarget.x, wanderTarget.y, wanderTarget.z, 1.0);
		}
	}

	public @Nullable BlockPos getFlowerPos() {
		return flowerPos;
	}

	public boolean hasFlower() {
		return flowerPos != null;
	}

	public void setFlowerPos(BlockPos flowerPos) {
		this.flowerPos = flowerPos;
	}

	@Debug
	public int getMoveGoalTicks() {
		return Math.max(moveToHiveGoal.ticks, moveToFlowerGoal.ticks);
	}

	@Debug
	public List<BlockPos> getPossibleHives() {
		return moveToHiveGoal.possibleHives;
	}

	/**
	 * Возвращает {@code true}, если пчела не опыляла слишком долго —
	 * счётчик {@code ticksSincePollination} превысил порог {@link #POLLINATION_FAIL_TICKS}.
	 */
	private boolean failedPollinatingTooLong() {
		return ticksSincePollination > POLLINATION_FAIL_TICKS;
	}

	void clearHivePos() {
		hivePos = null;
		ticksLeftToFindHive = MAX_TICKS_TO_FIND_HIVE;
	}

	void clearFlowerPos() {
		flowerPos = null;
		ticksUntilCanPollinate = MathHelper.nextInt(random, MIN_TICKS_UNTIL_POLLINATE, MAX_TICKS_UNTIL_POLLINATE_INIT);
	}

	/**
	 * Определяет, может ли пчела войти в улей в текущий момент.
	 * Пчела входит, если несёт нектар, слишком долго не опыляла или среда принуждает её оставаться в улье,
	 * и при этом улей не горит.
	 */
	boolean canEnterHive() {
		if (cannotEnterHiveTicks > 0 || pollinateGoal.isRunning() || hasStung() || getTarget() != null) {
			return false;
		}

		boolean wantsToEnter = hasNectar()
				|| failedPollinatingTooLong()
				|| getEntityWorld()
						.getEnvironmentAttributes()
						.getAttributeValue(EnvironmentAttributes.BEES_STAY_IN_HIVE_GAMEPLAY, getEntityPos());

		return wantsToEnter && !isHiveNearFire();
	}

	public void setCannotEnterHiveTicks(int cannotEnterHiveTicks) {
		this.cannotEnterHiveTicks = cannotEnterHiveTicks;
	}

	/**
	 * Возвращает интерполированный угол наклона тела пчелы между предыдущим и текущим тиком.
	 * Используется рендером для плавного визуального наклона при атаке.
	 */
	public float getBodyPitch(float tickProgress) {
		return MathHelper.lerp(tickProgress, lastPitch, currentPitch);
	}

	/**
	 * Обновляет угол наклона тела: плавно увеличивает при преследовании цели и уменьшает в покое.
	 */
	private void updateBodyPitch() {
		lastPitch = currentPitch;

		if (isNearTarget()) {
			currentPitch = Math.min(1.0F, currentPitch + 0.2F);
		}
		else {
			currentPitch = Math.max(0.0F, currentPitch - 0.24F);
		}
	}

	/**
	 * Серверный тик пчелы: обрабатывает утопание, постепенную гибель после ужаления
	 * (случайная вероятность смерти каждые 5 тиков) и счётчик опыления.
	 */
	@Override
	protected void mobTick(ServerWorld world) {
		boolean hasStung = hasStung();

		if (isTouchingWater()) {
			ticksInsideWater++;
		}
		else {
			ticksInsideWater = 0;
		}

		if (ticksInsideWater > POLLINATE_TICKS_MIN) {
			damage(world, getDamageSources().drown(), 1.0F);
		}

		if (hasStung) {
			ticksSinceSting++;

			if (ticksSinceSting % 5 == 0
					&& random.nextInt(MathHelper.clamp(MAX_LIFETIME_AFTER_STINGING - ticksSinceSting, 1, MAX_LIFETIME_AFTER_STINGING)) == 0) {
				damage(world, getDamageSources().generic(), getHealth());
			}
		}

		if (!hasNectar()) {
			ticksSincePollination++;
		}

		tickAngerLogic(world, false);
	}

	public void resetPollinationTicks() {
		ticksSincePollination = 0;
	}

	private boolean isHiveNearFire() {
		BeehiveBlockEntity beehiveBlockEntity = getHive();
		return beehiveBlockEntity != null && beehiveBlockEntity.isNearFire();
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
	public @Nullable LazyEntityReference<LivingEntity> getAngryAt() {
		return angryAt;
	}

	@Override
	public void setAngryAt(@Nullable LazyEntityReference<LivingEntity> angryAt) {
		this.angryAt = angryAt;
	}

	@Override
	public void chooseRandomAngerTime() {
		setAngerDuration(ANGER_TIME_RANGE.get(random));
	}

	private boolean doesHiveHaveSpace(BlockPos pos) {
		BlockEntity blockEntity = getEntityWorld().getBlockEntity(pos);
		return blockEntity instanceof BeehiveBlockEntity hive ? !hive.isFullOfBees() : false;
	}

	@Debug
	public boolean hasHivePos() {
		return hivePos != null;
	}

	@Debug
	public @Nullable BlockPos getHivePos() {
		return hivePos;
	}

	@Debug
	public GoalSelector getGoalSelector() {
		return goalSelector;
	}

	int getCropsGrownSincePollination() {
		return cropsGrownSincePollination;
	}

	private void resetCropCounter() {
		cropsGrownSincePollination = 0;
	}

	void addCropCounter() {
		cropsGrownSincePollination++;
	}

	@Override
	public void tickMovement() {
		super.tickMovement();

		if (getEntityWorld().isClient()) {
			return;
		}

		if (cannotEnterHiveTicks > 0) {
			cannotEnterHiveTicks--;
		}

		if (ticksLeftToFindHive > 0) {
			ticksLeftToFindHive--;
		}

		if (ticksUntilCanPollinate > 0) {
			ticksUntilCanPollinate--;
		}

		boolean isChasing = hasAngerTime() && !hasStung() && getTarget() != null
				&& getTarget().squaredDistanceTo(this) < 4.0;
		setNearTarget(isChasing);

		if (age % POLLINATE_TICKS_MIN == 0 && !hasValidHive()) {
			hivePos = null;
		}
	}

	@Nullable BeehiveBlockEntity getHive() {
		if (hivePos == null) {
			return null;
		}

		return isTooFar(hivePos)
				? null
				: getEntityWorld()
						.getBlockEntity(hivePos, BlockEntityType.BEEHIVE)
						.orElse(null);
	}

	boolean hasValidHive() {
		return getHive() != null;
	}

	public boolean hasNectar() {
		return getBeeFlag(HAS_NECTAR_FLAG);
	}

	void setHasNectar(boolean hasNectar) {
		if (hasNectar) {
			resetPollinationTicks();
		}

		setBeeFlag(HAS_NECTAR_FLAG, hasNectar);
	}

	public boolean hasStung() {
		return getBeeFlag(HAS_STUNG_FLAG);
	}

	private void setHasStung(boolean hasStung) {
		setBeeFlag(HAS_STUNG_FLAG, hasStung);
	}

	private boolean isNearTarget() {
		return getBeeFlag(NEAR_TARGET_FLAG);
	}

	private void setNearTarget(boolean nearTarget) {
		setBeeFlag(NEAR_TARGET_FLAG, nearTarget);
	}

	boolean isTooFar(BlockPos pos) {
		return !isWithinDistance(pos, TOO_FAR_DISTANCE);
	}

	/**
	 * Устанавливает или сбрасывает отдельный бит в байтовом флаге {@link #BEE_FLAGS}.
	 * Используется для компактного хранения булевых состояний пчелы в одном трекируемом байте.
	 */
	private void setBeeFlag(int bit, boolean value) {
		if (value) {
			dataTracker.set(BEE_FLAGS, (byte) (dataTracker.get(BEE_FLAGS) | bit));
		}
		else {
			dataTracker.set(BEE_FLAGS, (byte) (dataTracker.get(BEE_FLAGS) & ~bit));
		}
	}

	private boolean getBeeFlag(int location) {
		return (dataTracker.get(BEE_FLAGS) & location) != 0;
	}

	public static DefaultAttributeContainer.Builder createBeeAttributes() {
		return AnimalEntity.createAnimalAttributes()
		                   .add(EntityAttributes.MAX_HEALTH, 10.0)
		                   .add(EntityAttributes.FLYING_SPEED, 0.6F)
		                   .add(EntityAttributes.MOVEMENT_SPEED, 0.3F)
		                   .add(EntityAttributes.ATTACK_DAMAGE, 2.0);
	}

	@Override
	protected EntityNavigation createNavigation(World world) {
		BirdNavigation birdNavigation = new BirdNavigation(this, world) {
			@Override
			public boolean isValidPosition(BlockPos pos) {
				return !this.world.getBlockState(pos.down()).isAir();
			}

			@Override
			public void tick() {
				if (!BeeEntity.this.pollinateGoal.isRunning()) {
					super.tick();
				}
			}
		};
		birdNavigation.setCanOpenDoors(false);
		birdNavigation.setCanSwim(false);
		birdNavigation.setMaxFollowRange(48.0F);
		return birdNavigation;
	}

	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		ItemStack itemStack = player.getStackInHand(hand);
		if (isBreedingItem(itemStack) && itemStack.getItem() instanceof BlockItem blockItem
				&& blockItem.getBlock() instanceof FlowerBlock flowerBlock) {
			StatusEffectInstance statusEffectInstance = flowerBlock.getContactEffect();
			if (statusEffectInstance == null) {
				return super.interactMob(player, hand);
			}

			eat(player, hand, itemStack);

			if (!getEntityWorld().isClient()) {
				addStatusEffect(statusEffectInstance);
			}

			return ActionResult.SUCCESS;
		}

		return super.interactMob(player, hand);
	}

	@Override
	public boolean isBreedingItem(ItemStack stack) {
		return stack.isIn(ItemTags.BEE_FOOD);
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return null;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_BEE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_BEE_DEATH;
	}

	@Override
	protected float getSoundVolume() {
		return 0.4F;
	}

	public @Nullable BeeEntity createChild(ServerWorld serverWorld, PassiveEntity passiveEntity) {
		return EntityType.BEE.create(serverWorld, SpawnReason.BREEDING);
	}

	@Override
	protected void fall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition) {
	}

	@Override
	public boolean isFlappingWings() {
		return isInAir() && age % WING_FLAP_TICKS == 0;
	}

	@Override
	public boolean isInAir() {
		return !isOnGround();
	}

	public void onHoneyDelivered() {
		setHasNectar(false);
		resetCropCounter();
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		if (isInvulnerableTo(world, source)) {
			return false;
		}

		pollinateGoal.cancel();
		return super.damage(world, source, amount);
	}

	@Override
	protected void swimUpward(TagKey<Fluid> fluid) {
		setVelocity(getVelocity().add(0.0, 0.01, 0.0));
	}

	@Override
	public Vec3d getLeashOffset() {
		return new Vec3d(0.0, 0.5F * getStandingEyeHeight(), getWidth() * 0.2F);
	}

	boolean isWithinDistance(BlockPos pos, int distance) {
		return pos.isWithinDistance(getBlockPos(), distance);
	}

	public void setHivePos(BlockPos pos) {
		hivePos = pos;
	}

	/**
	 * Проверяет, является ли блок привлекательным для пчелы (цветок, не залитый водой).
	 * Подсолнух засчитывается только верхней половиной, чтобы избежать двойного опыления.
	 */
	public static boolean isAttractive(BlockState state) {
		if (!state.isIn(BlockTags.BEE_ATTRACTIVE)) {
			return false;
		}

		if (state.get(Properties.WATERLOGGED, false)) {
			return false;
		}

		return state.isOf(Blocks.SUNFLOWER)
				? state.get(TallPlantBlock.HALF) == DoubleBlockHalf.UPPER
				: true;
	}

	@Override
	public void registerTracking(ServerWorld world, DebugTrackable.Tracker tracker) {
		super.registerTracking(world, tracker);
		tracker.track(
				DebugSubscriptionTypes.BEES,
				() -> new BeeDebugData(
						Optional.ofNullable(getHivePos()),
						Optional.ofNullable(getFlowerPos()),
						getMoveGoalTicks(),
						getPossibleHives()
				)
		);
	}

	class BeeLookControl extends LookControl {

		BeeLookControl(final MobEntity entity) {
			super(entity);
		}

		@Override
		public void tick() {
			if (!BeeEntity.this.hasAngerTime()) {
				super.tick();
			}
		}

		@Override
		protected boolean shouldStayHorizontal() {
			return !BeeEntity.this.pollinateGoal.isRunning();
		}
	}

	class BeeRevengeGoal extends RevengeGoal {

		BeeRevengeGoal(final BeeEntity bee) {
			super(bee);
		}

		@Override
		public boolean shouldContinue() {
			return BeeEntity.this.hasAngerTime() && super.shouldContinue();
		}

		@Override
		protected void setMobEntityTarget(MobEntity mob, LivingEntity target) {
			if (mob instanceof BeeEntity && mob.canSee(target)) {
				mob.setTarget(target);
			}
		}
	}

	class BeeWanderAroundGoal extends Goal {

		BeeWanderAroundGoal() {
			setControls(EnumSet.of(Goal.Control.MOVE));
		}

		@Override
		public boolean canStart() {
			return BeeEntity.this.navigation.isIdle() && BeeEntity.this.random.nextInt(10) == 0;
		}

		@Override
		public boolean shouldContinue() {
			return BeeEntity.this.navigation.isFollowingPath();
		}

		@Override
		public void start() {
			Vec3d target = getRandomLocation();

			if (target != null) {
				BeeEntity.this.navigation.startMovingAlong(
					BeeEntity.this.navigation.findPathTo(BlockPos.ofFloored(target), 1),
					1.0
				);
			}
		}

		private @Nullable Vec3d getRandomLocation() {
			Vec3d direction;

			if (BeeEntity.this.hasValidHive()
					&& !BeeEntity.this.isWithinDistance(BeeEntity.this.hivePos, getMaxWanderDistance())
			) {
				Vec3d hiveCenter = Vec3d.ofCenter(BeeEntity.this.hivePos);
				direction = hiveCenter.subtract(BeeEntity.this.getEntityPos()).normalize();
			}
			else {
				direction = BeeEntity.this.getRotationVec(0.0F);
			}

			int searchRadius = 8;
			Vec3d aboveGround = AboveGroundTargeting.find(
				BeeEntity.this, searchRadius, 7, direction.x, direction.z, (float) (Math.PI / 2), 3, 1
			);

			return aboveGround != null
					? aboveGround
					: NoPenaltySolidTargeting.find(BeeEntity.this, searchRadius, 4, -2, direction.x, direction.z, (float) (Math.PI / 2));
		}

		private int getMaxWanderDistance() {
			int offset = BeeEntity.this.hasHivePos() || BeeEntity.this.hasFlower() ? MAX_HIVE_SEARCH_DISTANCE : 16;
			return TOO_FAR_DISTANCE - offset;
		}
	}

	class EnterHiveGoal extends BeeEntity.NotAngryGoal {

		@Override
		public boolean canBeeStart() {
			if (BeeEntity.this.hivePos == null
					|| !BeeEntity.this.canEnterHive()
					|| !BeeEntity.this.hivePos.isWithinDistance(BeeEntity.this.getEntityPos(), 2.0)
			) {
				return false;
			}

			BeehiveBlockEntity hive = BeeEntity.this.getHive();

			if (hive == null) {
				return false;
			}

			if (hive.isFullOfBees()) {
				BeeEntity.this.hivePos = null;
				return false;
			}

			return true;
		}

		@Override
		public boolean canBeeContinue() {
			return false;
		}

		@Override
		public void start() {
			BeehiveBlockEntity hive = BeeEntity.this.getHive();

			if (hive != null) {
				hive.tryEnterHive(BeeEntity.this);
			}
		}
	}

	class FindHiveGoal extends BeeEntity.NotAngryGoal {

		@Override
		public boolean canBeeStart() {
			return BeeEntity.this.ticksLeftToFindHive == 0
					&& !BeeEntity.this.hasHivePos()
					&& BeeEntity.this.canEnterHive();
		}

		@Override
		public boolean canBeeContinue() {
			return false;
		}

		@Override
		public void start() {
			BeeEntity.this.ticksLeftToFindHive = MAX_TICKS_TO_FIND_HIVE;
			List<BlockPos> freeHives = getNearbyFreeHives();

			if (freeHives.isEmpty()) {
				return;
			}

			for (BlockPos hivePos : freeHives) {
				if (!BeeEntity.this.moveToHiveGoal.isPossibleHive(hivePos)) {
					BeeEntity.this.hivePos = hivePos;
					return;
				}
			}

			BeeEntity.this.moveToHiveGoal.clearPossibleHives();
			BeeEntity.this.hivePos = freeHives.get(0);
		}

		/**
		 * Ищет ближайшие ульи с свободным местом через {@link PointOfInterestStorage},
		 * отсортированные по расстоянию от текущей позиции пчелы.
		 */
		private List<BlockPos> getNearbyFreeHives() {
			BlockPos currentPos = BeeEntity.this.getBlockPos();
			PointOfInterestStorage poiStorage = ((ServerWorld) BeeEntity.this.getEntityWorld()).getPointOfInterestStorage();

			return poiStorage
					.getInCircle(
						poiType -> poiType.isIn(PointOfInterestTypeTags.BEE_HOME),
						currentPos,
						POLLINATE_TICKS_MIN,
						PointOfInterestStorage.OccupationStatus.ANY
					)
					.map(PointOfInterest::getPos)
					.filter(BeeEntity.this::doesHiveHaveSpace)
					.sorted(Comparator.comparingDouble(pos -> pos.getSquaredDistance(currentPos)))
					.collect(Collectors.toList());
		}
	}

	class GrowCropsGoal extends BeeEntity.NotAngryGoal {

		static final int GROW_CROPS_INTERVAL = 30;

		@Override
		public boolean canBeeStart() {
			if (BeeEntity.this.getCropsGrownSincePollination() >= MAX_POLLINATED_CROPS) {
				return false;
			}

			return BeeEntity.this.random.nextFloat() >= 0.3F
					&& BeeEntity.this.hasNectar()
					&& BeeEntity.this.hasValidHive();
		}

		@Override
		public boolean canBeeContinue() {
			return canBeeStart();
		}

		@Override
		public void tick() {
			if (BeeEntity.this.random.nextInt(getTickCount(GROW_CROPS_INTERVAL)) != 0) {
				return;
			}

			for (int layerIndex = 1; layerIndex <= 2; layerIndex++) {
				BlockPos blockPos = BeeEntity.this.getBlockPos().down(layerIndex);
				BlockState blockState = BeeEntity.this.getEntityWorld().getBlockState(blockPos);
				Block block = blockState.getBlock();
				BlockState grown = null;

				if (blockState.isIn(BlockTags.BEE_GROWABLES)) {
					if (block instanceof CropBlock cropBlock) {
						if (!cropBlock.isMature(blockState)) {
							grown = cropBlock.withAge(cropBlock.getAge(blockState) + 1);
						}
					}
					else if (block instanceof StemBlock) {
						int stemAge = blockState.get(StemBlock.AGE);

						if (stemAge < 7) {
							grown = blockState.with(StemBlock.AGE, stemAge + 1);
						}
					}
					else if (blockState.isOf(Blocks.SWEET_BERRY_BUSH)) {
						int berryAge = blockState.get(SweetBerryBushBlock.AGE);

						if (berryAge < 3) {
							grown = blockState.with(SweetBerryBushBlock.AGE, berryAge + 1);
						}
					}
					else if (blockState.isOf(Blocks.CAVE_VINES) || blockState.isOf(Blocks.CAVE_VINES_PLANT)) {
						Fertilizable fertilizable = (Fertilizable) block;

						if (fertilizable.isFertilizable(BeeEntity.this.getEntityWorld(), blockPos, blockState)) {
							fertilizable.grow(
								(ServerWorld) BeeEntity.this.getEntityWorld(),
								BeeEntity.this.random,
								blockPos,
								blockState
							);
							grown = BeeEntity.this.getEntityWorld().getBlockState(blockPos);
						}
					}

					if (grown != null) {
						BeeEntity.this.getEntityWorld().syncWorldEvent(2011, blockPos, 15);
						BeeEntity.this.getEntityWorld().setBlockState(blockPos, grown);
						BeeEntity.this.addCropCounter();
					}
				}
			}
		}
	}

	public class MoveToFlowerGoal extends BeeEntity.NotAngryGoal {

		private static final int MAX_FLOWER_NAVIGATION_TICKS = 2400;
		int ticks;

		MoveToFlowerGoal() {
			setControls(EnumSet.of(Goal.Control.MOVE));
		}

		@Override
		public boolean canBeeStart() {
			return BeeEntity.this.flowerPos != null
					&& !BeeEntity.this.hasPositionTarget()
					&& shouldMoveToFlower()
					&& !BeeEntity.this.isWithinDistance(BeeEntity.this.flowerPos, 2);
		}

		@Override
		public boolean canBeeContinue() {
			return canBeeStart();
		}

		@Override
		public void start() {
			ticks = 0;
			super.start();
		}

		@Override
		public void stop() {
			ticks = 0;
			BeeEntity.this.navigation.stop();
			BeeEntity.this.navigation.resetRangeMultiplier();
		}

		@Override
		public void tick() {
			if (BeeEntity.this.flowerPos == null) {
				return;
			}

			ticks++;

			if (ticks > getTickCount(MAX_FLOWER_NAVIGATION_TICKS)) {
				BeeEntity.this.clearFlowerPos();
			}
			else if (!BeeEntity.this.navigation.isFollowingPath()) {
				if (BeeEntity.this.isTooFar(BeeEntity.this.flowerPos)) {
					BeeEntity.this.clearFlowerPos();
				}
				else {
					BeeEntity.this.startMovingTo(BeeEntity.this.flowerPos);
				}
			}
		}

		private boolean shouldMoveToFlower() {
			return BeeEntity.this.ticksSincePollination > FLOWER_NAVIGATION_START_TICKS;
		}
	}

	@Debug
	public class MoveToHiveGoal extends BeeEntity.NotAngryGoal {

		public static final int MAX_HIVE_NAVIGATION_TICKS = 2400;
		int ticks;
		private static final int MAX_POSSIBLE_HIVES = 3;
		final List<BlockPos> possibleHives = Lists.newArrayList();
		private @Nullable Path path;
		private static final int TICKS_UNTIL_LOST = 60;
		private int ticksUntilLost;

		MoveToHiveGoal() {
			setControls(EnumSet.of(Goal.Control.MOVE));
		}

		@Override
		public boolean canBeeStart() {
			return BeeEntity.this.hivePos != null
					&& !BeeEntity.this.isTooFar(BeeEntity.this.hivePos)
					&& !BeeEntity.this.hasPositionTarget()
					&& BeeEntity.this.canEnterHive()
					&& !isCloseEnough(BeeEntity.this.hivePos)
					&& BeeEntity.this.getEntityWorld().getBlockState(BeeEntity.this.hivePos).isIn(BlockTags.BEEHIVES);
		}

		@Override
		public boolean canBeeContinue() {
			return canBeeStart();
		}

		@Override
		public void start() {
			ticks = 0;
			ticksUntilLost = 0;
			super.start();
		}

		@Override
		public void stop() {
			ticks = 0;
			ticksUntilLost = 0;
			BeeEntity.this.navigation.stop();
			BeeEntity.this.navigation.resetRangeMultiplier();
		}

		@Override
		public void tick() {
			if (BeeEntity.this.hivePos == null) {
				return;
			}

			ticks++;

			if (ticks > getTickCount(MAX_HIVE_NAVIGATION_TICKS)) {
				makeChosenHivePossibleHive();
				return;
			}

			if (BeeEntity.this.navigation.isFollowingPath()) {
				return;
			}

			if (!BeeEntity.this.isWithinDistance(BeeEntity.this.hivePos, HIVE_RETURN_DISTANCE)) {
				if (BeeEntity.this.isTooFar(BeeEntity.this.hivePos)) {
					BeeEntity.this.clearHivePos();
				}
				else {
					BeeEntity.this.startMovingTo(BeeEntity.this.hivePos);
				}

				return;
			}

			boolean reachedHive = startMovingToFar(BeeEntity.this.hivePos);

			if (!reachedHive) {
				makeChosenHivePossibleHive();
			}
			else if (path != null && BeeEntity.this.navigation.getCurrentPath().equalsPath(path)) {
				ticksUntilLost++;

				if (ticksUntilLost > TICKS_UNTIL_LOST) {
					BeeEntity.this.clearHivePos();
					ticksUntilLost = 0;
				}
			}
			else {
				path = BeeEntity.this.navigation.getCurrentPath();
			}
		}

		private boolean startMovingToFar(BlockPos pos) {
			int speed = BeeEntity.this.isWithinDistance(pos, 3) ? 1 : 2;
			BeeEntity.this.navigation.setRangeMultiplier(10.0F);
			BeeEntity.this.navigation.startMovingTo(pos.getX(), pos.getY(), pos.getZ(), speed, 1.0);

			return BeeEntity.this.navigation.getCurrentPath() != null
					&& BeeEntity.this.navigation.getCurrentPath().reachesTarget();
		}

		boolean isPossibleHive(BlockPos pos) {
			return possibleHives.contains(pos);
		}

		private void addPossibleHive(BlockPos pos) {
			possibleHives.add(pos);

			while (possibleHives.size() > MAX_POSSIBLE_HIVES) {
				possibleHives.remove(0);
			}
		}

		void clearPossibleHives() {
			possibleHives.clear();
		}

		private void makeChosenHivePossibleHive() {
			if (BeeEntity.this.hivePos != null) {
				addPossibleHive(BeeEntity.this.hivePos);
			}

			BeeEntity.this.clearHivePos();
		}

		private boolean isCloseEnough(BlockPos pos) {
			if (BeeEntity.this.isWithinDistance(pos, 2)) {
				return true;
			}

			Path currentPath = BeeEntity.this.navigation.getCurrentPath();
			return currentPath != null
					&& currentPath.getTarget().equals(pos)
					&& currentPath.reachesTarget()
					&& currentPath.isFinished();
		}
	}

	abstract class NotAngryGoal extends Goal {

		/**
		 * Определяет, может ли цель начать выполнение при условии, что пчела не злится.
		 * Реализуется каждой конкретной целью с учётом её специфических условий.
		 */
		public abstract boolean canBeeStart();

		/**
		 * Определяет, может ли цель продолжать выполнение при условии, что пчела не злится.
		 */
		public abstract boolean canBeeContinue();

		@Override
		public boolean canStart() {
			return canBeeStart() && !BeeEntity.this.hasAngerTime();
		}

		@Override
		public boolean shouldContinue() {
			return canBeeContinue() && !BeeEntity.this.hasAngerTime();
		}
	}

	class PollinateGoal extends BeeEntity.NotAngryGoal {

		private static final int POLLINATION_TICKS = 400;
		private static final double VERTICAL_STEP = 0.1;
		private static final int FLOWER_SEARCH_RADIUS = 25;
		private static final float FLOWER_SEARCH_Y_MIN = 0.35F;
		private static final float FLOWER_SEARCH_Y_MAX = 0.6F;
		private static final float CHANGE_TARGET_CHANCE = 0.33333334F;
		private static final int MIN_FLOWER_DISTANCE = 5;
		private int pollinationTicks;
		private int lastPollinationTick;
		private boolean running;
		private @Nullable Vec3d nextTarget;
		private int ticks;
		private static final int MAX_POLLINATION_TICKS = 600;
		private Long2LongOpenHashMap unreachableFlowerPosCache = new Long2LongOpenHashMap();

		PollinateGoal() {
			setControls(EnumSet.of(Goal.Control.MOVE));
		}

		@Override
		public boolean canBeeStart() {
			if (BeeEntity.this.ticksUntilCanPollinate > 0) {
				return false;
			}

			if (BeeEntity.this.hasNectar()) {
				return false;
			}

			if (BeeEntity.this.getEntityWorld().isRaining()) {
				return false;
			}

			Optional<BlockPos> flower = getFlower();

			if (flower.isPresent()) {
				BeeEntity.this.flowerPos = flower.get();
				BeeEntity.this.navigation.startMovingTo(
					BeeEntity.this.flowerPos.getX() + 0.5,
					BeeEntity.this.flowerPos.getY() + 0.5,
					BeeEntity.this.flowerPos.getZ() + 0.5,
					1.2F
				);
				return true;
			}

			BeeEntity.this.ticksUntilCanPollinate = MathHelper.nextInt(
				BeeEntity.this.random, MIN_TICKS_UNTIL_POLLINATE, MAX_TICKS_UNTIL_POLLINATE_INIT
			);
			return false;
		}

		@Override
		public boolean canBeeContinue() {
			if (!running) {
				return false;
			}

			if (!BeeEntity.this.hasFlower()) {
				return false;
			}

			if (BeeEntity.this.getEntityWorld().isRaining()) {
				return false;
			}

			return completedPollination() ? BeeEntity.this.random.nextFloat() < 0.2F : true;
		}

		private boolean completedPollination() {
			return pollinationTicks > POLLINATION_TICKS;
		}

		boolean isRunning() {
			return running;
		}

		void cancel() {
			running = false;
		}

		@Override
		public void start() {
			pollinationTicks = 0;
			ticks = 0;
			lastPollinationTick = 0;
			running = true;
			BeeEntity.this.resetPollinationTicks();
		}

		@Override
		public void stop() {
			if (completedPollination()) {
				BeeEntity.this.setHasNectar(true);
			}

			running = false;
			BeeEntity.this.navigation.stop();
			BeeEntity.this.ticksUntilCanPollinate = MAX_TICKS_UNTIL_POLLINATE;
		}

		@Override
		public boolean shouldRunEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			if (!BeeEntity.this.hasFlower()) {
				return;
			}

			ticks++;

			if (ticks > FLOWER_NAVIGATION_START_TICKS) {
				BeeEntity.this.clearFlowerPos();
				running = false;
				BeeEntity.this.ticksUntilCanPollinate = MAX_TICKS_UNTIL_POLLINATE;
				return;
			}

			Vec3d flowerTop = Vec3d.ofBottomCenter(BeeEntity.this.flowerPos).add(0.0, FLOWER_SEARCH_Y_MAX, 0.0);

			if (flowerTop.distanceTo(BeeEntity.this.getEntityPos()) > 1.0) {
				nextTarget = flowerTop;
				moveToNextTarget();
				return;
			}

			if (nextTarget == null) {
				nextTarget = flowerTop;
			}

			boolean reachedTarget = BeeEntity.this.getEntityPos().distanceTo(nextTarget) <= VERTICAL_STEP;
			boolean shouldMove = true;

			if (!reachedTarget && ticks > FLOWER_NAVIGATION_START_TICKS) {
				BeeEntity.this.clearFlowerPos();
			}
			else {
				if (reachedTarget) {
					boolean shouldChangeTarget = BeeEntity.this.random.nextInt(FLOWER_SEARCH_RADIUS) == 0;

					if (shouldChangeTarget) {
						nextTarget = new Vec3d(
							flowerTop.getX() + getRandomOffset(),
							flowerTop.getY(),
							flowerTop.getZ() + getRandomOffset()
						);
						BeeEntity.this.navigation.stop();
					}
					else {
						shouldMove = false;
					}

					BeeEntity.this.getLookControl().lookAt(flowerTop.getX(), flowerTop.getY(), flowerTop.getZ());
				}

				if (shouldMove) {
					moveToNextTarget();
				}

				pollinationTicks++;

				if (BeeEntity.this.random.nextFloat() < 0.05F
						&& pollinationTicks > lastPollinationTick + MAX_TICKS_UNTIL_POLLINATE_INIT) {
					lastPollinationTick = pollinationTicks;
					BeeEntity.this.playSound(SoundEvents.ENTITY_BEE_POLLINATE, 1.0F, 1.0F);
				}
			}
		}

		private void moveToNextTarget() {
			BeeEntity.this.getMoveControl().moveTo(nextTarget.getX(), nextTarget.getY(), nextTarget.getZ(), FLOWER_SEARCH_Y_MIN);
		}

		private float getRandomOffset() {
			return (BeeEntity.this.random.nextFloat() * 2.0F - 1.0F) * CHANGE_TARGET_CHANCE;
		}

		/**
		 * Ищет ближайший доступный цветок в радиусе 5 блоков, используя кэш недостижимых позиций
		 * для пропуска позиций, к которым путь не был найден в течение {@link #FLOWER_NAVIGATION_START_TICKS} тиков.
		 */
		private Optional<BlockPos> getFlower() {
			Long2LongOpenHashMap freshCache = new Long2LongOpenHashMap();

			for (BlockPos blockPos : BlockPos.iterateOutwards(BeeEntity.this.getBlockPos(), 5, 5, 5)) {
				long cachedExpiry = unreachableFlowerPosCache.getOrDefault(blockPos.asLong(), Long.MIN_VALUE);

				if (BeeEntity.this.getEntityWorld().getTime() < cachedExpiry) {
					freshCache.put(blockPos.asLong(), cachedExpiry);
					continue;
				}

				if (BeeEntity.isAttractive(BeeEntity.this.getEntityWorld().getBlockState(blockPos))) {
					Path path = BeeEntity.this.navigation.findPathTo(blockPos, 1);

					if (path != null && path.reachesTarget()) {
						return Optional.of(blockPos);
					}

					freshCache.put(blockPos.asLong(), BeeEntity.this.getEntityWorld().getTime() + FLOWER_NAVIGATION_START_TICKS);
				}
			}

			unreachableFlowerPosCache = freshCache;
			return Optional.empty();
		}
	}

	class StingGoal extends MeleeAttackGoal {

		StingGoal(final PathAwareEntity mob, final double speed, final boolean pauseWhenMobIdle) {
			super(mob, speed, pauseWhenMobIdle);
		}

		@Override
		public boolean canStart() {
			return super.canStart() && BeeEntity.this.hasAngerTime() && !BeeEntity.this.hasStung();
		}

		@Override
		public boolean shouldContinue() {
			return super.shouldContinue() && BeeEntity.this.hasAngerTime() && !BeeEntity.this.hasStung();
		}
	}

	static class StingTargetGoal extends ActiveTargetGoal<PlayerEntity> {

		StingTargetGoal(BeeEntity bee) {
			super(bee, PlayerEntity.class, 10, true, false, bee::shouldAngerAt);
		}

		@Override
		public boolean canStart() {
			return canSting() && super.canStart();
		}

		@Override
		public boolean shouldContinue() {
			if (!canSting()) {
				target = null;
				return false;
			}

			return mob.getTarget() != null && super.shouldContinue();
		}

		private boolean canSting() {
			BeeEntity bee = (BeeEntity) mob;
			return bee.hasAngerTime() && !bee.hasStung();
		}
	}

	class ValidateFlowerGoal extends BeeEntity.NotAngryGoal {

		private final int ticksUntilNextValidate = MathHelper.nextInt(BeeEntity.this.random, MIN_TICKS_UNTIL_POLLINATE, 40);
		private long lastValidateTime = -1L;

		@Override
		public void start() {
			if (BeeEntity.this.flowerPos != null
					&& BeeEntity.this.getEntityWorld().isPosLoaded(BeeEntity.this.flowerPos)
					&& !isFlower(BeeEntity.this.flowerPos)) {
				BeeEntity.this.clearFlowerPos();
			}

			lastValidateTime = BeeEntity.this.getEntityWorld().getTime();
		}

		@Override
		public boolean canBeeStart() {
			return BeeEntity.this.getEntityWorld().getTime() > lastValidateTime + ticksUntilNextValidate;
		}

		@Override
		public boolean canBeeContinue() {
			return false;
		}

		private boolean isFlower(BlockPos pos) {
			return BeeEntity.isAttractive(BeeEntity.this.getEntityWorld().getBlockState(pos));
		}
	}

	class ValidateHiveGoal extends BeeEntity.NotAngryGoal {

		private final int ticksUntilNextValidate = MathHelper.nextInt(BeeEntity.this.random, MIN_TICKS_UNTIL_POLLINATE, 40);
		private long lastValidateTime = -1L;

		@Override
		public void start() {
			if (BeeEntity.this.hivePos != null
					&& BeeEntity.this.getEntityWorld().isPosLoaded(BeeEntity.this.hivePos)
					&& !BeeEntity.this.hasValidHive()) {
				BeeEntity.this.clearHivePos();
			}

			lastValidateTime = BeeEntity.this.getEntityWorld().getTime();
		}

		@Override
		public boolean canBeeStart() {
			return BeeEntity.this.getEntityWorld().getTime() > lastValidateTime + ticksUntilNextValidate;
		}

		@Override
		public boolean canBeeContinue() {
			return false;
		}
	}
}
