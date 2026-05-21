package net.minecraft.network.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.minecraft.network.OpaqueByteBufHolder;

/**
 * Класс local buf unpacker.
 */
public class LocalBufUnpacker extends ChannelInboundHandlerAdapter {

	/**
	 * Channel read.
	 *
	 * @param context context
	 * @param buf buf
	 */
	public void channelRead(ChannelHandlerContext context, Object buf) {
		context.fireChannelRead(OpaqueByteBufHolder.unpack(buf));
	}
}
