package net.minecraft.entity.mob;

import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Скелет иссушителя — мощный скелет из Нижнего мира с мечом.
 */
public class WitherSkeletonEntity extends AbstractSkeletonEntity {

	private static final int WITHER_EFFECT_DURATION = 200;

	public WitherSkeletonEntity(EntityType<? extends WitherSkeletonEntity> entityType, World world) {
		super(entityType, world);
		setPathfindingPenalty(PathNodeType.LAVA, 8.0F);
	}

	@Override
	protected void initGoals() {
		targetSelector.add(3, new ActiveTargetGoal<>(this, AbstractPiglinEntity.class, true));
		super.initGoals();
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_WITHER_SKELETON_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_WITHER_SKELETON_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_WITHER_SKELETON_DEATH;
	}

	@Override
	SoundEvent getStepSound() {
		return SoundEvents.ENTITY_WITHER_SKELETON_STEP;
	}

	@Override
	public TagKey<Item> getPreferredWeapons() {
		return null;
	}

	@Override
	public boolean canPickupItem(ItemStack stack) {
		return !stack.isIn(ItemTags.WITHER_SKELETON_DISLIKED_WEAPONS) && super.canPickupItem(stack);
	}

	@Override
	protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
		equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
	}

	@Override
	protected void updateEnchantments(ServerWorldAccess world, Random random, LocalDifficulty localDifficulty) {
	}

	@Override
	public @Nullable EntityData initialize(
			ServerWorldAccess world,
			LocalDifficulty difficulty,
			SpawnReason spawnReason,
			@Nullable EntityData entityData
	) {
		EntityData result = super.initialize(world, difficulty, spawnReason, entityData);
		getAttributeInstance(EntityAttributes.ATTACK_DAMAGE).setBaseValue(4.0);
		updateAttackType();
		return result;
	}

	@Override
	public boolean tryAttack(ServerWorld world, Entity target) {
		if (!super.tryAttack(world, target)) {
			return false;
		}

		if (target instanceof LivingEntity livingTarget) {
			livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, WITHER_EFFECT_DURATION), this);
		}

		return true;
	}

	@Override
	protected PersistentProjectileEntity createArrowProjectile(
			ItemStack arrow,
			float damageModifier,
			@Nullable ItemStack shotFrom
	) {
		PersistentProjectileEntity
				persistentProjectileEntity =
				super.createArrowProjectile(arrow, damageModifier, shotFrom);
		persistentProjectileEntity.setOnFireFor(100.0F);
		return persistentProjectileEntity;
	}

	@Override
	public boolean canHaveStatusEffect(StatusEffectInstance effect) {
		if (effect.equals(StatusEffects.WITHER)) {
			return false;
		}

		return super.canHaveStatusEffect(effect);
	}
}
