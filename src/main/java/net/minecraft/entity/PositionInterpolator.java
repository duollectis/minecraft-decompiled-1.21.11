package net.minecraft.entity;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Управляет плавной интерполяцией позиции и углов поворота сущности на клиенте.
 * Получает целевую позицию с сервера и за {@code lerpDuration} тиков плавно
 * перемещает сущность к ней, учитывая смещение от движения самой сущности.
 */
public class PositionInterpolator {

	public static final int DEFAULT_INTERPOLATION_DURATION = 3;
	private final Entity entity;
	private int lerpDuration;
	private final Data data = new Data(0, Vec3d.ZERO, 0.0F, 0.0F);
	private @Nullable Vec3d lastPos;
	private @Nullable Vec2f lastRotation;
	private final @Nullable Consumer<PositionInterpolator> callback;

	public PositionInterpolator(Entity entity) {
		this(entity, DEFAULT_INTERPOLATION_DURATION, null);
	}

	public PositionInterpolator(Entity entity, int lerpDuration) {
		this(entity, lerpDuration, null);
	}

	public PositionInterpolator(Entity entity, @Nullable Consumer<PositionInterpolator> callback) {
		this(entity, DEFAULT_INTERPOLATION_DURATION, callback);
	}

	public PositionInterpolator(Entity entity, int lerpDuration, @Nullable Consumer<PositionInterpolator> callback) {
		this.lerpDuration = lerpDuration;
		this.entity = entity;
		this.callback = callback;
	}

	public Vec3d getLerpedPos() {
		return data.step > 0 ? data.pos : entity.getEntityPos();
	}

	public float getLerpedYaw() {
		return data.step > 0 ? data.yaw : entity.getYaw();
	}

	public float getLerpedPitch() {
		return data.step > 0 ? data.pitch : entity.getPitch();
	}

	/**
	 * Устанавливает новую целевую позицию и углы для интерполяции.
	 * Если интерполяция уже идёт к тем же значениям — вызов игнорируется.
	 * При нулевой длительности интерполяции позиция применяется мгновенно.
	 */
	public void refreshPositionAndAngles(Vec3d targetPos, float yaw, float pitch) {
		if (lerpDuration == 0) {
			entity.refreshPositionAndAngles(targetPos, yaw, pitch);
			clear();
			return;
		}

		if (isInterpolating()
				&& Objects.equals(getLerpedYaw(), yaw)
				&& Objects.equals(getLerpedPitch(), pitch)
				&& Objects.equals(getLerpedPos(), targetPos)) {
			return;
		}

		data.step = lerpDuration;
		data.pos = targetPos;
		data.yaw = yaw;
		data.pitch = pitch;
		lastPos = entity.getEntityPos();
		lastRotation = new Vec2f(entity.getPitch(), entity.getYaw());

		if (callback != null) {
			callback.accept(this);
		}
	}

	public boolean isInterpolating() {
		return data.step > 0;
	}

	public void setLerpDuration(int lerpDuration) {
		this.lerpDuration = lerpDuration;
	}

	/**
	 * Выполняет один шаг интерполяции: сдвигает сущность на 1/{@code step} расстояния
	 * к целевой позиции, учитывая смещение от собственного движения сущности.
	 */
	public void tick() {
		if (!isInterpolating()) {
			clear();
			return;
		}

		double lerpFactor = 1.0 / data.step;

		if (lastPos != null) {
			Vec3d entityMovement = entity.getEntityPos().subtract(lastPos);
			if (entity.getEntityWorld().isSpaceEmpty(
					entity,
					entity.calculateDefaultBoundingBox(data.pos.add(entityMovement))
			)) {
				data.addPos(entityMovement);
			}
		}

		if (lastRotation != null) {
			float yawDelta = entity.getYaw() - lastRotation.y;
			float pitchDelta = entity.getPitch() - lastRotation.x;
			data.addRotation(yawDelta, pitchDelta);
		}

		double lerpedX = MathHelper.lerp(lerpFactor, entity.getX(), data.pos.x);
		double lerpedY = MathHelper.lerp(lerpFactor, entity.getY(), data.pos.y);
		double lerpedZ = MathHelper.lerp(lerpFactor, entity.getZ(), data.pos.z);
		Vec3d lerpedPos = new Vec3d(lerpedX, lerpedY, lerpedZ);
		float lerpedYaw = (float) MathHelper.lerpAngleDegrees(lerpFactor, entity.getYaw(), data.yaw);
		float lerpedPitch = (float) MathHelper.lerp(lerpFactor, entity.getPitch(), data.pitch);

		entity.setPosition(lerpedPos);
		entity.setRotation(lerpedYaw, lerpedPitch);
		data.tick();
		lastPos = lerpedPos;
		lastRotation = new Vec2f(entity.getPitch(), entity.getYaw());
	}

	public void clear() {
		data.step = 0;
		lastPos = null;
		lastRotation = null;
	}

	/**
	 * Внутреннее состояние интерполяции: целевая позиция, углы и оставшееся число шагов.
	 */
	static class Data {

		protected int step;
		Vec3d pos;
		float yaw;
		float pitch;

		Data(int step, Vec3d pos, float yaw, float pitch) {
			this.step = step;
			this.pos = pos;
			this.yaw = yaw;
			this.pitch = pitch;
		}

		public void tick() {
			step--;
		}

		public void addPos(Vec3d delta) {
			pos = pos.add(delta);
		}

		public void addRotation(float yawDelta, float pitchDelta) {
			yaw += yawDelta;
			pitch += pitchDelta;
		}
	}
}
