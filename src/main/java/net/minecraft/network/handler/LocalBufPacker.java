package net.minecraft.network.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.OpaqueByteBufHolder;

/**
 * Netty-обработчик исходящего трафика для локальных соединений:
 * оборачивает объект в {@link OpaqueByteBufHolder} перед записью в pipeline.
 */
public class LocalBufPacker extends ChannelOutboundHandlerAdapter {

	@Override
	public void write(ChannelHandlerContext context, Object buf, ChannelPromise promise) {
		context.write(OpaqueByteBufHolder.pack(buf), promise);
	}
}
