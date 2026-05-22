package net.minecraft.entity.mob;

import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.SkeletonHorseTrapTriggerGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jspecify.annotations.Nullable;

/**
 * Лошадь-скелет — нежить, появляющаяся при ударе молнии в «ловушку» (trapped).
 * В режиме ловушки активирует {@link SkeletonHorseTrapTriggerGoal} и автоматически
 * исчезает через {@value #DESPAWN_AGE} тиков. Умеет плавать с особыми звуками галопа.
 */
public class SkeletonHorseEntity extends AbstractHorseEntity {

	private final SkeletonHorseTrapTriggerGoal trapTriggerGoal = new SkeletonHorseTrapTriggerGoal(this);
	private static final int DESPAWN_AGE = 18000;
	private static final EntityDimensions BABY_BASE_DIMENSIONS = EntityType.SKELETON_HORSE
			.getDimensions()
			.withAttachments(EntityAttachments
					.builder()
					.add(EntityAttachmentType.PASSENGER, 0.0F, EntityType.SKELETON_HORSE.getHeight() - 0.03125F, 0.0F))
			.scaled(0.5F);
	private boolean trapped;
	private int trapTime;

	public SkeletonHorseEntity(EntityType<? extends SkeletonHorseEntity> entityType, World world) {
		super(entityType, world);
	}

	public static DefaultAttributeContainer.Builder createSkeletonHorseAttributes() {
		return createBaseHorseAttributes()
				.add(EntityAttributes.MAX_HEALTH, 15.0)
				.add(EntityAttributes.MOVEMENT_SPEED, 0.2F);
	}

	public static boolean canSpawn(
			EntityType<? extends AnimalEntity> type,
			WorldAccess world,
			SpawnReason reason,
			BlockPos pos,
			Random random
	) {
		return !SpawnReason.isAnySpawner(reason)
				? AnimalEntity.isValidNaturalSpawn(type, world, reason, pos, random)
				: SpawnReason.isTrialSpawner(reason) || isLightLevelValidForNaturalSpawn(world, pos);
	}

	@Override
	protected void initAttributes(Random random) {
		getAttributeInstance(EntityAttributes.JUMP_STRENGTH).setBaseValue(getChildJumpStrengthBonus(random::nextDouble));
	}

	@Override
	protected void initCustomGoals() {
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return isSubmergedIn(FluidTags.WATER)
				? SoundEvents.ENTITY_SKELETON_HORSE_AMBIENT_WATER
				: SoundEvents.ENTITY_SKELETON_HORSE_AMBIENT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_SKELETON_HORSE_DEATH;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_SKELETON_HORSE_HURT;
	}

	@Override
	protected SoundEvent getSwimSound() {
		if (!isOnGround()) {
			return SoundEvents.ENTITY_SKELETON_HORSE_SWIM;
		}

		if (!hasPassengers()) {
			return SoundEvents.ENTITY_SKELETON_HORSE_STEP_WATER;
		}

		soundTicks++;
		if (soundTicks > 5 && soundTicks % 3 == 0) {
			return SoundEvents.ENTITY_SKELETON_HORSE_GALLOP_WATER;
		}

		return SoundEvents.ENTITY_SKELETON_HORSE_STEP_WATER;
	}

	@Override
	protected void playSwimSound(float volume) {
		if (isOnGround()) {
			super.playSwimSound(0.3F);
		}
		else {
			super.playSwimSound(Math.min(0.1F, volume * 25.0F));
		}
	}

	@Override
	protected void playJumpSound() {
		if (isTouchingWater()) {
			playSound(SoundEvents.ENTITY_SKELETON_HORSE_JUMP_WATER, 0.4F, 1.0F);
		}
		else {
			super.playJumpSound();
		}
	}

	@Override
	public EntityDimensions getBaseDimensions(EntityPose pose) {
		return isBaby() ? BABY_BASE_DIMENSIONS : super.getBaseDimensions(pose);
	}

	@Override
	public void tickMovement() {
		super.tickMovement();
		if (isTrapped() && trapTime++ >= DESPAWN_AGE) {
			discard();
		}
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putBoolean("SkeletonTrap", isTrapped());
		view.putInt("SkeletonTrapTime", trapTime);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		setTrapped(view.getBoolean("SkeletonTrap", false));
		trapTime = view.getInt("SkeletonTrapTime", 0);
	}

	@Override
	protected float getBaseWaterMovementSpeedMultiplier() {
		return 0.96F;
	}

	public boolean isTrapped() {
		return trapped;
	}

	public void setTrapped(boolean trapped) {
		if (trapped == this.trapped) {
			return;
		}

		this.trapped = trapped;
		if (trapped) {
			goalSelector.add(1, trapTriggerGoal);
		}
		else {
			goalSelector.remove(trapTriggerGoal);
		}
	}

	@Override
	public @Nullable PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
		return EntityType.SKELETON_HORSE.create(world, SpawnReason.BREEDING);
	}

	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		return isTame() ? super.interactMob(player, hand) : ActionResult.PASS;
	}

	@Override
	public boolean canUseSlot(EquipmentSlot slot) {
		return true;
	}
}
