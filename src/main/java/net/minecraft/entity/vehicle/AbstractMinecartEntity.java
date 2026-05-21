package net.minecraft.entity.vehicle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.datafixers.util.Pair;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PoweredRailBlock;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.*;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Util;
import net.minecraft.util.math.*;
import net.minecraft.world.BlockLocating;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * {@code AbstractMinecartEntity}.
 */
public abstract class AbstractMinecartEntity extends VehicleEntity {

	private static final Vec3d VILLAGER_PASSENGER_ATTACHMENT_POS = new Vec3d(0.0, 0.0, 0.0);
	private static final TrackedData<Optional<BlockState>> CUSTOM_BLOCK_STATE = DataTracker.registerData(
			AbstractMinecartEntity.class, TrackedDataHandlerRegistry.OPTIONAL_BLOCK_STATE
	);
	private static final TrackedData<Integer>
			BLOCK_OFFSET =
			DataTracker.registerData(AbstractMinecartEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final ImmutableMap<EntityPose, ImmutableList<Integer>>
			DISMOUNT_FREE_Y_SPACES_NEEDED =
			ImmutableMap.of(
					EntityPose.STANDING,
					ImmutableList.of(0, 1, -1),
					EntityPose.CROUCHING,
					ImmutableList.of(0, 1, -1),
					EntityPose.SWIMMING,
					ImmutableList.of(0, 1)
			);
	protected static final float VELOCITY_SLOWDOWN_MULTIPLIER = 0.95F;
	private static final boolean DEFAULT_YAW_FLIPPED = false;
	private boolean onRail;
	private boolean yawFlipped = false;
	private final MinecartController controller;
	private static final Map<RailShape, Pair<Vec3i, Vec3i>> ADJACENT_RAIL_POSITIONS_BY_SHAPE = Maps.newEnumMap(
			(Map) Util.make(
					() -> {
						Vec3i vec3i = Direction.WEST.getVector();
						Vec3i vec3i2 = Direction.EAST.getVector();
						Vec3i vec3i3 = Direction.NORTH.getVector();
						Vec3i vec3i4 = Direction.SOUTH.getVector();
						Vec3i vec3i5 = vec3i.down();
						Vec3i vec3i6 = vec3i2.down();
						Vec3i vec3i7 = vec3i3.down();
						Vec3i vec3i8 = vec3i4.down();
						return ImmutableMap.of(
								RailShape.NORTH_SOUTH,
								Pair.of(vec3i3, vec3i4),
								RailShape.EAST_WEST,
								Pair.of(vec3i, vec3i2),
								RailShape.ASCENDING_EAST,
								Pair.of(vec3i5, vec3i2),
								RailShape.ASCENDING_WEST,
								Pair.of(vec3i, vec3i6),
								RailShape.ASCENDING_NORTH,
								Pair.of(vec3i3, vec3i8),
								RailShape.ASCENDING_SOUTH,
								Pair.of(vec3i7, vec3i4),
								RailShape.SOUTH_EAST,
								Pair.of(vec3i4, vec3i2),
								RailShape.SOUTH_WEST,
								Pair.of(vec3i4, vec3i),
								RailShape.NORTH_WEST,
								Pair.of(vec3i3, vec3i),
								RailShape.NORTH_EAST,
								Pair.of(vec3i3, vec3i2)
						);
					}
			)
	);

	protected AbstractMinecartEntity(EntityType<?> entityType, World world) {
		super(entityType, world);
		this.intersectionChecked = true;
		if (areMinecartImprovementsEnabled(world)) {
			this.controller = new ExperimentalMinecartController(this);
		}
		else {
			this.controller = new DefaultMinecartController(this);
		}
	}

	protected AbstractMinecartEntity(EntityType<?> type, World world, double x, double y, double z) {
		this(type, world);
		this.initPosition(x, y, z);
	}

	/**
	 * Инициализирует position.
	 *
	 * @param x x
	 * @param y y
	 * @param z z
	 */
	public void initPosition(double x, double y, double z) {
		this.setPosition(x, y, z);
		this.lastX = x;
		this.lastY = y;
		this.lastZ = z;
	}

	public static <T extends AbstractMinecartEntity> @Nullable T create(
			World world,
			double x,
			double y,
			double z,
			EntityType<T> type,
			SpawnReason reason,
			ItemStack stack,
			@Nullable PlayerEntity player
	) {
		T abstractMinecartEntity = (T) type.create(world, reason);
		if (abstractMinecartEntity != null) {
			abstractMinecartEntity.initPosition(x, y, z);
			EntityType.copier(world, stack, player).accept(abstractMinecartEntity);
			if (abstractMinecartEntity.getController() instanceof ExperimentalMinecartController experimentalMinecartController) {
				BlockPos blockPos = abstractMinecartEntity.getRailOrMinecartPos();
				BlockState blockState = world.getBlockState(blockPos);
				experimentalMinecartController.adjustToRail(blockPos, blockState, true);
			}
		}

		return abstractMinecartEntity;
	}

	public MinecartController getController() {
		return this.controller;
	}

	@Override
	protected Entity.MoveEffect getMoveEffect() {
		return Entity.MoveEffect.EVENTS;
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(CUSTOM_BLOCK_STATE, Optional.empty());
		builder.add(BLOCK_OFFSET, this.getDefaultBlockOffset());
	}

	@Override
	public boolean collidesWith(Entity other) {
		return AbstractBoatEntity.canCollide(this, other);
	}

	@Override
	public boolean isPushable() {
		return true;
	}

	@Override
	public Vec3d positionInPortal(Direction.Axis portalAxis, BlockLocating.Rectangle portalRect) {
		return LivingEntity.positionInPortal(super.positionInPortal(portalAxis, portalRect));
	}

	@Override
	protected Vec3d getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
		boolean bl = passenger instanceof VillagerEntity || passenger instanceof WanderingTraderEntity;
		return bl ? VILLAGER_PASSENGER_ATTACHMENT_POS
		          : super.getPassengerAttachmentPos(passenger, dimensions, scaleFactor);
	}

