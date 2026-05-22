package net.minecraft.entity.boss.dragon;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathMinHeap;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.dragon.phase.Phase;
import net.minecraft.entity.boss.dragon.phase.PhaseManager;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.gen.feature.EndPortalFeature;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Главная сущность Дракона Края. Управляет телом дракона, состоящим из нескольких
 * {@link EnderDragonPart}, системой поиска пути по 24 узлам-точкам облёта острова,
 * а также делегирует логику поведения текущей {@link Phase} через {@link PhaseManager}.
 */
public class EnderDragonEntity extends MobEntity implements Monster {

	private static final Logger LOGGER = LogUtils.getLogger();

	public static final TrackedData<Integer> PHASE_TYPE =
			DataTracker.registerData(EnderDragonEntity.class, TrackedDataHandlerRegistry.INTEGER);

	private static final TargetPredicate CLOSE_PLAYER_PREDICATE =
			TargetPredicate.createAttackable().setBaseMaxDistance(64.0);

	private static final int MAX_HEALTH = 200;
	private static final int PATH_NODE_COUNT = 24;
	private static final int OUTER_RING_COUNT = 12;
	private static final int MIDDLE_RING_COUNT = 8;
	private static final int INNER_RING_COUNT = 4;
	private static final int MIDDLE_RING_START = 12;
	private static final int INNER_RING_START = 20;
	private static final float OUTER_RING_RADIUS = 60.0F;
	private static final float MIDDLE_RING_RADIUS = 40.0F;
	private static final float INNER_RING_RADIUS = 20.0F;
	private static final int OUTER_RING_HEIGHT_OFFSET = 5;
	private static final int MIDDLE_RING_HEIGHT_OFFSET = 15;
	private static final int MIN_PATH_NODE_Y = 73;
	private static final float TAKEOFF_THRESHOLD = 0.25F;
	private static final float WING_SITTING_SPEED = 0.1F;
	private static final float WING_SLOWED_FACTOR = 0.5F;
	private static final float MOVEMENT_SPEED_FACTOR = 0.06F;
	private static final float VELOCITY_DAMPING_XZ = 0.91F;
	private static final float YAW_ACCELERATION_DAMPING = 0.8F;
	private static final float YAW_ACCELERATION_SCALE = 0.1F;
	private static final float MAX_YAW_DELTA = 50.0F;
	private static final float VELOCITY_LATERAL_SCALE = 0.8F;
	private static final float VELOCITY_LATERAL_BONUS = 0.15F;
	private static final float WING_LAUNCH_THRESHOLD = -0.3F;
	private static final int GROWL_INTERVAL_BASE = 200;
	private static final int DEATH_EXPLOSION_SPREAD_XZ = 8;
	private static final int DEATH_EXPLOSION_SPREAD_Y = 4;
	private static final int DEATH_PARTICLE_START_TICK = 180;
	private static final int DEATH_XP_TICK_INTERVAL = 5;
	private static final int DEATH_XP_START_TICK = 150;
	private static final int DEATH_TOTAL_TICKS = 200;
	private static final int DEATH_XP_FIRST_KILL = 12000;
	private static final int DEATH_XP_REPEAT_KILL = 500;
	private static final float DEATH_XP_PERIODIC_FRACTION = 0.08F;
	private static final float DEATH_XP_FINAL_FRACTION = 0.2F;
	private static final int WORLD_EVENT_DRAGON_DEATH = 1028;
	private static final int WORLD_EVENT_BLOCK_BREAK = 2008;
	private static final float WING_DAMAGE = 5.0F;
	private static final float HEAD_DAMAGE = 10.0F;
	private static final float CRYSTAL_EXPLOSION_DAMAGE = 10.0F;
	private static final float BODY_DAMAGE_REDUCTION = 4.0F;
	private static final float BODY_DAMAGE_MIN = 1.0F;
	private static final float DAMAGE_THRESHOLD = 0.01F;
	private static final float DEATH_RISE_SPEED = 0.1F;
	private static final int CRYSTAL_SEARCH_INTERVAL = 10;
	private static final int CRYSTAL_SEARCH_RADIUS = 32;
	private static final double WING_LAUNCH_FORCE = 4.0;
	private static final double WING_LAUNCH_VERTICAL = 0.2;
	private static final double MIN_HORIZONTAL_DIST_SQ = 0.1;
	private static final int TAIL_SEGMENT_COUNT = 3;
	private static final int TAIL_FRAME_BASE = 12;
	private static final int TAIL_FRAME_STEP = 2;
	private static final float TAIL_SEGMENT_SPACING = 1.5F;
	private static final float HEAD_FORWARD_OFFSET = 6.5F;
	private static final float NECK_FORWARD_OFFSET = 5.5F;
	private static final float WING_LATERAL_OFFSET = 4.5F;
	private static final float WING_VERTICAL_OFFSET = 2.0F;
	private static final float BODY_LATERAL_OFFSET = 0.5F;
	private static final int PITCH_FRAME_NEAR = 5;
	private static final int PITCH_FRAME_FAR = 10;
	private static final float PITCH_SCALE = 10.0F;
	private static final float LANDING_PITCH_FACTOR = 6.0F;
	private static final float LANDING_PITCH_MULTIPLIER = 1.5F;
	private static final float LANDING_PITCH_DEGREES = 5.0F;
	private static final float SITTING_PITCH_DEGREES = -45.0F;
	private static final float LANDING_MIN_DISTANCE_FACTOR = 4.0F;
	private static final float LANDING_MAX_PITCH_SCALE = 1.0F;

