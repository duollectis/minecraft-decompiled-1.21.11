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
 * {@code ServerWaypoint}.
 */
public interface ServerWaypoint extends Waypoint {

	int AZIMUTH_THRESHOLD = 332;

	boolean hasWaypoint();

	Optional<ServerWaypoint.WaypointTracker> createTracker(ServerPlayerEntity receiver);

	Waypoint.Config getWaypointConfig();

	static boolean cannotReceive(LivingEntity source, ServerPlayerEntity receiver) {
		if (receiver.isSpectator()) {
			return false;
		}
		else if (!source.isSpectator() && !source.hasPassengerDeep(receiver)) {
			double d = Math.min(
					source.getAttributeValue(EntityAttributes.WAYPOINT_TRANSMIT_RANGE),
					receiver.getAttributeValue(EntityAttributes.WAYPOINT_RECEIVE_RANGE)
			);
			return source.distanceTo(receiver) >= d;
		}
		else {
			return true;
		}
	}

	static boolean canReceive(ChunkPos source, ServerPlayerEntity receiver) {
		return receiver.getChunkFilter().isWithinDistanceExcludingEdge(source.x, source.z);
	}

	static boolean shouldUseAzimuth(LivingEntity source, ServerPlayerEntity receiver) {
		return source.distanceTo(receiver) > 332.0F;
	}

	/**
	 * {@code AzimuthWaypointTracker}.
	 */
	public static class AzimuthWaypointTracker implements ServerWaypoint.WaypointTracker {

		private final LivingEntity source;
		private final Waypoint.Config config;
		private final ServerPlayerEntity receiver;
		private float azimuth;

		public AzimuthWaypointTracker(LivingEntity source, Waypoint.Config config, ServerPlayerEntity receiver) {
			this.source = source;
			this.config = config;
			this.receiver = receiver;
			Vec3d vec3d = receiver.getEntityPos().subtract(source.getEntityPos()).rotateYClockwise();
			this.azimuth = (float) MathHelper.atan2(vec3d.getZ(), vec3d.getX());
		}

		@Override
		public boolean isInvalid() {
			return ServerWaypoint.cannotReceive(this.source, this.receiver)
					|| ServerWaypoint.canReceive(this.source.getChunkPos(), this.receiver)
					|| !ServerWaypoint.shouldUseAzimuth(this.source, this.receiver);
		}

		@Override
		public void track() {
			this.receiver.networkHandler.sendPacket(WaypointS2CPacket.trackAzimuth(
					this.source.getUuid(),
					this.config,
					this.azimuth
			));
		}

		@Override
		public void untrack() {
			this.receiver.networkHandler.sendPacket(WaypointS2CPacket.untrack(this.source.getUuid()));
		}

		@Override
		public void update() {
			Vec3d vec3d = this.receiver.getEntityPos().subtract(this.source.getEntityPos()).rotateYClockwise();
			float f = (float) MathHelper.atan2(vec3d.getZ(), vec3d.getX());
			if (MathHelper.abs(f - this.azimuth) > 0.008726646F) {
				this.receiver.networkHandler.sendPacket(WaypointS2CPacket.updateAzimuth(
						this.source.getUuid(),
						this.config,
						f
				));
				this.azimuth = f;
			}
		}
	}

	/**
	 * {@code ChebyshevDistanceValidatedTracker}.
	 */
	public interface ChebyshevDistanceValidatedTracker extends ServerWaypoint.WaypointTracker {

		int getDistanceToOriginalPos();

		@Override
		default boolean isInvalid() {
			return this.getDistanceToOriginalPos() > 1;
		}
	}

	/**
	 * {@code ChunkWaypointTracker}.
	 */
	public static class ChunkWaypointTracker implements ServerWaypoint.ChebyshevDistanceValidatedTracker {

		private final LivingEntity source;
		private final Waypoint.Config config;
		private final ServerPlayerEntity receiver;
		private ChunkPos chunkPos;

		public ChunkWaypointTracker(LivingEntity source, Waypoint.Config config, ServerPlayerEntity receiver) {
			this.source = source;
			this.config = config;
			this.receiver = receiver;
			this.chunkPos = source.getChunkPos();
		}

		@Override
		public int getDistanceToOriginalPos() {
			return this.chunkPos.getChebyshevDistance(this.source.getChunkPos());
		}

		@Override
		public void track() {
			this.receiver.networkHandler.sendPacket(WaypointS2CPacket.trackChunk(
					this.source.getUuid(),
					this.config,
					this.chunkPos
			));
		}

		@Override
		public void untrack() {
			this.receiver.networkHandler.sendPacket(WaypointS2CPacket.untrack(this.source.getUuid()));
		}

		@Override
		public void update() {
			ChunkPos chunkPos = this.source.getChunkPos();
			if (chunkPos.getChebyshevDistance(this.chunkPos) > 0) {
				this.receiver.networkHandler.sendPacket(WaypointS2CPacket.updateChunk(
						this.source.getUuid(),
						this.config,
						chunkPos
				));
				this.chunkPos = chunkPos;
			}
		}

		@Override
		public boolean isInvalid() {
			return !ServerWaypoint.ChebyshevDistanceValidatedTracker.super.isInvalid() && !ServerWaypoint.cannotReceive(
					this.source,
					this.receiver
			)
			       ? ServerWaypoint.canReceive(this.chunkPos, this.receiver)
			       : true;
		}
	}

	/**
	 * {@code ManhattanDistanceValidatedTracker}.
	 */
	public interface ManhattanDistanceValidatedTracker extends ServerWaypoint.WaypointTracker {

		int getDistanceToOriginalPos();

		@Override
		default boolean isInvalid() {
			return this.getDistanceToOriginalPos() > 1;
		}
	}

	/**
	 * {@code PositionalWaypointTracker}.
	 */
	public static class PositionalWaypointTracker implements ServerWaypoint.ManhattanDistanceValidatedTracker {

		private final LivingEntity source;
		private final Waypoint.Config config;
		private final ServerPlayerEntity receiver;
		private BlockPos pos;

		public PositionalWaypointTracker(LivingEntity source, Waypoint.Config config, ServerPlayerEntity receiver) {
			this.source = source;
			this.receiver = receiver;
			this.config = config;
			this.pos = source.getBlockPos();
		}

		@Override
		public void track() {
			this.receiver.networkHandler.sendPacket(WaypointS2CPacket.trackPos(
					this.source.getUuid(),
					this.config,
					this.pos
			));
		}

		@Override
		public void untrack() {
			this.receiver.networkHandler.sendPacket(WaypointS2CPacket.untrack(this.source.getUuid()));
		}

		@Override
		public void update() {
			BlockPos blockPos = this.source.getBlockPos();
			if (blockPos.getManhattanDistance(this.pos) > 0) {
				this.receiver.networkHandler.sendPacket(WaypointS2CPacket.updatePos(
						this.source.getUuid(),
						this.config,
						blockPos
				));
				this.pos = blockPos;
			}
		}

		@Override
		public int getDistanceToOriginalPos() {
			return this.pos.getManhattanDistance(this.source.getBlockPos());
		}

		@Override
		public boolean isInvalid() {
			return ServerWaypoint.ManhattanDistanceValidatedTracker.super.isInvalid() || ServerWaypoint.cannotReceive(
					this.source,
					this.receiver
			);
		}
	}

	/**
	 * {@code WaypointTracker}.
	 */
	public interface WaypointTracker {

		void track();

		void untrack();

		void update();

		boolean isInvalid();
	}
}
