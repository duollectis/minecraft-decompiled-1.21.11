package net.minecraft.world.waypoint;

import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.encoding.VarInts;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.apache.commons.lang3.function.TriFunction;
import org.slf4j.Logger;

import java.util.UUID;

/**
 * Клиентское представление вейпоинта, полученного от сервера.
 * <p>
 * Существует в трёх режимах: позиционный ({@link Positional}), чанковый ({@link ChunkBased})
 * и азимутальный ({@link Azimuth}). Режим определяется при создании и закодирован в {@link Type}.
 * Пустой вейпоинт ({@link Empty}) используется как заглушка при прекращении отслеживания.
 */
public abstract class TrackedWaypoint implements Waypoint {

	static final Logger LOGGER = LogUtils.getLogger();

	public static final PacketCodec<ByteBuf, TrackedWaypoint> PACKET_CODEC =
		PacketCodec.of(TrackedWaypoint::writeBuf, TrackedWaypoint::fromBuf);

	protected final Either<UUID, String> source;
	private final Waypoint.Config config;
	private final Type type;

	TrackedWaypoint(Either<UUID, String> source, Waypoint.Config config, Type type) {
		this.source = source;
		this.config = config;
		this.type = type;
	}

	public Either<UUID, String> getSource() {
		return source;
	}

	public abstract void handleUpdate(TrackedWaypoint waypoint);

	public void writeBuf(ByteBuf buf) {
		PacketByteBuf packetBuf = new PacketByteBuf(buf);
		packetBuf.writeEither(source, Uuids.PACKET_CODEC, PacketByteBuf::writeString);
		Waypoint.Config.PACKET_CODEC.encode(packetBuf, config);
		packetBuf.writeEnumConstant(type);
		writeAdditionalDataToBuf(buf);
	}

	public abstract void writeAdditionalDataToBuf(ByteBuf buf);

	private static TrackedWaypoint fromBuf(ByteBuf buf) {
		PacketByteBuf packetBuf = new PacketByteBuf(buf);
		Either<UUID, String> source = packetBuf.readEither(Uuids.PACKET_CODEC, PacketByteBuf::readString);
		Waypoint.Config config = Waypoint.Config.PACKET_CODEC.decode(packetBuf);
		Type type = packetBuf.readEnumConstant(Type.class);
		return type.factory.apply(source, config, packetBuf);
	}

	public static TrackedWaypoint ofPos(UUID source, Waypoint.Config config, Vec3i pos) {
		return new Positional(source, config, pos);
	}

	public static TrackedWaypoint ofChunk(UUID source, Waypoint.Config config, ChunkPos chunkPos) {
		return new ChunkBased(source, config, chunkPos);
	}

	public static TrackedWaypoint ofAzimuth(UUID source, Waypoint.Config config, float azimuth) {
		return new Azimuth(source, config, azimuth);
	}

	public static TrackedWaypoint empty(UUID uuid) {
		return new Empty(uuid);
	}

	/**
	 * Вычисляет относительный угол рыскания (yaw) от камеры до вейпоинта.
	 * Используется для отрисовки индикатора направления на HUD.
	 */
	public abstract double getRelativeYaw(
		World world,
		YawProvider yawProvider,
		EntityTickProgress tickProgress
	);

	/**
	 * Определяет вертикальный угол наклона (pitch) к вейпоинту относительно камеры.
	 * Используется для отображения стрелки вверх/вниз, когда вейпоинт вне экрана.
	 */
	public abstract Pitch getPitch(
		World world,
		PitchProvider cameraProvider,
		EntityTickProgress tickProgress
	);

	public abstract double squaredDistanceTo(Entity receiver);

	public Waypoint.Config getConfig() {
		return config;
	}

	// -------------------------------------------------------------------------
	// Реализации
	// -------------------------------------------------------------------------

	static class Azimuth extends TrackedWaypoint {

		private float azimuth;

		public Azimuth(UUID source, Waypoint.Config config, float azimuth) {
			super(Either.left(source), config, Type.AZIMUTH);
			this.azimuth = azimuth;
		}

		public Azimuth(Either<UUID, String> source, Waypoint.Config config, PacketByteBuf buf) {
			super(source, config, Type.AZIMUTH);
			azimuth = buf.readFloat();
		}

