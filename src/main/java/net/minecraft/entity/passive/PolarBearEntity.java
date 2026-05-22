package net.minecraft.entity.passive;

import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.Biome;
import org.jspecify.annotations.Nullable;

/**
 * Белый медведь — нейтральное существо, атакующее при угрозе детёнышам или при прямой атаке.
 * Реализует интерфейс {@link Angerable} для управления состоянием гнева.
 * При приближении врага встаёт на задние лапы (анимация предупреждения).
 */
public class PolarBearEntity extends AnimalEntity implements Angerable {

	private static final TrackedData<Boolean> WARNING = DataTracker.registerData(
		PolarBearEntity.class, TrackedDataHandlerRegistry.BOOLEAN
	);

	private static final float DEFAULT_ATTACK_DAMAGE = 6.0F;
	private static final float WARNING_ANIMATION_MAX = 6.0F;
	private static final int WARNING_SOUND_COOLDOWN_TICKS = 40;
	private static final int WARNING_ATTACK_COOLDOWN_THRESHOLD = 10;

	private static final UniformIntProvider ANGER_TIME_RANGE = TimeHelper.betweenSeconds(20, 39);

	private float lastWarningAnimationProgress;
	private float warningAnimationProgress;
	private int warningSoundCooldown;
	private long angerEndTime;
	private @Nullable LazyEntityReference<LivingEntity> angryAt;

	public PolarBearEntity(EntityType<? extends PolarBearEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	public @Nullable PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
		return EntityType.POLAR_BEAR.create(world, SpawnReason.BREEDING);
	}

	@Override
	public boolean isBreedingItem(ItemStack stack) {
		return false;
	}

