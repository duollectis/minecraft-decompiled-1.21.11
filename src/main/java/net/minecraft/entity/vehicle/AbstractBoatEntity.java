package net.minecraft.entity.vehicle;

import com.google.common.collect.Lists;
import net.minecraft.block.BlockState;
import net.minecraft.block.LilyPadBlock;
import net.minecraft.entity.*;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.BoatPaddleStateC2SPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockLocating;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Базовый класс для всех лодок и плотов.
 * Реализует физику движения по воде, пузырьковым столбам, суше и воздуху,
 * управление вёслами, посадку/высадку пассажиров и привязь.
 */
public abstract class AbstractBoatEntity extends VehicleEntity implements Leashable {

	private static final TrackedData<Boolean> LEFT_PADDLE_MOVING =
			DataTracker.registerData(AbstractBoatEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean> RIGHT_PADDLE_MOVING =
			DataTracker.registerData(AbstractBoatEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Integer> BUBBLE_WOBBLE_TICKS =
			DataTracker.registerData(AbstractBoatEntity.class, TrackedDataHandlerRegistry.INTEGER);

	public static final int LEFT_PADDLE_INDEX = 0;
	public static final int RIGHT_PADDLE_INDEX = 1;

	private static final int BUBBLE_WOBBLE_TICKS_MAX = 60;
	private static final int UNDERWATER_TICKS_MAX = 60;
	private static final float NEXT_PADDLE_PHASE = (float) (Math.PI / 8);
	public static final double EMIT_SOUND_EVENT_PADDLE_ROTATION = (float) (Math.PI / 4);

	/** Смещение пассажира вперёд при двух пассажирах (первый). */
	private static final float PASSENGER_FRONT_OFFSET = 0.2F;
	/** Смещение пассажира назад при двух пассажирах (второй). */
	private static final float PASSENGER_BACK_OFFSET = -0.6F;
	/** Дополнительное смещение для животных-пассажиров. */
	private static final float ANIMAL_PASSENGER_EXTRA_OFFSET = 0.2F;

	private static final float DAMAGE_WOBBLE_ANIMATE_MULTIPLIER = 11.0F;
	private static final int DAMAGE_WOBBLE_ANIMATE_TICKS = 10;

	private static final float PADDLE_TURN_SPEED = 0.005F;
	private static final float PADDLE_FORWARD_SPEED = 0.04F;
	private static final float PADDLE_BACK_SPEED = 0.005F;

	private static final double GRAVITY_IN_WATER = 0.04;
	private static final double GRAVITY_UNDER_FLOWING_WATER = -7.0E-4;
	private static final double BUOYANCY_UNDER_WATER = 0.01F;
	private static final float DRAG_IN_WATER = 0.9F;
	private static final float DRAG_UNDER_WATER = 0.45F;
	private static final float DRAG_IN_AIR = 0.9F;
	private static final float DRAG_BASE = 0.05F;
	private static final double BUOYANCY_SCALE = 0.65;
	private static final double BUOYANCY_DAMPING = 0.75;

	private static final double BUBBLE_COLUMN_DRAG_VELOCITY = -0.7;
	private static final double BUBBLE_COLUMN_LAUNCH_VELOCITY_PLAYER = 2.7;
	private static final double BUBBLE_COLUMN_LAUNCH_VELOCITY_NO_PLAYER = 0.6;
	private static final float BUBBLE_WOBBLE_STRENGTH_INCREASE = 0.05F;
	private static final float BUBBLE_WOBBLE_STRENGTH_DECREASE = 0.1F;
	private static final float BUBBLE_WOBBLE_AMPLITUDE = 10.0F;
	private static final double BUBBLE_WOBBLE_FREQUENCY = 0.5;

	private static final float PASSENGER_YAW_CLAMP = 105.0F;
	private static final int ANIMAL_PASSENGER_YAW_EVEN = 90;
	private static final int ANIMAL_PASSENGER_YAW_ODD = 270;

	private static final double LEASH_HEIGHT_FACTOR = 0.88;
	private static final double LEASH_WIDTH_FACTOR = 0.64;
	private static final double QUAD_LEASH_HEIGHT = 0.88;
	private static final double QUAD_LEASH_WIDTH = 0.382;

	private static final double ENTITY_PUSH_EXPAND_H = 0.2F;
	private static final double ENTITY_PUSH_EXPAND_V = -0.01F;

	private static final float SPLASH_SOUND_VOLUME = 1.0F;
	private static final float SPLASH_SOUND_PITCH_BASE = 0.8F;
	private static final float SPLASH_SOUND_PITCH_RANGE = 0.4F;
	private static final double SPLASH_PARTICLE_Y_OFFSET = 0.7;
	private static final int SPLASH_SOUND_CHANCE = 100;

	private final float[] paddlePhases = new float[2];
	private float ticksUnderwater;
	private float yawVelocity;
	private final PositionInterpolator interpolator = new PositionInterpolator(this, 3);
	private boolean pressingLeft;
	private boolean pressingRight;
	private boolean pressingForward;
	private boolean pressingBack;
	private double waterLevel;
	private float nearbySlipperiness;
	private Location location;
	private Location lastLocation;
	private double fallVelocity;
	private boolean onBubbleColumnSurface;
	private boolean bubbleColumnIsDrag;
	private float bubbleWobbleStrength;
	private float bubbleWobble;
	private float lastBubbleWobble;
	private Leashable.@Nullable LeashData leashData;
	private final Supplier<Item> itemSupplier;

	public AbstractBoatEntity(EntityType<? extends AbstractBoatEntity> type, World world, Supplier<Item> itemSupplier) {
		super(type, world);
		this.itemSupplier = itemSupplier;
		intersectionChecked = true;
	}

	/**
	 * Инициализирует позицию лодки и сохраняет её как «прошлую» позицию,
	 * чтобы избежать ложного расчёта скорости при первом тике.
	 */
	public void initPosition(double x, double y, double z) {
		setPosition(x, y, z);
		lastX = x;
		lastY = y;
		lastZ = z;
	}

	@Override
	protected Entity.MoveEffect getMoveEffect() {
		return Entity.MoveEffect.EVENTS;
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(LEFT_PADDLE_MOVING, false);
		builder.add(RIGHT_PADDLE_MOVING, false);
		builder.add(BUBBLE_WOBBLE_TICKS, 0);
	}

	@Override
	public boolean collidesWith(Entity other) {
		return canCollide(this, other);
	}

	/**
	 * Определяет, могут ли две сущности сталкиваться.
	 * Лодки не сталкиваются с сущностями, связанными через транспортное средство.
	 */
	public static boolean canCollide(Entity entity, Entity other) {
		return (other.isCollidable(entity) || other.isPushable()) && !entity.isConnectedThroughVehicle(other);
	}

	@Override
	public boolean isCollidable(@Nullable Entity entity) {
		return true;
	}

	@Override
	public boolean isPushable() {
		return true;
	}

	@Override
	public Vec3d positionInPortal(Direction.Axis portalAxis, BlockLocating.Rectangle portalRect) {
		return LivingEntity.positionInPortal(super.positionInPortal(portalAxis, portalRect));
	}

	/** Возвращает вертикальное смещение точки крепления пассажира относительно центра лодки. */
	protected abstract double getPassengerAttachmentY(EntityDimensions dimensions);

	/**
	 * Вычисляет позицию крепления пассажира с учётом количества пассажиров и типа сущности.
	 * При двух пассажирах первый смещается вперёд, второй — назад.
	 */
	@Override
	protected Vec3d getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
		float horizontalOffset = getPassengerHorizontalOffset();

		if (getPassengerList().size() > 1) {
			int passengerIndex = getPassengerList().indexOf(passenger);
			horizontalOffset = passengerIndex == 0 ? PASSENGER_FRONT_OFFSET : PASSENGER_BACK_OFFSET;

			if (passenger instanceof AnimalEntity) {
				horizontalOffset += ANIMAL_PASSENGER_EXTRA_OFFSET;
			}
		}

		return new Vec3d(0.0, getPassengerAttachmentY(dimensions), horizontalOffset)
				.rotateY(-getYaw() * (float) (Math.PI / 180.0));
	}

	@Override
	public void onBubbleColumnSurfaceCollision(boolean drag, BlockPos pos) {
		if (getEntityWorld() instanceof ServerWorld) {
			onBubbleColumnSurface = true;
			bubbleColumnIsDrag = drag;

			if (getBubbleWobbleTicks() == 0) {
				setBubbleWobbleTicks(BUBBLE_WOBBLE_TICKS_MAX);
			}
		}

		if (!isSubmergedInWater() && random.nextInt(SPLASH_SOUND_CHANCE) == 0) {
			getEntityWorld().playSoundClient(
					getX(), getY(), getZ(),
					getSplashSound(), getSoundCategory(),
					SPLASH_SOUND_VOLUME,
					SPLASH_SOUND_PITCH_BASE + SPLASH_SOUND_PITCH_RANGE * random.nextFloat(),
					false
			);
			getEntityWorld().addParticleClient(
					ParticleTypes.SPLASH,
					getX() + random.nextFloat(),
					getY() + SPLASH_PARTICLE_Y_OFFSET,
					getZ() + random.nextFloat(),
					0.0, 0.0, 0.0
			);
			emitGameEvent(GameEvent.SPLASH, getControllingPassenger());
		}
	}

	@Override
	public void pushAwayFrom(Entity entity) {
		if (entity instanceof AbstractBoatEntity) {
			if (entity.getBoundingBox().minY < getBoundingBox().maxY) {
				super.pushAwayFrom(entity);
			}
		} else if (entity.getBoundingBox().minY <= getBoundingBox().minY) {
			super.pushAwayFrom(entity);
		}
	}

	@Override
	public void animateDamage(float yaw) {
		setDamageWobbleSide(-getDamageWobbleSide());
		setDamageWobbleTicks(DAMAGE_WOBBLE_ANIMATE_TICKS);
		setDamageWobbleStrength(getDamageWobbleStrength() * DAMAGE_WOBBLE_ANIMATE_MULTIPLIER);
	}

	@Override
	public boolean canHit() {
		return !isRemoved();
	}

	@Override
	public PositionInterpolator getInterpolator() {
		return interpolator;
	}

	@Override
	public Direction getMovementDirection() {
		return getHorizontalFacing().rotateYClockwise();
	}

	/**
	 * Основной тик лодки: обновляет местоположение, физику, вёсла и взаимодействие с сущностями.
	 * Пассажиры выбрасываются при нахождении под водой дольше {@code UNDERWATER_TICKS_MAX} тиков.
	 */
	@Override
	public void tick() {
		lastLocation = location;
		location = checkLocation();

		if (location != Location.UNDER_WATER && location != Location.UNDER_FLOWING_WATER) {
			ticksUnderwater = 0.0F;
		} else {
			ticksUnderwater++;
		}

		if (!getEntityWorld().isClient() && ticksUnderwater >= UNDERWATER_TICKS_MAX) {
			removeAllPassengers();
		}

		if (getDamageWobbleTicks() > 0) {
			setDamageWobbleTicks(getDamageWobbleTicks() - 1);
		}

		if (getDamageWobbleStrength() > 0.0F) {
			setDamageWobbleStrength(getDamageWobbleStrength() - 1.0F);
		}

		super.tick();
		interpolator.tick();

		if (isLogicalSideForUpdatingMovement()) {
			if (!(getFirstPassenger() instanceof PlayerEntity)) {
				setPaddlesMoving(false, false);
			}

			updateVelocity();

			if (getEntityWorld().isClient()) {
				updatePaddles();
				getEntityWorld().sendPacket(
						new BoatPaddleStateC2SPacket(isPaddleMoving(0), isPaddleMoving(1))
				);
			}

			move(MovementType.SELF, getVelocity());
		} else {
			setVelocity(Vec3d.ZERO);
		}

		tickBlockCollision();
		tickBlockCollision();
		handleBubbleColumn();
		tickPaddleSounds();
		pushNearbyEntities();
	}

	private void tickPaddleSounds() {
		for (int paddleIndex = 0; paddleIndex <= 1; paddleIndex++) {
			if (isPaddleMoving(paddleIndex)) {
				float phase = paddlePhases[paddleIndex];
				boolean crossedSoundThreshold = phase % (float) (Math.PI * 2) <= (float) EMIT_SOUND_EVENT_PADDLE_ROTATION
						&& (phase + NEXT_PADDLE_PHASE) % (float) (Math.PI * 2) >= (float) EMIT_SOUND_EVENT_PADDLE_ROTATION;

				if (!isSilent() && crossedSoundThreshold) {
					SoundEvent sound = getPaddleSound();

					if (sound != null) {
						Vec3d rotVec = getRotationVec(1.0F);
						double offsetZ = paddleIndex == RIGHT_PADDLE_INDEX ? -rotVec.z : rotVec.z;
						double offsetX = paddleIndex == RIGHT_PADDLE_INDEX ? rotVec.x : -rotVec.x;
						getEntityWorld().playSound(
								null,
								getX() + offsetX, getY(), getZ() + offsetZ,
								sound, getSoundCategory(),
								1.0F,
								SPLASH_SOUND_PITCH_BASE + SPLASH_SOUND_PITCH_RANGE * random.nextFloat()
						);
					}
				}

				paddlePhases[paddleIndex] += NEXT_PADDLE_PHASE;
			} else {
				paddlePhases[paddleIndex] = 0.0F;
			}
		}
	}

	private void pushNearbyEntities() {
		List<Entity> nearby = getEntityWorld().getOtherEntities(
				this,
				getBoundingBox().expand(ENTITY_PUSH_EXPAND_H, ENTITY_PUSH_EXPAND_V, ENTITY_PUSH_EXPAND_H),
				EntityPredicates.canBePushedBy(this)
		);

		if (nearby.isEmpty()) {
			return;
		}

		boolean canPickUp = !getEntityWorld().isClient()
				&& !(getControllingPassenger() instanceof PlayerEntity);

		for (Entity entity : nearby) {
			if (entity.hasPassenger(this)) {
				continue;
			}

			if (canPickUp
					&& getPassengerList().size() < getMaxPassengers()
					&& !entity.hasVehicle()
					&& isSmallerThanBoat(entity)
					&& entity instanceof LivingEntity
					&& !entity.getType().isIn(EntityTypeTags.CANNOT_BE_PUSHED_ONTO_BOATS)) {
				entity.startRiding(this);
			} else {
				pushAwayFrom(entity);
			}
		}
	}

	private void handleBubbleColumn() {
		if (getEntityWorld().isClient()) {
			int wobbleTicks = getBubbleWobbleTicks();
			bubbleWobbleStrength = wobbleTicks > 0
					? Math.min(bubbleWobbleStrength + BUBBLE_WOBBLE_STRENGTH_INCREASE, 1.0F)
					: Math.max(bubbleWobbleStrength - BUBBLE_WOBBLE_STRENGTH_DECREASE, 0.0F);

			lastBubbleWobble = bubbleWobble;
			bubbleWobble = BUBBLE_WOBBLE_AMPLITUDE * (float) Math.sin(BUBBLE_WOBBLE_FREQUENCY * age) * bubbleWobbleStrength;
			return;
		}

		if (!onBubbleColumnSurface) {
			setBubbleWobbleTicks(0);
		}

		int ticks = getBubbleWobbleTicks();

		if (ticks <= 0) {
			return;
		}

		setBubbleWobbleTicks(--ticks);
		int elapsed = BUBBLE_WOBBLE_TICKS_MAX - ticks - 1;

		if (elapsed > 0 && ticks == 0) {
			setBubbleWobbleTicks(0);
			Vec3d velocity = getVelocity();

			if (bubbleColumnIsDrag) {
				setVelocity(velocity.add(0.0, BUBBLE_COLUMN_DRAG_VELOCITY, 0.0));
				removeAllPassengers();
			} else {
				double launchY = hasPassenger(passenger -> passenger instanceof PlayerEntity)
						? BUBBLE_COLUMN_LAUNCH_VELOCITY_PLAYER
						: BUBBLE_COLUMN_LAUNCH_VELOCITY_NO_PLAYER;
				setVelocity(velocity.x, launchY, velocity.z);
			}
		}

		onBubbleColumnSurface = false;
	}

	protected @Nullable SoundEvent getPaddleSound() {
		return switch (checkLocation()) {
			case IN_WATER, UNDER_WATER, UNDER_FLOWING_WATER -> SoundEvents.ENTITY_BOAT_PADDLE_WATER;
			case ON_LAND -> SoundEvents.ENTITY_BOAT_PADDLE_LAND;
			default -> null;
		};
	}

	public void setPaddlesMoving(boolean left, boolean right) {
		dataTracker.set(LEFT_PADDLE_MOVING, left);
		dataTracker.set(RIGHT_PADDLE_MOVING, right);
	}

	/**
	 * Возвращает интерполированную фазу вёсла для плавной анимации на клиенте.
	 *
	 * @param paddle индекс вёсла (0 — левое, 1 — правое)
	 * @param tickProgress прогресс текущего тика (0.0–1.0)
	 */
	public float lerpPaddlePhase(int paddle, float tickProgress) {
		return isPaddleMoving(paddle)
				? MathHelper.clampedLerp(
						tickProgress,
						paddlePhases[paddle] - NEXT_PADDLE_PHASE,
						paddlePhases[paddle]
				)
				: 0.0F;
	}

	@Override
	public Leashable.@Nullable LeashData getLeashData() {
		return leashData;
	}

	@Override
	public void setLeashData(Leashable.@Nullable LeashData leashData) {
		this.leashData = leashData;
	}

	@Override
	public Vec3d getLeashOffset() {
		return new Vec3d(0.0, LEASH_HEIGHT_FACTOR * getHeight(), LEASH_WIDTH_FACTOR * getWidth());
	}

	@Override
	public boolean canUseQuadLeashAttachmentPoint() {
		return true;
	}

	@Override
	public Vec3d[] getQuadLeashOffsets() {
		return Leashable.createQuadLeashOffsets(this, 0.0, QUAD_LEASH_WIDTH, QUAD_LEASH_WIDTH, QUAD_LEASH_HEIGHT);
	}

	/**
	 * Определяет текущее местоположение лодки (вода, суша, воздух, под водой).
	 * Обновляет {@code waterLevel} и {@code nearbySlipperiness} как побочный эффект.
	 */
	private Location checkLocation() {
		Location underWater = getUnderWaterLocation();

		if (underWater != null) {
			waterLevel = getBoundingBox().maxY;
			return underWater;
		}

		if (checkBoatInWater()) {
			return Location.IN_WATER;
		}

		float slipperiness = getNearbySlipperiness();

		if (slipperiness > 0.0F) {
			nearbySlipperiness = slipperiness;
			return Location.ON_LAND;
		}

		return Location.IN_AIR;
	}

	/**
	 * Вычисляет высоту воды под лодкой для корректного позиционирования при приводнении.
	 */
	public float getWaterHeightBelow() {
		Box box = getBoundingBox();
		int minX = MathHelper.floor(box.minX);
		int maxX = MathHelper.ceil(box.maxX);
		int minY = MathHelper.floor(box.maxY);
		int maxY = MathHelper.ceil(box.maxY - fallVelocity);
		int minZ = MathHelper.floor(box.minZ);
		int maxZ = MathHelper.ceil(box.maxZ);
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		outer:
		for (int y = minY; y < maxY; y++) {
			float maxFluidHeight = 0.0F;

			for (int x = minX; x < maxX; x++) {
				for (int z = minZ; z < maxZ; z++) {
					mutable.set(x, y, z);
					FluidState fluidState = getEntityWorld().getFluidState(mutable);

					if (fluidState.isIn(FluidTags.WATER)) {
						maxFluidHeight = Math.max(maxFluidHeight, fluidState.getHeight(getEntityWorld(), mutable));
					}

					if (maxFluidHeight >= 1.0F) {
						continue outer;
					}
				}
			}

			if (maxFluidHeight < 1.0F) {
				return mutable.getY() + maxFluidHeight;
			}
		}

		return maxY + 1;
	}

	/**
	 * Вычисляет среднюю скользкость блоков под лодкой для симуляции движения по суше.
	 */
	public float getNearbySlipperiness() {
		Box box = getBoundingBox();
		Box groundBox = new Box(box.minX, box.minY - 0.001, box.minZ, box.maxX, box.minY, box.maxZ);
		int minX = MathHelper.floor(groundBox.minX) - 1;
		int maxX = MathHelper.ceil(groundBox.maxX) + 1;
		int minY = MathHelper.floor(groundBox.minY) - 1;
		int maxY = MathHelper.ceil(groundBox.maxY) + 1;
		int minZ = MathHelper.floor(groundBox.minZ) - 1;
		int maxZ = MathHelper.ceil(groundBox.maxZ) + 1;
		VoxelShape groundShape = VoxelShapes.cuboid(groundBox);
		float totalSlipperiness = 0.0F;
		int count = 0;
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (int x = minX; x < maxX; x++) {
			for (int z = minZ; z < maxZ; z++) {
				int edgeCount = (x != minX && x != maxX - 1 ? 0 : 1) + (z != minZ && z != maxZ - 1 ? 0 : 1);

				if (edgeCount == 2) {
					continue;
				}

				for (int y = minY; y < maxY; y++) {
					if (edgeCount > 0 && (y == minY || y == maxY - 1)) {
						continue;
					}

					mutable.set(x, y, z);
					BlockState blockState = getEntityWorld().getBlockState(mutable);

					if (blockState.getBlock() instanceof LilyPadBlock) {
						continue;
					}

					if (VoxelShapes.matchesAnywhere(
							blockState.getCollisionShape(getEntityWorld(), mutable).offset(mutable),
							groundShape,
							BooleanBiFunction.AND
					)) {
						totalSlipperiness += blockState.getBlock().getSlipperiness();
						count++;
					}
				}
			}
		}

		return totalSlipperiness / count;
	}

	private boolean checkBoatInWater() {
		Box box = getBoundingBox();
		int minX = MathHelper.floor(box.minX);
		int maxX = MathHelper.ceil(box.maxX);
		int minY = MathHelper.floor(box.minY);
		int maxY = MathHelper.ceil(box.minY + 0.001);
		int minZ = MathHelper.floor(box.minZ);
		int maxZ = MathHelper.ceil(box.maxZ);
		boolean inWater = false;
		waterLevel = -Double.MAX_VALUE;
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (int x = minX; x < maxX; x++) {
			for (int y = minY; y < maxY; y++) {
				for (int z = minZ; z < maxZ; z++) {
					mutable.set(x, y, z);
					FluidState fluidState = getEntityWorld().getFluidState(mutable);

					if (fluidState.isIn(FluidTags.WATER)) {
						float fluidTop = y + fluidState.getHeight(getEntityWorld(), mutable);
						waterLevel = Math.max((double) fluidTop, waterLevel);
						inWater |= box.minY < fluidTop;
					}
				}
			}
		}

		return inWater;
	}

	private @Nullable Location getUnderWaterLocation() {
		Box box = getBoundingBox();
		double topY = box.maxY + 0.001;
		int minX = MathHelper.floor(box.minX);
		int maxX = MathHelper.ceil(box.maxX);
		int minY = MathHelper.floor(box.maxY);
		int maxY = MathHelper.ceil(topY);
		int minZ = MathHelper.floor(box.minZ);
		int maxZ = MathHelper.ceil(box.maxZ);
		boolean stillWater = false;
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (int x = minX; x < maxX; x++) {
			for (int y = minY; y < maxY; y++) {
				for (int z = minZ; z < maxZ; z++) {
					mutable.set(x, y, z);
					FluidState fluidState = getEntityWorld().getFluidState(mutable);

					if (!fluidState.isIn(FluidTags.WATER)) {
						continue;
					}

					if (topY < mutable.getY() + fluidState.getHeight(getEntityWorld(), mutable)) {
						if (!fluidState.isStill()) {
							return Location.UNDER_FLOWING_WATER;
						}

						stillWater = true;
					}
				}
			}
		}

		return stillWater ? Location.UNDER_WATER : null;
	}

	@Override
	protected double getGravity() {
		return GRAVITY_IN_WATER;
	}

	/**
	 * Обновляет скорость лодки в зависимости от текущего местоположения.
	 * Реализует плавучесть, торможение и гравитацию для каждого типа среды.
	 */
	private void updateVelocity() {
		double gravityY = -getFinalGravity();
		double buoyancy = 0.0;
		float drag = DRAG_BASE;

		if (lastLocation == Location.IN_AIR
				&& location != Location.IN_AIR
				&& location != Location.ON_LAND) {
			waterLevel = getBodyY(1.0);
			double targetY = getWaterHeightBelow() - getHeight() + 0.101;

			if (getEntityWorld().isSpaceEmpty(this, getBoundingBox().offset(0.0, targetY - getY(), 0.0))) {
				setPosition(getX(), targetY, getZ());
				setVelocity(getVelocity().multiply(1.0, 0.0, 1.0));
				fallVelocity = 0.0;
			}

			location = Location.IN_WATER;
		} else {
			if (location == Location.IN_WATER) {
				buoyancy = (waterLevel - getY()) / getHeight();
				drag = DRAG_IN_WATER;
			} else if (location == Location.UNDER_FLOWING_WATER) {
				gravityY = GRAVITY_UNDER_FLOWING_WATER;
				drag = DRAG_IN_WATER;
			} else if (location == Location.UNDER_WATER) {
				buoyancy = BUOYANCY_UNDER_WATER;
				drag = DRAG_UNDER_WATER;
			} else if (location == Location.IN_AIR) {
				drag = DRAG_IN_AIR;
			} else if (location == Location.ON_LAND) {
				drag = nearbySlipperiness;

				if (getControllingPassenger() instanceof PlayerEntity) {
					nearbySlipperiness /= 2.0F;
				}
			}

			Vec3d velocity = getVelocity();
			setVelocity(velocity.x * drag, velocity.y + gravityY, velocity.z * drag);
			yawVelocity *= drag;

			if (buoyancy > 0.0) {
				Vec3d boosted = getVelocity();
				setVelocity(
						boosted.x,
						(boosted.y + buoyancy * (getGravity() / BUOYANCY_SCALE)) * BUOYANCY_DAMPING,
						boosted.z
				);
			}
		}
	}

	private void updatePaddles() {
		if (!hasPassengers()) {
			return;
		}

		float forwardForce = 0.0F;

		if (pressingLeft) {
			yawVelocity--;
		}

		if (pressingRight) {
			yawVelocity++;
		}

		if (pressingRight != pressingLeft && !pressingForward && !pressingBack) {
			forwardForce += PADDLE_TURN_SPEED;
		}

		setYaw(getYaw() + yawVelocity);

		if (pressingForward) {
			forwardForce += PADDLE_FORWARD_SPEED;
		}

		if (pressingBack) {
			forwardForce -= PADDLE_BACK_SPEED;
		}

		setVelocity(getVelocity().add(
				MathHelper.sin(-getYaw() * (float) (Math.PI / 180.0)) * forwardForce,
				0.0,
				MathHelper.cos(getYaw() * (float) (Math.PI / 180.0)) * forwardForce
		));
		setPaddlesMoving(
				pressingRight && !pressingLeft || pressingForward,
				pressingLeft && !pressingRight || pressingForward
		);
	}

	protected float getPassengerHorizontalOffset() {
		return 0.0F;
	}

	public boolean isSmallerThanBoat(Entity entity) {
		return entity.getWidth() < getWidth();
	}

	/**
		* Обновляет позицию пассажира и поворачивает его вместе с лодкой.
		* Животные при полной загрузке разворачиваются боком для визуального разнообразия.
		*/
	@Override
	protected void updatePassengerPosition(Entity passenger, Entity.PositionUpdater positionUpdater) {
		super.updatePassengerPosition(passenger, positionUpdater);

		if (passenger.getType().isIn(EntityTypeTags.CAN_TURN_IN_BOATS)) {
			return;
		}

		passenger.setYaw(passenger.getYaw() + yawVelocity);
		passenger.setHeadYaw(passenger.getHeadYaw() + yawVelocity);
		clampPassengerYaw(passenger);

		if (passenger instanceof AnimalEntity animal
				&& getPassengerList().size() == getMaxPassengers()) {
			int bodyYawOffset = animal.getId() % 2 == 0 ? ANIMAL_PASSENGER_YAW_EVEN : ANIMAL_PASSENGER_YAW_ODD;
			passenger.setBodyYaw(animal.bodyYaw + bodyYawOffset);
			passenger.setHeadYaw(passenger.getHeadYaw() + bodyYawOffset);
		}
	}

	/**
		* Вычисляет безопасную позицию для высадки пассажира из лодки.
		* Проверяет блоки рядом с лодкой и возвращает первую подходящую позицию.
		*/
	@Override
	public Vec3d updatePassengerForDismount(LivingEntity passenger) {
		Vec3d dismountOffset = getPassengerDismountOffset(
				getWidth() * MathHelper.SQUARE_ROOT_OF_TWO,
				passenger.getWidth(),
				passenger.getYaw()
		);
		double targetX = getX() + dismountOffset.x;
		double targetZ = getZ() + dismountOffset.z;
		BlockPos abovePos = BlockPos.ofFloored(targetX, getBoundingBox().maxY, targetZ);
		BlockPos belowPos = abovePos.down();

		if (getEntityWorld().isWater(belowPos)) {
			return super.updatePassengerForDismount(passenger);
		}

		List<Vec3d> candidates = Lists.newArrayList();
		double aboveHeight = getEntityWorld().getDismountHeight(abovePos);

		if (Dismounting.canDismountInBlock(aboveHeight)) {
			candidates.add(new Vec3d(targetX, abovePos.getY() + aboveHeight, targetZ));
		}

		double belowHeight = getEntityWorld().getDismountHeight(belowPos);

		if (Dismounting.canDismountInBlock(belowHeight)) {
			candidates.add(new Vec3d(targetX, belowPos.getY() + belowHeight, targetZ));
		}

		for (EntityPose pose : passenger.getPoses()) {
			for (Vec3d candidate : candidates) {
				if (Dismounting.canPlaceEntityAt(getEntityWorld(), candidate, passenger, pose)) {
					passenger.setPose(pose);
					return candidate;
				}
			}
		}

		return super.updatePassengerForDismount(passenger);
	}

	/**
		* Ограничивает угол поворота пассажира относительно лодки в пределах ±105°.
		*/
	protected void clampPassengerYaw(Entity passenger) {
		passenger.setBodyYaw(getYaw());
		float yawDiff = MathHelper.wrapDegrees(passenger.getYaw() - getYaw());
		float clampedDiff = MathHelper.clamp(yawDiff, -PASSENGER_YAW_CLAMP, PASSENGER_YAW_CLAMP);
		passenger.lastYaw += clampedDiff - yawDiff;
		passenger.setYaw(passenger.getYaw() + clampedDiff - yawDiff);
		passenger.setHeadYaw(passenger.getYaw());
	}

	@Override
	public void onPassengerLookAround(Entity passenger) {
		clampPassengerYaw(passenger);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		writeLeashData(view, leashData);
	}

	@Override
	protected void readCustomData(ReadView view) {
		readLeashData(view);
	}

	/**
		* Обрабатывает взаимодействие игрока с лодкой: посадка возможна только если лодка не под водой.
		*/
	@Override
	public ActionResult interact(PlayerEntity player, Hand hand) {
		ActionResult result = super.interact(player, hand);

		if (result != ActionResult.PASS) {
			return result;
		}

		if (player.shouldCancelInteraction()) {
			return ActionResult.PASS;
		}

		if (ticksUnderwater >= UNDERWATER_TICKS_MAX) {
			return ActionResult.PASS;
		}

		if (!getEntityWorld().isClient() && !player.startRiding(this)) {
			return ActionResult.PASS;
		}

		return ActionResult.SUCCESS;
	}

	@Override
	public void remove(Entity.RemovalReason reason) {
		if (!getEntityWorld().isClient() && reason.shouldDestroy() && isLeashed()) {
			detachLeash();
		}

		super.remove(reason);
	}

	@Override
	protected void fall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition) {
		fallVelocity = getVelocity().y;

		if (hasVehicle()) {
			return;
		}

		if (onGround) {
			onLanding();
		} else if (!getEntityWorld().getFluidState(getBlockPos().down()).isIn(FluidTags.WATER)
				&& heightDifference < 0.0) {
			fallDistance -= (float) heightDifference;
		}
	}