	public final EnderDragonFrameTracker frameTracker = new EnderDragonFrameTracker();
	private final EnderDragonPart[] parts;
	public final EnderDragonPart head;
	private final EnderDragonPart neck;
	private final EnderDragonPart body;
	private final EnderDragonPart tail1;
	private final EnderDragonPart tail2;
	private final EnderDragonPart tail3;
	private final EnderDragonPart rightWing;
	private final EnderDragonPart leftWing;
	public float lastWingPosition;
	public float wingPosition;
	public boolean slowedDownByBlock;
	public int ticksSinceDeath = 0;
	public float yawAcceleration;
	public @Nullable EndCrystalEntity connectedCrystal;
	private @Nullable EnderDragonFight fight;
	private BlockPos fightOrigin = BlockPos.ORIGIN;
	private final PhaseManager phaseManager;
	private int ticksUntilNextGrowl = 100;
	private float damageDuringSitting;
	private final PathNode[] pathNodes = new PathNode[PATH_NODE_COUNT];
	private final int[] pathNodeConnections = new int[PATH_NODE_COUNT];
	private final PathMinHeap pathHeap = new PathMinHeap();

	public EnderDragonEntity(EntityType<? extends EnderDragonEntity> entityType, World world) {
		super(EntityType.ENDER_DRAGON, world);
		head = new EnderDragonPart(this, "head", 1.0F, 1.0F);
		neck = new EnderDragonPart(this, "neck", 3.0F, 3.0F);
		body = new EnderDragonPart(this, "body", 5.0F, 3.0F);
		tail1 = new EnderDragonPart(this, "tail", 2.0F, 2.0F);
		tail2 = new EnderDragonPart(this, "tail", 2.0F, 2.0F);
		tail3 = new EnderDragonPart(this, "tail", 2.0F, 2.0F);
		rightWing = new EnderDragonPart(this, "wing", 4.0F, 2.0F);
		leftWing = new EnderDragonPart(this, "wing", 4.0F, 2.0F);
		parts = new EnderDragonPart[]{head, neck, body, tail1, tail2, tail3, rightWing, leftWing};
		setHealth(getMaxHealth());
		noClip = true;
		phaseManager = new PhaseManager(this);
	}

	public void setFight(EnderDragonFight fight) {
		this.fight = fight;
	}

	public void setFightOrigin(BlockPos fightOrigin) {
		this.fightOrigin = fightOrigin;
	}

	public BlockPos getFightOrigin() {
		return fightOrigin;
	}

	public static DefaultAttributeContainer.Builder createEnderDragonAttributes() {
		return MobEntity
				.createMobAttributes()
				.add(EntityAttributes.MAX_HEALTH, MAX_HEALTH)
				.add(EntityAttributes.CAMERA_DISTANCE, 16.0);
	}

	@Override
	public boolean isFlappingWings() {
		float currentCos = MathHelper.cos(wingPosition * (float) (Math.PI * 2));
		float lastCos = MathHelper.cos(lastWingPosition * (float) (Math.PI * 2));
		return lastCos <= WING_LAUNCH_THRESHOLD && currentCos >= WING_LAUNCH_THRESHOLD;
	}

	@Override
	public void addFlapEffects() {
		if (getEntityWorld().isClient() && !isSilent()) {
			getEntityWorld().playSoundClient(
					getX(),
					getY(),
					getZ(),
					SoundEvents.ENTITY_ENDER_DRAGON_FLAP,
					getSoundCategory(),
					5.0F,
					0.8F + random.nextFloat() * 0.3F,
					false
			);
		}
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(PHASE_TYPE, PhaseType.HOVER.getTypeId());
	}

	@Override
	public void tickMovement() {
		addAirTravelEffects();
		tickClientSide();
		syncFightIfNeeded();

		lastWingPosition = wingPosition;

		if (isDead()) {
			spawnDeathParticles();
			return;
		}

		tickWithEndCrystals();
		updateWingPosition();

		setYaw(MathHelper.wrapDegrees(getYaw()));

		if (isAiDisabled()) {
			wingPosition = 0.5F;
			return;
		}

		frameTracker.tick(getY(), getYaw());

		if (getEntityWorld() instanceof ServerWorld serverWorld) {
			tickServerMovement(serverWorld);
		} else {
			interpolator.tick();
			phaseManager.getCurrent().clientTick();
		}

		if (!getEntityWorld().isClient()) {
			tickBlockCollision();
		}

		bodyYaw = getYaw();
		updateBodyParts();
	}

	private void tickClientSide() {
		if (!getEntityWorld().isClient()) {
			return;
		}

		setHealth(getHealth());

		if (isSilent() || phaseManager.getCurrent().isSittingOrHovering()) {
			return;
		}

		if (--ticksUntilNextGrowl < 0) {
			getEntityWorld().playSoundClient(
					getX(),
					getY(),
					getZ(),
					SoundEvents.ENTITY_ENDER_DRAGON_GROWL,
					getSoundCategory(),
					2.5F,
					0.8F + random.nextFloat() * 0.3F,
					false
			);
			ticksUntilNextGrowl = GROWL_INTERVAL_BASE + random.nextInt(GROWL_INTERVAL_BASE);
		}
	}

