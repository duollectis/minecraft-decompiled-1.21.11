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

@Environment(EnvType.CLIENT)
/**
 * {@code Camera}.
 */
public class Camera implements TrackedWaypoint.YawProvider {

	private static final float BASE_CAMERA_DISTANCE = 4.0F;
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
	private final EnvironmentAttributeInterpolator
			environmentAttributeInterpolator =
			new EnvironmentAttributeInterpolator();

	public void update(World area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickProgress) {
		this.ready = true;
		this.area = area;
		this.focusedEntity = focusedEntity;
		this.thirdPerson = thirdPerson;
		this.lastTickProgress = tickProgress;
		if (focusedEntity.hasVehicle()
				&& focusedEntity.getVehicle() instanceof MinecartEntity minecartEntity
				&& minecartEntity.getController() instanceof ExperimentalMinecartController experimentalMinecartController
				&& experimentalMinecartController.hasCurrentLerpSteps()) {
			Vec3d vec3d = minecartEntity.getPassengerRidingPos(focusedEntity)
			                            .subtract(minecartEntity.getEntityPos())
			                            .subtract(focusedEntity.getVehicleAttachmentPos(minecartEntity))
			                            .add(new Vec3d(
					                            0.0,
					                            MathHelper.lerp(tickProgress, this.lastCameraY, this.cameraY),
					                            0.0
			                            ));
			this.setRotation(focusedEntity.getYaw(tickProgress), focusedEntity.getPitch(tickProgress));
			this.setPos(experimentalMinecartController.getLerpedPosition(tickProgress).add(vec3d));
		}
		else {
			this.setRotation(focusedEntity.getYaw(tickProgress), focusedEntity.getPitch(tickProgress));
			this.setPos(
					MathHelper.lerp((double) tickProgress, focusedEntity.lastX, focusedEntity.getX()),
					MathHelper.lerp((double) tickProgress, focusedEntity.lastY, focusedEntity.getY()) + MathHelper.lerp(
							tickProgress,
							this.lastCameraY,
							this.cameraY
					),
					MathHelper.lerp((double) tickProgress, focusedEntity.lastZ, focusedEntity.getZ())
			);
		}

		if (thirdPerson) {
			if (inverseView) {
				this.setRotation(this.yaw + 180.0F, -this.pitch);
			}

			float f = 4.0F;
			float g = 1.0F;
			if (focusedEntity instanceof LivingEntity livingEntity) {
				g = livingEntity.getScale();
				f = (float) livingEntity.getAttributeValue(EntityAttributes.CAMERA_DISTANCE);
			}

			float h = g;
			float i = f;
			if (focusedEntity.hasVehicle() && focusedEntity.getVehicle() instanceof LivingEntity livingEntity2) {
				h = livingEntity2.getScale();
				i = (float) livingEntity2.getAttributeValue(EntityAttributes.CAMERA_DISTANCE);
			}

			this.moveBy(-this.clipToSpace(Math.max(g * f, h * i)), 0.0F, 0.0F);
		}
		else if (focusedEntity instanceof LivingEntity && ((LivingEntity) focusedEntity).isSleeping()) {
			Direction direction = ((LivingEntity) focusedEntity).getSleepingDirection();
			this.setRotation(direction != null ? direction.getPositiveHorizontalDegrees() - 180.0F : 0.0F, 0.0F);
			this.moveBy(0.0F, 0.3F, 0.0F);
		}
	}

	public void updateEyeHeight() {
		if (this.focusedEntity != null) {
			this.lastCameraY = this.cameraY;
			this.cameraY = this.cameraY + (this.focusedEntity.getStandingEyeHeight() - this.cameraY) * 0.5F;
			this.environmentAttributeInterpolator.update(this.area, this.pos);
		}
	}

	private float clipToSpace(float distance) {
		float f = 0.1F;

		for (int i = 0; i < 8; i++) {
			float g = (i & 1) * 2 - 1;
			float h = (i >> 1 & 1) * 2 - 1;
			float j = (i >> 2 & 1) * 2 - 1;
			Vec3d vec3d = this.pos.add(g * 0.1F, h * 0.1F, j * 0.1F);
			Vec3d vec3d2 = vec3d.add(new Vec3d(this.horizontalPlane).multiply(-distance));
			HitResult hitResult = this.area
					.raycast(new RaycastContext(
							vec3d,
							vec3d2,
							RaycastContext.ShapeType.VISUAL,
							RaycastContext.FluidHandling.NONE,
							this.focusedEntity
					));
			if (hitResult.getType() != HitResult.Type.MISS) {
				float k = (float) hitResult.getPos().squaredDistanceTo(this.pos);
				if (k < MathHelper.square(distance)) {
					distance = MathHelper.sqrt(k);
				}
			}
		}

		return distance;
	}

	protected void moveBy(float surge, float heave, float sway) {
		Vector3f vector3f = new Vector3f(sway, heave, -surge).rotate(this.rotation);
		this.setPos(new Vec3d(this.pos.x + vector3f.x, this.pos.y + vector3f.y, this.pos.z + vector3f.z));
	}

