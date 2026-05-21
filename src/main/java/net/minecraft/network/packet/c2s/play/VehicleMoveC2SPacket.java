package net.minecraft.network.packet.c2s.play;

import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.network.packet.PlayPackets;
import net.minecraft.util.math.Vec3d;

/**
 * Запись vehicle move c2 s packet.
 */
public record VehicleMoveC2SPacket(
		Vec3d position,
		float yaw,
		float pitch,
		boolean onGround
) implements Packet<ServerPlayPacketListener> {

	public static final PacketCodec<PacketByteBuf, VehicleMoveC2SPacket> CODEC = PacketCodec.tuple(
			Vec3d.PACKET_CODEC,
			VehicleMoveC2SPacket::position,
			PacketCodecs.FLOAT,
			VehicleMoveC2SPacket::yaw,
			PacketCodecs.FLOAT,
			VehicleMoveC2SPacket::pitch,
			PacketCodecs.BOOLEAN,
			VehicleMoveC2SPacket::onGround,
			VehicleMoveC2SPacket::new
	);

	/**
	 * From vehicle.
	 *
	 * @param vehicle vehicle
	 *
	 * @return VehicleMoveC2SPacket — результат операции
	 */
	public static VehicleMoveC2SPacket fromVehicle(Entity vehicle) {
		return vehicle.isInterpolating()
		       ? new VehicleMoveC2SPacket(
				vehicle.getInterpolator().getLerpedPos(),
				vehicle.getInterpolator().getLerpedYaw(),
				vehicle.getInterpolator().getLerpedPitch(),
				vehicle.isOnGround()
		)
		       : new VehicleMoveC2SPacket(
				       vehicle.getEntityPos(),
				       vehicle.getYaw(),
				       vehicle.getPitch(),
				       vehicle.isOnGround()
		       );
	}

	@Override
	public PacketType<VehicleMoveC2SPacket> getPacketType() {
		return PlayPackets.MOVE_VEHICLE_C2S;
	}

	/**
	 * Apply.
	 *
	 * @param serverPlayPacketListener server play packet listener
	 */
	public void apply(ServerPlayPacketListener serverPlayPacketListener) {
		serverPlayPacketListener.onVehicleMove(this);
	}
}
