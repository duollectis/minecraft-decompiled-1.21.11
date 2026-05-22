package net.minecraft.network.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToByteEncoder;
import net.minecraft.network.encoding.VarInts;

/**
 * Netty-обработчик исходящих пакетов: добавляет VarInt-заголовок с длиной пакета.
 * Максимальный размер заголовка — {@value #MAX_PREPEND_LENGTH} байта (21-битное число).
 * Помечен {@link Sharable}, так как не хранит состояния соединения.
 */
@Sharable
public class SizePrepender extends MessageToByteEncoder<ByteBuf> {

	public static final int MAX_PREPEND_LENGTH = 3;

	@Override
	protected void encode(ChannelHandlerContext ctx, ByteBuf input, ByteBuf output) {
		int packetSize = input.readableBytes();
		int headerSize = VarInts.getSizeInBytes(packetSize);

		if (headerSize > MAX_PREPEND_LENGTH) {
			throw new EncoderException("Packet too large: size " + packetSize + " is over 8");
		}

		output.ensureWritable(headerSize + packetSize);
		VarInts.write(output, packetSize);
		output.writeBytes(input, input.readerIndex(), packetSize);
	}
}
