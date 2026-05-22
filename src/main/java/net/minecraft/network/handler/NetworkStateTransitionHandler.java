package net.minecraft.network.handler;

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.packet.Packet;

/**
 * Интерфейс-маркер для обработчиков, которые умеют переключать состояние сетевого протокола.
 * Статические методы вызываются из {@link DecoderHandler} и {@link EncoderHandler}
 * после обработки пакета, помечённого как переходный.
 */
public interface NetworkStateTransitionHandler {

	static void onDecoded(ChannelHandlerContext context, Packet<?> packet) {
		if (!packet.transitionsNetworkState()) {
			return;
		}

		context.channel().config().setAutoRead(false);
		context.pipeline()
				.addBefore(context.name(), HandlerNames.INBOUND_CONFIG, new NetworkStateTransitions.InboundConfigurer());
		context.pipeline().remove(context.name());
	}

	static void onEncoded(ChannelHandlerContext context, Packet<?> packet) {
		if (!packet.transitionsNetworkState()) {
			return;
		}

		context.pipeline()
				.addAfter(context.name(), HandlerNames.OUTBOUND_CONFIG, new NetworkStateTransitions.OutboundConfigurer());
		context.pipeline().remove(context.name());
	}
}
