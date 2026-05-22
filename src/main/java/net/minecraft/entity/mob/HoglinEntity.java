package net.minecraft.entity.mob;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Dynamic;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.conversion.EntityConversionContext;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.world.*;
import net.minecraft.world.attribute.EnvironmentAttributes;
import org.jspecify.annotations.Nullable;

/**
 * Хоглин — агрессивное животное Нижнего мира, атакующее игроков и пигменов.
 * При нахождении в Верхнем мире (или в биомах без атрибута зомбификации пиглинов)
 * накапливает {@code timeInOverworld} и через {@value #CONVERSION_TIME} тиков
 * превращается в зоглина. Боится варпед-грибов.
 */
public class HoglinEntity extends AnimalEntity implements Monster, Hoglin {

	private static final TrackedData<Boolean>
			BABY =
			DataTracker.registerData(HoglinEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean>
			IMMUNE_TO_ZOMBIFICATION =
			DataTracker.registerData(HoglinEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final int MAX_HEALTH = 40;
	private static final float MOVEMENT_SPEED = 0.3F;
	private static final int ATTACK_KNOCKBACK = 1;
	private static final float KNOCKBACK_RESISTANCE = 0.6F;
	private static final int ATTACK_DAMAGE = 6;
	private static final float BABY_ATTACK_DAMAGE = 0.5F;
	private static final float BABY_SPAWN_CHANCE = 0.2F;
	private static final int ATTACK_MOVEMENT_COOLDOWN = 10;
	private static final byte ATTACK_STATUS = 4;
	public static final int CONVERSION_TIME = 300;
	private int movementCooldownTicks;
	private int timeInOverworld;
	private boolean cannotBeHunted;
	protected static final ImmutableList<? extends SensorType<? extends Sensor<? super HoglinEntity>>>
			SENSOR_TYPES =
			ImmutableList.of(
					SensorType.NEAREST_LIVING_ENTITIES,
					SensorType.NEAREST_PLAYERS,
					SensorType.NEAREST_ADULT,
					SensorType.HOGLIN_SPECIFIC_SENSOR
			);
	protected static final ImmutableList<? extends MemoryModuleType<?>>
			MEMORY_MODULE_TYPES =
			ImmutableList.<MemoryModuleType<?>>of(
					MemoryModuleType.BREED_TARGET,
					MemoryModuleType.MOBS,
					MemoryModuleType.VISIBLE_MOBS,
					MemoryModuleType.NEAREST_VISIBLE_PLAYER,
					MemoryModuleType.NEAREST_VISIBLE_TARGETABLE_PLAYER,
					MemoryModuleType.LOOK_TARGET,
					MemoryModuleType.WALK_TARGET,
					MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
					MemoryModuleType.PATH,
					MemoryModuleType.ATTACK_TARGET,
					MemoryModuleType.ATTACK_COOLING_DOWN,
					MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLIN,
					MemoryModuleType.AVOID_TARGET,
					MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT,
					MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT,
					MemoryModuleType.NEAREST_VISIBLE_ADULT_HOGLINS,
					MemoryModuleType.NEAREST_VISIBLE_ADULT,
					MemoryModuleType.NEAREST_REPELLENT,
					MemoryModuleType.PACIFIED,
					MemoryModuleType.IS_PANICKING
			);

	public HoglinEntity(EntityType<? extends HoglinEntity> entityType, World world) {
		super(entityType, world);
		experiencePoints = 5;
	}

	@VisibleForTesting
	public void setTimeInOverworld(int timeInOverworld) {
		this.timeInOverworld = timeInOverworld;
	}

	@Override
	public boolean canBeLeashed() {
		return true;
	}

	public static DefaultAttributeContainer.Builder createHoglinAttributes() {
		return HostileEntity.createHostileAttributes()
		                    .add(EntityAttributes.MAX_HEALTH, 40.0)
		                    .add(EntityAttributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
		                    .add(EntityAttributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE)
		                    .add(EntityAttributes.ATTACK_KNOCKBACK, 1.0)
		                    .add(EntityAttributes.ATTACK_DAMAGE, 6.0);
	}

	@Override
	public boolean tryAttack(ServerWorld world, Entity target) {
		if (target instanceof LivingEntity livingEntity) {
			movementCooldownTicks = ATTACK_MOVEMENT_COOLDOWN;
			getEntityWorld().sendEntityStatus(this, ATTACK_STATUS);
			playSound(SoundEvents.ENTITY_HOGLIN_ATTACK);
			HoglinBrain.onAttacking(this, livingEntity);
			return Hoglin.tryAttack(world, this, livingEntity);
		}

		return false;
	}

	@Override
	protected void knockback(LivingEntity target) {
		if (isAdult()) {
			Hoglin.knockback(this, target);
		}
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		boolean damaged = super.damage(world, source, amount);

		if (damaged && source.getAttacker() instanceof LivingEntity attacker) {
			HoglinBrain.onAttacked(world, this, attacker);
		}

		return damaged;
	}

	@Override
	protected Brain.Profile<HoglinEntity> createBrainProfile() {
		return Brain.createProfile(MEMORY_MODULE_TYPES, SENSOR_TYPES);
	}

	@Override
	protected Brain<?> deserializeBrain(Dynamic<?> dynamic) {
		return HoglinBrain.create(createBrainProfile().deserialize(dynamic));
	}

	@Override
	public Brain<HoglinEntity> getBrain() {
		return (Brain<HoglinEntity>) super.getBrain();
	}

	@Override
	protected void mobTick(ServerWorld world) {
		Profiler profiler = Profilers.get();
		profiler.push("hoglinBrain");
		getBrain().tick(world, this);
		profiler.pop();
		HoglinBrain.refreshActivities(this);

		if (canConvert()) {
			timeInOverworld++;
			if (timeInOverworld > CONVERSION_TIME) {
				playSound(SoundEvents.ENTITY_HOGLIN_CONVERTED_TO_ZOMBIFIED);
				zombify();
			}
		} else {
			timeInOverworld = 0;
		}
	}

	@Override
	public void tickMovement() {
		if (movementCooldownTicks > 0) {
			movementCooldownTicks--;
		}

		super.tickMovement();
	}

	@Override
	protected void onGrowUp() {
		if (isBaby()) {
			experiencePoints = 3;
			getAttributeInstance(EntityAttributes.ATTACK_DAMAGE).setBaseValue(BABY_ATTACK_DAMAGE);
		} else {
			experiencePoints = 5;
			getAttributeInstance(EntityAttributes.ATTACK_DAMAGE).setBaseValue(ATTACK_DAMAGE);
		}
	}

	public static boolean canSpawn(
			EntityType<HoglinEntity> type,
			WorldAccess world,
			SpawnReason spawnReason,
			BlockPos pos,
			Random random
	) {
		return !world.getBlockState(pos.down()).isOf(Blocks.NETHER_WART_BLOCK);
	}

	@Override
	public @Nullable EntityData initialize(
			ServerWorldAccess world,
			LocalDifficulty difficulty,
			SpawnReason spawnReason,
			@Nullable EntityData entityData
	) {
		if (world.getRandom().nextFloat() < BABY_SPAWN_CHANCE) {
			setBaby(true);
		}

		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return true;
	}

	@Override
	public float getPathfindingFavor(BlockPos pos, WorldView world) {
		if (HoglinBrain.isWarpedFungusAround(this, pos)) {
			return -1.0F;
		}

		return world.getBlockState(pos.down()).isOf(Blocks.CRIMSON_NYLIUM) ? 10.0F : 0.0F;
	}

	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		ActionResult actionResult = super.interactMob(player, hand);
		if (actionResult.isAccepted()) {
			setPersistent();
		}

		return actionResult;
	}

	@Override
	public void handleStatus(byte status) {
		if (status != ATTACK_STATUS) {
			super.handleStatus(status);
			return;
		}

		movementCooldownTicks = ATTACK_MOVEMENT_COOLDOWN;
		playSound(SoundEvents.ENTITY_HOGLIN_ATTACK);
	}

	@Override
	public int getMovementCooldownTicks() {
		return movementCooldownTicks;
	}

	@Override
	public boolean shouldDropExperience() {
		return true;
	}

	@Override
	protected int getExperienceToDrop(ServerWorld world) {
		return experiencePoints;
	}

	private void zombify() {
		convertTo(
				EntityType.ZOGLIN,
				EntityConversionContext.create(this, true, false),
				zoglin -> zoglin.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 200, 0))
		);
	}

