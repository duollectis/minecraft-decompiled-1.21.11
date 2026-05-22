package net.minecraft.entity.mob;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.component.type.AttackRangeComponent;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.UseRemainderComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.provider.EnchantmentProviders;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.control.BodyControl;
import net.minecraft.entity.ai.control.JumpControl;
import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.conversion.EntityConversionContext;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SingleStackInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.world.*;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.debug.DebugSubscriptionTypes;
import net.minecraft.world.debug.DebugTrackable;
import net.minecraft.world.debug.data.BrainDebugData;
import net.minecraft.world.debug.data.EntityPathDebugData;
import net.minecraft.world.debug.data.GoalSelectorDebugData;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

/**
 * Базовый класс для всех мобов с ИИ. Управляет целями, снаряжением и поведением.
 */
public abstract class MobEntity extends LivingEntity implements EquipmentHolder, Leashable, Targeter {

	private static final TrackedData<Byte>
			MOB_FLAGS =
			DataTracker.registerData(MobEntity.class, TrackedDataHandlerRegistry.BYTE);
	private static final int AI_DISABLED_FLAG = 1;
	private static final int LEFT_HANDED_FLAG = 2;
	private static final int ATTACKING_FLAG = 4;
	protected static final int MINIMUM_DROPPED_EXPERIENCE_PER_EQUIPMENT = 1;
	private static final Vec3i ITEM_PICK_UP_RANGE_EXPANDER = new Vec3i(1, 0, 1);
	private static final List<EquipmentSlot>
			EQUIPMENT_INIT_ORDER =
			List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
	public static final float BASE_SPAWN_EQUIPMENT_CHANCE = 0.15F;
	public static final float ARMOR_UPGRADE_CHANCE = 0.1087F;
	public static final float ARMOR_UPGRADE_ROLLS = 3.0F;
	public static final float DEFAULT_CAN_PICKUP_LOOT_CHANCE = 0.55F;
	public static final float BASE_ENCHANTED_ARMOR_CHANCE = 0.5F;
	public static final float BASE_ENCHANTED_MAIN_HAND_EQUIPMENT_CHANCE = 0.25F;
	public static final int DEFAULT_EQUIPMENT_LEVEL = 2;
	private static final double ATTACK_RANGE = Math.sqrt(2.04F) - 0.6F;
	private static final boolean DEFAULT_CAN_PICK_UP_LOOT = false;
	private static final boolean DEFAULT_PERSISTENCE_REQUIRED = false;
	private static final boolean DEFAULT_LEFT_HANDED = false;
	private static final boolean DEFAULT_NO_AI = false;
	protected static final Identifier RANDOM_SPAWN_BONUS_MODIFIER_ID = Identifier.ofVanilla("random_spawn_bonus");
	public static final String DROP_CHANCES_KEY = "drop_chances";
	public static final String LEFT_HANDED_KEY = "LeftHanded";
	public static final String CAN_PICK_UP_LOOT_KEY = "CanPickUpLoot";
	public static final String NO_AI_KEY = "NoAI";
	public int ambientSoundChance;
	protected int experiencePoints;
	protected LookControl lookControl;
	protected MoveControl moveControl;
	protected JumpControl jumpControl;
	private final BodyControl bodyControl;
	protected EntityNavigation navigation;
	protected final GoalSelector goalSelector;
	protected final GoalSelector targetSelector;
	private @Nullable LivingEntity target;
	private final MobVisibilityCache visibilityCache;
	private EquipmentDropChances equipmentDropChances = EquipmentDropChances.DEFAULT;
	private boolean canPickUpLoot = false;
	private boolean persistent = false;
	private final Map<PathNodeType, Float> pathfindingPenalties = Maps.newEnumMap(PathNodeType.class);
	private Optional<RegistryKey<LootTable>> lootTable = Optional.empty();
	private long lootTableSeed;
	private Leashable.@Nullable LeashData leashData;
	private BlockPos positionTarget = BlockPos.ORIGIN;
	private int positionTargetRange = -1;

	protected MobEntity(EntityType<? extends MobEntity> entityType, World world) {
		super(entityType, world);
		goalSelector = new GoalSelector();
		targetSelector = new GoalSelector();
		lookControl = new LookControl(this);
		moveControl = new MoveControl(this);
		jumpControl = new JumpControl(this);
		bodyControl = createBodyControl();
		navigation = createNavigation(world);
		visibilityCache = new MobVisibilityCache(this);

		if (world instanceof ServerWorld) {
			initGoals();
		}
	}

	protected void initGoals() {
	}

	public static DefaultAttributeContainer.Builder createMobAttributes() {
		return LivingEntity.createLivingAttributes().add(EntityAttributes.FOLLOW_RANGE, 16.0);
	}

	protected EntityNavigation createNavigation(World world) {
		return new MobNavigation(this, world);
	}

	protected boolean movesIndependently() {
		return false;
	}

	public float getPathfindingPenalty(PathNodeType nodeType) {
		MobEntity mobEntity2;
		if (this.getControllingVehicle() instanceof MobEntity mobEntity && mobEntity.movesIndependently()) {
			mobEntity2 = mobEntity;
		}
		else {
			mobEntity2 = this;
		}

		Float float_ = mobEntity2.pathfindingPenalties.get(nodeType);
		return float_ == null ? nodeType.getDefaultPenalty() : float_;
	}

	public void setPathfindingPenalty(PathNodeType nodeType, float penalty) {
		this.pathfindingPenalties.put(nodeType, penalty);
	}

	public void onStartPathfinding() {
	}

	public void onFinishPathfinding() {
	}

	protected BodyControl createBodyControl() {
		return new BodyControl(this);
	}

	public LookControl getLookControl() {
		return this.lookControl;
	}

	public MoveControl getMoveControl() {
		return this.getControllingVehicle() instanceof MobEntity mobEntity ? mobEntity.getMoveControl()
		                                                                   : this.moveControl;
	}

	public JumpControl getJumpControl() {
		return this.jumpControl;
	}

	public EntityNavigation getNavigation() {
		return this.getControllingVehicle() instanceof MobEntity mobEntity ? mobEntity.getNavigation()
		                                                                   : this.navigation;
	}

	@Override
	public @Nullable LivingEntity getControllingPassenger() {
		Entity entity = this.getFirstPassenger();
		return !this.isAiDisabled() && entity instanceof MobEntity mobEntity && entity.shouldControlVehicles()
		       ? mobEntity : null;
	}