		@Override
		public void handleUpdate(TrackedWaypoint waypoint) {
			if (waypoint instanceof Azimuth other) {
				azimuth = other.azimuth;
			} else {
				LOGGER.warn("Unsupported Waypoint update operation: {}", waypoint.getClass());
			}
		}

		@Override
		public void writeAdditionalDataToBuf(ByteBuf buf) {
			buf.writeFloat(azimuth);
		}

		@Override
		public double getRelativeYaw(World world, YawProvider yawProvider, EntityTickProgress tickProgress) {
			return MathHelper.subtractAngles(yawProvider.getCameraYaw(), azimuth * (180.0F / (float) Math.PI));
		}

		@Override
		public Pitch getPitch(World world, PitchProvider cameraProvider, EntityTickProgress tickProgress) {
			double pitch = cameraProvider.getPitch();

			if (pitch < -1.0) {
				return Pitch.DOWN;
			}

			return pitch > 1.0 ? Pitch.UP : Pitch.NONE;
		}

		@Override
		public double squaredDistanceTo(Entity receiver) {
			return Double.POSITIVE_INFINITY;
		}
	}

	static class ChunkBased extends TrackedWaypoint {

		private ChunkPos chunkPos;

		public ChunkBased(UUID source, Waypoint.Config config, ChunkPos chunkPos) {
			super(Either.left(source), config, Type.CHUNK);
			this.chunkPos = chunkPos;
		}

		public ChunkBased(Either<UUID, String> source, Waypoint.Config config, PacketByteBuf buf) {
			super(source, config, Type.CHUNK);
			chunkPos = new ChunkPos(buf.readVarInt(), buf.readVarInt());
		}

		@Override
		public void handleUpdate(TrackedWaypoint waypoint) {
			if (waypoint instanceof ChunkBased other) {
				chunkPos = other.chunkPos;
			} else {
				LOGGER.warn("Unsupported Waypoint update operation: {}", waypoint.getClass());
			}
		}

		@Override
		public void writeAdditionalDataToBuf(ByteBuf buf) {
			VarInts.write(buf, chunkPos.x);
			VarInts.write(buf, chunkPos.z);
		}

		private Vec3d getChunkCenterPos(double y) {
			return Vec3d.ofCenter(chunkPos.getCenterAtY((int) y));
		}

		@Override
		public double getRelativeYaw(World world, YawProvider yawProvider, EntityTickProgress tickProgress) {
			Vec3d cameraPos = yawProvider.getCameraPos();
			Vec3d direction = cameraPos.subtract(getChunkCenterPos(cameraPos.getY())).rotateYClockwise();
			float angle = (float) MathHelper.atan2(direction.getZ(), direction.getX()) * (180.0F / (float) Math.PI);
			return MathHelper.subtractAngles(yawProvider.getCameraYaw(), angle);
		}

		@Override
		public Pitch getPitch(World world, PitchProvider cameraProvider, EntityTickProgress tickProgress) {
			double pitch = cameraProvider.getPitch();

			if (pitch < -1.0) {
				return Pitch.DOWN;
			}

			return pitch > 1.0 ? Pitch.UP : Pitch.NONE;
		}

		@Override
		public double squaredDistanceTo(Entity receiver) {
			return receiver.squaredDistanceTo(Vec3d.ofCenter(chunkPos.getCenterAtY(receiver.getBlockY())));
		}
	}

	static class Empty extends TrackedWaypoint {

		private Empty(Either<UUID, String> source, Waypoint.Config config, PacketByteBuf buf) {
			super(source, config, Type.EMPTY);
		}

		Empty(UUID source) {
			super(Either.left(source), Waypoint.Config.DEFAULT, Type.EMPTY);
		}

		@Override
		public void handleUpdate(TrackedWaypoint waypoint) {
		}

		@Override
		public void writeAdditionalDataToBuf(ByteBuf buf) {
		}

		@Override
		public double getRelativeYaw(World world, YawProvider yawProvider, EntityTickProgress tickProgress) {
			return Double.NaN;
		}

		@Override
		public Pitch getPitch(World world, PitchProvider cameraProvider, EntityTickProgress tickProgress) {
			return Pitch.NONE;
		}

