package net.minecraft.world.debug.data;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.math.BlockBox;

import java.util.List;

/**
 * {@code StructureDebugData}.
 */
public record StructureDebugData(BlockBox boundingBox, List<StructureDebugData.Piece> pieces) {

	public static final PacketCodec<ByteBuf, StructureDebugData> PACKET_CODEC = PacketCodec.tuple(
			BlockBox.PACKET_CODEC,
			StructureDebugData::boundingBox,
			StructureDebugData.Piece.PACKET_CODEC.collect(PacketCodecs.toList()),
			StructureDebugData::pieces,
			StructureDebugData::new
	);

	/**
	 * {@code Piece}.
	 */
	public record Piece(BlockBox boundingBox, boolean isStart) {

		public static final PacketCodec<ByteBuf, StructureDebugData.Piece> PACKET_CODEC = PacketCodec.tuple(
				BlockBox.PACKET_CODEC,
				StructureDebugData.Piece::boundingBox,
				PacketCodecs.BOOLEAN,
				StructureDebugData.Piece::isStart,
				StructureDebugData.Piece::new
		);
	}
}
