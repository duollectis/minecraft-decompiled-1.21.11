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
 * {@code PigEntity}.
 */
public class PigEntity extends AnimalEntity implements ItemSteerable {

	private static final TrackedData<Integer>
			BOOST_TIME =
			DataTracker.registerData(PigEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<RegistryEntry<PigVariant>>
			VARIANT =
			DataTracker.registerData(PigEntity.class, TrackedDataHandlerRegistry.PIG_VARIANT);
	private final SaddledComponent saddledComponent = new SaddledComponent(this.dataTracker, BOOST_TIME);

	public PigEntity(EntityType<? extends PigEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(0, new SwimGoal(this));
		this.goalSelector.add(1, new EscapeDangerGoal(this, 1.25));
		this.goalSelector.add(3, new AnimalMateGoal(this, 1.0));
		this.goalSelector.add(4, new TemptGoal(this, 1.2, stack -> stack.isOf(Items.CARROT_ON_A_STICK), false));
		this.goalSelector.add(4, new TemptGoal(this, 1.2, stack -> stack.isIn(ItemTags.PIG_FOOD), false));
		this.goalSelector.add(5, new FollowParentGoal(this, 1.1));
		this.goalSelector.add(6, new WanderAroundFarGoal(this, 1.0));
		this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
		this.goalSelector.add(8, new LookAroundGoal(this));
	}

	public static DefaultAttributeContainer.Builder createPigAttributes() {
		return AnimalEntity
				.createAnimalAttributes()
				.add(EntityAttributes.MAX_HEALTH, 10.0)
				.add(EntityAttributes.MOVEMENT_SPEED, 0.25);
	}

	@Override
	public @Nullable LivingEntity getControllingPassenger() {
		return (LivingEntity) (this.hasSaddleEquipped()
				                       && this.getFirstPassenger() instanceof PlayerEntity playerEntity
				                       && playerEntity.isHolding(Items.CARROT_ON_A_STICK)
		                       ? playerEntity
		                       : super.getControllingPassenger()
		);
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		if (BOOST_TIME.equals(data) && this.getEntityWorld().isClient()) {
			this.saddledComponent.boost();
		}

		super.onTrackedDataSet(data);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(BOOST_TIME, 0);
		builder.add(VARIANT, Variants.getOrDefaultOrThrow(this.getRegistryManager(), PigVariants.DEFAULT));
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		Variants.writeData(view, this.getVariant());
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
		this.playSound(SoundEvents.ENTITY_PIG_STEP, 0.15F, 1.0F);
	}

	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		boolean bl = this.isBreedingItem(player.getStackInHand(hand));
		if (!bl && this.hasSaddleEquipped() && !this.hasPassengers() && !player.shouldCancelInteraction()) {
			if (!this.getEntityWorld().isClient()) {
				player.startRiding(this);
			}

			return ActionResult.SUCCESS;
		}
		else {
			ActionResult actionResult = super.interactMob(player, hand);
			if (!actionResult.isAccepted()) {
				ItemStack itemStack = player.getStackInHand(hand);
				return (ActionResult) (this.canEquip(itemStack, EquipmentSlot.SADDLE) ? itemStack.useOnEntity(
						player,
						this,
						hand
				) : ActionResult.PASS
				);
			}
			else {
				return actionResult;
			}
		}
	}

	@Override
	public boolean canUseSlot(EquipmentSlot slot) {
		return slot != EquipmentSlot.SADDLE ? super.canUseSlot(slot) : this.isAlive() && !this.isBaby();
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
		return (RegistryEntry<SoundEvent>) (slot == EquipmentSlot.SADDLE ? SoundEvents.ENTITY_PIG_SADDLE
		                                                                 : super.getEquipSound(
				                                                                 slot,
				                                                                 stack,
				                                                                 equippableComponent
		                                                                 )
		);
	}

