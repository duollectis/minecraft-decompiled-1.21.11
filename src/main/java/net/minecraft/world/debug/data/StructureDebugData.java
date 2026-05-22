package net.minecraft.world.debug.data;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.math.BlockBox;

import java.util.List;

/**
 * Отладочные данные структуры: общий ограничивающий прямоугольник и список составных частей.
 */
public record StructureDebugData(BlockBox boundingBox, List<StructureDebugData.Piece> pieces) {

	public static final PacketCodec<ByteBuf, StructureDebugData> PACKET_CODEC = PacketCodec.tuple(
			BlockBox.PACKET_CODEC,
			StructureDebugData::boundingBox,
			Piece.PACKET_CODEC.collect(PacketCodecs.toList()),
			StructureDebugData::pieces,
			StructureDebugData::new
	);

	/**
	 * Одна часть структуры: её ограничивающий прямоугольник и признак стартовой части.
	 */
	public record Piece(BlockBox boundingBox, boolean isStart) {

		public static final PacketCodec<ByteBuf, Piece> PACKET_CODEC = PacketCodec.tuple(
				BlockBox.PACKET_CODEC,
				Piece::boundingBox,
				PacketCodecs.BOOLEAN,
				Piece::isStart,
				Piece::new
		);
	}
}
