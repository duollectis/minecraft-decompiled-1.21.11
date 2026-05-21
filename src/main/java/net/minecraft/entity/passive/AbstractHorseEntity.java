package net.minecraft.entity.passive;

import com.google.common.collect.UnmodifiableIterator;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.MountScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jspecify.annotations.Nullable;

import java.util.function.DoubleSupplier;
import java.util.function.IntUnaryOperator;

/**
 * {@code AbstractHorseEntity}.
 */
public abstract class AbstractHorseEntity extends AnimalEntity implements RideableInventory, Tameable, JumpingMount {

	public static final int MAX_TEMPER = 499;
	public static final int TEMPER_RANGE = 500;
	public static final double TAMING_SPEED = 0.15;
	private static final float MIN_MOVEMENT_SPEED_BONUS = (float) getChildMovementSpeedBonus(() -> 0.0);
	private static final float MAX_MOVEMENT_SPEED_BONUS = (float) getChildMovementSpeedBonus(() -> 1.0);
	private static final float MIN_JUMP_STRENGTH_BONUS = (float) getChildJumpStrengthBonus(() -> 0.0);
	private static final float MAX_JUMP_STRENGTH_BONUS = (float) getChildJumpStrengthBonus(() -> 1.0);
	private static final float MIN_HEALTH_BONUS = getChildHealthBonus(max -> 0);
	private static final float MAX_HEALTH_BONUS = getChildHealthBonus(max -> max - 1);
	private static final float MIN_SPEED = 0.25F;
	private static final float MAX_SPEED = 0.5F;
	private static final TargetPredicate.EntityPredicate
			IS_BRED_HORSE =
			(entity, world) -> entity instanceof AbstractHorseEntity abstractHorseEntity
					&& abstractHorseEntity.isBred();
	private static final TargetPredicate PARENT_HORSE_PREDICATE = TargetPredicate.createNonAttackable()
	                                                                             .setBaseMaxDistance(16.0)
	                                                                             .ignoreVisibility()
	                                                                             .setPredicate(IS_BRED_HORSE);
	private static final TrackedData<Byte>
			HORSE_FLAGS =
			DataTracker.registerData(AbstractHorseEntity.class, TrackedDataHandlerRegistry.BYTE);
	private static final int TAMED_FLAG = 2;
	private static final int BRED_FLAG = 8;
	private static final int EATING_GRASS_FLAG = 16;
	private static final int ANGRY_FLAG = 32;
	private static final int EATING_FLAG = 64;
	public static final int INVENTORY_SLOT_COUNT = 3;
	private static final int DEFAULT_TEMPER = 0;
	private static final boolean DEFAULT_EATING_GRASS = false;
	private static final boolean DEFAULT_BRED = false;
	private static final boolean DEFAULT_TAME = false;
	private int eatingGrassTicks;
	private int eatingTicks;
	private int angryTicks;
	public int tailWagTicks;
	public int mountingTicks;
	protected SimpleInventory items;
	protected int temper = 0;
	protected float jumpStrength;
	protected boolean jumping;
	private float eatingGrassAnimationProgress;
	private float lastEatingGrassAnimationProgress;
	private float angryAnimationProgress;
	private float lastAngryAnimationProgress;
	private float eatingAnimationProgress;
	private float lastEatingAnimationProgress;
	protected boolean playExtraHorseSounds = true;
	protected int soundTicks;
	private @Nullable LazyEntityReference<LivingEntity> ownerReference;

	protected AbstractHorseEntity(EntityType<? extends AbstractHorseEntity> entityType, World world) {
		super(entityType, world);
		this.onChestedStatusChanged();
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(1, new AbstractHorseEntity.HorseEscapeDangerGoal(1.2));
		this.goalSelector.add(1, new HorseBondWithPlayerGoal(this, 1.2));
		this.goalSelector.add(2, new AnimalMateGoal(this, 1.0, AbstractHorseEntity.class));
		this.goalSelector.add(4, new FollowParentGoal(this, 1.0));
		this.goalSelector.add(6, new WanderAroundFarGoal(this, 0.7));
		this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
		this.goalSelector.add(8, new LookAroundGoal(this));
		if (this.shouldAmbientStand()) {
			this.goalSelector.add(9, new AmbientStandGoal(this));
		}

		this.initCustomGoals();
	}

	/**
	 * Инициализирует custom goals.
	 */
	protected void initCustomGoals() {
		this.goalSelector.add(0, new SwimGoal(this));
		this.goalSelector.add(3, new TemptGoal(this, 1.25, stack -> stack.isIn(ItemTags.HORSE_TEMPT_ITEMS), false));
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(HORSE_FLAGS, (byte) 0);
	}

	protected boolean getHorseFlag(int bitmask) {
		return (this.dataTracker.get(HORSE_FLAGS) & bitmask) != 0;
	}