	public MobVisibilityCache getVisibilityCache() {
		return this.visibilityCache;
	}

	@Override
	public @Nullable LivingEntity getTarget() {
		return this.target;
	}

	protected final @Nullable LivingEntity getTargetInBrain() {
		return this.getBrain().getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
	}

	public void setTarget(@Nullable LivingEntity target) {
		this.target = target;
	}

	@Override
	public boolean canTarget(EntityType<?> type) {
		return type != EntityType.GHAST;
	}

	/**
	 * Проверяет возможность use ranged weapon.
	 *
	 * @param stack stack
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canUseRangedWeapon(ItemStack stack) {
		return false;
	}

	/**
	 * Обрабатывает событие eating grass.
	 */
	public void onEatingGrass() {
		this.emitGameEvent(GameEvent.EAT);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(MOB_FLAGS, (byte) 0);
	}

	public int getMinAmbientSoundDelay() {
		return 80;
	}

	public void playAmbientSound() {
		this.playSound(this.getAmbientSound());
	}

	@Override
	public void baseTick() {
		super.baseTick();
		Profiler profiler = Profilers.get();
		profiler.push("mobBaseTick");
		if (this.isAlive() && this.random.nextInt(1000) < this.ambientSoundChance++) {
			this.resetSoundDelay();
			this.playAmbientSound();
		}

		profiler.pop();
	}

	@Override
	protected void playHurtSound(DamageSource damageSource) {
		this.resetSoundDelay();
		super.playHurtSound(damageSource);
	}

	private void resetSoundDelay() {
		this.ambientSoundChance = -this.getMinAmbientSoundDelay();
	}

	@Override
	protected int getExperienceToDrop(ServerWorld world) {
		if (experiencePoints <= 0) {
			return experiencePoints;
		}

		int total = experiencePoints;

		for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {
			if (equipmentSlot.increasesDroppedExperience()) {
				ItemStack itemStack = getEquippedStack(equipmentSlot);

				if (!itemStack.isEmpty() && equipmentDropChances.get(equipmentSlot) <= 1.0F) {
					total += 1 + random.nextInt(3);
				}
			}
		}

		return total;
	}

	public void playSpawnEffects() {
		if (this.getEntityWorld().isClient()) {
			this.addDeathParticles();
		}
		else {
			this.getEntityWorld().sendEntityStatus(this, (byte) 20);
		}
	}

	@Override
	public void handleStatus(byte status) {
		if (status == 20) {
			this.playSpawnEffects();
		}
		else {
			super.handleStatus(status);
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.getEntityWorld().isClient() && this.age % 5 == 0) {
			this.updateGoalControls();
		}
	}

	protected void updateGoalControls() {
		boolean notControlled = !(getControllingPassenger() instanceof MobEntity);
		boolean notInBoat = !(getVehicle() instanceof AbstractBoatEntity);
		goalSelector.setControlEnabled(Goal.Control.MOVE, notControlled);
		goalSelector.setControlEnabled(Goal.Control.JUMP, notControlled && notInBoat);
		goalSelector.setControlEnabled(Goal.Control.LOOK, notControlled);
	}

	@Override
	protected void turnHead(float bodyRotation) {
		this.bodyControl.tick();
	}

	protected @Nullable SoundEvent getAmbientSound() {
		return null;
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putBoolean("CanPickUpLoot", this.canPickUpLoot());
		view.putBoolean("PersistenceRequired", this.persistent);
		if (!this.equipmentDropChances.equals(EquipmentDropChances.DEFAULT)) {
			view.put("drop_chances", EquipmentDropChances.CODEC, this.equipmentDropChances);
		}

		this.writeLeashData(view, this.leashData);
		if (this.hasPositionTarget()) {
			view.putInt("home_radius", this.positionTargetRange);
			view.put("home_pos", BlockPos.CODEC, this.positionTarget);
		}

		view.putBoolean("LeftHanded", this.isLeftHanded());
		this.lootTable.ifPresent(lootTableKey -> view.put("DeathLootTable", LootTable.TABLE_KEY, lootTableKey));
		if (this.lootTableSeed != 0L) {
			view.putLong("DeathLootTableSeed", this.lootTableSeed);
		}

		if (this.isAiDisabled()) {
			view.putBoolean("NoAI", this.isAiDisabled());
		}
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		setCanPickUpLoot(view.getBoolean("CanPickUpLoot", false));
		this.persistent = view.getBoolean("PersistenceRequired", false);
		this.equipmentDropChances =
				view
						.<EquipmentDropChances>read("drop_chances", EquipmentDropChances.CODEC)
						.orElse(EquipmentDropChances.DEFAULT);
		this.readLeashData(view);
		this.positionTargetRange = view.getInt("home_radius", -1);
		if (this.positionTargetRange >= 0) {
			this.positionTarget = view.<BlockPos>read("home_pos", BlockPos.CODEC).orElse(BlockPos.ORIGIN);
		}

		setLeftHanded(view.getBoolean("LeftHanded", false));
		this.lootTable = view.read("DeathLootTable", LootTable.TABLE_KEY);
		this.lootTableSeed = view.getLong("DeathLootTableSeed", 0L);
		setAiDisabled(view.getBoolean("NoAI", false));
	}

	@Override
	protected void dropLoot(ServerWorld world, DamageSource damageSource, boolean causedByPlayer) {
		super.dropLoot(world, damageSource, causedByPlayer);
		this.lootTable = Optional.empty();
	}

	@Override
	public final Optional<RegistryKey<LootTable>> getLootTableKey() {
		return this.lootTable.isPresent() ? this.lootTable : super.getLootTableKey();
	}

	@Override
	public long getLootTableSeed() {
		return this.lootTableSeed;
	}

	public void setForwardSpeed(float forwardSpeed) {
		this.forwardSpeed = forwardSpeed;
	}

	public void setUpwardSpeed(float upwardSpeed) {
		this.upwardSpeed = upwardSpeed;
	}

	public void setSidewaysSpeed(float sidewaysSpeed) {
		this.sidewaysSpeed = sidewaysSpeed;
	}

	@Override
	public void setMovementSpeed(float movementSpeed) {
		super.setMovementSpeed(movementSpeed);
		this.setForwardSpeed(movementSpeed);
	}

