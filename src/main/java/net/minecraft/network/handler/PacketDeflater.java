package net.minecraft.network.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import net.minecraft.network.encoding.VarInts;

import java.util.zip.Deflater;

/**
 * Netty-обработчик исходящих пакетов: сжимает данные алгоритмом DEFLATE.
 * Если размер пакета меньше порога {@code compressionThreshold}, пакет передаётся без сжатия
 * с VarInt-заголовком {@code 0} (признак отсутствия сжатия).
 */
public class PacketDeflater extends MessageToByteEncoder<ByteBuf> {

	private static final int MAX_PACKET_SIZE = 8388608;
	private static final int DEFLATE_BUFFER_SIZE = 8192;

	private final byte[] deflateBuffer = new byte[DEFLATE_BUFFER_SIZE];
	private final Deflater deflater;
	private int compressionThreshold;

	public PacketDeflater(int compressionThreshold) {
		this.compressionThreshold = compressionThreshold;
		deflater = new Deflater();
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, ByteBuf input, ByteBuf output) {
		int packetSize = input.readableBytes();

		if (packetSize > MAX_PACKET_SIZE) {
			throw new IllegalArgumentException(
					"Packet too big (is " + packetSize + ", should be less than " + MAX_PACKET_SIZE + ")"
			);
		}

		if (packetSize < compressionThreshold) {
			VarInts.write(output, 0);
			output.writeBytes(input);
			return;
		}

		byte[] bytes = new byte[packetSize];
		input.readBytes(bytes);
		VarInts.write(output, bytes.length);
		deflater.setInput(bytes, 0, packetSize);
		deflater.finish();

		while (!deflater.finished()) {
			int written = deflater.deflate(deflateBuffer);
			output.writeBytes(deflateBuffer, 0, written);
		}

		deflater.reset();
	}

	public int getCompressionThreshold() {
		return compressionThreshold;
	}

	public void setCompressionThreshold(int compressionThreshold) {
		this.compressionThreshold = compressionThreshold;
	}
}
