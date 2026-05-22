package net.minecraft.entity.vehicle;

import com.mojang.datafixers.util.Pair;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PoweredRailBlock;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

/**
 * Экспериментальный контроллер движения вагонетки (флаг {@code minecart_improvements}).
 * Реализует субтиковое движение по рельсам с плавной клиентской интерполяцией через
 * систему шагов ({@link Step}). За один серверный тик вагонетка может пройти несколько
 * блоков рельса, что устраняет «дёрганье» при высоких скоростях.
 */
public class ExperimentalMinecartController extends MinecartController {

	public static final int REFRESH_FREQUENCY = 3;
	public static final double RAIL_VERTICAL_OFFSET = 0.1;
	public static final double MIN_VELOCITY_SQUARED_THRESHOLD = 0.005;

	private static final double MIN_VELOCITY_LENGTH = 1.0E-5F;
	private static final double PLAYER_INPUT_SPEED_THRESHOLD = 0.01;
	private static final double PLAYER_INPUT_NUDGE = 0.001;
	private static final double POWERED_RAIL_BOOST = 0.06;
	private static final double POWERED_RAIL_LAUNCH = 0.2;
	private static final double POWERED_RAIL_STOP_THRESHOLD = 0.01;
	private static final double BRAKE_STOP_THRESHOLD = 0.03;
	private static final double SLOPE_GRAVITY_BASE = 0.0078125;
	private static final double SLOPE_GRAVITY_SPEED_FACTOR = 0.02;
	private static final double SLOPE_GRAVITY_WATER_FACTOR = 0.2;
	private static final double YAW_FLIP_RANGE_LOW = 175.0;
	private static final double YAW_FLIP_RANGE_HIGH = 185.0;
	private static final double COLLISION_BOX_EXPAND_PICKUP = 0.2;
	private static final double COLLISION_BOX_EXPAND_PUSH = 1.0E-7;
	private static final float PITCH_LERP_FACTOR = 0.2F;
	private static final float LERP_STEPS_TOTAL = 3.0F;

	private @Nullable InterpolatedStep lastReturnedInterpolatedStep;
	private int lastQueriedTicksToNextRefresh;
	private float lastQueriedTickProgress;
	private int ticksToNextRefresh = 0;

	public final List<Step> stagingLerpSteps = new LinkedList<>();
	public final List<Step> currentLerpSteps = new LinkedList<>();
	public double totalWeight = 0.0;
	public Step initialStep = Step.ZERO;

	public ExperimentalMinecartController(AbstractMinecartEntity minecart) {
		super(minecart);
	}

	@Override
	public void tick() {
		if (getWorld() instanceof ServerWorld serverWorld) {
			BlockPos railPos = minecart.getRailOrMinecartPos();
			BlockState blockState = getWorld().getBlockState(railPos);

			if (minecart.isFirstUpdate()) {
				minecart.setOnRail(AbstractRailBlock.isRail(blockState));
				adjustToRail(railPos, blockState, true);
			}

			minecart.applyGravity();
			minecart.moveOnRail(serverWorld);
		} else {
			tickClient();
			boolean isOnRail = AbstractRailBlock.isRail(
					getWorld().getBlockState(minecart.getRailOrMinecartPos())
			);
			minecart.setOnRail(isOnRail);
		}
	}

	private void tickClient() {
		if (--ticksToNextRefresh > 0) {
			return;
		}

		setInitialStep();
		currentLerpSteps.clear();

		if (stagingLerpSteps.isEmpty()) {
			return;
		}

		currentLerpSteps.addAll(stagingLerpSteps);
		stagingLerpSteps.clear();
		totalWeight = 0.0;

		for (Step step : currentLerpSteps) {
			totalWeight += step.weight;
		}

		ticksToNextRefresh = totalWeight == 0.0 ? 0 : REFRESH_FREQUENCY;

		if (hasCurrentLerpSteps()) {
			setPos(getLerpedPosition(1.0F));
			setVelocity(getLerpedVelocity(1.0F));
			setPitch(getLerpedPitch(1.0F));
			setYaw(getLerpedYaw(1.0F));
		}
	}