	@Override
	public Vec3d updatePassengerForDismount(LivingEntity passenger) {
		Direction direction = this.getMovementDirection();
		if (direction.getAxis() == Direction.Axis.Y) {
			return super.updatePassengerForDismount(passenger);
		}
		else {
			int[][] is = Dismounting.getDismountOffsets(direction);
			BlockPos blockPos = this.getBlockPos();
			BlockPos.Mutable mutable = new BlockPos.Mutable();
			ImmutableList<EntityPose> immutableList = passenger.getPoses();
			UnmodifiableIterator e = immutableList.iterator();

			while (e.hasNext()) {
				EntityPose entityPose = (EntityPose) e.next();
				EntityDimensions entityDimensions = passenger.getDimensions(entityPose);
				float f = Math.min(entityDimensions.width(), 1.0F) / 2.0F;
				UnmodifiableIterator g = ((ImmutableList) DISMOUNT_FREE_Y_SPACES_NEEDED.get(entityPose)).iterator();

				while (g.hasNext()) {
					int i = (Integer) g.next();

					for (int[] js : is) {
						mutable.set(blockPos.getX() + js[0], blockPos.getY() + i, blockPos.getZ() + js[1]);
						double d = this.getEntityWorld()
						               .getDismountHeight(
								               Dismounting.getCollisionShape(this.getEntityWorld(), mutable),
								               () -> Dismounting.getCollisionShape(
										               this.getEntityWorld(),
										               mutable.down()
								               )
						               );
						if (Dismounting.canDismountInBlock(d)) {
							Box box = new Box(-f, 0.0, -f, f, entityDimensions.height(), f);
							Vec3d vec3d = Vec3d.ofCenter(mutable, d);
							if (Dismounting.canPlaceEntityAt(this.getEntityWorld(), passenger, box.offset(vec3d))) {
								passenger.setPose(entityPose);
								return vec3d;
							}
						}
					}
				}
			}

			double ex = this.getBoundingBox().maxY;
			mutable.set((double) blockPos.getX(), ex, (double) blockPos.getZ());
			UnmodifiableIterator var22 = immutableList.iterator();

			while (var22.hasNext()) {
				EntityPose entityPose2 = (EntityPose) var22.next();
				double g = passenger.getDimensions(entityPose2).height();
				int j = MathHelper.ceil(ex - mutable.getY() + g);
				double
						h =
						Dismounting.getCeilingHeight(
								mutable,
								j,
								pos -> this
										.getEntityWorld()
										.getBlockState(pos)
										.getCollisionShape(this.getEntityWorld(), pos)
						);
				if (ex + g <= h) {
					passenger.setPose(entityPose2);
					break;
				}
			}

			return super.updatePassengerForDismount(passenger);
		}
	}

	@Override
	protected float getVelocityMultiplier() {
		BlockState blockState = this.getEntityWorld().getBlockState(this.getBlockPos());
		return blockState.isIn(BlockTags.RAILS) ? 1.0F : super.getVelocityMultiplier();
	}

