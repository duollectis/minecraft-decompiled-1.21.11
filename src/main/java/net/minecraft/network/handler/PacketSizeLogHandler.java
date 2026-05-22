package net.minecraft.network.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.minecraft.network.OpaqueByteBufHolder;

/**
 * Netty-обработчик входящих сообщений, который фиксирует размер каждого пакета
 * в {@link PacketSizeLogger} для последующего отображения в профилировщике.
 */
public class PacketSizeLogHandler extends ChannelInboundHandlerAdapter {

	private final PacketSizeLogger logger;

	public PacketSizeLogHandler(PacketSizeLogger logger) {
		this.logger = logger;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		Object unpacked = OpaqueByteBufHolder.unpack(msg);

		if (unpacked instanceof ByteBuf byteBuf) {
			logger.increment(byteBuf.readableBytes());
		}

		ctx.fireChannelRead(unpacked);
	}
}
