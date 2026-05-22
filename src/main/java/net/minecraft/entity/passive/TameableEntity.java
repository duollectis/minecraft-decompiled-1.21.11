package net.minecraft.entity.passive;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Базовый класс для прирученных существ (волки, кошки и т.д.).
 * Управляет флагами приручения и сидения через битовое поле {@code TAMEABLE_FLAGS},
 * а также логикой телепортации к владельцу.
 */
public abstract class TameableEntity extends AnimalEntity implements Tameable {

	public static final int MAX_HEAD_ROTATION_ANGLE = 144;

	/** Минимальное расстояние (в блоках²) до владельца, при котором питомец телепортируется. */
	private static final double TELEPORT_DISTANCE_SQUARED = 144.0;
	private static final int TELEPORT_ATTEMPTS = 10;
	private static final int TELEPORT_RADIUS = 3;
	private static final int TELEPORT_HEIGHT_RANGE = 1;

	/** Битовая маска флага «сидит» в {@code TAMEABLE_FLAGS}. */
	private static final int SITTING_FLAG_MASK = 0x01;
	/** Битовая маска флага «приручён» в {@code TAMEABLE_FLAGS}. */
	private static final int TAMED_FLAG_MASK = 0x04;

	private static final int TAME_SUCCESS_STATUS = 7;
	private static final int TAME_FAIL_STATUS = 6;
	private static final int EMOTE_PARTICLE_COUNT = 7;

	protected static final TrackedData<Byte> TAMEABLE_FLAGS = DataTracker.registerData(
		TameableEntity.class,
		TrackedDataHandlerRegistry.BYTE
	);
	protected static final TrackedData<Optional<LazyEntityReference<LivingEntity>>> OWNER_UUID = DataTracker.registerData(
		TameableEntity.class,
		TrackedDataHandlerRegistry.LAZY_ENTITY_REFERENCE
	);

	private boolean sitting = false;

	protected TameableEntity(EntityType<? extends TameableEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(TAMEABLE_FLAGS, (byte) 0);
		builder.add(OWNER_UUID, Optional.empty());
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		LazyEntityReference<LivingEntity> ownerRef = getOwnerReference();
		LazyEntityReference.writeData(ownerRef, view, "Owner");
		view.putBoolean("Sitting", sitting);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		LazyEntityReference<LivingEntity> ownerRef = LazyEntityReference.fromDataOrPlayerName(
			view,
			"Owner",
			getEntityWorld()
		);
		if (ownerRef != null) {
			try {
				dataTracker.set(OWNER_UUID, Optional.of(ownerRef));
				setTamed(true, false);
			} catch (Throwable ignored) {
				setTamed(false, true);
			}
		} else {
			dataTracker.set(OWNER_UUID, Optional.empty());
			setTamed(false, true);
		}

		sitting = view.getBoolean("Sitting", false);
		setInSittingPose(sitting);
	}

	@Override
	public boolean canBeLeashed() {
		return true;
	}

	/**
	 * Показывает частицы эмоции: сердечки при успехе приручения, дым при неудаче.
	 *
	 * @param positive {@code true} — сердечки, {@code false} — дым
	 */
	protected void showEmoteParticle(boolean positive) {
		ParticleEffect particle = positive ? ParticleTypes.HEART : ParticleTypes.SMOKE;
		for (int count = 0; count < EMOTE_PARTICLE_COUNT; count++) {
			double vx = random.nextGaussian() * 0.02;
			double vy = random.nextGaussian() * 0.02;
			double vz = random.nextGaussian() * 0.02;
			getEntityWorld().addParticleClient(
				particle,
				getParticleX(1.0),
				getRandomBodyY() + 0.5,
				getParticleZ(1.0),
				vx,
				vy,
				vz
			);
		}
	}

	@Override
	public void handleStatus(byte status) {
		if (status == TAME_SUCCESS_STATUS) {
			showEmoteParticle(true);
		} else if (status == TAME_FAIL_STATUS) {
			showEmoteParticle(false);
		} else {
			super.handleStatus(status);
		}
	}

	public boolean isTamed() {
		return (dataTracker.get(TAMEABLE_FLAGS) & TAMED_FLAG_MASK) != 0;
	}

	public void setTamed(boolean tamed, boolean updateAttributes) {
		byte flags = dataTracker.get(TAMEABLE_FLAGS);
		if (tamed) {
			dataTracker.set(TAMEABLE_FLAGS, (byte) (flags | TAMED_FLAG_MASK));
		} else {
			dataTracker.set(TAMEABLE_FLAGS, (byte) (flags & ~TAMED_FLAG_MASK));
		}

		if (updateAttributes) {
			updateAttributesForTamed();
		}
	}

	protected void updateAttributesForTamed() {
	}

	public boolean isInSittingPose() {
		return (dataTracker.get(TAMEABLE_FLAGS) & SITTING_FLAG_MASK) != 0;
	}

	public void setInSittingPose(boolean inSittingPose) {
		byte flags = dataTracker.get(TAMEABLE_FLAGS);
		if (inSittingPose) {
			dataTracker.set(TAMEABLE_FLAGS, (byte) (flags | SITTING_FLAG_MASK));
		} else {
			dataTracker.set(TAMEABLE_FLAGS, (byte) (flags & ~SITTING_FLAG_MASK));
		}
	}

	@Override
	public @Nullable LazyEntityReference<LivingEntity> getOwnerReference() {
		return dataTracker.get(OWNER_UUID).orElse(null);
	}

