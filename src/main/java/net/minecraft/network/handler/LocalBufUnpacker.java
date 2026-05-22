package net.minecraft.network.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.minecraft.network.OpaqueByteBufHolder;

/**
 * Netty-обработчик входящего трафика для локальных соединений:
 * распаковывает {@link OpaqueByteBufHolder} перед передачей дальше по pipeline.
 */
public class LocalBufUnpacker extends ChannelInboundHandlerAdapter {

	@Override
	public void channelRead(ChannelHandlerContext context, Object buf) {
		context.fireChannelRead(OpaqueByteBufHolder.unpack(buf));
	}
}
