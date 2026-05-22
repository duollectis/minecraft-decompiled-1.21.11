package net.minecraft.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.vehicle.ExperimentalMinecartController;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.attribute.EnvironmentAttributeInterpolator;
import net.minecraft.world.waypoint.TrackedWaypoint;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Arrays;

/**
 * Управляет позицией и ориентацией игровой камеры в мировом пространстве.
 * Поддерживает режимы от первого и третьего лица, обрабатывает столкновения камеры
 * с геометрией мира через рейкаст, а также определяет тип погружения (вода, лава, снег).
 * Реализует {@link TrackedWaypoint.YawProvider} для интеграции с системой путевых точек.
 */
@Environment(EnvType.CLIENT)
public class Camera implements TrackedWaypoint.YawProvider {

	private static final float BASE_CAMERA_DISTANCE = 4.0F;
	/** Смещение угловых точек проекции при рейкасте столкновений камеры. */
	private static final float CLIP_CORNER_OFFSET = 0.1F;
	/** Смещение камеры вверх при режиме сна сущности. */
	private static final float SLEEP_VERTICAL_OFFSET = 0.3F;
	private static final Vector3f HORIZONTAL = new Vector3f(0.0F, 0.0F, -1.0F);
	private static final Vector3f VERTICAL = new Vector3f(0.0F, 1.0F, 0.0F);
	private static final Vector3f DIAGONAL = new Vector3f(-1.0F, 0.0F, 0.0F);

	private boolean ready;
	private World area;
	private Entity focusedEntity;
	private Vec3d pos = Vec3d.ZERO;
	private final BlockPos.Mutable blockPos = new BlockPos.Mutable();
	private final Vector3f horizontalPlane = new Vector3f(HORIZONTAL);
	private final Vector3f verticalPlane = new Vector3f(VERTICAL);
	private final Vector3f diagonalPlane = new Vector3f(DIAGONAL);
	private float pitch;
	private float yaw;
	private final Quaternionf rotation = new Quaternionf();
	private boolean thirdPerson;
	private float cameraY;
	private float lastCameraY;
	private float lastTickProgress;
	private final EnvironmentAttributeInterpolator environmentAttributeInterpolator = new EnvironmentAttributeInterpolator();

	/**
	 * Обновляет позицию и ориентацию камеры для текущего кадра.
	 * В режиме третьего лица отодвигает камеру назад с учётом масштаба сущности
	 * и атрибута {@link EntityAttributes#CAMERA_DISTANCE}, обрезая дистанцию по геометрии мира.
	 *
	 * @param area          мир, в котором находится сущность
	 * @param focusedEntity сущность, за которой следит камера
	 * @param thirdPerson   {@code true} — режим третьего лица
	 * @param inverseView   {@code true} — инвертированный вид (камера смотрит спереди)
	 * @param tickProgress  интерполяционный прогресс между тиками [0..1]
	 */
	public void update(World area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickProgress) {
		ready = true;
		this.area = area;
		this.focusedEntity = focusedEntity;
		this.thirdPerson = thirdPerson;
		lastTickProgress = tickProgress;

		if (focusedEntity.hasVehicle()
				&& focusedEntity.getVehicle() instanceof MinecartEntity minecartEntity
				&& minecartEntity.getController() instanceof ExperimentalMinecartController experimentalMinecartController
				&& experimentalMinecartController.hasCurrentLerpSteps()
		) {
			Vec3d passengerOffset = minecartEntity.getPassengerRidingPos(focusedEntity)
					.subtract(minecartEntity.getEntityPos())
					.subtract(focusedEntity.getVehicleAttachmentPos(minecartEntity))
					.add(new Vec3d(0.0, MathHelper.lerp(tickProgress, lastCameraY, cameraY), 0.0));
			setRotation(focusedEntity.getYaw(tickProgress), focusedEntity.getPitch(tickProgress));
			setPos(experimentalMinecartController.getLerpedPosition(tickProgress).add(passengerOffset));
		}
		else {
			setRotation(focusedEntity.getYaw(tickProgress), focusedEntity.getPitch(tickProgress));
			setPos(
					MathHelper.lerp((double) tickProgress, focusedEntity.lastX, focusedEntity.getX()),
					MathHelper.lerp((double) tickProgress, focusedEntity.lastY, focusedEntity.getY())
							+ MathHelper.lerp(tickProgress, lastCameraY, cameraY),
					MathHelper.lerp((double) tickProgress, focusedEntity.lastZ, focusedEntity.getZ())
			);
		}

		if (thirdPerson) {
			if (inverseView) {
				setRotation(yaw + 180.0F, -pitch);
			}

			float entityScale = 1.0F;
			float cameraDistance = BASE_CAMERA_DISTANCE;
			if (focusedEntity instanceof LivingEntity livingEntity) {
				entityScale = livingEntity.getScale();
				cameraDistance = (float) livingEntity.getAttributeValue(EntityAttributes.CAMERA_DISTANCE);
			}

			float vehicleScale = entityScale;
			float vehicleDistance = cameraDistance;
			if (focusedEntity.hasVehicle() && focusedEntity.getVehicle() instanceof LivingEntity vehicleEntity) {
				vehicleScale = vehicleEntity.getScale();
				vehicleDistance = (float) vehicleEntity.getAttributeValue(EntityAttributes.CAMERA_DISTANCE);
			}

			moveBy(-clipToSpace(Math.max(entityScale * cameraDistance, vehicleScale * vehicleDistance)), 0.0F, 0.0F);
		}
		else if (focusedEntity instanceof LivingEntity livingEntity && livingEntity.isSleeping()) {
			Direction sleepDirection = livingEntity.getSleepingDirection();
			setRotation(sleepDirection != null ? sleepDirection.getPositiveHorizontalDegrees() - 180.0F : 0.0F, 0.0F);
			moveBy(0.0F, SLEEP_VERTICAL_OFFSET, 0.0F);
		}
	}

