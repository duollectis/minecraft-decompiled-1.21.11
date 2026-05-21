package net.minecraft.network.packet.s2c.play;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.network.packet.PlayPackets;
import net.minecraft.util.function.ValueLists;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.waypoint.TrackedWaypoint;
import net.minecraft.world.waypoint.TrackedWaypointHandler;
import net.minecraft.world.waypoint.Waypoint;
import net.minecraft.world.waypoint.WaypointHandler;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;

public record WaypointS2CPacket(
		WaypointS2CPacket.Operation operation,
		TrackedWaypoint waypoint
) implements Packet<ClientPlayPacketListener> {

	public static final PacketCodec<RegistryByteBuf, WaypointS2CPacket> CODEC = PacketCodec.tuple(
			WaypointS2CPacket.Operation.PACKET_CODEC,
			WaypointS2CPacket::operation,
			TrackedWaypoint.PACKET_CODEC,
			WaypointS2CPacket::waypoint,
			WaypointS2CPacket::new
	);

	public static WaypointS2CPacket untrack(UUID source) {
		return new WaypointS2CPacket(WaypointS2CPacket.Operation.UNTRACK, TrackedWaypoint.empty(source));
	}

	public static WaypointS2CPacket trackPos(UUID source, Waypoint.Config config, Vec3i pos) {
		return new WaypointS2CPacket(WaypointS2CPacket.Operation.TRACK, TrackedWaypoint.ofPos(source, config, pos));
	}

	public static WaypointS2CPacket updatePos(UUID source, Waypoint.Config config, Vec3i pos) {
		return new WaypointS2CPacket(WaypointS2CPacket.Operation.UPDATE, TrackedWaypoint.ofPos(source, config, pos));
	}

	public static WaypointS2CPacket trackChunk(UUID source, Waypoint.Config config, ChunkPos chunkPos) {
		return new WaypointS2CPacket(
				WaypointS2CPacket.Operation.TRACK,
				TrackedWaypoint.ofChunk(source, config, chunkPos)
		);
	}

	public static WaypointS2CPacket updateChunk(UUID source, Waypoint.Config config, ChunkPos chunkPos) {
		return new WaypointS2CPacket(
				WaypointS2CPacket.Operation.UPDATE,
				TrackedWaypoint.ofChunk(source, config, chunkPos)
		);
	}

	public static WaypointS2CPacket trackAzimuth(UUID source, Waypoint.Config config, float azimuth) {
		return new WaypointS2CPacket(
				WaypointS2CPacket.Operation.TRACK,
				TrackedWaypoint.ofAzimuth(source, config, azimuth)
		);
	}

	public static WaypointS2CPacket updateAzimuth(UUID source, Waypoint.Config config, float azimuth) {
		return new WaypointS2CPacket(
				WaypointS2CPacket.Operation.UPDATE,
				TrackedWaypoint.ofAzimuth(source, config, azimuth)
		);
	}

	@Override
	public PacketType<WaypointS2CPacket> getPacketType() {
		return PlayPackets.WAYPOINT;
	}

	public void apply(ClientPlayPacketListener clientPlayPacketListener) {
		clientPlayPacketListener.onWaypoint(this);
	}

	public void apply(TrackedWaypointHandler handler) {
		this.operation.handler.accept(handler, this.waypoint);
	}

	static enum Operation {
		TRACK(WaypointHandler::onTrack),
		UNTRACK(WaypointHandler::onUntrack),
		UPDATE(WaypointHandler::onUpdate);

		final BiConsumer<TrackedWaypointHandler, TrackedWaypoint> handler;
		public static final IntFunction<WaypointS2CPacket.Operation> BY_INDEX = ValueLists.createIndexToValueFunction(
				(WaypointS2CPacket.Operation op) -> op.ordinal(), values(), ValueLists.OutOfBoundsHandling.WRAP
		);
		public static final PacketCodec<ByteBuf, WaypointS2CPacket.Operation>
				PACKET_CODEC =
				PacketCodecs.indexed(BY_INDEX, Enum::ordinal);

		private Operation(final BiConsumer<TrackedWaypointHandler, TrackedWaypoint> handler) {
			this.handler = handler;
		}
	}
}