	private void syncFightIfNeeded() {
		if (fight != null) {
			return;
		}

		if (getEntityWorld() instanceof ServerWorld serverWorld) {
			EnderDragonFight enderDragonFight = serverWorld.getEnderDragonFight();
			if (enderDragonFight != null && getUuid().equals(enderDragonFight.getDragonUuid())) {
				fight = enderDragonFight;
			}
		}
	}

	private void spawnDeathParticles() {
		float offsetX = (random.nextFloat() - 0.5F) * DEATH_EXPLOSION_SPREAD_XZ;
		float offsetY = (random.nextFloat() - 0.5F) * DEATH_EXPLOSION_SPREAD_Y;
		float offsetZ = (random.nextFloat() - 0.5F) * DEATH_EXPLOSION_SPREAD_XZ;
		getEntityWorld().addParticleClient(
				ParticleTypes.EXPLOSION,
				getX() + offsetX,
				getY() + 2.0 + offsetY,
				getZ() + offsetZ,
				0.0, 0.0, 0.0
		);
	}

	private void updateWingPosition() {
		Vec3d velocity = getVelocity();
		float wingFactor = 0.2F / ((float) velocity.horizontalLength() * 10.0F + 1.0F);
		wingFactor *= (float) Math.pow(2.0, velocity.y);

		if (phaseManager.getCurrent().isSittingOrHovering()) {
			wingPosition += WING_SITTING_SPEED;
		} else if (slowedDownByBlock) {
			wingPosition += wingFactor * WING_SLOWED_FACTOR;
		} else {
			wingPosition += wingFactor;
		}
	}

	/**
	 * Выполняет серверный тик движения: обновляет фазу, вычисляет вектор к цели,
	 * применяет ускорение по рысканью и скорость, затем обновляет позиции частей тела.
	 */
	private void tickServerMovement(ServerWorld serverWorld) {
		Phase phase = phaseManager.getCurrent();
		phase.serverTick(serverWorld);

		if (phaseManager.getCurrent() != phase) {
			phase = phaseManager.getCurrent();
			phase.serverTick(serverWorld);
		}

		Vec3d pathTarget = phase.getPathTarget();
		if (pathTarget != null) {
			applyPathTargetMovement(phase, pathTarget);
		}

		if (hurtTime == 0) {
			applyWingCollisionDamage(serverWorld);
		}

		updateBodyPartPositions(serverWorld);
	}

	private void applyPathTargetMovement(Phase phase, Vec3d pathTarget) {
		double dx = pathTarget.x - getX();
		double dy = pathTarget.y - getY();
		double dz = pathTarget.z - getZ();
		double distanceSq = dx * dx + dy * dy + dz * dz;
		float maxYAccel = phase.getMaxYAcceleration();
		double horizontalDist = Math.sqrt(dx * dx + dz * dz);

		if (horizontalDist > 0.0) {
			dy = MathHelper.clamp(dy / horizontalDist, -maxYAccel, maxYAccel);
		}

		setVelocity(getVelocity().add(0.0, dy * 0.01, 0.0));
		setYaw(MathHelper.wrapDegrees(getYaw()));

		Vec3d toTarget = pathTarget.subtract(getX(), getY(), getZ()).normalize();
		Vec3d currentDir = new Vec3d(
				MathHelper.sin(getYaw() * (float) (Math.PI / 180.0)),
				getVelocity().y,
				-MathHelper.cos(getYaw() * (float) (Math.PI / 180.0))
		).normalize();

		float alignment = Math.max(((float) currentDir.dotProduct(toTarget) + 0.5F) / 1.5F, 0.0F);

		if (Math.abs(dx) > 1.0E-5F || Math.abs(dz) > 1.0E-5F) {
			float yawDelta = MathHelper.clamp(
					MathHelper.wrapDegrees(
							180.0F - (float) MathHelper.atan2(dx, dz) * (180.0F / (float) Math.PI) - getYaw()
					),
					-MAX_YAW_DELTA, MAX_YAW_DELTA
			);
			yawAcceleration *= YAW_ACCELERATION_DAMPING;
			yawAcceleration += yawDelta * phase.getYawAcceleration();
			setYaw(getYaw() + yawAcceleration * YAW_ACCELERATION_SCALE);
		}

		float distanceFactor = (float) (2.0 / (distanceSq + 1.0));
		updateVelocity(MOVEMENT_SPEED_FACTOR * (alignment * distanceFactor + (1.0F - distanceFactor)), new Vec3d(0.0, 0.0, -1.0));

		if (slowedDownByBlock) {
			move(MovementType.SELF, getVelocity().multiply(0.8F));
		} else {
			move(MovementType.SELF, getVelocity());
		}

		Vec3d normalizedVelocity = getVelocity().normalize();
		double lateralDamping = VELOCITY_LATERAL_SCALE + VELOCITY_LATERAL_BONUS * (normalizedVelocity.dotProduct(currentDir) + 1.0) / 2.0;
		setVelocity(getVelocity().multiply(lateralDamping, VELOCITY_DAMPING_XZ, lateralDamping));
	}