	public void setInitialStep() {
		initialStep = new Step(getPos(), getVelocity(), getYaw(), getPitch(), 0.0F);
	}

	public boolean hasCurrentLerpSteps() {
		return !currentLerpSteps.isEmpty();
	}

	public float getLerpedPitch(float tickProgress) {
		InterpolatedStep step = getLerpedStep(tickProgress);
		return MathHelper.lerpAngleDegrees(step.partialTicksInStep, step.previousStep.xRot, step.currentStep.xRot);
	}

	public float getLerpedYaw(float tickProgress) {
		InterpolatedStep step = getLerpedStep(tickProgress);
		return MathHelper.lerpAngleDegrees(step.partialTicksInStep, step.previousStep.yRot, step.currentStep.yRot);
	}

	public Vec3d getLerpedPosition(float tickProgress) {
		InterpolatedStep step = getLerpedStep(tickProgress);
		return MathHelper.lerp(step.partialTicksInStep, step.previousStep.position, step.currentStep.position);
	}

	public Vec3d getLerpedVelocity(float tickProgress) {
		InterpolatedStep step = getLerpedStep(tickProgress);
		return MathHelper.lerp(step.partialTicksInStep, step.previousStep.movement, step.currentStep.movement);
	}

	/**
	 * Вычисляет интерполированный шаг для заданного прогресса тика.
	 * Кэширует результат для повторных вызовов с теми же параметрами.
	 */
	private InterpolatedStep getLerpedStep(float tickProgress) {
		if (tickProgress == lastQueriedTickProgress
				&& ticksToNextRefresh == lastQueriedTicksToNextRefresh
				&& lastReturnedInterpolatedStep != null) {
			return lastReturnedInterpolatedStep;
		}

		float normalizedProgress = (REFRESH_FREQUENCY - ticksToNextRefresh + tickProgress) / LERP_STEPS_TOTAL;
		float accumulated = 0.0F;
		float partialInStep = 1.0F;
		boolean found = false;
		int stepIndex = 0;

		for (int i = 0; i < currentLerpSteps.size(); i++) {
			float stepWeight = currentLerpSteps.get(i).weight;

			if (stepWeight <= 0.0F) {
				continue;
			}

			accumulated += stepWeight;

			if (accumulated >= totalWeight * normalizedProgress) {
				float prevAccumulated = accumulated - stepWeight;
				partialInStep = (float) ((normalizedProgress * totalWeight - prevAccumulated) / stepWeight);
				stepIndex = i;
				found = true;
				break;
			}
		}

		if (!found) {
			stepIndex = currentLerpSteps.size() - 1;
		}

		Step currentStep = currentLerpSteps.get(stepIndex);
		Step previousStep = stepIndex > 0 ? currentLerpSteps.get(stepIndex - 1) : initialStep;
		lastReturnedInterpolatedStep = new InterpolatedStep(partialInStep, currentStep, previousStep);
		lastQueriedTicksToNextRefresh = ticksToNextRefresh;
		lastQueriedTickProgress = tickProgress;
		return lastReturnedInterpolatedStep;
	}

