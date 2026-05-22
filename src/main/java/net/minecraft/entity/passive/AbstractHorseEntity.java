package net.minecraft.entity.passive;

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
 * Базовый класс для всех лошадеподобных существ (лошадь, осёл, мул, лама).
 * Управляет приручением, инвентарём, анимациями (злость, поедание травы, еда) и прыжками.
 * Атрибуты детёнышей вычисляются как взвешенное среднее родителей с небольшим случайным отклонением.
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
	private static final int EATING_TICKS_MAX = 30;
	private static final int EATING_GRASS_TICKS_MAX = 50;
	private static final int TAIL_WAG_TICKS_MAX = 8;
	private static final int MOUNTING_TICKS_MAX = 300;
	private static final int ANGRY_TICKS_DEFAULT = 20;
	private static final int PARTICLE_COUNT = 7;
	private static final int HEAL_INTERVAL = 900;
	private static final int GRASS_EAT_CHANCE = 300;
	private static final int TAIL_WAG_CHANCE = 200;
	private static final int STEP_SOUND_INTERVAL = 5;
	private static final int STEP_SOUND_PERIOD = 3;
	private static final int INVENTORY_SLOT_OFFSET = TEMPER_RANGE;

	private static final TargetPredicate.EntityPredicate IS_BRED_HORSE =
			(entity, world) -> entity instanceof AbstractHorseEntity horse && horse.isBred();
	private static final TargetPredicate PARENT_HORSE_PREDICATE = TargetPredicate.createNonAttackable()
			.setBaseMaxDistance(16.0)
			.ignoreVisibility()
			.setPredicate(IS_BRED_HORSE);

	private static final TrackedData<Byte> HORSE_FLAGS = DataTracker.registerData(
			AbstractHorseEntity.class,
			TrackedDataHandlerRegistry.BYTE
	);
	private static final int TAMED_FLAG = 2;
	private static final int BRED_FLAG = 8;
	private static final int EATING_GRASS_FLAG = 16;
	private static final int ANGRY_FLAG = 32;
	private static final int EATING_FLAG = 64;

	public static final int INVENTORY_SLOT_COUNT = 3;

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
		onChestedStatusChanged();
	}

	@Override
	protected void initGoals() {
		goalSelector.add(1, new HorseEscapeDangerGoal(1.2));
		goalSelector.add(1, new HorseBondWithPlayerGoal(this, 1.2));
		goalSelector.add(2, new AnimalMateGoal(this, 1.0, AbstractHorseEntity.class));
		goalSelector.add(4, new FollowParentGoal(this, 1.0));
		goalSelector.add(6, new WanderAroundFarGoal(this, 0.7));
		goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
		goalSelector.add(8, new LookAroundGoal(this));
		if (shouldAmbientStand()) {
			goalSelector.add(9, new AmbientStandGoal(this));
		}

		initCustomGoals();
	}

	protected void initCustomGoals() {
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(3, new TemptGoal(this, 1.25, stack -> stack.isIn(ItemTags.HORSE_TEMPT_ITEMS), false));
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(HORSE_FLAGS, (byte) 0);
	}

	protected boolean getHorseFlag(int bitmask) {
		return (dataTracker.get(HORSE_FLAGS) & bitmask) != 0;
	}

	protected void setHorseFlag(int bitmask, boolean flag) {
		byte current = dataTracker.get(HORSE_FLAGS);
		dataTracker.set(HORSE_FLAGS, flag ? (byte) (current | bitmask) : (byte) (current & ~bitmask));
	}

	public boolean isTame() {
		return getHorseFlag(TAMED_FLAG);
	}

	@Override
	public @Nullable LazyEntityReference<LivingEntity> getOwnerReference() {
		return ownerReference;
	}

	public void setOwner(@Nullable LivingEntity entity) {
		ownerReference = LazyEntityReference.of(entity);
	}

	public void setTame(boolean tame) {
		setHorseFlag(TAMED_FLAG, tame);
	}

	@Override
	public void onLongLeashTick() {
		super.onLongLeashTick();
		if (isEatingGrass()) {
			setEatingGrass(false);
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
		return getHorseFlag(EATING_GRASS_FLAG);
	}

	public boolean isAngry() {
		return getHorseFlag(ANGRY_FLAG);
	}

	public boolean isBred() {
		return getHorseFlag(BRED_FLAG);
	}

	public void setBred(boolean bred) {
		setHorseFlag(BRED_FLAG, bred);
	}

	@Override
	public boolean canUseSlot(EquipmentSlot slot) {
		return slot != EquipmentSlot.SADDLE
				? super.canUseSlot(slot)
				: isAlive() && !isBaby() && isTame();
	}

	public void equipHorseArmor(PlayerEntity player, ItemStack stack) {
		if (canEquip(stack, EquipmentSlot.BODY)) {
			equipBodyArmor(stack.splitUnlessCreative(1, player));
		}
	}

	@Override
	protected boolean canDispenserEquipSlot(EquipmentSlot slot) {
		return (slot == EquipmentSlot.BODY || slot == EquipmentSlot.SADDLE) && isTame()
				|| super.canDispenserEquipSlot(slot);
	}

	public int getTemper() {
		return temper;
	}

	public void setTemper(int temper) {
		this.temper = temper;
	}

	/**
	 * Увеличивает темперамент лошади на указанное значение, ограничивая результат диапазоном [0, maxTemper].
	 *
	 * @param difference прибавка к темпераменту
	 * @return итоговое значение темперамента
	 */
	public int addTemper(int difference) {
		int newTemper = MathHelper.clamp(getTemper() + difference, 0, getMaxTemper());
		setTemper(newTemper);
		return newTemper;
	}

	@Override
	public boolean isPushable() {
		return !hasPassengers();
	}

	private void playEatingAnimation() {
		setEating();
		if (isSilent()) {
			return;
		}

		SoundEvent eatSound = getEatSound();
		if (eatSound == null) {
			return;
		}

		getEntityWorld().playSound(
				null,
				getX(),
				getY(),
				getZ(),
				eatSound,
				getSoundCategory(),
				1.0F,
				1.0F + (random.nextFloat() - random.nextFloat()) * 0.2F
		);
	}

	@Override
	public boolean handleFallDamage(double fallDistance, float damagePerDistance, DamageSource damageSource) {
		if (fallDistance > 1.0) {
			playSound(SoundEvents.ENTITY_HORSE_LAND, 0.4F, 1.0F);
		}

		int damage = computeFallDamage(fallDistance, damagePerDistance);
		if (damage <= 0) {
			return false;
		}

		serverDamage(damageSource, damage);
		handleFallDamageForPassengers(fallDistance, damagePerDistance, damageSource);
		playBlockFallSound();
		return true;
	}

	public final int getInventorySize() {
		return MountScreenHandler.getSlotCount(getInventoryColumns());
	}

	/**
	 * Пересоздаёт инвентарь при изменении наличия сундука (осёл/лама).
	 * Копирует существующие предметы в новый инвентарь нужного размера.
	 */
	protected void onChestedStatusChanged() {
		SimpleInventory oldInventory = items;
		items = new SimpleInventory(getInventorySize());
		if (oldInventory == null) {
			return;
		}

		int copyCount = Math.min(oldInventory.size(), items.size());
		for (int slot = 0; slot < copyCount; slot++) {
			ItemStack stack = oldInventory.getStack(slot);
			if (!stack.isEmpty()) {
				items.setStack(slot, stack.copy());
			}
		}
	}

	@Override
	protected RegistryEntry<SoundEvent> getEquipSound(
			EquipmentSlot slot,
			ItemStack stack,
			EquippableComponent equippableComponent
	) {
		return slot == EquipmentSlot.SADDLE
				? (RegistryEntry<SoundEvent>) SoundEvents.ENTITY_HORSE_SADDLE
				: super.getEquipSound(slot, stack, equippableComponent);
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		boolean damaged = super.damage(world, source, amount);
		if (damaged && random.nextInt(3) == 0) {
			updateAnger();
		}

		return damaged;
	}

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
		if (state.isLiquid()) {
			return;
		}

		BlockState above = getEntityWorld().getBlockState(pos.up());
		BlockSoundGroup soundGroup = state.getSoundGroup();
		if (above.isOf(Blocks.SNOW)) {
			soundGroup = above.getSoundGroup();
		}

		if (hasPassengers() && playExtraHorseSounds) {
			soundTicks++;
			if (soundTicks > STEP_SOUND_INTERVAL && soundTicks % STEP_SOUND_PERIOD == 0) {
				playWalkSound(soundGroup);
			}
			else if (soundTicks <= STEP_SOUND_INTERVAL) {
				playSound(
						SoundEvents.ENTITY_HORSE_STEP_WOOD,
						soundGroup.getVolume() * 0.15F,
						soundGroup.getPitch()
				);
			}
		}
		else if (isWooden(soundGroup)) {
			playSound(SoundEvents.ENTITY_HORSE_STEP_WOOD, soundGroup.getVolume() * 0.15F, soundGroup.getPitch());
		}
		else {
			playSound(SoundEvents.ENTITY_HORSE_STEP, soundGroup.getVolume() * 0.15F, soundGroup.getPitch());
		}
	}

	private boolean isWooden(BlockSoundGroup soundGroup) {
		return soundGroup == BlockSoundGroup.WOOD
				|| soundGroup == BlockSoundGroup.NETHER_WOOD
				|| soundGroup == BlockSoundGroup.NETHER_STEM
				|| soundGroup == BlockSoundGroup.CHERRY_WOOD
				|| soundGroup == BlockSoundGroup.BAMBOO_WOOD;
	}

	protected void playWalkSound(BlockSoundGroup group) {
		playSound(SoundEvents.ENTITY_HORSE_GALLOP, group.getVolume() * 0.15F, group.getPitch());
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
		if (!getEntityWorld().isClient() && (!hasPassengers() || hasPassenger(player)) && isTame()) {
			player.openHorseInventory(this, items);
		}
	}

	/**
	 * Обрабатывает взаимодействие с едой: кормление, рост детёнышей, повышение темперамента.
	 *
	 * @param player игрок, взаимодействующий с лошадью
	 * @param stack  предмет в руке игрока
	 * @return {@link ActionResult#SUCCESS_SERVER} если еда была принята, иначе {@link ActionResult#PASS}
	 */
	public ActionResult interactHorse(PlayerEntity player, ItemStack stack) {
		boolean consumed = receiveFood(player, stack);
		if (consumed) {
			stack.decrementUnlessCreative(1, player);
		}

		return !consumed && !getEntityWorld().isClient() ? ActionResult.PASS : ActionResult.SUCCESS_SERVER;
	}

	/**
	 * Обрабатывает кормление лошади: лечение, рост, повышение темперамента и запуск размножения.
	 * Каждый тип еды имеет свои значения лечения, роста и бонуса темперамента.
	 *
	 * @param player игрок, дающий еду
	 * @param item   предмет-еда
	 * @return {@code true} если еда была использована
	 */
	protected boolean receiveFood(PlayerEntity player, ItemStack item) {
		boolean consumed = false;
		float healAmount = 0.0F;
		int growthTicks = 0;
		int temperBonus = 0;

		if (item.isOf(Items.WHEAT)) {
			healAmount = 2.0F;
			growthTicks = 20;
			temperBonus = 3;
		}
		else if (item.isOf(Items.SUGAR)) {
			healAmount = 1.0F;
			growthTicks = 30;
			temperBonus = 3;
		}
		else if (item.isOf(Blocks.HAY_BLOCK.asItem())) {
			healAmount = 20.0F;
			growthTicks = 180;
		}
		else if (item.isOf(Items.APPLE)) {
			healAmount = 3.0F;
			growthTicks = 60;
			temperBonus = 3;
		}
		else if (item.isOf(Items.RED_MUSHROOM)) {
			healAmount = 3.0F;
			temperBonus = 3;
		}
		else if (item.isOf(Items.CARROT)) {
			healAmount = 3.0F;
			growthTicks = 60;
			temperBonus = 3;
		}
		else if (item.isOf(Items.GOLDEN_CARROT)) {
			healAmount = 4.0F;
			growthTicks = 60;
			temperBonus = 5;
			if (!getEntityWorld().isClient() && isTame() && getBreedingAge() == 0 && !isInLove()) {
				consumed = true;
				lovePlayer(player);
			}
		}
		else if (item.isOf(Items.GOLDEN_APPLE) || item.isOf(Items.ENCHANTED_GOLDEN_APPLE)) {
			healAmount = 10.0F;
			growthTicks = 240;
			temperBonus = 10;
			if (!getEntityWorld().isClient() && isTame() && getBreedingAge() == 0 && !isInLove()) {
				consumed = true;
				lovePlayer(player);
			}
		}

		if (getHealth() < getMaxHealth() && healAmount > 0.0F) {
			heal(healAmount);
			consumed = true;
		}

		if (isBaby() && growthTicks > 0) {
			getEntityWorld().addParticleClient(
					ParticleTypes.HAPPY_VILLAGER,
					getParticleX(1.0),
					getRandomBodyY() + 0.5,
					getParticleZ(1.0),
					0.0,
					0.0,
					0.0
			);
			if (!getEntityWorld().isClient()) {
				growUp(growthTicks);
				consumed = true;
			}
		}

		if (temperBonus > 0 && (consumed || !isTame()) && getTemper() < getMaxTemper()
				&& !getEntityWorld().isClient()
		) {
			addTemper(temperBonus);
			consumed = true;
		}

		if (consumed) {
			playEatingAnimation();
			emitGameEvent(GameEvent.EAT);
		}

		return consumed;
	}

	protected void putPlayerOnBack(PlayerEntity player) {
		setEatingGrass(false);
		setNotAngry();
		if (!getEntityWorld().isClient()) {
			player.setYaw(getYaw());
			player.setPitch(getPitch());
			player.startRiding(this);
		}
	}

	@Override
	public boolean isImmobile() {
		return super.isImmobile() && hasPassengers() && hasSaddleEquipped() || isEatingGrass() || isAngry();
	}

	@Override
	public boolean isBreedingItem(ItemStack stack) {
		return stack.isIn(ItemTags.HORSE_FOOD);
	}

	private void wagTail() {
		tailWagTicks = 1;
	}

	@Override
	protected void dropInventory(ServerWorld world) {
		super.dropInventory(world);
		if (items == null) {
			return;
		}

		for (int slot = 0; slot < items.size(); slot++) {
			ItemStack stack = items.getStack(slot);
			if (!stack.isEmpty() && !EnchantmentHelper.hasAnyEnchantmentsWith(
					stack,
					EnchantmentEffectComponentTypes.PREVENT_EQUIPMENT_DROP
			)) {
				dropStack(world, stack);
			}
		}
	}

	@Override
	public void tickMovement() {
		if (random.nextInt(TAIL_WAG_CHANCE) == 0) {
			wagTail();
		}

		super.tickMovement();
		if (!(getEntityWorld() instanceof ServerWorld serverWorld) || !isAlive()) {
			return;
		}

		if (random.nextInt(HEAL_INTERVAL) == 0 && deathTime == 0) {
			heal(1.0F);
		}

		if (eatsGrass()) {
			if (!isEatingGrass()
					&& !hasPassengers()
					&& random.nextInt(GRASS_EAT_CHANCE) == 0
					&& serverWorld.getBlockState(getBlockPos().down()).isOf(Blocks.GRASS_BLOCK)
			) {
				setEatingGrass(true);
			}

			if (isEatingGrass() && ++eatingGrassTicks > EATING_GRASS_TICKS_MAX) {
				eatingGrassTicks = 0;
				setEatingGrass(false);
			}
		}

		walkToParent(serverWorld);
	}

	protected void walkToParent(ServerWorld world) {
		if (!isBred() || !isBaby() || isEatingGrass()) {
			return;
		}

		LivingEntity parent = world.getClosestEntity(
				AbstractHorseEntity.class,
				PARENT_HORSE_PREDICATE,
				this,
				getX(),
				getY(),
				getZ(),
				getBoundingBox().expand(16.0)
		);
		if (parent != null && squaredDistanceTo(parent) > 4.0) {
			navigation.findPathTo(parent, 0);
		}
	}

	public boolean eatsGrass() {
		return true;
	}

	@Override
	public void tick() {
		super.tick();
		if (eatingTicks > 0 && ++eatingTicks > EATING_TICKS_MAX) {
			eatingTicks = 0;
			setHorseFlag(EATING_FLAG, false);
		}

		if (angryTicks > 0 && --angryTicks <= 0) {
			setNotAngry();
		}

		if (tailWagTicks > 0 && ++tailWagTicks > TAIL_WAG_TICKS_MAX) {
			tailWagTicks = 0;
		}

		if (mountingTicks > 0) {
			mountingTicks++;
			if (mountingTicks > MOUNTING_TICKS_MAX) {
				mountingTicks = 0;
			}
		}

		lastEatingGrassAnimationProgress = eatingGrassAnimationProgress;
		if (isEatingGrass()) {
			eatingGrassAnimationProgress += (1.0F - eatingGrassAnimationProgress) * 0.4F + 0.05F;
			if (eatingGrassAnimationProgress > 1.0F) {
				eatingGrassAnimationProgress = 1.0F;
			}
		}
		else {
			eatingGrassAnimationProgress += (0.0F - eatingGrassAnimationProgress) * 0.4F - 0.05F;
			if (eatingGrassAnimationProgress < 0.0F) {
				eatingGrassAnimationProgress = 0.0F;
			}
		}

		lastAngryAnimationProgress = angryAnimationProgress;
		if (isAngry()) {
			eatingGrassAnimationProgress = 0.0F;
			lastEatingGrassAnimationProgress = eatingGrassAnimationProgress;
			angryAnimationProgress += (1.0F - angryAnimationProgress) * 0.4F + 0.05F;
			if (angryAnimationProgress > 1.0F) {
				angryAnimationProgress = 1.0F;
			}
		}
		else {
			jumping = false;
			angryAnimationProgress += (0.8F * angryAnimationProgress * angryAnimationProgress * angryAnimationProgress
					- angryAnimationProgress) * 0.6F - 0.05F;
			if (angryAnimationProgress < 0.0F) {
				angryAnimationProgress = 0.0F;
			}
		}

		lastEatingAnimationProgress = eatingAnimationProgress;
		if (getHorseFlag(EATING_FLAG)) {
			eatingAnimationProgress += (1.0F - eatingAnimationProgress) * 0.7F + 0.05F;
			if (eatingAnimationProgress > 1.0F) {
				eatingAnimationProgress = 1.0F;
			}
		}
		else {
			eatingAnimationProgress += (0.0F - eatingAnimationProgress) * 0.7F - 0.05F;
			if (eatingAnimationProgress < 0.0F) {
				eatingAnimationProgress = 0.0F;
			}
		}
	}

	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		if (hasPassengers() || isBaby()) {
			return super.interactMob(player, hand);
		}

		if (isTame() && player.shouldCancelInteraction()) {
			openInventory(player);
			return ActionResult.SUCCESS;
		}

		ItemStack held = player.getStackInHand(hand);
		if (!held.isEmpty()) {
			ActionResult useResult = held.useOnEntity(player, this, hand);
			if (useResult.isAccepted()) {
				return useResult;
			}

			if (canEquip(held, EquipmentSlot.BODY) && !isWearingBodyArmor()) {
				equipHorseArmor(player, held);
				return ActionResult.SUCCESS;
			}
		}

		putPlayerOnBack(player);
		return ActionResult.SUCCESS;
	}

	private void setEating() {
		if (!getEntityWorld().isClient()) {
			eatingTicks = 1;
			setHorseFlag(EATING_FLAG, true);
		}
	}

	public void setEatingGrass(boolean eatingGrass) {
		setHorseFlag(EATING_GRASS_FLAG, eatingGrass);
	}

	public void setAngry(int ticks) {
		setEatingGrass(false);
		setHorseFlag(ANGRY_FLAG, true);
		angryTicks = ticks;
	}

	public void setNotAngry() {
		setHorseFlag(ANGRY_FLAG, false);
		angryTicks = 0;
	}

	public @Nullable SoundEvent getAmbientStandSound() {
		return getAmbientSound();
	}

	public void updateAnger() {
		if (shouldAmbientStand() && (canActVoluntarily() || !getEntityWorld().isClient())) {
			setAngry(ANGRY_TICKS_DEFAULT);
		}
	}

	public void playAngrySound() {
		if (isAngry() || getEntityWorld().isClient()) {
			return;
		}

		updateAnger();
		playSound(getAngrySound());
	}

	/**
	 * Приручает лошадь: устанавливает владельца, флаг приручения и выдаёт достижение игроку.
	 *
	 * @param player игрок, приручивший лошадь
	 * @return всегда {@code true}
	 */
	public boolean bondWithPlayer(PlayerEntity player) {
		setOwner(player);
		setTame(true);
		if (player instanceof ServerPlayerEntity serverPlayer) {
			Criteria.TAME_ANIMAL.trigger(serverPlayer, this);
		}

		getEntityWorld().sendEntityStatus(this, (byte) 7);
		return true;
	}

	@Override
	protected void tickControlled(PlayerEntity controllingPlayer, Vec3d movementInput) {
		super.tickControlled(controllingPlayer, movementInput);
		Vec2f rotation = getControlledRotation(controllingPlayer);
		setRotation(rotation.y, rotation.x);
		lastYaw = bodyYaw = headYaw = getYaw();
		if (!isLogicalSideForUpdatingMovement()) {
			return;
		}

		if (movementInput.z <= 0.0) {
			soundTicks = 0;
		}

		if (isOnGround()) {
			if (jumpStrength > 0.0F && !isJumping()) {
				jump(jumpStrength, movementInput);
			}

			jumpStrength = 0.0F;
		}
	}

	protected Vec2f getControlledRotation(LivingEntity controllingPassenger) {
		return new Vec2f(controllingPassenger.getPitch() * 0.5F, controllingPassenger.getYaw());
	}

	@Override
	protected void addPassenger(Entity passenger) {
		super.addPassenger(passenger);
		passenger.setAngles(getYaw(0.0F), getPitch(0.0F));
	}

	@Override
	protected Vec3d getControlledMovementInput(PlayerEntity controllingPlayer, Vec3d movementInput) {
		if (isOnGround() && jumpStrength == 0.0F && isAngry() && !jumping) {
			return Vec3d.ZERO;
		}

		float sideways = controllingPlayer.sidewaysSpeed * 0.5F;
		float forward = controllingPlayer.forwardSpeed;
		if (forward <= 0.0F) {
			forward *= MIN_SPEED;
		}

		return new Vec3d(sideways, 0.0, forward);
	}

	@Override
	protected float getSaddledSpeed(PlayerEntity controllingPlayer) {
		return (float) getAttributeValue(EntityAttributes.MOVEMENT_SPEED);
	}

	/**
	 * Выполняет прыжок лошади с учётом силы прыжка и направления движения.
	 * При движении вперёд добавляет горизонтальную составляющую скорости.
	 *
	 * @param strength     сила прыжка [0..1]
	 * @param movementInput вектор ввода управления
	 */
	protected void jump(float strength, Vec3d movementInput) {
		double jumpVelocity = getJumpVelocity(strength);
		Vec3d velocity = getVelocity();
		setVelocity(velocity.x, jumpVelocity, velocity.z);
		velocityDirty = true;
		if (movementInput.z > 0.0) {
			float sinYaw = MathHelper.sin(getYaw() * (float) (Math.PI / 180.0));
			float cosYaw = MathHelper.cos(getYaw() * (float) (Math.PI / 180.0));
			setVelocity(getVelocity().add(-0.4F * sinYaw * strength, 0.0, 0.4F * cosYaw * strength));
		}
	}

	protected void playJumpSound() {
		playSound(SoundEvents.ENTITY_HORSE_JUMP, 0.4F, 1.0F);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putBoolean("EatingHaystack", isEatingGrass());
		view.putBoolean("Bred", isBred());
		view.putInt("Temper", getTemper());
		view.putBoolean("Tame", isTame());
		LazyEntityReference.writeData(ownerReference, view, "Owner");
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		setEatingGrass(view.getBoolean("EatingHaystack", false));
		setBred(view.getBoolean("Bred", false));
		setTemper(view.getInt("Temper", 0));
		setTame(view.getBoolean("Tame", false));
		ownerReference = LazyEntityReference.fromDataOrPlayerName(view, "Owner", getEntityWorld());
	}

	@Override
	public boolean canBreedWith(AnimalEntity other) {
		return false;
	}

	protected boolean canBreed() {
		return !hasPassengers() && !hasVehicle() && isTame() && !isBaby()
				&& getHealth() >= getMaxHealth() && isInLove();
	}

	public boolean isControlledByMob() {
		return false;
	}

	@Override
	public @Nullable PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
		return null;
	}

	protected void setChildAttributes(PassiveEntity other, AbstractHorseEntity child) {
		setChildAttribute(other, child, EntityAttributes.MAX_HEALTH, MIN_HEALTH_BONUS, MAX_HEALTH_BONUS);
		setChildAttribute(other, child, EntityAttributes.JUMP_STRENGTH, MIN_JUMP_STRENGTH_BONUS, MAX_JUMP_STRENGTH_BONUS);
		setChildAttribute(other, child, EntityAttributes.MOVEMENT_SPEED, MIN_MOVEMENT_SPEED_BONUS, MAX_MOVEMENT_SPEED_BONUS);
	}

	private void setChildAttribute(
			PassiveEntity other,
			AbstractHorseEntity child,
			RegistryEntry<EntityAttribute> attribute,
			double min,
			double max
	) {
		double value = calculateAttributeBaseValue(
				getAttributeBaseValue(attribute),
				other.getAttributeBaseValue(attribute),
				min,
				max,
				random
		);
		child.getAttributeInstance(attribute).setBaseValue(value);
	}

	/**
	 * Вычисляет базовое значение атрибута детёныша как взвешенное среднее родителей
	 * с небольшим случайным отклонением (±15% от диапазона).
	 * Результат отражается от границ диапазона, если выходит за них.
	 */
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

		parentBase = MathHelper.clamp(parentBase, min, max);
		otherParentBase = MathHelper.clamp(otherParentBase, min, max);
		double spread = TAMING_SPEED * (max - min);
		double range = Math.abs(parentBase - otherParentBase) + spread * 2.0;
		double midpoint = (parentBase + otherParentBase) / 2.0;
		double randomOffset = (random.nextDouble() + random.nextDouble() + random.nextDouble()) / 3.0 - 0.5;
		double result = midpoint + range * randomOffset;

		if (result > max) {
			return max - (result - max);
		}

		if (result < min) {
			return min + (min - result);
		}

		return result;
	}

	public float getEatingGrassAnimationProgress(float tickProgress) {
		return MathHelper.lerp(tickProgress, lastEatingGrassAnimationProgress, eatingGrassAnimationProgress);
	}

	public float getAngryAnimationProgress(float tickProgress) {
		return MathHelper.lerp(tickProgress, lastAngryAnimationProgress, angryAnimationProgress);
	}

	public float getEatingAnimationProgress(float tickProgress) {
		return MathHelper.lerp(tickProgress, lastEatingAnimationProgress, eatingAnimationProgress);
	}

	@Override
	public void setJumpStrength(int strength) {
		if (!hasSaddleEquipped()) {
			return;
		}

		if (strength < 0) {
			strength = 0;
		}
		else {
			jumping = true;
			updateAnger();
		}

		jumpStrength = clampJumpStrength(strength);
	}

	@Override
	public boolean canJump() {
		return hasSaddleEquipped();
	}

	@Override
	public void startJumping(int height) {
		jumping = true;
		updateAnger();
		playJumpSound();
	}

	@Override
	public void stopJumping() {
	}

	protected void spawnPlayerReactionParticles(boolean positive) {
		ParticleEffect particle = positive ? ParticleTypes.HEART : ParticleTypes.SMOKE;
		for (int i = 0; i < PARTICLE_COUNT; i++) {
			double vx = random.nextGaussian() * 0.02;
			double vy = random.nextGaussian() * 0.02;
			double vz = random.nextGaussian() * 0.02;
			getEntityWorld().addParticleClient(
					particle,
					getParticleX(1.0),
					getRandomBodyY() + 0.5,
					getParticleZ(1.0),
					vx,
					vy,
					vz
			);
		}
	}

	@Override
	public void handleStatus(byte status) {
		if (status == 7) {
			spawnPlayerReactionParticles(true);
		}
		else if (status == 6) {
			spawnPlayerReactionParticles(false);
		}
		else {
			super.handleStatus(status);
		}
	}

	@Override
	protected void updatePassengerPosition(Entity passenger, Entity.PositionUpdater positionUpdater) {
		super.updatePassengerPosition(passenger, positionUpdater);
		if (passenger instanceof LivingEntity livingPassenger) {
			livingPassenger.bodyYaw = bodyYaw;
		}
	}

	protected static float getChildHealthBonus(IntUnaryOperator randomIntGetter) {
		return 15.0F + randomIntGetter.applyAsInt(8) + randomIntGetter.applyAsInt(9);
	}

	protected static double getChildJumpStrengthBonus(DoubleSupplier randomDoubleGetter) {
		return 0.4F + randomDoubleGetter.getAsDouble() * 0.2
				+ randomDoubleGetter.getAsDouble() * 0.2
				+ randomDoubleGetter.getAsDouble() * 0.2;
	}

	protected static double getChildMovementSpeedBonus(DoubleSupplier randomDoubleGetter) {
		return (0.45F + randomDoubleGetter.getAsDouble() * 0.3
				+ randomDoubleGetter.getAsDouble() * 0.3
				+ randomDoubleGetter.getAsDouble() * 0.3) * MIN_SPEED;
	}

	@Override
	public boolean isClimbing() {
		return false;
	}

	@Override
	public @Nullable StackReference getStackReference(int slot) {
		int inventorySlot = slot - INVENTORY_SLOT_OFFSET;
		return inventorySlot >= 0 && inventorySlot < items.size()
				? items.getStackReference(inventorySlot)
				: super.getStackReference(slot);
	}

	@Override
	public @Nullable LivingEntity getControllingPassenger() {
		return hasSaddleEquipped() && getFirstPassenger() instanceof PlayerEntity player
				? player
				: super.getControllingPassenger();
	}

	private @Nullable Vec3d locateSafeDismountingPos(Vec3d offset, LivingEntity passenger) {
		double targetX = getX() + offset.x;
		double baseY = getBoundingBox().minY;
		double targetZ = getZ() + offset.z;
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (EntityPose pose : passenger.getPoses()) {
			mutable.set(targetX, baseY, targetZ);
			double maxY = getBoundingBox().maxY + 0.75;

			do {
				double dismountHeight = getEntityWorld().getDismountHeight(mutable);
				if (mutable.getY() + dismountHeight > maxY) {
					break;
				}

				if (Dismounting.canDismountInBlock(dismountHeight)) {
					Box box = passenger.getBoundingBox(pose);
					Vec3d dismountPos = new Vec3d(targetX, mutable.getY() + dismountHeight, targetZ);
					if (Dismounting.canPlaceEntityAt(getEntityWorld(), passenger, box.offset(dismountPos))) {
						passenger.setPose(pose);
						return dismountPos;
					}
				}

				mutable.move(Direction.UP);
			}
			while (mutable.getY() < maxY);
		}

		return null;
	}

	@Override
	public Vec3d updatePassengerForDismount(LivingEntity passenger) {
		float armAngle = passenger.getMainArm() == Arm.RIGHT ? 90.0F : -90.0F;
		Vec3d primaryOffset = getPassengerDismountOffset(getWidth(), passenger.getWidth(), getYaw() + armAngle);
		Vec3d primaryPos = locateSafeDismountingPos(primaryOffset, passenger);
		if (primaryPos != null) {
			return primaryPos;
		}

		Vec3d secondaryOffset = getPassengerDismountOffset(
				getWidth(),
				passenger.getWidth(),
				getYaw() + (passenger.getMainArm() == Arm.LEFT ? 90.0F : -90.0F)
		);
		Vec3d secondaryPos = locateSafeDismountingPos(secondaryOffset, passenger);
		return secondaryPos != null ? secondaryPos : getEntityPos();
	}

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

		initAttributes(world.getRandom());
		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	public boolean areInventoriesDifferent(Inventory inventory) {
		return items != inventory;
	}

	public int getMinAmbientStandDelay() {
		return getMinAmbientSoundDelay();
	}

	@Override
	protected Vec3d getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
		return super.getPassengerAttachmentPos(passenger, dimensions, scaleFactor)
				.add(
						new Vec3d(
								0.0,
								TAMING_SPEED * lastAngryAnimationProgress * scaleFactor,
								-0.7 * lastAngryAnimationProgress * scaleFactor
						).rotateY(-getYaw() * (float) (Math.PI / 180.0))
				);
	}

	public int getInventoryColumns() {
		return 0;
	}

	/**
	 * Цель ИИ: побег от опасности. Неактивна, если лошадь управляется мобом.
	 */
	class HorseEscapeDangerGoal extends EscapeDangerGoal {

		public HorseEscapeDangerGoal(double speed) {
			super(AbstractHorseEntity.this, speed);
		}

		@Override
		public boolean isInDanger() {
			return !AbstractHorseEntity.this.isControlledByMob() && super.isInDanger();
		}
	}
}