	private void applyWingCollisionDamage(ServerWorld serverWorld) {
		launchLivingEntities(
				serverWorld,
				serverWorld.getOtherEntities(
						this,
						rightWing.getBoundingBox().expand(4.0, 2.0, 4.0).offset(0.0, -2.0, 0.0),
						EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR
				)
		);
		launchLivingEntities(
				serverWorld,
				serverWorld.getOtherEntities(
						this,
						leftWing.getBoundingBox().expand(4.0, 2.0, 4.0).offset(0.0, -2.0, 0.0),
						EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR
				)
		);
		damageLivingEntities(
				serverWorld,
				serverWorld.getOtherEntities(
						this,
						head.getBoundingBox().expand(1.0),
						EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR
				)
		);
		damageLivingEntities(
				serverWorld,
				serverWorld.getOtherEntities(
						this,
						neck.getBoundingBox().expand(1.0),
						EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR
				)
		);
	}

	private void updateBodyParts() {
		Vec3d[] previousPositions = new Vec3d[parts.length];
		for (int idx = 0; idx < parts.length; idx++) {
			previousPositions[idx] = new Vec3d(parts[idx].getX(), parts[idx].getY(), parts[idx].getZ());
		}

		float pitchAngle = (float) (frameTracker.getFrame(PITCH_FRAME_NEAR).y() - frameTracker.getFrame(PITCH_FRAME_FAR).y())
				* PITCH_SCALE * (float) (Math.PI / 180.0);
		float pitchCos = MathHelper.cos(pitchAngle);
		float pitchSin = MathHelper.sin(pitchAngle);
		float yawRad = getYaw() * (float) (Math.PI / 180.0);
		float yawSin = MathHelper.sin(yawRad);
		float yawCos = MathHelper.cos(yawRad);

		movePart(body, yawSin * BODY_LATERAL_OFFSET, 0.0, -yawCos * BODY_LATERAL_OFFSET);
		movePart(rightWing, yawCos * WING_LATERAL_OFFSET, WING_VERTICAL_OFFSET, yawSin * WING_LATERAL_OFFSET);
		movePart(leftWing, yawCos * -WING_LATERAL_OFFSET, WING_VERTICAL_OFFSET, yawSin * -WING_LATERAL_OFFSET);

		float headYawSin = MathHelper.sin(getYaw() * (float) (Math.PI / 180.0) - yawAcceleration * YAW_ACCELERATION_SCALE);
		float headYawCos = MathHelper.cos(getYaw() * (float) (Math.PI / 180.0) - yawAcceleration * YAW_ACCELERATION_SCALE);
		float headVertical = getHeadVerticalMovement();

		movePart(head, headYawSin * HEAD_FORWARD_OFFSET * pitchCos, headVertical + pitchSin * HEAD_FORWARD_OFFSET, -headYawCos * HEAD_FORWARD_OFFSET * pitchCos);
		movePart(neck, headYawSin * NECK_FORWARD_OFFSET * pitchCos, headVertical + pitchSin * NECK_FORWARD_OFFSET, -headYawCos * NECK_FORWARD_OFFSET * pitchCos);

		EnderDragonFrameTracker.Frame baseFrame = frameTracker.getFrame(PITCH_FRAME_NEAR);
		EnderDragonPart[] tailParts = {tail1, tail2, tail3};

		for (int tailIdx = 0; tailIdx < TAIL_SEGMENT_COUNT; tailIdx++) {
			EnderDragonFrameTracker.Frame tailFrame = frameTracker.getFrame(TAIL_FRAME_BASE + tailIdx * TAIL_FRAME_STEP);
			float tailAngle = getYaw() * (float) (Math.PI / 180.0)
					+ wrapYawChange(tailFrame.yRot() - baseFrame.yRot()) * (float) (Math.PI / 180.0);
			float tailSin = MathHelper.sin(tailAngle);
			float tailCos = MathHelper.cos(tailAngle);
			float tailOffset = (tailIdx + 1) * 2.0F;

			movePart(
					tailParts[tailIdx],
					-(yawSin * TAIL_SEGMENT_SPACING + tailSin * tailOffset) * pitchCos,
					tailFrame.y() - baseFrame.y() - (tailOffset + TAIL_SEGMENT_SPACING) * pitchSin + 1.5,
					(yawCos * TAIL_SEGMENT_SPACING + tailCos * tailOffset) * pitchCos
			);
		}

		for (int idx = 0; idx < parts.length; idx++) {
			parts[idx].lastX = previousPositions[idx].x;
			parts[idx].lastY = previousPositions[idx].y;
			parts[idx].lastZ = previousPositions[idx].z;
			parts[idx].lastRenderX = previousPositions[idx].x;
			parts[idx].lastRenderY = previousPositions[idx].y;
			parts[idx].lastRenderZ = previousPositions[idx].z;
		}
	}

	private void updateBodyPartPositions(ServerWorld serverWorld) {
		slowedDownByBlock = destroyBlocks(serverWorld, head.getBoundingBox())
				| destroyBlocks(serverWorld, neck.getBoundingBox())
				| destroyBlocks(serverWorld, body.getBoundingBox());

		if (fight != null) {
			fight.updateFight(this);
		}
	}

	private void movePart(EnderDragonPart part, double dx, double dy, double dz) {
		part.setPosition(getX() + dx, getY() + dy, getZ() + dz);
	}

	private float getHeadVerticalMovement() {
		if (phaseManager.getCurrent().isSittingOrHovering()) {
			return -1.0F;
		}

		EnderDragonFrameTracker.Frame nearFrame = frameTracker.getFrame(PITCH_FRAME_NEAR);
		EnderDragonFrameTracker.Frame originFrame = frameTracker.getFrame(0);
		return (float) (nearFrame.y() - originFrame.y());
	}

