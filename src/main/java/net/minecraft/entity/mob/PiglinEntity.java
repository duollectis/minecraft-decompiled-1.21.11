package net.minecraft.entity.mob;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Dynamic;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Пиглин — агрессивный житель Нижнего мира, торгующий золотом и охотящийся на хоглинов.
 * Использует Brain-архитектуру ({@link PiglinBrain}) для управления поведением:
 * бартер, охота, атака, танец при получении золота.
 * Детёныши пиглинов не охотятся и имеют уменьшенные размеры.
 */
public class PiglinEntity extends AbstractPiglinEntity implements CrossbowUser, InventoryOwner {

	private static final TrackedData<Boolean>
			BABY =
			DataTracker.registerData(PiglinEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean>
			CHARGING =
			DataTracker.registerData(PiglinEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean>
			DANCING =
			DataTracker.registerData(PiglinEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final Identifier BABY_SPEED_BOOST_ID = Identifier.ofVanilla("baby");
	private static final EntityAttributeModifier BABY_SPEED_BOOST = new EntityAttributeModifier(
			BABY_SPEED_BOOST_ID, 0.2F, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
	);
	private static final int DETECTION_RANGE = 16;
	private static final float MOVEMENT_SPEED = 0.35F;
	private static final float BARTER_CHANCE = 0.35F;
	private static final int BARTER_COOLDOWN_TICKS = 5;
	private static final float ARMOR_EQUIP_CHANCE = 0.1F;
	private static final int DANCE_TICKS = 3;
	private static final float HUNT_CHANCE = 0.2F;
	private static final float INITIAL_WEAPON_CROSSBOW_CHANCE = 0.5F;
	private static final int GOLDEN_SPEAR_RARITY = 10;
	private static final EntityDimensions
			BABY_BASE_DIMENSIONS =
			EntityType.PIGLIN.getDimensions().scaled(0.5F).withEyeHeight(0.97F);
	private static final double BABY_SPEED = 0.5;
	private final SimpleInventory inventory = new SimpleInventory(8);
	private boolean cannotHunt;
	protected static final ImmutableList<SensorType<? extends Sensor<? super PiglinEntity>>>
			SENSOR_TYPES =
			ImmutableList.of(
					SensorType.NEAREST_LIVING_ENTITIES,
					SensorType.NEAREST_PLAYERS,
					SensorType.NEAREST_ITEMS,
					SensorType.HURT_BY,
					SensorType.PIGLIN_SPECIFIC_SENSOR
			);
	protected static final ImmutableList<MemoryModuleType<?>> MEMORY_MODULE_TYPES = ImmutableList.of(
			MemoryModuleType.LOOK_TARGET,
			MemoryModuleType.DOORS_TO_CLOSE,
			MemoryModuleType.MOBS,
			MemoryModuleType.VISIBLE_MOBS,
			MemoryModuleType.NEAREST_VISIBLE_PLAYER,
			MemoryModuleType.NEAREST_VISIBLE_TARGETABLE_PLAYER,
			MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS,
			MemoryModuleType.NEARBY_ADULT_PIGLINS,
			MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM,
			MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS,
			MemoryModuleType.HURT_BY,
			MemoryModuleType.HURT_BY_ENTITY,
			new MemoryModuleType[]{
					MemoryModuleType.WALK_TARGET,
					MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
					MemoryModuleType.ATTACK_TARGET,
					MemoryModuleType.ATTACK_COOLING_DOWN,
					MemoryModuleType.INTERACTION_TARGET,
					MemoryModuleType.PATH,
					MemoryModuleType.ANGRY_AT,
					MemoryModuleType.UNIVERSAL_ANGER,
					MemoryModuleType.AVOID_TARGET,
					MemoryModuleType.ADMIRING_ITEM,
					MemoryModuleType.TIME_TRYING_TO_REACH_ADMIRE_ITEM,
					MemoryModuleType.ADMIRING_DISABLED,
					MemoryModuleType.DISABLE_WALK_TO_ADMIRE_ITEM,
					MemoryModuleType.CELEBRATE_LOCATION,
					MemoryModuleType.DANCING,
					MemoryModuleType.HUNTED_RECENTLY,
					MemoryModuleType.NEAREST_VISIBLE_BABY_HOGLIN,
					MemoryModuleType.NEAREST_VISIBLE_NEMESIS,
					MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED,
					MemoryModuleType.RIDE_TARGET,
					MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT,
					MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT,
					MemoryModuleType.NEAREST_VISIBLE_HUNTABLE_HOGLIN,
					MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD,
					MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM,
					MemoryModuleType.ATE_RECENTLY,
					MemoryModuleType.NEAREST_REPELLENT,
					MemoryModuleType.SPEAR_FLEEING_TIME,
					MemoryModuleType.SPEAR_FLEEING_POSITION,
					MemoryModuleType.SPEAR_CHARGE_POSITION,
					MemoryModuleType.SPEAR_ENGAGE_TIME,
					MemoryModuleType.SPEAR_STATUS
			}
	);

	public PiglinEntity(EntityType<? extends AbstractPiglinEntity> entityType, World world) {
		super(entityType, world);
		experiencePoints = 5;
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putBoolean("IsBaby", isBaby());
		view.putBoolean("CannotHunt", cannotHunt);
		writeInventory(view);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		setBaby(view.getBoolean("IsBaby", false));
		setCannotHunt(view.getBoolean("CannotHunt", false));
		readInventory(view);
	}

	@Debug
	@Override
	public SimpleInventory getInventory() {
		return inventory;
	}

	@Override
	protected void dropEquipment(ServerWorld world, DamageSource source, boolean causedByPlayer) {
		super.dropEquipment(world, source, causedByPlayer);
		inventory.clearToList().forEach(stack -> dropStack(world, stack));
	}

	protected ItemStack addItem(ItemStack stack) {
		return inventory.addStack(stack);
	}

	protected boolean canInsertIntoInventory(ItemStack stack) {
		return inventory.canInsert(stack);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(BABY, false);
		builder.add(CHARGING, false);
		builder.add(DANCING, false);
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		super.onTrackedDataSet(data);

		if (BABY.equals(data)) {
			calculateDimensions();
		}
	}

	public static DefaultAttributeContainer.Builder createPiglinAttributes() {
		return HostileEntity.createHostileAttributes()
		                    .add(EntityAttributes.MAX_HEALTH, 16.0)
		                    .add(EntityAttributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
		                    .add(EntityAttributes.ATTACK_DAMAGE, 5.0);
	}

	public static boolean canSpawn(
			EntityType<PiglinEntity> type,
			WorldAccess world,
			SpawnReason spawnReason,
			BlockPos pos,
			Random random
	) {
		return !world.getBlockState(pos.down()).isOf(Blocks.NETHER_WART_BLOCK);
	}

	/**
	 * Инициализирует пиглина при спавне: с шансом {@link #HUNT_CHANCE} создаёт детёныша,
	 * иначе выдаёт взрослому начальное оружие. Устанавливает флаг недавней охоты.
	 */
	@Override
	public @Nullable EntityData initialize(
			ServerWorldAccess world,
			LocalDifficulty difficulty,
			SpawnReason spawnReason,
			@Nullable EntityData entityData
	) {
		Random random = world.getRandom();

		if (spawnReason != SpawnReason.STRUCTURE) {
			if (random.nextFloat() < HUNT_CHANCE) {
				setBaby(true);
			}
			else if (isAdult()) {
				equipStack(EquipmentSlot.MAINHAND, makeInitialWeapon());
			}
		}

		PiglinBrain.setHuntedRecently(this, world.getRandom());
		initEquipment(random, difficulty);
		updateEnchantments(world, random, difficulty);

		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return !isPersistent();
	}

	@Override
	protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
		if (isAdult()) {
			equipAtChance(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET), random);
			equipAtChance(EquipmentSlot.CHEST, new ItemStack(Items.GOLDEN_CHESTPLATE), random);
			equipAtChance(EquipmentSlot.LEGS, new ItemStack(Items.GOLDEN_LEGGINGS), random);
			equipAtChance(EquipmentSlot.FEET, new ItemStack(Items.GOLDEN_BOOTS), random);
		}
	}

	private void equipAtChance(EquipmentSlot slot, ItemStack stack, Random random) {
		if (random.nextFloat() < ARMOR_EQUIP_CHANCE) {
			equipStack(slot, stack);
		}
	}

	@Override
	protected Brain.Profile<PiglinEntity> createBrainProfile() {
		return Brain.createProfile(MEMORY_MODULE_TYPES, SENSOR_TYPES);
	}

	@Override
	protected Brain<?> deserializeBrain(Dynamic<?> dynamic) {
		return PiglinBrain.create(this, createBrainProfile().deserialize(dynamic));
	}

	@Override
	public Brain<PiglinEntity> getBrain() {
		return (Brain<PiglinEntity>) super.getBrain();
	}

	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		ActionResult actionResult = super.interactMob(player, hand);

		if (actionResult.isAccepted()) {
			return actionResult;
		}

		if (getEntityWorld() instanceof ServerWorld serverWorld) {
			return PiglinBrain.playerInteract(serverWorld, this, player, hand);
		}

		boolean willingToTrade = PiglinBrain.isWillingToTrade(this, player.getStackInHand(hand))
				&& getActivity() != PiglinActivity.ADMIRING_ITEM;

		return willingToTrade ? ActionResult.SUCCESS : ActionResult.PASS;
	}

	@Override
	public EntityDimensions getBaseDimensions(EntityPose pose) {
		return isBaby() ? BABY_BASE_DIMENSIONS : super.getBaseDimensions(pose);
	}

	/**
	 * Устанавливает флаг детёныша и применяет/снимает модификатор скорости {@link #BABY_SPEED_BOOST}.
	 */
	@Override
	public void setBaby(boolean baby) {
		getDataTracker().set(BABY, baby);

		if (getEntityWorld().isClient()) {
			return;
		}

		EntityAttributeInstance speedAttribute = getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
		speedAttribute.removeModifier(BABY_SPEED_BOOST.id());

		if (baby) {
			speedAttribute.addTemporaryModifier(BABY_SPEED_BOOST);
		}
	}

	@Override
	public boolean isBaby() {
		return getDataTracker().get(BABY);
	}

	private void setCannotHunt(boolean cannotHunt) {
		this.cannotHunt = cannotHunt;
	}

	@Override
	protected boolean canHunt() {
		return !cannotHunt;
	}

	@Override
	protected void mobTick(ServerWorld world) {
		Profiler profiler = Profilers.get();
		profiler.push("piglinBrain");
		getBrain().tick(world, this);
		profiler.pop();
		PiglinBrain.tickActivities(this);
		super.mobTick(world);
	}

	@Override
	protected int getExperienceToDrop(ServerWorld world) {
		return experiencePoints;
	}

	@Override
	protected void zombify(ServerWorld world) {
		PiglinBrain.pickupItemWithOffHand(world, this);
		inventory.clearToList().forEach(stack -> dropStack(world, stack));
		super.zombify(world);
	}

	private ItemStack makeInitialWeapon() {
		if (random.nextFloat() < INITIAL_WEAPON_CROSSBOW_CHANCE) {
			return new ItemStack(Items.CROSSBOW);
		}

		Item meleeWeapon = random.nextInt(GOLDEN_SPEAR_RARITY) == 0 ? Items.GOLDEN_SPEAR : Items.GOLDEN_SWORD;
		return new ItemStack(meleeWeapon);
	}

	@Override
	public @Nullable TagKey<Item> getPreferredWeapons() {
		return isBaby() ? null : ItemTags.PIGLIN_PREFERRED_WEAPONS;
	}

	private boolean isCharging() {
		return dataTracker.get(CHARGING);
	}

	@Override
	public void setCharging(boolean charging) {
		dataTracker.set(CHARGING, charging);
	}

	@Override
	public void postShoot() {
		despawnCounter = 0;
	}

	@Override
	public PiglinActivity getActivity() {
		if (isDancing()) {
			return PiglinActivity.DANCING;
		}

		if (PiglinBrain.isGoldenItem(getOffHandStack())) {
			return PiglinActivity.ADMIRING_ITEM;
		}

		if (isAttacking() && isHoldingTool()) {
			return PiglinActivity.ATTACKING_WITH_MELEE_WEAPON;
		}

		if (isCharging()) {
			return PiglinActivity.CROSSBOW_CHARGE;
		}

		return isHolding(Items.CROSSBOW) && CrossbowItem.isCharged(getWeaponStack())
			? PiglinActivity.CROSSBOW_HOLD
			: PiglinActivity.DEFAULT;
	}

	public boolean isDancing() {
		return dataTracker.get(DANCING);
	}

	public void setDancing(boolean dancing) {
		dataTracker.set(DANCING, dancing);
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		boolean damaged = super.damage(world, source, amount);

		if (damaged && source.getAttacker() instanceof LivingEntity attacker) {
			PiglinBrain.onAttacked(world, this, attacker);
		}

		return damaged;
	}

	@Override
	public void shootAt(LivingEntity target, float pullProgress) {
		shoot(this, 1.6F);
	}

	@Override
	public boolean canUseRangedWeapon(ItemStack stack) {
		return stack.getItem() == Items.CROSSBOW || stack.contains(DataComponentTypes.KINETIC_WEAPON);
	}

	/**
	 * Экипирует предмет в основную руку через механизм лута (с учётом шансов выпадения).
	 */
	protected void equipToMainHand(ItemStack stack) {
		equipLootStack(EquipmentSlot.MAINHAND, stack);
	}

	/**
	 * Экипирует предмет в левую руку: предмет бартера идёт с гарантированным дропом,
	 * остальные — через механизм лута.
	 */
	protected void equipToOffHand(ItemStack stack) {
		if (stack.isOf(PiglinBrain.BARTERING_ITEM)) {
			equipStack(EquipmentSlot.OFFHAND, stack);
			setDropGuaranteed(EquipmentSlot.OFFHAND);
		}
		else {
			equipLootStack(EquipmentSlot.OFFHAND, stack);
		}
	}

	@Override
	public boolean canGather(ServerWorld world, ItemStack stack) {
		return world.getGameRules().getValue(GameRules.DO_MOB_GRIEFING)
				&& canPickUpLoot()
				&& PiglinBrain.canGather(this, stack);
	}

	/**
	 * Проверяет, может ли пиглин надеть предмет: сравнивает новый предмет с текущим
	 * в соответствующем слоте через {@link #prefersNewEquipment}.
	 */
	protected boolean canEquipStack(ItemStack stack) {
		EquipmentSlot slot = getPreferredEquipmentSlot(stack);
		ItemStack current = getEquippedStack(slot);
		return prefersNewEquipment(stack, current, slot);
	}

	/**
	 * Определяет приоритет нового предмета над текущим: золотые предметы всегда предпочтительнее
	 * не-золотых, а предметы из тега предпочтительного оружия — над остальными.
	 * Зачарование {@code PREVENT_ARMOR_CHANGE} блокирует замену.
	 */
	@Override
	protected boolean prefersNewEquipment(ItemStack newStack, ItemStack currentStack, EquipmentSlot slot) {
		if (EnchantmentHelper.hasAnyEnchantmentsWith(
				currentStack,
				EnchantmentEffectComponentTypes.PREVENT_ARMOR_CHANGE
		)) {
			return false;
		}

		TagKey<Item> preferredWeapons = getPreferredWeapons();
		boolean newIsGolden = PiglinBrain.isGoldenItem(newStack)
				|| preferredWeapons != null && newStack.isIn(preferredWeapons);
		boolean currentIsGolden = PiglinBrain.isGoldenItem(currentStack)
				|| preferredWeapons != null && currentStack.isIn(preferredWeapons);

		if (newIsGolden && !currentIsGolden) {
			return true;
		}

		if (newIsGolden || currentIsGolden) {
			return !newIsGolden;
		}

		return super.prefersNewEquipment(newStack, currentStack, slot);
	}

	@Override
	protected void loot(ServerWorld world, ItemEntity itemEntity) {
		triggerItemPickedUpByEntityCriteria(itemEntity);
		PiglinBrain.loot(world, this, itemEntity);
	}

	@Override
	public boolean startRiding(Entity entity, boolean force, boolean emitEvent) {
		if (isBaby() && entity.getType() == EntityType.HOGLIN) {
			entity = getTopMostPassenger(entity, 3);
		}

		return super.startRiding(entity, force, emitEvent);
	}

	private Entity getTopMostPassenger(Entity entity, int maxLevel) {
		List<Entity> passengers = entity.getPassengerList();
		return maxLevel != 1 && !passengers.isEmpty()
			? getTopMostPassenger(passengers.getFirst(), maxLevel - 1)
			: entity;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return getEntityWorld().isClient() ? null : PiglinBrain.getCurrentActivitySound(this).orElse(null);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_PIGLIN_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_PIGLIN_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.ENTITY_PIGLIN_STEP, 0.15F, 1.0F);
	}

	@Override
	protected void playZombificationSound() {
		playSound(SoundEvents.ENTITY_PIGLIN_CONVERTED_TO_ZOMBIFIED);
	}
}