	/**
	 * Плавно интерполирует высоту глаз камеры к текущей высоте глаз сущности.
	 * Вызывается каждый тик для сглаживания перехода при приседании/вставании.
	 */
	public void updateEyeHeight() {
		if (focusedEntity == null) {
			return;
		}

		lastCameraY = cameraY;
		cameraY = cameraY + (focusedEntity.getStandingEyeHeight() - cameraY) * 0.5F;
		environmentAttributeInterpolator.update(area, pos);
	}

	/**
	 * Обрезает дистанцию камеры до ближайшего препятствия в мире.
	 * Проверяет 8 угловых точек вокруг позиции камеры, чтобы избежать клиппинга через стены.
	 *
	 * @param distance желаемая дистанция отдаления камеры
	 * @return фактическая дистанция, не превышающая расстояние до ближайшего блока
	 */
	private float clipToSpace(float distance) {
		for (int cornerIndex = 0; cornerIndex < 8; cornerIndex++) {
			float offsetX = ((cornerIndex & 1) * 2 - 1) * CLIP_CORNER_OFFSET;
			float offsetY = ((cornerIndex >> 1 & 1) * 2 - 1) * CLIP_CORNER_OFFSET;
			float offsetZ = ((cornerIndex >> 2 & 1) * 2 - 1) * CLIP_CORNER_OFFSET;
			Vec3d rayStart = pos.add(offsetX, offsetY, offsetZ);
			Vec3d rayEnd = rayStart.add(new Vec3d(horizontalPlane).multiply(-distance));
			HitResult hitResult = area.raycast(new RaycastContext(
					rayStart,
					rayEnd,
					RaycastContext.ShapeType.VISUAL,
					RaycastContext.FluidHandling.NONE,
					focusedEntity
			));

			if (hitResult.getType() != HitResult.Type.MISS) {
				float squaredDist = (float) hitResult.getPos().squaredDistanceTo(pos);
				if (squaredDist < MathHelper.square(distance)) {
					distance = MathHelper.sqrt(squaredDist);
				}
			}
		}

		return distance;
	}

	protected void moveBy(float surge, float heave, float sway) {
		Vector3f offset = new Vector3f(sway, heave, -surge).rotate(rotation);
		setPos(new Vec3d(pos.x + offset.x, pos.y + offset.y, pos.z + offset.z));
	}

	protected void setRotation(float yaw, float pitch) {
		this.pitch = pitch;
		this.yaw = yaw;
		rotation.rotationYXZ(
				(float) Math.PI - yaw * (float) (Math.PI / 180.0),
				-pitch * (float) (Math.PI / 180.0),
				0.0F
		);
		HORIZONTAL.rotate(rotation, horizontalPlane);
		VERTICAL.rotate(rotation, verticalPlane);
		DIAGONAL.rotate(rotation, diagonalPlane);
	}

	protected void setPos(double x, double y, double z) {
		setPos(new Vec3d(x, y, z));
	}

	protected void setPos(Vec3d pos) {
		this.pos = pos;
		blockPos.set(pos.x, pos.y, pos.z);
	}

	@Override
	public Vec3d getCameraPos() {
		return pos;
	}

	public BlockPos getBlockPos() {
		return blockPos;
	}