	/**
	 * Выравнивает позицию и угол вагонетки по рельсу.
	 * При {@code ignoreWeight = true} шаг добавляется с нулевым весом (мгновенное выравнивание без анимации).
	 *
	 * @param pos позиция блока рельса
	 * @param blockState состояние блока рельса
	 * @param ignoreWeight если {@code true} — шаг не учитывается в интерполяции
	 */
	public void adjustToRail(BlockPos pos, BlockState blockState, boolean ignoreWeight) {
		if (!AbstractRailBlock.isRail(blockState)) {
			return;
		}

		RailShape railShape = blockState.get(((AbstractRailBlock) blockState.getBlock()).getShapeProperty());
		Pair<Vec3i, Vec3i> railEnds = AbstractMinecartEntity.getAdjacentRailPositionsByShape(railShape);
		Vec3d dirA = new Vec3d(railEnds.getFirst()).multiply(0.5);
		Vec3d dirB = new Vec3d(railEnds.getSecond()).multiply(0.5);
		Vec3d horizontalA = dirA.getHorizontal();
		Vec3d horizontalB = dirB.getHorizontal();

		boolean preferB = getVelocity().length() > MIN_VELOCITY_LENGTH
				&& getVelocity().dotProduct(horizontalA) < getVelocity().dotProduct(horizontalB);

		if (preferB || ascends(horizontalB, railShape)) {
			Vec3d temp = horizontalA;
			horizontalA = horizontalB;
			horizontalB = temp;
		}

		float yaw = 180.0F - (float) (Math.atan2(horizontalA.z, horizontalA.x) * 180.0 / Math.PI);
		yaw += minecart.isYawFlipped() ? 180.0F : 0.0F;

		Vec3d currentPos = getPos();
		boolean isDiagonal = dirA.getX() != dirB.getX() && dirA.getZ() != dirB.getZ();
		Vec3d targetPos;

		if (isDiagonal) {
			Vec3d railDir = dirB.subtract(dirA);
			Vec3d relativePos = currentPos.subtract(pos.toBottomCenterPos()).subtract(dirA);
			Vec3d projected = railDir.multiply(railDir.dotProduct(relativePos) / railDir.dotProduct(railDir));
			targetPos = pos.toBottomCenterPos().add(dirA).add(projected);
			yaw = 180.0F - (float) (Math.atan2(projected.z, projected.x) * 180.0 / Math.PI);
			yaw += minecart.isYawFlipped() ? 180.0F : 0.0F;
		} else {
			boolean alignZ = dirA.subtract(dirB).x != 0.0;
			boolean alignX = dirA.subtract(dirB).z != 0.0;
			targetPos = new Vec3d(
					alignX ? pos.toCenterPos().x : currentPos.x,
					pos.getY(),
					alignZ ? pos.toCenterPos().z : currentPos.z
			);
		}

		Vec3d posOffset = targetPos.subtract(currentPos);
		setPos(currentPos.add(posOffset));

		float pitch = 0.0F;
		boolean isSloped = dirA.getY() != dirB.getY();

		if (isSloped) {
			Vec3d slopeEnd = pos.toBottomCenterPos().add(horizontalB);
			double distToEnd = slopeEnd.distanceTo(getPos());
			setPos(getPos().add(0.0, distToEnd + RAIL_VERTICAL_OFFSET, 0.0));
			pitch = minecart.isYawFlipped() ? 45.0F : -45.0F;
		} else {
			setPos(getPos().add(0.0, RAIL_VERTICAL_OFFSET, 0.0));
		}

		setAngles(yaw, pitch);

		double distMoved = currentPos.distanceTo(getPos());

		if (distMoved > 0.0) {
			stagingLerpSteps.add(new Step(
					getPos(), getVelocity(), getYaw(), getPitch(),
					ignoreWeight ? 0.0F : (float) distMoved
			));
		}
	}

	private void setAngles(float yaw, float pitch) {
		double yawDiff = Math.abs(yaw - getYaw());

		if (yawDiff >= YAW_FLIP_RANGE_LOW && yawDiff <= YAW_FLIP_RANGE_HIGH) {
			minecart.setYawFlipped(!minecart.isYawFlipped());
			yaw -= 180.0F;
			pitch *= -1.0F;
		}

		pitch = Math.clamp(pitch, -45.0F, 45.0F);
		setPitch(pitch % 360.0F);
		setYaw(yaw % 360.0F);
	}

