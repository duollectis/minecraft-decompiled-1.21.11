package net.minecraft.network.handler;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.network.state.NetworkState;
import net.minecraft.util.profiling.jfr.FlightProfiler;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;

/**
 * Netty-обработчик входящего трафика: декодирует {@link ByteBuf} в {@link Packet}
 * согласно текущему {@link NetworkState}.
 */
public class DecoderHandler<T extends PacketListener> extends ByteToMessageDecoder implements NetworkStateTransitionHandler {

	private static final Logger LOGGER = LogUtils.getLogger();
	private final NetworkState<T> state;

	public DecoderHandler(NetworkState<T> state) {
		this.state = state;
	}

	@Override
	protected void decode(ChannelHandlerContext context, ByteBuf buf, List<Object> out) throws Exception {
		int packetSize = buf.readableBytes();

		Packet<? super T> packet;
		try {
			packet = state.codec().decode(buf);
		} catch (Exception e) {
			if (e instanceof PacketException) {
				buf.skipBytes(buf.readableBytes());
			}

			throw e;
		}

		PacketType<? extends Packet<? super T>> packetType = packet.getPacketType();
		FlightProfiler.INSTANCE.onPacketReceived(state.id(), packetType, context.channel().remoteAddress(), packetSize);

		if (buf.readableBytes() > 0) {
			throw new IOException(
					"Packet "
							+ state.id().getId()
							+ "/"
							+ packetType
							+ " ("
							+ packet.getClass().getSimpleName()
							+ ") was larger than I expected, found "
							+ buf.readableBytes()
							+ " bytes extra whilst reading packet "
							+ packetType
			);
		}

		out.add(packet);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(
					ClientConnection.PACKET_RECEIVED_MARKER,
					" IN: [{}:{}] {} -> {} bytes",
					new Object[]{state.id().getId(), packetType, packet.getClass().getName(), packetSize}
			);
		}

		NetworkStateTransitionHandler.onDecoded(context, packet);
	}
}
