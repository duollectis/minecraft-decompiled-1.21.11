package net.minecraft.entity.mob;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.provider.EnchantmentProviders;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LazyEntityReference;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.Potions;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Identifier;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.Optional;

/**
 * Эндермен — нейтральный моб, атакующий игрока при зрительном контакте.
 * Умеет телепортироваться, подбирать и переносить блоки.
 * Получает урон от воды и снарядов (кроме водяных зелий).
 */
public class EndermanEntity extends HostileEntity implements Angerable {

	private static final Identifier ATTACKING_SPEED_MODIFIER_ID = Identifier.ofVanilla("attacking");
	private static final EntityAttributeModifier ATTACKING_SPEED_BOOST = new EntityAttributeModifier(
			ATTACKING_SPEED_MODIFIER_ID, 0.15F, EntityAttributeModifier.Operation.ADD_VALUE
	);
	private static final int ANGRY_SOUND_COOLDOWN = 400;
	private static final int TARGET_ABANDON_AGE_OFFSET = 600;
	private static final TrackedData<Optional<BlockState>> CARRIED_BLOCK = DataTracker.registerData(
			EndermanEntity.class, TrackedDataHandlerRegistry.OPTIONAL_BLOCK_STATE
	);
	private static final TrackedData<Boolean>
			ANGRY =
			DataTracker.registerData(EndermanEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean>
			PROVOKED =
			DataTracker.registerData(EndermanEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private int lastAngrySoundAge = Integer.MIN_VALUE;
	private int ageWhenTargetSet;
	private static final UniformIntProvider ANGER_TIME_RANGE = TimeHelper.betweenSeconds(20, 39);
	private long angerEndTime;
	private @Nullable LazyEntityReference<LivingEntity> angryAt;

	public EndermanEntity(EntityType<? extends EndermanEntity> entityType, World world) {
		super(entityType, world);
		setPathfindingPenalty(PathNodeType.WATER, -1.0F);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(1, new EndermanEntity.ChasePlayerGoal(this));
		goalSelector.add(2, new MeleeAttackGoal(this, 1.0, false));
		goalSelector.add(7, new WanderAroundFarGoal(this, 1.0, 0.0F));
		goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
		goalSelector.add(8, new LookAroundGoal(this));
		goalSelector.add(10, new EndermanEntity.PlaceBlockGoal(this));
		goalSelector.add(11, new EndermanEntity.PickUpBlockGoal(this));
		targetSelector.add(1, new EndermanEntity.TeleportTowardsPlayerGoal(this, this::shouldAngerAt));
		targetSelector.add(2, new RevengeGoal(this));
		targetSelector.add(3, new ActiveTargetGoal<>(this, EndermiteEntity.class, true, false));
		targetSelector.add(4, new UniversalAngerGoal<>(this, false));
	}

	@Override
	public float getPathfindingFavor(BlockPos pos, WorldView world) {
		return 0.0F;
	}

	public static DefaultAttributeContainer.Builder createEndermanAttributes() {
		return HostileEntity.createHostileAttributes()
		                    .add(EntityAttributes.MAX_HEALTH, 40.0)
		                    .add(EntityAttributes.MOVEMENT_SPEED, 0.3F)
		                    .add(EntityAttributes.ATTACK_DAMAGE, 7.0)
		                    .add(EntityAttributes.FOLLOW_RANGE, 64.0)
		                    .add(EntityAttributes.STEP_HEIGHT, 1.0);
	}

	@Override
	public void setTarget(@Nullable LivingEntity target) {
		super.setTarget(target);
		EntityAttributeInstance speedAttribute = getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);

		if (target == null) {
			ageWhenTargetSet = 0;
			dataTracker.set(ANGRY, false);
			dataTracker.set(PROVOKED, false);
			speedAttribute.removeModifier(ATTACKING_SPEED_MODIFIER_ID);
			return;
		}

		ageWhenTargetSet = age;
		dataTracker.set(ANGRY, true);

		if (!speedAttribute.hasModifier(ATTACKING_SPEED_MODIFIER_ID)) {
			speedAttribute.addTemporaryModifier(ATTACKING_SPEED_BOOST);
		}
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(CARRIED_BLOCK, Optional.empty());
		builder.add(ANGRY, false);
		builder.add(PROVOKED, false);
	}

	@Override
	public void chooseRandomAngerTime() {
		setAngerDuration(ANGER_TIME_RANGE.get(random));
	}

	@Override
	public void setAngerEndTime(long angerEndTime) {
		this.angerEndTime = angerEndTime;
	}

	@Override
	public long getAngerEndTime() {
		return angerEndTime;
	}

	@Override
	public void setAngryAt(@Nullable LazyEntityReference<LivingEntity> angryAt) {
		this.angryAt = angryAt;
	}

	@Override
	public @Nullable LazyEntityReference<LivingEntity> getAngryAt() {
		return angryAt;
	}

	public void playAngrySound() {
		if (age < lastAngrySoundAge + ANGRY_SOUND_COOLDOWN) {
			return;
		}

		lastAngrySoundAge = age;

		if (isSilent()) {
			return;
		}

		getEntityWorld()
			.playSoundClient(
				getX(),
				getEyeY(),
				getZ(),
				SoundEvents.ENTITY_ENDERMAN_STARE,
				getSoundCategory(),
				2.5F,
				1.0F,
				false
			);
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		if (ANGRY.equals(data) && isProvoked() && getEntityWorld().isClient()) {
			playAngrySound();
		}

		super.onTrackedDataSet(data);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		BlockState blockState = getCarriedBlock();

		if (blockState != null) {
			view.put("carriedBlockState", BlockState.CODEC, blockState);
		}

		writeAngerToData(view);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		setCarriedBlock(
			view.<BlockState>read("carriedBlockState", BlockState.CODEC)
				.filter(blockState -> !blockState.isAir())
				.orElse(null)
		);
		readAngerFromData(getEntityWorld(), view);
	}

	boolean isPlayerStaring(PlayerEntity player) {
		return LivingEntity.NOT_WEARING_GAZE_DISGUISE_PREDICATE.test(player)
			&& isEntityLookingAtMe(player, 0.025, true, false, getEyeY());
	}

	@Override
	public void tickMovement() {
		if (getEntityWorld().isClient()) {
			for (int i = 0; i < 2; i++) {
				getEntityWorld()
					.addParticleClient(
						ParticleTypes.PORTAL,
						getParticleX(0.5),
						getRandomBodyY() - 0.25,
						getParticleZ(0.5),
						(random.nextDouble() - 0.5) * 2.0,
						-random.nextDouble(),
						(random.nextDouble() - 0.5) * 2.0
					);
			}
		}

		jumping = false;

		if (!getEntityWorld().isClient()) {
			tickAngerLogic((ServerWorld) getEntityWorld(), true);
		}

		super.tickMovement();
	}

	@Override
	public boolean hurtByWater() {
		return true;
	}

	@Override
	protected void mobTick(ServerWorld world) {
		if (world.isDay() && age >= ageWhenTargetSet + TARGET_ABANDON_AGE_OFFSET) {
			float brightness = getBrightnessAtEyes();

			if (brightness > 0.5F
				&& world.isSkyVisible(getBlockPos())
				&& random.nextFloat() * 30.0F < (brightness - 0.4F) * 2.0F
			) {
				setTarget(null);
				teleportRandomly();
			}
		}

		super.mobTick(world);
	}

	/**
	 * Телепортирует эндермена в случайную точку в радиусе 64 блоков.
	 * Не выполняется на клиентской стороне или если моб мёртв.
	 */
	protected boolean teleportRandomly() {
		if (getEntityWorld().isClient() || !isAlive()) {
			return false;
		}

		double targetX = getX() + (random.nextDouble() - 0.5) * 64.0;
		double targetY = getY() + (random.nextInt(64) - 32);
		double targetZ = getZ() + (random.nextDouble() - 0.5) * 64.0;

		return teleportTo(targetX, targetY, targetZ);
	}

	boolean teleportTo(Entity entity) {
		Vec3d direction = new Vec3d(
			getX() - entity.getX(),
			getBodyY(0.5) - entity.getEyeY(),
			getZ() - entity.getZ()
		).normalize();

		double targetX = getX() + (random.nextDouble() - 0.5) * 8.0 - direction.x * 16.0;
		double targetY = getY() + (random.nextInt(16) - 8) - direction.y * 16.0;
		double targetZ = getZ() + (random.nextDouble() - 0.5) * 8.0 - direction.z * 16.0;

		return teleportTo(targetX, targetY, targetZ);
	}

	private boolean teleportTo(double x, double y, double z) {
		BlockPos.Mutable mutable = new BlockPos.Mutable(x, y, z);

		while (mutable.getY() > getEntityWorld().getBottomY()
			&& !getEntityWorld().getBlockState(mutable).blocksMovement()
		) {
			mutable.move(Direction.DOWN);
		}

		BlockState blockState = getEntityWorld().getBlockState(mutable);
		boolean blocksMovement = blockState.blocksMovement();
		boolean isWater = blockState.getFluidState().isIn(FluidTags.WATER);

		if (!blocksMovement || isWater) {
			return false;
		}

		Vec3d prevPos = getEntityPos();
		boolean teleported = teleport(x, y, z, true);

		if (!teleported) {
			return false;
		}

		getEntityWorld().emitGameEvent(GameEvent.TELEPORT, prevPos, GameEvent.Emitter.of(this));

		if (!isSilent()) {
			getEntityWorld().playSound(
					null,
					lastX,
					lastY,
					lastZ,
					SoundEvents.ENTITY_ENDERMAN_TELEPORT,
					getSoundCategory(),
					1.0F,
					1.0F
			);
			playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
		}

		return teleported;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return isAngry() ? SoundEvents.ENTITY_ENDERMAN_SCREAM : SoundEvents.ENTITY_ENDERMAN_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_ENDERMAN_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_ENDERMAN_DEATH;
	}

	@Override
	protected void dropEquipment(ServerWorld world, DamageSource source, boolean causedByPlayer) {
		super.dropEquipment(world, source, causedByPlayer);
		BlockState blockState = getCarriedBlock();

		if (blockState == null) {
			return;
		}

		ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
		EnchantmentHelper.applyEnchantmentProvider(
			axe,
			world.getRegistryManager(),
			EnchantmentProviders.ENDERMAN_LOOT_DROP,
			world.getLocalDifficulty(getBlockPos()),
			getRandom()
		);
		LootWorldContext.Builder builder = new LootWorldContext.Builder((ServerWorld) getEntityWorld())
			.add(LootContextParameters.ORIGIN, getEntityPos())
			.add(LootContextParameters.TOOL, axe)
			.addOptional(LootContextParameters.THIS_ENTITY, this);

		for (ItemStack drop : blockState.getDroppedStacks(builder)) {
			dropStack(world, drop);
		}
	}

	public void setCarriedBlock(@Nullable BlockState state) {
		dataTracker.set(CARRIED_BLOCK, Optional.ofNullable(state));
	}

	public @Nullable BlockState getCarriedBlock() {
		return dataTracker.get(CARRIED_BLOCK).orElse(null);
	}

	/**
	 * Эндермен уклоняется от снарядов телепортацией.
	 * Водяные зелья наносят урон напрямую, остальные снаряды провоцируют до 64 попыток телепортации.
	 */
	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		if (isInvulnerableTo(world, source)) {
			return false;
		}

		PotionEntity potion = source.getSource() instanceof PotionEntity p ? p : null;

		if (!source.isIn(DamageTypeTags.IS_PROJECTILE) && potion == null) {
			boolean damaged = super.damage(world, source, amount);

			if (!(source.getAttacker() instanceof LivingEntity) && random.nextInt(10) != 0) {
				teleportRandomly();
			}

			return damaged;
		}

		boolean damaged = potion != null && damageFromPotion(world, source, potion, amount);

		for (int i = 0; i < 64; i++) {
			if (teleportRandomly()) {
				return true;
			}
		}

		return damaged;
	}