	/**
	 * Выполняет субтиковое движение по рельсам: за один тик вагонетка может пройти несколько
	 * блоков рельса, пока не исчерпает оставшееся расстояние ({@code remainingMovement}).
	 * Каждая итерация добавляет шаг в {@code stagingLerpSteps} для клиентской интерполяции.
	 */
	@Override
	public void moveOnRail(ServerWorld world) {
		for (MoveIteration iteration = new MoveIteration();
				iteration.shouldContinue() && minecart.isAlive();
				iteration.initial = false) {

			Vec3d velocityBefore = getVelocity();
			BlockPos blockPos = minecart.getRailOrMinecartPos();
			BlockState blockState = getWorld().getBlockState(blockPos);
			boolean isOnRail = AbstractRailBlock.isRail(blockState);

			if (minecart.isOnRail() != isOnRail) {
				minecart.setOnRail(isOnRail);
				adjustToRail(blockPos, blockState, false);
			}

			if (isOnRail) {
				minecart.onLanding();
				minecart.resetPosition();

				if (blockState.isOf(Blocks.ACTIVATOR_RAIL)) {
					minecart.onActivatorRail(
							world,
							blockPos.getX(), blockPos.getY(), blockPos.getZ(),
							blockState.get(PoweredRailBlock.POWERED)
					);
				}

				RailShape railShape = blockState.get(((AbstractRailBlock) blockState.getBlock()).getShapeProperty());
				Vec3d newHorizontalVelocity = calcNewHorizontalVelocity(
						world, velocityBefore.getHorizontal(), iteration, blockPos, blockState, railShape
				);

				if (iteration.initial) {
					iteration.remainingMovement = newHorizontalVelocity.horizontalLength();
				} else {
					iteration.remainingMovement += newHorizontalVelocity.horizontalLength()
							- velocityBefore.horizontalLength();
				}

				setVelocity(newHorizontalVelocity);
				iteration.remainingMovement = minecart.moveAlongTrack(
						blockPos, railShape, iteration.remainingMovement
				);
			} else {
				minecart.moveOffRail(world);
				iteration.remainingMovement = 0.0;
			}

			Vec3d currentPos = getPos();
			Vec3d posChange = currentPos.subtract(minecart.getLastRenderPos());
			double distMoved = posChange.length();

			if (distMoved > MIN_VELOCITY_LENGTH) {
				if (posChange.horizontalLengthSquared() > MIN_VELOCITY_LENGTH) {
					float yaw = 180.0F - (float) (Math.atan2(posChange.z, posChange.x) * 180.0 / Math.PI);
					float pitch;

					if (minecart.isOnGround() && !minecart.isOnRail()) {
						pitch = 0.0F;
					} else {
						pitch = 90.0F - (float) (Math.atan2(
								posChange.horizontalLength(), posChange.y
						) * 180.0 / Math.PI);
					}

					yaw += minecart.isYawFlipped() ? 180.0F : 0.0F;
					pitch *= minecart.isYawFlipped() ? -1.0F : 1.0F;
					setAngles(yaw, pitch);
				} else if (!minecart.isOnRail()) {
					setPitch(minecart.isOnGround()
							? 0.0F
							: MathHelper.lerpAngleDegrees(PITCH_LERP_FACTOR, getPitch(), 0.0F)
					);
				}

				stagingLerpSteps.add(new Step(
						currentPos, getVelocity(), getYaw(), getPitch(),
						(float) Math.min(distMoved, getMaxSpeed(world))
				));
			} else if (velocityBefore.horizontalLengthSquared() > 0.0) {
				stagingLerpSteps.add(new Step(currentPos, getVelocity(), getYaw(), getPitch(), 1.0F));
			}

			if (distMoved > MIN_VELOCITY_LENGTH || iteration.initial) {
				minecart.tickBlockCollision();
				minecart.tickBlockCollision();
			}
		}
	}

