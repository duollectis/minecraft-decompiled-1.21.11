package net.minecraft.entity.mob;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.conversion.EntityConversionContext;
import net.minecraft.entity.conversion.EntityConversionType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.minecraft.world.attribute.EnvironmentAttributes;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * Слизень — моб, разделяющийся на меньших слизней при смерти.
 * Размер определяет здоровье, урон и скорость. Атакует игроков и железных големов.
 * Может спавниться в болотах и в чанках с определённым seed-ом ниже y=40.
 */
public class SlimeEntity extends MobEntity implements Monster {

	private static final TrackedData<Integer>
			SLIME_SIZE =
			DataTracker.registerData(SlimeEntity.class, TrackedDataHandlerRegistry.INTEGER);
	public static final int MIN_SIZE = 1;
	public static final int MAX_SIZE = 127;
	public static final int DEFAULT_SIZE = 4;
	public float targetStretch;
	public float stretch;
	public float lastStretch;
	private boolean onGroundLastTick = false;

	public SlimeEntity(EntityType<? extends SlimeEntity> entityType, World world) {
		super(entityType, world);
		reinitDimensions();
		moveControl = new SlimeEntity.SlimeMoveControl(this);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(1, new SlimeEntity.SwimmingGoal(this));
		goalSelector.add(2, new SlimeEntity.FaceTowardTargetGoal(this));
		goalSelector.add(3, new SlimeEntity.RandomLookGoal(this));
		goalSelector.add(5, new SlimeEntity.MoveGoal(this));
		targetSelector.add(
			1,
			new ActiveTargetGoal<>(
				this,
				PlayerEntity.class,
				10,
				true,
				false,
				(target, world) -> Math.abs(target.getY() - getY()) <= 4.0
			)
		);
		targetSelector.add(3, new ActiveTargetGoal<>(this, IronGolemEntity.class, true));
	}

	@Override
	public SoundCategory getSoundCategory() {
		return SoundCategory.HOSTILE;
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(SLIME_SIZE, 1);
	}

	@VisibleForTesting
	public void setSize(int size, boolean heal) {
		int clamped = MathHelper.clamp(size, 1, MAX_SIZE);
		dataTracker.set(SLIME_SIZE, clamped);
		refreshPosition();
		calculateDimensions();
		getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(clamped * clamped);
		getAttributeInstance(EntityAttributes.MOVEMENT_SPEED).setBaseValue(0.2F + 0.1F * clamped);
		getAttributeInstance(EntityAttributes.ATTACK_DAMAGE).setBaseValue(clamped);

		if (heal) {
			setHealth(getMaxHealth());
		}

		experiencePoints = clamped;
	}

	public int getSize() {
		return dataTracker.get(SLIME_SIZE);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putInt("Size", getSize() - 1);
		view.putBoolean("wasOnGround", onGroundLastTick);
	}

	@Override
	protected void readCustomData(ReadView view) {
		setSize(view.getInt("Size", 0) + 1, false);
		super.readCustomData(view);
		onGroundLastTick = view.getBoolean("wasOnGround", false);
	}

	public boolean isSmall() {
		return getSize() <= 1;
	}

	protected ParticleEffect getParticles() {
		return ParticleTypes.ITEM_SLIME;
	}

