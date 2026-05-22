package net.minecraft.network.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.util.ReferenceCountUtil;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.state.NetworkState;

/**
 * Фабрика и реализации переходников состояния сетевого протокола.
 * Используется для замены {@link DecoderHandler}/{@link EncoderHandler} в Netty pipeline
 * при смене фазы соединения (например, LOGIN → CONFIGURATION → PLAY).
 */
public class NetworkStateTransitions {

	public static <T extends PacketListener> NetworkStateTransitions.DecoderTransitioner decoderTransitioner(
			NetworkState<T> newState
	) {
		return decoderSwapper(new DecoderHandler<T>(newState));
	}

	private static NetworkStateTransitions.DecoderTransitioner decoderSwapper(ChannelInboundHandler newDecoder) {
		return context -> {
			context.pipeline().replace(context.name(), HandlerNames.DECODER, newDecoder);
			context.channel().config().setAutoRead(true);
		};
	}

	public static <T extends PacketListener> NetworkStateTransitions.EncoderTransitioner encoderTransitioner(
			NetworkState<T> newState
	) {
		return encoderSwapper(new EncoderHandler<T>(newState));
	}

	private static NetworkStateTransitions.EncoderTransitioner encoderSwapper(ChannelOutboundHandler newEncoder) {
		return context -> context.pipeline().replace(context.name(), HandlerNames.ENCODER, newEncoder);
	}

	/**
	 * Функциональный интерфейс для переключения входящего декодера в pipeline.
	 */
	@FunctionalInterface
	public interface DecoderTransitioner {

		void run(ChannelHandlerContext context);

		default NetworkStateTransitions.DecoderTransitioner andThen(NetworkStateTransitions.DecoderTransitioner next) {
			return context -> {
				run(context);
				next.run(context);
			};
		}
	}

	/**
	 * Функциональный интерфейс для переключения исходящего энкодера в pipeline.
	 */
	@FunctionalInterface
	public interface EncoderTransitioner {

		void run(ChannelHandlerContext context);

		default NetworkStateTransitions.EncoderTransitioner andThen(NetworkStateTransitions.EncoderTransitioner next) {
			return context -> {
				run(context);
				next.run(context);
			};
		}
	}

	/**
	 * Временный обработчик, устанавливаемый в pipeline на время переключения входящего декодера.
	 * Отклоняет любые пакеты/буферы до завершения перехода.
	 */
	public static class InboundConfigurer extends ChannelDuplexHandler {

		@Override
		public void channelRead(ChannelHandlerContext context, Object received) {
			if (received instanceof ByteBuf || received instanceof Packet) {
				ReferenceCountUtil.release(received);
				throw new DecoderException(
						"Pipeline has no inbound protocol configured, can't process packet " + received);
			}

			context.fireChannelRead(received);
		}

		@Override
		public void write(ChannelHandlerContext context, Object received, ChannelPromise promise) throws Exception {
			if (!(received instanceof NetworkStateTransitions.DecoderTransitioner transitioner)) {
				context.write(received, promise);
				return;
			}

			try {
				transitioner.run(context);
			} finally {
				ReferenceCountUtil.release(received);
			}

			promise.setSuccess();
		}
	}

	/**
	 * Временный обработчик, устанавливаемый в pipeline на время переключения исходящего энкодера.
	 * Отклоняет любые пакеты до завершения перехода.
	 */
	public static class OutboundConfigurer extends ChannelOutboundHandlerAdapter {

		@Override
		public void write(ChannelHandlerContext context, Object received, ChannelPromise promise) throws Exception {
			if (received instanceof Packet) {
				ReferenceCountUtil.release(received);
				throw new EncoderException(
						"Pipeline has no outbound protocol configured, can't process packet " + received);
			}

			if (received instanceof NetworkStateTransitions.EncoderTransitioner transitioner) {
				try {
					transitioner.run(context);
				} finally {
					ReferenceCountUtil.release(received);
				}

				promise.setSuccess();
				return;
			}

			context.write(received, promise);
		}
	}
}
