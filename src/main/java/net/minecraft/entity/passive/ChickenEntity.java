package net.minecraft.entity.passive;

import net.minecraft.block.BlockState;
import net.minecraft.component.ComponentType;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.spawn.SpawnContext;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTables;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.LazyRegistryEntryReference;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Курица — животное, периодически откладывающее яйца.
 * Поддерживает анимацию взмахов крыльями и замедление падения.
 * Если на курице сидит джокей (зомби-ребёнок), она деспавнится вместе с ним.
 */
public class ChickenEntity extends AnimalEntity {

	private static final EntityDimensions BABY_BASE_DIMENSIONS = EntityType.CHICKEN
		.getDimensions()
		.scaled(0.5F)
		.withEyeHeight(0.2975F);

	private static final TrackedData<RegistryEntry<ChickenVariant>> VARIANT = DataTracker.registerData(
		ChickenEntity.class,
		TrackedDataHandlerRegistry.CHICKEN_VARIANT
	);

	/** Минимальное время между кладками яиц (в тиках). */
	private static final int EGG_LAY_MIN_TICKS = 6000;
	/** Случайный разброс времени кладки яиц (в тиках). */
	private static final int EGG_LAY_RANDOM_TICKS = 6000;
	/** Опыт за убийство курицы-джокея. */
	private static final int JOCKEY_EXPERIENCE = 10;

	public float flapProgress;
	public float maxWingDeviation;
	public float lastMaxWingDeviation;
	public float lastFlapProgress;
	public float flapSpeed = 1.0F;
	private float wingFlapSpeedThreshold = 1.0F;
	public int eggLayTime;
	public boolean hasJockey = false;

	public ChickenEntity(EntityType<? extends ChickenEntity> entityType, World world) {
		super(entityType, world);
		eggLayTime = random.nextInt(EGG_LAY_RANDOM_TICKS) + EGG_LAY_MIN_TICKS;
		setPathfindingPenalty(PathNodeType.WATER, 0.0F);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(1, new EscapeDangerGoal(this, 1.4));
		goalSelector.add(2, new AnimalMateGoal(this, 1.0));
		goalSelector.add(3, new TemptGoal(this, 1.0, stack -> stack.isIn(ItemTags.CHICKEN_FOOD), false));
		goalSelector.add(4, new FollowParentGoal(this, 1.1));
		goalSelector.add(5, new WanderAroundFarGoal(this, 1.0));
		goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
		goalSelector.add(7, new LookAroundGoal(this));
	}

	@Override
	public EntityDimensions getBaseDimensions(EntityPose pose) {
		return isBaby() ? BABY_BASE_DIMENSIONS : super.getBaseDimensions(pose);
	}

	public static DefaultAttributeContainer.Builder createChickenAttributes() {
		return AnimalEntity.createAnimalAttributes()
			.add(EntityAttributes.MAX_HEALTH, 4.0)
			.add(EntityAttributes.MOVEMENT_SPEED, 0.25);
	}

	@Override
	public void tickMovement() {
		super.tickMovement();
		lastFlapProgress = flapProgress;
		lastMaxWingDeviation = maxWingDeviation;
		maxWingDeviation += (isOnGround() ? -1.0F : 4.0F) * 0.3F;
		maxWingDeviation = MathHelper.clamp(maxWingDeviation, 0.0F, 1.0F);
		if (!isOnGround() && flapSpeed < 1.0F) {
			flapSpeed = 1.0F;
		}

		flapSpeed *= 0.9F;
		Vec3d velocity = getVelocity();
		if (!isOnGround() && velocity.y < 0.0) {
			setVelocity(velocity.multiply(1.0, 0.6, 1.0));
		}

		flapProgress += flapSpeed * 2.0F;

		if (getEntityWorld() instanceof ServerWorld serverWorld && isAlive() && !isBaby() && !hasJockey() && --eggLayTime <= 0) {
			if (forEachGiftedItem(serverWorld, LootTables.CHICKEN_LAY_GAMEPLAY, this::dropStack)) {
				playSound(
					SoundEvents.ENTITY_CHICKEN_EGG,
					1.0F,
					(random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F
				);
				emitGameEvent(GameEvent.ENTITY_PLACE);
			}

			eggLayTime = random.nextInt(EGG_LAY_RANDOM_TICKS) + EGG_LAY_MIN_TICKS;
		}
	}

	@Override
	protected boolean isFlappingWings() {
		return speed > wingFlapSpeedThreshold;
	}

	@Override
	protected void addFlapEffects() {
		wingFlapSpeedThreshold = speed + maxWingDeviation / 2.0F;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_CHICKEN_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_CHICKEN_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_CHICKEN_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.ENTITY_CHICKEN_STEP, 0.15F, 1.0F);
	}