	protected void setRotation(float yaw, float pitch) {
		this.pitch = pitch;
		this.yaw = yaw;
		this.rotation.rotationYXZ(
				(float) Math.PI - yaw * (float) (Math.PI / 180.0),
				-pitch * (float) (Math.PI / 180.0),
				0.0F
		);
		HORIZONTAL.rotate(this.rotation, this.horizontalPlane);
		VERTICAL.rotate(this.rotation, this.verticalPlane);
		DIAGONAL.rotate(this.rotation, this.diagonalPlane);
	}

	protected void setPos(double x, double y, double z) {
		this.setPos(new Vec3d(x, y, z));
	}

	protected void setPos(Vec3d pos) {
		this.pos = pos;
		this.blockPos.set(pos.x, pos.y, pos.z);
	}

	@Override
	public Vec3d getCameraPos() {
		return this.pos;
	}

	public BlockPos getBlockPos() {
		return this.blockPos;
	}

	public float getPitch() {
		return this.pitch;
	}

	public float getYaw() {
		return this.yaw;
	}

	@Override
	public float getCameraYaw() {
		return MathHelper.wrapDegrees(this.getYaw());
	}

	public Quaternionf getRotation() {
		return this.rotation;
	}

	public Entity getFocusedEntity() {
		return this.focusedEntity;
	}

	public boolean isReady() {
		return this.ready;
	}

	public boolean isThirdPerson() {
		return this.thirdPerson;
	}

	public EnvironmentAttributeInterpolator getEnvironmentAttributeInterpolator() {
		return this.environmentAttributeInterpolator;
	}

	public Camera.Projection getProjection() {
		MinecraftClient minecraftClient = MinecraftClient.getInstance();
		double
				d =
				(double) minecraftClient.getWindow().getFramebufferWidth() / minecraftClient
						.getWindow()
						.getFramebufferHeight();
		double
				e =
				Math.tan(minecraftClient.options.getFov().getValue().intValue() * (float) (Math.PI / 180.0) / 2.0)
						* 0.05F;
		double f = e * d;
		Vec3d vec3d = new Vec3d(this.horizontalPlane).multiply(0.05F);
		Vec3d vec3d2 = new Vec3d(this.diagonalPlane).multiply(f);
		Vec3d vec3d3 = new Vec3d(this.verticalPlane).multiply(e);
		return new Camera.Projection(vec3d, vec3d2, vec3d3);
	}

	public CameraSubmersionType getSubmersionType() {
		if (!this.ready) {
			return CameraSubmersionType.NONE;
		}
		else {
			FluidState fluidState = this.area.getFluidState(this.blockPos);
			if (fluidState.isIn(FluidTags.WATER) && this.pos.y < this.blockPos.getY() + fluidState.getHeight(
					this.area,
					this.blockPos
			)) {
				return CameraSubmersionType.WATER;
			}
			else {
				Camera.Projection projection = this.getProjection();

				for (Vec3d vec3d : Arrays.asList(
						projection.center,
						projection.getBottomRight(),
						projection.getTopRight(),
						projection.getBottomLeft(),
						projection.getTopLeft()
				)) {
					Vec3d vec3d2 = this.pos.add(vec3d);
					BlockPos blockPos = BlockPos.ofFloored(vec3d2);
					FluidState fluidState2 = this.area.getFluidState(blockPos);
					if (fluidState2.isIn(FluidTags.LAVA)) {
						if (vec3d2.y <= fluidState2.getHeight(this.area, blockPos) + blockPos.getY()) {
							return CameraSubmersionType.LAVA;
						}
					}
					else {
						BlockState blockState = this.area.getBlockState(blockPos);
						if (blockState.isOf(Blocks.POWDER_SNOW)) {
							return CameraSubmersionType.POWDER_SNOW;
						}
					}
				}

				return CameraSubmersionType.NONE;
			}
		}
	}

	public Vector3fc getHorizontalPlane() {
		return this.horizontalPlane;
	}

	public Vector3fc getVerticalPlane() {
		return this.verticalPlane;
	}

	public Vector3fc getDiagonalPlane() {
		return this.diagonalPlane;
	}

	public void reset() {
		this.area = null;
		this.focusedEntity = null;
		this.environmentAttributeInterpolator.clear();
		this.ready = false;
	}

	public float getLastTickProgress() {
		return this.lastTickProgress;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Projection}.
	 */
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
			return this.center.add(this.y).add(this.x);
		}

		public Vec3d getTopRight() {
			return this.center.add(this.y).subtract(this.x);
		}

		public Vec3d getBottomLeft() {
			return this.center.subtract(this.y).add(this.x);
		}

		public Vec3d getTopLeft() {
			return this.center.subtract(this.y).subtract(this.x);
		}

		public Vec3d getPosition(float factorX, float factorY) {
			return this.center.add(this.y.multiply(factorY)).subtract(this.x.multiply(factorX));
		}
	}
}
