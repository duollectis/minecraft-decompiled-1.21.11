package net.minecraft.network.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.minecraft.network.OpaqueByteBufHolder;

/**
 * Класс packet size log handler.
 */
public class PacketSizeLogHandler extends ChannelInboundHandlerAdapter {

	private final PacketSizeLogger logger;

	public PacketSizeLogHandler(PacketSizeLogger logger) {
		this.logger = logger;
	}

	/**
	 * Channel read.
	 *
	 * @param context context
	 * @param value value
	 */
	public void channelRead(ChannelHandlerContext context, Object value) {
		value = OpaqueByteBufHolder.unpack(value);
		if (value instanceof ByteBuf byteBuf) {
			this.logger.increment(byteBuf.readableBytes());
		}

		context.fireChannelRead(value);
	}
}