	@Override
	public void onStruckByLightning(ServerWorld world, LightningEntity lightning) {
		if (world.getDifficulty() != Difficulty.PEACEFUL) {
			ZombifiedPiglinEntity zombifiedPiglinEntity = this.convertTo(
					EntityType.ZOMBIFIED_PIGLIN,
					EntityConversionContext.create(this, false, true),
					zombifiedPiglinEntityx -> {
						zombifiedPiglinEntityx.initEquipment(
								this.getRandom(),
								world.getLocalDifficulty(this.getBlockPos())
						);
						zombifiedPiglinEntityx.setPersistent();
					}
			);
			if (zombifiedPiglinEntity == null) {
				super.onStruckByLightning(world, lightning);
			}
		}
		else {
			super.onStruckByLightning(world, lightning);
		}
	}

	@Override
	protected void tickControlled(PlayerEntity controllingPlayer, Vec3d movementInput) {
		super.tickControlled(controllingPlayer, movementInput);
		this.setRotation(controllingPlayer.getYaw(), controllingPlayer.getPitch() * 0.5F);
		this.lastYaw = this.bodyYaw = this.headYaw = this.getYaw();
		this.saddledComponent.tickBoost();
	}

	@Override
	protected Vec3d getControlledMovementInput(PlayerEntity controllingPlayer, Vec3d movementInput) {
		return new Vec3d(0.0, 0.0, 1.0);
	}

	@Override
	protected float getSaddledSpeed(PlayerEntity controllingPlayer) {
		return (float) (this.getAttributeValue(EntityAttributes.MOVEMENT_SPEED) * 0.225
				* this.saddledComponent.getMovementSpeedMultiplier()
		);
	}

	@Override
	public boolean consumeOnAStickItem() {
		return this.saddledComponent.boost(this.getRandom());
	}

	public @Nullable PigEntity createChild(ServerWorld serverWorld, PassiveEntity passiveEntity) {
		PigEntity pigEntity = EntityType.PIG.create(serverWorld, SpawnReason.BREEDING);
		if (pigEntity != null && passiveEntity instanceof PigEntity pigEntity2) {
			pigEntity.setVariant(this.random.nextBoolean() ? this.getVariant() : pigEntity2.getVariant());
		}

		return pigEntity;
	}

	@Override
	public boolean isBreedingItem(ItemStack stack) {
		return stack.isIn(ItemTags.PIG_FOOD);
	}

	@Override
	public Vec3d getLeashOffset() {
		return new Vec3d(0.0, 0.6F * this.getStandingEyeHeight(), this.getWidth() * 0.4F);
	}

	private void setVariant(RegistryEntry<PigVariant> variant) {
		this.dataTracker.set(VARIANT, variant);
	}

	public RegistryEntry<PigVariant> getVariant() {
		return this.dataTracker.get(VARIANT);
	}

	@Override
	public <T> @Nullable T get(ComponentType<? extends T> type) {
		return type == DataComponentTypes.PIG_VARIANT ? castComponentValue((ComponentType<T>) type, this.getVariant())
		                                              : super.get(type);
	}

	@Override
	protected void copyComponentsFrom(ComponentsAccess from) {
		this.copyComponentFrom(from, DataComponentTypes.PIG_VARIANT);
		super.copyComponentsFrom(from);
	}

	@Override
	protected <T> boolean setApplicableComponent(ComponentType<T> type, T value) {
		if (type == DataComponentTypes.PIG_VARIANT) {
			this.setVariant(castComponentValue(DataComponentTypes.PIG_VARIANT, value));
			return true;
		}
		else {
			return super.setApplicableComponent(type, value);
		}
	}

	@Override
	public EntityData initialize(
			ServerWorldAccess world,
			LocalDifficulty difficulty,
			SpawnReason spawnReason,
			@Nullable EntityData entityData
	) {
		Variants
				.select(SpawnContext.of(world, this.getBlockPos()), RegistryKeys.PIG_VARIANT)
				.ifPresent(this::setVariant);
		return super.initialize(world, difficulty, spawnReason, entityData);
	}
}
