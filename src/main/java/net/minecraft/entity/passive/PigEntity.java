package net.minecraft.entity.passive;

import net.minecraft.block.BlockState;
import net.minecraft.component.ComponentType;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.conversion.EntityConversionContext;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.spawn.SpawnContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Свинья — животное, которое можно оседлать и управлять с помощью морковки на удочке.
 * При ударе молнией превращается в зомби-пиглина (кроме мирного режима).
 */
public class PigEntity extends AnimalEntity implements ItemSteerable {

	private static final TrackedData<Integer> BOOST_TIME = DataTracker.registerData(
		PigEntity.class,
		TrackedDataHandlerRegistry.INTEGER
	);
	private static final TrackedData<RegistryEntry<PigVariant>> VARIANT = DataTracker.registerData(
		PigEntity.class,
		TrackedDataHandlerRegistry.PIG_VARIANT
	);

	private final SaddledComponent saddledComponent = new SaddledComponent(dataTracker, BOOST_TIME);

	public PigEntity(EntityType<? extends PigEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(1, new EscapeDangerGoal(this, 1.25));
		goalSelector.add(3, new AnimalMateGoal(this, 1.0));
		goalSelector.add(4, new TemptGoal(this, 1.2, stack -> stack.isOf(Items.CARROT_ON_A_STICK), false));
		goalSelector.add(4, new TemptGoal(this, 1.2, stack -> stack.isIn(ItemTags.PIG_FOOD), false));
		goalSelector.add(5, new FollowParentGoal(this, 1.1));
		goalSelector.add(6, new WanderAroundFarGoal(this, 1.0));
		goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
		goalSelector.add(8, new LookAroundGoal(this));
	}

	public static DefaultAttributeContainer.Builder createPigAttributes() {
		return AnimalEntity.createAnimalAttributes()
			.add(EntityAttributes.MAX_HEALTH, 10.0)
			.add(EntityAttributes.MOVEMENT_SPEED, 0.25);
	}