	private void tickWithEndCrystals() {
		if (connectedCrystal != null) {
			if (connectedCrystal.isRemoved()) {
				connectedCrystal = null;
			} else if (age % CRYSTAL_SEARCH_INTERVAL == 0 && getHealth() < getMaxHealth()) {
				setHealth(getHealth() + 1.0F);
			}
		}

		if (random.nextInt(CRYSTAL_SEARCH_INTERVAL) == 0) {
			List<EndCrystalEntity> nearbyCrystals = getEntityWorld()
					.getNonSpectatingEntities(EndCrystalEntity.class, getBoundingBox().expand(CRYSTAL_SEARCH_RADIUS));
			EndCrystalEntity nearest = null;
			double minDistSq = Double.MAX_VALUE;

			for (EndCrystalEntity crystal : nearbyCrystals) {
				double distSq = crystal.squaredDistanceTo(this);
				if (distSq < minDistSq) {
					minDistSq = distSq;
					nearest = crystal;
				}
			}

			connectedCrystal = nearest;
		}
	}

	private void launchLivingEntities(ServerWorld world, List<Entity> entities) {
		double bodyCenterX = (body.getBoundingBox().minX + body.getBoundingBox().maxX) / 2.0;
		double bodyCenterZ = (body.getBoundingBox().minZ + body.getBoundingBox().maxZ) / 2.0;

		for (Entity entity : entities) {
			if (!(entity instanceof LivingEntity livingEntity)) {
				continue;
			}

			double relX = entity.getX() - bodyCenterX;
			double relZ = entity.getZ() - bodyCenterZ;
			double distSq = Math.max(relX * relX + relZ * relZ, MIN_HORIZONTAL_DIST_SQ);
			entity.addVelocity(relX / distSq * WING_LAUNCH_FORCE, WING_LAUNCH_VERTICAL, relZ / distSq * WING_LAUNCH_FORCE);

			if (phaseManager.getCurrent().isSittingOrHovering()) {
				continue;
			}

			if (livingEntity.getLastAttackedTime() < entity.age - 2) {
				DamageSource damageSource = getDamageSources().mobAttack(this);
				entity.damage(world, damageSource, WING_DAMAGE);
				EnchantmentHelper.onTargetDamaged(world, entity, damageSource);
			}
		}
	}

	private void damageLivingEntities(ServerWorld world, List<Entity> entities) {
		for (Entity entity : entities) {
			if (!(entity instanceof LivingEntity)) {
				continue;
			}

			DamageSource damageSource = getDamageSources().mobAttack(this);
			entity.damage(world, damageSource, HEAD_DAMAGE);
			EnchantmentHelper.onTargetDamaged(world, entity, damageSource);
		}
	}

	private float wrapYawChange(double yawDegrees) {
		return (float) MathHelper.wrapDegrees(yawDegrees);
	}

	private boolean destroyBlocks(ServerWorld world, Box box) {
		int minX = MathHelper.floor(box.minX);
		int minY = MathHelper.floor(box.minY);
		int minZ = MathHelper.floor(box.minZ);
		int maxX = MathHelper.floor(box.maxX);
		int maxY = MathHelper.floor(box.maxY);
		int maxZ = MathHelper.floor(box.maxZ);
		boolean blockedByImmune = false;
		boolean destroyedAny = false;

		for (int bx = minX; bx <= maxX; bx++) {
			for (int by = minY; by <= maxY; by++) {
				for (int bz = minZ; bz <= maxZ; bz++) {
					BlockPos blockPos = new BlockPos(bx, by, bz);
					BlockState blockState = world.getBlockState(blockPos);

					if (blockState.isAir() || blockState.isIn(BlockTags.DRAGON_TRANSPARENT)) {
						continue;
					}

					if (world.getGameRules().getValue(GameRules.DO_MOB_GRIEFING)
							&& !blockState.isIn(BlockTags.DRAGON_IMMUNE)) {
						destroyedAny = world.removeBlock(blockPos, false) || destroyedAny;
					} else {
						blockedByImmune = true;
					}
				}
			}
		}

		if (destroyedAny) {
			BlockPos particlePos = new BlockPos(
					minX + random.nextInt(maxX - minX + 1),
					minY + random.nextInt(maxY - minY + 1),
					minZ + random.nextInt(maxZ - minZ + 1)
			);
			world.syncWorldEvent(WORLD_EVENT_BLOCK_BREAK, particlePos, 0);
		}

		return blockedByImmune;
	}

