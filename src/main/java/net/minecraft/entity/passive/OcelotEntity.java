package net.minecraft.entity.passive;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Оцелот — дикое животное джунглей, которое можно приручить угощением рыбой.
 * В отличие от кошки, оцелот не становится ручным питомцем, а лишь начинает доверять игроку.
 */
public class OcelotEntity extends AnimalEntity {

	public static final double CROUCHING_SPEED = 0.6;
	public static final double NORMAL_SPEED = 0.8;
	public static final double SPRINTING_SPEED = 1.33;
	private static final TrackedData<Boolean> TRUSTING = DataTracker.registerData(
			OcelotEntity.class,
			TrackedDataHandlerRegistry.BOOLEAN
	);
	private OcelotEntity.@Nullable FleeGoal<PlayerEntity> fleeGoal;
	private OcelotEntity.@Nullable OcelotTemptGoal temptGoal;

	public OcelotEntity(EntityType<? extends OcelotEntity> entityType, World world) {
		super(entityType, world);
		updateFleeing();
	}

	boolean isTrusting() {
		return dataTracker.get(TRUSTING);
	}

	private void setTrusting(boolean trusting) {
		dataTracker.set(TRUSTING, trusting);
		updateFleeing();
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putBoolean("Trusting", isTrusting());
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		setTrusting(view.getBoolean("Trusting", false));
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(TRUSTING, false);
	}

	@Override
	protected void initGoals() {
		temptGoal = new OcelotEntity.OcelotTemptGoal(this, CROUCHING_SPEED, stack -> stack.isIn(ItemTags.OCELOT_FOOD), true);
		goalSelector.add(1, new SwimGoal(this));
		goalSelector.add(3, temptGoal);
		goalSelector.add(7, new PounceAtTargetGoal(this, 0.3F));
		goalSelector.add(8, new AttackGoal(this));
		goalSelector.add(9, new AnimalMateGoal(this, NORMAL_SPEED));
		goalSelector.add(10, new WanderAroundFarGoal(this, NORMAL_SPEED, 1.0000001E-5F));
		goalSelector.add(11, new LookAtEntityGoal(this, PlayerEntity.class, 10.0F));
		targetSelector.add(1, new ActiveTargetGoal<>(this, ChickenEntity.class, false));
		targetSelector.add(
				1,
				new ActiveTargetGoal<>(
						this,
						TurtleEntity.class,
						10,
						false,
						false,
						TurtleEntity.BABY_TURTLE_ON_LAND_FILTER
				)
		);
	}

	@Override
	public void mobTick(ServerWorld world) {
		double speed = getMoveControl().getSpeed();

		if (getMoveControl().isMoving()) {
			if (speed == CROUCHING_SPEED) {
				setPose(EntityPose.CROUCHING);
				setSprinting(false);
			} else if (speed == SPRINTING_SPEED) {
				setPose(EntityPose.STANDING);
				setSprinting(true);
			} else {
				setPose(EntityPose.STANDING);
				setSprinting(false);
			}
		} else {
			setPose(EntityPose.STANDING);
			setSprinting(false);
		}
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return !isTrusting() && age > 2400;
	}

	public static DefaultAttributeContainer.Builder createOcelotAttributes() {
		return AnimalEntity.createAnimalAttributes()
				.add(EntityAttributes.MAX_HEALTH, 10.0)
				.add(EntityAttributes.MOVEMENT_SPEED, 0.3F)
				.add(EntityAttributes.ATTACK_DAMAGE, 3.0);
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_OCELOT_AMBIENT;
	}

	@Override
	public int getMinAmbientSoundDelay() {
		return 900;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_OCELOT_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_OCELOT_DEATH;
	}

	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		ItemStack itemStack = player.getStackInHand(hand);

		if ((temptGoal == null || temptGoal.isActive())
				&& !isTrusting()
				&& isBreedingItem(itemStack)
				&& player.squaredDistanceTo(this) < 9.0
		) {
			eat(player, hand, itemStack);

			if (!getEntityWorld().isClient()) {
				if (random.nextInt(3) == 0) {
					setTrusting(true);
					showEmoteParticle(true);
					getEntityWorld().sendEntityStatus(this, (byte) 41);
				} else {
					showEmoteParticle(false);
					getEntityWorld().sendEntityStatus(this, (byte) 40);
				}
			}

			return ActionResult.SUCCESS;
		}