	@Override
	public void animateDamage(float yaw) {
		this.setDamageWobbleSide(-this.getDamageWobbleSide());
		this.setDamageWobbleTicks(10);
		this.setDamageWobbleStrength(this.getDamageWobbleStrength() + this.getDamageWobbleStrength() * 10.0F);
	}

	@Override
	public boolean canHit() {
		return !this.isRemoved();
	}

	public static Pair<Vec3i, Vec3i> getAdjacentRailPositionsByShape(RailShape shape) {
		return ADJACENT_RAIL_POSITIONS_BY_SHAPE.get(shape);
	}

	@Override
	public Direction getMovementDirection() {
		return this.controller.getHorizontalFacing();
	}

	@Override
	protected double getGravity() {
		return this.isTouchingWater() ? 0.005 : 0.04;
	}

	@Override
	public void tick() {
		if (this.getDamageWobbleTicks() > 0) {
			this.setDamageWobbleTicks(this.getDamageWobbleTicks() - 1);
		}

		if (this.getDamageWobbleStrength() > 0.0F) {
			this.setDamageWobbleStrength(this.getDamageWobbleStrength() - 1.0F);
		}

		this.attemptTickInVoid();
		this.tickLastPos();
		this.tickPortalTeleportation();
		this.controller.tick();
		this.updateWaterState();
		if (this.isInLava()) {
			this.igniteByLava();
			this.setOnFireFromLava();
			this.fallDistance *= 0.5;
		}

		this.firstUpdate = false;
	}

	public boolean isFirstUpdate() {
		return this.firstUpdate;
	}

	public BlockPos getRailOrMinecartPos() {
		int i = MathHelper.floor(this.getX());
		int j = MathHelper.floor(this.getY());
		int k = MathHelper.floor(this.getZ());
		if (areMinecartImprovementsEnabled(this.getEntityWorld())) {
			double d = this.getY() - 0.1 - 1.0E-5F;
			if (this.getEntityWorld().getBlockState(BlockPos.ofFloored(i, d, k)).isIn(BlockTags.RAILS)) {
				j = MathHelper.floor(d);
			}
		}
		else if (this.getEntityWorld().getBlockState(new BlockPos(i, j - 1, k)).isIn(BlockTags.RAILS)) {
			j--;
		}

		return new BlockPos(i, j, k);
	}

	protected double getMaxSpeed(ServerWorld world) {
		return this.controller.getMaxSpeed(world);
	}

	/**
	 * Обрабатывает событие activator rail.
	 *
	 * @param serverWorld server world
	 * @param y y
	 * @param z z
	 * @param i i
	 * @param bl bl
	 */
	public void onActivatorRail(ServerWorld serverWorld, int y, int z, int i, boolean bl) {
	}

	@Override
	public void lerpPosAndRotation(int step, double x, double y, double z, double yaw, double pitch) {
		super.lerpPosAndRotation(step, x, y, z, yaw, pitch);
	}

	@Override
	public void applyGravity() {
		super.applyGravity();
	}

	@Override
	public void refreshPosition() {
		super.refreshPosition();
	}

	@Override
	public boolean updateWaterState() {
		return super.updateWaterState();
	}

	@Override
	public Vec3d getMovement() {
		return this.controller.limitSpeed(super.getMovement());
	}

	@Override
	public PositionInterpolator getInterpolator() {
		return this.controller.getInterpolator();
	}

	@Override
	public void onSpawnPacket(EntitySpawnS2CPacket packet) {
		super.onSpawnPacket(packet);
		this.controller.setLerpTargetVelocity(this.getVelocity());
	}

	@Override
	public void setVelocityClient(Vec3d clientVelocity) {
		this.controller.setLerpTargetVelocity(clientVelocity);
	}

	/**
	 * Перемещает on rail.
	 *
	 * @param world world
	 */
	protected void moveOnRail(ServerWorld world) {
		this.controller.moveOnRail(world);
	}

	/**
	 * Перемещает off rail.
	 *
	 * @param world world
	 */
	protected void moveOffRail(ServerWorld world) {
		double d = this.getMaxSpeed(world);
		Vec3d vec3d = this.getVelocity();
		this.setVelocity(MathHelper.clamp(vec3d.x, -d, d), vec3d.y, MathHelper.clamp(vec3d.z, -d, d));
		if (this.isOnGround()) {
			this.setVelocity(this.getVelocity().multiply(0.5));
		}

		this.move(MovementType.SELF, this.getVelocity());
		if (!this.isOnGround()) {
			this.setVelocity(this.getVelocity().multiply(0.95));
		}
	}

