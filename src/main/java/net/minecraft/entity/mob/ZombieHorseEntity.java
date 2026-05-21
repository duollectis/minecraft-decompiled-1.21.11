package net.minecraft.entity.mob;

import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.function.DoubleSupplier;

/**
 * {@code ZombieHorseEntity}.
 */
public class ZombieHorseEntity extends AbstractHorseEntity {

	private static final float SPEED_DIVISOR = 42.16F;
	private static final double BASE_JUMP_STRENGTH = 0.5;
	private static final double JUMP_STRENGTH_VARIANCE = 0.06666666666666667;
	private static final double BASE_SPEED_NUMERATOR = 9.0;
	private static final double SPEED_VARIANCE = 1.0;
	private static final EntityDimensions BABY_BASE_DIMENSIONS = EntityType.ZOMBIE_HORSE
			.getDimensions()
			.withAttachments(EntityAttachments
					.builder()
					.add(EntityAttachmentType.PASSENGER, 0.0F, EntityType.ZOMBIE_HORSE.getHeight() - 0.03125F, 0.0F))
			.scaled(0.5F);

	public ZombieHorseEntity(EntityType<? extends ZombieHorseEntity> entityType, World world) {
		super(entityType, world);
		this.setPathfindingPenalty(PathNodeType.DANGER_OTHER, -1.0F);
		this.setPathfindingPenalty(PathNodeType.DAMAGE_OTHER, -1.0F);
	}

	public static DefaultAttributeContainer.Builder createZombieHorseAttributes() {
		return createBaseHorseAttributes().add(EntityAttributes.MAX_HEALTH, 25.0);
	}

	@Override
	public ActionResult interact(PlayerEntity player, Hand hand) {
		this.setPersistent();
		return super.interact(player, hand);
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return true;
	}

	@Override
	public boolean isControlledByMob() {
		return this.getFirstPassenger() instanceof MobEntity;
	}

	@Override
	protected void initAttributes(Random random) {
		this.getAttributeInstance(EntityAttributes.JUMP_STRENGTH).setBaseValue(getBaseJumpStrength(random::nextDouble));
		this
				.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED)
				.setBaseValue(getBaseMovementSpeed(random::nextDouble));
	}

	private static double getBaseJumpStrength(DoubleSupplier randomSupplier) {
		return 0.5
				+ randomSupplier.getAsDouble() * 0.06666666666666667
				+ randomSupplier.getAsDouble() * 0.06666666666666667
				+ randomSupplier.getAsDouble() * 0.06666666666666667;
	}

	private static double getBaseMovementSpeed(DoubleSupplier randomSupplier) {
		return (9.0 + randomSupplier.getAsDouble() * 1.0 + randomSupplier.getAsDouble() * 1.0
				+ randomSupplier.getAsDouble() * 1.0
		) / 42.16F;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_ZOMBIE_HORSE_AMBIENT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_ZOMBIE_HORSE_DEATH;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_ZOMBIE_HORSE_HURT;
	}

	@Override
	protected SoundEvent getAngrySound() {
		return SoundEvents.ENTITY_ZOMBIE_HORSE_ANGRY;
	}

	@Override
	protected SoundEvent getEatSound() {
		return SoundEvents.ENTITY_ZOMBIE_HORSE_EAT;
	}

	@Override
	public @Nullable PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
		return null;
	}

	@Override
	public boolean canEat() {
		return false;
	}

	@Override
	protected void initCustomGoals() {
		this.goalSelector.add(0, new SwimGoal(this));
		this.goalSelector.add(3, new TemptGoal(this, 1.25, stack -> stack.isIn(ItemTags.ZOMBIE_HORSE_FOOD), false));
	}

	@Override
	public @Nullable EntityData initialize(
			ServerWorldAccess world,
			LocalDifficulty difficulty,
			SpawnReason spawnReason,
			@Nullable EntityData entityData
	) {
		if (spawnReason == SpawnReason.NATURAL) {
			ZombieEntity zombieEntity = EntityType.ZOMBIE.create(this.getEntityWorld(), SpawnReason.JOCKEY);
			if (zombieEntity != null) {
				zombieEntity.refreshPositionAndAngles(this.getX(), this.getY(), this.getZ(), this.getYaw(), 0.0F);
				zombieEntity.initialize(world, difficulty, spawnReason, null);
				zombieEntity.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SPEAR));
				zombieEntity.startRiding(this, false, false);
			}
		}

		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		boolean bl = !this.isBaby() && this.isTame() && player.shouldCancelInteraction();
		if (!this.hasPassengers() && !bl) {
			ItemStack itemStack = player.getStackInHand(hand);
			if (!itemStack.isEmpty()) {
				if (this.isBreedingItem(itemStack)) {
					return this.interactHorse(player, itemStack);
				}

				if (!this.isTame()) {
					this.playAngrySound();
					return ActionResult.SUCCESS;
				}
			}

			return super.interactMob(player, hand);
		}
		else {
			return super.interactMob(player, hand);
		}
	}

	@Override
	public boolean canUseSlot(EquipmentSlot slot) {
		return true;
	}

	@Override
	public boolean canBeLeashed() {
		return this.isTame() || !this.isControlledByMob();
	}

	@Override
	public boolean isBreedingItem(ItemStack stack) {
		return stack.isIn(ItemTags.ZOMBIE_HORSE_FOOD);
	}

	@Override
	protected EquipmentSlot getDaylightProtectionSlot() {
		return EquipmentSlot.BODY;
	}

	@Override
	public Vec3d[] getQuadLeashOffsets() {
		return Leashable.createQuadLeashOffsets(this, 0.04, 0.41, 0.18, 0.73);
	}

	@Override
	public EntityDimensions getBaseDimensions(EntityPose pose) {
		return this.isBaby() ? BABY_BASE_DIMENSIONS : super.getBaseDimensions(pose);
	}

	@Override
	public float getRiderChargingSpeedMultiplier() {
		return 1.4F;
	}
}
