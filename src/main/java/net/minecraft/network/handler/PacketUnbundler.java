package net.minecraft.network.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import net.minecraft.network.packet.Packet;

import java.util.List;

/**
 * Netty-обработчик исходящих пакетов: разворачивает бандл пакетов в отдельные сообщения.
 * Если пакет инициирует смену состояния сети, обработчик удаляет себя из пайплайна.
 */
public class PacketUnbundler extends MessageToMessageEncoder<Packet<?>> {

	private final PacketBundleHandler bundleHandler;

	public PacketUnbundler(PacketBundleHandler bundleHandler) {
		this.bundleHandler = bundleHandler;
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, Packet<?> packet, List<Object> output) throws Exception {
		bundleHandler.forEachPacket(packet, output::add);

		if (packet.transitionsNetworkState()) {
			ctx.pipeline().remove(ctx.name());
		}
	}
}