	protected void setHorseFlag(int bitmask, boolean flag) {
		byte b = this.dataTracker.get(HORSE_FLAGS);
		if (flag) {
			this.dataTracker.set(HORSE_FLAGS, (byte) (b | bitmask));
		}
		else {
			this.dataTracker.set(HORSE_FLAGS, (byte) (b & ~bitmask));
		}
	}

	public boolean isTame() {
		return this.getHorseFlag(2);
	}

	@Override
	public @Nullable LazyEntityReference<LivingEntity> getOwnerReference() {
		return this.ownerReference;
	}

	public void setOwner(@Nullable LivingEntity entity) {
		this.ownerReference = LazyEntityReference.of(entity);
	}

	public void setTame(boolean tame) {
		this.setHorseFlag(2, tame);
	}

	@Override
	public void onLongLeashTick() {
		super.onLongLeashTick();
		if (this.isEatingGrass()) {
			this.setEatingGrass(false);
		}
	}

	@Override
	public boolean canUseQuadLeashAttachmentPoint() {
		return true;
	}

	@Override
	public Vec3d[] getQuadLeashOffsets() {
		return Leashable.createQuadLeashOffsets(this, 0.04, 0.52, 0.23, 0.87);
	}

	public boolean isEatingGrass() {
		return this.getHorseFlag(16);
	}

	public boolean isAngry() {
		return this.getHorseFlag(32);
	}

	public boolean isBred() {
		return this.getHorseFlag(8);
	}

	public void setBred(boolean bred) {
		this.setHorseFlag(8, bred);
	}

	@Override
	public boolean canUseSlot(EquipmentSlot slot) {
		return slot != EquipmentSlot.SADDLE ? super.canUseSlot(slot)
		                                    : this.isAlive() && !this.isBaby() && this.isTame();
	}

	/**
	 * Equip horse armor.
	 *
	 * @param player player
	 * @param stack stack
	 */
	public void equipHorseArmor(PlayerEntity player, ItemStack stack) {
		if (this.canEquip(stack, EquipmentSlot.BODY)) {
			this.equipBodyArmor(stack.splitUnlessCreative(1, player));
		}
	}

	@Override
	protected boolean canDispenserEquipSlot(EquipmentSlot slot) {
		return (slot == EquipmentSlot.BODY || slot == EquipmentSlot.SADDLE) && this.isTame()
				|| super.canDispenserEquipSlot(slot);
	}

	public int getTemper() {
		return this.temper;
	}

	public void setTemper(int temper) {
		this.temper = temper;
	}

	/**
	 * Добавляет temper.
	 *
	 * @param difference difference
	 *
	 * @return int — результат операции
	 */
	public int addTemper(int difference) {
		int i = MathHelper.clamp(this.getTemper() + difference, 0, this.getMaxTemper());
		this.setTemper(i);
		return i;
	}

	@Override
	public boolean isPushable() {
		return !this.hasPassengers();
	}

	private void playEatingAnimation() {
		this.setEating();
		if (!this.isSilent()) {
			SoundEvent soundEvent = this.getEatSound();
			if (soundEvent != null) {
				this.getEntityWorld()
				    .playSound(
						    null,
						    this.getX(),
						    this.getY(),
						    this.getZ(),
						    soundEvent,
						    this.getSoundCategory(),
						    1.0F,
						    1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F
				    );
			}
		}
	}

	@Override
	public boolean handleFallDamage(double fallDistance, float damagePerDistance, DamageSource damageSource) {
		if (fallDistance > 1.0) {
			this.playSound(SoundEvents.ENTITY_HORSE_LAND, 0.4F, 1.0F);
		}

		int i = this.computeFallDamage(fallDistance, damagePerDistance);
		if (i <= 0) {
			return false;
		}
		else {
			this.serverDamage(damageSource, i);
			this.handleFallDamageForPassengers(fallDistance, damagePerDistance, damageSource);
			this.playBlockFallSound();
			return true;
		}
	}

	public final int getInventorySize() {
		return MountScreenHandler.getSlotCount(this.getInventoryColumns());
	}

	/**
	 * Обрабатывает событие chested status changed.
	 */
	protected void onChestedStatusChanged() {
		SimpleInventory simpleInventory = this.items;
		this.items = new SimpleInventory(this.getInventorySize());
		if (simpleInventory != null) {
			int i = Math.min(simpleInventory.size(), this.items.size());

			for (int j = 0; j < i; j++) {
				ItemStack itemStack = simpleInventory.getStack(j);
				if (!itemStack.isEmpty()) {
					this.items.setStack(j, itemStack.copy());
				}
			}
		}
	}

	@Override
	protected RegistryEntry<SoundEvent> getEquipSound(
			EquipmentSlot slot,
			ItemStack stack,
			EquippableComponent equippableComponent
	) {
		return (RegistryEntry<SoundEvent>) (slot == EquipmentSlot.SADDLE ? SoundEvents.ENTITY_HORSE_SADDLE
		                                                                 : super.getEquipSound(
				                                                                 slot,
				                                                                 stack,
				                                                                 equippableComponent
		                                                                 )
		);
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		boolean bl = super.damage(world, source, amount);
		if (bl && this.random.nextInt(3) == 0) {
			this.updateAnger();
		}

		return bl;
	}