	/**
	 * Перемещает along track.
	 *
	 * @param pos pos
	 * @param shape shape
	 * @param remainingMovement remaining movement
	 *
	 * @return double — результат операции
	 */
	protected double moveAlongTrack(BlockPos pos, RailShape shape, double remainingMovement) {
		return this.controller.moveAlongTrack(pos, shape, remainingMovement);
	}

	@Override
	public void move(MovementType type, Vec3d movement) {
		if (areMinecartImprovementsEnabled(this.getEntityWorld())) {
			Vec3d vec3d = this.getEntityPos().add(movement);
			super.move(type, movement);
			boolean bl = this.controller.handleCollision();
			if (bl) {
				super.move(type, vec3d.subtract(this.getEntityPos()));
			}

			if (type.equals(MovementType.PISTON)) {
				this.onRail = false;
			}
		}
		else {
			super.move(type, movement);
			this.tickBlockCollision();
		}
	}

	@Override
	public void tickBlockCollision() {
		if (areMinecartImprovementsEnabled(this.getEntityWorld())) {
			super.tickBlockCollision();
		}
		else {
			this.tickBlockCollision(this.getEntityPos(), this.getEntityPos());
			this.clearQueuedCollisionChecks();
		}
	}

	@Override
	public boolean isOnRail() {
		return this.onRail;
	}

	public void setOnRail(boolean onRail) {
		this.onRail = onRail;
	}

	public boolean isYawFlipped() {
		return this.yawFlipped;
	}

	public void setYawFlipped(boolean yawFlipped) {
		this.yawFlipped = yawFlipped;
	}

	public Vec3d getLaunchDirection(BlockPos railPos) {
		BlockState blockState = this.getEntityWorld().getBlockState(railPos);
		if (blockState.isOf(Blocks.POWERED_RAIL) && blockState.get(PoweredRailBlock.POWERED)) {
			RailShape railShape = blockState.get(((AbstractRailBlock) blockState.getBlock()).getShapeProperty());
			if (railShape == RailShape.EAST_WEST) {
				if (this.willHitBlockAt(railPos.west())) {
					return new Vec3d(1.0, 0.0, 0.0);
				}

				if (this.willHitBlockAt(railPos.east())) {
					return new Vec3d(-1.0, 0.0, 0.0);
				}
			}
			else if (railShape == RailShape.NORTH_SOUTH) {
				if (this.willHitBlockAt(railPos.north())) {
					return new Vec3d(0.0, 0.0, 1.0);
				}

				if (this.willHitBlockAt(railPos.south())) {
					return new Vec3d(0.0, 0.0, -1.0);
				}
			}

			return Vec3d.ZERO;
		}
		else {
			return Vec3d.ZERO;
		}
	}

	/**
	 * Will hit block at.
	 *
	 * @param pos pos
	 *
	 * @return boolean — результат операции
	 */
	public boolean willHitBlockAt(BlockPos pos) {
		return this.getEntityWorld().getBlockState(pos).isSolidBlock(this.getEntityWorld(), pos);
	}

	/**
	 * Применяет slowdown.
	 *
	 * @param velocity velocity
	 *
	 * @return Vec3d — результат операции
	 */
	protected Vec3d applySlowdown(Vec3d velocity) {
		double d = this.controller.getSpeedRetention();
		Vec3d vec3d = velocity.multiply(d, 0.0, d);
		if (this.isTouchingWater()) {
			vec3d = vec3d.multiply(0.95F);
		}

		return vec3d;
	}

	@Override
	protected void readCustomData(ReadView view) {
		this.setCustomBlockState(view.read("DisplayState", BlockState.CODEC));
		this.setBlockOffset(view.getInt("DisplayOffset", this.getDefaultBlockOffset()));
		this.yawFlipped = view.getBoolean("FlippedRotation", false);
		this.firstUpdate = view.getBoolean("HasTicked", false);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		this.getCustomBlockState().ifPresent(state -> view.put("DisplayState", BlockState.CODEC, state));
		int i = this.getBlockOffset();
		if (i != this.getDefaultBlockOffset()) {
			view.putInt("DisplayOffset", i);
		}

		view.putBoolean("FlippedRotation", this.yawFlipped);
		view.putBoolean("HasTicked", this.firstUpdate);
	}

