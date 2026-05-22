package net.minecraft.entity.mob;

import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.raid.Raid;
import net.minecraft.world.*;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

/**
 * Базовый класс для патрулирующих мобов (пиллагеры, капитаны).
 */
public abstract class PatrolEntity extends HostileEntity {

	private static final float PATROL_LEADER_SPAWN_CHANCE = 0.06F;
	private static final int MAX_BLOCK_LIGHT_FOR_SPAWN = 8;
	private static final double DESPAWN_DISTANCE_SQUARED = 16384.0;
	private static final int PATROL_RANDOM_RANGE = 500;
	private @Nullable BlockPos patrolTarget;
	private boolean patrolLeader;
	private boolean patrolling;

	protected PatrolEntity(EntityType<? extends PatrolEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	protected void initGoals() {
		super.initGoals();
		goalSelector.add(4, new PatrolEntity.PatrolGoal<>(this, 0.7, 0.595));
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putNullable("patrol_target", BlockPos.CODEC, patrolTarget);
		view.putBoolean("PatrolLeader", patrolLeader);
		view.putBoolean("Patrolling", patrolling);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		patrolTarget = view.<BlockPos>read("patrol_target", BlockPos.CODEC).orElse(null);
		patrolLeader = view.getBoolean("PatrolLeader", false);
		patrolling = view.getBoolean("Patrolling", false);
	}

	public boolean canLead() {
		return true;
	}

	@Override
	public @Nullable EntityData initialize(
			ServerWorldAccess world,
			LocalDifficulty difficulty,
			SpawnReason spawnReason,
			@Nullable EntityData entityData
	) {
		if (spawnReason != SpawnReason.PATROL
				&& spawnReason != SpawnReason.EVENT
				&& spawnReason != SpawnReason.STRUCTURE
				&& world.getRandom().nextFloat() < PATROL_LEADER_SPAWN_CHANCE
				&& canLead()) {
			patrolLeader = true;
		}

		if (isPatrolLeader()) {
			equipStack(
					EquipmentSlot.HEAD,
					Raid.createOminousBanner(getRegistryManager().getOrThrow(RegistryKeys.BANNER_PATTERN))
			);
			setEquipmentDropChance(EquipmentSlot.HEAD, 2.0F);
		}

		if (spawnReason == SpawnReason.PATROL) {
			patrolling = true;
		}

		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	public static boolean canSpawn(
			EntityType<? extends PatrolEntity> type,
			WorldAccess world,
			SpawnReason spawnReason,
			BlockPos pos,
			Random random
	) {
		if (world.getLightLevel(LightType.BLOCK, pos) > MAX_BLOCK_LIGHT_FOR_SPAWN) {
			return false;
		}

		return canSpawnIgnoreLightLevel(type, world, spawnReason, pos, random);
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return !patrolling || distanceSquared > DESPAWN_DISTANCE_SQUARED;
	}

	public void setPatrolTarget(BlockPos targetPos) {
		patrolTarget = targetPos;
		patrolling = true;
	}

	public @Nullable BlockPos getPatrolTarget() {
		return patrolTarget;
	}

	public boolean hasPatrolTarget() {
		return patrolTarget != null;
	}

	public void setPatrolLeader(boolean patrolLeader) {
		this.patrolLeader = patrolLeader;
		patrolling = true;
	}

	public boolean isPatrolLeader() {
		return patrolLeader;
	}

	public boolean hasNoRaid() {
		return true;
	}

	public void setRandomPatrolTarget() {
		patrolTarget = getBlockPos().add(
				-PATROL_RANDOM_RANGE + random.nextInt(PATROL_RANDOM_RANGE * 2),
				0,
				-PATROL_RANDOM_RANGE + random.nextInt(PATROL_RANDOM_RANGE * 2)
		);
		patrolling = true;
	}

	protected boolean isRaidCenterSet() {
		return patrolling;
	}

	protected void setPatrolling(boolean patrolling) {
		this.patrolling = patrolling;
	}

	public static class PatrolGoal<T extends PatrolEntity> extends Goal {

		private static final int PATROL_SEARCH_COOLDOWN = 200;
		private static final double PATROL_NEAR_DISTANCE = 10.0;
		private static final double PATROL_SIDE_OFFSET = 0.4;
		private static final double PATROL_AHEAD_DISTANCE = 10.0;
		private static final int WANDER_RANGE = 8;
		private final T entity;
		private final double leaderSpeed;
		private final double followSpeed;
		private long nextPatrolSearchTime;

		public PatrolGoal(T entity, double leaderSpeed, double followSpeed) {
			this.entity = entity;
			this.leaderSpeed = leaderSpeed;
			this.followSpeed = followSpeed;
			nextPatrolSearchTime = -1L;
			setControls(EnumSet.of(Goal.Control.MOVE));
		}

		@Override
		public boolean canStart() {
			boolean onCooldown = entity.getEntityWorld().getTime() < nextPatrolSearchTime;
			return entity.isRaidCenterSet()
					&& entity.getTarget() == null
					&& !entity.hasControllingPassenger()
					&& entity.hasPatrolTarget()
					&& !onCooldown;
		}

		@Override
		public void start() {
		}

		@Override
		public void stop() {
		}

		@Override
		public void tick() {
			boolean isLeader = entity.isPatrolLeader();
			EntityNavigation navigation = entity.getNavigation();
			if (!navigation.isIdle()) {
				return;
			}

			List<PatrolEntity> patrolMembers = findPatrolTargets();
			if (entity.isRaidCenterSet() && patrolMembers.isEmpty()) {
				entity.setPatrolling(false);
			}
			else if (isLeader && entity.getPatrolTarget().isWithinDistance(entity.getEntityPos(), PATROL_NEAR_DISTANCE)) {
				entity.setRandomPatrolTarget();
			}
			else {
				Vec3d targetCenter = Vec3d.ofBottomCenter(entity.getPatrolTarget());
				Vec3d entityPos = entity.getEntityPos();
				Vec3d toTarget = entityPos.subtract(targetCenter);
				Vec3d sideOffset = toTarget.rotateY(90.0F).multiply(PATROL_SIDE_OFFSET).add(targetCenter);
				Vec3d moveTarget = sideOffset.subtract(entityPos).normalize().multiply(PATROL_AHEAD_DISTANCE).add(entityPos);
				BlockPos movePos = BlockPos.ofFloored(moveTarget);
				movePos = entity.getEntityWorld().getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, movePos);
				double speed = isLeader ? followSpeed : leaderSpeed;
				if (!navigation.startMovingTo(movePos.getX(), movePos.getY(), movePos.getZ(), speed)) {
					wander();
					nextPatrolSearchTime = entity.getEntityWorld().getTime() + PATROL_SEARCH_COOLDOWN;
				}
				else if (isLeader) {
					for (PatrolEntity member : patrolMembers) {
						member.setPatrolTarget(movePos);
					}
				}
			}
		}

		private List<PatrolEntity> findPatrolTargets() {
			return entity
					.getEntityWorld()
					.getEntitiesByClass(
							PatrolEntity.class,
							entity.getBoundingBox().expand(16.0),
							patrolEntity -> patrolEntity.hasNoRaid() && !patrolEntity.isPartOf(entity)
					);
		}

		private boolean wander() {
			Random random = entity.getRandom();
			BlockPos wanderPos = entity
					.getEntityWorld()
					.getTopPosition(
							Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
							entity.getBlockPos().add(
									-WANDER_RANGE + random.nextInt(WANDER_RANGE * 2),
									0,
									-WANDER_RANGE + random.nextInt(WANDER_RANGE * 2)
							)
					);
			return entity.getNavigation().startMovingTo(wanderPos.getX(), wanderPos.getY(), wanderPos.getZ(), leaderSpeed);
		}
	}
}