	public void setOwner(@Nullable LivingEntity owner) {
		dataTracker.set(OWNER_UUID, Optional.ofNullable(owner).map(LazyEntityReference::of));
	}

	public void setOwner(@Nullable LazyEntityReference<LivingEntity> owner) {
		dataTracker.set(OWNER_UUID, Optional.ofNullable(owner));
	}

	public void setTamedBy(PlayerEntity player) {
		setTamed(true, true);
		setOwner(player);
		if (player instanceof ServerPlayerEntity serverPlayer) {
			Criteria.TAME_ANIMAL.trigger(serverPlayer, this);
		}
	}

	@Override
	public boolean canTarget(LivingEntity target) {
		return isOwner(target) ? false : super.canTarget(target);
	}

	public boolean isOwner(LivingEntity entity) {
		return entity == getOwner();
	}

	public boolean canAttackWithOwner(LivingEntity target, LivingEntity owner) {
		return true;
	}

	@Override
	public @Nullable Team getScoreboardTeam() {
		Team team = super.getScoreboardTeam();
		if (team != null) {
			return team;
		}

		if (isTamed()) {
			LivingEntity topOwner = getTopLevelOwner();
			if (topOwner != null) {
				return topOwner.getScoreboardTeam();
			}
		}

		return null;
	}

	@Override
	public boolean isInSameTeam(Entity other) {
		if (isTamed()) {
			LivingEntity topOwner = getTopLevelOwner();
			if (other == topOwner) {
				return true;
			}

			if (topOwner != null) {
				return topOwner.isInSameTeam(other);
			}
		}

		return super.isInSameTeam(other);
	}

	@Override
	public void onDeath(DamageSource damageSource) {
		if (getEntityWorld() instanceof ServerWorld serverWorld
			&& serverWorld.getGameRules().getValue(GameRules.SHOW_DEATH_MESSAGES)
			&& getOwner() instanceof ServerPlayerEntity serverPlayer
		) {
			serverPlayer.sendMessage(getDamageTracker().getDeathMessage());
		}

		super.onDeath(damageSource);
	}

	public boolean isSitting() {
		return sitting;
	}

	public void setSitting(boolean sitting) {
		this.sitting = sitting;
	}

	public void tryTeleportToOwner() {
		LivingEntity owner = getOwner();
		if (owner != null) {
			tryTeleportNear(owner.getBlockPos());
		}
	}

	/**
	 * Проверяет, нужно ли телепортироваться к владельцу.
	 * Телепортация нужна, если расстояние до владельца превышает {@value #TELEPORT_DISTANCE_SQUARED} блоков².
	 */
	public boolean shouldTryTeleportToOwner() {
		LivingEntity owner = getOwner();
		return owner != null && squaredDistanceTo(owner) >= TELEPORT_DISTANCE_SQUARED;
	}

	private void tryTeleportNear(BlockPos pos) {
		for (int attempt = 0; attempt < TELEPORT_ATTEMPTS; attempt++) {
			int dx = random.nextBetween(-TELEPORT_RADIUS, TELEPORT_RADIUS);
			int dz = random.nextBetween(-TELEPORT_RADIUS, TELEPORT_RADIUS);
			if (Math.abs(dx) >= 2 || Math.abs(dz) >= 2) {
				int dy = random.nextBetween(-TELEPORT_HEIGHT_RANGE, TELEPORT_HEIGHT_RANGE);
				if (tryTeleportTo(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz)) {
					return;
				}
			}
		}
	}

	private boolean tryTeleportTo(int x, int y, int z) {
		if (!canTeleportTo(new BlockPos(x, y, z))) {
			return false;
		}

		refreshPositionAndAngles(x + 0.5, y, z + 0.5, getYaw(), getPitch());
		navigation.stop();
		return true;
	}

	private boolean canTeleportTo(BlockPos pos) {
		PathNodeType nodeType = LandPathNodeMaker.getLandNodeType(this, pos);
		if (nodeType != PathNodeType.WALKABLE) {
			return false;
		}

		BlockState below = getEntityWorld().getBlockState(pos.down());
		if (!canTeleportOntoLeaves() && below.getBlock() instanceof LeavesBlock) {
			return false;
		}

		BlockPos offset = pos.subtract(getBlockPos());
		return getEntityWorld().isSpaceEmpty(this, getBoundingBox().offset(offset));
	}

	/**
	 * Проверяет, не может ли питомец следовать за владельцем.
	 * Питомец не следует, если сидит, едет верхом, привязан или владелец в режиме наблюдателя.
	 */
	public final boolean cannotFollowOwner() {
		return isSitting()
			|| hasVehicle()
			|| mightBeLeashed()
			|| getOwner() != null && getOwner().isSpectator();
	}

	protected boolean canTeleportOntoLeaves() {
		return false;
	}

	/**
	 * Цель побега от опасности с поддержкой телепортации к владельцу.
	 */
	public class TameableEscapeDangerGoal extends EscapeDangerGoal {

		public TameableEscapeDangerGoal(final double speed, final TagKey<DamageType> dangerousDamageTypes) {
			super(TameableEntity.this, speed, dangerousDamageTypes);
		}

		public TameableEscapeDangerGoal(final double speed) {
			super(TameableEntity.this, speed);
		}

		@Override
		public void tick() {
			if (!TameableEntity.this.cannotFollowOwner() && TameableEntity.this.shouldTryTeleportToOwner()) {
				TameableEntity.this.tryTeleportToOwner();
			}

			super.tick();
		}
	}
}