	/**
		* Наносит урон конкретной части тела дракона. Если дракон сидит/парит,
		* накапливает {@code damageDuringSitting} и при превышении порога {@code TAKEOFF_THRESHOLD}
		* принудительно переводит дракона в фазу взлёта.
		*/
	public boolean damagePart(ServerWorld world, EnderDragonPart part, DamageSource source, float amount) {
		if (phaseManager.getCurrent().getType() == PhaseType.DYING) {
			return false;
		}

		amount = phaseManager.getCurrent().modifyDamageTaken(source, amount);

		if (part != head) {
			amount = amount / BODY_DAMAGE_REDUCTION + Math.min(amount, BODY_DAMAGE_MIN);
		}

		if (amount < DAMAGE_THRESHOLD) {
			return false;
		}

		if (source.getAttacker() instanceof PlayerEntity
				|| source.isIn(DamageTypeTags.ALWAYS_HURTS_ENDER_DRAGONS)) {
			float healthBefore = getHealth();
			parentDamage(world, source, amount);

			if (isDead() && !phaseManager.getCurrent().isSittingOrHovering()) {
				setHealth(1.0F);
				phaseManager.setPhase(PhaseType.DYING);
			}

			if (phaseManager.getCurrent().isSittingOrHovering()) {
				damageDuringSitting += healthBefore - getHealth();
				if (damageDuringSitting > TAKEOFF_THRESHOLD * getMaxHealth()) {
					damageDuringSitting = 0.0F;
					phaseManager.setPhase(PhaseType.TAKEOFF);
				}
			}
		}

		return true;
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		return damagePart(world, body, source, amount);
	}

	protected void parentDamage(ServerWorld world, DamageSource source, float amount) {
		super.damage(world, source, amount);
	}

	@Override
	public void kill(ServerWorld world) {
		remove(Entity.RemovalReason.KILLED);
		emitGameEvent(GameEvent.ENTITY_DIE);

		if (fight != null) {
			fight.updateFight(this);
			fight.dragonKilled(this);
		}
	}

	@Override
	protected void updatePostDeath() {
		if (fight != null) {
			fight.updateFight(this);
		}

		ticksSinceDeath++;

		if (ticksSinceDeath >= DEATH_PARTICLE_START_TICK && ticksSinceDeath <= DEATH_TOTAL_TICKS) {
			float offsetX = (random.nextFloat() - 0.5F) * DEATH_EXPLOSION_SPREAD_XZ;
			float offsetY = (random.nextFloat() - 0.5F) * DEATH_EXPLOSION_SPREAD_Y;
			float offsetZ = (random.nextFloat() - 0.5F) * DEATH_EXPLOSION_SPREAD_XZ;
			getEntityWorld().addParticleClient(
					ParticleTypes.EXPLOSION_EMITTER,
					getX() + offsetX,
					getY() + 2.0 + offsetY,
					getZ() + offsetZ,
					0.0, 0.0, 0.0
			);
		}

		int totalXp = fight != null && !fight.hasPreviouslyKilled()
				? DEATH_XP_FIRST_KILL
				: DEATH_XP_REPEAT_KILL;

		if (getEntityWorld() instanceof ServerWorld serverWorld) {
			if (ticksSinceDeath > DEATH_XP_START_TICK
					&& ticksSinceDeath % DEATH_XP_TICK_INTERVAL == 0
					&& serverWorld.getGameRules().getValue(GameRules.DO_MOB_LOOT)) {
				ExperienceOrbEntity.spawn(serverWorld, getEntityPos(), MathHelper.floor(totalXp * DEATH_XP_PERIODIC_FRACTION));
			}

			if (ticksSinceDeath == 1 && !isSilent()) {
				serverWorld.syncGlobalEvent(WORLD_EVENT_DRAGON_DEATH, getBlockPos(), 0);
			}
		}

		Vec3d riseVec = new Vec3d(0.0, DEATH_RISE_SPEED, 0.0);
		move(MovementType.SELF, riseVec);

		for (EnderDragonPart part : parts) {
			part.resetPosition();
			part.setPosition(part.getEntityPos().add(riseVec));
		}

		if (ticksSinceDeath == DEATH_TOTAL_TICKS && getEntityWorld() instanceof ServerWorld serverWorld2) {
			if (serverWorld2.getGameRules().getValue(GameRules.DO_MOB_LOOT)) {
				ExperienceOrbEntity.spawn(serverWorld2, getEntityPos(), MathHelper.floor(totalXp * DEATH_XP_FINAL_FRACTION));
			}

			if (fight != null) {
				fight.dragonKilled(this);
			}

			remove(Entity.RemovalReason.KILLED);
			emitGameEvent(GameEvent.ENTITY_DIE);
		}
	}

	/**
		* Инициализирует 24 узла пути облёта острова (три кольца: внешнее 12 узлов r=60,
		* среднее 8 узлов r=40, внутреннее 4 узла r=20) и возвращает индекс ближайшего
		* к текущей позиции дракона узла.
		*/
	public int getNearestPathNodeIndex() {
		if (pathNodes[0] == null) {
			initPathNodes();
		}

		return getNearestPathNodeIndex(getX(), getY(), getZ());
	}

