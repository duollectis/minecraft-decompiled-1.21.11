package net.minecraft.entity.mob;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.Shearable;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTables;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jspecify.annotations.Nullable;

/**
 * Болотный скелет — вариант скелета, стреляющий отравленными стрелами.
 */
public class BoggedEntity extends AbstractSkeletonEntity implements Shearable {

	private static final int POISON_ARROW_DURATION = 100;
	private static final int HARD_ATTACK_INTERVAL = 50;
	private static final int REGULAR_ATTACK_INTERVAL = 70;
	private static final String SHEARED_KEY = "sheared";
	private static final TrackedData<Boolean>
			SHEARED =
			DataTracker.registerData(BoggedEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

	public static DefaultAttributeContainer.Builder createBoggedAttributes() {
		return AbstractSkeletonEntity.createAbstractSkeletonAttributes().add(EntityAttributes.MAX_HEALTH, 16.0);
	}

	public BoggedEntity(EntityType<? extends BoggedEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(SHEARED, false);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putBoolean(SHEARED_KEY, isSheared());
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		setSheared(view.getBoolean(SHEARED_KEY, false));
	}

	public boolean isSheared() {
		return dataTracker.get(SHEARED);
	}

	public void setSheared(boolean sheared) {
		dataTracker.set(SHEARED, sheared);
	}

	@Override
	protected ActionResult interactMob(PlayerEntity player, Hand hand) {
		ItemStack itemStack = player.getStackInHand(hand);
		if (!itemStack.isOf(Items.SHEARS) || !isShearable()) {
			return super.interactMob(player, hand);
		}

		if (getEntityWorld() instanceof ServerWorld serverWorld) {
			sheared(serverWorld, SoundCategory.PLAYERS, itemStack);
			emitGameEvent(GameEvent.SHEAR, player);
			itemStack.damage(1, player, hand.getEquipmentSlot());
		}

		return ActionResult.SUCCESS;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_BOGGED_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_BOGGED_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_BOGGED_DEATH;
	}

	@Override
	protected SoundEvent getStepSound() {
		return SoundEvents.ENTITY_BOGGED_STEP;
	}

	@Override
	protected PersistentProjectileEntity createArrowProjectile(
			ItemStack arrow,
			float damageModifier,
			@Nullable ItemStack shotFrom
	) {
		PersistentProjectileEntity arrow2 = super.createArrowProjectile(arrow, damageModifier, shotFrom);
		if (arrow2 instanceof ArrowEntity arrowEntity) {
			arrowEntity.addEffect(new StatusEffectInstance(StatusEffects.POISON, POISON_ARROW_DURATION));
		}

		return arrow2;
	}

	@Override
	protected int getHardAttackInterval() {
		return HARD_ATTACK_INTERVAL;
	}

	@Override
	protected int getRegularAttackInterval() {
		return REGULAR_ATTACK_INTERVAL;
	}

	@Override
	public void sheared(ServerWorld world, SoundCategory shearedSoundCategory, ItemStack shears) {
		world.playSoundFromEntity(null, this, SoundEvents.ENTITY_BOGGED_SHEAR, shearedSoundCategory, 1.0F, 1.0F);
		dropShearedItems(world, shears);
		setSheared(true);
	}

	private void dropShearedItems(ServerWorld world, ItemStack shears) {
		forEachShearedItem(
				world,
				LootTables.BOGGED_SHEARING,
				shears,
				(serverWorld, stack) -> dropStack(serverWorld, stack, getHeight())
		);
	}

	@Override
	public boolean isShearable() {
		return !isSheared() && isAlive();
	}
}