	/**
	 * Создаёт детёныша при размножении. Вариант наследуется случайно от одного из родителей.
	 */
	@Override
	public @Nullable ChickenEntity createChild(ServerWorld serverWorld, PassiveEntity passiveEntity) {
		ChickenEntity baby = EntityType.CHICKEN.create(serverWorld, SpawnReason.BREEDING);
		if (baby != null && passiveEntity instanceof ChickenEntity otherChicken) {
			baby.setVariant(random.nextBoolean() ? getVariant() : otherChicken.getVariant());
		}

		return baby;
	}

	@Override
	public EntityData initialize(
		ServerWorldAccess world,
		LocalDifficulty difficulty,
		SpawnReason spawnReason,
		@Nullable EntityData entityData
	) {
		Variants.select(SpawnContext.of(world, getBlockPos()), RegistryKeys.CHICKEN_VARIANT).ifPresent(this::setVariant);
		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	@Override
	public boolean isBreedingItem(ItemStack stack) {
		return stack.isIn(ItemTags.CHICKEN_FOOD);
	}

	@Override
	protected int getExperienceToDrop(ServerWorld world) {
		return hasJockey() ? JOCKEY_EXPERIENCE : super.getExperienceToDrop(world);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(VARIANT, Variants.getOrDefaultOrThrow(getRegistryManager(), ChickenVariants.TEMPERATE));
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		hasJockey = view.getBoolean("IsChickenJockey", false);
		view.getOptionalInt("EggLayTime").ifPresent(time -> eggLayTime = time);
		Variants.fromData(view, RegistryKeys.CHICKEN_VARIANT).ifPresent(this::setVariant);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putBoolean("IsChickenJockey", hasJockey);
		view.putInt("EggLayTime", eggLayTime);
		Variants.writeData(view, getVariant());
	}

	public void setVariant(RegistryEntry<ChickenVariant> variant) {
		dataTracker.set(VARIANT, variant);
	}

	public RegistryEntry<ChickenVariant> getVariant() {
		return dataTracker.get(VARIANT);
	}

	@Override
	public <T> @Nullable T get(ComponentType<? extends T> type) {
		return type == DataComponentTypes.CHICKEN_VARIANT
			? castComponentValue((ComponentType<T>) type, new LazyRegistryEntryReference<>(getVariant()))
			: super.get(type);
	}

	@Override
	protected void copyComponentsFrom(ComponentsAccess from) {
		copyComponentFrom(from, DataComponentTypes.CHICKEN_VARIANT);
		super.copyComponentsFrom(from);
	}

	@Override
	protected <T> boolean setApplicableComponent(ComponentType<T> type, T value) {
		if (type == DataComponentTypes.CHICKEN_VARIANT) {
			Optional<RegistryEntry<ChickenVariant>> resolved = castComponentValue(DataComponentTypes.CHICKEN_VARIANT, value)
				.resolveEntry(getRegistryManager());
			if (resolved.isPresent()) {
				setVariant(resolved.get());
				return true;
			}

			return false;
		}

		return super.setApplicableComponent(type, value);
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return hasJockey();
	}

	@Override
	protected void updatePassengerPosition(Entity passenger, Entity.PositionUpdater positionUpdater) {
		super.updatePassengerPosition(passenger, positionUpdater);
		if (passenger instanceof LivingEntity livingPassenger) {
			livingPassenger.bodyYaw = bodyYaw;
		}
	}

	public boolean hasJockey() {
		return hasJockey;
	}

	public void setHasJockey(boolean hasJockey) {
		this.hasJockey = hasJockey;
	}
}