	/**
	 * Определяет, следует ли ambient stand.
	 *
	 * @return boolean — результат операции
	 */
	protected boolean shouldAmbientStand() {
		return true;
	}

	protected @Nullable SoundEvent getEatSound() {
		return null;
	}

	protected @Nullable SoundEvent getAngrySound() {
		return null;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		if (!state.isLiquid()) {
			BlockState blockState = this.getEntityWorld().getBlockState(pos.up());
			BlockSoundGroup blockSoundGroup = state.getSoundGroup();
			if (blockState.isOf(Blocks.SNOW)) {
				blockSoundGroup = blockState.getSoundGroup();
			}

			if (this.hasPassengers() && this.playExtraHorseSounds) {
				this.soundTicks++;
				if (this.soundTicks > 5 && this.soundTicks % 3 == 0) {
					this.playWalkSound(blockSoundGroup);
				}
				else if (this.soundTicks <= 5) {
					this.playSound(
							SoundEvents.ENTITY_HORSE_STEP_WOOD,
							blockSoundGroup.getVolume() * 0.15F,
							blockSoundGroup.getPitch()
					);
				}
			}
			else if (this.isWooden(blockSoundGroup)) {
				this.playSound(
						SoundEvents.ENTITY_HORSE_STEP_WOOD,
						blockSoundGroup.getVolume() * 0.15F,
						blockSoundGroup.getPitch()
				);
			}
			else {
				this.playSound(
						SoundEvents.ENTITY_HORSE_STEP,
						blockSoundGroup.getVolume() * 0.15F,
						blockSoundGroup.getPitch()
				);
			}
		}
	}

	private boolean isWooden(BlockSoundGroup soundGroup) {
		return soundGroup == BlockSoundGroup.WOOD
				|| soundGroup == BlockSoundGroup.NETHER_WOOD
				|| soundGroup == BlockSoundGroup.NETHER_STEM
				|| soundGroup == BlockSoundGroup.CHERRY_WOOD
				|| soundGroup == BlockSoundGroup.BAMBOO_WOOD;
	}

	/**
	 * Play walk sound.
	 *
	 * @param group group
	 */
	protected void playWalkSound(BlockSoundGroup group) {
		this.playSound(SoundEvents.ENTITY_HORSE_GALLOP, group.getVolume() * 0.15F, group.getPitch());
	}

	public static DefaultAttributeContainer.Builder createBaseHorseAttributes() {
		return AnimalEntity.createAnimalAttributes()
		                   .add(EntityAttributes.JUMP_STRENGTH, 0.7)
		                   .add(EntityAttributes.MAX_HEALTH, 53.0)
		                   .add(EntityAttributes.MOVEMENT_SPEED, 0.225F)
		                   .add(EntityAttributes.STEP_HEIGHT, 1.0)
		                   .add(EntityAttributes.SAFE_FALL_DISTANCE, 6.0)
		                   .add(EntityAttributes.FALL_DAMAGE_MULTIPLIER, 0.5);
	}

	@Override
	public int getLimitPerChunk() {
		return 6;
	}

	public int getMaxTemper() {
		return 100;
	}

	@Override
	protected float getSoundVolume() {
		return 0.8F;
	}

	@Override
	public int getMinAmbientSoundDelay() {
		return 400;
	}

	@Override
	public void openInventory(PlayerEntity player) {
		if (!this.getEntityWorld().isClient() && (!this.hasPassengers() || this.hasPassenger(player))
				&& this.isTame()) {
			player.openHorseInventory(this, this.items);
		}
	}

	/**
	 * Выполняет взаимодействие с horse.
	 *
	 * @param player player
	 * @param stack stack
	 *
	 * @return ActionResult — результат операции
	 */
	public ActionResult interactHorse(PlayerEntity player, ItemStack stack) {
		boolean bl = this.receiveFood(player, stack);
		if (bl) {
			stack.decrementUnlessCreative(1, player);
		}

		return (ActionResult) (!bl && !this.getEntityWorld().isClient() ? ActionResult.PASS
		                                                                : ActionResult.SUCCESS_SERVER
		);
	}