	@Override
	public boolean isBreedingItem(ItemStack stack) {
		return stack.isIn(ItemTags.HOGLIN_FOOD);
	}

	public boolean isAdult() {
		return !isBaby();
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(BABY, false);
		builder.add(IMMUNE_TO_ZOMBIFICATION, false);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putBoolean("IsImmuneToZombification", isImmuneToZombification());
		view.putInt("TimeInOverworld", timeInOverworld);
		view.putBoolean("CannotBeHunted", cannotBeHunted);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		setImmuneToZombification(view.getBoolean("IsImmuneToZombification", false));
		timeInOverworld = view.getInt("TimeInOverworld", 0);
		setCannotBeHunted(view.getBoolean("CannotBeHunted", false));
	}

	public void setImmuneToZombification(boolean immuneToZombification) {
		getDataTracker().set(IMMUNE_TO_ZOMBIFICATION, immuneToZombification);
	}

	private boolean isImmuneToZombification() {
		return getDataTracker().get(IMMUNE_TO_ZOMBIFICATION);
	}

	/**
	 * Возвращает {@code true}, если хоглин находится в Верхнем мире и не имеет иммунитета —
	 * то есть должен начать процесс зомбификации.
	 */
	public boolean canConvert() {
		return !isImmuneToZombification()
				&& !isAiDisabled()
				&& getEntityWorld()
				.getEnvironmentAttributes()
				.getAttributeValue(EnvironmentAttributes.PIGLINS_ZOMBIFY_GAMEPLAY, getEntityPos());
	}

	private void setCannotBeHunted(boolean cannotBeHunted) {
		this.cannotBeHunted = cannotBeHunted;
	}

	public boolean canBeHunted() {
		return isAdult() && !cannotBeHunted;
	}

	@Override
	public @Nullable PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
		HoglinEntity child = EntityType.HOGLIN.create(world, SpawnReason.BREEDING);
		if (child != null) {
			child.setPersistent();
		}

		return child;
	}

	@Override
	public boolean canEat() {
		return !HoglinBrain.isNearPlayer(this) && super.canEat();
	}

	@Override
	public SoundCategory getSoundCategory() {
		return SoundCategory.HOSTILE;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return getEntityWorld().isClient()
				? null
				: HoglinBrain.getSoundEvent(this).orElse(null);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_HOGLIN_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_HOGLIN_DEATH;
	}

	@Override
	protected SoundEvent getSwimSound() {
		return SoundEvents.ENTITY_HOSTILE_SWIM;
	}

	@Override
	protected SoundEvent getSplashSound() {
		return SoundEvents.ENTITY_HOSTILE_SPLASH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.ENTITY_HOGLIN_STEP, 0.15F, 1.0F);
	}

	@Override
	public @Nullable LivingEntity getTarget() {
		return getTargetInBrain();
	}
}