	@Override
	public @Nullable LivingEntity getControllingPassenger() {
		if (hasSaddleEquipped()
			&& getFirstPassenger() instanceof PlayerEntity player
			&& player.isHolding(Items.CARROT_ON_A_STICK)
		) {
			return player;
		}

		return super.getControllingPassenger();
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		if (BOOST_TIME.equals(data) && getEntityWorld().isClient()) {
			saddledComponent.boost();
		}

		super.onTrackedDataSet(data);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(BOOST_TIME, 0);
		builder.add(VARIANT, Variants.getOrDefaultOrThrow(getRegistryManager(), PigVariants.DEFAULT));
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		Variants.writeData(view, getVariant());
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		Variants.fromData(view, RegistryKeys.PIG_VARIANT).ifPresent(this::setVariant);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_PIG_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_PIG_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_PIG_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.ENTITY_PIG_STEP, 0.15F, 1.0F);
	}

	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		boolean isBreedingItem = isBreedingItem(player.getStackInHand(hand));
		if (!isBreedingItem && hasSaddleEquipped() && !hasPassengers() && !player.shouldCancelInteraction()) {
			if (!getEntityWorld().isClient()) {
				player.startRiding(this);
			}

			return ActionResult.SUCCESS;
		}

		ActionResult parentResult = super.interactMob(player, hand);
		if (parentResult.isAccepted()) {
			return parentResult;
		}

		ItemStack heldStack = player.getStackInHand(hand);
		return canEquip(heldStack, EquipmentSlot.SADDLE)
			? heldStack.useOnEntity(player, this, hand)
			: ActionResult.PASS;
	}

	@Override
	public boolean canUseSlot(EquipmentSlot slot) {
		return slot == EquipmentSlot.SADDLE ? isAlive() && !isBaby() : super.canUseSlot(slot);
	}

	@Override
	protected boolean canDispenserEquipSlot(EquipmentSlot slot) {
		return slot == EquipmentSlot.SADDLE || super.canDispenserEquipSlot(slot);
	}

	@Override
	protected RegistryEntry<SoundEvent> getEquipSound(
		EquipmentSlot slot,
		ItemStack stack,
		EquippableComponent equippableComponent
	) {
		return slot == EquipmentSlot.SADDLE
			? SoundEvents.ENTITY_PIG_SADDLE
			: super.getEquipSound(slot, stack, equippableComponent);
	}

	/**
	 * При ударе молнией превращается в зомби-пиглина (только не в мирном режиме).
	 * Зомби-пиглин получает снаряжение и становится постоянным (не деспавнится).
	 */
	@Override
	public void onStruckByLightning(ServerWorld world, LightningEntity lightning) {
		if (world.getDifficulty() == Difficulty.PEACEFUL) {
			super.onStruckByLightning(world, lightning);
			return;
		}

		ZombifiedPiglinEntity zombifiedPiglin = convertTo(
			EntityType.ZOMBIFIED_PIGLIN,
			EntityConversionContext.create(this, false, true),
			converted -> {
				converted.initEquipment(getRandom(), world.getLocalDifficulty(getBlockPos()));
				converted.setPersistent();
			}
		);
		if (zombifiedPiglin == null) {
			super.onStruckByLightning(world, lightning);
		}
	}

	@Override
	protected void tickControlled(PlayerEntity controllingPlayer, Vec3d movementInput) {
		super.tickControlled(controllingPlayer, movementInput);
		setRotation(controllingPlayer.getYaw(), controllingPlayer.getPitch() * 0.5F);
		lastYaw = bodyYaw = headYaw = getYaw();
		saddledComponent.tickBoost();
	}

	@Override
	protected Vec3d getControlledMovementInput(PlayerEntity controllingPlayer, Vec3d movementInput) {
		return new Vec3d(0.0, 0.0, 1.0);
	}

	@Override
	protected float getSaddledSpeed(PlayerEntity controllingPlayer) {
		return (float) (getAttributeValue(EntityAttributes.MOVEMENT_SPEED) * 0.225
			* saddledComponent.getMovementSpeedMultiplier());
	}

	@Override
	public boolean consumeOnAStickItem() {
		return saddledComponent.boost(getRandom());
	}

	/**
	 * Создаёт детёныша при размножении. Вариант наследуется случайно от одного из родителей.
	 */
	@Override
	public @Nullable PigEntity createChild(ServerWorld serverWorld, PassiveEntity passiveEntity) {
		PigEntity baby = EntityType.PIG.create(serverWorld, SpawnReason.BREEDING);
		if (baby != null && passiveEntity instanceof PigEntity otherPig) {
			baby.setVariant(random.nextBoolean() ? getVariant() : otherPig.getVariant());
		}

		return baby;
	}

	@Override
	public boolean isBreedingItem(ItemStack stack) {
		return stack.isIn(ItemTags.PIG_FOOD);
	}

	@Override
	public Vec3d getLeashOffset() {
		return new Vec3d(0.0, 0.6F * getStandingEyeHeight(), getWidth() * 0.4F);
	}

	private void setVariant(RegistryEntry<PigVariant> variant) {
		dataTracker.set(VARIANT, variant);
	}

	public RegistryEntry<PigVariant> getVariant() {
		return dataTracker.get(VARIANT);
	}

	@Override
	public <T> @Nullable T get(ComponentType<? extends T> type) {
		return type == DataComponentTypes.PIG_VARIANT
			? castComponentValue((ComponentType<T>) type, getVariant())
			: super.get(type);
	}

	@Override
	protected void copyComponentsFrom(ComponentsAccess from) {
		copyComponentFrom(from, DataComponentTypes.PIG_VARIANT);
		super.copyComponentsFrom(from);
	}

	@Override
	protected <T> boolean setApplicableComponent(ComponentType<T> type, T value) {
		if (type == DataComponentTypes.PIG_VARIANT) {
			setVariant(castComponentValue(DataComponentTypes.PIG_VARIANT, value));
			return true;
		}

		return super.setApplicableComponent(type, value);
	}

	@Override
	public EntityData initialize(
		ServerWorldAccess world,
		LocalDifficulty difficulty,
		SpawnReason spawnReason,
		@Nullable EntityData entityData
	) {
		Variants.select(SpawnContext.of(world, getBlockPos()), RegistryKeys.PIG_VARIANT).ifPresent(this::setVariant);
		return super.initialize(world, difficulty, spawnReason, entityData);
	}
}