	public void stopMovement() {
		this.getNavigation().stop();
		this.setSidewaysSpeed(0.0F);
		this.setUpwardSpeed(0.0F);
		this.setMovementSpeed(0.0F);
		this.setVelocity(0.0, 0.0, 0.0);
		this.resetLeashMomentum();
	}

	@Override
	public void tickMovement() {
		super.tickMovement();
		if (this.getType().isIn(EntityTypeTags.BURN_IN_DAYLIGHT)) {
			this.tickBurnInDaylight();
		}

		Profiler profiler = Profilers.get();
		profiler.push("looting");
		if (this.getEntityWorld() instanceof ServerWorld serverWorld
				&& this.canPickUpLoot()
				&& this.isAlive()
				&& !this.dead
				&& serverWorld.getGameRules().getValue(GameRules.DO_MOB_GRIEFING)) {
			Vec3i vec3i = this.getItemPickUpRangeExpander();

			for (ItemEntity itemEntity : this.getEntityWorld()
			                                 .getNonSpectatingEntities(
					                                 ItemEntity.class,
					                                 this
							                                 .getBoundingBox()
							                                 .expand(vec3i.getX(), vec3i.getY(), vec3i.getZ())
			                                 )) {
				if (!itemEntity.isRemoved() && !itemEntity.getStack().isEmpty() && !itemEntity.cannotPickup()
						&& this.canGather(serverWorld, itemEntity.getStack())
				) {
					this.loot(serverWorld, itemEntity);
				}
			}
		}

		profiler.pop();
	}

	protected EquipmentSlot getDaylightProtectionSlot() {
		return EquipmentSlot.HEAD;
	}

	private void tickBurnInDaylight() {
		if (this.isAlive() && this.isAffectedByDaylight()) {
			EquipmentSlot equipmentSlot = this.getDaylightProtectionSlot();
			ItemStack itemStack = this.getEquippedStack(equipmentSlot);
			if (!itemStack.isEmpty()) {
				if (itemStack.isDamageable()) {
					Item item = itemStack.getItem();
					itemStack.setDamage(itemStack.getDamage() + this.random.nextInt(2));
					if (itemStack.getDamage() >= itemStack.getMaxDamage()) {
						this.sendEquipmentBreakStatus(item, equipmentSlot);
						this.equipStack(equipmentSlot, ItemStack.EMPTY);
					}
				}
			}
			else {
				this.setOnFireFor(8.0F);
			}
		}
	}

	private boolean isAffectedByDaylight() {
		if (!this.getEntityWorld().isClient()
				&& this
				.getEntityWorld()
				.getEnvironmentAttributes()
				.getAttributeValue(EnvironmentAttributes.MONSTERS_BURN_GAMEPLAY, this.getEntityPos())) {
			float f = this.getBrightnessAtEyes();
			BlockPos blockPos = BlockPos.ofFloored(this.getX(), this.getEyeY(), this.getZ());
			boolean bl = this.isTouchingWaterOrRain() || this.inPowderSnow || this.wasInPowderSnow;
			if (f > 0.5F && this.random.nextFloat() * 30.0F < (f - 0.4F) * 2.0F && !bl && this
					.getEntityWorld()
					.isSkyVisible(blockPos)) {
				return true;
			}
		}

		return false;
	}

	protected Vec3i getItemPickUpRangeExpander() {
		return ITEM_PICK_UP_RANGE_EXPANDER;
	}

	protected void loot(ServerWorld world, ItemEntity itemEntity) {
		ItemStack itemStack = itemEntity.getStack();
		ItemStack itemStack2 = this.tryEquip(world, itemStack.copy());
		if (!itemStack2.isEmpty()) {
			this.triggerItemPickedUpByEntityCriteria(itemEntity);
			this.sendPickup(itemEntity, itemStack2.getCount());
			itemStack.decrement(itemStack2.getCount());
			if (itemStack.isEmpty()) {
				itemEntity.discard();
			}
		}
	}

	public ItemStack tryEquip(ServerWorld world, ItemStack stack) {
		EquipmentSlot equipmentSlot = this.getPreferredEquipmentSlot(stack);
		if (!this.canEquip(stack, equipmentSlot)) {
			return ItemStack.EMPTY;
		}
		else {
			ItemStack itemStack = this.getEquippedStack(equipmentSlot);
			boolean bl = this.prefersNewEquipment(stack, itemStack, equipmentSlot);
			if (equipmentSlot.isArmorSlot() && !bl) {
				equipmentSlot = EquipmentSlot.MAINHAND;
				itemStack = this.getEquippedStack(equipmentSlot);
				bl = itemStack.isEmpty();
			}

			if (bl && this.canPickupItem(stack)) {
				double d = this.equipmentDropChances.get(equipmentSlot);
				if (!itemStack.isEmpty() && Math.max(this.random.nextFloat() - 0.1F, 0.0F) < d) {
					this.dropStack(world, itemStack);
				}

				ItemStack itemStack2 = equipmentSlot.split(stack);
				this.equipLootStack(equipmentSlot, itemStack2);
				return itemStack2;
			}
			else {
				return ItemStack.EMPTY;
			}
		}
	}

	public void equipLootStack(EquipmentSlot slot, ItemStack stack) {
		this.equipStack(slot, stack);
		this.setDropGuaranteed(slot);
		this.persistent = true;
	}

	public boolean canRemoveSaddle(PlayerEntity player) {
		return !this.hasPassengers();
	}

	public void setDropGuaranteed(EquipmentSlot slot) {
		this.equipmentDropChances = this.equipmentDropChances.withGuaranteed(slot);
	}

	protected boolean prefersNewEquipment(ItemStack newStack, ItemStack currentStack, EquipmentSlot slot) {
		if (currentStack.isEmpty()) {
			return true;
		}
		else if (slot.isArmorSlot()) {
			return this.prefersNewArmor(newStack, currentStack, slot);
		}
		else {
			return slot == EquipmentSlot.MAINHAND ? this.prefersNewWeapon(newStack, currentStack, slot) : false;
		}
	}