	private Vec3d calcNewHorizontalVelocity(
			ServerWorld world,
			Vec3d horizontalVelocity,
			MoveIteration iteration,
			BlockPos pos,
			BlockState railState,
			RailShape railShape
	) {
		Vec3d velocity = horizontalVelocity;

		if (!iteration.slopeVelocityApplied) {
			Vec3d withSlope = applySlopeVelocity(velocity, railShape);

			if (withSlope.horizontalLengthSquared() != velocity.horizontalLengthSquared()) {
				iteration.slopeVelocityApplied = true;
				velocity = withSlope;
			}
		}

		if (iteration.initial) {
			Vec3d withInput = applyInitialVelocity(velocity);

			if (withInput.horizontalLengthSquared() != velocity.horizontalLengthSquared()) {
				iteration.decelerated = true;
				velocity = withInput;
			}
		}

		if (!iteration.decelerated) {
			Vec3d decelerated = decelerateFromPoweredRail(velocity, railState);

			if (decelerated.horizontalLengthSquared() != velocity.horizontalLengthSquared()) {
				iteration.decelerated = true;
				velocity = decelerated;
			}
		}

		if (iteration.initial) {
			velocity = minecart.applySlowdown(velocity);

			if (velocity.lengthSquared() > 0.0) {
				double maxSpeed = Math.min(velocity.length(), minecart.getMaxSpeed(world));
				velocity = velocity.normalize().multiply(maxSpeed);
			}
		}

		if (!iteration.accelerated) {
			Vec3d accelerated = accelerateFromPoweredRail(velocity, pos, railState);

			if (accelerated.horizontalLengthSquared() != velocity.horizontalLengthSquared()) {
				iteration.accelerated = true;
				velocity = accelerated;
			}
		}

		return velocity;
	}

	private Vec3d applySlopeVelocity(Vec3d horizontalVelocity, RailShape railShape) {
		double gravity = Math.max(SLOPE_GRAVITY_BASE, horizontalVelocity.horizontalLength() * SLOPE_GRAVITY_SPEED_FACTOR);

		if (minecart.isTouchingWater()) {
			gravity *= SLOPE_GRAVITY_WATER_FACTOR;
		}

		return switch (railShape) {
			case ASCENDING_EAST -> horizontalVelocity.add(-gravity, 0.0, 0.0);
			case ASCENDING_WEST -> horizontalVelocity.add(gravity, 0.0, 0.0);
			case ASCENDING_NORTH -> horizontalVelocity.add(0.0, 0.0, gravity);
			case ASCENDING_SOUTH -> horizontalVelocity.add(0.0, 0.0, -gravity);
			default -> horizontalVelocity;
		};
	}

	private Vec3d applyInitialVelocity(Vec3d horizontalVelocity) {
		if (!(minecart.getFirstPassenger() instanceof ServerPlayerEntity serverPlayer)) {
			return horizontalVelocity;
		}

		Vec3d playerInput = serverPlayer.getInputVelocityForMinecart();

		if (playerInput.lengthSquared() <= 0.0) {
			return horizontalVelocity;
		}

		Vec3d normalizedInput = playerInput.normalize();
		double speedSq = horizontalVelocity.horizontalLengthSquared();

		if (normalizedInput.lengthSquared() > 0.0 && speedSq < PLAYER_INPUT_SPEED_THRESHOLD) {
			return horizontalVelocity.add(
					new Vec3d(normalizedInput.x, 0.0, normalizedInput.z).normalize().multiply(PLAYER_INPUT_NUDGE)
			);
		}

		return horizontalVelocity;
	}

	private Vec3d decelerateFromPoweredRail(Vec3d velocity, BlockState railState) {
		if (!railState.isOf(Blocks.POWERED_RAIL) || railState.get(PoweredRailBlock.POWERED)) {
			return velocity;
		}

		return velocity.length() < BRAKE_STOP_THRESHOLD ? Vec3d.ZERO : velocity.multiply(0.5);
	}

	private Vec3d accelerateFromPoweredRail(Vec3d velocity, BlockPos railPos, BlockState railState) {
		if (!railState.isOf(Blocks.POWERED_RAIL) || !railState.get(PoweredRailBlock.POWERED)) {
			return velocity;
		}

		if (velocity.length() > POWERED_RAIL_STOP_THRESHOLD) {
			return velocity.normalize().multiply(velocity.length() + POWERED_RAIL_BOOST);
		}

		Vec3d launchDir = minecart.getLaunchDirection(railPos);
		return launchDir.lengthSquared() <= 0.0
				? velocity
				: launchDir.multiply(velocity.length() + POWERED_RAIL_LAUNCH);
	}

