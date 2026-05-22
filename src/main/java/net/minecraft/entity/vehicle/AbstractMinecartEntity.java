package net.minecraft.entity.vehicle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
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
 * Базовый класс для всех вагонеток. Управляет движением по рельсам через {@link MinecartController}:
 * в зависимости от флага {@code minecart_improvements} используется либо
 * {@link ExperimentalMinecartController}, либо {@link DefaultMinecartController}.
 * Отвечает за физику столкновений, высадку пассажиров и отображение кастомного блока внутри.
 */
public abstract class AbstractMinecartEntity extends VehicleEntity {

	private static final Vec3d VILLAGER_PASSENGER_ATTACHMENT_POS = new Vec3d(0.0, 0.0, 0.0);

	private static final TrackedData<Optional<BlockState>> CUSTOM_BLOCK_STATE =
			DataTracker.registerData(AbstractMinecartEntity.class, TrackedDataHandlerRegistry.OPTIONAL_BLOCK_STATE);
	private static final TrackedData<Integer> BLOCK_OFFSET =
			DataTracker.registerData(AbstractMinecartEntity.class, TrackedDataHandlerRegistry.INTEGER);

	private static final ImmutableMap<EntityPose, ImmutableList<Integer>> DISMOUNT_FREE_Y_SPACES_NEEDED =
			ImmutableMap.of(
					EntityPose.STANDING, ImmutableList.of(0, 1, -1),
					EntityPose.CROUCHING, ImmutableList.of(0, 1, -1),
					EntityPose.SWIMMING, ImmutableList.of(0, 1)
			);

	protected static final float VELOCITY_SLOWDOWN_MULTIPLIER = 0.95F;

	private static final int DEFAULT_BLOCK_OFFSET = 6;
	private static final double PUSH_SCALE = 0.1F;
	private static final double PUSH_HALF = 0.5;
	private static final double PUSH_MIN_DIST_SQ = 1.0E-4F;
	private static final double PUSH_ENTITY_FRACTION = 4.0;
	private static final double PUSH_ALIGNMENT_THRESHOLD = 0.8F;
	private static final double FALL_DISTANCE_LAVA_FACTOR = 0.5;
	private static final double ADJUSTED_Y_OFFSET = 0.1;
	private static final double ADJUSTED_Y_EPSILON = 1.0E-5F;

	private static final Map<RailShape, Pair<Vec3i, Vec3i>> ADJACENT_RAIL_POSITIONS_BY_SHAPE =
			Maps.newEnumMap(
					(Map) Util.make(() -> ImmutableMap.<RailShape, Pair<Vec3i, Vec3i>>builder()
							.put(RailShape.NORTH_SOUTH, Pair.of(Direction.NORTH.getVector(), Direction.SOUTH.getVector()))
							.put(RailShape.EAST_WEST, Pair.of(Direction.WEST.getVector(), Direction.EAST.getVector()))
							.put(RailShape.ASCENDING_EAST, Pair.of(Direction.WEST.getVector().down(), Direction.EAST.getVector()))
							.put(RailShape.ASCENDING_WEST, Pair.of(Direction.WEST.getVector(), Direction.EAST.getVector().down()))
							.put(RailShape.ASCENDING_NORTH, Pair.of(Direction.NORTH.getVector(), Direction.SOUTH.getVector().down()))
							.put(RailShape.ASCENDING_SOUTH, Pair.of(Direction.NORTH.getVector().down(), Direction.SOUTH.getVector()))
							.put(RailShape.SOUTH_EAST, Pair.of(Direction.SOUTH.getVector(), Direction.EAST.getVector()))
							.put(RailShape.SOUTH_WEST, Pair.of(Direction.SOUTH.getVector(), Direction.WEST.getVector()))
							.put(RailShape.NORTH_WEST, Pair.of(Direction.NORTH.getVector(), Direction.WEST.getVector()))
							.put(RailShape.NORTH_EAST, Pair.of(Direction.NORTH.getVector(), Direction.EAST.getVector()))
							.build()
					)
			);

	private boolean onRail;
	private boolean yawFlipped = false;
	private final MinecartController controller;

	protected AbstractMinecartEntity(EntityType<?> entityType, World world) {
		super(entityType, world);
		intersectionChecked = true;
		controller = areMinecartImprovementsEnabled(world)
				? new ExperimentalMinecartController(this)
				: new DefaultMinecartController(this);
	}

