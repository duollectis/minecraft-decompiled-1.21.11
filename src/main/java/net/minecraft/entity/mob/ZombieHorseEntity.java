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
 * Лошадь-зомби — нежить, которую можно приручить. При естественном спавне
 * автоматически получает всадника-зомби с железным копьём. Не размножается
 * и не ест. Может быть привязана на поводок только если приручена или
 * не управляется мобом.
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
		setPathfindingPenalty(PathNodeType.DANGER_OTHER, -1.0F);
		setPathfindingPenalty(PathNodeType.DAMAGE_OTHER, -1.0F);
	}

	public static DefaultAttributeContainer.Builder createZombieHorseAttributes() {
		return createBaseHorseAttributes().add(EntityAttributes.MAX_HEALTH, 25.0);
	}

	@Override
	public ActionResult interact(PlayerEntity player, Hand hand) {
		setPersistent();
		return super.interact(player, hand);
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return true;
	}

	@Override
	public boolean isControlledByMob() {
		return getFirstPassenger() instanceof MobEntity;
	}

	@Override
	protected void initAttributes(Random random) {
		getAttributeInstance(EntityAttributes.JUMP_STRENGTH).setBaseValue(getBaseJumpStrength(random::nextDouble));
		getAttributeInstance(EntityAttributes.MOVEMENT_SPEED).setBaseValue(getBaseMovementSpeed(random::nextDouble));
	}

	private static double getBaseJumpStrength(DoubleSupplier randomSupplier) {
		return 0.5
				+ randomSupplier.getAsDouble() * JUMP_STRENGTH_VARIANCE
				+ randomSupplier.getAsDouble() * JUMP_STRENGTH_VARIANCE
				+ randomSupplier.getAsDouble() * JUMP_STRENGTH_VARIANCE;
	}

	private static double getBaseMovementSpeed(DoubleSupplier randomSupplier) {
		return (BASE_SPEED_NUMERATOR
				+ randomSupplier.getAsDouble() * SPEED_VARIANCE
				+ randomSupplier.getAsDouble() * SPEED_VARIANCE
				+ randomSupplier.getAsDouble() * SPEED_VARIANCE
		) / SPEED_DIVISOR;
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
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(3, new TemptGoal(this, 1.25, stack -> stack.isIn(ItemTags.ZOMBIE_HORSE_FOOD), false));
	}

	@Override
	public @Nullable EntityData initialize(
			ServerWorldAccess world,
			LocalDifficulty difficulty,
			SpawnReason spawnReason,
			@Nullable EntityData entityData
	) {
		if (spawnReason == SpawnReason.NATURAL) {
			ZombieEntity zombie = EntityType.ZOMBIE.create(getEntityWorld(), SpawnReason.JOCKEY);

			if (zombie != null) {
				zombie.refreshPositionAndAngles(getX(), getY(), getZ(), getYaw(), 0.0F);
				zombie.initialize(world, difficulty, spawnReason, null);
				zombie.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SPEAR));
				zombie.startRiding(this, false, false);
			}
		}

		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		boolean shouldOpenInventory = !isBaby() && isTame() && player.shouldCancelInteraction();
		if (hasPassengers() || shouldOpenInventory) {
			return super.interactMob(player, hand);
		}

		ItemStack heldStack = player.getStackInHand(hand);
		if (!heldStack.isEmpty()) {
			if (isBreedingItem(heldStack)) {
				return interactHorse(player, heldStack);
			}

			if (!isTame()) {
				playAngrySound();
				return ActionResult.SUCCESS;
			}
		}

		return super.interactMob(player, hand);
	}

	@Override
	public boolean canUseSlot(EquipmentSlot slot) {
		return true;
	}

	@Override
	public boolean canBeLeashed() {
		return isTame() || !isControlledByMob();
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
		return isBaby() ? BABY_BASE_DIMENSIONS : super.getBaseDimensions(pose);
	}

	@Override
	public float getRiderChargingSpeedMultiplier() {
		return 1.4F;
	}
}