	/**
	 * Перемещает вагонетку вдоль рельса на {@code remainingMovement} блоков.
	 * Учитывает наклонные рельсы и останавливает вагонетку в V-образных впадинах.
	 */
	@Override
	public double moveAlongTrack(BlockPos blockPos, RailShape railShape, double remainingMovement) {
		if (remainingMovement < MIN_VELOCITY_LENGTH) {
			return 0.0;
		}

		Vec3d startPos = getPos();
		Pair<Vec3i, Vec3i> railEnds = AbstractMinecartEntity.getAdjacentRailPositionsByShape(railShape);
		Vec3i endA = railEnds.getFirst();
		Vec3i endB = railEnds.getSecond();
		Vec3d horizontalVelocity = getVelocity().getHorizontal();

		if (horizontalVelocity.length() < MIN_VELOCITY_LENGTH) {
			setVelocity(Vec3d.ZERO);
			return 0.0;
		}

		boolean isSloped = endA.getY() != endB.getY();
		Vec3d dirB = new Vec3d(endB).multiply(0.5).getHorizontal();
		Vec3d dirA = new Vec3d(endA).multiply(0.5).getHorizontal();

		if (horizontalVelocity.dotProduct(dirA) < horizontalVelocity.dotProduct(dirB)) {
			dirA = dirB;
		}

		Vec3d targetPos = blockPos.toBottomCenterPos()
				.add(dirA)
				.add(0.0, RAIL_VERTICAL_OFFSET, 0.0)
				.add(dirA.normalize().multiply(MIN_VELOCITY_LENGTH));

		if (isSloped && !ascends(horizontalVelocity, railShape)) {
			targetPos = targetPos.add(0.0, 1.0, 0.0);
		}

		Vec3d toTarget = targetPos.subtract(getPos()).normalize();
		Vec3d adjustedVelocity = toTarget.multiply(horizontalVelocity.length() / toTarget.horizontalLength());
		Vec3d moveTarget = startPos.add(
				adjustedVelocity.normalize().multiply(
						remainingMovement * (isSloped ? MathHelper.SQUARE_ROOT_OF_TWO : 1.0F)
				)
		);

		if (startPos.squaredDistanceTo(targetPos) <= startPos.squaredDistanceTo(moveTarget)) {
			remainingMovement = targetPos.subtract(moveTarget).horizontalLength();
			moveTarget = targetPos;
		} else {
			remainingMovement = 0.0;
		}

		minecart.move(MovementType.SELF, moveTarget.subtract(startPos));

		BlockState newBlockState = getWorld().getBlockState(BlockPos.ofFloored(moveTarget));

		if (isSloped && AbstractRailBlock.isRail(newBlockState)) {
			RailShape newRailShape = newBlockState.get(
					((AbstractRailBlock) newBlockState.getBlock()).getShapeProperty()
			);

			if (restOnVShapedTrack(railShape, newRailShape)) {
				return 0.0;
			}

			double distToTarget = targetPos.getHorizontal().distanceTo(getPos().getHorizontal());
			double targetY = targetPos.y + (ascends(adjustedVelocity, railShape) ? distToTarget : -distToTarget);

			if (getPos().y < targetY) {
				setPos(getPos().x, targetY, getPos().z);
			}
		}

		if (getPos().distanceTo(startPos) < MIN_VELOCITY_LENGTH
				&& moveTarget.distanceTo(startPos) > MIN_VELOCITY_LENGTH) {
			setVelocity(Vec3d.ZERO);
			return 0.0;
		}

		setVelocity(adjustedVelocity);
		return remainingMovement;
	}

	private boolean restOnVShapedTrack(RailShape currentShape, RailShape newShape) {
		if (getVelocity().lengthSquared() >= MIN_VELOCITY_SQUARED_THRESHOLD) {
			return false;
		}

		if (!newShape.isAscending()) {
			return false;
		}

		if (!ascends(getVelocity(), currentShape)) {
			return false;
		}

		if (ascends(getVelocity(), newShape)) {
			return false;
		}

		setVelocity(Vec3d.ZERO);
		return true;
	}

	@Override
	public double getMaxSpeed(ServerWorld world) {
		double speedFromRules = world.getGameRules().getValue(GameRules.MAX_MINECART_SPEED).intValue();
		double waterFactor = minecart.isTouchingWater() ? 0.5 : 1.0;
		return speedFromRules * waterFactor / 20.0;
	}

