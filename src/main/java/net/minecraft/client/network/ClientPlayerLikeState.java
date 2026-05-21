package net.minecraft.client.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Хранит и интерполирует состояние движения для сущностей, похожих на игрока.
 * <p>Используется {@link ClientMannequinEntity} и другими клиентскими сущностями
 * для плавной интерполяции позиции, скорости и анимации движения между тиками.
 * Позиция интерполируется с коэффициентом 0.25 для плавности, но при больших
 * скачках (> 10 единиц) телепортируется мгновенно.
 */
@Environment(EnvType.CLIENT)
public class ClientPlayerLikeState {

	private static final double TELEPORT_THRESHOLD = 10.0;
	private static final float MOVEMENT_LERP_FACTOR = 0.4F;

	private Vec3d velocity = Vec3d.ZERO;
	private float distanceMoved;
	private float lastDistanceMoved;
	private double x;
	private double y;
	private double z;
	private double lastX;
	private double lastY;
	private double lastZ;
	private float movement;
	private float lastMovement;

	/**
	 * Обновляет состояние за один тик: сохраняет скорость и интерполирует позицию.
	 *
	 * @param pos      текущая позиция сущности
	 * @param velocity текущая скорость сущности
	 */
	public void tick(Vec3d pos, Vec3d velocity) {
		lastDistanceMoved = distanceMoved;
		this.velocity = velocity;
		setPos(pos);
	}

	/**
	 * Добавляет пройденное расстояние к накопленному счётчику.
	 *
	 * @param delta приращение пройденного расстояния
	 */
	public void addDistanceMoved(float delta) {
		distanceMoved += delta;
	}

	/**
	 * Возвращает текущую скорость сущности.
	 *
	 * @return вектор скорости
	 */
	public Vec3d getVelocity() {
		return velocity;
	}

	/**
	 * Возвращает интерполированную X-координату.
	 *
	 * @param tickProgress прогресс текущего тика [0.0, 1.0]
	 * @return интерполированная X-координата
	 */
	public double lerpX(float tickProgress) {
		return MathHelper.lerp((double) tickProgress, lastX, x);
	}

	/**
	 * Возвращает интерполированную Y-координату.
	 *
	 * @param tickProgress прогресс текущего тика [0.0, 1.0]
	 * @return интерполированная Y-координата
	 */
	public double lerpY(float tickProgress) {
		return MathHelper.lerp((double) tickProgress, lastY, y);
	}

	/**
	 * Возвращает интерполированную Z-координату.
	 *
	 * @param tickProgress прогресс текущего тика [0.0, 1.0]
	 * @return интерполированная Z-координата
	 */
	public double lerpZ(float tickProgress) {
		return MathHelper.lerp((double) tickProgress, lastZ, z);
	}

	/**
	 * Обновляет значение движения с плавным переходом.
	 *
	 * @param movement новое значение движения
	 */
	public void tickMovement(float movement) {
		lastMovement = this.movement;
		this.movement = this.movement + (movement - this.movement) * MOVEMENT_LERP_FACTOR;
	}

	/**
	 * Сбрасывает движение до нуля (используется при езде верхом).
	 */
	public void tickRiding() {
		lastMovement = movement;
		movement = 0.0F;
	}

	/**
	 * Возвращает интерполированное значение движения.
	 *
	 * @param tickProgress прогресс текущего тика [0.0, 1.0]
	 * @return интерполированное движение
	 */
	public float lerpMovement(float tickProgress) {
		return MathHelper.lerp(tickProgress, lastMovement, movement);
	}

	/**
	 * Возвращает обратно интерполированное пройденное расстояние (для анимации ног).
	 *
	 * @param tickProgress прогресс текущего тика [0.0, 1.0]
	 * @return отрицательное интерполированное расстояние
	 */
	public float getReverseLerpedDistanceMoved(float tickProgress) {
		float delta = distanceMoved - lastDistanceMoved;
		return -(distanceMoved + delta * tickProgress);
	}

	/**
	 * Возвращает интерполированное пройденное расстояние.
	 *
	 * @param tickProgress прогресс текущего тика [0.0, 1.0]
	 * @return интерполированное расстояние
	 */
	public float getLerpedDistanceMoved(float tickProgress) {
		return MathHelper.lerp(tickProgress, lastDistanceMoved, distanceMoved);
	}

	private void setPos(Vec3d pos) {
		lastX = x;
		lastY = y;
		lastZ = z;

		double dx = pos.getX() - x;
		double dy = pos.getY() - y;
		double dz = pos.getZ() - z;

		if (dx > TELEPORT_THRESHOLD || dx < -TELEPORT_THRESHOLD) {
			x = pos.getX();
			lastX = x;
		}
		else {
			x += dx * 0.25;
		}

		if (dy > TELEPORT_THRESHOLD || dy < -TELEPORT_THRESHOLD) {
			y = pos.getY();
			lastY = y;
		}
		else {
			y += dy * 0.25;
		}

		if (dz > TELEPORT_THRESHOLD || dz < -TELEPORT_THRESHOLD) {
			z = pos.getZ();
			lastZ = z;
		}
		else {
			z += dz * 0.25;
		}
	}
}
