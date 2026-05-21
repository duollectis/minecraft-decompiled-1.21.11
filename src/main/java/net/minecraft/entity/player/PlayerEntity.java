package net.minecraft.entity.player;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.math.IntMath;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.*;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.DefaultPermissions;
import net.minecraft.command.permission.PermissionPredicate;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.component.type.BlocksAttacksComponent;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.dialog.type.Dialog;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.AbstractNautilusEntity;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.vehicle.CommandBlockMinecartEntity;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.StackReference;
import net.minecraft.inventory.StackWithSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.*;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Team;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stat;
import net.minecraft.stat.Stats;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.village.TradeOfferList;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;

/**
 * {@code PlayerEntity}.
 */
public abstract class PlayerEntity extends PlayerLikeEntity implements ContainerUser {

	public static final int MAX_HEALTH = 20;
	public static final int SLEEP_DURATION = 100;
	public static final int WAKE_UP_DURATION = 10;
	public static final int ENDER_SLOT_OFFSET = 200;
	public static final int HELD_ITEM_SLOT_OFFSET = 499;
	public static final int CRAFTING_SLOT_OFFSET = 500;
	public static final float BLOCK_INTERACTION_RANGE = 4.5F;
	public static final float ENTITY_INTERACTION_RANGE = 3.0F;
	private static final int EXPLOSION_RESET_GRACE_TIME = 40;
	private static final TrackedData<Float>
			ABSORPTION_AMOUNT =
			DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Integer>
			SCORE =
			DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<OptionalInt> LEFT_SHOULDER_PARROT_VARIANT_ID = DataTracker.registerData(
			PlayerEntity.class, TrackedDataHandlerRegistry.OPTIONAL_INT
	);
	private static final TrackedData<OptionalInt> RIGHT_SHOULDER_PARROT_VARIANT_ID = DataTracker.registerData(
			PlayerEntity.class, TrackedDataHandlerRegistry.OPTIONAL_INT
	);
	private static final short DEFAULT_ENCHANTING_SEED = 0;
	private static final float DEFAULT_EXPERIENCE_PROGRESS = 0.0F;
	private static final int DEFAULT_EXPERIENCE_LEVEL = 0;
	private static final int DEFAULT_TOTAL_EXPERIENCE = 0;
	private static final int DEFAULT_SLEEP_TIMER = 0;
	private static final int DEFAULT_LAST_LEVEL_UP_SOUND_TIME = 0;
	private static final int DEFAULT_ABILITY_RESYNC_COUNTDOWN = 0;
	private static final boolean DEFAULT_REDUCED_DEBUG_INFO = false;
	private static final int DEFAULT_EXPERIENCE_PICKUP_DELAY = 0;
	public static final float BLOCK_INTERACTION_RANGE_CREATIVE_BONUS = 2.0F;
	final PlayerInventory inventory;
	protected EnderChestInventory enderChestInventory = new EnderChestInventory();
	public final PlayerScreenHandler playerScreenHandler;
	public ScreenHandler currentScreenHandler;
	protected HungerManager hungerManager = new HungerManager();
	protected int abilityResyncCountdown;
	public int experiencePickUpDelay;
	private int sleepTimer = 0;
	protected boolean isSubmergedInWater;
	private final PlayerAbilities abilities = new PlayerAbilities();
	public int experienceLevel = 0;
	public int totalExperience = 0;
	public float experienceProgress = 0.0F;
	protected int enchantingTableSeed = 0;
	protected final float baseFlySpeed = 0.02F;
	private int lastPlayedLevelUpSoundTime;
	private final GameProfile gameProfile;
	private boolean reducedDebugInfo;
	private ItemStack selectedItem = ItemStack.EMPTY;
	private final ItemCooldownManager itemCooldownManager = this.createCooldownManager();
	private Optional<GlobalPos> lastDeathPos = Optional.empty();
	public @Nullable FishingBobberEntity fishHook;
	protected float damageTiltYaw;
	public @Nullable Vec3d currentExplosionImpactPos;
	public @Nullable Entity explodedBy;
	private boolean ignoreFallDamageFromCurrentExplosion = false;
	private int currentExplosionResetGraceTime = 0;

	public PlayerEntity(World world, GameProfile profile) {
		super(EntityType.PLAYER, world);
		this.setUuid(profile.id());
		this.gameProfile = profile;
		this.inventory = new PlayerInventory(this, this.equipment);
		this.playerScreenHandler = new PlayerScreenHandler(this.inventory, !world.isClient(), this);
		this.currentScreenHandler = this.playerScreenHandler;
	}

	@Override
	protected EntityEquipment createEquipment() {
		return new PlayerEquipment(this);
	}

	public boolean isBlockBreakingRestricted(World world, BlockPos pos, GameMode gameMode) {
		if (!gameMode.isBlockBreakingRestricted()) {
			return false;
		}
		else if (gameMode == GameMode.SPECTATOR) {
			return true;
		}
		else if (this.canModifyBlocks()) {
			return false;
		}
		else {
			ItemStack itemStack = this.getMainHandStack();
			return itemStack.isEmpty() || !itemStack.canBreak(new CachedBlockPosition(world, pos, false));
		}
	}

	public static DefaultAttributeContainer.Builder createPlayerAttributes() {
		return LivingEntity.createLivingAttributes()
		                   .add(EntityAttributes.ATTACK_DAMAGE, 1.0)
		                   .add(EntityAttributes.MOVEMENT_SPEED, 0.1F)
		                   .add(EntityAttributes.ATTACK_SPEED)
		                   .add(EntityAttributes.LUCK)
		                   .add(EntityAttributes.BLOCK_INTERACTION_RANGE, 4.5)
		                   .add(EntityAttributes.ENTITY_INTERACTION_RANGE, 3.0)
		                   .add(EntityAttributes.BLOCK_BREAK_SPEED)
		                   .add(EntityAttributes.SUBMERGED_MINING_SPEED)
		                   .add(EntityAttributes.SNEAKING_SPEED)
		                   .add(EntityAttributes.MINING_EFFICIENCY)
		                   .add(EntityAttributes.SWEEPING_DAMAGE_RATIO)
		                   .add(EntityAttributes.WAYPOINT_TRANSMIT_RANGE, 6.0E7)
		                   .add(EntityAttributes.WAYPOINT_RECEIVE_RANGE, 6.0E7);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(ABSORPTION_AMOUNT, 0.0F);
		builder.add(SCORE, 0);
		builder.add(LEFT_SHOULDER_PARROT_VARIANT_ID, OptionalInt.empty());
		builder.add(RIGHT_SHOULDER_PARROT_VARIANT_ID, OptionalInt.empty());
	}

	@Override
	public void tick() {
		this.noClip = this.isSpectator();
		if (this.isSpectator() || this.hasVehicle()) {
			this.setOnGround(false);
		}

		if (this.experiencePickUpDelay > 0) {
			this.experiencePickUpDelay--;
		}

		if (this.isSleeping()) {
			this.sleepTimer++;
			if (this.sleepTimer > 100) {
				this.sleepTimer = 100;
			}

			if (!this.getEntityWorld().isClient()
					&& !this.getEntityWorld()
					        .getEnvironmentAttributes()
					        .getAttributeValue(EnvironmentAttributes.BED_RULE_GAMEPLAY, this.getEntityPos())
					        .canSleep(this.getEntityWorld())) {
				this.wakeUp(false, true);
			}
		}
		else if (this.sleepTimer > 0) {
			this.sleepTimer++;
			if (this.sleepTimer >= 110) {
				this.sleepTimer = 0;
			}
		}

		this.updateWaterSubmersionState();
		super.tick();
		int i = 29999999;
		double d = MathHelper.clamp(this.getX(), -2.9999999E7, 2.9999999E7);
		double e = MathHelper.clamp(this.getZ(), -2.9999999E7, 2.9999999E7);
		if (d != this.getX() || e != this.getZ()) {
			this.setPosition(d, this.getY(), e);
		}

		this.ticksSinceLastAttack++;
		this.ticksSinceHandEquipping++;
		ItemStack itemStack = this.getMainHandStack();
		if (!ItemStack.areEqual(this.selectedItem, itemStack)) {
			if (!ItemStack.areItemsEqual(this.selectedItem, itemStack)) {
				this.resetTicksSince();
			}

			this.selectedItem = itemStack.copy();
		}

		if (!this.isSubmergedIn(FluidTags.WATER) && this.isEquipped(Items.TURTLE_HELMET)) {
			this.updateTurtleHelmet();
		}

		this.itemCooldownManager.update();
		this.updatePose();
		if (this.currentExplosionResetGraceTime > 0) {
			this.currentExplosionResetGraceTime--;
		}
	}

	@Override
	protected float getMaxRelativeHeadRotation() {
		return this.isBlocking() ? 15.0F : super.getMaxRelativeHeadRotation();
	}

	/**
	 * Определяет, следует ли cancel interaction.
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldCancelInteraction() {
		return this.isSneaking();
	}

	/**
	 * Определяет, следует ли dismount.
	 *
	 * @return boolean — результат операции
	 */
	protected boolean shouldDismount() {
		return this.isSneaking();
	}

	/**
	 * Clip at ledge.
	 *
	 * @return boolean — результат операции
	 */
	protected boolean clipAtLedge() {
		return this.isSneaking();
	}

	/**
	 * Обновляет water submersion state.
	 *
	 * @return boolean — результат операции
	 */
	protected boolean updateWaterSubmersionState() {
		this.isSubmergedInWater = this.isSubmergedIn(FluidTags.WATER);
		return this.isSubmergedInWater;
	}

	@Override
	public void onBubbleColumnSurfaceCollision(boolean drag, BlockPos pos) {
		if (!this.getAbilities().flying) {
			super.onBubbleColumnSurfaceCollision(drag, pos);
		}
	}

