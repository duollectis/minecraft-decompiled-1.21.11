package net.minecraft.entity.vehicle;

import com.mojang.datafixers.util.Pair;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PoweredRailBlock;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.PositionInterpolator;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Стандартный контроллер движения вагонетки, реализующий классическую физику рельсов Minecraft.
 * Управляет движением по рельсам ({@link #moveOnRail}), обработкой столкновений ({@link #handleCollision}),
 * привязкой позиции к рельсу ({@link #snapPositionToRail}) и интерполяцией позиции на клиенте.
 */
public class DefaultMinecartController extends MinecartController {

	private static final double MIN_VELOCITY_THRESHOLD = 0.01;
	private static final double MAX_SPEED_NORMAL = 0.4;
	private static final double MAX_SPEED_IN_WATER = 0.2;
	private static final double COLLISION_BOX_EXPAND = 0.2;
	private static final double SLOPE_GRAVITY = 0.0078125;
	private static final double SLOPE_GRAVITY_IN_WATER_FACTOR = 0.2;
	private static final double BRAKE_STOP_THRESHOLD = 0.03;
	private static final double POWERED_RAIL_BOOST = 0.06;
	private static final double POWERED_RAIL_NUDGE = 0.02;
	private static final double PASSENGER_SLOWDOWN = 0.75;
	private static final double SPEED_RETENTION_WITH_PASSENGERS = 0.997;
	private static final double SPEED_RETENTION_EMPTY = 0.96;
	private static final double YAW_FLIP_THRESHOLD_LOW = -170.0;
	private static final double YAW_FLIP_THRESHOLD_HIGH = 170.0;
	private static final double MIN_MOVEMENT_SQ = 0.001;
	private static final double RAIL_SURFACE_OFFSET = 0.0625;
	private static final double RAIL_AXIS_Y_SCALE = 2.0;

	private final PositionInterpolator interpolator;
	private Vec3d velocity = Vec3d.ZERO;

	public DefaultMinecartController(AbstractMinecartEntity minecart) {
		super(minecart);
		interpolator = new PositionInterpolator(minecart, this::onLerp);
	}

	@Override
	public PositionInterpolator getInterpolator() {
		return interpolator;
	}

	/**
	 * Вызывается интерполятором при каждом шаге клиентской интерполяции позиции.
	 * Синхронизирует вектор скорости вагонетки с последним полученным от сервера значением.
	 */
	public void onLerp(PositionInterpolator interpolator) {
		setVelocity(velocity);
	}

	@Override
	public void setLerpTargetVelocity(Vec3d vec3d) {
		velocity = vec3d;
		setVelocity(velocity);
	}

	@Override
	public void tick() {
		if (getWorld() instanceof ServerWorld serverWorld) {
			minecart.applyGravity();
			BlockPos railPos = minecart.getRailOrMinecartPos();
			BlockState blockState = getWorld().getBlockState(railPos);
			boolean isOnRail = AbstractRailBlock.isRail(blockState);
			minecart.setOnRail(isOnRail);

			if (isOnRail) {
				moveOnRail(serverWorld);

				if (blockState.isOf(Blocks.ACTIVATOR_RAIL)) {
					minecart.onActivatorRail(
							serverWorld,
							railPos.getX(), railPos.getY(), railPos.getZ(),
							blockState.get(PoweredRailBlock.POWERED)
					);
				}
			} else {
				minecart.moveOffRail(serverWorld);
			}

			minecart.tickBlockCollision();
			setPitch(0.0F);

			double deltaX = minecart.lastX - getX();
			double deltaZ = minecart.lastZ - getZ();

			if (deltaX * deltaX + deltaZ * deltaZ > MIN_MOVEMENT_SQ) {
				setYaw((float) (MathHelper.atan2(deltaZ, deltaX) * 180.0 / Math.PI));

				if (minecart.isYawFlipped()) {
					setYaw(getYaw() + 180.0F);
				}
			}

			double yawDelta = MathHelper.wrapDegrees(getYaw() - minecart.lastYaw);

			if (yawDelta < YAW_FLIP_THRESHOLD_LOW || yawDelta >= YAW_FLIP_THRESHOLD_HIGH) {
				setYaw(getYaw() + 180.0F);
				minecart.setYawFlipped(!minecart.isYawFlipped());
			}

			setPitch(getPitch() % 360.0F);
			setYaw(getYaw() % 360.0F);
			handleCollision();
		} else {
			if (interpolator.isInterpolating()) {
				interpolator.tick();
			} else {
				minecart.refreshPosition();
				setPitch(getPitch() % 360.0F);
				setYaw(getYaw() % 360.0F);
			}
		}
	}

	/**
	 * Выполняет полный цикл физики движения по рельсу за один тик:
	 * применяет гравитацию уклона, вычисляет направление по форме рельса,
	 * обрабатывает ввод игрока, торможение/ускорение и коррекцию высоты по наклону.
	 */
	@Override
	public void moveOnRail(ServerWorld world) {
		BlockPos blockPos = minecart.getRailOrMinecartPos();
		BlockState blockState = getWorld().getBlockState(blockPos);
		minecart.onLanding();

		double cartX = minecart.getX();
		double cartY = minecart.getY();
		double cartZ = minecart.getZ();
		Vec3d snappedPos = snapPositionToRail(cartX, cartY, cartZ);
		cartY = blockPos.getY();

		boolean isPowered = false;
		boolean isBraking = false;

		if (blockState.isOf(Blocks.POWERED_RAIL)) {
			isPowered = blockState.get(PoweredRailBlock.POWERED);
			isBraking = !isPowered;
		}

		double slopeGravity = SLOPE_GRAVITY;

		if (minecart.isTouchingWater()) {
			slopeGravity *= SLOPE_GRAVITY_IN_WATER_FACTOR;
		}

		Vec3d currentVelocity = getVelocity();
		RailShape railShape = blockState.get(((AbstractRailBlock) blockState.getBlock()).getShapeProperty());

		switch (railShape) {
			case ASCENDING_EAST -> {
				setVelocity(currentVelocity.add(-slopeGravity, 0.0, 0.0));
				cartY++;
			}
			case ASCENDING_WEST -> {
				setVelocity(currentVelocity.add(slopeGravity, 0.0, 0.0));
				cartY++;
			}
			case ASCENDING_NORTH -> {
				setVelocity(currentVelocity.add(0.0, 0.0, slopeGravity));
				cartY++;
			}
			case ASCENDING_SOUTH -> {
				setVelocity(currentVelocity.add(0.0, 0.0, -slopeGravity));
				cartY++;
			}
		}

		currentVelocity = getVelocity();
		Pair<Vec3i, Vec3i> railEnds = AbstractMinecartEntity.getAdjacentRailPositionsByShape(railShape);
		Vec3i endA = railEnds.getFirst();
		Vec3i endB = railEnds.getSecond();
		double dirX = endB.getX() - endA.getX();
		double dirZ = endB.getZ() - endA.getZ();
		double dirLength = Math.sqrt(dirX * dirX + dirZ * dirZ);
		double dotProduct = currentVelocity.x * dirX + currentVelocity.z * dirZ;

		if (dotProduct < 0.0) {
			dirX = -dirX;
			dirZ = -dirZ;
		}

		double speed = Math.min(2.0, currentVelocity.horizontalLength());
		currentVelocity = new Vec3d(speed * dirX / dirLength, currentVelocity.y, speed * dirZ / dirLength);
		setVelocity(currentVelocity);

		Entity firstPassenger = minecart.getFirstPassenger();
		Vec3d playerInput = firstPassenger instanceof ServerPlayerEntity serverPlayer
				? serverPlayer.getInputVelocityForMinecart()
				: Vec3d.ZERO;

		if (firstPassenger instanceof PlayerEntity && playerInput.lengthSquared() > 0.0) {
			Vec3d normalizedInput = playerInput.normalize();
			double playerSpeedSq = getVelocity().horizontalLengthSquared();

			if (normalizedInput.lengthSquared() > 0.0 && playerSpeedSq < MIN_VELOCITY_THRESHOLD) {
				setVelocity(getVelocity().add(playerInput.x * 0.001, 0.0, playerInput.z * 0.001));
				isBraking = false;
			}
		}

		if (isBraking) {
			double brakeSpeed = getVelocity().horizontalLength();

			if (brakeSpeed < BRAKE_STOP_THRESHOLD) {
				setVelocity(Vec3d.ZERO);
			} else {
				setVelocity(getVelocity().multiply(0.5, 0.0, 0.5));
			}
		}

		double railEndAX = blockPos.getX() + 0.5 + endA.getX() * 0.5;
		double railEndAZ = blockPos.getZ() + 0.5 + endA.getZ() * 0.5;
		double railEndBX = blockPos.getX() + 0.5 + endB.getX() * 0.5;
		double railEndBZ = blockPos.getZ() + 0.5 + endB.getZ() * 0.5;
		dirX = railEndBX - railEndAX;
		dirZ = railEndBZ - railEndAZ;

		double interpolation;

		if (dirX == 0.0) {
			interpolation = cartZ - blockPos.getZ();
		} else if (dirZ == 0.0) {
			interpolation = cartX - blockPos.getX();
		} else {
			double relX = cartX - railEndAX;
			double relZ = cartZ - railEndAZ;
			interpolation = (relX * dirX + relZ * dirZ) * 2.0;
		}

		cartX = railEndAX + dirX * interpolation;
		cartZ = railEndAZ + dirZ * interpolation;
		setPos(cartX, cartY, cartZ);

		double passengerSlowdown = minecart.hasPassengers() ? PASSENGER_SLOWDOWN : 1.0;
		double maxSpeed = minecart.getMaxSpeed(world);
		currentVelocity = getVelocity();
		minecart.move(
				MovementType.SELF,
				new Vec3d(
						MathHelper.clamp(passengerSlowdown * currentVelocity.x, -maxSpeed, maxSpeed),
						0.0,
						MathHelper.clamp(passengerSlowdown * currentVelocity.z, -maxSpeed, maxSpeed)
				)
		);

		if (endA.getY() != 0
				&& MathHelper.floor(minecart.getX()) - blockPos.getX() == endA.getX()
				&& MathHelper.floor(minecart.getZ()) - blockPos.getZ() == endA.getZ()) {
			setPos(minecart.getX(), minecart.getY() + endA.getY(), minecart.getZ());
		} else if (endB.getY() != 0
				&& MathHelper.floor(minecart.getX()) - blockPos.getX() == endB.getX()
				&& MathHelper.floor(minecart.getZ()) - blockPos.getZ() == endB.getZ()) {
			setPos(minecart.getX(), minecart.getY() + endB.getY(), minecart.getZ());
		}

		setVelocity(minecart.applySlowdown(getVelocity()));

		Vec3d newSnappedPos = snapPositionToRail(minecart.getX(), minecart.getY(), minecart.getZ());

		if (newSnappedPos != null && snappedPos != null) {
			double heightDiff = (snappedPos.y - newSnappedPos.y) * 0.05;
			Vec3d vel = getVelocity();
			double horizSpeed = vel.horizontalLength();

			if (horizSpeed > 0.0) {
				double factor = (horizSpeed + heightDiff) / horizSpeed;
				setVelocity(vel.multiply(factor, 1.0, factor));
			}

			setPos(minecart.getX(), newSnappedPos.y, minecart.getZ());
		}

		int newBlockX = MathHelper.floor(minecart.getX());
		int newBlockZ = MathHelper.floor(minecart.getZ());

		if (newBlockX != blockPos.getX() || newBlockZ != blockPos.getZ()) {
			Vec3d vel = getVelocity();
			double horizSpeed = vel.horizontalLength();
			setVelocity(
					horizSpeed * (newBlockX - blockPos.getX()),
					vel.y,
					horizSpeed * (newBlockZ - blockPos.getZ())
			);
		}

		if (isPowered) {
			Vec3d vel = getVelocity();
			double horizSpeed = vel.horizontalLength();

			if (horizSpeed > MIN_VELOCITY_THRESHOLD) {
				setVelocity(vel.add(
						vel.x / horizSpeed * POWERED_RAIL_BOOST,
						0.0,
						vel.z / horizSpeed * POWERED_RAIL_BOOST
				));
			} else {
				double newVelX = vel.x;
				double newVelZ = vel.z;

				if (railShape == RailShape.EAST_WEST) {
					if (minecart.willHitBlockAt(blockPos.west())) {
						newVelX = POWERED_RAIL_NUDGE;
					} else if (minecart.willHitBlockAt(blockPos.east())) {
						newVelX = -POWERED_RAIL_NUDGE;
					}
				} else {
					if (railShape != RailShape.NORTH_SOUTH) {
						return;
					}

					if (minecart.willHitBlockAt(blockPos.north())) {
						newVelZ = POWERED_RAIL_NUDGE;
					} else if (minecart.willHitBlockAt(blockPos.south())) {
						newVelZ = -POWERED_RAIL_NUDGE;
					}
				}

				setVelocity(newVelX, vel.y, newVelZ);
			}
		}
	}

	/**
	 * Симулирует движение вагонетки вперёд на {@code movement} блоков вдоль рельса
	 * и возвращает итоговую позицию, привязанную к рельсу. Используется для предсказания позиции.
	 */
	public @Nullable Vec3d simulateMovement(double x, double y, double z, double movement) {
		int blockX = MathHelper.floor(x);
		int blockY = MathHelper.floor(y);
		int blockZ = MathHelper.floor(z);

		if (getWorld().getBlockState(new BlockPos(blockX, blockY - 1, blockZ)).isIn(BlockTags.RAILS)) {
			blockY--;
		}

		BlockState blockState = getWorld().getBlockState(new BlockPos(blockX, blockY, blockZ));

		if (!AbstractRailBlock.isRail(blockState)) {
			return null;
		}

		RailShape railShape = blockState.get(((AbstractRailBlock) blockState.getBlock()).getShapeProperty());
		y = blockY;

		if (railShape.isAscending()) {
			y = blockY + 1;
		}

		Pair<Vec3i, Vec3i> railEnds = AbstractMinecartEntity.getAdjacentRailPositionsByShape(railShape);
		Vec3i endA = railEnds.getFirst();
		Vec3i endB = railEnds.getSecond();
		double dirX = endB.getX() - endA.getX();
		double dirZ = endB.getZ() - endA.getZ();
		double dirLength = Math.sqrt(dirX * dirX + dirZ * dirZ);
		dirX /= dirLength;
		dirZ /= dirLength;
		x += dirX * movement;
		z += dirZ * movement;

		if (endA.getY() != 0
				&& MathHelper.floor(x) - blockX == endA.getX()
				&& MathHelper.floor(z) - blockZ == endA.getZ()) {
			y += endA.getY();
		} else if (endB.getY() != 0
				&& MathHelper.floor(x) - blockX == endB.getX()
				&& MathHelper.floor(z) - blockZ == endB.getZ()) {
			y += endB.getY();
		}

		return snapPositionToRail(x, y, z);
	}

	/**
	 * Привязывает произвольную позицию к ближайшей точке на рельсе, вычисляя интерполяцию
	 * вдоль оси рельса. Возвращает {@code null}, если под позицией нет рельса.
	 */
	public @Nullable Vec3d snapPositionToRail(double x, double y, double z) {
		int blockX = MathHelper.floor(x);
		int blockY = MathHelper.floor(y);
		int blockZ = MathHelper.floor(z);

		if (getWorld().getBlockState(new BlockPos(blockX, blockY - 1, blockZ)).isIn(BlockTags.RAILS)) {
			blockY--;
		}

		BlockState blockState = getWorld().getBlockState(new BlockPos(blockX, blockY, blockZ));

		if (!AbstractRailBlock.isRail(blockState)) {
			return null;
		}

		RailShape railShape = blockState.get(((AbstractRailBlock) blockState.getBlock()).getShapeProperty());
		Pair<Vec3i, Vec3i> railEnds = AbstractMinecartEntity.getAdjacentRailPositionsByShape(railShape);
		Vec3i endA = railEnds.getFirst();
		Vec3i endB = railEnds.getSecond();

		double endAX = blockX + 0.5 + endA.getX() * 0.5;
		double endAY = blockY + RAIL_SURFACE_OFFSET + endA.getY() * 0.5;
		double endAZ = blockZ + 0.5 + endA.getZ() * 0.5;
		double endBX = blockX + 0.5 + endB.getX() * 0.5;
		double endBY = blockY + RAIL_SURFACE_OFFSET + endB.getY() * 0.5;
		double endBZ = blockZ + 0.5 + endB.getZ() * 0.5;

		double axisX = endBX - endAX;
		double axisYScaled = (endBY - endAY) * RAIL_AXIS_Y_SCALE;
		double axisZ = endBZ - endAZ;

		double interpolation;

		if (axisX == 0.0) {
			interpolation = z - blockZ;
		} else if (axisZ == 0.0) {
			interpolation = x - blockX;
		} else {
			double relX = x - endAX;
			double relZ = z - endAZ;
			interpolation = (relX * axisX + relZ * axisZ) * RAIL_AXIS_Y_SCALE;
		}

		x = endAX + axisX * interpolation;
		y = endAY + axisYScaled * interpolation;
		z = endAZ + axisZ * interpolation;

		if (axisYScaled < 0.0) {
			y++;
		} else if (axisYScaled > 0.0) {
			y += 0.5;
		}

		return new Vec3d(x, y, z);
	}

	@Override
	public double moveAlongTrack(BlockPos blockPos, RailShape railShape, double remainingMovement) {
		return 0.0;
	}

	/**
	 * Обрабатывает столкновения вагонетки с другими сущностями в радиусе {@code COLLISION_BOX_EXPAND}.
	 * Быстрые вагонетки подбирают пассажиров или отталкивают сущности; медленные — только отталкивают другие вагонетки.
	 */
	@Override
	public boolean handleCollision() {
		Box box = minecart.getBoundingBox().expand(COLLISION_BOX_EXPAND, 0.0, COLLISION_BOX_EXPAND);

		if (minecart.isRideable() && getVelocity().horizontalLengthSquared() >= MIN_VELOCITY_THRESHOLD) {
			List<Entity> entities = getWorld().getOtherEntities(
					minecart, box, EntityPredicates.canBePushedBy(minecart)
			);

			for (Entity entity : entities) {
				if (entity instanceof PlayerEntity
						|| entity instanceof IronGolemEntity
						|| entity instanceof AbstractMinecartEntity
						|| minecart.hasPassengers()
						|| entity.hasVehicle()) {
					entity.pushAwayFrom(minecart);
				} else {
					entity.startRiding(minecart);
				}
			}
		} else {
			for (Entity entity : getWorld().getOtherEntities(minecart, box)) {
				if (!minecart.hasPassenger(entity)
						&& entity.isPushable()
						&& entity instanceof AbstractMinecartEntity) {
					entity.pushAwayFrom(minecart);
				}
			}
		}

		return false;
	}

	/**
	 * Возвращает горизонтальное направление движения вагонетки с учётом флага переворота оси.
	 */
	@Override
	public Direction getHorizontalFacing() {
		return minecart.isYawFlipped()
				? minecart.getHorizontalFacing().getOpposite().rotateYClockwise()
				: minecart.getHorizontalFacing().rotateYClockwise();
	}

	/**
	 * Ограничивает скорость вагонетки по горизонтальным осям до {@code MAX_SPEED_NORMAL}.
	 * Защищает от NaN-значений, возвращая нулевой вектор при некорректных данных.
	 */
	@Override
	public Vec3d limitSpeed(Vec3d velocity) {
		if (Double.isNaN(velocity.x) || Double.isNaN(velocity.y) || Double.isNaN(velocity.z)) {
			return Vec3d.ZERO;
		}

		return new Vec3d(
				MathHelper.clamp(velocity.x, -MAX_SPEED_NORMAL, MAX_SPEED_NORMAL),
				velocity.y,
				MathHelper.clamp(velocity.z, -MAX_SPEED_NORMAL, MAX_SPEED_NORMAL)
		);
	}

	/** Возвращает максимальную скорость: замедляет вагонетку вдвое при движении в воде. */
	@Override
	public double getMaxSpeed(ServerWorld world) {
		return minecart.isTouchingWater() ? MAX_SPEED_IN_WATER : MAX_SPEED_NORMAL;
	}

	/** Возвращает коэффициент сохранения скорости за тик: с пассажиром — 0.997, без — 0.96. */
	@Override
	public double getSpeedRetention() {
		return minecart.hasPassengers() ? SPEED_RETENTION_WITH_PASSENGERS : SPEED_RETENTION_EMPTY;
	}
}