	private void initPathNodes() {
		for (int nodeIdx = 0; nodeIdx < PATH_NODE_COUNT; nodeIdx++) {
			int heightOffset = OUTER_RING_HEIGHT_OFFSET;
			int nodeX;
			int nodeZ;

			if (nodeIdx < OUTER_RING_COUNT) {
				nodeX = MathHelper.floor(OUTER_RING_RADIUS * MathHelper.cos(2.0F * ((float) -Math.PI + (float) (Math.PI / 12) * nodeIdx)));
				nodeZ = MathHelper.floor(OUTER_RING_RADIUS * MathHelper.sin(2.0F * ((float) -Math.PI + (float) (Math.PI / 12) * nodeIdx)));
			} else if (nodeIdx < MIDDLE_RING_START + MIDDLE_RING_COUNT) {
				int ringIdx = nodeIdx - MIDDLE_RING_START;
				nodeX = MathHelper.floor(MIDDLE_RING_RADIUS * MathHelper.cos(2.0F * ((float) -Math.PI + (float) (Math.PI / 8) * ringIdx)));
				nodeZ = MathHelper.floor(MIDDLE_RING_RADIUS * MathHelper.sin(2.0F * ((float) -Math.PI + (float) (Math.PI / 8) * ringIdx)));
				heightOffset += MIDDLE_RING_HEIGHT_OFFSET;
			} else {
				int ringIdx = nodeIdx - INNER_RING_START;
				nodeX = MathHelper.floor(INNER_RING_RADIUS * MathHelper.cos(2.0F * ((float) -Math.PI + (float) (Math.PI / 4) * ringIdx)));
				nodeZ = MathHelper.floor(INNER_RING_RADIUS * MathHelper.sin(2.0F * ((float) -Math.PI + (float) (Math.PI / 4) * ringIdx)));
			}

			int nodeY = Math.max(
					MIN_PATH_NODE_Y,
					getEntityWorld()
							.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, new BlockPos(nodeX, 0, nodeZ))
							.getY() + heightOffset
			);
			pathNodes[nodeIdx] = new PathNode(nodeX, nodeY, nodeZ);
		}