	private boolean prefersNewArmor(ItemStack newStack, ItemStack currentStack, EquipmentSlot slot) {
		if (EnchantmentHelper.hasAnyEnchantmentsWith(
				currentStack,
				EnchantmentEffectComponentTypes.PREVENT_ARMOR_CHANGE
		)) {
			return false;
		}
		else {
			double d = this.getAttributeValueWithStack(newStack, EntityAttributes.ARMOR, slot);
			double e = this.getAttributeValueWithStack(currentStack, EntityAttributes.ARMOR, slot);
			double f = this.getAttributeValueWithStack(newStack, EntityAttributes.ARMOR_TOUGHNESS, slot);
			double g = this.getAttributeValueWithStack(currentStack, EntityAttributes.ARMOR_TOUGHNESS, slot);
			if (d != e) {
				return d > e;
			}
			else {
				return f != g ? f > g : this.prefersNewDamageableItem(newStack, currentStack);
			}
		}
	}

	private boolean prefersNewWeapon(ItemStack newStack, ItemStack currentStack, EquipmentSlot slot) {
		TagKey<Item> tagKey = this.getPreferredWeapons();
		if (tagKey != null) {
			if (currentStack.isIn(tagKey) && !newStack.isIn(tagKey)) {
				return false;
			}

			if (!currentStack.isIn(tagKey) && newStack.isIn(tagKey)) {
				return true;
			}
		}

		double d = this.getAttributeValueWithStack(newStack, EntityAttributes.ATTACK_DAMAGE, slot);
		double e = this.getAttributeValueWithStack(currentStack, EntityAttributes.ATTACK_DAMAGE, slot);
		return d != e ? d > e : this.prefersNewDamageableItem(newStack, currentStack);
	}

	private double getAttributeValueWithStack(
			ItemStack stack,
			RegistryEntry<EntityAttribute> attribute,
			EquipmentSlot slot
	) {
		double d = this.getAttributes().hasAttribute(attribute) ? this.getAttributeBaseValue(attribute) : 0.0;
		AttributeModifiersComponent
				attributeModifiersComponent =
				stack.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
		return attributeModifiersComponent.applyOperations(attribute, d, slot);
	}