	private boolean ascends(Vec3d velocity, RailShape railShape) {
		return switch (railShape) {
			case ASCENDING_EAST -> velocity.x < 0.0;
			case ASCENDING_WEST -> velocity.x > 0.0;
			case ASCENDING_NORTH -> velocity.z > 0.0;
			case ASCENDING_SOUTH -> velocity.z < 0.0;
			default -> false;
		};
	}

	@Override
	public double getSpeedRetention() {
		return minecart.hasPassengers() ? 0.997 : 0.975;
	}

	@Override
	public boolean handleCollision() {
		boolean pickedUp = pickUpEntities(minecart.getBoundingBox().expand(COLLISION_BOX_EXPAND_PICKUP, 0.0, COLLISION_BOX_EXPAND_PICKUP));

		if (!minecart.horizontalCollision && !minecart.verticalCollision) {
			return false;
		}

		boolean pushedAway = pushAwayFromEntities(minecart.getBoundingBox().expand(COLLISION_BOX_EXPAND_PUSH));
		return pickedUp && !pushedAway;
	}

	/**
	 * Пытается посадить ближайшую подходящую сущность в вагонетку.
	 *
	 * @param box область поиска сущностей
	 * @return {@code true} если хотя бы одна сущность была посажена
	 */
	public boolean pickUpEntities(Box box) {
		if (!minecart.isRideable() || minecart.hasPassengers()) {
			return false;
		}

		List<Entity> entities = getWorld().getOtherEntities(
				minecart, box, EntityPredicates.canBePushedBy(minecart)
		);

		for (Entity entity : entities) {
			if (entity instanceof PlayerEntity
					|| entity instanceof IronGolemEntity
					|| entity instanceof AbstractMinecartEntity
					|| minecart.hasPassengers()
					|| entity.hasVehicle()) {
				continue;
			}

			if (entity.startRiding(minecart)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Отталкивает сущности от вагонетки при столкновении.
	 *
	 * @param box область поиска сущностей
	 * @return {@code true} если хотя бы одна сущность была оттолкнута
	 */
	public boolean pushAwayFromEntities(Box box) {
		boolean pushed = false;

		if (minecart.isRideable()) {
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
					pushed = true;
				}
			}
		} else {
			for (Entity entity : getWorld().getOtherEntities(minecart, box)) {
				if (!minecart.hasPassenger(entity)
						&& entity.isPushable()
						&& entity instanceof AbstractMinecartEntity) {
					entity.pushAwayFrom(minecart);
					pushed = true;
				}
			}
		}

		return pushed;
	}

	/** Хранит один шаг интерполяции: позицию, скорость, углы и вес для клиентской анимации. */
	record InterpolatedStep(
			float partialTicksInStep,
			Step currentStep,
			Step previousStep
	) {
	}

	/** Состояние итерации субтикового движения по рельсам. */
	static class MoveIteration {

		double remainingMovement = 0.0;
		boolean initial = true;
		boolean slopeVelocityApplied = false;
		boolean decelerated = false;
		boolean accelerated = false;

		/** Продолжать итерацию, пока это первый шаг или осталось расстояние для движения. */
		public boolean shouldContinue() {
			return initial || remainingMovement > MIN_VELOCITY_LENGTH;
		}
	}

	/**
	 * Один шаг движения вагонетки для клиентской интерполяции.
	 * Передаётся от сервера к клиенту через пакет и воспроизводится плавно.
	 */
	public record Step(Vec3d position, Vec3d movement, float yRot, float xRot, float weight) {

		public static final PacketCodec<ByteBuf, Step> PACKET_CODEC = PacketCodec.tuple(
				Vec3d.PACKET_CODEC, Step::position,
				Vec3d.PACKET_CODEC, Step::movement,
				PacketCodecs.DEGREES, Step::yRot,
				PacketCodecs.DEGREES, Step::xRot,
				PacketCodecs.FLOAT, Step::weight,
				Step::new
		);

		public static final Step ZERO = new Step(Vec3d.ZERO, Vec3d.ZERO, 0.0F, 0.0F, 0.0F);
	}
}