	@Override
	public void pushAwayFrom(Entity entity) {
		if (!this.getEntityWorld().isClient()) {
			if (!entity.noClip && !this.noClip) {
				if (!this.hasPassenger(entity)) {
					double d = entity.getX() - this.getX();
					double e = entity.getZ() - this.getZ();
					double f = d * d + e * e;
					if (f >= 1.0E-4F) {
						f = Math.sqrt(f);
						d /= f;
						e /= f;
						double g = 1.0 / f;
						if (g > 1.0) {
							g = 1.0;
						}

						d *= g;
						e *= g;
						d *= 0.1F;
						e *= 0.1F;
						d *= 0.5;
						e *= 0.5;
						if (entity instanceof AbstractMinecartEntity abstractMinecartEntity) {
							this.pushAwayFromMinecart(abstractMinecartEntity, d, e);
						}
						else {
							this.addVelocity(-d, 0.0, -e);
							entity.addVelocity(d / 4.0, 0.0, e / 4.0);
						}
					}
				}
			}
		}
	}

	private void pushAwayFromMinecart(AbstractMinecartEntity entity, double xDiff, double zDiff) {
		double d;
		double e;
		if (areMinecartImprovementsEnabled(this.getEntityWorld())) {
			d = this.getVelocity().x;
			e = this.getVelocity().z;
		}
		else {
			d = entity.getX() - this.getX();
			e = entity.getZ() - this.getZ();
		}

		Vec3d vec3d = new Vec3d(d, 0.0, e).normalize();
		Vec3d
				vec3d2 =
				new Vec3d(
						MathHelper.cos(this.getYaw() * (float) (Math.PI / 180.0)),
						0.0,
						MathHelper.sin(this.getYaw() * (float) (Math.PI / 180.0))
				)
						.normalize();
		double f = Math.abs(vec3d.dotProduct(vec3d2));
		if (!(f < 0.8F) || areMinecartImprovementsEnabled(this.getEntityWorld())) {
			Vec3d vec3d3 = this.getVelocity();
			Vec3d vec3d4 = entity.getVelocity();
			if (entity.isSelfPropelling() && !this.isSelfPropelling()) {
				this.setVelocity(vec3d3.multiply(0.2, 1.0, 0.2));
				this.addVelocity(vec3d4.x - xDiff, 0.0, vec3d4.z - zDiff);
				entity.setVelocity(vec3d4.multiply(0.95, 1.0, 0.95));
			}
			else if (!entity.isSelfPropelling() && this.isSelfPropelling()) {
				entity.setVelocity(vec3d4.multiply(0.2, 1.0, 0.2));
				entity.addVelocity(vec3d3.x + xDiff, 0.0, vec3d3.z + zDiff);
				this.setVelocity(vec3d3.multiply(0.95, 1.0, 0.95));
			}
			else {
				double g = (vec3d4.x + vec3d3.x) / 2.0;
				double h = (vec3d4.z + vec3d3.z) / 2.0;
				this.setVelocity(vec3d3.multiply(0.2, 1.0, 0.2));
				this.addVelocity(g - xDiff, 0.0, h - zDiff);
				entity.setVelocity(vec3d4.multiply(0.2, 1.0, 0.2));
				entity.addVelocity(g + xDiff, 0.0, h + zDiff);
			}
		}
	}

	public BlockState getContainedBlock() {
		return this.getCustomBlockState().orElseGet(this::getDefaultContainedBlock);
	}

	private Optional<BlockState> getCustomBlockState() {
		return this.getDataTracker().get(CUSTOM_BLOCK_STATE);
	}

	public BlockState getDefaultContainedBlock() {
		return Blocks.AIR.getDefaultState();
	}

	public int getBlockOffset() {
		return this.getDataTracker().get(BLOCK_OFFSET);
	}

	public int getDefaultBlockOffset() {
		return 6;
	}

	public void setCustomBlockState(Optional<BlockState> customBlockState) {
		this.getDataTracker().set(CUSTOM_BLOCK_STATE, customBlockState);
	}

	public void setBlockOffset(int offset) {
		this.getDataTracker().set(BLOCK_OFFSET, offset);
	}

	/**
	 * Are minecart improvements enabled.
	 *
	 * @param world world
	 *
	 * @return boolean — результат операции
	 */
	public static boolean areMinecartImprovementsEnabled(World world) {
		return world.getEnabledFeatures().contains(FeatureFlags.MINECART_IMPROVEMENTS);
	}

	@Override
	public abstract ItemStack getPickBlockStack();

	public boolean isRideable() {
		return false;
	}

	public boolean isSelfPropelling() {
		return false;
	}
}