	/**
	 * Получает food.
	 *
	 * @param player player
	 * @param item item
	 *
	 * @return boolean — результат операции
	 */
	protected boolean receiveFood(PlayerEntity player, ItemStack item) {
		boolean bl = false;
		float f = 0.0F;
		int i = 0;
		int j = 0;
		if (item.isOf(Items.WHEAT)) {
			f = 2.0F;
			i = 20;
			j = 3;
		}
		else if (item.isOf(Items.SUGAR)) {
			f = 1.0F;
			i = 30;
			j = 3;
		}
		else if (item.isOf(Blocks.HAY_BLOCK.asItem())) {
			f = 20.0F;
			i = 180;
		}
		else if (item.isOf(Items.APPLE)) {
			f = 3.0F;
			i = 60;
			j = 3;
		}
		else if (item.isOf(Items.RED_MUSHROOM)) {
			f = 3.0F;
			i = 0;
			j = 3;
		}
		else if (item.isOf(Items.CARROT)) {
			f = 3.0F;
			i = 60;
			j = 3;
		}
		else if (item.isOf(Items.GOLDEN_CARROT)) {
			f = 4.0F;
			i = 60;
			j = 5;
			if (!this.getEntityWorld().isClient() && this.isTame() && this.getBreedingAge() == 0 && !this.isInLove()) {
				bl = true;
				this.lovePlayer(player);
			}
		}
		else if (item.isOf(Items.GOLDEN_APPLE) || item.isOf(Items.ENCHANTED_GOLDEN_APPLE)) {
			f = 10.0F;
			i = 240;
			j = 10;
			if (!this.getEntityWorld().isClient() && this.isTame() && this.getBreedingAge() == 0 && !this.isInLove()) {
				bl = true;
				this.lovePlayer(player);
			}
		}

		if (this.getHealth() < this.getMaxHealth() && f > 0.0F) {
			this.heal(f);
			bl = true;
		}

		if (this.isBaby() && i > 0) {
			this.getEntityWorld()
			    .addParticleClient(
					    ParticleTypes.HAPPY_VILLAGER,
					    this.getParticleX(1.0),
					    this.getRandomBodyY() + 0.5,
					    this.getParticleZ(1.0),
					    0.0,
					    0.0,
					    0.0
			    );
			if (!this.getEntityWorld().isClient()) {
				this.growUp(i);
				bl = true;
			}
		}

		if (j > 0 && (bl || !this.isTame()) && this.getTemper() < this.getMaxTemper() && !this
				.getEntityWorld()
				.isClient()) {
			this.addTemper(j);
			bl = true;
		}

		if (bl) {
			this.playEatingAnimation();
			this.emitGameEvent(GameEvent.EAT);
		}

		return bl;
	}

	/**
	 * Put player on back.
	 *
	 * @param player player
	 */
	protected void putPlayerOnBack(PlayerEntity player) {
		this.setEatingGrass(false);
		this.setNotAngry();
		if (!this.getEntityWorld().isClient()) {
			player.setYaw(this.getYaw());
			player.setPitch(this.getPitch());
			player.startRiding(this);
		}
	}

	@Override
	public boolean isImmobile() {
		return super.isImmobile() && this.hasPassengers() && this.hasSaddleEquipped() || this.isEatingGrass()
				|| this.isAngry();
	}

	@Override
	public boolean isBreedingItem(ItemStack stack) {
		return stack.isIn(ItemTags.HORSE_FOOD);
	}

	private void wagTail() {
		this.tailWagTicks = 1;
	}

	@Override
	protected void dropInventory(ServerWorld world) {
		super.dropInventory(world);
		if (this.items != null) {
			for (int i = 0; i < this.items.size(); i++) {
				ItemStack itemStack = this.items.getStack(i);
				if (!itemStack.isEmpty() && !EnchantmentHelper.hasAnyEnchantmentsWith(
						itemStack,
						EnchantmentEffectComponentTypes.PREVENT_EQUIPMENT_DROP
				)) {
					this.dropStack(world, itemStack);
				}
			}
		}
	}

	@Override
	public void tickMovement() {
		if (this.random.nextInt(200) == 0) {
			this.wagTail();
		}

		super.tickMovement();
		if (this.getEntityWorld() instanceof ServerWorld serverWorld && this.isAlive()) {
			if (this.random.nextInt(900) == 0 && this.deathTime == 0) {
				this.heal(1.0F);
			}

			if (this.eatsGrass()) {
				if (!this.isEatingGrass()
						&& !this.hasPassengers()
						&& this.random.nextInt(300) == 0
						&& serverWorld.getBlockState(this.getBlockPos().down()).isOf(Blocks.GRASS_BLOCK)) {
					this.setEatingGrass(true);
				}

				if (this.isEatingGrass() && ++this.eatingGrassTicks > 50) {
					this.eatingGrassTicks = 0;
					this.setEatingGrass(false);
				}
			}

			this.walkToParent(serverWorld);
		}
	}

	/**
	 * Walk to parent.
	 *
	 * @param world world
	 */
	protected void walkToParent(ServerWorld world) {
		if (this.isBred() && this.isBaby() && !this.isEatingGrass()) {
			LivingEntity livingEntity = world.getClosestEntity(
					AbstractHorseEntity.class,
					PARENT_HORSE_PREDICATE,
					this,
					this.getX(),
					this.getY(),
					this.getZ(),
					this.getBoundingBox().expand(16.0)
			);
			if (livingEntity != null && this.squaredDistanceTo(livingEntity) > 4.0) {
				this.navigation.findPathTo(livingEntity, 0);
			}
		}
	}