	@Override
	protected void initGoals() {
		super.initGoals();
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(1, new PolarBearEntity.AttackGoal());
		goalSelector.add(1, new EscapeDangerGoal(
			this,
			2.0,
			polarBear -> polarBear.isBaby() ? DamageTypeTags.PANIC_CAUSES : DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES
		));
		goalSelector.add(4, new FollowParentGoal(this, 1.25));
		goalSelector.add(5, new WanderAroundGoal(this, 1.0));
		goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
		goalSelector.add(7, new LookAroundGoal(this));
		targetSelector.add(1, new PolarBearEntity.PolarBearRevengeGoal());
		targetSelector.add(2, new PolarBearEntity.ProtectBabiesGoal());
		targetSelector.add(3, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false, this::shouldAngerAt));
		targetSelector.add(4, new ActiveTargetGoal<>(this, FoxEntity.class, 10, true, true, null));
		targetSelector.add(5, new UniversalAngerGoal<>(this, false));
	}

	public static DefaultAttributeContainer.Builder createPolarBearAttributes() {
		return AnimalEntity.createAnimalAttributes()
			.add(EntityAttributes.MAX_HEALTH, 30.0)
			.add(EntityAttributes.FOLLOW_RANGE, 20.0)
			.add(EntityAttributes.MOVEMENT_SPEED, 0.25)
			.add(EntityAttributes.ATTACK_DAMAGE, DEFAULT_ATTACK_DAMAGE);
	}

	public static boolean canSpawn(
		EntityType<PolarBearEntity> type,
		WorldAccess world,
		SpawnReason spawnReason,
		BlockPos pos,
		Random random
	) {
		RegistryEntry<Biome> biome = world.getBiome(pos);
		return biome.isIn(BiomeTags.POLAR_BEARS_SPAWN_ON_ALTERNATE_BLOCKS)
			? isLightLevelValidForNaturalSpawn(world, pos)
				&& world.getBlockState(pos.down()).isIn(BlockTags.POLAR_BEARS_SPAWNABLE_ON_ALTERNATE)
			: isValidNaturalSpawn(type, world, spawnReason, pos, random);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		readAngerFromData(getEntityWorld(), view);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		writeAngerToData(view);
	}

	@Override
	public void chooseRandomAngerTime() {
		setAngerDuration(ANGER_TIME_RANGE.get(random));
	}

	@Override
	public void setAngerEndTime(long angerEndTime) {
		this.angerEndTime = angerEndTime;
	}

	@Override
	public long getAngerEndTime() {
		return angerEndTime;
	}

	@Override
	public void setAngryAt(@Nullable LazyEntityReference<LivingEntity> angryAt) {
		this.angryAt = angryAt;
	}

	@Override
	public @Nullable LazyEntityReference<LivingEntity> getAngryAt() {
		return angryAt;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return isBaby() ? SoundEvents.ENTITY_POLAR_BEAR_AMBIENT_BABY : SoundEvents.ENTITY_POLAR_BEAR_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_POLAR_BEAR_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_POLAR_BEAR_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.ENTITY_POLAR_BEAR_STEP, 0.15F, 1.0F);
	}

	protected void playWarningSound() {
		if (warningSoundCooldown > 0) {
			return;
		}

		playSound(SoundEvents.ENTITY_POLAR_BEAR_WARNING);
		warningSoundCooldown = WARNING_SOUND_COOLDOWN_TICKS;
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(WARNING, false);
	}

	@Override
	public void tick() {
		super.tick();
		if (getEntityWorld().isClient()) {
			if (warningAnimationProgress != lastWarningAnimationProgress) {
				calculateDimensions();
			}

			lastWarningAnimationProgress = warningAnimationProgress;
			float delta = isWarning() ? 1.0F : -1.0F;
			warningAnimationProgress = MathHelper.clamp(warningAnimationProgress + delta, 0.0F, WARNING_ANIMATION_MAX);
		}

		if (warningSoundCooldown > 0) {
			warningSoundCooldown--;
		}

		if (!getEntityWorld().isClient()) {
			tickAngerLogic((ServerWorld) getEntityWorld(), true);
		}
	}

	@Override
	public EntityDimensions getBaseDimensions(EntityPose pose) {
		if (warningAnimationProgress <= 0.0F) {
			return super.getBaseDimensions(pose);
		}

		float scale = 1.0F + warningAnimationProgress / WARNING_ANIMATION_MAX;
		return super.getBaseDimensions(pose).scaled(1.0F, scale);
	}

	public boolean isWarning() {
		return dataTracker.get(WARNING);
	}

	public void setWarning(boolean warning) {
		dataTracker.set(WARNING, warning);
	}

	public float getWarningAnimationProgress(float tickProgress) {
		return MathHelper.lerp(tickProgress, lastWarningAnimationProgress, warningAnimationProgress) / WARNING_ANIMATION_MAX;
	}

	@Override
	protected float getBaseWaterMovementSpeedMultiplier() {
		return 0.98F;
	}

	@Override
	public EntityData initialize(
		ServerWorldAccess world,
		LocalDifficulty difficulty,
		SpawnReason spawnReason,
		@Nullable EntityData entityData
	) {
		if (entityData == null) {
			entityData = new PassiveEntity.PassiveData(1.0F);
		}

		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	class AttackGoal extends MeleeAttackGoal {

		public AttackGoal() {
			super(PolarBearEntity.this, 1.25, true);
		}

		@Override
		protected void attack(LivingEntity target) {
			float targetWidthSq = (target.getWidth() + 3.0F) * (target.getWidth() + 3.0F);
			if (canAttack(target)) {
				resetCooldown();
				mob.tryAttack(getServerWorld(mob), target);
				PolarBearEntity.this.setWarning(false);
			} else if (mob.squaredDistanceTo(target) < targetWidthSq) {
				if (isCooledDown()) {
					PolarBearEntity.this.setWarning(false);
					resetCooldown();
				}

				if (getCooldown() <= WARNING_ATTACK_COOLDOWN_THRESHOLD) {
					PolarBearEntity.this.setWarning(true);
					PolarBearEntity.this.playWarningSound();
				}
			} else {
				resetCooldown();
				PolarBearEntity.this.setWarning(false);
			}
		}

		@Override
		public void stop() {
			PolarBearEntity.this.setWarning(false);
			super.stop();
		}
	}

	class PolarBearRevengeGoal extends RevengeGoal {

		public PolarBearRevengeGoal() {
			super(PolarBearEntity.this);
		}

		@Override
		public void start() {
			super.start();
			if (PolarBearEntity.this.isBaby()) {
				callSameTypeForRevenge();
				stop();
			}
		}

		@Override
		protected void setMobEntityTarget(MobEntity mob, LivingEntity target) {
			if (mob instanceof PolarBearEntity bear && !bear.isBaby()) {
				super.setMobEntityTarget(mob, target);
			}
		}
	}

	/**
	 * Цель защиты детёнышей: взрослый медведь атакует игроков, если рядом есть медвежата.
	 * Радиус поиска детёнышей — 8×4×8 блоков, дальность преследования вдвое меньше стандартной.
	 */
	class ProtectBabiesGoal extends ActiveTargetGoal<PlayerEntity> {

		public ProtectBabiesGoal() {
			super(PolarBearEntity.this, PlayerEntity.class, 20, true, true, null);
		}

		@Override
		public boolean canStart() {
			if (PolarBearEntity.this.isBaby()) {
				return false;
			}

			if (!super.canStart()) {
				return false;
			}

			for (PolarBearEntity nearby : PolarBearEntity.this.getEntityWorld()
				.getNonSpectatingEntities(
					PolarBearEntity.class,
					PolarBearEntity.this.getBoundingBox().expand(8.0, 4.0, 8.0)
				)
			) {
				if (nearby.isBaby()) {
					return true;
				}
			}

			return false;
		}

		@Override
		protected double getFollowRange() {
			return super.getFollowRange() * 0.5;
		}
	}
}