	@Override
	public void onBubbleColumnCollision(boolean drag) {
		if (!this.getAbilities().flying) {
			super.onBubbleColumnCollision(drag);
		}
	}

	private void updateTurtleHelmet() {
		this.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 200, 0, false, false, true));
	}

	private boolean isEquipped(Item item) {
		for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {
			ItemStack itemStack = this.getEquippedStack(equipmentSlot);
			EquippableComponent equippableComponent = itemStack.get(DataComponentTypes.EQUIPPABLE);
			if (itemStack.isOf(item) && equippableComponent != null && equippableComponent.slot() == equipmentSlot) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Создаёт cooldown manager.
	 *
	 * @return ItemCooldownManager — результат операции
	 */
	protected ItemCooldownManager createCooldownManager() {
		return new ItemCooldownManager();
	}

	/**
	 * Обновляет pose.
	 */
	protected void updatePose() {
		if (this.canChangeIntoPose(EntityPose.SWIMMING)) {
			EntityPose entityPose = this.getExpectedPose();
			EntityPose entityPose2;
			if (this.isSpectator() || this.hasVehicle() || this.canChangeIntoPose(entityPose)) {
				entityPose2 = entityPose;
			}
			else if (this.canChangeIntoPose(EntityPose.CROUCHING)) {
				entityPose2 = EntityPose.CROUCHING;
			}
			else {
				entityPose2 = EntityPose.SWIMMING;
			}

			this.setPose(entityPose2);
		}
	}

	private EntityPose getExpectedPose() {
		if (this.isSleeping()) {
			return EntityPose.SLEEPING;
		}
		else if (this.isSwimming()) {
			return EntityPose.SWIMMING;
		}
		else if (this.isGliding()) {
			return EntityPose.GLIDING;
		}
		else if (this.isUsingRiptide()) {
			return EntityPose.SPIN_ATTACK;
		}
		else {
			return this.isSneaking() && !this.abilities.flying ? EntityPose.CROUCHING : EntityPose.STANDING;
		}
	}

	/**
	 * Проверяет возможность change into pose.
	 *
	 * @param pose pose
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	protected boolean canChangeIntoPose(EntityPose pose) {
		return this
				.getEntityWorld()
				.isSpaceEmpty(this, this.getDimensions(pose).getBoxAt(this.getEntityPos()).contract(1.0E-7));
	}

	@Override
	protected SoundEvent getSwimSound() {
		return SoundEvents.ENTITY_PLAYER_SWIM;
	}

	@Override
	protected SoundEvent getSplashSound() {
		return SoundEvents.ENTITY_PLAYER_SPLASH;
	}

	@Override
	protected SoundEvent getHighSpeedSplashSound() {
		return SoundEvents.ENTITY_PLAYER_SPLASH_HIGH_SPEED;
	}

	@Override
	public int getDefaultPortalCooldown() {
		return 10;
	}

	@Override
	public void playSound(SoundEvent sound, float volume, float pitch) {
		this
				.getEntityWorld()
				.playSound(this, this.getX(), this.getY(), this.getZ(), sound, this.getSoundCategory(), volume, pitch);
	}

	@Override
	public SoundCategory getSoundCategory() {
		return SoundCategory.PLAYERS;
	}

	@Override
	protected int getBurningDuration() {
		return 20;
	}

	@Override
	public void handleStatus(byte status) {
		if (status == 9) {
			this.consumeItem();
		}
		else if (status == 23) {
			this.setReducedDebugInfo(false);
		}
		else if (status == 22) {
			this.setReducedDebugInfo(true);
		}
		else {
			super.handleStatus(status);
		}
	}

	/**
	 * Закрывает handled screen.
	 */
	protected void closeHandledScreen() {
		this.currentScreenHandler = this.playerScreenHandler;
	}

	/**
	 * Обрабатывает событие handled screen closed.
	 */
	protected void onHandledScreenClosed() {
	}

	@Override
	public void tickRiding() {
		if (!this.getEntityWorld().isClient() && this.shouldDismount() && this.hasVehicle()) {
			this.stopRiding();
			this.setSneaking(false);
		}
		else {
			super.tickRiding();
		}
	}

	@Override
	public void tickMovement() {
		if (this.abilityResyncCountdown > 0) {
			this.abilityResyncCountdown--;
		}

		this.tickHunger();
		this.inventory.updateItems();
		if (this.abilities.flying && !this.hasVehicle()) {
			this.onLanding();
		}

		super.tickMovement();
		this.tickHandSwing();
		this.headYaw = this.getYaw();
		this.setMovementSpeed((float) this.getAttributeValue(EntityAttributes.MOVEMENT_SPEED));
		if (this.getHealth() > 0.0F && !this.isSpectator()) {
			Box box;
			if (this.hasVehicle() && !this.getVehicle().isRemoved()) {
				box = this.getBoundingBox().union(this.getVehicle().getBoundingBox()).expand(1.0, 0.0, 1.0);
			}
			else {
				box = this.getBoundingBox().expand(1.0, 0.5, 1.0);
			}

			List<Entity> list = this.getEntityWorld().getOtherEntities(this, box);
			List<Entity> list2 = Lists.newArrayList();

			for (Entity entity : list) {
				if (entity.getType() == EntityType.EXPERIENCE_ORB) {
					list2.add(entity);
				}
				else if (!entity.isRemoved()) {
					this.collideWithEntity(entity);
				}
			}

			if (!list2.isEmpty()) {
				this.collideWithEntity(Util.getRandom(list2, this.random));
			}
		}

		this.handleShoulderEntities();
	}

	/**
	 * Выполняет тик обновления для hunger.
	 */
	protected void tickHunger() {
	}

	/**
	 * Обрабатывает shoulder entities.
	 */
	public void handleShoulderEntities() {
	}

	/**
	 * Бросает shoulder entities.
	 */
	protected void dropShoulderEntities() {
	}

	private void collideWithEntity(Entity entity) {
		entity.onPlayerCollision(this);
	}

	public int getScore() {
		return this.dataTracker.get(SCORE);
	}

	public void setScore(int score) {
		this.dataTracker.set(SCORE, score);
	}

	/**
	 * Добавляет score.
	 *
	 * @param score score
	 */
	public void addScore(int score) {
		int i = this.getScore();
		this.dataTracker.set(SCORE, i + score);
	}

	/**
	 * Использует riptide.
	 *
	 * @param riptideTicks riptide ticks
	 * @param riptideAttackDamage riptide attack damage
	 * @param stack stack
	 */
	public void useRiptide(int riptideTicks, float riptideAttackDamage, ItemStack stack) {
		this.riptideTicks = riptideTicks;
		this.riptideAttackDamage = riptideAttackDamage;
		this.riptideStack = stack;
		if (!this.getEntityWorld().isClient()) {
			this.dropShoulderEntities();
			this.setLivingFlag(4, true);
		}
	}

	@Override
	public ItemStack getWeaponStack() {
		return this.isUsingRiptide() && this.riptideStack != null ? this.riptideStack : super.getWeaponStack();
	}

	@Override
	public void onDeath(DamageSource damageSource) {
		super.onDeath(damageSource);
		this.refreshPosition();
		if (!this.isSpectator() && this.getEntityWorld() instanceof ServerWorld serverWorld) {
			this.drop(serverWorld, damageSource);
		}

		if (damageSource != null) {
			this.setVelocity(
					-MathHelper.cos((this.getDamageTiltYaw() + this.getYaw()) * (float) (Math.PI / 180.0)) * 0.1F,
					0.1F,
					-MathHelper.sin((this.getDamageTiltYaw() + this.getYaw()) * (float) (Math.PI / 180.0)) * 0.1F
			);
		}
		else {
			this.setVelocity(0.0, 0.1, 0.0);
		}

		this.incrementStat(Stats.DEATHS);
		this.resetStat(Stats.CUSTOM.getOrCreateStat(Stats.TIME_SINCE_DEATH));
		this.resetStat(Stats.CUSTOM.getOrCreateStat(Stats.TIME_SINCE_REST));
		this.extinguish();
		this.setOnFire(false);
		this.setLastDeathPos(Optional.of(GlobalPos.create(this.getEntityWorld().getRegistryKey(), this.getBlockPos())));
	}

	@Override
	protected void dropInventory(ServerWorld world) {
		super.dropInventory(world);
		if (!world.getGameRules().getValue(GameRules.KEEP_INVENTORY)) {
			this.vanishCursedItems();
			this.inventory.dropAll();
		}
	}

	/**
	 * Vanish cursed items.
	 */
	protected void vanishCursedItems() {
		for (int i = 0; i < this.inventory.size(); i++) {
			ItemStack itemStack = this.inventory.getStack(i);
			if (!itemStack.isEmpty() && EnchantmentHelper.hasAnyEnchantmentsWith(
					itemStack,
					EnchantmentEffectComponentTypes.PREVENT_EQUIPMENT_DROP
			)) {
				this.inventory.removeStack(i);
			}
		}
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return source.getType().effects().getSound();
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_PLAYER_DEATH;
	}

	/**
	 * Бросает creative stack.
	 *
	 * @param stack stack
	 */
	public void dropCreativeStack(ItemStack stack) {
	}

	/**
	 * Бросает item.
	 *
	 * @param stack stack
	 * @param retainOwnership retain ownership
	 *
	 * @return @Nullable ItemEntity — результат операции
	 */
	public @Nullable ItemEntity dropItem(ItemStack stack, boolean retainOwnership) {
		return this.dropItem(stack, false, retainOwnership);
	}

	public float getBlockBreakingSpeed(BlockState block) {
		float f = this.inventory.getSelectedStack().getMiningSpeedMultiplier(block);
		if (f > 1.0F) {
			f += (float) this.getAttributeValue(EntityAttributes.MINING_EFFICIENCY);
		}

		if (StatusEffectUtil.hasHaste(this)) {
			f *= 1.0F + (StatusEffectUtil.getHasteAmplifier(this) + 1) * 0.2F;
		}

		if (this.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
			float g = switch (this.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
				case 0 -> 0.3F;
				case 1 -> 0.09F;
				case 2 -> 0.0027F;
				default -> 8.1E-4F;
			};
			f *= g;
		}

		f *= (float) this.getAttributeValue(EntityAttributes.BLOCK_BREAK_SPEED);
		if (this.isSubmergedIn(FluidTags.WATER)) {
			f *= (float) this.getAttributeInstance(EntityAttributes.SUBMERGED_MINING_SPEED).getValue();
		}

		if (!this.isOnGround()) {
			f /= 5.0F;
		}

		return f;
	}

	/**
	 * Проверяет возможность harvest.
	 *
	 * @param state state
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canHarvest(BlockState state) {
		return !state.isToolRequired() || this.inventory.getSelectedStack().isSuitableFor(state);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		this.setUuid(this.gameProfile.id());
		this.inventory.readData(view.getTypedListView("Inventory", StackWithSlot.CODEC));
		this.inventory.setSelectedSlot(view.getInt("SelectedItemSlot", 0));
		this.sleepTimer = view.getShort("SleepTimer", (short) 0);
		this.experienceProgress = view.getFloat("XpP", 0.0F);
		this.experienceLevel = view.getInt("XpLevel", 0);
		this.totalExperience = view.getInt("XpTotal", 0);
		this.enchantingTableSeed = view.getInt("XpSeed", 0);
		if (this.enchantingTableSeed == 0) {
			this.enchantingTableSeed = this.random.nextInt();
		}

		this.setScore(view.getInt("Score", 0));
		this.hungerManager.readData(view);
		view.<PlayerAbilities.Packed>read("abilities", PlayerAbilities.Packed.CODEC).ifPresent(this.abilities::unpack);
		this.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED).setBaseValue(this.abilities.getWalkSpeed());
		this.enderChestInventory.readData(view.getTypedListView("EnderItems", StackWithSlot.CODEC));
		this.setLastDeathPos(view.read("LastDeathLocation", GlobalPos.CODEC));
		this.currentExplosionImpactPos = view.<Vec3d>read("current_explosion_impact_pos", Vec3d.CODEC).orElse(null);
		this.ignoreFallDamageFromCurrentExplosion = view.getBoolean("ignore_fall_damage_from_current_explosion", false);
		this.currentExplosionResetGraceTime = view.getInt("current_impulse_context_reset_grace_time", 0);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		NbtHelper.writeDataVersion(view);
		this.inventory.writeData(view.getListAppender("Inventory", StackWithSlot.CODEC));
		view.putInt("SelectedItemSlot", this.inventory.getSelectedSlot());
		view.putShort("SleepTimer", (short) this.sleepTimer);
		view.putFloat("XpP", this.experienceProgress);
		view.putInt("XpLevel", this.experienceLevel);
		view.putInt("XpTotal", this.totalExperience);
		view.putInt("XpSeed", this.enchantingTableSeed);
		view.putInt("Score", this.getScore());
		this.hungerManager.writeData(view);
		view.put("abilities", PlayerAbilities.Packed.CODEC, this.abilities.pack());
		this.enderChestInventory.writeData(view.getListAppender("EnderItems", StackWithSlot.CODEC));
		this.lastDeathPos.ifPresent(pos -> view.put("LastDeathLocation", GlobalPos.CODEC, pos));
		view.putNullable("current_explosion_impact_pos", Vec3d.CODEC, this.currentExplosionImpactPos);
		view.putBoolean("ignore_fall_damage_from_current_explosion", this.ignoreFallDamageFromCurrentExplosion);
		view.putInt("current_impulse_context_reset_grace_time", this.currentExplosionResetGraceTime);
	}

	@Override
	public boolean isInvulnerableTo(ServerWorld world, DamageSource source) {
		if (super.isInvulnerableTo(world, source)) {
			return true;
		}
		else if (source.isIn(DamageTypeTags.IS_DROWNING)) {
			return !world.getGameRules().getValue(GameRules.DROWNING_DAMAGE);
		}
		else if (source.isIn(DamageTypeTags.IS_FALL)) {
			return !world.getGameRules().getValue(GameRules.FALL_DAMAGE);
		}
		else if (source.isIn(DamageTypeTags.IS_FIRE)) {
			return !world.getGameRules().getValue(GameRules.FIRE_DAMAGE);
		}
		else {
			return source.isIn(DamageTypeTags.IS_FREEZING) ? !world.getGameRules().getValue(GameRules.FREEZE_DAMAGE)
			                                               : false;
		}
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		if (this.isInvulnerableTo(world, source)) {
			return false;
		}
		else if (this.abilities.invulnerable && !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return false;
		}
		else {
			this.despawnCounter = 0;
			if (this.isDead()) {
				return false;
			}
			else {
				this.dropShoulderEntities();
				if (source.isScaledWithDifficulty()) {
					if (world.getDifficulty() == Difficulty.PEACEFUL) {
						amount = 0.0F;
					}

					if (world.getDifficulty() == Difficulty.EASY) {
						amount = Math.min(amount / 2.0F + 1.0F, amount);
					}

					if (world.getDifficulty() == Difficulty.HARD) {
						amount = amount * 3.0F / 2.0F;
					}
				}

				return amount == 0.0F ? false : super.damage(world, source, amount);
			}
		}
	}

	@Override
	protected void takeShieldHit(ServerWorld world, LivingEntity attacker) {
		super.takeShieldHit(world, attacker);
		ItemStack itemStack = this.getBlockingItem();
		BlocksAttacksComponent
				blocksAttacksComponent =
				itemStack != null ? itemStack.get(DataComponentTypes.BLOCKS_ATTACKS) : null;
		float f = attacker.getWeaponDisableBlockingForSeconds();
		if (f > 0.0F && blocksAttacksComponent != null) {
			blocksAttacksComponent.applyShieldCooldown(world, this, f, itemStack);
		}
	}

	@Override
	public boolean canTakeDamage() {
		return !this.getAbilities().invulnerable && super.canTakeDamage();
	}

	/**
	 * Определяет, следует ли damage player.
	 *
	 * @param player player
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldDamagePlayer(PlayerEntity player) {
		AbstractTeam abstractTeam = this.getScoreboardTeam();
		AbstractTeam abstractTeam2 = player.getScoreboardTeam();
		if (abstractTeam == null) {
			return true;
		}
		else {
			return !abstractTeam.isEqual(abstractTeam2) ? true : abstractTeam.isFriendlyFireAllowed();
		}
	}

	@Override
	public void damageArmor(DamageSource source, float amount) {
		this.damageEquipment(
				source,
				amount,
				EquipmentSlot.FEET,
				EquipmentSlot.LEGS,
				EquipmentSlot.CHEST,
				EquipmentSlot.HEAD
		);
	}

	@Override
	public void damageHelmet(DamageSource source, float amount) {
		this.damageEquipment(source, amount, EquipmentSlot.HEAD);
	}

	@Override
	protected void applyDamage(ServerWorld world, DamageSource source, float amount) {
		if (!this.isInvulnerableTo(world, source)) {
			amount = this.applyArmorToDamage(source, amount);
			amount = this.modifyAppliedDamage(source, amount);
			float var8 = Math.max(amount - this.getAbsorptionAmount(), 0.0F);
			this.setAbsorptionAmount(this.getAbsorptionAmount() - (amount - var8));
			float g = amount - var8;
			if (g > 0.0F && g < 3.4028235E37F) {
				this.increaseStat(Stats.DAMAGE_ABSORBED, Math.round(g * 10.0F));
			}

			if (var8 != 0.0F) {
				this.addExhaustion(source.getExhaustion());
				this.getDamageTracker().onDamage(source, var8);
				this.setHealth(this.getHealth() - var8);
				if (var8 < 3.4028235E37F) {
					this.increaseStat(Stats.DAMAGE_TAKEN, Math.round(var8 * 10.0F));
				}

				this.emitGameEvent(GameEvent.ENTITY_DAMAGE);
			}
		}
	}

	/**
	 * Определяет, следует ли filter text.
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldFilterText() {
		return false;
	}

	/**
	 * Открывает edit sign screen.
	 *
	 * @param sign sign
	 * @param front front
	 */
	public void openEditSignScreen(SignBlockEntity sign, boolean front) {
	}

	/**
	 * Открывает command block minecart screen.
	 *
	 * @param minecart minecart
	 */
	public void openCommandBlockMinecartScreen(CommandBlockMinecartEntity minecart) {
	}

	/**
	 * Открывает command block screen.
	 *
	 * @param commandBlock command block
	 */
	public void openCommandBlockScreen(CommandBlockBlockEntity commandBlock) {
	}

	/**
	 * Открывает structure block screen.
	 *
	 * @param structureBlock structure block
	 */
	public void openStructureBlockScreen(StructureBlockBlockEntity structureBlock) {
	}

	/**
	 * Открывает test block screen.
	 *
	 * @param testBlock test block
	 */
	public void openTestBlockScreen(TestBlockEntity testBlock) {
	}

	/**
	 * Открывает test instance block screen.
	 *
	 * @param testInstanceBlock test instance block
	 */
	public void openTestInstanceBlockScreen(TestInstanceBlockEntity testInstanceBlock) {
	}

	/**
	 * Открывает jigsaw screen.
	 *
	 * @param jigsaw jigsaw
	 */
	public void openJigsawScreen(JigsawBlockEntity jigsaw) {
	}

	/**
	 * Открывает horse inventory.
	 *
	 * @param horse horse
	 * @param inventory inventory
	 */
	public void openHorseInventory(AbstractHorseEntity horse, Inventory inventory) {
	}

	/**
	 * Открывает nautilus inventory.
	 *
	 * @param nautilus nautilus
	 * @param inventory inventory
	 */
	public void openNautilusInventory(AbstractNautilusEntity nautilus, Inventory inventory) {
	}

	/**
	 * Открывает handled screen.
	 *
	 * @param factory factory
	 *
	 * @return OptionalInt — результат операции
	 */
	public OptionalInt openHandledScreen(@Nullable NamedScreenHandlerFactory factory) {
		return OptionalInt.empty();
	}

	/**
	 * Открывает dialog.
	 *
	 * @param dialog dialog
	 */
	public void openDialog(RegistryEntry<Dialog> dialog) {
	}

	public void sendTradeOffers(
			int syncId,
			TradeOfferList offers,
			int levelProgress,
			int experience,
			boolean leveled,
			boolean refreshable
	) {
	}

	/**
	 * Использует book.
	 *
	 * @param book book
	 * @param hand hand
	 */
	public void useBook(ItemStack book, Hand hand) {
	}

	/**
	 * Interact.
	 *
	 * @param entity entity
	 * @param hand hand
	 *
	 * @return ActionResult — результат операции
	 */
	public ActionResult interact(Entity entity, Hand hand) {
		if (this.isSpectator()) {
			if (entity instanceof NamedScreenHandlerFactory) {
				this.openHandledScreen((NamedScreenHandlerFactory) entity);
			}

			return ActionResult.PASS;
		}
		else {
			ItemStack itemStack = this.getStackInHand(hand);
			ItemStack itemStack2 = itemStack.copy();
			ActionResult actionResult = entity.interact(this, hand);
			if (actionResult.isAccepted()) {
				if (this.isInCreativeMode() && itemStack == this.getStackInHand(hand)
						&& itemStack.getCount() < itemStack2.getCount()) {
					itemStack.setCount(itemStack2.getCount());
				}

				return actionResult;
			}
			else {
				if (!itemStack.isEmpty() && entity instanceof LivingEntity) {
					if (this.isInCreativeMode()) {
						itemStack = itemStack2;
					}

					ActionResult actionResult2 = itemStack.useOnEntity(this, (LivingEntity) entity, hand);
					if (actionResult2.isAccepted()) {
						this
								.getEntityWorld()
								.emitGameEvent(
										GameEvent.ENTITY_INTERACT,
										entity.getEntityPos(),
										GameEvent.Emitter.of(this)
								);
						if (itemStack.isEmpty() && !this.isInCreativeMode()) {
							this.setStackInHand(hand, ItemStack.EMPTY);
						}

						return actionResult2;
					}
				}

				return ActionResult.PASS;
			}
		}
	}

	@Override
	public void dismountVehicle() {
		super.dismountVehicle();
		this.ridingCooldown = 0;
	}

	@Override
	protected boolean isImmobile() {
		return super.isImmobile() || this.isSleeping();
	}

	@Override
	public boolean shouldSwimInFluids() {
		return !this.abilities.flying;
	}

	@Override
	protected Vec3d adjustMovementForSneaking(Vec3d movement, MovementType type) {
		float f = this.getStepHeight();
		if (!this.abilities.flying
				&& !(movement.y > 0.0)
				&& (type == MovementType.SELF || type == MovementType.PLAYER)
				&& this.clipAtLedge()
				&& this.isStandingOnSurface(f)) {
			double d = movement.x;
			double e = movement.z;
			double g = 0.05;
			double h = Math.signum(d) * 0.05;

			double i;
			for (i = Math.signum(e) * 0.05; d != 0.0 && this.isSpaceAroundPlayerEmpty(d, 0.0, f); d -= h) {
				if (Math.abs(d) <= 0.05) {
					d = 0.0;
					break;
				}
			}

			while (e != 0.0 && this.isSpaceAroundPlayerEmpty(0.0, e, f)) {
				if (Math.abs(e) <= 0.05) {
					e = 0.0;
					break;
				}

				e -= i;
			}

			while (d != 0.0 && e != 0.0 && this.isSpaceAroundPlayerEmpty(d, e, f)) {
				if (Math.abs(d) <= 0.05) {
					d = 0.0;
				}
				else {
					d -= h;
				}

				if (Math.abs(e) <= 0.05) {
					e = 0.0;
				}
				else {
					e -= i;
				}
			}

			return new Vec3d(d, movement.y, e);
		}
		else {
			return movement;
		}
	}

	private boolean isStandingOnSurface(float stepHeight) {
		return this.isOnGround() || this.fallDistance < stepHeight && !this.isSpaceAroundPlayerEmpty(
				0.0,
				0.0,
				stepHeight - this.fallDistance
		);
	}

	private boolean isSpaceAroundPlayerEmpty(double offsetX, double offsetZ, double stepHeight) {
		Box box = this.getBoundingBox();
		return this.getEntityWorld()
		           .isSpaceEmpty(
				           this,
				           new Box(
						           box.minX + 1.0E-7 + offsetX,
						           box.minY - stepHeight - 1.0E-7,
						           box.minZ + 1.0E-7 + offsetZ,
						           box.maxX - 1.0E-7 + offsetX,
						           box.minY,
						           box.maxZ - 1.0E-7 + offsetZ
				           )
		           );
	}

	/**
	 * Attack.
	 *
	 * @param target target
	 */
	public void attack(Entity target) {
		if (!this.cannotAttack(target)) {
			float
					f =
					this.isUsingRiptide() ? this.riptideAttackDamage
					                      : (float) this.getAttributeValue(EntityAttributes.ATTACK_DAMAGE);
			ItemStack itemStack = this.getWeaponStack();
			DamageSource damageSource = this.getDamageSource(itemStack);
			float g = this.getAttackCooldownProgress(0.5F);
			float h = g * (this.getDamageAgainst(target, f, damageSource) - f);
			f *= this.getAttackCooldownDamageModifier();
			this.beforePlayerAttack();
			if (!this.tryDeflect(target)) {
				if (f > 0.0F || h > 0.0F) {
					boolean bl = g > 0.9F;
					boolean bl2;
					if (this.isSprinting() && bl) {
						this.playAttackSound(SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK);
						bl2 = true;
					}
					else {
						bl2 = false;
					}

					f += itemStack.getItem().getBonusAttackDamage(target, f, damageSource);
					boolean bl3 = bl && this.isCriticalHit(target);
					if (bl3) {
						f *= 1.5F;
					}

					float i = f + h;
					boolean bl4 = this.canUseSweepAttack(bl, bl3, bl2);
					float j = 0.0F;
					if (target instanceof LivingEntity livingEntity) {
						j = livingEntity.getHealth();
					}

					Vec3d vec3d = target.getVelocity();
					boolean bl5 = target.sidedDamage(damageSource, i);
					if (bl5) {
						this.knockbackTarget(
								target,
								this.getAttackKnockbackAgainst(target, damageSource) + (bl2 ? 0.5F : 0.0F),
								vec3d
						);
						if (bl4) {
							this.doSweepingAttack(target, f, damageSource, g);
						}

						this.addAttackParticlesAndSounds(target, bl3, bl4, bl, false, h);
						this.onAttacking(target);
						this.onTargetDamaged(target, itemStack, damageSource, true);
						this.handleAttackDamage(target, j);
						this.addExhaustion(0.1F);
					}
					else {
						this.playAttackSound(SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE);
					}
				}

				this.useAttackEnchantmentEffects();
			}
		}
	}

	private void playAttackSound(SoundEvent sound) {
		this
				.getEntityWorld()
				.playSound(null, this.getX(), this.getY(), this.getZ(), sound, this.getSoundCategory(), 1.0F, 1.0F);
	}

	private DamageSource getDamageSource(ItemStack stack) {
		return stack.getDamageSource(this, () -> this.getDamageSources().playerAttack(this));
	}

	private boolean cannotAttack(Entity target) {
		return !target.isAttackable() ? true : target.handleAttack(this);
	}

	private boolean tryDeflect(Entity entity) {
		if (entity.getType().isIn(EntityTypeTags.REDIRECTABLE_PROJECTILE)
				&& entity instanceof ProjectileEntity projectileEntity
				&& projectileEntity.deflect(
				ProjectileDeflection.REDIRECTED,
				this,
				LazyEntityReference.of(this),
				true
		)) {
			this
					.getEntityWorld()
					.playSound(
							null,
							this.getX(),
							this.getY(),
							this.getZ(),
							SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE,
							this.getSoundCategory()
					);
			return true;
		}
		else {
			return false;
		}
	}

	private boolean isCriticalHit(Entity target) {
		return this.fallDistance > 0.0
				&& !this.isOnGround()
				&& !this.isClimbing()
				&& !this.isTouchingWater()
				&& !this.hasBlindnessEffect()
				&& !this.hasVehicle()
				&& target instanceof LivingEntity
				&& !this.isSprinting();
	}

	private boolean canUseSweepAttack(boolean cooldownPassed, boolean criticalHit, boolean knockback) {
		if (cooldownPassed && !criticalHit && !knockback && this.isOnGround()) {
			double d = this.getMovement().horizontalLengthSquared();
			double e = this.getMovementSpeed() * 2.5;
			if (d < MathHelper.square(e)) {
				return this.getStackInHand(Hand.MAIN_HAND).isIn(ItemTags.SWORDS);
			}
		}

		return false;
	}

	private void addAttackParticlesAndSounds(
			Entity target,
			boolean criticalHit,
			boolean sweeping,
			boolean cooldownPassed,
			boolean pierce,
			float enchantDamage
	) {
		if (criticalHit) {
			this.playAttackSound(SoundEvents.ENTITY_PLAYER_ATTACK_CRIT);
			this.addCritParticles(target);
		}

		if (!criticalHit && !sweeping && !pierce) {
			this.playAttackSound(
					cooldownPassed ? SoundEvents.ENTITY_PLAYER_ATTACK_STRONG : SoundEvents.ENTITY_PLAYER_ATTACK_WEAK);
		}

		if (enchantDamage > 0.0F) {
			this.addEnchantedHitParticles(target);
		}
	}

	private void handleAttackDamage(Entity target, float healthBeforeAttack) {
		if (target instanceof LivingEntity) {
			float f = healthBeforeAttack - ((LivingEntity) target).getHealth();
			this.increaseStat(Stats.DAMAGE_DEALT, Math.round(f * 10.0F));
			if (this.getEntityWorld() instanceof ServerWorld && f > 2.0F) {
				int i = (int) (f * 0.5);
				((ServerWorld) this.getEntityWorld())
						.spawnParticles(
								ParticleTypes.DAMAGE_INDICATOR,
								target.getX(),
								target.getBodyY(0.5),
								target.getZ(),
								i,
								0.1,
								0.0,
								0.1,
								0.2
						);
			}
		}
	}

	private void onTargetDamaged(
			Entity target,
			ItemStack stack,
			DamageSource damageSource,
			boolean runEnchantmentEffects
	) {
		Entity entity = target;
		if (target instanceof EnderDragonPart) {
			entity = ((EnderDragonPart) target).owner;
		}

		boolean bl = false;
		if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
			if (entity instanceof LivingEntity livingEntity) {
				bl = stack.postHit(livingEntity, this);
			}

			if (runEnchantmentEffects) {
				EnchantmentHelper.onTargetDamaged(serverWorld, target, damageSource, stack);
			}
		}

		if (!this.getEntityWorld().isClient() && !stack.isEmpty() && entity instanceof LivingEntity) {
			if (bl) {
				stack.postDamageEntity((LivingEntity) entity, this);
			}

			if (stack.isEmpty()) {
				if (stack == this.getMainHandStack()) {
					this.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
				}
				else {
					this.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
				}
			}
		}
	}

	@Override
	public void knockbackTarget(Entity target, float strength, Vec3d playerTargetVelocity) {
		if (strength > 0.0F) {
			if (target instanceof LivingEntity livingEntity) {
				livingEntity.takeKnockback(
						strength,
						MathHelper.sin(this.getYaw() * (float) (Math.PI / 180.0)),
						-MathHelper.cos(this.getYaw() * (float) (Math.PI / 180.0))
				);
			}
			else {
				target.addVelocity(
						-MathHelper.sin(this.getYaw() * (float) (Math.PI / 180.0)) * strength,
						0.1,
						MathHelper.cos(this.getYaw() * (float) (Math.PI / 180.0)) * strength
				);
			}

			this.setVelocity(this.getVelocity().multiply(0.6, 1.0, 0.6));
			this.setSprinting(false);
		}

		if (target instanceof ServerPlayerEntity && target.knockedBack) {
			((ServerPlayerEntity) target).networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(target));
			target.knockedBack = false;
			target.setVelocity(playerTargetVelocity);
		}
	}

	@Override
	public float getSoundPitch() {
		return 1.0F;
	}

	private void doSweepingAttack(Entity target, float damage, DamageSource damageSource, float cooldownProgress) {
		this.playAttackSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP);
		if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
			float var12 = 1.0F + (float) this.getAttributeValue(EntityAttributes.SWEEPING_DAMAGE_RATIO) * damage;

			for (LivingEntity livingEntity : this
					.getEntityWorld()
					.getNonSpectatingEntities(LivingEntity.class, target.getBoundingBox().expand(1.0, 0.25, 1.0))) {
				if (livingEntity != this
						&& livingEntity != target
						&& !this.isTeammate(livingEntity)
						&& !(livingEntity instanceof ArmorStandEntity armorStandEntity && armorStandEntity.isMarker())
						&& this.squaredDistanceTo(livingEntity) < 9.0) {
					float g = this.getDamageAgainst(livingEntity, var12, damageSource) * cooldownProgress;
					if (livingEntity.damage(serverWorld, damageSource, g)) {
						livingEntity.takeKnockback(
								0.4F,
								MathHelper.sin(this.getYaw() * (float) (Math.PI / 180.0)),
								-MathHelper.cos(this.getYaw() * (float) (Math.PI / 180.0))
						);
						EnchantmentHelper.onTargetDamaged(serverWorld, livingEntity, damageSource);
					}
				}
			}

			double d = -MathHelper.sin(this.getYaw() * (float) (Math.PI / 180.0));
			double e = MathHelper.cos(this.getYaw() * (float) (Math.PI / 180.0));
			serverWorld.spawnParticles(
					ParticleTypes.SWEEP_ATTACK,
					this.getX() + d,
					this.getBodyY(0.5),
					this.getZ() + e,
					0,
					d,
					0.0,
					e,
					0.0
			);
		}
	}

	protected float getDamageAgainst(Entity target, float baseDamage, DamageSource damageSource) {
		return baseDamage;
	}

	@Override
	protected void attackLivingEntity(LivingEntity target) {
		this.attack(target);
	}

	/**
	 * Добавляет crit particles.
	 *
	 * @param target target
	 */
	public void addCritParticles(Entity target) {
	}

	private float getAttackCooldownDamageModifier() {
		float f = this.getAttackCooldownProgress(0.5F);
		return 0.2F + f * f * 0.8F;
	}

	@Override
	public boolean pierce(
			EquipmentSlot slot,
			Entity target,
			float damage,
			boolean dealDamage,
			boolean knockback,
			boolean dismount
	) {
		if (this.cannotAttack(target)) {
			return false;
		}
		else {
			ItemStack itemStack = this.getEquippedStack(slot);
			DamageSource damageSource = this.getDamageSource(itemStack);
			float f = this.getDamageAgainst(target, damage, damageSource) - damage;
			if (!this.isUsingItem() || this.getActiveHand().getEquipmentSlot() != slot) {
				f *= this.getAttackCooldownProgress(0.5F);
				damage *= this.getAttackCooldownDamageModifier();
			}

			if (knockback && this.tryDeflect(target)) {
				return true;
			}
			else {
				float g = dealDamage ? damage + f : 0.0F;
				float h = 0.0F;
				if (target instanceof LivingEntity livingEntity) {
					h = livingEntity.getHealth();
				}

				Vec3d vec3d = target.getVelocity();
				boolean bl = dealDamage && target.sidedDamage(damageSource, g);
				if (knockback) {
					this.knockbackTarget(target, 0.4F + this.getAttackKnockbackAgainst(target, damageSource), vec3d);
				}

				boolean bl2 = false;
				if (dismount && target.hasVehicle()) {
					bl2 = true;
					target.stopRiding();
				}

				if (!bl && !knockback && !bl2) {
					return false;
				}
				else {
					this.addAttackParticlesAndSounds(target, false, false, dealDamage, true, f);
					this.onAttacking(target);
					this.onTargetDamaged(target, itemStack, damageSource, bl);
					this.handleAttackDamage(target, h);
					this.addExhaustion(0.1F);
					return true;
				}
			}
		}
	}

	/**
	 * Добавляет enchanted hit particles.
	 *
	 * @param target target
	 */
	public void addEnchantedHitParticles(Entity target) {
	}

	@Override
	public void remove(Entity.RemovalReason reason) {
		super.remove(reason);
		this.playerScreenHandler.onClosed(this);
		if (this.shouldCloseHandledScreenOnRespawn()) {
			this.onHandledScreenClosed();
		}
	}

	@Override
	public boolean isControlledByPlayer() {
		return true;
	}

	@Override
	protected boolean isControlledByMainPlayer() {
		return this.isMainPlayer();
	}

	public boolean isMainPlayer() {
		return false;
	}

	@Override
	public boolean canMoveVoluntarily() {
		return !this.getEntityWorld().isClient() || this.isMainPlayer();
	}

	@Override
	public boolean canActVoluntarily() {
		return !this.getEntityWorld().isClient() || this.isMainPlayer();
	}

	public GameProfile getGameProfile() {
		return this.gameProfile;
	}

	public PlayerConfigEntry getPlayerConfigEntry() {
		return new PlayerConfigEntry(this.gameProfile);
	}

	public PlayerInventory getInventory() {
		return this.inventory;
	}

	public PlayerAbilities getAbilities() {
		return this.abilities;
	}

	@Override
	public boolean isInCreativeMode() {
		return this.abilities.creativeMode;
	}

	/**
	 * Определяет, следует ли skip block drops.
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldSkipBlockDrops() {
		return this.abilities.creativeMode;
	}

	/**
	 * Обрабатывает событие pickup slot click.
	 *
	 * @param cursorStack cursor stack
	 * @param slotStack slot stack
	 * @param clickType click type
	 */
	public void onPickupSlotClick(ItemStack cursorStack, ItemStack slotStack, ClickType clickType) {
	}

	/**
	 * Определяет, следует ли close handled screen on respawn.
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldCloseHandledScreenOnRespawn() {
		return this.currentScreenHandler != this.playerScreenHandler;
	}

	/**
	 * Проверяет возможность drop items.
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canDropItems() {
		return true;
	}

	public Either<PlayerEntity.SleepFailureReason, Unit> trySleep(BlockPos pos) {
		this.sleep(pos);
		this.sleepTimer = 0;
		return Either.right(Unit.INSTANCE);
	}

	/**
	 * Wake up.
	 *
	 * @param skipSleepTimer skip sleep timer
	 * @param updateSleepingPlayers update sleeping players
	 */
	public void wakeUp(boolean skipSleepTimer, boolean updateSleepingPlayers) {
		super.wakeUp();
		if (this.getEntityWorld() instanceof ServerWorld && updateSleepingPlayers) {
			((ServerWorld) this.getEntityWorld()).updateSleepingPlayers();
		}

		this.sleepTimer = skipSleepTimer ? 0 : 100;
	}

	@Override
	public void wakeUp() {
		this.wakeUp(true, true);
	}

	/**
	 * Проверяет возможность reset time by sleeping.
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canResetTimeBySleeping() {
		return this.isSleeping() && this.sleepTimer >= 100;
	}

	public int getSleepTimer() {
		return this.sleepTimer;
	}

	/**
	 * Отправляет message.
	 *
	 * @param message message
	 * @param overlay overlay
	 */
	public void sendMessage(Text message, boolean overlay) {
	}

	/**
	 * Increment stat.
	 *
	 * @param stat stat
	 */
	public void incrementStat(Identifier stat) {
		this.incrementStat(Stats.CUSTOM.getOrCreateStat(stat));
	}

	/**
	 * Increase stat.
	 *
	 * @param stat stat
	 * @param amount amount
	 */
	public void increaseStat(Identifier stat, int amount) {
		this.increaseStat(Stats.CUSTOM.getOrCreateStat(stat), amount);
	}

	/**
	 * Increment stat.
	 *
	 * @param stat stat
	 */
	public void incrementStat(Stat<?> stat) {
		this.increaseStat(stat, 1);
	}

	/**
	 * Increase stat.
	 *
	 * @param stat stat
	 * @param amount amount
	 */
	public void increaseStat(Stat<?> stat, int amount) {
	}

	/**
	 * Сбрасывает stat.
	 *
	 * @param stat stat
	 */
	public void resetStat(Stat<?> stat) {
	}

	/**
	 * Unlock recipes.
	 *
	 * @param recipes recipes
	 *
	 * @return int — результат операции
	 */
	public int unlockRecipes(Collection<RecipeEntry<?>> recipes) {
		return 0;
	}

	/**
	 * Обрабатывает событие recipe crafted.
	 *
	 * @param recipe recipe
	 * @param ingredients ingredients
	 */
	public void onRecipeCrafted(RecipeEntry<?> recipe, List<ItemStack> ingredients) {
	}

	/**
	 * Unlock recipes.
	 *
	 * @param recipes recipes
	 */
	public void unlockRecipes(List<RegistryKey<Recipe<?>>> recipes) {
	}

	/**
	 * Lock recipes.
	 *
	 * @param recipes recipes
	 *
	 * @return int — результат операции
	 */
	public int lockRecipes(Collection<RecipeEntry<?>> recipes) {
		return 0;
	}

	@Override
	public void travel(Vec3d movementInput) {
		if (this.hasVehicle()) {
			super.travel(movementInput);
		}
		else {
			if (this.isSwimming()) {
				double d = this.getRotationVector().y;
				double e = d < -0.2 ? 0.085 : 0.06;
				if (d <= 0.0
						|| this.jumping
						|| !this
						.getEntityWorld()
						.getFluidState(BlockPos.ofFloored(this.getX(), this.getY() + 1.0 - 0.1, this.getZ()))
						.isEmpty()) {
					Vec3d vec3d = this.getVelocity();
					this.setVelocity(vec3d.add(0.0, (d - vec3d.y) * e, 0.0));
				}
			}

			if (this.getAbilities().flying) {
				double d = this.getVelocity().y;
				super.travel(movementInput);
				this.setVelocity(this.getVelocity().withAxis(Direction.Axis.Y, d * 0.6));
			}
			else {
				super.travel(movementInput);
			}
		}
	}

	@Override
	protected boolean canGlide() {
		return !this.abilities.flying && super.canGlide();
	}

	@Override
	public void updateSwimming() {
		if (this.abilities.flying) {
			this.setSwimming(false);
		}
		else {
			super.updateSwimming();
		}
	}

	/**
	 * Does not suffocate.
	 *
	 * @param pos pos
	 *
	 * @return boolean — результат операции
	 */
	protected boolean doesNotSuffocate(BlockPos pos) {
		return !this.getEntityWorld().getBlockState(pos).shouldSuffocate(this.getEntityWorld(), pos);
	}

	@Override
	public float getMovementSpeed() {
		return (float) this.getAttributeValue(EntityAttributes.MOVEMENT_SPEED);
	}

	@Override
	public boolean handleFallDamage(double fallDistance, float damagePerDistance, DamageSource damageSource) {
		if (this.abilities.allowFlying) {
			return false;
		}
		else {
			if (fallDistance >= 2.0) {
				this.increaseStat(Stats.FALL_ONE_CM, (int) Math.round(fallDistance * 100.0));
			}

			boolean bl = this.currentExplosionImpactPos != null && this.ignoreFallDamageFromCurrentExplosion;
			double d;
			if (bl) {
				d = Math.min(fallDistance, this.currentExplosionImpactPos.y - this.getY());
				boolean bl2 = d <= 0.0;
				if (bl2) {
					this.clearCurrentExplosion();
				}
				else {
					this.tryClearCurrentExplosion();
				}
			}
			else {
				d = fallDistance;
			}

			if (d > 0.0 && super.handleFallDamage(d, damagePerDistance, damageSource)) {
				this.clearCurrentExplosion();
				return true;
			}
			else {
				this.handleFallDamageForPassengers(fallDistance, damagePerDistance, damageSource);
				return false;
			}
		}
	}

	/**
	 * Проверяет gliding.
	 *
	 * @return boolean — результат операции
	 */
	public boolean checkGliding() {
		if (!this.isGliding() && this.canGlide() && !this.isTouchingWater()) {
			this.startGliding();
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Запускает gliding.
	 */
	public void startGliding() {
		this.setFlag(7, true);
	}

	@Override
	protected void onSwimmingStart() {
		if (!this.isSpectator()) {
			super.onSwimmingStart();
		}
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		if (this.isTouchingWater()) {
			this.playSwimSound();
			this.playSecondaryStepSound(state);
		}
		else {
			BlockPos blockPos = this.getStepSoundPos(pos);
			if (!pos.equals(blockPos)) {
				BlockState blockState = this.getEntityWorld().getBlockState(blockPos);
				if (blockState.isIn(BlockTags.COMBINATION_STEP_SOUND_BLOCKS)) {
					this.playCombinationStepSounds(blockState, state);
				}
				else {
					super.playStepSound(blockPos, blockState);
				}
			}
			else {
				super.playStepSound(pos, state);
			}
		}
	}

	@Override
	public LivingEntity.FallSounds getFallSounds() {
		return new LivingEntity.FallSounds(SoundEvents.ENTITY_PLAYER_SMALL_FALL, SoundEvents.ENTITY_PLAYER_BIG_FALL);
	}

	@Override
	public boolean onKilledOther(ServerWorld world, LivingEntity other, DamageSource damageSource) {
		this.incrementStat(Stats.KILLED.getOrCreateStat(other.getType()));
		return true;
	}

	@Override
	public void slowMovement(BlockState state, Vec3d multiplier) {
		if (!this.abilities.flying) {
			super.slowMovement(state, multiplier);
		}

		this.tryClearCurrentExplosion();
	}

	/**
	 * Добавляет experience.
	 *
	 * @param experience experience
	 */
	public void addExperience(int experience) {
		this.addScore(experience);
		this.experienceProgress = this.experienceProgress + (float) experience / this.getNextLevelExperience();
		this.totalExperience = MathHelper.clamp(this.totalExperience + experience, 0, Integer.MAX_VALUE);

		while (this.experienceProgress < 0.0F) {
			float f = this.experienceProgress * this.getNextLevelExperience();
			if (this.experienceLevel > 0) {
				this.addExperienceLevels(-1);
				this.experienceProgress = 1.0F + f / this.getNextLevelExperience();
			}
			else {
				this.addExperienceLevels(-1);
				this.experienceProgress = 0.0F;
			}
		}

		while (this.experienceProgress >= 1.0F) {
			this.experienceProgress = (this.experienceProgress - 1.0F) * this.getNextLevelExperience();
			this.addExperienceLevels(1);
			this.experienceProgress = this.experienceProgress / this.getNextLevelExperience();
		}
	}

	public int getEnchantingTableSeed() {
		return this.enchantingTableSeed;
	}

	/**
	 * Применяет enchantment costs.
	 *
	 * @param enchantedItem enchanted item
	 * @param experienceLevels experience levels
	 */
	public void applyEnchantmentCosts(ItemStack enchantedItem, int experienceLevels) {
		this.experienceLevel -= experienceLevels;
		if (this.experienceLevel < 0) {
			this.experienceLevel = 0;
			this.experienceProgress = 0.0F;
			this.totalExperience = 0;
		}

		this.enchantingTableSeed = this.random.nextInt();
	}

	/**
	 * Добавляет experience levels.
	 *
	 * @param levels levels
	 */
	public void addExperienceLevels(int levels) {
		this.experienceLevel = IntMath.saturatedAdd(this.experienceLevel, levels);
		if (this.experienceLevel < 0) {
			this.experienceLevel = 0;
			this.experienceProgress = 0.0F;
			this.totalExperience = 0;
		}

		if (levels > 0 && this.experienceLevel % 5 == 0 && this.lastPlayedLevelUpSoundTime < this.age - 100.0F) {
			float f = this.experienceLevel > 30 ? 1.0F : this.experienceLevel / 30.0F;
			this.getEntityWorld()
			    .playSound(
					    null,
					    this.getX(),
					    this.getY(),
					    this.getZ(),
					    SoundEvents.ENTITY_PLAYER_LEVELUP,
					    this.getSoundCategory(),
					    f * 0.75F,
					    1.0F
			    );
			this.lastPlayedLevelUpSoundTime = this.age;
		}
	}

	public int getNextLevelExperience() {
		if (this.experienceLevel >= 30) {
			return 112 + (this.experienceLevel - 30) * 9;
		}
		else {
			return this.experienceLevel >= 15 ? 37 + (this.experienceLevel - 15) * 5 : 7 + this.experienceLevel * 2;
		}
	}

	/**
	 * Добавляет exhaustion.
	 *
	 * @param exhaustion exhaustion
	 */
	public void addExhaustion(float exhaustion) {
		if (!this.abilities.invulnerable) {
			if (!this.getEntityWorld().isClient()) {
				this.hungerManager.addExhaustion(exhaustion);
			}
		}
	}

	@Override
	public void useAttackEnchantmentEffects() {
		if (this.canSprintOrFly()) {
			super.useAttackEnchantmentEffects();
		}
	}

	/**
	 * Проверяет возможность sprint or fly.
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	protected boolean canSprintOrFly() {
		return this.getHungerManager().canSprint() || this.getAbilities().allowFlying;
	}

	public Optional<SculkShriekerWarningManager> getSculkShriekerWarningManager() {
		return Optional.empty();
	}

	public HungerManager getHungerManager() {
		return this.hungerManager;
	}

	/**
	 * Проверяет возможность consume.
	 *
	 * @param ignoreHunger ignore hunger
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canConsume(boolean ignoreHunger) {
		return this.abilities.invulnerable || ignoreHunger || this.hungerManager.isNotFull();
	}

	/**
	 * Проверяет возможность food heal.
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canFoodHeal() {
		return this.getHealth() > 0.0F && this.getHealth() < this.getMaxHealth();
	}

	/**
	 * Проверяет возможность modify blocks.
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canModifyBlocks() {
		return this.abilities.allowModifyWorld;
	}

	/**
	 * Проверяет возможность place on.
	 *
	 * @param pos pos
	 * @param facing facing
	 * @param stack stack
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canPlaceOn(BlockPos pos, Direction facing, ItemStack stack) {
		if (this.abilities.allowModifyWorld) {
			return true;
		}
		else {
			BlockPos blockPos = pos.offset(facing.getOpposite());
			CachedBlockPosition cachedBlockPosition = new CachedBlockPosition(this.getEntityWorld(), blockPos, false);
			return stack.canPlaceOn(cachedBlockPosition);
		}
	}

	@Override
	protected int getExperienceToDrop(ServerWorld world) {
		return !world.getGameRules().getValue(GameRules.KEEP_INVENTORY) && !this.isSpectator() ? Math.min(
				this.experienceLevel * 7,
				100
		) : 0;
	}

	@Override
	protected boolean shouldAlwaysDropExperience() {
		return true;
	}

	@Override
	public boolean shouldRenderName() {
		return true;
	}

	@Override
	protected Entity.MoveEffect getMoveEffect() {
		return this.abilities.flying || this.isOnGround() && this.isSneaky() ? Entity.MoveEffect.NONE
		                                                                     : Entity.MoveEffect.ALL;
	}

	/**
	 * Отправляет abilities update.
	 */
	public void sendAbilitiesUpdate() {
	}

	@Override
	public Text getName() {
		return Text.literal(this.gameProfile.name());
	}

	@Override
	public String getStringifiedName() {
		return this.gameProfile.name();
	}

	public EnderChestInventory getEnderChestInventory() {
		return this.enderChestInventory;
	}

	@Override
	protected boolean isArmorSlot(EquipmentSlot slot) {
		return slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR;
	}

	/**
	 * Give item stack.
	 *
	 * @param stack stack
	 *
	 * @return boolean — результат операции
	 */
	public boolean giveItemStack(ItemStack stack) {
		return this.inventory.insertStack(stack);
	}

	public abstract @Nullable GameMode getGameMode();

	@Override
	public boolean isSpectator() {
		return this.getGameMode() == GameMode.SPECTATOR;
	}

	@Override
	public boolean canBeHitByProjectile() {
		return !this.isSpectator() && super.canBeHitByProjectile();
	}

	@Override
	public boolean isSwimming() {
		return !this.abilities.flying && !this.isSpectator() && super.isSwimming();
	}

	public boolean isCreative() {
		return this.getGameMode() == GameMode.CREATIVE;
	}

	@Override
	public boolean isPushedByFluids() {
		return !this.abilities.flying;
	}

	@Override
	public Text getDisplayName() {
		MutableText mutableText = Team.decorateName(this.getScoreboardTeam(), this.getName());
		return this.addTellClickEvent(mutableText);
	}

	private MutableText addTellClickEvent(MutableText component) {
		String string = this.getGameProfile().name();
		return component.styled(
				style -> style
						.withClickEvent(new ClickEvent.SuggestCommand("/tell " + string + " "))
						.withHoverEvent(this.getHoverEvent())
						.withInsertion(string)
		);
	}

	@Override
	public String getNameForScoreboard() {
		return this.getGameProfile().name();
	}

	@Override
	protected void setAbsorptionAmountUnclamped(float absorptionAmount) {
		this.getDataTracker().set(ABSORPTION_AMOUNT, absorptionAmount);
	}

	@Override
	public float getAbsorptionAmount() {
		return this.getDataTracker().get(ABSORPTION_AMOUNT);
	}

	@Override
	public @Nullable StackReference getStackReference(int slot) {
		if (slot == 499) {
			return new StackReference() {
				@Override
				public ItemStack get() {
					return PlayerEntity.this.currentScreenHandler.getCursorStack();
				}

				@Override
				public boolean set(ItemStack stack) {
					PlayerEntity.this.currentScreenHandler.setCursorStack(stack);
					return true;
				}
			};
		}
		else {
			final int i = slot - 500;
			if (i >= 0 && i < 4) {
				return new StackReference() {
					@Override
					public ItemStack get() {
						return PlayerEntity.this.playerScreenHandler.getCraftingInput().getStack(i);
					}

					@Override
					public boolean set(ItemStack stack) {
						PlayerEntity.this.playerScreenHandler.getCraftingInput().setStack(i, stack);
						PlayerEntity.this.playerScreenHandler.onContentChanged(PlayerEntity.this.inventory);
						return true;
					}
				};
			}
			else if (slot >= 0 && slot < this.inventory.getMainStacks().size()) {
				return this.inventory.getStackReference(slot);
			}
			else {
				int j = slot - 200;
				return j >= 0 && j < this.enderChestInventory.size() ? this.enderChestInventory.getStackReference(j)
				                                                     : super.getStackReference(slot);
			}
		}
	}

	public boolean hasReducedDebugInfo() {
		return this.reducedDebugInfo;
	}

	public void setReducedDebugInfo(boolean reducedDebugInfo) {
		this.reducedDebugInfo = reducedDebugInfo;
	}

	@Override
	public void setFireTicks(int fireTicks) {
		super.setFireTicks(this.abilities.invulnerable ? Math.min(fireTicks, 1) : fireTicks);
	}

	protected static Optional<ParrotEntity.Variant> readParrotVariant(NbtCompound nbt) {
		if (!nbt.isEmpty()) {
			EntityType<?> entityType = nbt.<EntityType<?>>get("id", EntityType.CODEC).orElse(null);
			if (entityType == EntityType.PARROT) {
				return nbt.get("Variant", ParrotEntity.Variant.INDEX_CODEC);
			}
		}

		return Optional.empty();
	}

	/**
	 * Map parrot variant.
	 *
	 * @param variant variant
	 *
	 * @return OptionalInt — результат операции
	 */
	protected static OptionalInt mapParrotVariant(Optional<ParrotEntity.Variant> variant) {
		return variant.<OptionalInt>map(variantx -> OptionalInt.of(variantx.getIndex())).orElse(OptionalInt.empty());
	}

	private static Optional<ParrotEntity.Variant> mapParrotVariantIfPresent(OptionalInt variantIndex) {
		return variantIndex.isPresent() ? Optional.of(ParrotEntity.Variant.byIndex(variantIndex.getAsInt()))
		                                : Optional.empty();
	}

	public void setLeftShoulderParrotVariant(Optional<ParrotEntity.Variant> variant) {
		this.dataTracker.set(LEFT_SHOULDER_PARROT_VARIANT_ID, mapParrotVariant(variant));
	}

	public Optional<ParrotEntity.Variant> getLeftShoulderParrotVariant() {
		return mapParrotVariantIfPresent(this.dataTracker.get(LEFT_SHOULDER_PARROT_VARIANT_ID));
	}

	public void setRightShoulderParrotVariant(Optional<ParrotEntity.Variant> variant) {
		this.dataTracker.set(RIGHT_SHOULDER_PARROT_VARIANT_ID, mapParrotVariant(variant));
	}

	public Optional<ParrotEntity.Variant> getRightShoulderParrotVariant() {
		return mapParrotVariantIfPresent(this.dataTracker.get(RIGHT_SHOULDER_PARROT_VARIANT_ID));
	}

	public float getAttackCooldownProgressPerTick() {
		return (float) (1.0 / this.getAttributeValue(EntityAttributes.ATTACK_SPEED) * 20.0);
	}

	public boolean isBelowMinimumAttackCharge(ItemStack stack, int baseTime) {
		float f = stack.getOrDefault(DataComponentTypes.MINIMUM_ATTACK_CHARGE, 0.0F);
		float g = (this.ticksSinceLastAttack + baseTime) / this.getAttackCooldownProgressPerTick();
		return f > 0.0F && g < f;
	}

	public float getAttackCooldownProgress(float baseTime) {
		return MathHelper.clamp(
				(this.ticksSinceLastAttack + baseTime) / this.getAttackCooldownProgressPerTick(),
				0.0F,
				1.0F
		);
	}

	public float getHandEquippingProgress(float baseTime) {
		return MathHelper.clamp(
				(this.ticksSinceHandEquipping + baseTime) / this.getAttackCooldownProgressPerTick(),
				0.0F,
				1.0F
		);
	}

	/**
	 * Сбрасывает ticks since.
	 */
	public void resetTicksSince() {
		this.ticksSinceLastAttack = 0;
		this.ticksSinceHandEquipping = 0;
	}

	@Override
	public void beforePlayerAttack() {
		this.resetTicksSinceLastAttack();
		super.beforePlayerAttack();
	}

	/**
	 * Сбрасывает ticks since last attack.
	 */
	public void resetTicksSinceLastAttack() {
		this.ticksSinceLastAttack = 0;
	}

	public ItemCooldownManager getItemCooldownManager() {
		return this.itemCooldownManager;
	}

	@Override
	protected float getVelocityMultiplier() {
		return !this.abilities.flying && !this.isGliding() ? super.getVelocityMultiplier() : 1.0F;
	}

	@Override
	public float getLuck() {
		return (float) this.getAttributeValue(EntityAttributes.LUCK);
	}

	public boolean isCreativeLevelTwoOp() {
		return this.abilities.creativeMode && this.getPermissions().hasPermission(DefaultPermissions.GAMEMASTERS);
	}

	public PermissionPredicate getPermissions() {
		return PermissionPredicate.NONE;
	}

	@Override
	public ImmutableList<EntityPose> getPoses() {
		return ImmutableList.of(EntityPose.STANDING, EntityPose.CROUCHING, EntityPose.SWIMMING);
	}

	@Override
	public ItemStack getProjectileType(ItemStack stack) {
		if (!(stack.getItem() instanceof RangedWeaponItem)) {
			return ItemStack.EMPTY;
		}
		else {
			Predicate<ItemStack> predicate = ((RangedWeaponItem) stack.getItem()).getHeldProjectiles();
			ItemStack itemStack = RangedWeaponItem.getHeldProjectile(this, predicate);
			if (!itemStack.isEmpty()) {
				return itemStack;
			}
			else {
				predicate = ((RangedWeaponItem) stack.getItem()).getProjectiles();

				for (int i = 0; i < this.inventory.size(); i++) {
					ItemStack itemStack2 = this.inventory.getStack(i);
					if (predicate.test(itemStack2)) {
						return itemStack2;
					}
				}

				return this.isInCreativeMode() ? new ItemStack(Items.ARROW) : ItemStack.EMPTY;
			}
		}
	}

	@Override
	public Vec3d getLeashPos(float tickProgress) {
		double d = 0.22 * (this.getMainArm() == Arm.RIGHT ? -1.0 : 1.0);
		float f = MathHelper.lerp(tickProgress * 0.5F, this.getPitch(), this.lastPitch) * (float) (Math.PI / 180.0);
		float g = MathHelper.lerp(tickProgress, this.lastBodyYaw, this.bodyYaw) * (float) (Math.PI / 180.0);
		if (this.isGliding() || this.isUsingRiptide()) {
			Vec3d vec3d = this.getRotationVec(tickProgress);
			Vec3d vec3d2 = this.getVelocity();
			double e = vec3d2.horizontalLengthSquared();
			double h = vec3d.horizontalLengthSquared();
			float k;
			if (e > 0.0 && h > 0.0) {
				double i = (vec3d2.x * vec3d.x + vec3d2.z * vec3d.z) / Math.sqrt(e * h);
				double j = vec3d2.x * vec3d.z - vec3d2.z * vec3d.x;
				k = (float) (Math.signum(j) * Math.acos(i));
			}
			else {
				k = 0.0F;
			}

			return this.getLerpedPos(tickProgress).add(new Vec3d(d, -0.11, 0.85).rotateZ(-k).rotateX(-f).rotateY(-g));
		}
		else if (this.isInSwimmingPose()) {
			return this.getLerpedPos(tickProgress).add(new Vec3d(d, 0.2, -0.15).rotateX(-f).rotateY(-g));
		}
		else {
			double l = this.getBoundingBox().getLengthY() - 1.0;
			double e = this.isInSneakingPose() ? -0.2 : 0.07;
			return this.getLerpedPos(tickProgress).add(new Vec3d(d, l, e).rotateY(-g));
		}
	}

	@Override
	public boolean isPlayer() {
		return true;
	}

	public boolean isUsingSpyglass() {
		return this.isUsingItem() && this.getActiveItem().isOf(Items.SPYGLASS);
	}

	@Override
	public boolean shouldSave() {
		return false;
	}

	public Optional<GlobalPos> getLastDeathPos() {
		return this.lastDeathPos;
	}

	public void setLastDeathPos(Optional<GlobalPos> lastDeathPos) {
		this.lastDeathPos = lastDeathPos;
	}

	@Override
	public float getDamageTiltYaw() {
		return this.damageTiltYaw;
	}

	@Override
	public void animateDamage(float yaw) {
		super.animateDamage(yaw);
		this.damageTiltYaw = yaw;
	}

	public boolean hasBlindnessEffect() {
		return this.hasStatusEffect(StatusEffects.BLINDNESS);
	}

	@Override
	public boolean canSprintAsVehicle() {
		return true;
	}

	@Override
	protected float getOffGroundSpeed() {
		if (this.abilities.flying && !this.hasVehicle()) {
			return this.isSprinting() ? this.abilities.getFlySpeed() * 2.0F : this.abilities.getFlySpeed();
		}
		else {
			return this.isSprinting() ? 0.025999999F : 0.02F;
		}
	}

	@Override
	public boolean isViewingContainerAt(ViewerCountManager viewerCountManager, BlockPos pos) {
		return viewerCountManager.isPlayerViewing(this);
	}

	@Override
	public double getContainerInteractionRange() {
		return this.getBlockInteractionRange();
	}

	public double getBlockInteractionRange() {
		return this.getAttributeValue(EntityAttributes.BLOCK_INTERACTION_RANGE);
	}

	public double getEntityInteractionRange() {
		return this.getAttributeValue(EntityAttributes.ENTITY_INTERACTION_RANGE);
	}

	/**
	 * Проверяет возможность interact with entity.
	 *
	 * @param entity entity
	 * @param additionalRange additional range
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canInteractWithEntity(Entity entity, double additionalRange) {
		return entity.isRemoved() ? false : this.canInteractWithEntityIn(entity.getBoundingBox(), additionalRange);
	}

	/**
	 * Проверяет возможность interact with entity in.
	 *
	 * @param box box
	 * @param additionalRange additional range
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canInteractWithEntityIn(Box box, double additionalRange) {
		double d = this.getEntityInteractionRange() + additionalRange;
		double e = box.squaredMagnitude(this.getEyePos());
		return e < d * d;
	}

	/**
	 * Проверяет возможность attack entity in.
	 *
	 * @param box box
	 * @param additionalRange additional range
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canAttackEntityIn(Box box, double additionalRange) {
		return this.getAttackRange().isWithinRange(this, box, additionalRange);
	}

	/**
	 * Проверяет возможность interact with block at.
	 *
	 * @param pos pos
	 * @param additionalRange additional range
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canInteractWithBlockAt(BlockPos pos, double additionalRange) {
		double d = this.getBlockInteractionRange() + additionalRange;
		return new Box(pos).squaredMagnitude(this.getEyePos()) < d * d;
	}

	public void setIgnoreFallDamageFromCurrentExplosion(boolean ignoreFallDamageFromCurrentExplosion) {
		this.ignoreFallDamageFromCurrentExplosion = ignoreFallDamageFromCurrentExplosion;
		if (ignoreFallDamageFromCurrentExplosion) {
			this.setCurrentExplosionResetGraceTime(40);
		}
		else {
			this.currentExplosionResetGraceTime = 0;
		}
	}

	public void setCurrentExplosionResetGraceTime(int currentExplosionResetGraceTime) {
		this.currentExplosionResetGraceTime =
				Math.max(this.currentExplosionResetGraceTime, currentExplosionResetGraceTime);
	}

	/**
	 * Определяет, следует ли ignore fall damage from current explosion.
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldIgnoreFallDamageFromCurrentExplosion() {
		return this.ignoreFallDamageFromCurrentExplosion;
	}

	/**
	 * Try clear current explosion.
	 */
	public void tryClearCurrentExplosion() {
		if (this.currentExplosionResetGraceTime == 0) {
			this.clearCurrentExplosion();
		}
	}

	public boolean isInCurrentExplosionResetGraceTime() {
		return this.currentExplosionResetGraceTime > 0;
	}

	/**
	 * Очищает current explosion.
	 */
	public void clearCurrentExplosion() {
		this.currentExplosionResetGraceTime = 0;
		this.explodedBy = null;
		this.currentExplosionImpactPos = null;
		this.ignoreFallDamageFromCurrentExplosion = false;
	}

	/**
	 * Определяет, следует ли rotate with minecart.
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldRotateWithMinecart() {
		return false;
	}

	@Override
	public boolean isClimbing() {
		return this.abilities.flying ? false : super.isClimbing();
	}

	/**
	 * As string.
	 *
	 * @return String — результат операции
	 */
	public String asString() {
		return MoreObjects.toStringHelper(this)
		                  .add("name", this.getStringifiedName())
		                  .add("id", this.getId())
		                  .add("pos", this.getEntityPos())
		                  .add("mode", this.getGameMode())
		                  .add("permission", this.getPermissions())
		                  .toString();
	}

	/**
	 * {@code SleepFailureReason}.
	 */
	public record SleepFailureReason(@Nullable Text message) {

		public static final PlayerEntity.SleepFailureReason TOO_FAR_AWAY = new PlayerEntity.SleepFailureReason(
				Text.translatable("block.minecraft.bed.too_far_away")
		);
		public static final PlayerEntity.SleepFailureReason
				OBSTRUCTED =
				new PlayerEntity.SleepFailureReason(Text.translatable("block.minecraft.bed.obstructed"));
		public static final PlayerEntity.SleepFailureReason OTHER = new PlayerEntity.SleepFailureReason(null);
		public static final PlayerEntity.SleepFailureReason
				NOT_SAFE =
				new PlayerEntity.SleepFailureReason(Text.translatable("block.minecraft.bed.not_safe"));
	}
}