	private boolean damageFromPotion(ServerWorld world, DamageSource source, PotionEntity potion, float amount) {
		ItemStack stack = potion.getStack();
		PotionContentsComponent contents = stack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);

		if (!contents.matches(Potions.WATER)) {
			return false;
		}

		return super.damage(world, source, amount);
	}

	public boolean isAngry() {
		return dataTracker.get(ANGRY);
	}

	public boolean isProvoked() {
		return dataTracker.get(PROVOKED);
	}

	public void setProvoked() {
		dataTracker.set(PROVOKED, true);
	}

	@Override
	public boolean cannotDespawn() {
		return super.cannotDespawn() || getCarriedBlock() != null;
	}

	static class ChasePlayerGoal extends Goal {

		private final EndermanEntity enderman;
		private @Nullable LivingEntity target;

		public ChasePlayerGoal(EndermanEntity enderman) {
			this.enderman = enderman;
			setControls(EnumSet.of(Goal.Control.JUMP, Goal.Control.MOVE));
		}

		@Override
		public boolean canStart() {
			target = enderman.getTarget();

			if (!(target instanceof PlayerEntity playerEntity)) {
				return false;
			}

			double distSq = target.squaredDistanceTo(enderman);

			return distSq <= 256.0 && enderman.isPlayerStaring(playerEntity);
		}

		@Override
		public void start() {
			enderman.getNavigation().stop();
		}

		@Override
		public void tick() {
			enderman.getLookControl().lookAt(target.getX(), target.getEyeY(), target.getZ());
		}
	}

	static class PickUpBlockGoal extends Goal {

		private final EndermanEntity enderman;

		public PickUpBlockGoal(EndermanEntity enderman) {
			this.enderman = enderman;
		}

		@Override
		public boolean canStart() {
			if (enderman.getCarriedBlock() != null) {
				return false;
			}

			if (!getServerWorld(enderman).getGameRules().getValue(GameRules.DO_MOB_GRIEFING)) {
				return false;
			}

			return enderman.getRandom().nextInt(toGoalTicks(20)) == 0;
		}

		@Override
		public void tick() {
			Random random = enderman.getRandom();
			World world = enderman.getEntityWorld();
			int x = MathHelper.floor(enderman.getX() - 2.0 + random.nextDouble() * 4.0);
			int y = MathHelper.floor(enderman.getY() + random.nextDouble() * 3.0);
			int z = MathHelper.floor(enderman.getZ() - 2.0 + random.nextDouble() * 4.0);
			BlockPos blockPos = new BlockPos(x, y, z);
			BlockState blockState = world.getBlockState(blockPos);
			Vec3d fromPos = new Vec3d(enderman.getBlockX() + 0.5, y + 0.5, enderman.getBlockZ() + 0.5);
			Vec3d toPos = new Vec3d(x + 0.5, y + 0.5, z + 0.5);
			BlockHitResult hitResult = world.raycast(
				new RaycastContext(
					fromPos,
					toPos,
					RaycastContext.ShapeType.OUTLINE,
					RaycastContext.FluidHandling.NONE,
					enderman
				)
			);
			boolean hasLineOfSight = hitResult.getBlockPos().equals(blockPos);

			if (blockState.isIn(BlockTags.ENDERMAN_HOLDABLE) && hasLineOfSight) {
				world.removeBlock(blockPos, false);
				world.emitGameEvent(GameEvent.BLOCK_DESTROY, blockPos, GameEvent.Emitter.of(enderman, blockState));
				enderman.setCarriedBlock(blockState.getBlock().getDefaultState());
			}
		}
	}

	static class PlaceBlockGoal extends Goal {

		private final EndermanEntity enderman;

		public PlaceBlockGoal(EndermanEntity enderman) {
			this.enderman = enderman;
		}

		@Override
		public boolean canStart() {
			if (enderman.getCarriedBlock() == null) {
				return false;
			}

			if (!getServerWorld(enderman).getGameRules().getValue(GameRules.DO_MOB_GRIEFING)) {
				return false;
			}

			return enderman.getRandom().nextInt(toGoalTicks(2000)) == 0;
		}

		@Override
		public void tick() {
			Random random = enderman.getRandom();
			World world = enderman.getEntityWorld();
			int x = MathHelper.floor(enderman.getX() - 1.0 + random.nextDouble() * 2.0);
			int y = MathHelper.floor(enderman.getY() + random.nextDouble() * 2.0);
			int z = MathHelper.floor(enderman.getZ() - 1.0 + random.nextDouble() * 2.0);
			BlockPos blockPos = new BlockPos(x, y, z);
			BlockState stateAbove = world.getBlockState(blockPos);
			BlockPos posBelow = blockPos.down();
			BlockState stateBelow = world.getBlockState(posBelow);
			BlockState carried = enderman.getCarriedBlock();

			if (carried == null) {
				return;
			}

			carried = Block.postProcessState(carried, enderman.getEntityWorld(), blockPos);

			if (canPlaceOn(world, blockPos, carried, stateAbove, stateBelow, posBelow)) {
				world.setBlockState(blockPos, carried, 3);
				world.emitGameEvent(GameEvent.BLOCK_PLACE, blockPos, GameEvent.Emitter.of(enderman, carried));
				enderman.setCarriedBlock(null);
			}
		}

		private boolean canPlaceOn(
			World world,
			BlockPos posAbove,
			BlockState carriedState,
			BlockState stateAbove,
			BlockState state,
			BlockPos pos
		) {
			return stateAbove.isAir()
				&& !state.isAir()
				&& !state.isOf(Blocks.BEDROCK)
				&& state.isFullCube(world, pos)
				&& carriedState.canPlaceAt(world, posAbove)
				&& world.getOtherEntities(enderman, Box.from(Vec3d.of(posAbove))).isEmpty();
		}
	}

	static class TeleportTowardsPlayerGoal extends ActiveTargetGoal<PlayerEntity> {

		private final EndermanEntity enderman;
		private @Nullable PlayerEntity targetPlayer;
		private int lookAtPlayerWarmup;
		private int ticksSinceUnseenTeleport;
		private final TargetPredicate staringPlayerPredicate;
		private final TargetPredicate validTargetPredicate = TargetPredicate.createAttackable().ignoreVisibility();
		private final TargetPredicate.EntityPredicate angerPredicate;

		public TeleportTowardsPlayerGoal(
				EndermanEntity enderman,
				TargetPredicate.@Nullable EntityPredicate targetPredicate
		) {
			super(enderman, PlayerEntity.class, 10, false, false, targetPredicate);
			this.enderman = enderman;
			this.angerPredicate =
					(playerEntity, world) ->
							(enderman.isPlayerStaring((PlayerEntity) playerEntity) || enderman.shouldAngerAt(
									playerEntity,
									world
							)
							)
									&& !enderman.hasPassengerDeep(playerEntity);
			this.staringPlayerPredicate =
					TargetPredicate
							.createAttackable()
							.setBaseMaxDistance(this.getFollowRange())
							.setPredicate(this.angerPredicate);
		}

		@Override
		public boolean canStart() {
			targetPlayer = getServerWorld(enderman)
				.getClosestPlayer(
					staringPlayerPredicate.setBaseMaxDistance(getFollowRange()),
					enderman
				);

			return targetPlayer != null;
		}

		@Override
		public void start() {
			lookAtPlayerWarmup = getTickCount(5);
			ticksSinceUnseenTeleport = 0;
			enderman.setProvoked();
		}

		@Override
		public void stop() {
			targetPlayer = null;
			super.stop();
		}

		@Override
		public boolean shouldContinue() {
			if (targetPlayer != null) {
				if (!angerPredicate.test(targetPlayer, getServerWorld(enderman))) {
					return false;
				}

				enderman.lookAtEntity(targetPlayer, 10.0F, 10.0F);
				return true;
			}

			if (targetEntity != null) {
				if (enderman.hasPassengerDeep(targetEntity)) {
					return false;
				}

				if (validTargetPredicate.test(getServerWorld(enderman), enderman, targetEntity)) {
					return true;
				}
			}

			return super.shouldContinue();
		}

		@Override
		public void tick() {
			if (enderman.getTarget() == null) {
				super.setTargetEntity(null);
			}

			if (targetPlayer != null) {
				if (--lookAtPlayerWarmup <= 0) {
					targetEntity = targetPlayer;
					targetPlayer = null;
					super.start();
				}

				return;
			}

			if (targetEntity != null && !enderman.hasVehicle()) {
				if (enderman.isPlayerStaring((PlayerEntity) targetEntity)) {
					if (targetEntity.squaredDistanceTo(enderman) < 16.0) {
						enderman.teleportRandomly();
					}

					ticksSinceUnseenTeleport = 0;
				} else if (targetEntity.squaredDistanceTo(enderman) > 256.0
					&& ticksSinceUnseenTeleport++ >= getTickCount(30)
					&& enderman.teleportTo(targetEntity)
				) {
					ticksSinceUnseenTeleport = 0;
				}
			}

			super.tick();
		}
	}
}