		// Предвычисленные битовые маски смежности для графа из 24 узлов трёх колец
		pathNodeConnections[0] = 6146;
		pathNodeConnections[1] = 8197;
		pathNodeConnections[2] = 8202;
		pathNodeConnections[3] = 16404;
		pathNodeConnections[4] = 32808;
		pathNodeConnections[5] = 32848;
		pathNodeConnections[6] = 65696;
		pathNodeConnections[7] = 131392;
		pathNodeConnections[8] = 131712;
		pathNodeConnections[9] = 263424;
		pathNodeConnections[10] = 526848;
		pathNodeConnections[11] = 525313;
		pathNodeConnections[12] = 1581057;
		pathNodeConnections[13] = 3166214;
		pathNodeConnections[14] = 2138120;
		pathNodeConnections[15] = 6373424;
		pathNodeConnections[16] = 4358208;
		pathNodeConnections[17] = 12910976;
		pathNodeConnections[18] = 9044480;
		pathNodeConnections[19] = 9706496;
		pathNodeConnections[20] = 15216640;
		pathNodeConnections[21] = 13688832;
		pathNodeConnections[22] = 11763712;
		pathNodeConnections[23] = 8257536;
	}

	public int getNearestPathNodeIndex(double x, double y, double z) {
		float minDistSq = 10000.0F;
		int nearestIdx = 0;
		PathNode queryNode = new PathNode(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
		int startIdx = fight == null || fight.getAliveEndCrystals() == 0 ? OUTER_RING_COUNT : 0;

		for (int nodeIdx = startIdx; nodeIdx < PATH_NODE_COUNT; nodeIdx++) {
			if (pathNodes[nodeIdx] == null) {
				continue;
			}

			float distSq = pathNodes[nodeIdx].getSquaredDistance(queryNode);
			if (distSq < minDistSq) {
				minDistSq = distSq;
				nearestIdx = nodeIdx;
			}
		}

		return nearestIdx;
	}

	/**
		* Выполняет поиск пути алгоритмом A* по графу из 24 узлов от узла {@code from} до {@code to}.
		* Если кристаллы живы, поиск ограничен только внешним кольцом (узлы 0–11).
		*
		* @param from      индекс начального узла
		* @param to        индекс целевого узла
		* @param extraNode дополнительный узел, добавляемый в конец пути (например, точка посадки)
		* @return найденный путь или {@code null}, если путь не найден
		*/
	public @Nullable Path findPath(int from, int to, @Nullable PathNode extraNode) {
		for (int nodeIdx = 0; nodeIdx < PATH_NODE_COUNT; nodeIdx++) {
			PathNode node = pathNodes[nodeIdx];
			node.visited = false;
			node.heapWeight = 0.0F;
			node.penalizedPathLength = 0.0F;
			node.distanceToNearestTarget = 0.0F;
			node.previous = null;
			node.heapIndex = -1;
		}

		PathNode startNode = pathNodes[from];
		PathNode targetNode = pathNodes[to];
		startNode.penalizedPathLength = 0.0F;
		startNode.distanceToNearestTarget = startNode.getDistance(targetNode);
		startNode.heapWeight = startNode.distanceToNearestTarget;
		pathHeap.clear();
		pathHeap.push(startNode);

		PathNode closestNode = startNode;
		int searchStart = fight == null || fight.getAliveEndCrystals() == 0 ? OUTER_RING_COUNT : 0;

		while (!pathHeap.isEmpty()) {
			PathNode current = pathHeap.pop();

			if (current.equals(targetNode)) {
				if (extraNode != null) {
					extraNode.previous = targetNode;
					targetNode = extraNode;
				}

				return getPathOfAllPredecessors(startNode, targetNode);
			}

			if (current.getDistance(targetNode) < closestNode.getDistance(targetNode)) {
				closestNode = current;
			}

			current.visited = true;
			int currentIdx = 0;

			for (int nodeIdx = 0; nodeIdx < PATH_NODE_COUNT; nodeIdx++) {
				if (pathNodes[nodeIdx] == current) {
					currentIdx = nodeIdx;
					break;
				}
			}

			for (int neighborIdx = searchStart; neighborIdx < PATH_NODE_COUNT; neighborIdx++) {
				if ((pathNodeConnections[currentIdx] & 1 << neighborIdx) == 0) {
					continue;
				}

				PathNode neighbor = pathNodes[neighborIdx];
				if (neighbor.visited) {
					continue;
				}

				float newPathLength = current.penalizedPathLength + current.getDistance(neighbor);
				if (neighbor.isInHeap() && newPathLength >= neighbor.penalizedPathLength) {
					continue;
				}

				neighbor.previous = current;
				neighbor.penalizedPathLength = newPathLength;
				neighbor.distanceToNearestTarget = neighbor.getDistance(targetNode);

				if (neighbor.isInHeap()) {
					pathHeap.setNodeWeight(neighbor, neighbor.penalizedPathLength + neighbor.distanceToNearestTarget);
				} else {
					neighbor.heapWeight = neighbor.penalizedPathLength + neighbor.distanceToNearestTarget;
					pathHeap.push(neighbor);
				}
			}
		}

		if (closestNode == startNode) {
			return null;
		}

		LOGGER.debug("Failed to find path from {} to {}", from, to);

		if (extraNode != null) {
			extraNode.previous = closestNode;
			closestNode = extraNode;
		}

		return getPathOfAllPredecessors(startNode, closestNode);
	}

	private Path getPathOfAllPredecessors(PathNode start, PathNode end) {
		List<PathNode> pathList = new ArrayList<>();
		PathNode current = end;
		pathList.add(0, end);

		while (current.previous != null) {
			current = current.previous;
			pathList.add(0, current);
		}

		return new Path(pathList, new BlockPos(end.x, end.y, end.z), true);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putInt("DragonPhase", phaseManager.getCurrent().getType().getTypeId());
		view.putInt("DragonDeathTime", ticksSinceDeath);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		view.getOptionalInt("DragonPhase").ifPresent(phase -> phaseManager.setPhase(PhaseType.getFromId(phase)));
		ticksSinceDeath = view.getInt("DragonDeathTime", 0);
	}

	@Override
	public void checkDespawn() {
	}

	public EnderDragonPart[] getBodyParts() {
		return parts;
	}

	@Override
	public boolean canHit() {
		return false;
	}

	@Override
	public SoundCategory getSoundCategory() {
		return SoundCategory.HOSTILE;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_ENDER_DRAGON_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_ENDER_DRAGON_HURT;
	}

	@Override
	protected float getSoundVolume() {
		return 5.0F;
	}

	public Vec3d getRotationVectorFromPhase(float tickProgress) {
		Phase phase = phaseManager.getCurrent();
		PhaseType<? extends Phase> phaseType = phase.getType();

		if (phaseType == PhaseType.LANDING || phaseType == PhaseType.TAKEOFF) {
			BlockPos topPos = getEntityWorld().getTopPosition(
					Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
					EndPortalFeature.offsetOrigin(fightOrigin)
			);
			float distFactor = Math.max((float) Math.sqrt(topPos.getSquaredDistance(getEntityPos())) / LANDING_MIN_DISTANCE_FACTOR, LANDING_MAX_PITCH_SCALE);
			float pitchScale = LANDING_PITCH_FACTOR / distFactor;
			float savedPitch = getPitch();
			setPitch(-pitchScale * LANDING_PITCH_MULTIPLIER * LANDING_PITCH_DEGREES);
			Vec3d vec = getRotationVec(tickProgress);
			setPitch(savedPitch);
			return vec;
		}

		if (phase.isSittingOrHovering()) {
			float savedPitch = getPitch();
			setPitch(SITTING_PITCH_DEGREES);
			Vec3d vec = getRotationVec(tickProgress);
			setPitch(savedPitch);
			return vec;
		}

		return getRotationVec(tickProgress);
	}

	public void crystalDestroyed(ServerWorld world, EndCrystalEntity crystal, BlockPos pos, DamageSource source) {
		PlayerEntity attacker = source.getAttacker() instanceof PlayerEntity player
				? player
				: world.getClosestPlayer(CLOSE_PLAYER_PREDICATE, pos.getX(), pos.getY(), pos.getZ());

		if (crystal == connectedCrystal) {
			damagePart(world, head, getDamageSources().explosion(crystal, attacker), CRYSTAL_EXPLOSION_DAMAGE);
		}

		phaseManager.getCurrent().crystalDestroyed(crystal, pos, source, attacker);
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		if (PHASE_TYPE.equals(data) && getEntityWorld().isClient()) {
			phaseManager.setPhase(PhaseType.getFromId(getDataTracker().get(PHASE_TYPE)));
		}

		super.onTrackedDataSet(data);
	}

	public PhaseManager getPhaseManager() {
		return phaseManager;
	}

	public @Nullable EnderDragonFight getFight() {
		return fight;
	}

	@Override
	public boolean addStatusEffect(StatusEffectInstance effect, @Nullable Entity source) {
		return false;
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
	public void onSpawnPacket(EntitySpawnS2CPacket packet) {
		super.onSpawnPacket(packet);
		EnderDragonPart[] bodyParts = getBodyParts();

		for (int idx = 0; idx < bodyParts.length; idx++) {
			bodyParts[idx].setId(idx + packet.getEntityId() + 1);
		}
	}

	@Override
	public boolean canTarget(LivingEntity target) {
		return target.canTakeDamage();
	}

	@Override
	protected float clampScale(float scale) {
		return 1.0F;
	}
}