		@Override
		public double squaredDistanceTo(Entity receiver) {
			return Double.POSITIVE_INFINITY;
		}
	}

	static class Positional extends TrackedWaypoint {

		private Vec3i pos;

		public Positional(UUID uuid, Waypoint.Config config, Vec3i pos) {
			super(Either.left(uuid), config, Type.VEC3I);
			this.pos = pos;
		}

		public Positional(Either<UUID, String> source, Waypoint.Config config, PacketByteBuf buf) {
			super(source, config, Type.VEC3I);
			pos = new Vec3i(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
		}

		@Override
		public void handleUpdate(TrackedWaypoint waypoint) {
			if (waypoint instanceof Positional other) {
				pos = other.pos;
			} else {
				LOGGER.warn("Unsupported Waypoint update operation: {}", waypoint.getClass());
			}
		}

		@Override
		public void writeAdditionalDataToBuf(ByteBuf buf) {
			VarInts.write(buf, pos.getX());
			VarInts.write(buf, pos.getY());
			VarInts.write(buf, pos.getZ());
		}

		/**
		 * Возвращает позицию источника для вычисления направления.
		 * <p>
		 * Если источник — сущность и она находится в пределах 3 блоков от сохранённой позиции,
		 * используется интерполированная позиция камеры сущности. Иначе — центр блока позиции.
		 */
		private Vec3d getSourcePos(World world, EntityTickProgress tickProgress) {
			return source
				.left()
				.map(world::getEntity)
				.map(entity -> entity.getBlockPos().getManhattanDistance(pos) > 3
					? null
					: entity.getCameraPosVec(tickProgress.getTickProgress(entity))
				)
				.orElseGet(() -> Vec3d.ofCenter(pos));
		}

		@Override
		public double getRelativeYaw(World world, YawProvider yawProvider, EntityTickProgress tickProgress) {
			Vec3d direction = yawProvider.getCameraPos()
				.subtract(getSourcePos(world, tickProgress))
				.rotateYClockwise();
			float angle = (float) MathHelper.atan2(direction.getZ(), direction.getX()) * (180.0F / (float) Math.PI);
			return MathHelper.subtractAngles(yawProvider.getCameraYaw(), angle);
		}

		/**
		 * Определяет вертикальный наклон с учётом того, находится ли вейпоинт
		 * за камерой (z > 1.0 в пространстве проекции — инвертируем Y).
		 */
		@Override
		public Pitch getPitch(World world, PitchProvider cameraProvider, EntityTickProgress tickProgress) {
			Vec3d projected = cameraProvider.project(getSourcePos(world, tickProgress));
			boolean isBehind = projected.z > 1.0;
			double effectiveY = isBehind ? -projected.y : projected.y;

			if (effectiveY < -1.0) {
				return Pitch.DOWN;
			}

			if (effectiveY > 1.0) {
				return Pitch.UP;
			}

			if (isBehind) {
				if (projected.y > 0.0) {
					return Pitch.UP;
				}

				if (projected.y < 0.0) {
					return Pitch.DOWN;
				}
			}

			return Pitch.NONE;
		}

		@Override
		public double squaredDistanceTo(Entity receiver) {
			return receiver.squaredDistanceTo(Vec3d.ofCenter(pos));
		}
	}

	// -------------------------------------------------------------------------
	// Вложенные типы
	// -------------------------------------------------------------------------

	/** Вертикальный угол наклона вейпоинта относительно камеры. */
	public enum Pitch {
		NONE,
		UP,
		DOWN
	}

	public interface PitchProvider {

		Vec3d project(Vec3d sourcePos);

		double getPitch();
	}

	/** Тип вейпоинта, определяющий способ десериализации из пакета. */
	enum Type {
		EMPTY(Empty::new),
		VEC3I(Positional::new),
		CHUNK(ChunkBased::new),
		AZIMUTH(Azimuth::new);

		final TriFunction<Either<UUID, String>, Waypoint.Config, PacketByteBuf, TrackedWaypoint> factory;

		Type(TriFunction<Either<UUID, String>, Waypoint.Config, PacketByteBuf, TrackedWaypoint> factory) {
			this.factory = factory;
		}
	}

	public interface YawProvider {

		float getCameraYaw();

		Vec3d getCameraPos();
	}
}