	@Override
	public void tick() {
		lastStretch = stretch;
		stretch = stretch + (targetStretch - stretch) * 0.5F;
		super.tick();

		if (isOnGround() && !onGroundLastTick) {
			float width = getDimensions(getPose()).width() * 2.0F;
			float halfWidth = width / 2.0F;

			for (int i = 0; i < width * 16.0F; i++) {
				float angle = random.nextFloat() * (float) (Math.PI * 2);
				float spread = random.nextFloat() * 0.5F + 0.5F;
				float px = MathHelper.sin(angle) * halfWidth * spread;
				float pz = MathHelper.cos(angle) * halfWidth * spread;
				getEntityWorld()
					.addParticleClient(
						getParticles(),
						getX() + px,
						getY(),
						getZ() + pz,
						0.0,
						0.0,
						0.0
					);
			}

			playSound(
				getSquishSound(),
				getSoundVolume(),
				((random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F) / 0.8F
			);
			targetStretch = -0.5F;
		} else if (!isOnGround() && onGroundLastTick) {
			targetStretch = 1.0F;
		}

		onGroundLastTick = isOnGround();
		updateStretch();
	}

	protected void updateStretch() {
		targetStretch *= 0.6F;
	}

	protected int getTicksUntilNextJump() {
		return random.nextInt(20) + 10;
	}

	@Override
	public void calculateDimensions() {
		double x = getX();
		double y = getY();
		double z = getZ();
		super.calculateDimensions();
		setPosition(x, y, z);
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		if (SLIME_SIZE.equals(data)) {
			calculateDimensions();
			setYaw(headYaw);
			bodyYaw = headYaw;

			if (isTouchingWater() && random.nextInt(20) == 0) {
				onSwimmingStart();
			}
		}

		super.onTrackedDataSet(data);
	}

	@Override
	public EntityType<? extends SlimeEntity> getType() {
		return (EntityType<? extends SlimeEntity>) super.getType();
	}

	@Override
	public void remove(Entity.RemovalReason reason) {
		int size = getSize();

		if (!getEntityWorld().isClient() && size > 1 && isDead()) {
			float width = getDimensions(getPose()).width();
			float halfWidth = width / 2.0F;
			int childSize = size / 2;
			int count = 2 + random.nextInt(3);
			Team team = getScoreboardTeam();

			for (int i = 0; i < count; i++) {
				float offsetX = (i % 2 - 0.5F) * halfWidth;
				float offsetZ = (i / 2 - 0.5F) * halfWidth;
				convertTo(
					getType(),
					new EntityConversionContext(EntityConversionType.SPLIT_ON_DEATH, false, false, team),
					SpawnReason.TRIGGERED,
					newSlime -> {
						newSlime.setSize(childSize, true);
						newSlime.refreshPositionAndAngles(
							getX() + offsetX,
							getY() + 0.5,
							getZ() + offsetZ,
							random.nextFloat() * 360.0F,
							0.0F
						);
					}
				);
			}
		}

		super.remove(reason);
	}

	@Override
	public void pushAwayFrom(Entity entity) {
		super.pushAwayFrom(entity);

		if (entity instanceof IronGolemEntity && canAttack()) {
			damage((LivingEntity) entity);
		}
	}

	@Override
	public void onPlayerCollision(PlayerEntity player) {
		if (canAttack()) {
			damage(player);
		}
	}

	/**
	 * Наносит урон цели при прямом контакте.
	 * Проверяет дальность атаки и видимость перед нанесением урона.
	 */
	protected void damage(LivingEntity target) {
		if (!(getEntityWorld() instanceof ServerWorld serverWorld) || !isAlive()) {
			return;
		}

		if (!isInAttackRange(target) || !canSee(target)) {
			return;
		}

		DamageSource damageSource = getDamageSources().mobAttack(this);

		if (target.damage(serverWorld, damageSource, getDamageAmount())) {
			playSound(
				SoundEvents.ENTITY_SLIME_ATTACK,
				1.0F,
				(random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F
			);
			EnchantmentHelper.onTargetDamaged(serverWorld, target, damageSource);
		}
	}

	@Override
	protected Vec3d getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
		return new Vec3d(0.0, dimensions.height() - 0.015625 * getSize() * scaleFactor, 0.0);
	}

	protected boolean canAttack() {
		return !isSmall() && canActVoluntarily();
	}

	protected float getDamageAmount() {
		return (float) getAttributeValue(EntityAttributes.ATTACK_DAMAGE);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return isSmall() ? SoundEvents.ENTITY_SLIME_HURT_SMALL : SoundEvents.ENTITY_SLIME_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return isSmall() ? SoundEvents.ENTITY_SLIME_DEATH_SMALL : SoundEvents.ENTITY_SLIME_DEATH;
	}

	protected SoundEvent getSquishSound() {
		return isSmall() ? SoundEvents.ENTITY_SLIME_SQUISH_SMALL : SoundEvents.ENTITY_SLIME_SQUISH;
	}

	public static boolean canSpawn(
			EntityType<SlimeEntity> type,
			WorldAccess world,
			SpawnReason spawnReason,
			BlockPos pos,
			Random random
	) {
		if (world.getDifficulty() != Difficulty.PEACEFUL) {
			if (SpawnReason.isAnySpawner(spawnReason)) {
				return canMobSpawn(type, world, spawnReason, pos, random);
			}

			if (world.getBiome(pos).isIn(BiomeTags.ALLOWS_SURFACE_SLIME_SPAWNS) && pos.getY() > 50 && pos.getY() < 70) {
				float spawnChance = world
						.getEnvironmentAttributes()
						.getAttributeValue(EnvironmentAttributes.SURFACE_SLIME_SPAWN_CHANCE_GAMEPLAY, pos);

				if (random.nextFloat() < spawnChance && world.getLightLevel(pos) <= random.nextInt(8)) {
					return canMobSpawn(type, world, spawnReason, pos, random);
				}
			}

			if (!(world instanceof StructureWorldAccess)) {
				return false;
			}

			ChunkPos chunkPos = new ChunkPos(pos);
			StructureWorldAccess structureWorld = (StructureWorldAccess) world;
			boolean isSlimeChunk = ChunkRandom
					.getSlimeRandom(
							chunkPos.x,
							chunkPos.z,
							structureWorld.getSeed(),
							987234911L
					)
					.nextInt(10) == 0;

			if (random.nextInt(10) == 0 && isSlimeChunk && pos.getY() < 40) {
				return canMobSpawn(type, world, spawnReason, pos, random);
			}
		}

		return false;
	}

	@Override
	protected float getSoundVolume() {
		return 0.4F * getSize();
	}

	@Override
	public int getMaxLookPitchChange() {
		return 0;
	}

	protected boolean makesJumpSound() {
		return getSize() > 0;
	}

	@Override
	public void jump() {
		Vec3d velocity = getVelocity();
		setVelocity(velocity.x, getJumpVelocity(), velocity.z);
		velocityDirty = true;
	}

	@Override
	public @Nullable EntityData initialize(
		ServerWorldAccess world,
		LocalDifficulty difficulty,
		SpawnReason spawnReason,
		@Nullable EntityData entityData
	) {
		Random random = world.getRandom();
		int tier = random.nextInt(3);

		if (tier < 2 && random.nextFloat() < 0.5F * difficulty.getClampedLocalDifficulty()) {
			tier++;
		}

		setSize(1 << tier, true);

		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	float getJumpSoundPitch() {
		float basePitch = isSmall() ? 1.4F : 0.8F;

		return ((random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F) * basePitch;
	}

	protected SoundEvent getJumpSound() {
		return isSmall() ? SoundEvents.ENTITY_SLIME_JUMP_SMALL : SoundEvents.ENTITY_SLIME_JUMP;
	}

	@Override
	public EntityDimensions getBaseDimensions(EntityPose pose) {
		return super.getBaseDimensions(pose).scaled(getSize());
	}

	static class FaceTowardTargetGoal extends Goal {

		private final SlimeEntity slime;
		private int ticksLeft;

		public FaceTowardTargetGoal(SlimeEntity slime) {
			this.slime = slime;
			setControls(EnumSet.of(Goal.Control.LOOK));
		}

		@Override
		public boolean canStart() {
			LivingEntity target = slime.getTarget();

			if (target == null) {
				return false;
			}

			return slime.canTarget(target) && slime.getMoveControl() instanceof SlimeEntity.SlimeMoveControl;
		}

		@Override
		public void start() {
			ticksLeft = toGoalTicks(300);
			super.start();
		}

		@Override
		public boolean shouldContinue() {
			LivingEntity target = slime.getTarget();

			if (target == null) {
				return false;
			}

			return slime.canTarget(target) && --ticksLeft > 0;
		}

		@Override
		public boolean shouldRunEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity target = slime.getTarget();

			if (target != null) {
				slime.lookAtEntity(target, 10.0F, 10.0F);
			}

			if (slime.getMoveControl() instanceof SlimeEntity.SlimeMoveControl slimeMoveControl) {
				slimeMoveControl.look(slime.getYaw(), slime.canAttack());
			}
		}
	}

	static class MoveGoal extends Goal {

		private final SlimeEntity slime;

		public MoveGoal(SlimeEntity slime) {
			this.slime = slime;
			setControls(EnumSet.of(Goal.Control.JUMP, Goal.Control.MOVE));
		}

		@Override
		public boolean canStart() {
			return !slime.hasVehicle();
		}

		@Override
		public void tick() {
			if (slime.getMoveControl() instanceof SlimeEntity.SlimeMoveControl slimeMoveControl) {
				slimeMoveControl.move(1.0);
			}
		}
	}

	static class RandomLookGoal extends Goal {

		private final SlimeEntity slime;
		private float targetYaw;
		private int timer;

		public RandomLookGoal(SlimeEntity slime) {
			this.slime = slime;
			setControls(EnumSet.of(Goal.Control.LOOK));
		}

		@Override
		public boolean canStart() {
			return slime.getTarget() == null
				&& (slime.isOnGround() || slime.isTouchingWater() || slime.isInLava()
				|| slime.hasStatusEffect(StatusEffects.LEVITATION))
				&& slime.getMoveControl() instanceof SlimeEntity.SlimeMoveControl;
		}

		@Override
		public void tick() {
			if (--timer <= 0) {
				timer = getTickCount(40 + slime.getRandom().nextInt(60));
				targetYaw = slime.getRandom().nextInt(360);
			}

			if (slime.getMoveControl() instanceof SlimeEntity.SlimeMoveControl slimeMoveControl) {
				slimeMoveControl.look(targetYaw, false);
			}
		}
	}

	static class SlimeMoveControl extends MoveControl {

		private float targetYaw;
		private int ticksUntilJump;
		private final SlimeEntity slime;
		private boolean jumpOften;

		public SlimeMoveControl(SlimeEntity slime) {
			super(slime);
			this.slime = slime;
			targetYaw = 180.0F * slime.getYaw() / (float) Math.PI;
		}

		public void look(float targetYaw, boolean jumpOften) {
			this.targetYaw = targetYaw;
			this.jumpOften = jumpOften;
		}

		public void move(double speed) {
			this.speed = speed;
			state = MoveControl.State.MOVE_TO;
		}

		@Override
		public void tick() {
			entity.setYaw(wrapDegrees(entity.getYaw(), targetYaw, 90.0F));
			entity.headYaw = entity.getYaw();
			entity.bodyYaw = entity.getYaw();

			if (state != MoveControl.State.MOVE_TO) {
				entity.setForwardSpeed(0.0F);
				return;
			}

			state = MoveControl.State.WAIT;

			if (entity.isOnGround()) {
				entity.setMovementSpeed(
					(float) (speed * entity.getAttributeValue(EntityAttributes.MOVEMENT_SPEED))
				);

				if (--ticksUntilJump <= 0) {
					ticksUntilJump = slime.getTicksUntilNextJump();

					if (jumpOften) {
						ticksUntilJump /= 3;
					}

					slime.getJumpControl().setActive();

					if (slime.makesJumpSound()) {
						slime.playSound(
							slime.getJumpSound(),
							slime.getSoundVolume(),
							slime.getJumpSoundPitch()
						);
					}
				} else {
					slime.sidewaysSpeed = 0.0F;
					slime.forwardSpeed = 0.0F;
					entity.setMovementSpeed(0.0F);
				}
			} else {
				entity.setMovementSpeed(
					(float) (speed * entity.getAttributeValue(EntityAttributes.MOVEMENT_SPEED))
				);
			}
		}
	}

	static class SwimmingGoal extends Goal {

		private final SlimeEntity slime;

		public SwimmingGoal(SlimeEntity slime) {
			this.slime = slime;
			setControls(EnumSet.of(Goal.Control.JUMP, Goal.Control.MOVE));
			slime.getNavigation().setCanSwim(true);
		}

		@Override
		public boolean canStart() {
			return (slime.isTouchingWater() || slime.isInLava())
				&& slime.getMoveControl() instanceof SlimeEntity.SlimeMoveControl;
		}

		@Override
		public boolean shouldRunEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			if (slime.getRandom().nextFloat() < 0.8F) {
				slime.getJumpControl().setActive();
			}

			if (slime.getMoveControl() instanceof SlimeEntity.SlimeMoveControl slimeMoveControl) {
				slimeMoveControl.move(1.2);
			}
		}
	}
}