	public float getPitch() {
		return pitch;
	}

	public float getYaw() {
		return yaw;
	}

	@Override
	public float getCameraYaw() {
		return MathHelper.wrapDegrees(getYaw());
	}

	public Quaternionf getRotation() {
		return rotation;
	}

	public Entity getFocusedEntity() {
		return focusedEntity;
	}

	public boolean isReady() {
		return ready;
	}

	public boolean isThirdPerson() {
		return thirdPerson;
	}

	public EnvironmentAttributeInterpolator getEnvironmentAttributeInterpolator() {
		return environmentAttributeInterpolator;
	}

	/**
	 * Вычисляет проекцию ближней плоскости камеры в мировых координатах.
	 * Используется для определения типа погружения камеры (вода, лава, снег).
	 *
	 * @return объект {@link Projection} с центром и осями ближней плоскости
	 */
	public Camera.Projection getProjection() {
		MinecraftClient client = MinecraftClient.getInstance();
		double aspectRatio = (double) client.getWindow().getFramebufferWidth() / client.getWindow().getFramebufferHeight();
		double halfFovTan = Math.tan(client.options.getFov().getValue().intValue() * (float) (Math.PI / 180.0) / 2.0) * 0.05F;
		double halfFovTanX = halfFovTan * aspectRatio;
		Vec3d center = new Vec3d(horizontalPlane).multiply(0.05F);
		Vec3d axisX = new Vec3d(diagonalPlane).multiply(halfFovTanX);
		Vec3d axisY = new Vec3d(verticalPlane).multiply(halfFovTan);
		return new Camera.Projection(center, axisX, axisY);
	}

	/**
	 * Определяет тип среды, в которую погружена камера (вода, лава, порошковый снег).
	 * Проверяет центр и четыре угла ближней плоскости проекции.
	 *
	 * @return тип погружения камеры
	 */
	public CameraSubmersionType getSubmersionType() {
		if (!ready) {
			return CameraSubmersionType.NONE;
		}

		FluidState fluidState = area.getFluidState(blockPos);
		if (fluidState.isIn(FluidTags.WATER)
				&& pos.y < blockPos.getY() + fluidState.getHeight(area, blockPos)
		) {
			return CameraSubmersionType.WATER;
		}

		Camera.Projection projection = getProjection();

		for (Vec3d corner : Arrays.asList(
				projection.center,
				projection.getBottomRight(),
				projection.getTopRight(),
				projection.getBottomLeft(),
				projection.getTopLeft()
		)) {
			Vec3d checkPos = pos.add(corner);
			BlockPos checkBlock = BlockPos.ofFloored(checkPos);
			FluidState lavaState = area.getFluidState(checkBlock);

			if (lavaState.isIn(FluidTags.LAVA)) {
				if (checkPos.y <= lavaState.getHeight(area, checkBlock) + checkBlock.getY()) {
					return CameraSubmersionType.LAVA;
				}
			}
			else {
				BlockState blockState = area.getBlockState(checkBlock);
				if (blockState.isOf(Blocks.POWDER_SNOW)) {
					return CameraSubmersionType.POWDER_SNOW;
				}
			}
		}

		return CameraSubmersionType.NONE;
	}

	public Vector3fc getHorizontalPlane() {
		return horizontalPlane;
	}

	public Vector3fc getVerticalPlane() {
		return verticalPlane;
	}

	public Vector3fc getDiagonalPlane() {
		return diagonalPlane;
	}

	public void reset() {
		area = null;
		focusedEntity = null;
		environmentAttributeInterpolator.clear();
		ready = false;
	}

	public float getLastTickProgress() {
		return lastTickProgress;
	}

	/**
	 * Описывает ближнюю плоскость проекции камеры в мировых координатах.
	 * Используется для проверки погружения камеры в жидкости и блоки.
	 */
	@Environment(EnvType.CLIENT)
	public static class Projection {

		final Vec3d center;
		private final Vec3d x;
		private final Vec3d y;

		Projection(Vec3d center, Vec3d x, Vec3d y) {
			this.center = center;
			this.x = x;
			this.y = y;
		}

		public Vec3d getBottomRight() {
			return center.add(y).add(x);
		}

		public Vec3d getTopRight() {
			return center.add(y).subtract(x);
		}

		public Vec3d getBottomLeft() {
			return center.subtract(y).add(x);
		}

		public Vec3d getTopLeft() {
			return center.subtract(y).subtract(x);
		}

		public Vec3d getPosition(float factorX, float factorY) {
			return center.add(y.multiply(factorY)).subtract(x.multiply(factorX));
		}
	}
}
