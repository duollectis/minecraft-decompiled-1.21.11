package net.minecraft.world.waypoint;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.network.packet.s2c.play.WaypointS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

/**
 * Серверная сторона вейпоинта — управляет отслеживанием и отправкой пакетов игрокам.
 * <p>
 * Каждый {@link ServerWaypoint} может создавать {@link WaypointTracker} для конкретного
 * игрока-получателя. Трекер определяет, когда вейпоинт становится недействительным
 * (источник вышел из зоны видимости, сменил позицию и т.д.) и отправляет обновления.
 */
public interface ServerWaypoint extends Waypoint {

	/**
	 * Минимальное расстояние (в блоках), при котором используется азимутальный режим
	 * вместо точной позиции. При большом расстоянии точная позиция не нужна.
	 */
	int AZIMUTH_THRESHOLD = 332;

	boolean hasWaypoint();

	Optional<WaypointTracker> createTracker(ServerPlayerEntity receiver);

	Waypoint.Config getWaypointConfig();

	/**
	 * Проверяет, не может ли игрок-получатель принять вейпоинт от источника.
	 * <p>
	 * Получение невозможно, если:
	 * <ul>
	 *   <li>источник — спектатор (не имеет физического присутствия);</li>
	 *   <li>получатель едет на источнике (вейпоинт бессмысленен);</li>
	 *   <li>расстояние между ними превышает минимум из дальностей передачи и приёма.</li>
	 * </ul>
	 * Спектатор-получатель всегда может принимать вейпоинты.
	 */
	static boolean cannotReceive(LivingEntity source, ServerPlayerEntity receiver) {
		if (receiver.isSpectator()) {
			return false;
		}

		if (source.isSpectator() || source.hasPassengerDeep(receiver)) {
			return true;
		}

		double maxRange = Math.min(
			source.getAttributeValue(EntityAttributes.WAYPOINT_TRANSMIT_RANGE),
			receiver.getAttributeValue(EntityAttributes.WAYPOINT_RECEIVE_RANGE)
		);

		return source.distanceTo(receiver) >= maxRange;
	}

	static boolean canReceive(ChunkPos source, ServerPlayerEntity receiver) {
		return receiver.getChunkFilter().isWithinDistanceExcludingEdge(source.x, source.z);
	}

	static boolean shouldUseAzimuth(LivingEntity source, ServerPlayerEntity receiver) {
		return source.distanceTo(receiver) > AZIMUTH_THRESHOLD;
	}

	/**
	 * Трекер вейпоинта на основе азимута — используется, когда источник далеко
	 * и точная позиция не нужна. Отправляет только угол направления.
	 */
	class AzimuthWaypointTracker implements WaypointTracker {

		private final LivingEntity source;
		private final Waypoint.Config config;
		private final ServerPlayerEntity receiver;
		private float azimuth;

		public AzimuthWaypointTracker(LivingEntity source, Waypoint.Config config, ServerPlayerEntity receiver) {
			this.source = source;
			this.config = config;
			this.receiver = receiver;
			Vec3d direction = receiver.getEntityPos().subtract(source.getEntityPos()).rotateYClockwise();
			azimuth = (float) MathHelper.atan2(direction.getZ(), direction.getX());
		}

		@Override
		public boolean isInvalid() {
			return cannotReceive(source, receiver)
				|| canReceive(source.getChunkPos(), receiver)
				|| !shouldUseAzimuth(source, receiver);
		}

		@Override
		public void track() {
			receiver.networkHandler.sendPacket(WaypointS2CPacket.trackAzimuth(source.getUuid(), config, azimuth));
		}

		@Override
		public void untrack() {
			receiver.networkHandler.sendPacket(WaypointS2CPacket.untrack(source.getUuid()));
		}

		@Override
		public void update() {
			Vec3d direction = receiver.getEntityPos().subtract(source.getEntityPos()).rotateYClockwise();
			float newAzimuth = (float) MathHelper.atan2(direction.getZ(), direction.getX());

			if (MathHelper.abs(newAzimuth - azimuth) > 0.008726646F) {
				receiver.networkHandler.sendPacket(WaypointS2CPacket.updateAzimuth(source.getUuid(), config, newAzimuth));
				azimuth = newAzimuth;
			}
		}
	}

	/**
	 * Трекер, инвалидирующийся при смещении источника более чем на 1 чанк
	 * по расстоянию Чебышёва от исходной позиции.
	 */
	interface ChebyshevDistanceValidatedTracker extends WaypointTracker {