		return super.interactMob(player, hand);
	}

	@Override
	public void handleStatus(byte status) {
		if (status == 41) {
			showEmoteParticle(true);
			return;
		}

		if (status == 40) {
			showEmoteParticle(false);
			return;
		}

		super.handleStatus(status);
	}

	private void showEmoteParticle(boolean positive) {
		ParticleEffect particleEffect = positive ? ParticleTypes.HEART : ParticleTypes.SMOKE;

		for (int count = 0; count < 7; count++) {
			double vx = random.nextGaussian() * 0.02;
			double vy = random.nextGaussian() * 0.02;
			double vz = random.nextGaussian() * 0.02;
			getEntityWorld().addParticleClient(
					particleEffect,
					getParticleX(1.0),
					getRandomBodyY() + 0.5,
					getParticleZ(1.0),
					vx,
					vy,
					vz
			);
		}
	}

	/**
	 * Обновляет цель побега: добавляет её в селектор, если оцелот не доверяет игроку,
	 * и удаляет, если доверие уже установлено.
	 */
	protected void updateFleeing() {
		if (fleeGoal == null) {
			fleeGoal = new OcelotEntity.FleeGoal<>(this, PlayerEntity.class, 16.0F, NORMAL_SPEED, SPRINTING_SPEED);
		}

		goalSelector.remove(fleeGoal);

		if (!isTrusting()) {
			goalSelector.add(4, fleeGoal);
		}
	}

	@Override
	public @Nullable OcelotEntity createChild(ServerWorld serverWorld, PassiveEntity passiveEntity) {
		return EntityType.OCELOT.create(serverWorld, SpawnReason.BREEDING);
	}

	@Override
	public boolean isBreedingItem(ItemStack stack) {
		return stack.isIn(ItemTags.OCELOT_FOOD);
	}

	public static boolean canSpawn(
			EntityType<OcelotEntity> type,
			WorldAccess world,
			SpawnReason spawnReason,
			BlockPos pos,
			Random random
	) {
		return random.nextInt(3) != 0;
	}

	@Override
	public boolean canSpawn(WorldView world) {
		if (!world.doesNotIntersectEntities(this) || world.containsFluid(getBoundingBox())) {
			return false;
		}

		BlockPos blockPos = getBlockPos();

		if (blockPos.getY() < world.getSeaLevel()) {
			return false;
		}

		BlockState blockState = world.getBlockState(blockPos.down());

		return blockState.isOf(Blocks.GRASS_BLOCK) || blockState.isIn(BlockTags.LEAVES);
	}

	@Override
	public @Nullable EntityData initialize(
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

	@Override
	public Vec3d getLeashOffset() {
		return new Vec3d(0.0, 0.5F * getStandingEyeHeight(), getWidth() * 0.4F);
	}

	@Override
	public boolean bypassesSteppingEffects() {
		return isInSneakingPose() || super.bypassesSteppingEffects();
	}

	static class FleeGoal<T extends LivingEntity> extends FleeEntityGoal<T> {

		private final OcelotEntity ocelot;

		public FleeGoal(
				OcelotEntity ocelot,
				Class<T> fleeFromType,
				float distance,
				double slowSpeed,
				double fastSpeed
		) {
			super(ocelot, fleeFromType, distance, slowSpeed, fastSpeed, EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR);
			this.ocelot = ocelot;
		}

		@Override
		public boolean canStart() {
			return !ocelot.isTrusting() && super.canStart();
		}

		@Override
		public boolean shouldContinue() {
			return !ocelot.isTrusting() && super.shouldContinue();
		}
	}

	static class OcelotTemptGoal extends TemptGoal {

		private final OcelotEntity ocelot;

		public OcelotTemptGoal(
				OcelotEntity ocelot,
				double speed,
				Predicate<ItemStack> foodPredicate,
				boolean canBeScared
		) {
			super(ocelot, speed, foodPredicate, canBeScared);
			this.ocelot = ocelot;
		}

		@Override
		protected boolean canBeScared() {
			return super.canBeScared() && !ocelot.isTrusting();
		}
	}
}
