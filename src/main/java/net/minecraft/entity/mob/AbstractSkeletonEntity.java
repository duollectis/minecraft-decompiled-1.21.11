package net.minecraft.entity.mob;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.util.Holidays;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Базовый класс для скелетов. Реализует стрельбу из лука и конвертацию при попадании молнии.
 */
public abstract class AbstractSkeletonEntity extends HostileEntity implements RangedAttackMob {

	private static final int HARD_ATTACK_INTERVAL = 20;
	private static final int REGULAR_ATTACK_INTERVAL = 40;
	protected static final int VARIANT_HARD_ATTACK_INTERVAL = 50;
	protected static final int VARIANT_REGULAR_ATTACK_INTERVAL = 70;
	private final BowAttackGoal<AbstractSkeletonEntity> bowAttackGoal = new BowAttackGoal<>(this, 1.0, HARD_ATTACK_INTERVAL, 15.0F);
	private final MeleeAttackGoal meleeAttackGoal = new MeleeAttackGoal(this, 1.2, false) {
		@Override
		public void stop() {
			super.stop();
			AbstractSkeletonEntity.this.setAttacking(false);
		}

		@Override
		public void start() {
			super.start();
			AbstractSkeletonEntity.this.setAttacking(true);
		}
	};

	protected AbstractSkeletonEntity(EntityType<? extends AbstractSkeletonEntity> entityType, World world) {
		super(entityType, world);
		updateAttackType();
	}

	@Override
	protected void initGoals() {
		goalSelector.add(2, new AvoidSunlightGoal(this));
		goalSelector.add(3, new EscapeSunlightGoal(this, 1.0));
		goalSelector.add(3, new FleeEntityGoal<>(this, WolfEntity.class, 6.0F, 1.0, 1.2));
		goalSelector.add(5, new WanderAroundFarGoal(this, 1.0));
		goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
		goalSelector.add(6, new LookAroundGoal(this));
		targetSelector.add(1, new RevengeGoal(this));
		targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
		targetSelector.add(3, new ActiveTargetGoal<>(this, IronGolemEntity.class, true));
		targetSelector.add(
				3,
				new ActiveTargetGoal<>(
						this,
						TurtleEntity.class,
						10,
						true,
						false,
						TurtleEntity.BABY_TURTLE_ON_LAND_FILTER
				)
		);
	}

	public static DefaultAttributeContainer.Builder createAbstractSkeletonAttributes() {
		return HostileEntity.createHostileAttributes().add(EntityAttributes.MOVEMENT_SPEED, 0.25);
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(getStepSound(), 0.15F, 1.0F);
	}

	abstract SoundEvent getStepSound();

	@Override
	public void tickRiding() {
		super.tickRiding();
		if (getControllingVehicle() instanceof PathAwareEntity pathAwareEntity) {
			bodyYaw = pathAwareEntity.bodyYaw;
		}
	}

	@Override
	protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
		super.initEquipment(random, localDifficulty);
		equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
	}

	@Override
	public @Nullable EntityData initialize(
			ServerWorldAccess world,
			LocalDifficulty difficulty,
			SpawnReason spawnReason,
			@Nullable EntityData entityData
	) {
		entityData = super.initialize(world, difficulty, spawnReason, entityData);
		Random random = world.getRandom();
		initEquipment(random, difficulty);
		updateEnchantments(world, random, difficulty);
		updateAttackType();
		setCanPickUpLoot(random.nextFloat() < 0.55F * difficulty.getClampedLocalDifficulty());
		if (getEquippedStack(EquipmentSlot.HEAD).isEmpty() && Holidays.isHalloween()
				&& random.nextFloat() < 0.25F) {
			equipStack(
					EquipmentSlot.HEAD,
					new ItemStack(random.nextFloat() < 0.1F ? Blocks.JACK_O_LANTERN : Blocks.CARVED_PUMPKIN)
			);
			setEquipmentDropChance(EquipmentSlot.HEAD, 0.0F);
		}

		return entityData;
	}

	/**
	 * Переключает режим атаки между луком и ближним боем в зависимости от предмета в руке.
	 * Интервал атаки лука зависит от сложности: на HARD — короче.
	 */
	public void updateAttackType() {
		if (getEntityWorld() == null || getEntityWorld().isClient()) {
			return;
		}

		goalSelector.remove(meleeAttackGoal);
		goalSelector.remove(bowAttackGoal);
		ItemStack heldStack = getStackInHand(ProjectileUtil.getHandPossiblyHolding(this, Items.BOW));

		if (heldStack.isOf(Items.BOW)) {
			int attackInterval = getEntityWorld().getDifficulty() == Difficulty.HARD
					? getHardAttackInterval()
					: getRegularAttackInterval();

			bowAttackGoal.setAttackInterval(attackInterval);
			goalSelector.add(4, bowAttackGoal);
		}
		else {
			goalSelector.add(4, meleeAttackGoal);
		}
	}

	protected int getHardAttackInterval() {
		return HARD_ATTACK_INTERVAL;
	}

	protected int getRegularAttackInterval() {
		return REGULAR_ATTACK_INTERVAL;
	}

	@Override
	public void shootAt(LivingEntity target, float pullProgress) {
		ItemStack bowStack = getStackInHand(ProjectileUtil.getHandPossiblyHolding(this, Items.BOW));
		ItemStack arrowStack = getProjectileType(bowStack);
		PersistentProjectileEntity arrow = createArrowProjectile(arrowStack, pullProgress, bowStack);

		double dx = target.getX() - getX();
		double dy = target.getBodyY(0.3333333333333333) - arrow.getY();
		double dz = target.getZ() - getZ();
		double horizDist = Math.sqrt(dx * dx + dz * dz);

		if (getEntityWorld() instanceof ServerWorld serverWorld) {
			ProjectileEntity.spawnWithVelocity(
					arrow,
					serverWorld,
					arrowStack,
					dx,
					dy + horizDist * 0.2F,
					dz,
					1.6F,
					14 - serverWorld.getDifficulty().getId() * 4
			);
		}

		playSound(SoundEvents.ENTITY_SKELETON_SHOOT, 1.0F, 1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
	}

	protected PersistentProjectileEntity createArrowProjectile(
			ItemStack arrow,
			float damageModifier,
			@Nullable ItemStack shotFrom
	) {
		return ProjectileUtil.createArrowProjectile(this, arrow, damageModifier, shotFrom);
	}

	@Override
	public boolean canUseRangedWeapon(ItemStack stack) {
		return stack.getItem() == Items.BOW;
	}

	@Override
	public TagKey<Item> getPreferredWeapons() {
		return ItemTags.SKELETON_PREFERRED_WEAPONS;
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		updateAttackType();
	}

	@Override
	public void onEquipStack(EquipmentSlot slot, ItemStack oldStack, ItemStack newStack) {
		super.onEquipStack(slot, oldStack, newStack);
		if (!getEntityWorld().isClient()) {
			updateAttackType();
		}
	}

	public boolean isShaking() {
		return isFrozen();
	}

	@Override
	public boolean canGather(ServerWorld world, ItemStack stack) {
		if (stack.isIn(ItemTags.SPEARS)) {
			return false;
		}

		return super.canGather(world, stack);
	}
}