	/**
	 * Eats grass.
	 *
	 * @return boolean — результат операции
	 */
	public boolean eatsGrass() {
		return true;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.eatingTicks > 0 && ++this.eatingTicks > 30) {
			this.eatingTicks = 0;
			this.setHorseFlag(64, false);
		}

		if (this.angryTicks > 0 && --this.angryTicks <= 0) {
			this.setNotAngry();
		}

		if (this.tailWagTicks > 0 && ++this.tailWagTicks > 8) {
			this.tailWagTicks = 0;
		}

		if (this.mountingTicks > 0) {
			this.mountingTicks++;
			if (this.mountingTicks > 300) {
				this.mountingTicks = 0;
			}
		}

		this.lastEatingGrassAnimationProgress = this.eatingGrassAnimationProgress;
		if (this.isEatingGrass()) {
			this.eatingGrassAnimationProgress =
					this.eatingGrassAnimationProgress + ((1.0F - this.eatingGrassAnimationProgress) * 0.4F + 0.05F);
			if (this.eatingGrassAnimationProgress > 1.0F) {
				this.eatingGrassAnimationProgress = 1.0F;
			}
		}
		else {
			this.eatingGrassAnimationProgress =
					this.eatingGrassAnimationProgress + ((0.0F - this.eatingGrassAnimationProgress) * 0.4F - 0.05F);
			if (this.eatingGrassAnimationProgress < 0.0F) {
				this.eatingGrassAnimationProgress = 0.0F;
			}
		}

		this.lastAngryAnimationProgress = this.angryAnimationProgress;
		if (this.isAngry()) {
			this.eatingGrassAnimationProgress = 0.0F;
			this.lastEatingGrassAnimationProgress = this.eatingGrassAnimationProgress;
			this.angryAnimationProgress =
					this.angryAnimationProgress + ((1.0F - this.angryAnimationProgress) * 0.4F + 0.05F);
			if (this.angryAnimationProgress > 1.0F) {
				this.angryAnimationProgress = 1.0F;
			}
		}
		else {
			this.jumping = false;
			this.angryAnimationProgress = this.angryAnimationProgress
					+ ((0.8F * this.angryAnimationProgress * this.angryAnimationProgress * this.angryAnimationProgress
					- this.angryAnimationProgress
			) * 0.6F - 0.05F
			);
			if (this.angryAnimationProgress < 0.0F) {
				this.angryAnimationProgress = 0.0F;
			}
		}