	protected AbstractMinecartEntity(EntityType<?> type, World world, double x, double y, double z) {
		this(type, world);
		initPosition(x, y, z);
	}

	public void initPosition(double x, double y, double z) {
		setPosition(x, y, z);
		lastX = x;
		lastY = y;
		lastZ = z;
	}

	/**
	 * Создаёт вагонетку заданного типа, инициализирует её позицию и выравнивает по рельсу
	 * при включённых улучшениях вагонеток.
	 */
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
		T minecart = type.create(world, reason);

		if (minecart == null) {
			return null;
		}

		minecart.initPosition(x, y, z);
		EntityType.copier(world, stack, player).accept(minecart);

		if (minecart.getController() instanceof ExperimentalMinecartController experimental) {
			BlockPos railPos = minecart.getRailOrMinecartPos();
			BlockState railState = world.getBlockState(railPos);
			experimental.adjustToRail(railPos, railState, true);
		}

		return minecart;
	}

	public MinecartController getController() {
		return controller;
	}

	@Override
	protected Entity.MoveEffect getMoveEffect() {
		return Entity.MoveEffect.EVENTS;
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(CUSTOM_BLOCK_STATE, Optional.empty());
		builder.add(BLOCK_OFFSET, getDefaultBlockOffset());
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
		boolean isVillagerType = passenger instanceof VillagerEntity
				|| passenger instanceof WanderingTraderEntity;
		return isVillagerType
				? VILLAGER_PASSENGER_ATTACHMENT_POS
				: super.getPassengerAttachmentPos(passenger, dimensions, scaleFactor);
	}

	/**
	 * Вычисляет безопасную позицию для высадки пассажира из вагонетки.
	 * Перебирает все позы пассажира и смещения вокруг вагонетки, ища первое место,
	 * где пассажир физически помещается без коллизий с блоками.
	 */
	@Override
	public Vec3d updatePassengerForDismount(LivingEntity passenger) {
		Direction direction = getMovementDirection();

		if (direction.getAxis() == Direction.Axis.Y) {
			return super.updatePassengerForDismount(passenger);
		}

		int[][] dismountOffsets = Dismounting.getDismountOffsets(direction);
		BlockPos cartPos = getBlockPos();
		BlockPos.Mutable candidatePos = new BlockPos.Mutable();
		ImmutableList<EntityPose> poses = passenger.getPoses();

		for (EntityPose pose : poses) {
			EntityDimensions poseDimensions = passenger.getDimensions(pose);
			float halfWidth = Math.min(poseDimensions.width(), 1.0F) / 2.0F;
			ImmutableList<Integer> yOffsets = DISMOUNT_FREE_Y_SPACES_NEEDED.get(pose);

			for (int yOffset : yOffsets) {
				for (int[] offset : dismountOffsets) {
					candidatePos.set(
							cartPos.getX() + offset[0],
							cartPos.getY() + yOffset,
							cartPos.getZ() + offset[1]
					);
					double groundHeight = getEntityWorld().getDismountHeight(
							Dismounting.getCollisionShape(getEntityWorld(), candidatePos),
							() -> Dismounting.getCollisionShape(getEntityWorld(), candidatePos.down())
					);

					if (!Dismounting.canDismountInBlock(groundHeight)) {
						continue;
					}

					Box passengerBox = new Box(
							-halfWidth, 0.0, -halfWidth,
							halfWidth, poseDimensions.height(), halfWidth
					);
					Vec3d dismountPos = Vec3d.ofCenter(candidatePos, groundHeight);

					if (Dismounting.canPlaceEntityAt(getEntityWorld(), passenger, passengerBox.offset(dismountPos))) {
						passenger.setPose(pose);
						return dismountPos;
					}
				}
			}
		}

		double cartTopY = getBoundingBox().maxY;
		candidatePos.set((double) cartPos.getX(), cartTopY, (double) cartPos.getZ());

		for (EntityPose pose : poses) {
			double poseHeight = passenger.getDimensions(pose).height();
			int spacesNeeded = MathHelper.ceil(cartTopY - candidatePos.getY() + poseHeight);
			double ceilingHeight = Dismounting.getCeilingHeight(
					candidatePos,
					spacesNeeded,
					pos -> getEntityWorld().getBlockState(pos).getCollisionShape(getEntityWorld(), pos)
			);

			if (cartTopY + poseHeight <= ceilingHeight) {
				passenger.setPose(pose);
				break;
			}
		}

		return super.updatePassengerForDismount(passenger);
	}

	@Override
	protected float getVelocityMultiplier() {
		BlockState blockState = getEntityWorld().getBlockState(getBlockPos());
		return blockState.isIn(BlockTags.RAILS) ? 1.0F : super.getVelocityMultiplier();
	}

	@Override
	public void animateDamage(float yaw) {
		setDamageWobbleSide(-getDamageWobbleSide());
		setDamageWobbleTicks(10);
		setDamageWobbleStrength(getDamageWobbleStrength() + getDamageWobbleStrength() * 10.0F);
	}

	@Override
	public boolean canHit() {
		return !isRemoved();
	}

	public static Pair<Vec3i, Vec3i> getAdjacentRailPositionsByShape(RailShape shape) {
		return ADJACENT_RAIL_POSITIONS_BY_SHAPE.get(shape);
	}

	@Override
	public Direction getMovementDirection() {
		return controller.getHorizontalFacing();
	}

	@Override
	protected double getGravity() {
		return isTouchingWater() ? 0.005 : 0.04;
	}

	@Override
	public void tick() {
		if (getDamageWobbleTicks() > 0) {
			setDamageWobbleTicks(getDamageWobbleTicks() - 1);
		}

		if (getDamageWobbleStrength() > 0.0F) {
			setDamageWobbleStrength(getDamageWobbleStrength() - 1.0F);
		}

		attemptTickInVoid();
		tickLastPos();
		tickPortalTeleportation();
		controller.tick();
		updateWaterState();

		if (isInLava()) {
			igniteByLava();
			setOnFireFromLava();
			fallDistance *= FALL_DISTANCE_LAVA_FACTOR;
		}

		firstUpdate = false;
	}

	public boolean isFirstUpdate() {
		return firstUpdate;
	}

	/**
	 * Возвращает позицию блока рельса под вагонеткой.
	 * При включённых улучшениях проверяет блок чуть ниже текущей позиции для точности.
	 */
	public BlockPos getRailOrMinecartPos() {
		int x = MathHelper.floor(getX());
		int y = MathHelper.floor(getY());
		int z = MathHelper.floor(getZ());

		if (areMinecartImprovementsEnabled(getEntityWorld())) {
			double adjustedY = getY() - ADJUSTED_Y_OFFSET - ADJUSTED_Y_EPSILON;

			if (getEntityWorld().getBlockState(BlockPos.ofFloored(x, adjustedY, z)).isIn(BlockTags.RAILS)) {
				y = MathHelper.floor(adjustedY);
			}
		} else if (getEntityWorld().getBlockState(new BlockPos(x, y - 1, z)).isIn(BlockTags.RAILS)) {
			y--;
		}

		return new BlockPos(x, y, z);
	}

	protected double getMaxSpeed(ServerWorld world) {
		return controller.getMaxSpeed(world);
	}

	public void onActivatorRail(ServerWorld serverWorld, int x, int y, int z, boolean powered) {
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
		return controller.limitSpeed(super.getMovement());
	}

	@Override
	public PositionInterpolator getInterpolator() {
		return controller.getInterpolator();
	}

	@Override
	public void onSpawnPacket(EntitySpawnS2CPacket packet) {
		super.onSpawnPacket(packet);
		controller.setLerpTargetVelocity(getVelocity());
	}

	@Override
	public void setVelocityClient(Vec3d clientVelocity) {
		controller.setLerpTargetVelocity(clientVelocity);
	}

	protected void moveOnRail(ServerWorld world) {
		controller.moveOnRail(world);
	}

	protected void moveOffRail(ServerWorld world) {
		double maxSpeed = getMaxSpeed(world);
		Vec3d velocity = getVelocity();
		setVelocity(
				MathHelper.clamp(velocity.x, -maxSpeed, maxSpeed),
				velocity.y,
				MathHelper.clamp(velocity.z, -maxSpeed, maxSpeed)
		);

		if (isOnGround()) {
			setVelocity(getVelocity().multiply(0.5));
		}

		move(MovementType.SELF, getVelocity());

		if (!isOnGround()) {
			setVelocity(getVelocity().multiply(VELOCITY_SLOWDOWN_MULTIPLIER));
		}
	}

	protected double moveAlongTrack(BlockPos pos, RailShape shape, double remainingMovement) {
		return controller.moveAlongTrack(pos, shape, remainingMovement);
	}

	@Override
	public void move(MovementType type, Vec3d movement) {
		if (areMinecartImprovementsEnabled(getEntityWorld())) {
			Vec3d targetPos = getEntityPos().add(movement);
			super.move(type, movement);
			boolean collisionHandled = controller.handleCollision();

			if (collisionHandled) {
				super.move(type, targetPos.subtract(getEntityPos()));
			}

			if (type.equals(MovementType.PISTON)) {
				onRail = false;
			}
		} else {
			super.move(type, movement);
			tickBlockCollision();
		}
	}

	@Override
	public void tickBlockCollision() {
		if (areMinecartImprovementsEnabled(getEntityWorld())) {
			super.tickBlockCollision();
		} else {
			tickBlockCollision(getEntityPos(), getEntityPos());
			clearQueuedCollisionChecks();
		}
	}

	@Override
	public boolean isOnRail() {
		return onRail;
	}

	public void setOnRail(boolean onRail) {
		this.onRail = onRail;
	}

	public boolean isYawFlipped() {
		return yawFlipped;
	}

	public void setYawFlipped(boolean yawFlipped) {
		this.yawFlipped = yawFlipped;
	}

	/**
	 * Определяет направление запуска вагонетки от активированного рельса-ускорителя.
	 * Если рельс активирован и вагонетка упирается в блок с одной стороны — толкает в противоположную.
	 */
	public Vec3d getLaunchDirection(BlockPos railPos) {
		BlockState blockState = getEntityWorld().getBlockState(railPos);

		if (!blockState.isOf(Blocks.POWERED_RAIL) || !blockState.get(PoweredRailBlock.POWERED)) {
			return Vec3d.ZERO;
		}

		RailShape railShape = blockState.get(((AbstractRailBlock) blockState.getBlock()).getShapeProperty());

		if (railShape == RailShape.EAST_WEST) {
			if (willHitBlockAt(railPos.west())) {
				return new Vec3d(1.0, 0.0, 0.0);
			}

			if (willHitBlockAt(railPos.east())) {
				return new Vec3d(-1.0, 0.0, 0.0);
			}
		} else if (railShape == RailShape.NORTH_SOUTH) {
			if (willHitBlockAt(railPos.north())) {
				return new Vec3d(0.0, 0.0, 1.0);
			}

			if (willHitBlockAt(railPos.south())) {
				return new Vec3d(0.0, 0.0, -1.0);
			}
		}

		return Vec3d.ZERO;
	}

	public boolean willHitBlockAt(BlockPos pos) {
		return getEntityWorld().getBlockState(pos).isSolidBlock(getEntityWorld(), pos);
	}

	protected Vec3d applySlowdown(Vec3d velocity) {
		double speedRetention = controller.getSpeedRetention();
		Vec3d slowed = velocity.multiply(speedRetention, 0.0, speedRetention);
		return isTouchingWater() ? slowed.multiply(VELOCITY_SLOWDOWN_MULTIPLIER) : slowed;
	}

	@Override
	protected void readCustomData(ReadView view) {
		setCustomBlockState(view.read("DisplayState", BlockState.CODEC));
		setBlockOffset(view.getInt("DisplayOffset", getDefaultBlockOffset()));
		yawFlipped = view.getBoolean("FlippedRotation", false);
		firstUpdate = view.getBoolean("HasTicked", false);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		getCustomBlockState().ifPresent(state -> view.put("DisplayState", BlockState.CODEC, state));
		int offset = getBlockOffset();

		if (offset != getDefaultBlockOffset()) {
			view.putInt("DisplayOffset", offset);
		}

		view.putBoolean("FlippedRotation", yawFlipped);
		view.putBoolean("HasTicked", firstUpdate);
	}

	/**
	 * Обрабатывает столкновение с другой сущностью.
	 * Вагонетки с самоходом имеют приоритет над обычными при обмене скоростями.
	 */
	@Override
	public void pushAwayFrom(Entity entity) {
		if (getEntityWorld().isClient()) {
			return;
		}

		if (entity.noClip || noClip) {
			return;
		}

		if (hasPassenger(entity)) {
			return;
		}

		double dx = entity.getX() - getX();
		double dz = entity.getZ() - getZ();
		double distSq = dx * dx + dz * dz;

		if (distSq < PUSH_MIN_DIST_SQ) {
			return;
		}

		double dist = Math.sqrt(distSq);
		dx /= dist;
		dz /= dist;
		double scale = Math.min(1.0 / dist, 1.0);
		dx = dx * scale * PUSH_SCALE * PUSH_HALF;
		dz = dz * scale * PUSH_SCALE * PUSH_HALF;

		if (entity instanceof AbstractMinecartEntity otherCart) {
			pushAwayFromMinecart(otherCart, dx, dz);
		} else {
			addVelocity(-dx, 0.0, -dz);
			entity.addVelocity(dx / PUSH_ENTITY_FRACTION, 0.0, dz / PUSH_ENTITY_FRACTION);
		}
	}

	private void pushAwayFromMinecart(AbstractMinecartEntity other, double xDiff, double zDiff) {
		boolean improvements = areMinecartImprovementsEnabled(getEntityWorld());
		double relX = improvements ? getVelocity().x : other.getX() - getX();
		double relZ = improvements ? getVelocity().z : other.getZ() - getZ();

		Vec3d relDir = new Vec3d(relX, 0.0, relZ).normalize();
		Vec3d facingDir = new Vec3d(
				MathHelper.cos(getYaw() * (float) (Math.PI / 180.0)),
				0.0,
				MathHelper.sin(getYaw() * (float) (Math.PI / 180.0))
		).normalize();
		double alignment = Math.abs(relDir.dotProduct(facingDir));

		if (alignment < PUSH_ALIGNMENT_THRESHOLD && !improvements) {
			return;
		}

		Vec3d myVelocity = getVelocity();
		Vec3d otherVelocity = other.getVelocity();

		if (other.isSelfPropelling() && !isSelfPropelling()) {
			setVelocity(myVelocity.multiply(0.2, 1.0, 0.2));
			addVelocity(otherVelocity.x - xDiff, 0.0, otherVelocity.z - zDiff);
			other.setVelocity(otherVelocity.multiply(VELOCITY_SLOWDOWN_MULTIPLIER, 1.0, VELOCITY_SLOWDOWN_MULTIPLIER));
		} else if (!other.isSelfPropelling() && isSelfPropelling()) {
			other.setVelocity(otherVelocity.multiply(0.2, 1.0, 0.2));
			other.addVelocity(myVelocity.x + xDiff, 0.0, myVelocity.z + zDiff);
			setVelocity(myVelocity.multiply(VELOCITY_SLOWDOWN_MULTIPLIER, 1.0, VELOCITY_SLOWDOWN_MULTIPLIER));
		} else {
			double avgX = (otherVelocity.x + myVelocity.x) / 2.0;
			double avgZ = (otherVelocity.z + myVelocity.z) / 2.0;
			setVelocity(myVelocity.multiply(0.2, 1.0, 0.2));
			addVelocity(avgX - xDiff, 0.0, avgZ - zDiff);
			other.setVelocity(otherVelocity.multiply(0.2, 1.0, 0.2));
			other.addVelocity(avgX + xDiff, 0.0, avgZ + zDiff);
		}
	}

	public BlockState getContainedBlock() {
		return getCustomBlockState().orElseGet(this::getDefaultContainedBlock);
	}

	private Optional<BlockState> getCustomBlockState() {
		return getDataTracker().get(CUSTOM_BLOCK_STATE);
	}

	public BlockState getDefaultContainedBlock() {
		return Blocks.AIR.getDefaultState();
	}

	public int getBlockOffset() {
		return getDataTracker().get(BLOCK_OFFSET);
	}

	public int getDefaultBlockOffset() {
		return DEFAULT_BLOCK_OFFSET;
	}

	public void setCustomBlockState(Optional<BlockState> customBlockState) {
		getDataTracker().set(CUSTOM_BLOCK_STATE, customBlockState);
	}

	public void setBlockOffset(int offset) {
		getDataTracker().set(BLOCK_OFFSET, offset);
	}

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
