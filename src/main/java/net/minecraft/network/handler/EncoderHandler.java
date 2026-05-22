package net.minecraft.network.handler;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.network.state.NetworkState;
import net.minecraft.util.profiling.jfr.FlightProfiler;
import org.slf4j.Logger;

/**
 * Netty-обработчик исходящего трафика: кодирует {@link Packet} в {@link ByteBuf}
 * согласно текущему {@link NetworkState}.
 */
public class EncoderHandler<T extends PacketListener> extends MessageToByteEncoder<Packet<T>> {

	private static final Logger LOGGER = LogUtils.getLogger();
	private final NetworkState<T> state;

	public EncoderHandler(NetworkState<T> state) {
		this.state = state;
	}

	@Override
	protected void encode(ChannelHandlerContext context, Packet<T> packet, ByteBuf out) throws Exception {
		PacketType<? extends Packet<? super T>> packetType = packet.getPacketType();

		try {
			state.codec().encode(out, packet);
			int packetSize = out.readableBytes();

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(
						ClientConnection.PACKET_SENT_MARKER,
						"OUT: [{}:{}] {} -> {} bytes",
						new Object[]{state.id().getId(), packetType, packet.getClass().getName(), packetSize}
				);
			}

			FlightProfiler.INSTANCE.onPacketSent(
					state.id(),
					packetType,
					context.channel().remoteAddress(),
					packetSize
			);
		} catch (Throwable e) {
			LOGGER.error("Error sending packet {}", packetType, e);
			if (packet.isWritingErrorSkippable()) {
				throw new PacketEncoderException(e);
			}

			throw e;
		} finally {
			NetworkStateTransitionHandler.onEncoded(context, packet);
		}
	}
}
