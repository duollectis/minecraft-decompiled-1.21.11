package net.minecraft.entity.boss;

import com.google.common.collect.ImmutableList;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.WindChargeEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.TintedParticleEffect;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

/**
 * Сущность Иссушителя (Wither) — летающий трёхголовый босс.
 * После призыва проходит фазу неуязвимости ({@link #ON_SUMMONED_INVUL_TIMER} тиков),
 * затем переходит в боевой режим с атаками черепами и разрушением блоков.
 */
public class WitherEntity extends HostileEntity implements RangedAttackMob {

	private static final TrackedData<Integer> TRACKED_ENTITY_ID_1 =
			DataTracker.registerData(WitherEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Integer> TRACKED_ENTITY_ID_2 =
			DataTracker.registerData(WitherEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Integer> TRACKED_ENTITY_ID_3 =
			DataTracker.registerData(WitherEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final List<TrackedData<Integer>> TRACKED_ENTITY_IDS =
			ImmutableList.of(TRACKED_ENTITY_ID_1, TRACKED_ENTITY_ID_2, TRACKED_ENTITY_ID_3);
	private static final TrackedData<Integer> INVUL_TIMER =
			DataTracker.registerData(WitherEntity.class, TrackedDataHandlerRegistry.INTEGER);

	private static final int ON_SUMMONED_INVUL_TIMER = 220;
	private static final int DEFAULT_INVUL_TIMER = 0;
	private static final int SIDE_HEAD_COUNT = 2;
	private static final int SKULL_COOLDOWN_BASE = 10;
	private static final int SKULL_COOLDOWN_RANDOM = 10;
	private static final int SKULL_COOLDOWN_ON_HIT = 40;
	private static final int SKULL_COOLDOWN_ON_HIT_RANDOM = 20;
	private static final int CHARGED_SKULL_THRESHOLD = 15;
	private static final int CHARGED_SKULL_BONUS = 3;
	private static final int BLOCK_BREAKING_COOLDOWN_ON_HIT = 20;
	private static final int HEAL_INTERVAL_TICKS = 20;
	private static final int INVUL_HEAL_INTERVAL_TICKS = 10;
	private static final int INVUL_HEAL_AMOUNT = 10;
	private static final int HEAL_AMOUNT = 1;
	private static final double CHARGED_SKULL_RANGE_X = 10.0;
	private static final double CHARGED_SKULL_RANGE_Y = 5.0;
	private static final double CHARGED_SKULL_RANGE_Z = 10.0;
	private static final double HEAD_TARGET_MAX_DISTANCE = 20.0;
	private static final double HEAD_TRACK_DISTANCE_SQ = 900.0;
	private static final double HORIZONTAL_CHASE_DISTANCE_SQ = 9.0;
	private static final double SIDE_HEAD_OFFSET = 1.3;
	private static final float SIDE_HEAD_YAW_SPEED = 10.0F;
	private static final float SIDE_HEAD_PITCH_SPEED = 40.0F;
	private static final float VELOCITY_DAMPING_Y = 0.6F;
	private static final float VELOCITY_PUSH_FACTOR = 0.3F;
	private static final float VELOCITY_DRAG_FACTOR = 0.6F;
	private static final float HORIZONTAL_SPEED_THRESHOLD = 0.05F;
	private static final float PARTICLE_SPREAD = 0.3F;
	private static final float INVUL_PARTICLE_HEIGHT = 3.3F;
	private static final float CENTER_HEAD_Y_OFFSET = 3.0F;
	private static final float SIDE_HEAD_Y_OFFSET = 2.2F;
	private static final int EXPERIENCE_POINTS = 50;

	private static final TargetPredicate.EntityPredicate CAN_ATTACK_PREDICATE =
			(entity, world) -> !entity.getType().isIn(EntityTypeTags.WITHER_FRIENDS) && entity.isMobOrPlayer();
	private static final TargetPredicate HEAD_TARGET_PREDICATE =
			TargetPredicate.createAttackable()
					.setBaseMaxDistance(HEAD_TARGET_MAX_DISTANCE)
					.setPredicate(CAN_ATTACK_PREDICATE);

	private final float[] sideHeadPitches = new float[SIDE_HEAD_COUNT];
	private final float[] sideHeadYaws = new float[SIDE_HEAD_COUNT];
	private final float[] lastSideHeadPitches = new float[SIDE_HEAD_COUNT];
	private final float[] lastSideHeadYaws = new float[SIDE_HEAD_COUNT];
	private final int[] skullCooldowns = new int[SIDE_HEAD_COUNT];
	private final int[] chargedSkullCooldowns = new int[SIDE_HEAD_COUNT];
	private int blockBreakingCooldown;
	private final ServerBossBar bossBar =
			(ServerBossBar) new ServerBossBar(getDisplayName(), BossBar.Color.PURPLE, BossBar.Style.PROGRESS)
					.setDarkenSky(true);

	public WitherEntity(EntityType<? extends WitherEntity> entityType, World world) {
		super(entityType, world);
		moveControl = new FlightMoveControl(this, 10, false);
		setHealth(getMaxHealth());
		experiencePoints = EXPERIENCE_POINTS;
	}

	@Override
	protected EntityNavigation createNavigation(World world) {
		BirdNavigation navigation = new BirdNavigation(this, world);
		navigation.setCanOpenDoors(false);
		navigation.setCanSwim(true);
		return navigation;
	}

	@Override
	protected void initGoals() {
		goalSelector.add(0, new DescendAtHalfHealthGoal());
		goalSelector.add(2, new ProjectileAttackGoal(this, 1.0, 40, 20.0F));
		goalSelector.add(5, new FlyGoal(this, 1.0));
		goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
		goalSelector.add(7, new LookAroundGoal(this));
		targetSelector.add(1, new RevengeGoal(this));
		targetSelector.add(2, new ActiveTargetGoal<>(this, LivingEntity.class, 0, false, false, CAN_ATTACK_PREDICATE));
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(TRACKED_ENTITY_ID_1, 0);
		builder.add(TRACKED_ENTITY_ID_2, 0);
		builder.add(TRACKED_ENTITY_ID_3, 0);
		builder.add(INVUL_TIMER, DEFAULT_INVUL_TIMER);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putInt("Invul", getInvulnerableTimer());
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		setInvulTimer(view.getInt("Invul", 0));
		if (hasCustomName()) {
			bossBar.setName(getDisplayName());
		}
	}

	@Override
	public void setCustomName(@Nullable Text name) {
		super.setCustomName(name);
		bossBar.setName(getDisplayName());
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_WITHER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_WITHER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_WITHER_DEATH;
	}

	@Override
	public void tickMovement() {
		Vec3d velocity = getVelocity().multiply(1.0, VELOCITY_DAMPING_Y, 1.0);

		if (!getEntityWorld().isClient() && getTrackedEntityId(0) > 0) {
			Entity target = getEntityWorld().getEntityById(getTrackedEntityId(0));
			if (target != null) {
				double velY = velocity.y;
				if (getY() < target.getY() || !isArmored() && getY() < target.getY() + 5.0) {
					velY = Math.max(0.0, velY);
					velY += VELOCITY_PUSH_FACTOR - velY * VELOCITY_DRAG_FACTOR;
				}

				velocity = new Vec3d(velocity.x, velY, velocity.z);
				Vec3d toTarget = new Vec3d(target.getX() - getX(), 0.0, target.getZ() - getZ());
				if (toTarget.horizontalLengthSquared() > HORIZONTAL_CHASE_DISTANCE_SQ) {
					Vec3d direction = toTarget.normalize();
					velocity = velocity.add(
							direction.x * VELOCITY_PUSH_FACTOR - velocity.x * VELOCITY_DRAG_FACTOR,
							0.0,
							direction.z * VELOCITY_PUSH_FACTOR - velocity.z * VELOCITY_DRAG_FACTOR
					);
				}
			}
		}

		setVelocity(velocity);
		if (velocity.horizontalLengthSquared() > HORIZONTAL_SPEED_THRESHOLD) {
			setYaw((float) MathHelper.atan2(velocity.z, velocity.x) * (180.0F / (float) Math.PI) - 90.0F);
		}

		super.tickMovement();

		for (int i = 0; i < SIDE_HEAD_COUNT; i++) {
			lastSideHeadYaws[i] = sideHeadYaws[i];
			lastSideHeadPitches[i] = sideHeadPitches[i];
		}

		for (int i = 0; i < SIDE_HEAD_COUNT; i++) {
			int targetId = getTrackedEntityId(i + 1);
			Entity sideTarget = targetId > 0 ? getEntityWorld().getEntityById(targetId) : null;

			if (sideTarget != null) {
				double headX = getHeadX(i + 1);
				double headY = getHeadY(i + 1);
				double headZ = getHeadZ(i + 1);
				double dx = sideTarget.getX() - headX;
				double dy = sideTarget.getEyeY() - headY;
				double dz = sideTarget.getZ() - headZ;
				double horizontalDist = Math.sqrt(dx * dx + dz * dz);
				float yaw = (float) (MathHelper.atan2(dz, dx) * 180.0F / (float) Math.PI) - 90.0F;
				float pitch = (float) (-(MathHelper.atan2(dy, horizontalDist) * 180.0F / (float) Math.PI));
				sideHeadPitches[i] = getNextAngle(sideHeadPitches[i], pitch, SIDE_HEAD_PITCH_SPEED);
				sideHeadYaws[i] = getNextAngle(sideHeadYaws[i], yaw, SIDE_HEAD_YAW_SPEED);
			} else {
				sideHeadYaws[i] = getNextAngle(sideHeadYaws[i], bodyYaw, SIDE_HEAD_YAW_SPEED);
			}
		}

		boolean armored = isArmored();
		float particleSpread = PARTICLE_SPREAD * getScale();

		for (int i = 0; i < 3; i++) {
			double headX = getHeadX(i);
			double headY = getHeadY(i);
			double headZ = getHeadZ(i);
			getEntityWorld().addParticleClient(
					ParticleTypes.SMOKE,
					headX + random.nextGaussian() * particleSpread,
					headY + random.nextGaussian() * particleSpread,
					headZ + random.nextGaussian() * particleSpread,
					0.0, 0.0, 0.0
			);

			if (armored && getEntityWorld().random.nextInt(4) == 0) {
				getEntityWorld().addParticleClient(
						TintedParticleEffect.create(ParticleTypes.ENTITY_EFFECT, 0.7F, 0.7F, 0.5F),
						headX + random.nextGaussian() * particleSpread,
						headY + random.nextGaussian() * particleSpread,
						headZ + random.nextGaussian() * particleSpread,
						0.0, 0.0, 0.0
				);
			}
		}

		if (getInvulnerableTimer() > 0) {
			float invulParticleHeight = INVUL_PARTICLE_HEIGHT * getScale();
			for (int i = 0; i < 3; i++) {
				getEntityWorld().addParticleClient(
						TintedParticleEffect.create(ParticleTypes.ENTITY_EFFECT, 0.7F, 0.7F, 0.9F),
						getX() + random.nextGaussian(),
						getY() + random.nextFloat() * invulParticleHeight,
						getZ() + random.nextGaussian(),
						0.0, 0.0, 0.0
				);
			}
		}
	}

	@Override
	protected void mobTick(ServerWorld world) {
		if (getInvulnerableTimer() > 0) {
			int remainingInvul = getInvulnerableTimer() - 1;
			bossBar.setPercent(1.0F - remainingInvul / (float) ON_SUMMONED_INVUL_TIMER);

			if (remainingInvul <= 0) {
				world.createExplosion(this, getX(), getEyeY(), getZ(), 7.0F, false, World.ExplosionSourceType.MOB);
				if (!isSilent()) {
					world.syncGlobalEvent(1023, getBlockPos(), 0);
				}
			}

			setInvulTimer(remainingInvul);
			if (age % INVUL_HEAL_INTERVAL_TICKS == 0) {
				heal(INVUL_HEAL_AMOUNT);
			}

			return;
		}

		super.mobTick(world);

		for (int headIdx = 1; headIdx < 3; headIdx++) {
			if (age >= skullCooldowns[headIdx - 1]) {
				skullCooldowns[headIdx - 1] = age + SKULL_COOLDOWN_BASE + random.nextInt(SKULL_COOLDOWN_RANDOM);

				if ((world.getDifficulty() == Difficulty.NORMAL || world.getDifficulty() == Difficulty.HARD)
						&& chargedSkullCooldowns[headIdx - 1]++ > CHARGED_SKULL_THRESHOLD) {
					double randX = MathHelper.nextDouble(random, getX() - CHARGED_SKULL_RANGE_X, getX() + CHARGED_SKULL_RANGE_X);
					double randY = MathHelper.nextDouble(random, getY() - CHARGED_SKULL_RANGE_Y, getY() + CHARGED_SKULL_RANGE_Y);
					double randZ = MathHelper.nextDouble(random, getZ() - CHARGED_SKULL_RANGE_Z, getZ() + CHARGED_SKULL_RANGE_Z);
					shootSkullAt(headIdx + 1, randX, randY, randZ, true);
					chargedSkullCooldowns[headIdx - 1] = 0;
				}

				int trackedId = getTrackedEntityId(headIdx);
				if (trackedId > 0) {
					LivingEntity sideTarget = (LivingEntity) world.getEntityById(trackedId);
					if (sideTarget != null
							&& canTarget(sideTarget)
							&& squaredDistanceTo(sideTarget) <= HEAD_TRACK_DISTANCE_SQ
							&& canSee(sideTarget)) {
						shootSkullAt(headIdx + 1, sideTarget);
						skullCooldowns[headIdx - 1] = age + SKULL_COOLDOWN_ON_HIT + random.nextInt(SKULL_COOLDOWN_ON_HIT_RANDOM);
						chargedSkullCooldowns[headIdx - 1] = 0;
					} else {
						setTrackedEntityId(headIdx, 0);
					}
				} else {
					List<LivingEntity> nearbyTargets = world.getTargets(
							LivingEntity.class,
							HEAD_TARGET_PREDICATE,
							this,
							getBoundingBox().expand(20.0, 8.0, 20.0)
					);
					if (!nearbyTargets.isEmpty()) {
						setTrackedEntityId(headIdx, nearbyTargets.get(random.nextInt(nearbyTargets.size())).getId());
					}
				}
			}
		}

		LivingEntity mainTarget = getTarget();
		setTrackedEntityId(0, mainTarget != null ? mainTarget.getId() : 0);

		if (blockBreakingCooldown > 0) {
			blockBreakingCooldown--;
			if (blockBreakingCooldown == 0 && world.getGameRules().getValue(GameRules.DO_MOB_GRIEFING)) {
				boolean brokeBlock = false;
				int halfWidth = MathHelper.floor(getWidth() / 2.0F + 1.0F);
				int height = MathHelper.floor(getHeight());

				for (BlockPos blockPos : BlockPos.iterate(
						getBlockX() - halfWidth, getBlockY(), getBlockZ() - halfWidth,
						getBlockX() + halfWidth, getBlockY() + height, getBlockZ() + halfWidth
				)) {
					BlockState blockState = world.getBlockState(blockPos);
					if (canDestroy(blockState)) {
						brokeBlock = world.breakBlock(blockPos, true, this) || brokeBlock;
					}
				}

				if (brokeBlock) {
					world.syncWorldEvent(null, 1022, getBlockPos(), 0);
				}
			}
		}

		if (age % HEAL_INTERVAL_TICKS == 0) {
			heal(HEAL_AMOUNT);
		}

		bossBar.setPercent(getHealth() / getMaxHealth());
	}

	public static boolean canDestroy(BlockState block) {
		return !block.isAir() && !block.isIn(BlockTags.WITHER_IMMUNE);
	}

	/**
	 * Запускает фазу призыва: устанавливает таймер неуязвимости и сбрасывает здоровье до 1/3.
	 */
	public void onSummoned() {
		setInvulTimer(ON_SUMMONED_INVUL_TIMER);
		bossBar.setPercent(0.0F);
		setHealth(getMaxHealth() / 3.0F);
	}

	@Override
	public void slowMovement(BlockState state, Vec3d multiplier) {
	}

	@Override
	public void onStartedTrackingBy(ServerPlayerEntity player) {
		super.onStartedTrackingBy(player);
		bossBar.addPlayer(player);
	}

	@Override
	public void onStoppedTrackingBy(ServerPlayerEntity player) {
		super.onStoppedTrackingBy(player);
		bossBar.removePlayer(player);
	}

	private double getHeadX(int headIndex) {
		if (headIndex <= 0) {
			return getX();
		}

		float angle = (bodyYaw + 180 * (headIndex - 1)) * (float) (Math.PI / 180.0);
		return getX() + MathHelper.cos(angle) * SIDE_HEAD_OFFSET * getScale();
	}

	private double getHeadY(int headIndex) {
		float yOffset = headIndex <= 0 ? CENTER_HEAD_Y_OFFSET : SIDE_HEAD_Y_OFFSET;
		return getY() + yOffset * getScale();
	}

	private double getHeadZ(int headIndex) {
		if (headIndex <= 0) {
			return getZ();
		}

		float angle = (bodyYaw + 180 * (headIndex - 1)) * (float) (Math.PI / 180.0);
		return getZ() + MathHelper.sin(angle) * SIDE_HEAD_OFFSET * getScale();
	}

	/**
	 * Плавно поворачивает угол {@code lastAngle} к {@code desiredAngle} с ограничением скорости {@code maxDifference}.
	 */
	private float getNextAngle(float lastAngle, float desiredAngle, float maxDifference) {
		float delta = MathHelper.wrapDegrees(desiredAngle - lastAngle);
		delta = MathHelper.clamp(delta, -maxDifference, maxDifference);
		return lastAngle + delta;
	}

	private void shootSkullAt(int headIndex, LivingEntity target) {
		shootSkullAt(
				headIndex,
				target.getX(),
				target.getY() + target.getStandingEyeHeight() * 0.5,
				target.getZ(),
				headIndex == 0 && random.nextFloat() < 0.001F
		);
	}

	private void shootSkullAt(int headIndex, double targetX, double targetY, double targetZ, boolean charged) {
		if (!isSilent()) {
			getEntityWorld().syncWorldEvent(null, WorldEvents.WITHER_SHOOTS, getBlockPos(), 0);
		}

		double originX = getHeadX(headIndex);
		double originY = getHeadY(headIndex);
		double originZ = getHeadZ(headIndex);
		Vec3d direction = new Vec3d(targetX - originX, targetY - originY, targetZ - originZ).normalize();
		WitherSkullEntity skull = new WitherSkullEntity(getEntityWorld(), this, direction);
		skull.setOwner(this);
		if (charged) {
			skull.setCharged(true);
		}

		skull.setPosition(originX, originY, originZ);
		getEntityWorld().spawnEntity(skull);
	}

	@Override
	public void shootAt(LivingEntity target, float pullProgress) {
		shootSkullAt(0, target);
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		if (isInvulnerableTo(world, source)) {
			return false;
		}

		if (source.isIn(DamageTypeTags.WITHER_IMMUNE_TO) || source.getAttacker() instanceof WitherEntity) {
			return false;
		}

		if (getInvulnerableTimer() > 0 && !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return false;
		}

		if (isArmored()) {
			Entity sourceEntity = source.getSource();
			if (sourceEntity instanceof PersistentProjectileEntity || sourceEntity instanceof WindChargeEntity) {
				return false;
			}
		}

		Entity attacker = source.getAttacker();
		if (attacker != null && attacker.getType().isIn(EntityTypeTags.WITHER_FRIENDS)) {
			return false;
		}

		if (blockBreakingCooldown <= 0) {
			blockBreakingCooldown = BLOCK_BREAKING_COOLDOWN_ON_HIT;
		}

		for (int i = 0; i < chargedSkullCooldowns.length; i++) {
			chargedSkullCooldowns[i] += CHARGED_SKULL_BONUS;
		}

		return super.damage(world, source, amount);
	}

	@Override
	protected void dropEquipment(ServerWorld world, DamageSource source, boolean causedByPlayer) {
		super.dropEquipment(world, source, causedByPlayer);
		ItemEntity star = dropItem(world, Items.NETHER_STAR);
		if (star != null) {
			star.setCovetedItem();
		}
	}

	@Override
	public void checkDespawn() {
		if (getEntityWorld().getDifficulty() == Difficulty.PEACEFUL && !getType().isAllowedInPeaceful()) {
			discard();
		} else {
			despawnCounter = 0;
		}
	}

	@Override
	public boolean addStatusEffect(StatusEffectInstance effect, @Nullable Entity source) {
		return false;
	}

	public static DefaultAttributeContainer.Builder createWitherAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.MAX_HEALTH, 300.0)
				.add(EntityAttributes.MOVEMENT_SPEED, 0.6F)
				.add(EntityAttributes.FLYING_SPEED, 0.6F)
				.add(EntityAttributes.FOLLOW_RANGE, 40.0)
				.add(EntityAttributes.ARMOR, 4.0);
	}

	public float[] getSideHeadYaws() {
		return sideHeadYaws;
	}

	public float[] getSideHeadPitches() {
		return sideHeadPitches;
	}

	public int getInvulnerableTimer() {
		return dataTracker.get(INVUL_TIMER);
	}

	public void setInvulTimer(int ticks) {
		dataTracker.set(INVUL_TIMER, ticks);
	}

	public int getTrackedEntityId(int headIndex) {
		return dataTracker.get(TRACKED_ENTITY_IDS.get(headIndex));
	}

	public void setTrackedEntityId(int headIndex, int id) {
		dataTracker.set(TRACKED_ENTITY_IDS.get(headIndex), id);
	}

	/**
	 * Иссушитель переходит в «бронированный» режим при здоровье ≤ 50%, отражая снаряды.
	 */
	public boolean isArmored() {
		return getHealth() <= getMaxHealth() / 2.0F;
	}

	@Override
	protected boolean canStartRiding(Entity entity) {
		return false;
	}

	@Override
	public boolean canUsePortals(boolean allowVehicles) {
		return false;
	}

	@Override
	public boolean canHaveStatusEffect(StatusEffectInstance effect) {
		return effect.equals(StatusEffects.WITHER) ? false : super.canHaveStatusEffect(effect);
	}

	/**
	 * Цель, блокирующая движение Иссушителя во время фазы неуязвимости после призыва.
	 */
	class DescendAtHalfHealthGoal extends Goal {

		public DescendAtHalfHealthGoal() {
			setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.JUMP, Goal.Control.LOOK));
		}

		@Override
		public boolean canStart() {
			return WitherEntity.this.getInvulnerableTimer() > 0;
		}
	}
}
