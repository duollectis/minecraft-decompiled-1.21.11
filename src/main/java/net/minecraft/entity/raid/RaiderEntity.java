package net.minecraft.entity.raid;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.MoveToRaidCenterGoal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.IllagerEntity;
import net.minecraft.entity.mob.PatrolEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.raid.Raid;
import net.minecraft.village.raid.RaidManager;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestTypes;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Базовый класс для всех участников рейда (иллагеров и ведьм).
 * <p>Управляет привязкой к активному рейду, номером волны, флагом капитана
 * и набором целей ИИ: атака домов, подбор знамени, празднование победы.</p>
 */
public abstract class RaiderEntity extends PatrolEntity {

	protected static final TrackedData<Boolean>
			CELEBRATING =
			DataTracker.registerData(RaiderEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	static final Predicate<ItemEntity> OBTAINABLE_OMINOUS_BANNER_PREDICATE = itemEntity -> !itemEntity.cannotPickup()
			&& itemEntity.isAlive()
			&& ItemStack.areEqual(
			itemEntity.getStack(),
			Raid.createOminousBanner(itemEntity.getRegistryManager().getOrThrow(RegistryKeys.BANNER_PATTERN))
	);
	private static final int DEFAULT_WAVE = 0;
	private static final boolean DEFAULT_ABLE_TO_JOIN_RAID = false;
	protected @Nullable Raid raid;
	private int wave = 0;
	private boolean ableToJoinRaid = false;
	private int outOfRaidCounter;

	protected RaiderEntity(EntityType<? extends RaiderEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	protected void initGoals() {
		super.initGoals();
		goalSelector.add(1, new RaiderEntity.PickUpBannerAsLeaderGoal<>(this));
		goalSelector.add(3, new MoveToRaidCenterGoal<>(this));
		goalSelector.add(4, new RaiderEntity.AttackHomeGoal(this, 1.05F, 1));
		goalSelector.add(5, new RaiderEntity.CelebrateGoal(this));
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(CELEBRATING, false);
	}

	/**
	 * Применяет бонусы снаряжения и характеристик для указанной волны рейда.
	 *
	 * @param world  серверный мир
	 * @param wave   номер волны (начиная с 1)
	 * @param unused устаревший параметр, не используется
	 */
	public abstract void addBonusForWave(ServerWorld world, int wave, boolean unused);

	public boolean canJoinRaid() {
		return ableToJoinRaid;
	}

	public void setAbleToJoinRaid(boolean ableToJoinRaid) {
		this.ableToJoinRaid = ableToJoinRaid;
	}

	@Override
	public void tickMovement() {
		if (getEntityWorld() instanceof ServerWorld serverWorld && isAlive()) {
			Raid currentRaid = getRaid();

			if (canJoinRaid()) {
				if (currentRaid == null) {
					if (getEntityWorld().getTime() % 20L == 0L) {
						Raid nearbyRaid = serverWorld.getRaidAt(getBlockPos());

						if (nearbyRaid != null && RaidManager.isValidRaiderFor(this)) {
							nearbyRaid.addRaider(serverWorld, nearbyRaid.getGroupsSpawned(), this, null, true);
						}
					}
				}
				else {
					LivingEntity target = getTarget();

					if (target != null && (target.getType() == EntityType.PLAYER
							|| target.getType() == EntityType.IRON_GOLEM
					)) {
						despawnCounter = 0;
					}
				}
			}
		}

		super.tickMovement();
	}

	@Override
	protected void updateDespawnCounter() {
		despawnCounter += 2;
	}

	@Override
	public void onDeath(DamageSource damageSource) {
		if (getEntityWorld() instanceof ServerWorld serverWorld) {
			Entity attacker = damageSource.getAttacker();
			Raid currentRaid = getRaid();

			if (currentRaid != null) {
				if (isPatrolLeader()) {
					currentRaid.removeLeader(getWave());
				}

				if (attacker != null && attacker.getType() == EntityType.PLAYER) {
					currentRaid.addHero(attacker);
				}

				currentRaid.removeFromWave(serverWorld, this, false);
			}
		}

		super.onDeath(damageSource);
	}

	@Override
	public boolean hasNoRaid() {
		return !hasActiveRaid();
	}

	public void setRaid(@Nullable Raid raid) {
		this.raid = raid;
	}

	public @Nullable Raid getRaid() {
		return raid;
	}

	/**
	 * Проверяет, является ли существо капитаном волны.
	 * Капитан — это патрульный лидер, несущий зловещее знамя на голове.
	 */
	public boolean isCaptain() {
		ItemStack headStack = getEquippedStack(EquipmentSlot.HEAD);
		boolean wearsOminousBanner = !headStack.isEmpty()
				&& ItemStack.areEqual(
						headStack,
						Raid.createOminousBanner(getRegistryManager().getOrThrow(RegistryKeys.BANNER_PATTERN))
				);
		return wearsOminousBanner && isPatrolLeader();
	}

	/**
	 * Возвращает {@code true}, если существо связано с рейдом или рядом есть активный рейд.
	 */
	public boolean hasRaid() {
		if (getEntityWorld() instanceof ServerWorld serverWorld) {
			return getRaid() != null || serverWorld.getRaidAt(getBlockPos()) != null;
		}

		return false;
	}

	/**
	 * Возвращает {@code true}, если существо участвует в активном (незавершённом) рейде.
	 */
	public boolean hasActiveRaid() {
		return getRaid() != null && getRaid().isActive();
	}

	public void setWave(int wave) {
		this.wave = wave;
	}

	public int getWave() {
		return wave;
	}

	public boolean isCelebrating() {
		return dataTracker.get(CELEBRATING);
	}

	public void setCelebrating(boolean celebrating) {
		dataTracker.set(CELEBRATING, celebrating);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putInt("Wave", wave);
		view.putBoolean("CanJoinRaid", ableToJoinRaid);

		if (raid != null && getEntityWorld() instanceof ServerWorld serverWorld) {
			serverWorld.getRaidManager().getRaidId(raid).ifPresent(raidId -> view.putInt("RaidId", raidId));
		}
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		wave = view.getInt("Wave", 0);
		ableToJoinRaid = view.getBoolean("CanJoinRaid", false);

		if (getEntityWorld() instanceof ServerWorld serverWorld) {
			view.getOptionalInt("RaidId").ifPresent(raidId -> {
				raid = serverWorld.getRaidManager().getRaid(raidId);

				if (raid != null) {
					raid.addToWave(serverWorld, wave, this, false);

					if (isPatrolLeader()) {
						raid.setWaveCaptain(wave, this);
					}
				}
			});
		}
	}

	/**
	 * Подбирает зловещее знамя с земли, если в текущей волне нет живого капитана.
	 * Сбрасывает предыдущий головной предмет с шансом, назначает себя капитаном волны.
	 */
	@Override
	protected void loot(ServerWorld world, ItemEntity itemEntity) {
		ItemStack droppedStack = itemEntity.getStack();
		boolean waveHasCaptain = hasActiveRaid() && getRaid().getCaptain(getWave()) != null;

		if (hasActiveRaid()
				&& !waveHasCaptain
				&& ItemStack.areEqual(
						droppedStack,
						Raid.createOminousBanner(getRegistryManager().getOrThrow(RegistryKeys.BANNER_PATTERN))
				)
		) {
			ItemStack currentHead = getEquippedStack(EquipmentSlot.HEAD);
			double dropChance = getEquipmentDropChances().get(EquipmentSlot.HEAD);

			if (!currentHead.isEmpty() && Math.max(random.nextFloat() - 0.1F, 0.0F) < dropChance) {
				dropStack(world, currentHead);
			}

			triggerItemPickedUpByEntityCriteria(itemEntity);
			equipStack(EquipmentSlot.HEAD, droppedStack);
			sendPickup(itemEntity, droppedStack.getCount());
			itemEntity.discard();
			getRaid().setWaveCaptain(getWave(), this);
			setPatrolLeader(true);
		}
		else {
			super.loot(world, itemEntity);
		}
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return getRaid() == null ? super.canImmediatelyDespawn(distanceSquared) : false;
	}

	@Override
	public boolean cannotDespawn() {
		return super.cannotDespawn() || getRaid() != null;
	}

	public int getOutOfRaidCounter() {
		return outOfRaidCounter;
	}

	public void setOutOfRaidCounter(int outOfRaidCounter) {
		this.outOfRaidCounter = outOfRaidCounter;
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		if (hasActiveRaid()) {
			getRaid().updateBar();
		}

		return super.damage(world, source, amount);
	}

	@Override
	public @Nullable EntityData initialize(
			ServerWorldAccess world,
			LocalDifficulty difficulty,
			SpawnReason spawnReason,
			@Nullable EntityData entityData
	) {
		setAbleToJoinRaid(getType() != EntityType.WITCH || spawnReason != SpawnReason.NATURAL);
		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	/**
	 * @return звук, воспроизводимый при праздновании победы в рейде
	 */
	public abstract SoundEvent getCelebratingSound();

	/**
	 * Цель ИИ: поиск и атака ближайшего жилого дома во время рейда.
	 * <p>Существо ищет точки интереса типа {@code HOME} и движется к ним,
	 * пока не найдёт цель для атаки.</p>
	 */
	static class AttackHomeGoal extends Goal {

		private final RaiderEntity raider;
		private final double speed;
		private BlockPos home;
		private final List<BlockPos> lastHomes = Lists.newArrayList();
		private final int distance;
		private boolean finished;

		public AttackHomeGoal(RaiderEntity raider, double speed, int distance) {
			this.raider = raider;
			this.speed = speed;
			this.distance = distance;
			this.setControls(EnumSet.of(Goal.Control.MOVE));
		}

		@Override
		public boolean canStart() {
			this.purgeMemory();
			return this.isRaiding() && this.tryFindHome() && this.raider.getTarget() == null;
		}

		private boolean isRaiding() {
			return this.raider.hasActiveRaid() && !this.raider.getRaid().isFinished();
		}

		private boolean tryFindHome() {
			ServerWorld serverWorld = (ServerWorld) this.raider.getEntityWorld();
			BlockPos raiderPos = this.raider.getBlockPos();
			Optional<BlockPos> found = serverWorld.getPointOfInterestStorage()
				.getPosition(
					poi -> poi.matchesKey(PointOfInterestTypes.HOME),
					this::canLootHome,
					PointOfInterestStorage.OccupationStatus.ANY,
					raiderPos,
					48,
					this.raider.random
				);

			if (found.isEmpty()) {
				return false;
			}

			this.home = found.get().toImmutable();
			return true;
		}

		@Override
		public boolean shouldContinue() {
			if (this.raider.getNavigation().isIdle()) {
				return false;
			}

			return this.raider.getTarget() == null
					&& !this.home.isWithinDistance(this.raider.getEntityPos(), this.raider.getWidth() + this.distance)
					&& !this.finished;
		}

		@Override
		public void stop() {
			if (this.home.isWithinDistance(this.raider.getEntityPos(), this.distance)) {
				this.lastHomes.add(this.home);
			}
		}

		@Override
		public void start() {
			super.start();
			this.raider.setDespawnCounter(0);
			this.raider.getNavigation().startMovingTo(this.home.getX(), this.home.getY(), this.home.getZ(), this.speed);
			this.finished = false;
		}

		@Override
		public void tick() {
			if (!this.raider.getNavigation().isIdle()) {
				return;
			}

			Vec3d homeCenter = Vec3d.ofBottomCenter(this.home);
			Vec3d wanderTarget = NoPenaltyTargeting.findTo(this.raider, 16, 7, homeCenter, (float) (Math.PI / 10));

			if (wanderTarget == null) {
				wanderTarget = NoPenaltyTargeting.findTo(this.raider, 8, 7, homeCenter, (float) (Math.PI / 2));
			}

			if (wanderTarget == null) {
				this.finished = true;
				return;
			}

			this.raider.getNavigation().startMovingTo(wanderTarget.x, wanderTarget.y, wanderTarget.z, this.speed);
		}

		private boolean canLootHome(BlockPos pos) {
			for (BlockPos blockPos : this.lastHomes) {
				if (Objects.equals(pos, blockPos)) {
					return false;
				}
			}

			return true;
		}

		private void purgeMemory() {
			if (this.lastHomes.size() > 2) {
				this.lastHomes.remove(0);
			}
		}
	}

	/**
	 * Цель ИИ: празднование победы после проигрыша рейда.
	 * <p>Существо прыгает и издаёт звуки торжества, пока рейд не завершится.</p>
	 */
	public class CelebrateGoal extends Goal {

		private final RaiderEntity raider;

		CelebrateGoal(final RaiderEntity raider) {
			this.raider = raider;
			this.setControls(EnumSet.of(Goal.Control.MOVE));
		}

		@Override
		public boolean canStart() {
			Raid currentRaid = this.raider.getRaid();
			return this.raider.isAlive() && this.raider.getTarget() == null && currentRaid != null
					&& currentRaid.hasLost();
		}

		@Override
		public void start() {
			this.raider.setCelebrating(true);
			super.start();
		}

		@Override
		public void stop() {
			this.raider.setCelebrating(false);
			super.stop();
		}

		@Override
		public void tick() {
			if (!this.raider.isSilent() && this.raider.random.nextInt(this.getTickCount(100)) == 0) {
				RaiderEntity.this.playSound(RaiderEntity.this.getCelebratingSound());
			}

			if (!this.raider.hasVehicle() && this.raider.random.nextInt(this.getTickCount(50)) == 0) {
				this.raider.getJumpControl().setActive();
			}

			super.tick();
		}
	}

	/**
	 * Цель ИИ: сближение с целью перед атакой в составе патруля.
	 * <p>Когда рейдер замечает цель, он останавливается и передаёт её
	 * всем ближайшим рейдерам, после чего переходит в режим атаки.</p>
	 */
	protected static class PatrolApproachGoal extends Goal {

		private final RaiderEntity raider;
		private final float squaredDistance;
		public final TargetPredicate closeRaiderPredicate = TargetPredicate.createNonAttackable()
		                                                                   .setBaseMaxDistance(8.0)
		                                                                   .ignoreVisibility()
		                                                                   .ignoreDistanceScalingFactor();

		public PatrolApproachGoal(IllagerEntity raider, float distance) {
			this.raider = raider;
			this.squaredDistance = distance * distance;
			this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
		}

		@Override
		public boolean canStart() {
			LivingEntity livingEntity = this.raider.getAttacker();
			return this.raider.getRaid() == null
					&& this.raider.isRaidCenterSet()
					&& this.raider.getTarget() != null
					&& !this.raider.isAttacking()
					&& (livingEntity == null || livingEntity.getType() != EntityType.PLAYER);
		}

		@Override
		public void start() {
			super.start();
			this.raider.getNavigation().stop();

			for (RaiderEntity raiderEntity : getServerWorld(this.raider)
					.getTargets(
							RaiderEntity.class,
							this.closeRaiderPredicate,
							this.raider,
							this.raider.getBoundingBox().expand(8.0, 8.0, 8.0)
					)) {
				raiderEntity.setTarget(this.raider.getTarget());
			}
		}

		@Override
		public void stop() {
			super.stop();
			LivingEntity livingEntity = this.raider.getTarget();
			if (livingEntity != null) {
				for (RaiderEntity raiderEntity : getServerWorld(this.raider)
						.getTargets(
								RaiderEntity.class,
								this.closeRaiderPredicate,
								this.raider,
								this.raider.getBoundingBox().expand(8.0, 8.0, 8.0)
						)) {
					raiderEntity.setTarget(livingEntity);
					raiderEntity.setAttacking(true);
				}

				this.raider.setAttacking(true);
			}
		}

		@Override
		public boolean shouldRunEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity livingEntity = this.raider.getTarget();
			if (livingEntity != null) {
				if (this.raider.squaredDistanceTo(livingEntity) > this.squaredDistance) {
					this.raider.getLookControl().lookAt(livingEntity, 30.0F, 30.0F);
					if (this.raider.random.nextInt(50) == 0) {
						this.raider.playAmbientSound();
					}
				}
				else {
					this.raider.setAttacking(true);
				}

				super.tick();
			}
		}
	}

	/**
	 * Цель ИИ: подбор зловещего знамени для принятия роли капитана волны.
	 * <p>Если текущий капитан волны мёртв, существо ищет брошенное знамя
	 * и движется к нему, чтобы подобрать и стать новым лидером.</p>
	 *
	 * @param <T> конкретный тип рейдера-исполнителя
	 */
	public class PickUpBannerAsLeaderGoal<T extends RaiderEntity> extends Goal {

		private final T actor;
		private Int2LongOpenHashMap bannerItemCache = new Int2LongOpenHashMap();
		private @Nullable Path path;
		private @Nullable ItemEntity bannerItemEntity;

		public PickUpBannerAsLeaderGoal(final T actor) {
			this.actor = actor;
			this.setControls(EnumSet.of(Goal.Control.MOVE));
		}

		@Override
		public boolean canStart() {
			if (this.shouldStop()) {
				return false;
			}
			else {
				Int2LongOpenHashMap int2LongOpenHashMap = new Int2LongOpenHashMap();
				double d = RaiderEntity.this.getAttributeValue(EntityAttributes.FOLLOW_RANGE);

				for (ItemEntity itemEntity : this.actor
						.getEntityWorld()
						.getEntitiesByClass(
								ItemEntity.class,
								this.actor.getBoundingBox().expand(d, 8.0, d),
								RaiderEntity.OBTAINABLE_OMINOUS_BANNER_PREDICATE
						)) {
					long l = this.bannerItemCache.getOrDefault(itemEntity.getId(), Long.MIN_VALUE);
					if (RaiderEntity.this.getEntityWorld().getTime() < l) {
						int2LongOpenHashMap.put(itemEntity.getId(), l);
					}
					else {
						Path foundPath = this.actor.getNavigation().findPathTo(itemEntity, 1);

						if (foundPath != null && foundPath.reachesTarget()) {
							this.path = foundPath;
							this.bannerItemEntity = itemEntity;
							return true;
						}

						int2LongOpenHashMap.put(
								itemEntity.getId(),
								RaiderEntity.this.getEntityWorld().getTime() + Raid.FINISH_COOLDOWN_MAX_TICKS
						);
					}
				}

				this.bannerItemCache = int2LongOpenHashMap;
				return false;
			}
		}

		@Override
		public boolean shouldContinue() {
			if (this.bannerItemEntity == null || this.path == null) {
				return false;
			}

			if (this.bannerItemEntity.isRemoved()) {
				return false;
			}

			return !this.path.isFinished() && !this.shouldStop();
		}

		private boolean shouldStop() {
			if (!this.actor.hasActiveRaid()) {
				return true;
			}
			else if (this.actor.getRaid().isFinished()) {
				return true;
			}
			else if (!this.actor.canLead()) {
				return true;
			}
			else if (ItemStack.areEqual(
					this.actor.getEquippedStack(EquipmentSlot.HEAD),
					Raid.createOminousBanner(this.actor.getRegistryManager().getOrThrow(RegistryKeys.BANNER_PATTERN))
			)) {
				return true;
			}
			else {
				RaiderEntity raiderEntity = RaiderEntity.this.raid.getCaptain(this.actor.getWave());
				return raiderEntity != null && raiderEntity.isAlive();
			}
		}

		@Override
		public void start() {
			this.actor.getNavigation().startMovingAlong(this.path, 1.15F);
		}

		@Override
		public void stop() {
			this.path = null;
			this.bannerItemEntity = null;
		}

		@Override
		public void tick() {
			if (this.bannerItemEntity != null && this.bannerItemEntity.isInRange(this.actor, 1.414)) {
				this.actor.loot(castToServerWorld(RaiderEntity.this.getEntityWorld()), this.bannerItemEntity);
			}
		}
	}
}