	public boolean prefersNewDamageableItem(ItemStack newStack, ItemStack oldStack) {
		Set<Entry<RegistryEntry<Enchantment>>> oldEnchants = oldStack
				.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT)
				.getEnchantmentEntries();
		Set<Entry<RegistryEntry<Enchantment>>> newEnchants = newStack
				.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT)
				.getEnchantmentEntries();

		if (newEnchants.size() != oldEnchants.size()) {
			return newEnchants.size() > oldEnchants.size();
		}

		int newDamage = newStack.getDamage();
		int oldDamage = oldStack.getDamage();
		return newDamage != oldDamage
				? newDamage < oldDamage
				: newStack.contains(DataComponentTypes.CUSTOM_NAME) && !oldStack.contains(DataComponentTypes.CUSTOM_NAME);
	}

	public boolean canPickupItem(ItemStack stack) {
		return true;
	}

	public boolean canGather(ServerWorld world, ItemStack stack) {
		return this.canPickupItem(stack);
	}

	public @Nullable TagKey<Item> getPreferredWeapons() {
		return null;
	}

	public boolean canImmediatelyDespawn(double distanceSquared) {
		return true;
	}

	public boolean cannotDespawn() {
		return this.hasVehicle();
	}

	@Override
	public void checkDespawn() {
		if (getEntityWorld().getDifficulty() == Difficulty.PEACEFUL && !getType().isAllowedInPeaceful()) {
			discard();
			return;
		}

		if (isPersistent() || cannotDespawn()) {
			despawnCounter = 0;
			return;
		}

		Entity closestPlayer = getEntityWorld().getClosestPlayer(this, -1.0);

		if (closestPlayer == null) {
			return;
		}

		double distSq = closestPlayer.squaredDistanceTo(this);
		int immediateDespawnRange = getType().getSpawnGroup().getImmediateDespawnRange();
		int immediateDespawnRangeSq = immediateDespawnRange * immediateDespawnRange;

		if (distSq > immediateDespawnRangeSq && canImmediatelyDespawn(distSq)) {
			discard();
			return;
		}

		int despawnStartRange = getType().getSpawnGroup().getDespawnStartRange();
		int despawnStartRangeSq = despawnStartRange * despawnStartRange;

		if (despawnCounter > 600 && random.nextInt(800) == 0 && distSq > despawnStartRangeSq && canImmediatelyDespawn(distSq)) {
			discard();
		} else if (distSq < despawnStartRangeSq) {
			despawnCounter = 0;
		}
	}

	@Override
	protected final void tickNewAi() {
		despawnCounter++;
		Profiler profiler = Profilers.get();
		profiler.push("sensing");
		visibilityCache.clear();
		profiler.pop();
		int tickOffset = age + getId();

		if (tickOffset % 2 != 0 && age > 1) {
			profiler.push("targetSelector");
			this.targetSelector.tickGoals(false);
			profiler.pop();
			profiler.push("goalSelector");
			this.goalSelector.tickGoals(false);
			profiler.pop();
		}
		else {
			profiler.push("targetSelector");
			this.targetSelector.tick();
			profiler.pop();
			profiler.push("goalSelector");
			this.goalSelector.tick();
			profiler.pop();
		}

		profiler.push("navigation");
		this.navigation.tick();
		profiler.pop();
		profiler.push("mob tick");
		this.mobTick((ServerWorld) this.getEntityWorld());
		profiler.pop();
		profiler.push("controls");
		profiler.push("move");
		this.moveControl.tick();
		profiler.swap("look");
		this.lookControl.tick();
		profiler.swap("jump");
		this.jumpControl.tick();
		profiler.pop();
		profiler.pop();
	}

	protected void mobTick(ServerWorld world) {
	}

	public int getMaxLookPitchChange() {
		return 40;
	}

	public int getMaxHeadRotation() {
		return 75;
	}

	protected void clampHeadYaw() {
		float maxRotation = getMaxHeadRotation();
		float headYaw = getHeadYaw();
		float rawDelta = MathHelper.wrapDegrees(bodyYaw - headYaw);
		float clampedDelta = MathHelper.clamp(rawDelta, -maxRotation, maxRotation);
		setHeadYaw(headYaw + rawDelta - clampedDelta);
	}

	public int getMaxLookYawChange() {
		return 10;
	}

	public void lookAtEntity(Entity targetEntity, float maxYawChange, float maxPitchChange) {
		double dx = targetEntity.getX() - getX();
		double dz = targetEntity.getZ() - getZ();
		double dy = (targetEntity instanceof LivingEntity livingEntity)
				? livingEntity.getEyeY() - getEyeY()
				: (targetEntity.getBoundingBox().minY + targetEntity.getBoundingBox().maxY) / 2.0 - getEyeY();

		double horizDist = Math.sqrt(dx * dx + dz * dz);
		float targetYaw = (float) (MathHelper.atan2(dz, dx) * 180.0F / (float) Math.PI) - 90.0F;
		float targetPitch = (float) (-(MathHelper.atan2(dy, horizDist) * 180.0F / (float) Math.PI));

		setPitch(changeAngle(getPitch(), targetPitch, maxPitchChange));
		setYaw(changeAngle(getYaw(), targetYaw, maxYawChange));
	}

	private float changeAngle(float from, float to, float max) {
		float f = MathHelper.wrapDegrees(to - from);
		if (f > max) {
			f = max;
		}

		if (f < -max) {
			f = -max;
		}

		return from + f;
	}

	public static boolean canMobSpawn(
			EntityType<? extends MobEntity> type,
			WorldAccess world,
			SpawnReason spawnReason,
			BlockPos pos,
			Random random
	) {
		BlockPos blockPos = pos.down();
		return SpawnReason.isAnySpawner(spawnReason) || world
				.getBlockState(blockPos)
				.allowsSpawning(world, blockPos, type);
	}

	public boolean canSpawn(WorldAccess world, SpawnReason spawnReason) {
		return true;
	}

	public boolean canSpawn(WorldView world) {
		return !world.containsFluid(this.getBoundingBox()) && world.doesNotIntersectEntities(this);
	}

	public int getLimitPerChunk() {
		return 4;
	}

	public boolean spawnsTooManyForEachTry(int count) {
		return false;
	}

	@Override
	public int getSafeFallDistance() {
		if (this.getTarget() == null) {
			return this.getSafeFallDistance(0.0F);
		}
		else {
			int i = (int) (this.getHealth() - this.getMaxHealth() * 0.33F);
			i -= (3 - this.getEntityWorld().getDifficulty().getId()) * 4;
			if (i < 0) {
				i = 0;
			}

			return this.getSafeFallDistance(i);
		}
	}

	public ItemStack getBodyArmor() {
		return this.getEquippedStack(EquipmentSlot.BODY);
	}

	public boolean hasSaddleEquipped() {
		return this.isWearing(EquipmentSlot.SADDLE);
	}

	public boolean isWearingBodyArmor() {
		return this.isWearing(EquipmentSlot.BODY);
	}

	private boolean isWearing(EquipmentSlot slot) {
		return this.hasStackEquipped(slot) && this.canEquip(this.getEquippedStack(slot), slot);
	}

	public void equipBodyArmor(ItemStack stack) {
		this.equipLootStack(EquipmentSlot.BODY, stack);
	}

	public Inventory createEquipmentInventory(EquipmentSlot slot) {
		return new SingleStackInventory() {
			@Override
			public ItemStack getStack() {
				return MobEntity.this.getEquippedStack(slot);
			}

			@Override
			public void setStack(ItemStack stack) {
				MobEntity.this.equipStack(slot, stack);
				if (!stack.isEmpty()) {
					MobEntity.this.setDropGuaranteed(slot);
					MobEntity.this.setPersistent();
				}
			}

			@Override
			public void markDirty() {
			}

			@Override
			public boolean canPlayerUse(PlayerEntity player) {
				return player.getVehicle() == MobEntity.this || player.canInteractWithEntity(MobEntity.this, 4.0);
			}
		};
	}

	@Override
	protected void dropEquipment(ServerWorld world, DamageSource source, boolean causedByPlayer) {
		super.dropEquipment(world, source, causedByPlayer);

		for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {
			ItemStack itemStack = this.getEquippedStack(equipmentSlot);
			float f = this.equipmentDropChances.get(equipmentSlot);
			if (f != 0.0F) {
				boolean bl = this.equipmentDropChances.dropsExactly(equipmentSlot);
				if (source.getAttacker() instanceof LivingEntity livingEntity
						&& this.getEntityWorld() instanceof ServerWorld serverWorld) {
					f = EnchantmentHelper.getEquipmentDropChance(serverWorld, livingEntity, source, f);
				}

				if (!itemStack.isEmpty()
						&& !EnchantmentHelper.hasAnyEnchantmentsWith(
						itemStack,
						EnchantmentEffectComponentTypes.PREVENT_EQUIPMENT_DROP
				)
						&& (causedByPlayer || bl)
						&& this.random.nextFloat() < f) {
					if (!bl && itemStack.isDamageable()) {
						itemStack.setDamage(itemStack.getMaxDamage() - this.random.nextInt(
								1 + this.random.nextInt(Math.max(itemStack.getMaxDamage() - 3, 1))));
					}

					this.dropStack(world, itemStack);
					this.equipStack(equipmentSlot, ItemStack.EMPTY);
				}
			}
		}
	}

	public EquipmentDropChances getEquipmentDropChances() {
		return this.equipmentDropChances;
	}

	public void dropAllForeignEquipment(ServerWorld world) {
		this.dropForeignEquipment(world, stack -> true);
	}

	public Set<EquipmentSlot> dropForeignEquipment(ServerWorld world, Predicate<ItemStack> dropPredicate) {
		Set<EquipmentSlot> set = new HashSet<>();

		for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {
			ItemStack itemStack = this.getEquippedStack(equipmentSlot);
			if (!itemStack.isEmpty()) {
				if (!dropPredicate.test(itemStack)) {
					set.add(equipmentSlot);
				}
				else if (this.equipmentDropChances.dropsExactly(equipmentSlot)) {
					this.equipStack(equipmentSlot, ItemStack.EMPTY);
					this.dropStack(world, itemStack);
				}
			}
		}

		return set;
	}

	private LootWorldContext createEquipmentLootParameters(ServerWorld world) {
		return new LootWorldContext.Builder(world)
				.add(LootContextParameters.ORIGIN, this.getEntityPos())
				.add(LootContextParameters.THIS_ENTITY, this)
				.build(LootContextTypes.EQUIPMENT);
	}

	public void setEquipmentFromTable(EquipmentTable equipmentTable) {
		this.setEquipmentFromTable(equipmentTable.lootTable(), equipmentTable.slotDropChances());
	}

	public void setEquipmentFromTable(RegistryKey<LootTable> lootTable, Map<EquipmentSlot, Float> slotDropChances) {
		if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
			this.setEquipmentFromTable(lootTable, this.createEquipmentLootParameters(serverWorld), slotDropChances);
		}
	}

	protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
		if (random.nextFloat() < BASE_SPAWN_EQUIPMENT_CHANCE * localDifficulty.getClampedLocalDifficulty()) {
			int equipmentLevel = random.nextInt(3);

			for (int roll = 1; roll <= (int) ARMOR_UPGRADE_ROLLS; roll++) {
				if (random.nextFloat() < ARMOR_UPGRADE_CHANCE) {
					equipmentLevel++;
				}
			}

			float skipChance = getEntityWorld().getDifficulty() == Difficulty.HARD ? 0.1F : 0.25F;
			boolean firstSlot = true;

			for (EquipmentSlot equipmentSlot : EQUIPMENT_INIT_ORDER) {
				ItemStack itemStack = this.getEquippedStack(equipmentSlot);
				if (!firstSlot && random.nextFloat() < skipChance) {
					break;
				}

				firstSlot = false;

				if (itemStack.isEmpty()) {
					Item item = getEquipmentForSlot(equipmentSlot, equipmentLevel);
					if (item != null) {
						this.equipStack(equipmentSlot, new ItemStack(item));
					}
				}
			}
		}
	}

	public static @Nullable Item getEquipmentForSlot(EquipmentSlot equipmentSlot, int equipmentLevel) {
		switch (equipmentSlot) {
			case HEAD:
				if (equipmentLevel == 0) {
					return Items.LEATHER_HELMET;
				}
				else if (equipmentLevel == 1) {
					return Items.COPPER_HELMET;
				}
				else if (equipmentLevel == 2) {
					return Items.GOLDEN_HELMET;
				}
				else if (equipmentLevel == 3) {
					return Items.CHAINMAIL_HELMET;
				}
				else if (equipmentLevel == 4) {
					return Items.IRON_HELMET;
				}
				else if (equipmentLevel == 5) {
					return Items.DIAMOND_HELMET;
				}
			case CHEST:
				if (equipmentLevel == 0) {
					return Items.LEATHER_CHESTPLATE;
				}
				else if (equipmentLevel == 1) {
					return Items.COPPER_CHESTPLATE;
				}
				else if (equipmentLevel == 2) {
					return Items.GOLDEN_CHESTPLATE;
				}
				else if (equipmentLevel == 3) {
					return Items.CHAINMAIL_CHESTPLATE;
				}
				else if (equipmentLevel == 4) {
					return Items.IRON_CHESTPLATE;
				}
				else if (equipmentLevel == 5) {
					return Items.DIAMOND_CHESTPLATE;
				}
			case LEGS:
				if (equipmentLevel == 0) {
					return Items.LEATHER_LEGGINGS;
				}
				else if (equipmentLevel == 1) {
					return Items.COPPER_LEGGINGS;
				}
				else if (equipmentLevel == 2) {
					return Items.GOLDEN_LEGGINGS;
				}
				else if (equipmentLevel == 3) {
					return Items.CHAINMAIL_LEGGINGS;
				}
				else if (equipmentLevel == 4) {
					return Items.IRON_LEGGINGS;
				}
				else if (equipmentLevel == 5) {
					return Items.DIAMOND_LEGGINGS;
				}
			case FEET:
				if (equipmentLevel == 0) {
					return Items.LEATHER_BOOTS;
				}
				else if (equipmentLevel == 1) {
					return Items.COPPER_BOOTS;
				}
				else if (equipmentLevel == 2) {
					return Items.GOLDEN_BOOTS;
				}
				else if (equipmentLevel == 3) {
					return Items.CHAINMAIL_BOOTS;
				}
				else if (equipmentLevel == 4) {
					return Items.IRON_BOOTS;
				}
				else if (equipmentLevel == 5) {
					return Items.DIAMOND_BOOTS;
				}
			default:
				return null;
		}
	}

	protected void updateEnchantments(ServerWorldAccess world, Random random, LocalDifficulty localDifficulty) {
		this.enchantMainHandItem(world, random, localDifficulty);

		for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {
			if (equipmentSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
				this.enchantEquipment(world, random, equipmentSlot, localDifficulty);
			}
		}
	}

	protected void enchantMainHandItem(ServerWorldAccess world, Random random, LocalDifficulty localDifficulty) {
		this.enchantEquipment(world, EquipmentSlot.MAINHAND, random, BASE_ENCHANTED_MAIN_HAND_EQUIPMENT_CHANCE, localDifficulty);
	}

	protected void enchantEquipment(
			ServerWorldAccess world,
			Random random,
			EquipmentSlot slot,
			LocalDifficulty localDifficulty
	) {
		this.enchantEquipment(world, slot, random, 0.5F, localDifficulty);
	}

	private void enchantEquipment(
			ServerWorldAccess world,
			EquipmentSlot slot,
			Random random,
			float power,
			LocalDifficulty localDifficulty
	) {
		ItemStack itemStack = this.getEquippedStack(slot);
		if (!itemStack.isEmpty() && random.nextFloat() < power * localDifficulty.getClampedLocalDifficulty()) {
			EnchantmentHelper.applyEnchantmentProvider(
					itemStack,
					world.getRegistryManager(),
					EnchantmentProviders.MOB_SPAWN_EQUIPMENT,
					localDifficulty,
					random
			);
			this.equipStack(slot, itemStack);
		}
	}

	public @Nullable EntityData initialize(
			ServerWorldAccess world,
			LocalDifficulty difficulty,
			SpawnReason spawnReason,
			@Nullable EntityData entityData
	) {
		Random random = world.getRandom();
		EntityAttributeInstance
				entityAttributeInstance =
				Objects.requireNonNull(this.getAttributeInstance(EntityAttributes.FOLLOW_RANGE));
		if (!entityAttributeInstance.hasModifier(RANDOM_SPAWN_BONUS_MODIFIER_ID)) {
			entityAttributeInstance.addPersistentModifier(
					new EntityAttributeModifier(
							RANDOM_SPAWN_BONUS_MODIFIER_ID,
							random.nextTriangular(0.0, 0.11485000000000001),
							EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
					)
			);
		}

		setLeftHanded(random.nextFloat() < 0.05F);
		return entityData;
	}

	public void setPersistent() {
		this.persistent = true;
	}

	@Override
	public void setEquipmentDropChance(EquipmentSlot slot, float dropChance) {
		this.equipmentDropChances = this.equipmentDropChances.withChance(slot, dropChance);
	}

	@Override
	public boolean canPickUpLoot() {
		return this.canPickUpLoot;
	}

	public void setCanPickUpLoot(boolean canPickUpLoot) {
		this.canPickUpLoot = canPickUpLoot;
	}

	@Override
	protected boolean canDispenserEquipSlot(EquipmentSlot slot) {
		return this.canPickUpLoot();
	}

	public boolean isPersistent() {
		return this.persistent;
	}

	@Override
	public ActionResult interact(PlayerEntity player, Hand hand) {
		if (!this.isAlive()) {
			return ActionResult.PASS;
		}
		else {
			ActionResult actionResult = this.interactWithItem(player, hand);
			if (actionResult.isAccepted()) {
				this.emitGameEvent(GameEvent.ENTITY_INTERACT, player);
				return actionResult;
			}
			else {
				ActionResult actionResult2 = super.interact(player, hand);
				if (actionResult2 != ActionResult.PASS) {
					return actionResult2;
				}
				else {
					actionResult = this.interactMob(player, hand);
					if (actionResult.isAccepted()) {
						this.emitGameEvent(GameEvent.ENTITY_INTERACT, player);
						return actionResult;
					}
					else {
						return ActionResult.PASS;
					}
				}
			}
		}
	}

	private ActionResult interactWithItem(PlayerEntity player, Hand hand) {
		ItemStack itemStack = player.getStackInHand(hand);
		if (itemStack.isOf(Items.NAME_TAG)) {
			ActionResult actionResult = itemStack.useOnEntity(player, this, hand);
			if (actionResult.isAccepted()) {
				return actionResult;
			}
		}

		if (itemStack.getItem() instanceof SpawnEggItem spawnEggItem) {
			if (this.getEntityWorld() instanceof ServerWorld) {
				Optional<MobEntity> optional = spawnEggItem.spawnBaby(
						player,
						this,
						(EntityType<? extends MobEntity>) this.getType(),
						(ServerWorld) this.getEntityWorld(),
						this.getEntityPos(),
						itemStack
				);
				optional.ifPresent(entity -> this.onPlayerSpawnedChild(player, entity));
				if (optional.isEmpty()) {
					return ActionResult.PASS;
				}
			}

			return ActionResult.SUCCESS_SERVER;
		}
		else {
			return ActionResult.PASS;
		}
	}

	protected void onPlayerSpawnedChild(PlayerEntity player, MobEntity child) {
	}

	protected ActionResult interactMob(PlayerEntity player, Hand hand) {
		return ActionResult.PASS;
	}

	protected void eat(PlayerEntity player, Hand hand, ItemStack stack) {
		int i = stack.getCount();
		UseRemainderComponent useRemainderComponent = stack.get(DataComponentTypes.USE_REMAINDER);
		stack.decrementUnlessCreative(1, player);
		if (useRemainderComponent != null) {
			ItemStack
					itemStack =
					useRemainderComponent.convert(stack, i, player.isInCreativeMode(), player::giveOrDropStack);
			player.setStackInHand(hand, itemStack);
		}
	}

	public boolean isInPositionTargetRange() {
		return this.isInPositionTargetRange(this.getBlockPos());
	}

	public boolean isInPositionTargetRange(BlockPos pos) {
		return this.positionTargetRange == -1 ? true : this.positionTarget.getSquaredDistance(pos)
		                                               < this.positionTargetRange * this.positionTargetRange;
	}

	public boolean isInPositionTargetRange(Vec3d pos) {
		return this.positionTargetRange == -1 ? true : this.positionTarget.getSquaredDistance(pos)
		                                               < this.positionTargetRange * this.positionTargetRange;
	}

	public void setPositionTarget(BlockPos target, int range) {
		this.positionTarget = target;
		this.positionTargetRange = range;
	}

	public BlockPos getPositionTarget() {
		return this.positionTarget;
	}

	public int getPositionTargetRange() {
		return this.positionTargetRange;
	}

	public void clearPositionTarget() {
		this.positionTargetRange = -1;
	}

	public boolean hasPositionTarget() {
		return this.positionTargetRange != -1;
	}

	public <T extends MobEntity> @Nullable T convertTo(
			EntityType<T> entityType,
			EntityConversionContext context,
			SpawnReason reason,
			EntityConversionContext.Finalizer<T> finalizer
	) {
		if (this.isRemoved()) {
			return null;
		}
		else {
			T mobEntity = (T) entityType.create(this.getEntityWorld(), reason);
			if (mobEntity == null) {
				return null;
			}
			else {
				context.type().setUpNewEntity(this, mobEntity, context);
				finalizer.finalizeConversion(mobEntity);
				if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
					serverWorld.spawnEntity(mobEntity);
				}

				if (context.type().shouldDiscardOldEntity()) {
					this.discard();
				}

				return mobEntity;
			}
		}
	}

	public <T extends MobEntity> @Nullable T convertTo(
			EntityType<T> entityType,
			EntityConversionContext context,
			EntityConversionContext.Finalizer<T> finalizer
	) {
		return this.convertTo(entityType, context, SpawnReason.CONVERSION, finalizer);
	}

	@Override
	public Leashable.@Nullable LeashData getLeashData() {
		return this.leashData;
	}

	private void resetLeashMomentum() {
		if (this.leashData != null) {
			this.leashData.momentum = 0.0;
		}
	}

	@Override
	public void setLeashData(Leashable.@Nullable LeashData leashData) {
		this.leashData = leashData;
	}

	@Override
	public void onLeashRemoved() {
		if (this.getLeashData() == null) {
			this.clearPositionTarget();
		}
	}

	@Override
	public void snapLongLeash() {
		Leashable.super.snapLongLeash();
		this.goalSelector.disableControl(Goal.Control.MOVE);
	}

	@Override
	public boolean canBeLeashed() {
		return !(this instanceof Monster);
	}

	@Override
	public boolean startRiding(Entity entity, boolean force, boolean emitEvent) {
		boolean bl = super.startRiding(entity, force, emitEvent);
		if (bl && this.isLeashed()) {
			this.detachLeash();
		}

		return bl;
	}

	@Override
	public boolean canActVoluntarily() {
		return super.canActVoluntarily() && !this.isAiDisabled();
	}

	public void setAiDisabled(boolean aiDisabled) {
		byte b = this.dataTracker.get(MOB_FLAGS);
		this.dataTracker.set(MOB_FLAGS, aiDisabled ? (byte) (b | 1) : (byte) (b & -2));
	}

	public void setLeftHanded(boolean leftHanded) {
		byte b = this.dataTracker.get(MOB_FLAGS);
		this.dataTracker.set(MOB_FLAGS, leftHanded ? (byte) (b | 2) : (byte) (b & -3));
	}

	public void setAttacking(boolean attacking) {
		byte b = this.dataTracker.get(MOB_FLAGS);
		this.dataTracker.set(MOB_FLAGS, attacking ? (byte) (b | 4) : (byte) (b & -5));
	}

	public boolean isAiDisabled() {
		return (this.dataTracker.get(MOB_FLAGS) & 1) != 0;
	}

	public boolean isLeftHanded() {
		return (this.dataTracker.get(MOB_FLAGS) & 2) != 0;
	}

	public boolean isAttacking() {
		return (this.dataTracker.get(MOB_FLAGS) & 4) != 0;
	}

	public void setBaby(boolean baby) {
	}

	@Override
	public Arm getMainArm() {
		return this.isLeftHanded() ? Arm.LEFT : Arm.RIGHT;
	}

	public boolean isInAttackRange(LivingEntity entity) {
		AttackRangeComponent
				attackRangeComponent =
				this.getActiveOrMainHandStack().get(DataComponentTypes.ATTACK_RANGE);
		double d;
		double e;
		if (attackRangeComponent == null) {
			d = ATTACK_RANGE;
			e = 0.0;
		}
		else {
			d = attackRangeComponent.getEffectiveMaxRange(this);
			e = attackRangeComponent.getEffectiveMinRange(this);
		}

		Box box = entity.getHitbox();
		return this.getAttackBox(d).intersects(box) && (e <= 0.0 || !this.getAttackBox(e).intersects(box));
	}

	protected Box getAttackBox(double attackRange) {
		Entity entity = this.getVehicle();
		Box box3;
		if (entity != null) {
			Box box = entity.getBoundingBox();
			Box box2 = this.getBoundingBox();
			box3 = new Box(
					Math.min(box2.minX, box.minX),
					box2.minY,
					Math.min(box2.minZ, box.minZ),
					Math.max(box2.maxX, box.maxX),
					box2.maxY,
					Math.max(box2.maxZ, box.maxZ)
			);
		}
		else {
			box3 = this.getBoundingBox();
		}

		return box3.expand(attackRange, 0.0, attackRange);
	}

	@Override
	public boolean tryAttack(ServerWorld world, Entity target) {
		float f = (float) this.getAttributeValue(EntityAttributes.ATTACK_DAMAGE);
		ItemStack itemStack = this.getWeaponStack();
		DamageSource damageSource = itemStack.getDamageSource(this, () -> this.getDamageSources().mobAttack(this));
		f = EnchantmentHelper.getDamage(world, itemStack, target, damageSource, f);
		f += itemStack.getItem().getBonusAttackDamage(target, f, damageSource);
		Vec3d vec3d = target.getVelocity();
		boolean bl = target.damage(world, damageSource, f);
		if (bl) {
			this.knockbackTarget(target, this.getAttackKnockbackAgainst(target, damageSource), vec3d);
			if (target instanceof LivingEntity livingEntity) {
				itemStack.postHit(livingEntity, this);
			}

			EnchantmentHelper.onTargetDamaged(world, target, damageSource);
			this.onAttacking(target);
			this.playAttackSound();
		}

		this.useAttackEnchantmentEffects();
		return bl;
	}

	@Override
	protected void swimUpward(TagKey<Fluid> fluid) {
		if (this.getNavigation().canSwim()) {
			super.swimUpward(fluid);
		}
		else {
			this.setVelocity(this.getVelocity().add(0.0, 0.3, 0.0));
		}
	}

	@VisibleForTesting
	public void clearGoalsAndTasks() {
		this.clearGoals(goal -> true);
		this.getBrain().clear();
	}

	public void clearGoals(Predicate<Goal> predicate) {
		this.goalSelector.clear(predicate);
	}

	@Override
	protected void removeFromDimension() {
		super.removeFromDimension();

		for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {
			ItemStack itemStack = this.getEquippedStack(equipmentSlot);
			if (!itemStack.isEmpty()) {
				itemStack.setCount(0);
			}
		}
	}

	@Override
	public @Nullable ItemStack getPickBlockStack() {
		SpawnEggItem spawnEggItem = SpawnEggItem.forEntity(this.getType());
		return spawnEggItem == null ? null : new ItemStack(spawnEggItem);
	}

	@Override
	protected void updateAttribute(RegistryEntry<EntityAttribute> attribute) {
		super.updateAttribute(attribute);
		if (attribute.matches(EntityAttributes.FOLLOW_RANGE) || attribute.matches(EntityAttributes.TEMPT_RANGE)) {
			this.getNavigation().updateRange();
		}
	}

	@Override
	public void registerTracking(ServerWorld world, DebugTrackable.Tracker tracker) {
		tracker.track(
				DebugSubscriptionTypes.ENTITY_PATHS, () -> {
					Path path = this.getNavigation().getCurrentPath();
					return path != null && path.getDebugNodeInfos() != null ? new EntityPathDebugData(
							path.copy(),
							this.getNavigation().getNodeReachProximity()
					) : null;
				}
		);
		tracker.track(
				DebugSubscriptionTypes.GOAL_SELECTORS, () -> {
					Set<PrioritizedGoal> set = this.goalSelector.getGoals();
					List<GoalSelectorDebugData.Goal> list = new ArrayList<>(set.size());
					set.forEach(goal -> list.add(new GoalSelectorDebugData.Goal(
							goal.getPriority(),
							goal.isRunning(),
							goal.getGoal().getClass().getSimpleName()
					)));
					return new GoalSelectorDebugData(list);
				}
		);
		if (!this.brain.isEmpty()) {
			tracker.track(DebugSubscriptionTypes.BRAINS, () -> BrainDebugData.fromEntity(world, this));
		}
	}

	public float getRiderChargingSpeedMultiplier() {
		return 1.0F;
	}
}