	/**
		* Проверяет, движется ли указанное весло.
		* Весло считается движущимся только при наличии управляющего пассажира.
		*
		* @param paddle индекс вёсла (0 — левое, 1 — правое)
		*/
	public boolean isPaddleMoving(int paddle) {
		return dataTracker.get(paddle == LEFT_PADDLE_INDEX ? LEFT_PADDLE_MOVING : RIGHT_PADDLE_MOVING)
				&& getControllingPassenger() != null;
	}

	private void setBubbleWobbleTicks(int ticks) {
		dataTracker.set(BUBBLE_WOBBLE_TICKS, ticks);
	}

	private int getBubbleWobbleTicks() {
		return dataTracker.get(BUBBLE_WOBBLE_TICKS);
	}

	/**
		* Возвращает интерполированное значение тряски от пузырькового столба для анимации.
		*
		* @param tickProgress прогресс текущего тика (0.0–1.0)
		*/
	public float lerpBubbleWobble(float tickProgress) {
		return MathHelper.lerp(tickProgress, lastBubbleWobble, bubbleWobble);
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return getPassengerList().size() < getMaxPassengers() && !isSubmergedIn(FluidTags.WATER);
	}

	protected int getMaxPassengers() {
		return 2;
	}

	@Override
	public @Nullable LivingEntity getControllingPassenger() {
		return getFirstPassenger() instanceof LivingEntity living
				? living
				: super.getControllingPassenger();
	}

	public void setInputs(boolean pressingLeft, boolean pressingRight, boolean pressingForward, boolean pressingBack) {
		this.pressingLeft = pressingLeft;
		this.pressingRight = pressingRight;
		this.pressingForward = pressingForward;
		this.pressingBack = pressingBack;
	}

	@Override
	public boolean isSubmergedInWater() {
		return location == Location.UNDER_WATER || location == Location.UNDER_FLOWING_WATER;
	}

	@Override
	protected final Item asItem() {
		return itemSupplier.get();
	}

	@Override
	public final ItemStack getPickBlockStack() {
		return new ItemStack(itemSupplier.get());
	}

	/** Перечисление возможных местоположений лодки относительно воды и суши. */
	public enum Location {
		IN_WATER,
		UNDER_WATER,
		UNDER_FLOWING_WATER,
		ON_LAND,
		IN_AIR
	}
}