		int getDistanceToOriginalPos();

		@Override
		default boolean isInvalid() {
			return getDistanceToOriginalPos() > 1;
		}
	}

	/**
	 * Трекер вейпоинта на основе позиции чанка — используется на средних дистанциях,
	 * когда точная позиция блока не нужна, но азимут уже недостаточен.
	 */
	class ChunkWaypointTracker implements ChebyshevDistanceValidatedTracker {

		private final LivingEntity source;
		private final Waypoint.Config config;
		private final ServerPlayerEntity receiver;
		private ChunkPos chunkPos;

		public ChunkWaypointTracker(LivingEntity source, Waypoint.Config config, ServerPlayerEntity receiver) {
			this.source = source;
			this.config = config;
			this.receiver = receiver;
			chunkPos = source.getChunkPos();
		}

		@Override
		public int getDistanceToOriginalPos() {
			return chunkPos.getChebyshevDistance(source.getChunkPos());
		}

		@Override
		public void track() {
			receiver.networkHandler.sendPacket(WaypointS2CPacket.trackChunk(source.getUuid(), config, chunkPos));
		}

		@Override
		public void untrack() {
			receiver.networkHandler.sendPacket(WaypointS2CPacket.untrack(source.getUuid()));
		}

		@Override
		public void update() {
			ChunkPos currentChunk = source.getChunkPos();

			if (currentChunk.getChebyshevDistance(chunkPos) > 0) {
				receiver.networkHandler.sendPacket(WaypointS2CPacket.updateChunk(source.getUuid(), config, currentChunk));
				chunkPos = currentChunk;
			}
		}

		/**
		 * Трекер недействителен, если источник сместился, получатель не может принять
		 * вейпоинт, или чанк источника попал в зону прямой видимости получателя
		 * (в этом случае нужно переключиться на позиционный трекер).
		 */
		@Override
		public boolean isInvalid() {
			if (ChebyshevDistanceValidatedTracker.super.isInvalid() || cannotReceive(source, receiver)) {
				return true;
			}

			return canReceive(chunkPos, receiver);
		}
	}

	/**
	 * Трекер, инвалидирующийся при смещении источника более чем на 1 блок
	 * по манхэттенскому расстоянию от исходной позиции.
	 */
	interface ManhattanDistanceValidatedTracker extends WaypointTracker {

		int getDistanceToOriginalPos();

		@Override
		default boolean isInvalid() {
			return getDistanceToOriginalPos() > 1;
		}
	}

	/**
	 * Трекер вейпоинта на основе точной позиции блока — используется вблизи,
	 * когда источник находится в зоне прямой видимости получателя.
	 */
	class PositionalWaypointTracker implements ManhattanDistanceValidatedTracker {

		private final LivingEntity source;
		private final Waypoint.Config config;
		private final ServerPlayerEntity receiver;
		private BlockPos pos;

		public PositionalWaypointTracker(LivingEntity source, Waypoint.Config config, ServerPlayerEntity receiver) {
			this.source = source;
			this.receiver = receiver;
			this.config = config;
			pos = source.getBlockPos();
		}

		@Override
		public void track() {
			receiver.networkHandler.sendPacket(WaypointS2CPacket.trackPos(source.getUuid(), config, pos));
		}

		@Override
		public void untrack() {
			receiver.networkHandler.sendPacket(WaypointS2CPacket.untrack(source.getUuid()));
		}

		@Override
		public void update() {
			BlockPos currentPos = source.getBlockPos();

			if (currentPos.getManhattanDistance(pos) > 0) {
				receiver.networkHandler.sendPacket(WaypointS2CPacket.updatePos(source.getUuid(), config, currentPos));
				pos = currentPos;
			}
		}

		@Override
		public int getDistanceToOriginalPos() {
			return pos.getManhattanDistance(source.getBlockPos());
		}

		@Override
		public boolean isInvalid() {
			return ManhattanDistanceValidatedTracker.super.isInvalid() || cannotReceive(source, receiver);
		}
	}

	/**
	 * Контракт серверного трекера вейпоинта для одного игрока-получателя.
	 */
	interface WaypointTracker {

		/** Отправляет пакет начала отслеживания. */
		void track();

		/** Отправляет пакет прекращения отслеживания. */
		void untrack();

		/** Отправляет пакет обновления, если данные изменились. */
		void update();

		/** Возвращает {@code true}, если трекер устарел и должен быть пересоздан. */
		boolean isInvalid();
	}
}