		this.lastEatingAnimationProgress = this.eatingAnimationProgress;
		if (this.getHorseFlag(64)) {
			this.eatingAnimationProgress =
					this.eatingAnimationProgress + ((1.0F - this.eatingAnimationProgress) * 0.7F + 0.05F);
			if (this.eatingAnimationProgress > 1.0F) {
				this.eatingAnimationProgress = 1.0F;
			}
		}
		else {
			this.eatingAnimationProgress =
					this.eatingAnimationProgress + ((0.0F - this.eatingAnimationProgress) * 0.7F - 0.05F);
			if (this.eatingAnimationProgress < 0.0F) {
				this.eatingAnimationProgress = 0.0F;
			}
		}
	}

	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		if (this.hasPassengers() || this.isBaby()) {
			return super.interactMob(player, hand);
		}
		else if (this.isTame() && player.shouldCancelInteraction()) {
			this.openInventory(player);
			return ActionResult.SUCCESS;
		}
		else {
			ItemStack itemStack = player.getStackInHand(hand);
			if (!itemStack.isEmpty()) {
				ActionResult actionResult = itemStack.useOnEntity(player, this, hand);
				if (actionResult.isAccepted()) {
					return actionResult;
				}

				if (this.canEquip(itemStack, EquipmentSlot.BODY) && !this.isWearingBodyArmor()) {
					this.equipHorseArmor(player, itemStack);
					return ActionResult.SUCCESS;
				}
			}

			this.putPlayerOnBack(player);
			return ActionResult.SUCCESS;
		}
	}

	private void setEating() {
		if (!this.getEntityWorld().isClient()) {
			this.eatingTicks = 1;
			this.setHorseFlag(64, true);
		}
	}

	public void setEatingGrass(boolean eatingGrass) {
		this.setHorseFlag(16, eatingGrass);
	}

	public void setAngry(int ticks) {
		this.setEatingGrass(false);
		this.setHorseFlag(32, true);
		this.angryTicks = ticks;
	}

	public void setNotAngry() {
		this.setHorseFlag(32, false);
		this.angryTicks = 0;
	}

	public @Nullable SoundEvent getAmbientStandSound() {
		return this.getAmbientSound();
	}

	/**
	 * Обновляет anger.
	 */
	public void updateAnger() {
		if (this.shouldAmbientStand() && (this.canActVoluntarily() || !this.getEntityWorld().isClient())) {
			this.setAngry(20);
		}
	}

	/**
	 * Play angry sound.
	 */
	public void playAngrySound() {
		if (!this.isAngry() && !this.getEntityWorld().isClient()) {
			this.updateAnger();
			this.playSound(this.getAngrySound());
		}
	}

	/**
	 * Bond with player.
	 *
	 * @param player player
	 *
	 * @return boolean — результат операции
	 */
	public boolean bondWithPlayer(PlayerEntity player) {
		this.setOwner(player);
		this.setTame(true);
		if (player instanceof ServerPlayerEntity) {
			Criteria.TAME_ANIMAL.trigger((ServerPlayerEntity) player, this);
		}

		this.getEntityWorld().sendEntityStatus(this, (byte) 7);
		return true;
	}

	@Override
	protected void tickControlled(PlayerEntity controllingPlayer, Vec3d movementInput) {
		super.tickControlled(controllingPlayer, movementInput);
		Vec2f vec2f = this.getControlledRotation(controllingPlayer);
		this.setRotation(vec2f.y, vec2f.x);
		this.lastYaw = this.bodyYaw = this.headYaw = this.getYaw();
		if (this.isLogicalSideForUpdatingMovement()) {
			if (movementInput.z <= 0.0) {
				this.soundTicks = 0;
			}

			if (this.isOnGround()) {
				if (this.jumpStrength > 0.0F && !this.isJumping()) {
					this.jump(this.jumpStrength, movementInput);
				}

				this.jumpStrength = 0.0F;
			}
		}
	}

	protected Vec2f getControlledRotation(LivingEntity controllingPassenger) {
		return new Vec2f(controllingPassenger.getPitch() * 0.5F, controllingPassenger.getYaw());
	}

	@Override
	protected void addPassenger(Entity passenger) {
		super.addPassenger(passenger);
		passenger.setAngles(this.getYaw(0.0F), this.getPitch(0.0F));
	}

	@Override
	protected Vec3d getControlledMovementInput(PlayerEntity controllingPlayer, Vec3d movementInput) {
		if (this.isOnGround() && this.jumpStrength == 0.0F && this.isAngry() && !this.jumping) {
			return Vec3d.ZERO;
		}
		else {
			float f = controllingPlayer.sidewaysSpeed * 0.5F;
			float g = controllingPlayer.forwardSpeed;
			if (g <= 0.0F) {
				g *= 0.25F;
			}

			return new Vec3d(f, 0.0, g);
		}
	}

	@Override
	protected float getSaddledSpeed(PlayerEntity controllingPlayer) {
		return (float) this.getAttributeValue(EntityAttributes.MOVEMENT_SPEED);
	}

	/**
	 * Jump.
	 *
	 * @param strength strength
	 * @param movementInput movement input
	 */
	protected void jump(float strength, Vec3d movementInput) {
		double d = this.getJumpVelocity(strength);
		Vec3d vec3d = this.getVelocity();
		this.setVelocity(vec3d.x, d, vec3d.z);
		this.velocityDirty = true;
		if (movementInput.z > 0.0) {
			float f = MathHelper.sin(this.getYaw() * (float) (Math.PI / 180.0));
			float g = MathHelper.cos(this.getYaw() * (float) (Math.PI / 180.0));
			this.setVelocity(this.getVelocity().add(-0.4F * f * strength, 0.0, 0.4F * g * strength));
		}
	}

	/**
	 * Play jump sound.
	 */
	protected void playJumpSound() {
		this.playSound(SoundEvents.ENTITY_HORSE_JUMP, 0.4F, 1.0F);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putBoolean("EatingHaystack", this.isEatingGrass());
		view.putBoolean("Bred", this.isBred());
		view.putInt("Temper", this.getTemper());
		view.putBoolean("Tame", this.isTame());
		LazyEntityReference.writeData(this.ownerReference, view, "Owner");
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		this.setEatingGrass(view.getBoolean("EatingHaystack", false));
		this.setBred(view.getBoolean("Bred", false));
		this.setTemper(view.getInt("Temper", 0));
		this.setTame(view.getBoolean("Tame", false));
		this.ownerReference = LazyEntityReference.fromDataOrPlayerName(view, "Owner", this.getEntityWorld());
	}

	@Override
	public boolean canBreedWith(AnimalEntity other) {
		return false;
	}

	/**
	 * Проверяет возможность breed.
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	protected boolean canBreed() {
		return !this.hasPassengers() && !this.hasVehicle() && this.isTame() && !this.isBaby()
				&& this.getHealth() >= this.getMaxHealth() && this.isInLove();
	}

	public boolean isControlledByMob() {
		return false;
	}

	@Override
	public @Nullable PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
		return null;
	}

	protected void setChildAttributes(PassiveEntity other, AbstractHorseEntity child) {
		this.setChildAttribute(other, child, EntityAttributes.MAX_HEALTH, MIN_HEALTH_BONUS, MAX_HEALTH_BONUS);
		this.setChildAttribute(
				other,
				child,
				EntityAttributes.JUMP_STRENGTH,
				MIN_JUMP_STRENGTH_BONUS,
				MAX_JUMP_STRENGTH_BONUS
		);
		this.setChildAttribute(
				other,
				child,
				EntityAttributes.MOVEMENT_SPEED,
				MIN_MOVEMENT_SPEED_BONUS,
				MAX_MOVEMENT_SPEED_BONUS
		);
	}

	private void setChildAttribute(
			PassiveEntity other,
			AbstractHorseEntity child,
			RegistryEntry<EntityAttribute> attribute,
			double min,
			double max
	) {
		double
				d =
				calculateAttributeBaseValue(
						this.getAttributeBaseValue(attribute),
						other.getAttributeBaseValue(attribute),
						min,
						max,
						this.random
				);
		child.getAttributeInstance(attribute).setBaseValue(d);
	}

	static double calculateAttributeBaseValue(
			double parentBase,
			double otherParentBase,
			double min,
			double max,
			Random random
	) {
		if (max <= min) {
			throw new IllegalArgumentException("Incorrect range for an attribute");
		}
		else {
			parentBase = MathHelper.clamp(parentBase, min, max);
			otherParentBase = MathHelper.clamp(otherParentBase, min, max);
			double d = 0.15 * (max - min);
			double e = Math.abs(parentBase - otherParentBase) + d * 2.0;
			double f = (parentBase + otherParentBase) / 2.0;
			double g = (random.nextDouble() + random.nextDouble() + random.nextDouble()) / 3.0 - 0.5;
			double h = f + e * g;
			if (h > max) {
				double i = h - max;
				return max - i;
			}
			else if (h < min) {
				double i = min - h;
				return min + i;
			}
			else {
				return h;
			}
		}
	}

	public float getEatingGrassAnimationProgress(float tickProgress) {
		return MathHelper.lerp(tickProgress, this.lastEatingGrassAnimationProgress, this.eatingGrassAnimationProgress);
	}

	public float getAngryAnimationProgress(float tickProgress) {
		return MathHelper.lerp(tickProgress, this.lastAngryAnimationProgress, this.angryAnimationProgress);
	}

	public float getEatingAnimationProgress(float tickProgress) {
		return MathHelper.lerp(tickProgress, this.lastEatingAnimationProgress, this.eatingAnimationProgress);
	}

	@Override
	public void setJumpStrength(int strength) {
		if (this.hasSaddleEquipped()) {
			if (strength < 0) {
				strength = 0;
			}
			else {
				this.jumping = true;
				this.updateAnger();
			}

			this.jumpStrength = this.clampJumpStrength(strength);
		}
	}

	@Override
	public boolean canJump() {
		return this.hasSaddleEquipped();
	}

	@Override
	public void startJumping(int height) {
		this.jumping = true;
		this.updateAnger();
		this.playJumpSound();
	}

	@Override
	public void stopJumping() {
	}

	/**
	 * Создаёт (спавнит) player reaction particles.
	 *
	 * @param positive positive
	 */
	protected void spawnPlayerReactionParticles(boolean positive) {
		ParticleEffect particleEffect = positive ? ParticleTypes.HEART : ParticleTypes.SMOKE;

		for (int i = 0; i < 7; i++) {
			double d = this.random.nextGaussian() * 0.02;
			double e = this.random.nextGaussian() * 0.02;
			double f = this.random.nextGaussian() * 0.02;
			this
					.getEntityWorld()
					.addParticleClient(
							particleEffect,
							this.getParticleX(1.0),
							this.getRandomBodyY() + 0.5,
							this.getParticleZ(1.0),
							d,
							e,
							f
					);
		}
	}

	@Override
	public void handleStatus(byte status) {
		if (status == 7) {
			this.spawnPlayerReactionParticles(true);
		}
		else if (status == 6) {
			this.spawnPlayerReactionParticles(false);
		}
		else {
			super.handleStatus(status);
		}
	}

	@Override
	protected void updatePassengerPosition(Entity passenger, Entity.PositionUpdater positionUpdater) {
		super.updatePassengerPosition(passenger, positionUpdater);
		if (passenger instanceof LivingEntity) {
			((LivingEntity) passenger).bodyYaw = this.bodyYaw;
		}
	}

	protected static float getChildHealthBonus(IntUnaryOperator randomIntGetter) {
		return 15.0F + randomIntGetter.applyAsInt(8) + randomIntGetter.applyAsInt(9);
	}

	protected static double getChildJumpStrengthBonus(DoubleSupplier randomDoubleGetter) {
		return 0.4F + randomDoubleGetter.getAsDouble() * 0.2 + randomDoubleGetter.getAsDouble() * 0.2
				+ randomDoubleGetter.getAsDouble() * 0.2;
	}

	protected static double getChildMovementSpeedBonus(DoubleSupplier randomDoubleGetter) {
		return (0.45F + randomDoubleGetter.getAsDouble() * 0.3 + randomDoubleGetter.getAsDouble() * 0.3
				+ randomDoubleGetter.getAsDouble() * 0.3
		) * 0.25;
	}

	@Override
	public boolean isClimbing() {
		return false;
	}

	@Override
	public @Nullable StackReference getStackReference(int slot) {
		int i = slot - 500;
		return i >= 0 && i < this.items.size() ? this.items.getStackReference(i) : super.getStackReference(slot);
	}

	@Override
	public @Nullable LivingEntity getControllingPassenger() {
		return (LivingEntity) (this.hasSaddleEquipped() && this.getFirstPassenger() instanceof PlayerEntity playerEntity
		                       ? playerEntity
		                       : super.getControllingPassenger()
		);
	}

	private @Nullable Vec3d locateSafeDismountingPos(Vec3d offset, LivingEntity passenger) {
		double d = this.getX() + offset.x;
		double e = this.getBoundingBox().minY;
		double f = this.getZ() + offset.z;
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		UnmodifiableIterator var10 = passenger.getPoses().iterator();

		while (var10.hasNext()) {
			EntityPose entityPose = (EntityPose) var10.next();
			mutable.set(d, e, f);
			double g = this.getBoundingBox().maxY + 0.75;

			do {
				double h = this.getEntityWorld().getDismountHeight(mutable);
				if (mutable.getY() + h > g) {
					break;
				}

				if (Dismounting.canDismountInBlock(h)) {
					Box box = passenger.getBoundingBox(entityPose);
					Vec3d vec3d = new Vec3d(d, mutable.getY() + h, f);
					if (Dismounting.canPlaceEntityAt(this.getEntityWorld(), passenger, box.offset(vec3d))) {
						passenger.setPose(entityPose);
						return vec3d;
					}
				}

				mutable.move(Direction.UP);
			}
			while (mutable.getY() < g);
		}

		return null;
	}

	@Override
	public Vec3d updatePassengerForDismount(LivingEntity passenger) {
		Vec3d
				vec3d =
				getPassengerDismountOffset(
						this.getWidth(),
						passenger.getWidth(),
						this.getYaw() + (passenger.getMainArm() == Arm.RIGHT ? 90.0F : -90.0F)
				);
		Vec3d vec3d2 = this.locateSafeDismountingPos(vec3d, passenger);
		if (vec3d2 != null) {
			return vec3d2;
		}
		else {
			Vec3d
					vec3d3 =
					getPassengerDismountOffset(
							this.getWidth(),
							passenger.getWidth(),
							this.getYaw() + (passenger.getMainArm() == Arm.LEFT ? 90.0F : -90.0F)
					);
			Vec3d vec3d4 = this.locateSafeDismountingPos(vec3d3, passenger);
			return vec3d4 != null ? vec3d4 : this.getEntityPos();
		}
	}

	/**
	 * Инициализирует attributes.
	 *
	 * @param random random
	 */
	protected void initAttributes(Random random) {
	}

	@Override
	public @Nullable EntityData initialize(
			ServerWorldAccess world,
			LocalDifficulty difficulty,
			SpawnReason spawnReason,
			@Nullable EntityData entityData
	) {
		if (entityData == null) {
			entityData = new PassiveEntity.PassiveData(0.2F);
		}

		this.initAttributes(world.getRandom());
		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	/**
	 * Are inventories different.
	 *
	 * @param inventory inventory
	 *
	 * @return boolean — результат операции
	 */
	public boolean areInventoriesDifferent(Inventory inventory) {
		return this.items != inventory;
	}

	public int getMinAmbientStandDelay() {
		return this.getMinAmbientSoundDelay();
	}

	@Override
	protected Vec3d getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
		return super.getPassengerAttachmentPos(passenger, dimensions, scaleFactor)
		            .add(
				            new Vec3d(
						            0.0,
						            0.15 * this.lastAngryAnimationProgress * scaleFactor,
						            -0.7 * this.lastAngryAnimationProgress * scaleFactor
				            )
						            .rotateY(-this.getYaw() * (float) (Math.PI / 180.0))
		            );
	}

	public int getInventoryColumns() {
		return 0;
	}

	/**
	 * {@code HorseEscapeDangerGoal}.
	 */
	class HorseEscapeDangerGoal extends EscapeDangerGoal {

		public HorseEscapeDangerGoal(final double speed) {
			super(AbstractHorseEntity.this, speed);
		}

		@Override
		public boolean isInDanger() {
			return !AbstractHorseEntity.this.isControlledByMob() && super.isInDanger();
		}
	}
}
