package net.minecraft.entity.mob;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.conversion.EntityConversionContext;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Holidays;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/**
 * Зомби — базовый класс для всех зомби-подобных мобов.
 * Поддерживает механику детёнышей (baby), взлома дверей, призыва подкреплений на Hard-сложности
 * и конвертации в утопленника при длительном нахождении под водой.
 */
public class ZombieEntity extends HostileEntity {

	public static final float REINFORCEMENT_CHANCE = 0.05F;
	public static final int BURN_TICKS = 50;
	public static final int CONVERSION_TICKS = 40;
	public static final int REINFORCEMENT_RADIUS = 7;
	private static final float CONVERSION_CHANCE = 0.1F;
	private static final int NO_CONVERSION_TIME = -1;
	private static final Identifier BABY_SPEED_MODIFIER_ID = Identifier.ofVanilla("baby");
	private static final EntityAttributeModifier BABY_SPEED_BONUS = new EntityAttributeModifier(
			BABY_SPEED_MODIFIER_ID, 0.5, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
	);
	private static final Identifier
			REINFORCEMENT_CALLER_CHARGE_MODIFIER_ID =
			Identifier.ofVanilla("reinforcement_caller_charge");
	private static final EntityAttributeModifier
			REINFORCEMENT_CALLEE_CHARGE_REINFORCEMENT_BONUS =
			new EntityAttributeModifier(
					Identifier.ofVanilla("reinforcement_callee_charge"),
					-REINFORCEMENT_CHANCE,
					EntityAttributeModifier.Operation.ADD_VALUE
			);
	private static final Identifier LEADER_ZOMBIE_BONUS_MODIFIER_ID = Identifier.ofVanilla("leader_zombie_bonus");
	private static final Identifier
			ZOMBIE_RANDOM_SPAWN_BONUS_MODIFIER_ID =
			Identifier.ofVanilla("zombie_random_spawn_bonus");
	private static final EntityDimensions
			BABY_BASE_DIMENSIONS =
			EntityType.ZOMBIE.getDimensions().scaled(0.5F).withEyeHeight(0.93F);
	private static final Predicate<Difficulty>
			DOOR_BREAK_DIFFICULTY_CHECKER =
			difficulty -> difficulty == Difficulty.HARD;
	private static final TrackedData<Boolean>
			BABY =
			DataTracker.registerData(ZombieEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Integer>
			ZOMBIE_TYPE =
			DataTracker.registerData(ZombieEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Boolean>
			CONVERTING_IN_WATER =
			DataTracker.registerData(ZombieEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private final BreakDoorGoal breakDoorsGoal = new BreakDoorGoal(this, DOOR_BREAK_DIFFICULTY_CHECKER);
	private boolean canBreakDoors;
	private int inWaterTime;
	private int ticksUntilWaterConversion;

	public ZombieEntity(EntityType<? extends ZombieEntity> entityType, World world) {
		super(entityType, world);
	}

	public ZombieEntity(World world) {
		this(EntityType.ZOMBIE, world);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(4, new ZombieEntity.DestroyEggGoal(this, 1.0, 3));
		goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
		goalSelector.add(8, new LookAroundGoal(this));
		initCustomGoals();
	}

	protected void initCustomGoals() {
		goalSelector.add(2, new ChargeKineticWeaponGoal<>(this, 1.0, 1.0, 10.0F, 2.0F));
		goalSelector.add(3, new ZombieAttackGoal(this, 1.0, false));
		goalSelector.add(6, new MoveThroughVillageGoal(this, 1.0, true, 4, this::canBreakDoors));
		goalSelector.add(7, new WanderAroundFarGoal(this, 1.0));
		targetSelector.add(1, new RevengeGoal(this).setGroupRevenge(ZombifiedPiglinEntity.class));
		targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
		targetSelector.add(3, new ActiveTargetGoal<>(this, MerchantEntity.class, false));
		targetSelector.add(3, new ActiveTargetGoal<>(this, IronGolemEntity.class, true));
		targetSelector.add(
				5,
				new ActiveTargetGoal<>(
						this,
						TurtleEntity.class,
						10,
						true,
						false,
						TurtleEntity.BABY_TURTLE_ON_LAND_FILTER
				)
		);
	}

	public static DefaultAttributeContainer.Builder createZombieAttributes() {
		return HostileEntity.createHostileAttributes()
		                    .add(EntityAttributes.FOLLOW_RANGE, 35.0)
		                    .add(EntityAttributes.MOVEMENT_SPEED, 0.23F)
		                    .add(EntityAttributes.ATTACK_DAMAGE, 3.0)
		                    .add(EntityAttributes.ARMOR, 2.0)
		                    .add(EntityAttributes.SPAWN_REINFORCEMENTS);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(BABY, false);
		builder.add(ZOMBIE_TYPE, 0);
		builder.add(CONVERTING_IN_WATER, false);
	}

	public boolean isConvertingInWater() {
		return getDataTracker().get(CONVERTING_IN_WATER);
	}

	public boolean canBreakDoors() {
		return canBreakDoors;
	}

	/**
	 * Включает или отключает способность зомби ломать двери.
	 * Синхронизирует состояние с навигацией и целевым списком задач.
	 */
	public void setCanBreakDoors(boolean canBreakDoors) {
		if (navigation.canControlOpeningDoors()) {
			if (this.canBreakDoors != canBreakDoors) {
				this.canBreakDoors = canBreakDoors;
				navigation.setCanOpenDoors(canBreakDoors);

				if (canBreakDoors) {
					goalSelector.add(1, breakDoorsGoal);
				}
				else {
					goalSelector.remove(breakDoorsGoal);
				}
			}
		}
		else if (this.canBreakDoors) {
			goalSelector.remove(breakDoorsGoal);
			this.canBreakDoors = false;
		}
	}

	@Override
	public boolean isBaby() {
		return getDataTracker().get(BABY);
	}

	@Override
	protected int getExperienceToDrop(ServerWorld world) {
		if (isBaby()) {
			experiencePoints = (int) (experiencePoints * 2.5);
		}

		return super.getExperienceToDrop(world);
	}

	@Override
	public void setBaby(boolean baby) {
		getDataTracker().set(BABY, baby);

		if (getEntityWorld() == null || getEntityWorld().isClient()) {
			return;
		}

		EntityAttributeInstance speedAttribute = getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
		speedAttribute.removeModifier(BABY_SPEED_MODIFIER_ID);

		if (baby) {
			speedAttribute.addTemporaryModifier(BABY_SPEED_BONUS);
		}
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		if (BABY.equals(data)) {
			calculateDimensions();
		}

		super.onTrackedDataSet(data);
	}

	protected boolean canConvertInWater() {
		return true;
	}

	@Override
	public void tick() {
		if (getEntityWorld() instanceof ServerWorld serverWorld && isAlive() && !isAiDisabled()) {
			if (isConvertingInWater()) {
				ticksUntilWaterConversion--;

				if (ticksUntilWaterConversion < 0) {
					convertInWater(serverWorld);
				}
			}
			else if (canConvertInWater()) {
				if (isSubmergedIn(FluidTags.WATER)) {
					inWaterTime++;

					if (inWaterTime >= 600) {
						setTicksUntilWaterConversion(300);
					}
				}
				else {
					inWaterTime = -1;
				}
			}
		}

		super.tick();
	}

	private void setTicksUntilWaterConversion(int ticks) {
		ticksUntilWaterConversion = ticks;
		getDataTracker().set(CONVERTING_IN_WATER, true);
	}

	protected void convertInWater(ServerWorld world) {
		convertTo(world, EntityType.DROWNED);

		if (!isSilent()) {
			world.syncWorldEvent(null, 1040, getBlockPos(), 0);
		}
	}

	protected void convertTo(ServerWorld world, EntityType<? extends ZombieEntity> entityType) {
		convertTo(
				entityType,
				EntityConversionContext.create(this, true, true),
				newZombie -> newZombie.applyAttributeModifiers(
						world.getLocalDifficulty(newZombie.getBlockPos()).getClampedLocalDifficulty()
				)
		);
	}

	/**
	 * Заражает жителя, превращая его в зомби-жителя с сохранением профессии, сплетен и торговых предложений.
	 */
	@VisibleForTesting
	public boolean infectVillager(ServerWorld world, VillagerEntity villager) {
		ZombieVillagerEntity zombieVillager = villager.convertTo(
				EntityType.ZOMBIE_VILLAGER,
				EntityConversionContext.create(villager, true, true),
				converted -> {
					converted.initialize(
							world,
							world.getLocalDifficulty(converted.getBlockPos()),
							SpawnReason.CONVERSION,
							new ZombieEntity.ZombieData(false, true)
					);
					converted.setVillagerData(villager.getVillagerData());
					converted.setGossip(villager.getGossip().copy());
					converted.setOfferData(villager.getOffers().copy());
					converted.setExperience(villager.getExperience());

						if (!isSilent()) {
							world.syncWorldEvent(null, 1026, getBlockPos(), 0);
						}
				}
		);
		return zombieVillager != null;
	}

	protected boolean burnsInDaylight() {
		return true;
	}

	/**
	 * При получении урона на Hard-сложности с шансом призывает зомби-подкрепление в радиусе 7 блоков.
	 * Каждый призыв снижает атрибут {@code SPAWN_REINFORCEMENTS} у призывателя.
	 */
	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		if (!super.damage(world, source, amount)) {
			return false;
		}

		LivingEntity attacker = getTarget();
		if (attacker == null && source.getAttacker() instanceof LivingEntity livingAttacker) {
			attacker = livingAttacker;
		}

		if (attacker == null
				|| world.getDifficulty() != Difficulty.HARD
				|| random.nextFloat() >= getAttributeValue(EntityAttributes.SPAWN_REINFORCEMENTS)
				|| !world.shouldSpawnMonsters()
		) {
			return true;
		}

		int baseX = MathHelper.floor(getX());
		int baseY = MathHelper.floor(getY());
		int baseZ = MathHelper.floor(getZ());
		EntityType<? extends ZombieEntity> entityType = getType();
		ZombieEntity reinforcement = entityType.create(world, SpawnReason.REINFORCEMENT);

		if (reinforcement == null) {
			return true;
		}

		for (int attempt = 0; attempt < BURN_TICKS; attempt++) {
			int rx = baseX + MathHelper.nextInt(random, 7, CONVERSION_TICKS) * MathHelper.nextInt(random, -1, 1);
			int ry = baseY + MathHelper.nextInt(random, 7, CONVERSION_TICKS) * MathHelper.nextInt(random, -1, 1);
			int rz = baseZ + MathHelper.nextInt(random, 7, CONVERSION_TICKS) * MathHelper.nextInt(random, -1, 1);
			BlockPos spawnPos = new BlockPos(rx, ry, rz);

			if (!SpawnRestriction.isSpawnPosAllowed(entityType, world, spawnPos)) {
				continue;
			}

			if (!SpawnRestriction.canSpawn(entityType, world, SpawnReason.REINFORCEMENT, spawnPos, world.random)) {
				continue;
			}

			reinforcement.setPosition(rx, ry, rz);

			if (world.isPlayerInRange(rx, ry, rz, 7.0)
					|| !world.doesNotIntersectEntities(reinforcement)
					|| !world.isSpaceEmpty(reinforcement)
					|| (!reinforcement.canSpawnAsReinforcementInFluid() && world.containsFluid(reinforcement.getBoundingBox()))) {
				continue;
			}

			reinforcement.setTarget(attacker);
			reinforcement.initialize(world, world.getLocalDifficulty(reinforcement.getBlockPos()), SpawnReason.REINFORCEMENT, null);
			world.spawnEntityAndPassengers(reinforcement);

			EntityAttributeInstance callerAttr = getAttributeInstance(EntityAttributes.SPAWN_REINFORCEMENTS);
			EntityAttributeModifier existing = callerAttr.getModifier(REINFORCEMENT_CALLER_CHARGE_MODIFIER_ID);
			double currentCharge = existing != null ? existing.value() : 0.0;
			callerAttr.removeModifier(REINFORCEMENT_CALLER_CHARGE_MODIFIER_ID);
			callerAttr.addPersistentModifier(
					new EntityAttributeModifier(REINFORCEMENT_CALLER_CHARGE_MODIFIER_ID, currentCharge - REINFORCEMENT_CHANCE, EntityAttributeModifier.Operation.ADD_VALUE)
			);
			reinforcement.getAttributeInstance(EntityAttributes.SPAWN_REINFORCEMENTS)
			             .addPersistentModifier(REINFORCEMENT_CALLEE_CHARGE_REINFORCEMENT_BONUS);
			break;
		}

		return true;
	}

	@Override
	public boolean tryAttack(ServerWorld world, Entity target) {
		boolean attacked = super.tryAttack(world, target);

		if (attacked) {
			float localDifficulty = world.getLocalDifficulty(getBlockPos()).getLocalDifficulty();

			if (getMainHandStack().isEmpty() && isOnFire() && random.nextFloat() < localDifficulty * 0.3F) {
				target.setOnFireFor(2 * (int) localDifficulty);
			}
		}

		return attacked;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_ZOMBIE_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_ZOMBIE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_ZOMBIE_DEATH;
	}

	protected SoundEvent getStepSound() {
		return SoundEvents.ENTITY_ZOMBIE_STEP;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(getStepSound(), 0.15F, 1.0F);
	}

	@Override
	public EntityType<? extends ZombieEntity> getType() {
		return (EntityType<? extends ZombieEntity>) super.getType();
	}

	protected boolean canSpawnAsReinforcementInFluid() {
		return false;
	}

	@Override
	protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
		super.initEquipment(random, localDifficulty);
		float weaponChance = getEntityWorld().getDifficulty() == Difficulty.HARD ? REINFORCEMENT_CHANCE : 0.01F;

		if (random.nextFloat() >= weaponChance) {
			return;
		}

		int weaponRoll = random.nextInt(6);

		if (weaponRoll == 0) {
			equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
		}
		else if (weaponRoll == 1) {
			equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SPEAR));
		}
		else {
			equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SHOVEL));
		}
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putBoolean("IsBaby", isBaby());
		view.putBoolean("CanBreakDoors", canBreakDoors());
		view.putInt("InWaterTime", isTouchingWater() ? inWaterTime : -1);
		view.putInt("DrownedConversionTime", isConvertingInWater() ? ticksUntilWaterConversion : -1);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		setBaby(view.getBoolean("IsBaby", false));
		setCanBreakDoors(view.getBoolean("CanBreakDoors", false));
		inWaterTime = view.getInt("InWaterTime", 0);
		int conversionTime = view.getInt("DrownedConversionTime", NO_CONVERSION_TIME);

		if (conversionTime != NO_CONVERSION_TIME) {
			setTicksUntilWaterConversion(conversionTime);
		}
		else {
			getDataTracker().set(CONVERTING_IN_WATER, false);
		}
	}

	@Override
	public boolean onKilledOther(ServerWorld world, LivingEntity other, DamageSource damageSource) {
		boolean killed = super.onKilledOther(world, other, damageSource);

		if ((world.getDifficulty() == Difficulty.NORMAL || world.getDifficulty() == Difficulty.HARD)
				&& other instanceof VillagerEntity villagerEntity) {
			if (world.getDifficulty() != Difficulty.HARD && random.nextBoolean()) {
				return killed;
			}

			if (infectVillager(world, villagerEntity)) {
				killed = false;
			}
		}

		return killed;
	}

	@Override
	public EntityDimensions getBaseDimensions(EntityPose pose) {
		return isBaby() ? BABY_BASE_DIMENSIONS : super.getBaseDimensions(pose);
	}

	@Override
	public boolean canPickupItem(ItemStack stack) {
		if (stack.isIn(ItemTags.EGGS) && isBaby() && hasVehicle()) {
			return false;
		}

		return super.canPickupItem(stack);
	}

	@Override
	public boolean canGather(ServerWorld world, ItemStack stack) {
		if (stack.isOf(Items.GLOW_INK_SAC)) {
			return false;
		}

		return super.canGather(world, stack);
	}

	@Override
	public @Nullable EntityData initialize(
			ServerWorldAccess world,
			LocalDifficulty difficulty,
			SpawnReason spawnReason,
			@Nullable EntityData entityData
	) {
		Random spawnRandom = world.getRandom();
		entityData = super.initialize(world, difficulty, spawnReason, entityData);
		float clampedDifficulty = difficulty.getClampedLocalDifficulty();

		if (spawnReason != SpawnReason.CONVERSION) {
			setCanPickUpLoot(spawnRandom.nextFloat() < 0.55F * clampedDifficulty);
		}

		if (entityData == null) {
			entityData = new ZombieEntity.ZombieData(shouldBeBaby(spawnRandom), true);
		}

		if (entityData instanceof ZombieEntity.ZombieData zombieData) {
			if (zombieData.baby) {
				setBaby(true);

				if (zombieData.tryChickenJockey) {
					if (spawnRandom.nextFloat() < REINFORCEMENT_CHANCE) {
						List<ChickenEntity> nearbyChickens = world.getEntitiesByClass(
								ChickenEntity.class,
								getBoundingBox().expand(5.0, 3.0, 5.0),
								EntityPredicates.NOT_MOUNTED
						);

						if (!nearbyChickens.isEmpty()) {
							ChickenEntity chicken = nearbyChickens.get(0);
							chicken.setHasJockey(true);
							startRiding(chicken, false, false);
						}
					}
					else if (spawnRandom.nextFloat() < REINFORCEMENT_CHANCE) {
						ChickenEntity spawnedChicken = EntityType.CHICKEN.create(getEntityWorld(), SpawnReason.JOCKEY);

						if (spawnedChicken != null) {
							spawnedChicken.refreshPositionAndAngles(getX(), getY(), getZ(), getYaw(), 0.0F);
							spawnedChicken.initialize(world, difficulty, SpawnReason.JOCKEY, null);
							spawnedChicken.setHasJockey(true);
							startRiding(spawnedChicken, false, false);
							world.spawnEntity(spawnedChicken);
						}
					}
				}
			}

			setCanBreakDoors(spawnRandom.nextFloat() < clampedDifficulty * CONVERSION_CHANCE);

			if (spawnReason != SpawnReason.CONVERSION) {
				initEquipment(spawnRandom, difficulty);
				updateEnchantments(world, spawnRandom, difficulty);
			}
		}

		if (getEquippedStack(EquipmentSlot.HEAD).isEmpty() && Holidays.isHalloween() && spawnRandom.nextFloat() < 0.25F) {
			equipStack(
					EquipmentSlot.HEAD,
					new ItemStack(spawnRandom.nextFloat() < CONVERSION_CHANCE ? Blocks.JACK_O_LANTERN : Blocks.CARVED_PUMPKIN)
			);
			setEquipmentDropChance(EquipmentSlot.HEAD, 0.0F);
		}

		applyAttributeModifiers(clampedDifficulty);
		return entityData;
	}

	@VisibleForTesting
	public void setInWaterTime(int inWaterTime) {
		this.inWaterTime = inWaterTime;
	}

	@VisibleForTesting
	public void setTicksUntilWaterConversionDirect(int ticks) {
		ticksUntilWaterConversion = ticks;
	}

	public static boolean shouldBeBaby(Random random) {
		return random.nextFloat() < REINFORCEMENT_CHANCE;
	}

	/**
	 * Применяет случайные модификаторы атрибутов при спавне: сопротивление отбрасыванию,
	 * дальность обнаружения и шанс стать лидером (с повышенным здоровьем и способностью ломать двери).
	 */
	protected void applyAttributeModifiers(float difficultyMultiplier) {
		initAttributes();
		getAttributeInstance(EntityAttributes.KNOCKBACK_RESISTANCE)
				.overwritePersistentModifier(
						new EntityAttributeModifier(RANDOM_SPAWN_BONUS_MODIFIER_ID, random.nextDouble() * REINFORCEMENT_CHANCE, EntityAttributeModifier.Operation.ADD_VALUE)
				);
		double followRangeBonus = random.nextDouble() * 1.5 * difficultyMultiplier;

		if (followRangeBonus > 1.0) {
			getAttributeInstance(EntityAttributes.FOLLOW_RANGE)
					.overwritePersistentModifier(
							new EntityAttributeModifier(ZOMBIE_RANDOM_SPAWN_BONUS_MODIFIER_ID, followRangeBonus, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
					);
		}

		if (random.nextFloat() < difficultyMultiplier * REINFORCEMENT_CHANCE) {
			getAttributeInstance(EntityAttributes.SPAWN_REINFORCEMENTS)
					.overwritePersistentModifier(
							new EntityAttributeModifier(LEADER_ZOMBIE_BONUS_MODIFIER_ID, random.nextDouble() * 0.25 + 0.5, EntityAttributeModifier.Operation.ADD_VALUE)
					);
			getAttributeInstance(EntityAttributes.MAX_HEALTH)
					.overwritePersistentModifier(
							new EntityAttributeModifier(LEADER_ZOMBIE_BONUS_MODIFIER_ID, random.nextDouble() * 3.0 + 1.0, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
					);
			setCanBreakDoors(true);
		}
	}

	protected void initAttributes() {
		getAttributeInstance(EntityAttributes.SPAWN_REINFORCEMENTS).setBaseValue(random.nextDouble() * CONVERSION_CHANCE);
	}

	class DestroyEggGoal extends StepAndDestroyBlockGoal {

		DestroyEggGoal(final PathAwareEntity mob, final double speed, final int maxYDifference) {
			super(Blocks.TURTLE_EGG, mob, speed, maxYDifference);
		}

		@Override
		public void tickStepping(WorldAccess world, BlockPos pos) {
			world.playSound(
					null,
					pos,
					SoundEvents.ENTITY_ZOMBIE_DESTROY_EGG,
					SoundCategory.HOSTILE,
					0.5F,
					0.9F + ZombieEntity.this.random.nextFloat() * 0.2F
			);
		}

		@Override
		public void onDestroyBlock(World world, BlockPos pos) {
			world.playSound(
					null,
					pos,
					SoundEvents.ENTITY_TURTLE_EGG_BREAK,
					SoundCategory.BLOCKS,
					0.7F,
					0.9F + world.random.nextFloat() * 0.2F
			);
		}

		@Override
		public double getDesiredDistanceToTarget() {
			return 1.14;
		}
	}

	public static class ZombieData implements EntityData {

		public final boolean baby;
		public final boolean tryChickenJockey;

		public ZombieData(boolean baby, boolean tryChickenJockey) {
			this.baby = baby;
			this.tryChickenJockey = tryChickenJockey;
		}
	}
}
