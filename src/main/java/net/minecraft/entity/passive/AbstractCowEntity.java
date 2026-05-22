package net.minecraft.entity.passive;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Базовый класс для всех коров (обычная корова, мушрум).
 * Содержит общую логику: цели ИИ, звуки, доение ведром.
 */
public abstract class AbstractCowEntity extends AnimalEntity {

	private static final EntityDimensions BABY_BASE_DIMENSIONS = EntityType.COW
			.getDimensions()
			.scaled(0.5F)
			.withEyeHeight(0.665F);

	public AbstractCowEntity(EntityType<? extends AbstractCowEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(1, new EscapeDangerGoal(this, 2.0));
		goalSelector.add(2, new AnimalMateGoal(this, 1.0));
		goalSelector.add(3, new TemptGoal(this, 1.25, stack -> stack.isIn(ItemTags.COW_FOOD), false));
		goalSelector.add(4, new FollowParentGoal(this, 1.25));
		goalSelector.add(5, new WanderAroundFarGoal(this, 1.0));
		goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
		goalSelector.add(7, new LookAroundGoal(this));
	}

	@Override
	public boolean isBreedingItem(ItemStack stack) {
		return stack.isIn(ItemTags.COW_FOOD);
	}

	public static DefaultAttributeContainer.Builder createCowAttributes() {
		return AnimalEntity.createAnimalAttributes()
				.add(EntityAttributes.MAX_HEALTH, 10.0)
				.add(EntityAttributes.MOVEMENT_SPEED, 0.2F);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_COW_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_COW_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_COW_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.ENTITY_COW_STEP, 0.15F, 1.0F);
	}

	@Override
	protected float getSoundVolume() {
		return 0.4F;
	}

	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		ItemStack held = player.getStackInHand(hand);
		if (held.isOf(Items.BUCKET) && !isBaby()) {
			player.playSound(SoundEvents.ENTITY_COW_MILK, 1.0F, 1.0F);
			ItemStack milkBucket = ItemUsage.exchangeStack(held, player, Items.MILK_BUCKET.getDefaultStack());
			player.setStackInHand(hand, milkBucket);
			return ActionResult.SUCCESS;
		}

		return super.interactMob(player, hand);
	}

	@Override
	public EntityDimensions getBaseDimensions(EntityPose pose) {
		return isBaby() ? BABY_BASE_DIMENSIONS : super.getBaseDimensions(pose);
	}
}
